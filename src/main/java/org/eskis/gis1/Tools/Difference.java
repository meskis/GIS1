package org.eskis.gis1.Tools;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.GeometryCollector;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
//import org.springframework.util.Assert;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryCollectionIterator;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;

public class Difference {

    public static SimpleFeatureCollection difference(
            SimpleFeatureCollection first, SimpleFeatureCollection second,
            String name) {
        SimpleFeatureCollection features = null;
        try {

            features = FeatureCollections.newCollection();
            SimpleFeatureIterator firstIterator = first.features();
            while (firstIterator.hasNext()) {
                SimpleFeature firstFeature = firstIterator.next();
                SimpleFeatureIterator secondIterator = second.features();
                while (secondIterator.hasNext()) {
                    SimpleFeature secondFeature = secondIterator.next();
                    if (boundingBoxesOverlap(firstFeature, secondFeature)) {
                        Geometry shared = ((Geometry) firstFeature.getDefaultGeometry()).difference((Geometry) secondFeature.getDefaultGeometry());
                        if (!shared.isEmpty()) {

                            System.out.println("1 - " + firstFeature.getID());

                            System.out.println("2 - " + secondFeature.getID());

                            firstFeature.setDefaultGeometry(shared);
                            features.add(firstFeature);

                        }

                    } else {
                        features.add(firstFeature);
                        String c = firstFeature.getID();
                        String c2 = secondFeature.getID();
                        if (c.equals("456")) {
                            System.out.println("Nieksas " + c + " " + c2);
                        }
                    }

                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        return features;
    }

    protected static List<AttributeDescriptor> getNamedAttributeDescriptors(
            SimpleFeatureType schema) {
        List<AttributeDescriptor> result = new ArrayList<AttributeDescriptor>();

        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            Name name = new NameImpl(getShortName(schema.getName()) + "_"
                    + descriptor.getName());
            // Name name = new NameImpl("_" + descriptor.getName());
            result.add(new AttributeDescriptorImpl(descriptor.getType(), name,
                    descriptor.getMinOccurs(), descriptor.getMaxOccurs(),
                    descriptor.isNillable(), descriptor.getDefaultValue()));
        }

        return result;
    }

    protected static String getShortName(Name schemaName) {
        String[] temp = schemaName.toString().split(":");
        return temp[temp.length - 1];
    }

    private static boolean boundingBoxesOverlap(SimpleFeature first,
            SimpleFeature second) {
        BoundingBox firstBox = first.getBounds();
        BoundingBox secondBox = second.getBounds();

        if (firstBox.getMaxX() < secondBox.getMinX()) {
            return false;
        }
        if (firstBox.getMinX() > secondBox.getMaxX()) {
            return false;
        }
        if (firstBox.getMaxY() < secondBox.getMinY()) {
            return false;
        }
        if (firstBox.getMinY() > secondBox.getMaxY()) {
            return false;
        }
        return true;
    }
}
