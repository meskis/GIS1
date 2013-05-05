package org.eskis.gis1.Tools;

import java.util.Iterator;
import java.util.NoSuchElementException;


import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryTypeImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.eskis.gis1.Tools.WrappingIterator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;


/**
 * Wrapper that will trigger the buffer computation as features are requested
 */
class BufferedFeatureCollection extends DecoratingSimpleFeatureCollection {

    double distance;
    String attribute;
    SimpleFeatureType schema;

    public BufferedFeatureCollection(SimpleFeatureCollection delegate, String attribute,
            double distance) {
        super(delegate);
        this.distance = distance;
        this.attribute = attribute;

        // create schema
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor descriptor : delegate.getSchema().getAttributeDescriptors()) {
            if (!(descriptor.getType() instanceof GeometryTypeImpl)
                    || (!delegate.getSchema().getGeometryDescriptor().equals(descriptor))) {
                tb.add(descriptor);
            } else {
                AttributeTypeBuilder builder = new AttributeTypeBuilder();
                builder.setBinding(MultiPolygon.class);
                AttributeDescriptor attributeDescriptor = builder.buildDescriptor(descriptor.getLocalName(), builder.buildType());
                tb.add(attributeDescriptor);
                if (tb.getDefaultGeometry() == null) {
                    tb.setDefaultGeometry(descriptor.getLocalName());
                }
            }
        }
        tb.setDescription(delegate.getSchema().getDescription());
        tb.setCRS(delegate.getSchema().getCoordinateReferenceSystem());
        tb.setName(delegate.getSchema().getName());
        this.schema = tb.buildFeatureType();
    }

    @Override
    public SimpleFeatureIterator features() {
        return new BufferedFeatureIterator(delegate, this.attribute, this.distance, getSchema());
    }

    @Override
    public Iterator<SimpleFeature> iterator() {
        return new WrappingIterator(features());
    }

    @Override
    public void close(Iterator<SimpleFeature> close) {
        if (close instanceof WrappingIterator) {
            ((WrappingIterator) close).close();
        }
    }

    @Override
    public SimpleFeatureType getSchema() {
        return this.schema;
    }
}

/**
 * Buffers each feature as we scroll over the collection
 */
class BufferedFeatureIterator implements SimpleFeatureIterator {

    SimpleFeatureIterator delegate;
    SimpleFeatureCollection collection;
    Double distance;
    String attribute;
    int count;
    SimpleFeatureBuilder fb;
    SimpleFeature next;

    public BufferedFeatureIterator(SimpleFeatureCollection delegate, String attribute,
            Double distance, SimpleFeatureType schema) {
        this.delegate = delegate.features();
        this.distance = distance;
        this.collection = delegate;
        this.attribute = attribute;
        fb = new SimpleFeatureBuilder(schema);
    }

    public void close() {
        delegate.close();
    }

    public boolean hasNext() {
        while (next == null && delegate.hasNext()) {
            SimpleFeature f = delegate.next();
            for (Object value : f.getAttributes()) {
                if (value instanceof Geometry) {
                    Double fDistance = distance;
                    if (this.attribute != null) {
                        //fDistance = Converters.convert(f.getAttribute(this.attribute), Double.class);
                        fDistance = this.distance;
                    }
                    if (fDistance != null && fDistance != 0.0) {
                        value = ((Geometry) value).buffer(fDistance);
                    }
                }
                fb.add(value);
            }
            next = fb.buildFeature("" + count);
            count++;
            fb.reset();
        }
        return next != null;
    }

    public SimpleFeature next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException("hasNext() returned false!");
        }
        SimpleFeature result = next;
        next = null;
        return result;
    }
}
