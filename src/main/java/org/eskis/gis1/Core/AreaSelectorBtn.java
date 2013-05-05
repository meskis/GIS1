/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.Core;

import java.awt.event.ActionEvent;
import org.eskis.gis1.GIS;
import org.eskis.gis1.Selector;
import org.geotools.swing.action.SafeAction;

/**
 *
 * @author Marcus
 */
public class AreaSelectorBtn extends SafeAction
{
    protected GIS map;
    
    public AreaSelectorBtn(GIS map)
    {
        super("Mark Area");
        this.map = map;
    }

    @Override
    public void action(ActionEvent ae) throws Throwable {
        this.map.getMapPane().setCursorTool(new AreaSelectorTool(this.map));
    }
    
}
