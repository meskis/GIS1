package org.eskis.gis1.Core;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.ComponentOrientation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import org.eskis.gis1.GIS;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.AreaFunction;
import org.geotools.filter.Filter;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.util.CheckedHashMap;
import org.geotools.util.CheckedHashSet;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.And;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Intersects;
import org.opengis.geometry.BoundingBox;

public class Analyzer {

    protected GIS gisFrame;
    private JTextPane textPane;
    SimpleFeatureCollection polygonCollection = null;
    SimpleFeatureCollection fcResult = null;
    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
    SimpleFeature feature = null;
    Filter polyCheck = null;
    Filter andFil = null;
    Filter boundsCheck = null;
    String qryStr = null;
    String path = "C:\\Users\\Marcus\\Dropbox\\VU\\GIS\\gis_data";
    
    HashMap<String, Layer> loadedLayers;

    /**
     * Constructor
     *
     * @param gisMap
     */
    public Analyzer(GIS gisMap) {
        this.gisFrame = gisMap;
        
        loadedLayers = new HashMap<String, Layer>();
    }

    /**
     * Setter JTextPanel
     *
     * @param textPane
     */
    public void setOutput(JTextPane textPane) {
        this.textPane = textPane;
    }

    /**
     * Analyzer method
     */
    public void analyze() throws IOException {

        // Reset output
        this.textPane.removeAll();

        try {
            // get selected areas
            FeatureCollection selectedAreas = this.getSelectedAreas();
            System.out.println("Areas selected: " + selectedAreas.size());

            FeatureIterator iter = selectedAreas.features();

            while (iter.hasNext()) {
                Feature area = iter.next();

                AnalyzeArea((SimpleFeature) area);
            }

        } catch (Exception e) {
            addOutput("Error: " + e.toString());
        }

    }

    /**
     * Get selected features in the list
     *
     * @return
     */
    protected FeatureCollection getSelectedAreas() throws Exception {
        Layer layer = this.getAreaLayer();

        int[] selectedRows = gisFrame.getInfoTable().getSelectedRows();

        if (selectedRows.length == 0) {
            gisFrame.getInfoTable().selectAll();
            selectedRows = gisFrame.getInfoTable().getSelectedRows();
        }

        Set<FeatureId> IDs = new HashSet<FeatureId>();

        FeatureCollection selectedFeatures = new ListFeatureCollection(
                (SimpleFeatureType) layer.getFeatureSource().getSchema());

        for (int i = 0; i < selectedRows.length; i++) {
            String featureID = (String) gisFrame.getInfoTable().getValueAt(selectedRows[i], 0);

            try {
                FeatureCollection allFeatures = layer.getFeatureSource().getFeatures();
                FeatureIterator<Feature> iter = allFeatures.features();
                while (iter.hasNext()) {
                    Feature feature = iter.next();
                    if (feature.getIdentifier().getID().equals(featureID)) {
                        selectedFeatures.add(feature);
                        IDs.add(feature.getIdentifier());
                        break;
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return selectedFeatures;
    }

    /**
     * Calculate total river length in polygon (city, region)
     *
     * @param selectedFeatures
     */
    private void calcRiverLengths(SimpleFeature area) throws Exception {

        // Get rivers
        FeatureCollection riverCollection = getRivers();

        if (riverCollection.size() == 0) {
            throw new Exception("No rivers!");
        }

        try {
            SimpleFeatureCollection riverIntersections = FeatureCollections.newCollection();

            Geometry areaGeometry = (Geometry) area.getDefaultGeometry();

            Filter filter = (Filter) ff.intersects(ff.property("the_geom"), ff.literal(areaGeometry));

            FeatureCollection intersectingRivers = (SimpleFeatureCollection) getRiverLayer().getFeatureSource().getFeatures(filter);

            System.out.println("Found intersects: " + intersectingRivers.size());
            //addOutput("Intersecting rivers: " + intersectingRivers.size());

            riverIntersections.addAll(intersectingRivers);

            float totalRiverLength = calcRiverLength(riverIntersections);
            addOutput("River length: " + Float.toString(totalRiverLength));


        } finally {
        }

    }

    /**
     * Get river feature collection
     *
     * @return FeatureCollection
     */
    private FeatureCollection getRivers() throws Exception {
        // Get River layer
        Layer layer = getRiverLayer();

        FeatureCollection rivers = layer.getFeatureSource().getFeatures();

        return rivers;
    }

    protected void addOutput(String msg) {
        textPane.setText(textPane.getText() + "\n" + msg);
    }

    protected Layer getAreaLayer() throws Exception {
        Layer layer = this.gisFrame.getMapContent().layers().get(0);

        if (layer == null) {
            throw new Exception("Area layer not loaded!");
        }

        return layer;
    }

    protected Layer getRiverLayer() throws Exception {
        Layer layer = loadShpFile("LT10shp\\HIDRO_L.shp");

        if (layer == null) {
            throw new Exception("River layer not loaded!");
        }

        return layer;
    }

    protected Layer getStreetLayer() throws Exception {
        Layer layer = loadShpFile("LT10shp\\KELIAI.shp");

        if (layer == null) {
            throw new Exception("Street layer not loaded!");
        }

        return layer;
    }

    protected Layer getHydroLayer() throws Exception {
        Layer layer = loadShpFile("LT10shp\\PLOTAI.shp");

        if (layer == null) {
            throw new Exception("Hydrographical layer not loaded!");
        }

        return layer;
    }

    private void AnalyzeArea(SimpleFeature area) {

        // Title
        addOutput("Analyzing area: " + area.getAttribute("RAJVARDAS"));

        try {
            // Get rivers length in city area
            calcRiverLengths(area);

            // Get road length in city
            calcRoadLength(area);

            // Get hydragraphical area
            calcHydroArea(area);

            // Green area 


            // Area with buildings

            // Pramoniniu sodu masyvu plota kiekviename administraciniame vienete ir 

            // Kiekvieno ju santyki su bendru administracinio vieneto plotu.

        } catch (Exception e) {
            addOutput(e.toString());
        }

        addOutput("\n\n\n");
    }

    /**
     * Calc total passed FeatureCollection (river ) length
     *
     * @param riverIntersections
     * @return
     */
    private float calcRiverLength(SimpleFeatureCollection riverIntersections) {
        FeatureIterator iter = riverIntersections.features();

        float totalLength = 0;

        while (iter.hasNext()) {
            SimpleFeature river = (SimpleFeature) iter.next();

            // Get length attr
            float length = Float.parseFloat(river.getAttribute("SHAPE_len").toString());

            if (length != 0) {
                totalLength += length;
            }
        }

        return totalLength;
    }

    /**
     * Calc total road length in selected area
     *
     * @param area
     * @throws Exception
     */
    private void calcRoadLength(SimpleFeature area) throws Exception {
        try {
            SimpleFeatureCollection roadIntersections = FeatureCollections.newCollection();

            Geometry areaGeometry = (Geometry) area.getDefaultGeometry();

            Filter filter = (Filter) ff.intersects(ff.property("the_geom"), ff.literal(areaGeometry));

            FeatureCollection intersectingRoards = (SimpleFeatureCollection) getStreetLayer().getFeatureSource().getFeatures(filter);

            System.out.println("Found road intersects: " + intersectingRoards.size());
            //addOutput("Intersecting roads: " + intersectingRoards.size());

            roadIntersections.addAll(intersectingRoards);

            float totalRiverLength = calcRoadLength(roadIntersections);
            addOutput("Road length: " + Float.toString(totalRiverLength));


        } finally {
        }

    }

    /**
     * Calc total road length in area
     *
     * @param roadIntersections
     * @return
     */
    private float calcRoadLength(SimpleFeatureCollection roadIntersections) {
        FeatureIterator iter = roadIntersections.features();

        float totalLength = 0;

        while (iter.hasNext()) {
            SimpleFeature river = (SimpleFeature) iter.next();

            // Get length attr
            float length = Float.parseFloat(river.getAttribute("SHAPE_len").toString());

            if (length != 0) {
                totalLength += length;
            }

        }

        return totalLength;
    }

    private void calcHydroArea(SimpleFeature givenArea) throws Exception {
        SimpleFeatureCollection hydroAreaintersections = FeatureCollections.newCollection();

        Geometry areaGeometry = (Geometry) givenArea.getDefaultGeometry();

        Filter filter = (Filter) ff.intersects(ff.property("the_geom"), ff.literal(areaGeometry));

        FeatureCollection intersectingHydroAreas = (SimpleFeatureCollection) getHydroLayer().getFeatureSource().getFeatures(filter);

        System.out.println("Found HydroArea intersections: " + intersectingHydroAreas.size());

        hydroAreaintersections.addAll(intersectingHydroAreas);

        double totalHydroArea = calcHydroAreaSize(hydroAreaintersections);
        
        BigDecimal decimal = new BigDecimal(totalHydroArea);
        
        System.out.println("Decimal: " + decimal.toPlainString());
        
        DecimalFormat formatter = new DecimalFormat("##0.0######");
        System.out.println("Decimal formated: " + formatter.format(totalHydroArea));
        
        addOutput("Total hydrographical area: " + Double.toString(totalHydroArea));

    }

    private double calcHydroAreaSize(SimpleFeatureCollection hydroAreaintersections) {
        FeatureIterator iter = hydroAreaintersections.features();

        double totalLength = 0;

        while (iter.hasNext()) {
            SimpleFeature hydroArea = (SimpleFeature) iter.next();

            // Get area size
            AreaFunction areaFunction = new AreaFunction();

            Geometry hydroGeometry = (Geometry) hydroArea.getDefaultGeometry();

            double length = areaFunction.getArea(hydroGeometry);


            if (length != 0) {
                totalLength += length;
            }

        }

        return totalLength;
    }

    /**
     * Load desired SHP file to memory
     *
     * @param filename
     * @throws IOException
     */
    protected Layer loadShpFile(String filename) throws IOException {

        String hash = hashString(filename);
        
        // A bit caching and do not load layers few times
        if(loadedLayers.containsKey(hash) ){
            return loadedLayers.get(hash);
        }

        File file = new File(path + "\\" + filename);

        if (!file.exists()) {
            throw new FileNotFoundException("File '" + filename + "' not found");
        } else {
            System.out.println("File " + filename + " found! Loading...");
        }

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        layer.setTitle(file.getName());
        
        // Put in cache
        loadedLayers. put(hash, layer);

        return layer;
    }

    /**
     * Hash string to MD5
     *
     * @param string
     * @return
     * @throws NoSuchAlgorithmException
     */
    protected String hashString(String string) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");

            m.update(string.getBytes(), 0, string.length());

            return new BigInteger(1, m.digest()).toString(16);
        } catch (Exception e) {
            return "";
        }
    }
}