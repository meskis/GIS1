/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.Core;

import com.vividsolutions.jts.geom.Geometry;
import java.awt.Component;
import java.io.IOException;
import java.util.Iterator;
import org.eskis.gis1.GIS;
import org.eskis.gis1.Tools.AreaAnalyzer;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.Envelope2D;
import org.geotools.map.Layer;
import org.opengis.feature.simple.SimpleFeature;

/**
 *
 * @author Marcus
 */
public class TargetAnalysis 
{
    public GIS map;
    private Component emptyLabel;
    protected GUI frame;
    
    protected SimpleFeatureCollection cities;
    protected SimpleFeatureCollection roads;
    protected SimpleFeatureCollection rivers;

    public TargetAnalysis(GIS map) 
    {
        this.map = map;
    }

    public void perform() 
    {
        // Construct new window
        this.createDiablog();
    }

    private void createDiablog() {
        if(frame == null){
            frame = new GUI(this);
        }
        
        frame.setVisible(true);
    }
    
    public float getRiverDistance()
    {
        return this.frame.getDistanceToRiver();
    }
    
    public float getArea()
    {
        return this.frame.getArea();
    }
    
    /**
     * Distance to city (max)
     * @return 
     */
    public float getCityDistance()
    {
        return this.frame.getDistance();
    }
    
    public float getSlope()
    {
        return this.frame.getSlope();
    }
    
    public float getForests()
    {
        return this.frame.getForests();
    }
    
    public Layer getCitylayer()
    {
        return this.map.getMapContent().layers().get(3);
    }
    
    public Envelope2D getSelectedEnvelope()
    {
        Envelope2D env = new Envelope2D();
        
        return env;
    }
    
    
    /**
     * MAIN analysis logic here...
     * 
     * @throws IOException 
     */
    public void analyse() throws IOException
    {
        frame.setVisible(false);
        
        // TODO Create layer from selected area.. 
        // TODO select only features in selected acrea 9intersecting with selection shape)
        
        // Buferize cisites
        AreaAnalyzer analyzer = new AreaAnalyzer(this);
        analyzer.analyze();
 
    }

    public Layer getRoadLayer() {
        return this.map.getMapContent().layers().get(0);
    }
    
    public Layer getRegionLayer()
    {
        return this.map.getMapContent().layers().get(4);
    }

    public Layer getRiverLayer() {
        return this.map.getMapContent().layers().get(1);
    }

    public double getRoadDistance() {
        return this.frame.getRoadDistance();
    }

    public Layer getReljefLayer() {
        return this.map.getMapContent().layers().get(2);
    }

}