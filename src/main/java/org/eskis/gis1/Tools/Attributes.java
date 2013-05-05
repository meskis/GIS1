package org.eskis.gis1.Tools;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.vividsolutions.jts.util.Assert;
import org.geotools.feature.simple.SimpleFeatureBuilder;

public class Attributes {

    public static SimpleFeature addAttribute(SimpleFeature original, String attributeName, Class<?> clazz, Object value) {
        return addAttributes(original, new String[]{attributeName}, new Class<?>[]{clazz}, new Object[]{value});
    }

    public static SimpleFeature addAttributes(SimpleFeature original, String[] attributeNames, Class<?>[] classes, Object[] values) {
        Assert.isTrue(attributeNames.length == classes.length);
        Assert.isTrue(attributeNames.length == values.length);

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        SimpleFeatureType originalType = original.getFeatureType();

        typeBuilder.setCRS(originalType.getCoordinateReferenceSystem());
        typeBuilder.setName(originalType.getName());
        typeBuilder.addAll(originalType.getAttributeDescriptors());

        for (int i = 0; i < attributeNames.length; i++) {
            typeBuilder.add(attributeNames[i], classes[i]);
        }

        SimpleFeatureType type = typeBuilder.buildFeatureType();

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        builder.addAll(original.getAttributes());

        for (int i = 0; i < values.length; i++) {
            builder.add(values[i]);
        }

        SimpleFeature result = builder.buildFeature(original.getID());
        result.setDefaultGeometry(original.getDefaultGeometry());

        return result;
    }
}
