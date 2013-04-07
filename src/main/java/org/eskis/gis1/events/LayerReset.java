/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.events;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.geotools.swing.JMapFrame;

/**
 *
 * @author Marcus
 */
public class LayerReset implements ActionListener
{
    
    protected JMapFrame mapFrame;

    /**
     * Manage event
     * @param e 
     */
    public void actionPerformed(ActionEvent e) {
        mapFrame.repaint();
    }
    
    /**
     * Constructor
     */
    public LayerReset(JMapFrame map)
    {
        this.mapFrame = map;
    }
    
}
