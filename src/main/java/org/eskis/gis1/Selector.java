package org.eskis.gis1;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import static java.lang.Math.abs;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;

public class Selector extends CursorTool {

    private GIS map;
    private final Point startPosDevice;
    private final Point2D startPosWorld;
    private boolean dragged;

    public Selector(GIS map) {
        this.map = map;
        startPosDevice = new Point();
        startPosWorld = new DirectPosition2D();
        dragged = false;
    }

    @Override
    public void onMouseClicked(MapMouseEvent ev) {

        java.awt.Point screenPos = ev.getPoint();
        Rectangle rectangle = new Rectangle(screenPos.x - 2, screenPos.y - 2,
                5, 5);
        this.map.selectFeatures(rectangle);
    }

    @Override
    public void onMouseDragged(MapMouseEvent ev) {
        dragged = true;
    }

    @Override
    public void onMouseReleased(MapMouseEvent ev) {
        if (dragged && !ev.getPoint().equals(startPosDevice)) {
            Rectangle rectangle = new Rectangle((int) startPosDevice.getX(),
                    (int) startPosDevice.getY(),
                    abs((int) (ev.getX() - startPosDevice.getX())), abs((int) (ev.getY() - startPosDevice.getY())));
            // System.out.println(rectangle.toString());
            dragged = false;
            map.selectFeatures(rectangle);
            dragged = false;
        }
    }

    @Override
    public void onMousePressed(MapMouseEvent ev) {
        startPosDevice.setLocation(ev.getPoint());
        startPosWorld.setLocation(ev.getWorldPos());
    }

    @Override
    public boolean drawDragBox() {
        return true;
    }
}