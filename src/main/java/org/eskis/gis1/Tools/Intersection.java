/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.Tools;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.geometry.BoundingBox;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class Intersection {
    
        public static SimpleFeatureCollection intersection(SimpleFeatureCollection first, SimpleFeatureCollection second, String name) {
            
            SimpleFeatureCollection features = null;
            
            try {
            
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            
            typeBuilder.setCRS(first.getSchema().getCoordinateReferenceSystem());
            
            typeBuilder.setName(first.getSchema().getName());
            typeBuilder.add("geom", Polygon.class, 3346);
            typeBuilder.add(getShortName(first.getSchema().getName()) + "_id", String.class);
            typeBuilder.addAll(getNamedAttributeDescriptors(first.getSchema()));
            
            typeBuilder.add(getShortName(second.getSchema().getName()) + "_id", String.class);
            
            typeBuilder.addAll(getNamedAttributeDescriptors(second.getSchema()));

            SimpleFeatureType type = typeBuilder.buildFeatureType();

            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            features = FeatureCollections.newCollection();

            int id = 1;

            FeatureIterator<SimpleFeature> firstIterator = first.features();
        
            while (firstIterator.hasNext()) {
                SimpleFeature firstFeature = firstIterator.next();

                FeatureIterator<SimpleFeature> secondIterator = second.features();
                while (secondIterator.hasNext()) {
                    SimpleFeature secondFeature = secondIterator.next();

                    Geometry shared = ((Geometry) firstFeature.getDefaultGeometry()).intersection((Geometry) secondFeature.getDefaultGeometry());

                    if (!shared.isEmpty()) {
                        builder.add(shared);
                        builder.add(firstFeature.getID());
                        builder.addAll(firstFeature.getAttributes());
                        builder.add(secondFeature.getID());
                        builder.addAll(secondFeature.getAttributes());

                        SimpleFeature resultFeature = builder.buildFeature(Integer.toString(id));

                        resultFeature.setDefaultGeometry(shared);
                        features.add(resultFeature);
                        id++;
                    }
                }
            }
            }
            catch (Exception e) {
                System.out.println("Error: " + e);
            }
        return features;
    }
    
    private static boolean boundingBoxesOverlap(SimpleFeature first, SimpleFeature second) {
        BoundingBox firstBox = first.getBounds();
        BoundingBox secondBox = second.getBounds();

        if (firstBox.getMaxX() < secondBox.getMinX())
            return false;
        if (firstBox.getMinX() > secondBox.getMaxX())
            return false;
        if (firstBox.getMaxY() < secondBox.getMinY())
            return false;
        if (firstBox.getMinY() > secondBox.getMaxY())
            return false;
        return true;
    }
        
    protected static List<AttributeDescriptor> getNamedAttributeDescriptors(SimpleFeatureType schema) {
        List<AttributeDescriptor> result = new ArrayList<AttributeDescriptor>();

        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            Name name = new NameImpl(getShortName(schema.getName()) + "_" + descriptor.getName());
            result.add(new AttributeDescriptorImpl(descriptor.getType(), name, descriptor.getMinOccurs(), descriptor.getMaxOccurs(), descriptor.isNillable(), descriptor
                    .getDefaultValue()));
        }

        return result;
    }
    
    protected static String getShortName(Name schemaName) {
        String[] temp = schemaName.toString().split(":");
        return temp[temp.length - 1];
    }
    
    public static Filter idFilter(SimpleFeatureCollection features, String id) throws CQLException {
        return CQL.toFilter(features.getSchema().getName() + "_id = " + id);
    }
    
}
