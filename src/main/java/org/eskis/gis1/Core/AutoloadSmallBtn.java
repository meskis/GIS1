/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.Core;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import org.eskis.gis1.GIS;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.action.SafeAction;

/**
 *
 * @author Marcus
 */
public class AutoloadSmallBtn  extends SafeAction {

    protected GIS map;

    public AutoloadSmallBtn(GIS gis) {
        super("Load Sventoji");
        map = gis;
    }
    
    @Override
    public void action(ActionEvent ae) throws Throwable {

        String current = new java.io.File(".").getCanonicalPath();
        System.out.println(current);

        HashSet<String> files = new HashSet();

        files.add("sven_KEL_L.shp");
        files.add("sven_HID_L.shp");
        files.add("sven_PLO_P.shp");

        for (Iterator<String> i = files.iterator(); i.hasNext();) {
            String filename = i.next();

            File file = new File(current + "\\..\\gis_data\\LTsventoji\\" + filename);

            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            Style style = SLD.createSimpleStyle(featureSource.getSchema());
            Layer layer = new FeatureLayer(featureSource, style);
            layer.setTitle(file.getName());
            map.map.addLayer(layer);
        }


        HashSet<String> files2 = new HashSet();

        files2.add("gyvenvie.shp");
        files2.add("rajonai.shp");

        for (Iterator<String> i = files2.iterator(); i.hasNext();) {
            String filename = i.next();

            File file = new File(current + "\\..\\gis_data\\lt200shp\\" + filename);

            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            Style style = SLD.createSimpleStyle(featureSource.getSchema());
            Layer layer = new FeatureLayer(featureSource, style);
            layer.setTitle(file.getName());
            map.map.addLayer(layer);
        }

    }
        
}
