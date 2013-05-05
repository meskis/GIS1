/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.Tools;

import java.io.IOException;
import java.util.ArrayList;

import org.geotools.data.DataUtilities;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryCollectionIterator;
import com.vividsolutions.jts.geom.GeometryFactory;

public class ConversionUtils {

    private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    public static Geometry getGeometries(Layer layer) {
        
        ArrayList<Geometry> geometries = null;
        
        try {
        geometries = new ArrayList<Geometry>();

        FeatureIterator<?> iterator = layer.getFeatureSource().getFeatures().features();
        while (iterator.hasNext()) {
            SimpleFeature feature = (SimpleFeature) iterator.next();

            geometries.add((Geometry) feature.getDefaultGeometry());
        }
        }
        catch (Exception e) {
            System.out.println("Error: " + e);
        }

        return geometryFactory.buildGeometry(geometries);
    }
    
    public static Geometry getGeometries(SimpleFeatureCollection features) {
        
        ArrayList<Geometry> geometries = null;
        
        try {
        geometries = new ArrayList<Geometry>();

        FeatureIterator<?> iterator = features.features();
        while (iterator.hasNext()) {
            SimpleFeature feature = (SimpleFeature) iterator.next();

            geometries.add((Geometry) feature.getDefaultGeometry());
        }
        }
        catch (Exception e) {
            System.out.println("Error: " + e);
        }

        return geometryFactory.buildGeometry(geometries);
    }
    
    

    public static Layer geometryToLayer(Geometry geometry, String name) {
        return geometryToLayer(geometry, name, "Polygon");
    }

    public static Layer geometryToLayer(Geometry geometry, String name, String typeStr)  {
        try {
        return simpleFeatureCollectionToLayer(geometryToFeatures(geometry, name, typeStr), name);
        }
        catch (Exception e) {
            System.out.println("Error: " + e);
            return null;
        }
    }

    public static SimpleFeatureCollection geometryToFeatures(Geometry geometry, String name) {
        return geometryToFeatures(geometry, name, "Polygon");
    }

    public static SimpleFeatureCollection geometryToFeatures(Geometry geometry, String name, String typeStr) {
        SimpleFeatureType type = null;
        try {
            type = DataUtilities.createType(name, "geom:" + typeStr + ":srid=3346,area:double");
        }
        catch (Exception e) {
            System.out.println("Error :" + e);
        }

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        SimpleFeatureCollection features = FeatureCollections.newCollection();

        GeometryCollectionIterator iterator = new GeometryCollectionIterator(geometry);
        int id = 1;
        if (geometry.getNumGeometries() == 0) { // Skip first?
            iterator.next();
        }

        while (iterator.hasNext()) {
            Geometry geom = (Geometry) iterator.next();
            if (geom instanceof GeometryCollection) {
                continue;
            }

            builder.add(geom);
            builder.add(geom.getArea());

            SimpleFeature feature = builder.buildFeature(Integer.toString(id));
            feature.setDefaultGeometry(geom);
            features.add(feature);

            id++;
        }

        return features;
    }

    public static Layer simpleFeatureCollectionToLayer(SimpleFeatureCollection collection, String name)  {
        try {
        if (!collection.isEmpty()) {
            MemoryDataStore store = new MemoryDataStore(collection);

            SimpleFeatureSource featureSource = store.getFeatureSource(collection.getSchema().getName());

            Style style = !featureSource.getFeatures().isEmpty() ? SLD.createSimpleStyle(featureSource.getSchema()) : null;

            Layer layer = new FeatureLayer(featureSource, style);
            layer.setTitle(name);

            return layer;
        } else {
            System.out.println("Skipping empty layer: " + name);
        }
        }
        catch (Exception e) {
            System.out.println("Error: " + e);
        }

        return null;
    }
}
