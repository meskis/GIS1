/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.Tools;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JOptionPane;
import org.eskis.gis1.Core.TargetAnalysis;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.GeometryCollector;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

/**
 *
 * @author Marcus
 */
public class AreaAnalyzer {

    TargetAnalysis container;
    protected Layer layer;
    private static final Color DEFAULT_LINE = Color.BLACK;
    private static final Color DEFAULT_FILL = Color.WHITE;
    Rectangle selectionRectangle;
    Rectangle rec;
    Rectangle selectedRect;
    protected Layer cityAreaLayer;
    protected Geometry cityAreaGeometry;
    protected Layer riverAreaLayer;
    protected Geometry riverAreaGeometry;
    protected Layer roadAreaLayer;
    protected Geometry roadAreaGeometry;
    /**
     * Results
     */
    protected FeatureCollection<SimpleFeatureType, SimpleFeature> resultCollection;
    private Geometry searchArea;
    private SimpleFeatureCollection finalSearchCollectio;

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

            constructRiverLayer();
            constructRoadLayer();
            calcFinalArea();
//
            searchArea();
            
            constructReljef();

            JOptionPane.showMessageDialog(null, "Analysis done.");


        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Klaida analizuojant: " + e.toString());
            JOptionPane.showMessageDialog(null, e.getStackTrace());
        }
    }

    protected void constructCities() throws IOException {
        // Filter cities
        Layer cityLayer = this.container.getCitylayer();

        SimpleFeatureCollection selectedFeatures =  this.container.map.selectFeatures(selectedRect, this.container.getCitylayer(), "City Area");
        
        SimpleFeatureCollection cityCollection = FeatureCollections.newCollection();
        cityCollection.addAll(selectedFeatures);

        // Bufferize
        DefaultFeatureCollection bufferizedCities = new DefaultFeatureCollection(new BufferedFeatureCollection(cityCollection, "BufferedCi", container.getCityDistance()));

        // Join geometries
        Geometry cityFeatures = gis.ConversionUtils.getGeometries(bufferizedCities);
        Geometry oneBigCity = cityFeatures.buffer(0); // sujungia i viena

        cityAreaGeometry = oneBigCity;

        SimpleFeatureCollection gyvenam = gis.ConversionUtils.geometryToFeatures(oneBigCity, "BigCityArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.CYAN, 1);
        Layer layer = new FeatureLayer(gyvenam, style, "CityArea");
        layer.setVisible(false);
        this.container.map.getMapContent().addLayer(layer);
        cityAreaLayer = layer;
    }

    protected void constructRiverLayer() throws IOException {

        // 50 meters
        double riverWidth = 50;

        /**
         * River Buffer
         */
        Layer riverLayer = container.getRiverLayer();

        SimpleFeatureCollection selectedFeatures =  this.container.map.selectFeatures(selectedRect, this.container.getRiverLayer(), "City Area");
        
        SimpleFeatureCollection riverCollection = FeatureCollections.newCollection();
        riverCollection.addAll(selectedFeatures);

        // Bufferize
        DefaultFeatureCollection bufferizedRivers = new DefaultFeatureCollection(new BufferedFeatureCollection(riverCollection, "BufferedRiver", riverWidth));

        // Join geometries
        Geometry riverFeatures = gis.ConversionUtils.getGeometries(bufferizedRivers);
        Geometry riverArea = riverFeatures.buffer(0); // sujungia i viena

        /**
         * Second river buffer for final area
         */
        Geometry riverFullArea = riverArea.buffer(container.getRiverDistance());

        Geometry riverFinalArea = riverFullArea.difference(riverArea);
        riverAreaGeometry = riverFinalArea;

        // Add layer
        SimpleFeatureCollection singleColelctionItem = gis.ConversionUtils.geometryToFeatures(riverFinalArea, "BigRiverArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.BLUE, 1);
        Layer layer = new FeatureLayer(singleColelctionItem, style, "RiverArea");
        layer.setVisible(false);
        this.container.map.getMapContent().addLayer(layer);
        riverAreaLayer = layer;
    }

    protected void constructRoadLayer() throws IOException {

        double roadWidth = 20;

        Layer roadLayer = container.getRoadLayer();

        SimpleFeatureCollection selectedFeatures =  this.container.map.selectFeatures(selectedRect, this.container.getRoadLayer(), "Road Area");
        
        SimpleFeatureCollection roadCollection = FeatureCollections.newCollection();
        roadCollection.addAll(selectedFeatures);

        // Bufferize
        DefaultFeatureCollection bufferizedRoads = new DefaultFeatureCollection(new BufferedFeatureCollection(roadCollection, "BufferedRoads", roadWidth));

        // Join geometries
        Geometry roadFeatures = gis.ConversionUtils.getGeometries(bufferizedRoads);
        Geometry roadArea = roadFeatures.buffer(0); // sujungia i viena

        /**
         * Construct available road buffer
         */
        Geometry roadFullArea = roadArea.buffer(container.getRoadDistance());

        Geometry roadFinalArea = roadFullArea.difference(roadArea);

        roadAreaGeometry = roadFinalArea;

        // Add layer
        SimpleFeatureCollection singleCollectionItem = gis.ConversionUtils.geometryToFeatures(roadFinalArea, "BigRoadArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.BLUE, 1);
        Layer layer = new FeatureLayer(singleCollectionItem, style, "RoadArea");
        layer.setVisible(false);
        this.container.map.getMapContent().addLayer(layer);
        roadAreaLayer = layer;
    }
    
    protected void constructReljef(){
        
       SimpleFeatureCollection selectedFeatures =  this.container.map.selectFeatures(selectedRect, this.container.getReljefLayer(), "City Area");
        
       SimpleFeatureCollection reljefCollection = FeatureCollections.newCollection();
       reljefCollection.addAll(selectedFeatures);
        
    }

    protected void prepare() {
        selectionRectangle = container.map.getSelectedRectangle();
        double minX = container.map.startPosWorld.getX();
        double minY = container.map.startPosWorld.getY();
        
        double maxX = container.map.endPosWorld.getX();
        double maxY = container.map.endPosWorld.getY();
        
        double diff = maxX - minX;
        double diffCoord = selectionRectangle.getMaxX() - selectionRectangle.getMinX();
        double koef = diff / diffCoord;
        double n = 10 / koef;// atstumas max iki gyvenvietes
        int m = (int) n;
        System.out.println("Koorinates: minX " + minX + " maxX " + maxX
                + " Skirtumas: " + diff + " SkirtumasK: " + diffCoord
                + " koef: " + koef + " N: " + n + " M: " + m);

        int a = (int) selectionRectangle.getMaxX() + m;
        int b = (int) selectionRectangle.getMaxY() + m;
        int c = (int) selectionRectangle.getMinX() - m;
        int d = (int) selectionRectangle.getMinY() - m;

        rec = new Rectangle(50, 50, 50, 50);
        selectedRect = new Rectangle(selectionRectangle);
        // System.out.println("maxX "+selectionRectangle.getMaxX()+" maxY "+selectionRectangle.getMaxY()+" minX "+selectionRectangle.getMinX()+" minY "+selectionRectangle.getMinY()+" centrX "+selectionRectangle.getCenterX()+" centrY "+selectionRectangle.getCenterY());
        System.out.println("Turimas Rect objektas: "+selectedRect);
        System.out.println(rec);

        System.out.println("maxX " + selectionRectangle.getMaxX() + " maxY "
                + selectionRectangle.getMaxY() + " minX "
                + selectionRectangle.getMinX() + " minY "
                + selectionRectangle.getMinY() + " centrX "
                + selectionRectangle.getCenterX() + " centrY "
                + selectionRectangle.getCenterY());
    }

//    private void addNewLayer(FeatureCollection fc, String name) {
//        Layer newLayer = new FeatureLayer(fc, SLD.createSimpleStyle(fc.getSchema()), name);
//        MemoryDataStore mds = new MemoryDataStore(fc);
//        this.container.map.getMapContent().addLayer(newLayer);
//    }

    public Rectangle getSelectionRectangle() {
        double minX = container.map.startPosWorld.getX();
        double minY = container.map.startPosWorld.getY();
        double maxX = container.map.endPosWorld.getX();
        double maxY = container.map.endPosWorld.getY();

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

    /**
     * Area calc logic
     */
    private void calcFinalArea() {
        searchArea = cityAreaGeometry.intersection(riverAreaGeometry).intersection(roadAreaGeometry);

        // Display final area
        finalSearchCollectio = gis.ConversionUtils.geometryToFeatures(searchArea, "FinalArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.RED, 1);
        Layer layer = new FeatureLayer(finalSearchCollectio, style, "Final");
        layer.setVisible(true);
        this.container.map.getMapContent().addLayer(layer);
    }

    /**
     * Search area in final territory
     */
    private void searchArea() {
        
        // pridedam pavirsiu ir reljefa
        //DefaultFeatureCollection kPlotaiSuPavirsium = new DefaultFeatureCollection(new IntersectedFeatureCollection(kPlotaiBeMisku, pavirsius));
		// DefaultFeatureCollection kPlotaiSuReljefu = new
       // SimpleFeatureCollection kPlotaiBeMisku = gis.ConversionUtils.geometryToFeatures(gPlotaiBeMisku, "obj");
//
//		/*
//		 * Style stylePM = SLD.createPolygonStyle(DEFAULT_LINE, Color.ORANGE,
//		 * 1); Layer layerPM = new FeatureLayer(kPlotaiBeMisku, stylePM);
//		 * this.getMapContent().addLayer(layerPM);
//		 */
//
//		// Tinkami plotai su pavirsium ir reljefu
		// Geometry gPavirsius = ConversionUtils.getGeometries(pavirsius);
		// Geometry bPavirsiusBuferis = gPavirsius.buffer(0);//viena geometrija
//
//		DefaultFeatureCollection kPlotaiSuPavirsium = new DefaultFeatureCollection(
//				new gis.IntersectedFeatureCollection(kPlotaiBeMisku, pavirsius));
//		// DefaultFeatureCollection kPlotaiSuReljefu = new
//		// DefaultFeatureCollection(new
//		// IntersectedFeatureCollection(kPlotaiSuPavirsium,reljefas));
//		Style styleRM = SLD.createPolygonStyle(DEFAULT_LINE, Color.GREEN, 1);
//		Layer layerRM = new FeatureLayer(kPlotaiBeMisku, styleRM);
//		layerRM.setVisible(false);
//		this.getMapContent().addLayer(layerRM);
//
//		// Ipaisomas sklypas
//
//		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
//		// typeBuilder.setCRS(CRS.decode("EPSG:3346"));
//		typeBuilder.setCRS(kPlotaiSuPavirsium.getSchema()
//				.getCoordinateReferenceSystem());
//		// typeBuilder.setName(name);
//		typeBuilder.setName("Sklypas");
//
//		AttributeTypeBuilder builderA = new AttributeTypeBuilder();
//		builderA.setBinding(MultiPolygon.class);
//		AttributeDescriptor attributeDescriptor = builderA.buildDescriptor(
//				"geom", builderA.buildType());
//		typeBuilder.add(attributeDescriptor);
//		// typeBuilder.add("geom", Polygon.class, 3346);
//		typeBuilder.add("atstumas", String.class);
//		typeBuilder.setDefaultGeometry("geom");
//		SimpleFeatureType type = typeBuilder.buildFeatureType();
//
//		SimpleFeatureIterator i = kPlotaiSuPavirsium.features();
//
//		Collection<Geometry> geometrijos = new ArrayList<Geometry>();
//
//		SimpleFeatureCollection featurai = FeatureCollections.newCollection();
//		//int siz = featurai.size();
//		int id = 1;
//		while (i.hasNext()) {
//			SimpleFeature feature = i.next();
//			Geometry fGeometry = (Geometry) feature.getDefaultGeometry();
//			Geometry bbox = fGeometry.getEnvelope();
//			double miX = bbox.getEnvelopeInternal().getMinX();
//			double maX = bbox.getEnvelopeInternal().getMaxX();
//			double miY = bbox.getEnvelopeInternal().getMinY();
//			double maY = bbox.getEnvelopeInternal().getMaxY();
//			double iksai = maX - miX;
//			double ygrekai = maY - miY;
//			double krastas = Double.parseDouble(textField_4.getText());
//			int krastine = (int) krastas;
//			// jei maziau nei sklypo dydis nei nedet
//			if ((iksai < krastas) || (ygrekai < krastas)) {
//				continue;
//			} else {
//				int j = (int) iksai;
//				int k = (int) ygrekai;
//				boolean ardidint = true;
//				for (int l = (int) miX; l < (int) maX - krastine; l++) {
//				boolean keisti = false;
//					for (int l2 = (int) miY; l2 < (int) maY - krastine; l2++) {
//						Geometry sklypas = sklypas(l, l2, krastas);
//						if (!geometrijos.isEmpty()) {
//							Iterator<Geometry> ijk = geometrijos.iterator();
//							boolean breaking = false;
//							while (ijk.hasNext()) {
//								Geometry dgd = ijk.next();
//								if (dgd.overlaps(sklypas)) {
//									breaking = true;
//									break;
//								}
//
//							}
//							if (breaking) {
//								continue;
//							}
//						}
//						if (sklypas.coveredBy(fGeometry)) {
//							geometrijos.add(sklypas);
//							SimpleFeatureBuilder builder = new SimpleFeatureBuilder(
//									type);
//							builder.set("geom", sklypas);
//							builder.add(id);
//							builder.set("atstumas", fGeometry.getBoundary()
//									.distance(sklypas));
//							SimpleFeature resultFeature = builder
//									.buildFeature(String.valueOf(id));
//							resultFeature.setDefaultGeometry(sklypas);
//							featurai.add(resultFeature);
//							id++;
//							l2 += krastine;
//							ardidint = true;
//						}
////						if (ardidint) {
////							keisti = true;
////							break;
////						}
//						
//					}
////					if(keisti){
////						break;
////					}
//
//				}
//			}
//			if (id > 20){
//				i.close();
//				break;
//			}
//			System.out.println("Sekantis, jau yra: " + id);
//			
//		}
//		
//		//DefaultFeatureCollection kolekcija = new DefaultFeatureCollection(new IntersectedFeatureCollection(featurai,pavirsius));
//		DefaultFeatureCollection kolekcija = new DefaultFeatureCollection(new gis.IntersectedFeatureCollection(featurai,upesMax));
//		Style styleG = SLD.createPolygonStyle(DEFAULT_LINE, Color.RED, 1);
//		Layer layerG = new FeatureLayer(kolekcija, styleG);
//		layerG.setTitle("Sklypai_upiu.");
//		this.getMapContent().addLayer(layerG);
//
//		
//		DefaultFeatureCollection kolekcijs = new DefaultFeatureCollection(new gis.IntersectedFeatureCollection(featurai,ezeraiMax));
//		Style styleG2 = SLD.createPolygonStyle(DEFAULT_LINE, Color.RED, 1);
//		Layer layerG2 = new FeatureLayer(kolekcijs, styleG2);
//		layerG2.setTitle("Sklypai_ezeru");
//		this.getMapContent().addLayer(layerG2);
    }
}