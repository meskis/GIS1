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

            JOptionPane.showMessageDialog(null, "Analysis done.");


        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Klaida analizuojant: " + e.toString());
            JOptionPane.showMessageDialog(null, e.getStackTrace());
        }
    }

    protected void constructCities() throws IOException {
        // Filter cities
        Layer cityLayer = this.container.getCitylayer();

        SimpleFeatureCollection fullCityCollection = (SimpleFeatureCollection) cityLayer.getFeatureSource().getFeatures();

        /*
         * TODO Filter selected features only
         */

        SimpleFeatureCollection cityCollection = FeatureCollections.newCollection();
        cityCollection.addAll(fullCityCollection);

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
        Layer riverLayer = container.getRiverLayer();

        /*
         * TODO Filter selected features only
         */
        SimpleFeatureCollection fullRiverCollection = (SimpleFeatureCollection) riverLayer.getFeatureSource().getFeatures();

        SimpleFeatureCollection riverCollection = FeatureCollections.newCollection();
        riverCollection.addAll(fullRiverCollection);

        // Bufferize
        DefaultFeatureCollection bufferizedRivers = new DefaultFeatureCollection(new BufferedFeatureCollection(riverCollection, "BufferedRiver", container.getRiverDistance()));

        // Join geometries
        Geometry riverFeatures = gis.ConversionUtils.getGeometries(bufferizedRivers);
        Geometry riverArea = riverFeatures.buffer(0); // sujungia i viena
        
        riverAreaGeometry = riverArea;

        // Add layer
        SimpleFeatureCollection singleColelctionItem = gis.ConversionUtils.geometryToFeatures(riverArea, "BigRiverArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.BLUE, 1);
        Layer layer = new FeatureLayer(singleColelctionItem, style, "RiverArea");
        layer.setVisible(false);
        this.container.map.getMapContent().addLayer(layer);
        riverAreaLayer = layer;
    }

    protected void constructRoadLayer() throws IOException {
        Layer roadLayer = container.getRoadLayer();

        /*
         * TODO Filter selected features only
         */
        SimpleFeatureCollection fullRoadCollection = (SimpleFeatureCollection) roadLayer.getFeatureSource().getFeatures();

        SimpleFeatureCollection roadCollection = FeatureCollections.newCollection();
        roadCollection.addAll(fullRoadCollection);

        // Bufferize
        DefaultFeatureCollection bufferizedRoads = new DefaultFeatureCollection(new BufferedFeatureCollection(roadCollection, "BufferedRoads", container.getRoadDistance()));

        // Join geometries
        Geometry roadFeatures = gis.ConversionUtils.getGeometries(bufferizedRoads);
        Geometry roadArea = roadFeatures.buffer(0); // sujungia i viena

        roadAreaGeometry = roadArea;
        
        // Add layer
        SimpleFeatureCollection singleCollectionItem = gis.ConversionUtils.geometryToFeatures(roadArea, "BigRoadArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.BLUE, 1);
        Layer layer = new FeatureLayer(singleCollectionItem, style, "RoadArea");
        layer.setVisible(false);
        this.container.map.getMapContent().addLayer(layer);
        roadAreaLayer = layer;
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

    private void addNewLayer(FeatureCollection fc, String name) {
        Layer newLayer = new FeatureLayer(fc, SLD.createSimpleStyle(fc.getSchema()), name);
        MemoryDataStore mds = new MemoryDataStore(fc);
        this.container.map.getMapContent().addLayer(newLayer);
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

    /**
     * Area calc logic
     */
    private void calcFinalArea() {
        Geometry searchArea = cityAreaGeometry.difference(riverAreaGeometry).difference(roadAreaGeometry);
        
        // Display final area
        SimpleFeatureCollection singleCollectionItem = gis.ConversionUtils.geometryToFeatures(searchArea, "FinalArea");
        Style style = SLD.createPolygonStyle(DEFAULT_LINE, Color.RED, 1);
        Layer layer = new FeatureLayer(singleCollectionItem, style, "Final");
        layer.setVisible(true);
        this.container.map.getMapContent().addLayer(layer);
    }
}