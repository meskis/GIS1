package org.eskis.gis1.Core;

import java.awt.Point;
import java.awt.geom.Point2D;
import org.eskis.gis1.GIS;
import static java.lang.Math.abs;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import java.awt.Rectangle;

public class AreaSelectorTool extends CursorTool {

    private GIS map;
    protected Point startPoint;
    protected Point endPoint;
    protected Rectangle rectangle;
    private final Point startPosDevice;
    private final Point2D startPosWorld;

    public AreaSelectorTool(GIS map) {
        this.map = map;
        
        startPosDevice = new Point();
        startPosWorld = new DirectPosition2D();
    }

    @Override
    public boolean drawDragBox() {
        return true;
    }

    @Override
    public void onMousePressed(MapMouseEvent ev) {
        startPoint = ev.getPoint();
        startPosDevice.setLocation(ev.getPoint());
        startPosWorld.setLocation(ev.getWorldPos());
    }


    @Override
    public void onMouseReleased(MapMouseEvent ev) {
        endPoint = ev.getPoint();

        rectangle = new Rectangle(
                (int) startPosDevice.getX(), 
                (int) startPosDevice.getY(), 
                abs((int) (ev.getX() - startPosDevice.getX())), 
                abs((int) (ev.getY() - startPosDevice.getY()))
                );
        
        map.setSelectedRectangle(rectangle);

        System.out.println(rectangle.toString());
    }

    @Override
    public void onMouseMoved(MapMouseEvent ev) {
        //System.out.println(ev.getPoint().toString());
    }
}
