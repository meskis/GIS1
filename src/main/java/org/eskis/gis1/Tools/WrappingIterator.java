/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.eskis.gis1.Tools;

import java.util.Iterator;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

/**
 * An iterator wrapping a {@link SimpleFeatureIterator} and exposing its close method
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
class WrappingIterator implements Iterator<SimpleFeature> {
    SimpleFeatureIterator delegate;

    public WrappingIterator(SimpleFeatureIterator delegate) {
        super();
        this.delegate = delegate;
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public SimpleFeature next() {
        return delegate.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        delegate.close();
    }
}