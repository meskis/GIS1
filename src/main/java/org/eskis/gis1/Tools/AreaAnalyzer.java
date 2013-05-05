/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.Tools;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Map;
import javax.swing.JOptionPane;
import org.eskis.gis1.Core.TargetAnalysis;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.GeometryCollector;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 *
 * @author Marcus
 */
public class AreaAnalyzer {

    TargetAnalysis container;
    protected Layer layer;
    private static final Color DEFAULT_LINE = Color.BLACK;
    private static final Color DEFAULT_FILL = Color.WHITE;
    public Point2D startPosWorld = new DirectPosition2D();
    public Point2D endPosWorld = new DirectPosition2D();
    Rectangle selectionRectangle;
    Rectangle rec;
    Rectangle save;
    /**
     * Results
     */
    protected FeatureCollection<SimpleFeatureType, SimpleFeature> resultCollection;

    public AreaAnalyzer() {
    }

    public AreaAnalyzer(TargetAnalysis aThis) {
        container = aThis;
    }

    public void analyze() throws IOException {

        try {
            // Prepare selection
            prepare();

            constructCities();

            JOptionPane.showMessageDialog(null, "Analysis done.");


        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Klaida analizuojant: " + e.toString());
            JOptionPane.showMessageDialog(null, e.getStackTrace());
        }
    }

    protected void constructCities() throws IOException {
        // Filter cities
        Layer cityLayer = this.container.getCitylayer();

        SimpleFeatureCollection allCityCollection = FeatureCollections.newCollection();

        SimpleFeatureCollection selectedFeatures = this.container.map.selectFeatures(rec, container.getCitylayer(), "Miestai");
        allCityCollection.addAll(selectedFeatures);

        DefaultFeatureCollection gyvenvietesMin = new DefaultFeatureCollection(
                new BufferedFeatureCollection(allCityCollection, "gyvenvietesM", this.container.getCityDistance()));
        
        Geometry vietovesMG = gis.ConversionUtils.getGeometries(gyvenvietesMin);
	Geometry vietMbuferis = vietovesMG.buffer(5000);
        
        //addNewLayer(gyvenvietesMin, "GyvenBuff");
        
        SimpleFeatureCollection gyvenam = gis.ConversionUtils.geometryToFeatures(vietMbuferis, "gyvenM");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.CYAN, 1);
        Layer layer = new FeatureLayer(gyvenam, style);
        layer.setVisible(false);
        this.container.map.getMapContent().addLayer(layer);
    }

    protected void prepare() {
        selectionRectangle = container.map.getSelectedRectangle();
        double minX = startPosWorld.getX();
        double minY = startPosWorld.getY();
        double maxX = endPosWorld.getX();
        double maxY = endPosWorld.getY();
        double diff = maxX - minX;
        double diffCoord = selectionRectangle.getMaxX()
                - selectionRectangle.getMinX();
        double koef = diff / diffCoord;
        double n = 10000 / koef;// atstumas max iki gyvenvietes
        int m = (int) n;
        System.out.println("Koorinates: minX " + minX + " maxX " + maxX
                + " Skirtumas: " + diff + " SkirtumasK: " + diffCoord
                + " koef: " + koef + " N: " + n + " M: " + m);

        int a = (int) selectionRectangle.getMaxX() + m;
        int b = (int) selectionRectangle.getMaxY() + m;
        int c = (int) selectionRectangle.getMinX() - m;
        int d = (int) selectionRectangle.getMinY() - m;

        rec = new Rectangle(c, d, a - c, b - d);
        save = new Rectangle(selectionRectangle);
        // System.out.println("maxX "+selectionRectangle.getMaxX()+" maxY "+selectionRectangle.getMaxY()+" minX "+selectionRectangle.getMinX()+" minY "+selectionRectangle.getMinY()+" centrX "+selectionRectangle.getCenterX()+" centrY "+selectionRectangle.getCenterY());
        System.out.println(save);
        System.out.println(rec);

        System.out.println("maxX " + selectionRectangle.getMaxX() + " maxY "
                + selectionRectangle.getMaxY() + " minX "
                + selectionRectangle.getMinX() + " minY "
                + selectionRectangle.getMinY() + " centrX "
                + selectionRectangle.getCenterX() + " centrY "
                + selectionRectangle.getCenterY());
    }

    private void bufferCities() throws IOException {
        // Buffer cities
        Layer cityLayer = container.getCitylayer();

        SimpleFeatureCollection cityCollection = (SimpleFeatureCollection) cityLayer.getFeatureSource().getFeatures();


        // Buffer cities
        BufferedFeatureCollection bufferedCities = new BufferedFeatureCollection(cityCollection, "attribute", container.getCityDistance());

        Geometry singleCityFeature = joinFeatures(bufferedCities);

        //SimpleFeature singleCityArea = 



        Style style = SLD.createSimpleStyle(bufferedCities.getSchema());
        MapContent map = container.map.getMapContent();
        Layer layer = new FeatureLayer(bufferedCities, style);
        layer.setTitle("Buferizuoti miestai");
        map.addLayer(layer);
    }

    
    
    	private void addNewLayer(FeatureCollection fc, String name) {
		Layer newLayer = new FeatureLayer(fc, SLD.createSimpleStyle(fc
				.getSchema()), name);
		MemoryDataStore mds = new MemoryDataStore(fc);
		this.container.map.getMapContent().addLayer(newLayer);
	}
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private void bufferRivers() throws IOException {
        // Buffer cities
        Layer cityLayer = container.getRiverLayer();

        SimpleFeatureCollection cityCollection = (SimpleFeatureCollection) cityLayer.getFeatureSource().getFeatures();

        // Buffer cities

        BufferedFeatureCollection bufferedCities = new BufferedFeatureCollection(cityCollection, "attribute", container.getRiverDistance());

        Style style = SLD.createSimpleStyle(bufferedCities.getSchema());
        MapContent map = container.map.getMapContent();
        layer = new FeatureLayer(bufferedCities, style);
        layer.setTitle("Buferizuotos upes");
        map.addLayer(layer);
    }

    private void createSearchArea() {
        Point2D startPosWorld = new DirectPosition2D();
        Point2D endPosWorld = new DirectPosition2D();

        double minX = startPosWorld.getX();
        double minY = startPosWorld.getY();
        double maxX = endPosWorld.getX();
        double maxY = endPosWorld.getY();


    }

    private void test() {

        Geometry map_geometries = gis.ConversionUtils.getGeometries(layer);
        Geometry gyvMbuferis = map_geometries.buffer(0); // sujungia i viena
    }

    private void formResult() {

        Style styleG2 = SLD.createPolygonStyle(DEFAULT_LINE, Color.RED, 1);

        Layer layerG2 = new FeatureLayer(resultCollection, SLD.createSimpleStyle(resultCollection.getSchema()), "Results1");

        this.container.map.getMapContent().addLayer(layerG2);
    }

    private void search() {
    }

    private void bufferRoads() throws IOException {
        // Buffer cities
        Layer roadLayer = container.getRoadLayer();

        SimpleFeatureCollection roadCollection = (SimpleFeatureCollection) roadLayer.getFeatureSource().getFeatures();

        //Bufferize
        BufferedFeatureCollection bufferedCities = new BufferedFeatureCollection(roadCollection, "attribute", container.getRoadDistance());

        Style style = SLD.createSimpleStyle(bufferedCities.getSchema());
        MapContent map = container.map.getMapContent();
        layer = new FeatureLayer(bufferedCities, style, "Buferizuoti keliai");
        map.addLayer(layer);
    }

    public Rectangle getSelectionRectangle() {
        double minX = startPosWorld.getX();
        double minY = startPosWorld.getY();
        double maxX = endPosWorld.getX();
        double maxY = endPosWorld.getY();

        double diff = maxX - minX;

        double diffCoord = selectionRectangle.getMaxX()
                - selectionRectangle.getMinX();
        double koef = diff / diffCoord;
        double n = 10000 / koef;
        int m = (int) n;
        System.out.println("Koorinates: minX " + minX + " maxX " + maxX
                + " Skirtumas: " + diff + " SkirtumasK: " + diffCoord
                + " koef: " + koef + " N: " + n + " M: " + m);

        int a = (int) selectionRectangle.getMaxX() + m;
        int b = (int) selectionRectangle.getMaxY() + m;
        int c = (int) selectionRectangle.getMinX() - m;
        int d = (int) selectionRectangle.getMinY() - m;

        Rectangle rec = new Rectangle(c, d, a - c, b - d);
        Rectangle save = new Rectangle(selectionRectangle);

        return rec;
    }

    private void joinRoadsAndRivers() {
    }

    private Geometry joinFeatures(SimpleFeatureCollection collection) {
        Geometry geometry_array = gis.ConversionUtils.getGeometries(collection);
        Geometry result = geometry_array.buffer(0); // sujungia i viena

        return result;
    }
}
