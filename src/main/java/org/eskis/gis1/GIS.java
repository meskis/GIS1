package org.eskis.gis1;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.eskis.gis1.Core.AreaSelectorBtn;
import org.eskis.gis1.Core.PerformAnalysisBtn;
import org.eskis.gis1.Core.TargetAnalysis;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.*;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;

public class GIS extends JMapFrame implements ActionListener {

    public MapContent map = new MapContent();
    public JMapFrame mapFrame;
    
    private static final Color DEFAULT_LINE = Color.BLACK;
    private static final Color DEFAULT_FILL = Color.WHITE;
    private static final Color LINE_COLOUR = Color.BLUE;
    private static final Color FILL_COLOUR = Color.CYAN;
    private static final Color SELECTED_COLOUR = Color.RED;
    private static final float OPACITY = 1.0f;
    private static final float LINE_WIDTH = 1.0f;
    private static final float POINT_SIZE = 3.5f;
    private String geometryAttributeName;
    private GeometryType geometryType;
    private JMenuBar menubar = new JMenuBar();
    private JToolBar toolbar;

    private JPanel infoPanel = new JPanel(new BorderLayout());
    private JPanel searchPanel = new JPanel();
    private JTable infoTable = new JTable();
    private JTextField searchField = new JTextField(50);
    private JButton search = new JButton("Search");
    private JButton displayOnMap = new JButton("Display info");
    private JScrollPane scrollPane = new JScrollPane(infoTable);
    private StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    private SimpleFeatureCollection selectedFeatures = FeatureCollections.newCollection();
    private Layer lastSelected = null;
    
    protected JButton analyzeButton = new JButton("Analyze cities");
    private Rectangle selectedRecangle;

    /**
     * Construct
     */
    GIS() {
        map.setTitle("GIS uzduotis 1");

        this.getContentPane().setLayout(new BorderLayout());
        this.enableToolBar(true);

        this.enableLayerTable(true);
        this.enableStatusBar(true);
        toolbar = getToolBar();
        toolbar.addSeparator();
        toolbar.add(new JButton(new AddLayerButton()));
        toolbar.add(new JButton(new SelecItemstButton()));
        toolbar.add(new JButton(new DeSelecItemstButton()));
        toolbar.add(new JButton(new ZoomToSelect()));
        toolbar.add(new JButton(new AreaSelectorBtn(this)));
        toolbar.add(new JButton(new AutoloadBtn(this)));
        toolbar.add(new JButton(new PerformAnalysisBtn(this)));

        searchField.setText("include");
        searchPanel.add(searchField);
        searchPanel.add(search);
        displayOnMap.setEnabled(false);
        searchPanel.add(displayOnMap);
        searchPanel.add(this.analyzeButton);

        search.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                GIS.this.executeQuery(GIS.this.searchField.getText());
                GIS.this.displayOnMap.setEnabled(true);
            }
        });


        infoPanel.add(searchPanel, BorderLayout.NORTH);
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        infoTable.setModel(new DefaultTableModel(0, 0));
        infoTable.setPreferredScrollableViewportSize(new Dimension(500, 200));
        infoPanel.add(scrollPane, BorderLayout.CENTER);
        this.getContentPane().add(infoPanel, BorderLayout.SOUTH);

        this.pack();
        this.setMapContent(map);
        this.setSize(800, 600);
        this.setMinimumSize(new Dimension(850, 700));
        this.setMaximumSize(new Dimension(1366, 768));
        this.setLocation(250, 50);
        this.setVisible(true);
        
        displayOnMap.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (GIS.this.lastSelected == null) {
                    GIS.this.lastSelected = GIS.this.getSelectedLayer();
                }
                if (GIS.this.lastSelected == null) {
                    return;
                }
                //if(!Map.this.lastSelected.equals(Map.this.getSelectedLayer())){
                GIS.this.deSelect();
                //}

                GIS.this.setGeometry(GIS.this.getSelectedLayer().getFeatureSource().getSchema().getGeometryDescriptor());
                int[] selectedRows = GIS.this.infoTable.getSelectedRows();
                if (selectedRows.length == 0) {
                    GIS.this.infoTable.selectAll();
                    selectedRows = GIS.this.infoTable.getSelectedRows();
                }
                Set<FeatureId> IDs = new HashSet<FeatureId>();
                FeatureCollection selectedFeatures = new ListFeatureCollection(
                        (SimpleFeatureType) GIS.this.getSelectedLayer().getFeatureSource().getSchema());
                for (int i = 0; i < selectedRows.length; i++) {
                    String featureID = (String) GIS.this.infoTable.getValueAt(
                            selectedRows[i], 0);

                    try {
                        FeatureCollection allFeatures = GIS.this.getSelectedLayer().getFeatureSource().getFeatures();
                        FeatureIterator<Feature> iter = allFeatures.features();
                        while (iter.hasNext()) {
                            Feature feature = iter.next();
                            if (feature.getIdentifier().getID().equals(featureID)) {
                                selectedFeatures.add(feature);
                                IDs.add(feature.getIdentifier());
                                break;
                            }
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                GIS.this.selectedFeatures.addAll(selectedFeatures);
                GIS.this.displaySelectedFeatures(IDs);
                GIS.this.repaint();
            }
        });
        
        
        this.addActionListeners();
    }

    private void executeQuery(String queryText) {
        Layer selectedLayer = this.getSelectedLayer();
        if (selectedLayer == null) {
            JOptionPane.showMessageDialog(this, "Please select a layer, before searching.");
            return;
        }
        if(queryText.isEmpty() ){
            JOptionPane.showMessageDialog(this, "Enter query!");
            return;
        }
        SimpleFeatureSource source = (SimpleFeatureSource) selectedLayer.getFeatureSource();
        FeatureType schema = source.getSchema();

        String name = schema.getGeometryDescriptor().getLocalName();

        try {
            Filter filter = CQL.toFilter(queryText);

            Query query = new Query(schema.getName().getLocalPart(), filter,
                    new String[]{name});

            SimpleFeatureCollection features = source.getFeatures(filter);

            FeatureCollectionTableModel model = new FeatureCollectionTableModel(
                    features);
            infoTable.setModel(model);
        } catch (Exception e) {
              JOptionPane.showMessageDialog(this.infoPanel, e.getMessage().toString(), "Warning",         JOptionPane.WARNING_MESSAGE);
            System.out.println("error:");
            System.out.println(e.toString());
        }
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Attach event listeners to graphic elements
     */
    private void addActionListeners() {
        this.analyzeButton.addActionListener(new org.eskis.gis1.events.AnalyzeAction(this));
    }

    /**
     * Get selected items
     */
    public Layer getSelected() 
    {
        // Selected layer
        if(this.lastSelected == null){
            
            this.lastSelected = getSelectedLayer();
            
            // layer checking
            if(this.lastSelected == null){
                // No layer
                JOptionPane.showMessageDialog(this.infoPanel, "No layer selected/loaded", "Warning", JOptionPane.WARNING_MESSAGE);
                return null;
            }
        }
        return this.lastSelected;
    }

    public void setSelectedRectangle(Rectangle rectangle) {
        this.selectedRecangle = rectangle;
    }

    public void performAnalysis() 
    {
        TargetAnalysis analyser =  new TargetAnalysis(this);
        
        analyser.perform();
        
    }

    public class LayersList extends JList {

        LayersList(String[] layers) {
            super(layers);
        }
    }

    public class AddLayerButton extends SafeAction {

        AddLayerButton() {
            super("Add layer");
        }

        @Override
        public void action(ActionEvent ae) throws Throwable {
            File file = JFileDataStoreChooser.showOpenFile("shp", null);
            if (file == null) {
                return;
            }
            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();
            Style style = SLD.createSimpleStyle(featureSource.getSchema());
            Layer layer = new FeatureLayer(featureSource, style);
            layer.setTitle(file.getName());
            map.addLayer(layer);
        }
    }

    public class SelecItemstButton extends SafeAction {

        SelecItemstButton() {
            super("Select items");
        }

        @Override
        public void action(ActionEvent ae) throws Throwable {
            GIS.this.getMapPane().setCursorTool(new Selector(GIS.this));
        }
    }

    public class DeSelecItemstButton extends SafeAction {

        DeSelecItemstButton() {
            super("DESELECT items");
        }

        @Override
        public void action(ActionEvent ae) throws Throwable {
            GIS.this.deSelect();
        }
    }

    public class ZoomToSelect extends SafeAction {

        ZoomToSelect() {
            super("ZOOM to selection (+)");
        }

        @Override
        public void action(ActionEvent ae) throws Throwable {
            GIS.this.zoomToSelect();
        }
    }

    public class ChangeLayerVisibility extends SafeAction {

        ChangeLayerVisibility() {
            super("Toggle layer visibility (on/off)");
        }

        @Override
        public void action(ActionEvent ae) throws Throwable {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private void zoomToSelect() {
        FeatureIterator iter = selectedFeatures.features();
        double leftX = -1;
        double leftY = -1;
        double rightX = -1;
        double rightY = -1;
        while (iter.hasNext()) {
            BoundingBox box = iter.next().getBounds();
            rightX = (rightX == -1 || box.getMaxX() > rightX) ? box.getMaxX()
                    : rightX;
            rightY = (rightY == -1 || box.getMaxY() > rightY) ? box.getMaxY()
                    : rightY;
            leftX = (leftX == -1 || box.getMinX() < leftX) ? box.getMinX()
                    : leftX;
            leftY = (leftY == -1 || box.getMinY() < leftY) ? box.getMinY()
                    : leftY;
        }
        if (rightX == -1 || leftX == -1 || rightY == -1 || leftY == -1) {
            return;
        } else if (rightX == leftX) {
            if (rightX == 0) {
                rightX += 10;
            } else {
                leftX -= 10;
                leftX = (leftX < 0) ? 0 : leftX;
            }
        } else if (rightY == leftY) {
            if (rightY == 0) {
                rightY += 10;
            } else {
                leftY -= 10;
                leftY = (leftY < 0) ? 0 : leftY;
            }
        }
        Envelope2D envelope = new Envelope2D();
        envelope.setFrameFromDiagonal(leftX, leftY, rightX, rightY);
        getMapPane().setDisplayArea(envelope);
    }

    protected Layer getSelectedLayer() {
        List<Layer> layers = this.getMapContent().layers();
        for (Layer element : layers) {
            if (element.isSelected()) {
                System.out.println("Selected layer: " + element.getTitle());
                return element;
            }
        }
        return null;
    }

    /**
     * Retrieve information about the feature geometry
     */
    private void setGeometry(GeometryDescriptor geoD) {
        GeometryDescriptor geomDesc = geoD;
        geometryAttributeName = geomDesc.getLocalName();

        Class<?> clazz = geomDesc.getType().getBinding();

        if (Polygon.class.isAssignableFrom(clazz)
                || MultiPolygon.class.isAssignableFrom(clazz)) {
            geometryType = GeometryType.POLYGON;

        } else if (LineString.class.isAssignableFrom(clazz)
                || MultiLineString.class.isAssignableFrom(clazz)) {

            geometryType = GeometryType.LINE;

        } else {
            geometryType = GeometryType.POINT;
        }

    }

    private void deSelect() {
        this.selectedFeatures = FeatureCollections.newCollection();

        List<Layer> layers = this.getMapContent().layers();
        for (Layer element : layers) {
            this.setGeometry(element.getFeatureSource().getSchema().getGeometryDescriptor());
            Style style = createDefaultStyle();
            ((FeatureLayer) element).setStyle(style);
        }
        if (!this.lastSelected.equals(this.getSelectedLayer())) {
            infoTable.setModel(new DefaultTableModel(0, 0));
        }
    }

    /**
     * Create a default Style for feature display
     */
    private Style createDefaultStyle() {
        Rule rule = createRule(DEFAULT_LINE, DEFAULT_FILL);

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    /**
     * Helper for createXXXStyle methods. Creates a new Rule containing a
     * Symbolizer tailored to the geometry type of the features that we are
     * displaying.
     */
    private Rule createRule(Color outlineColor, Color fillColor) {
        Symbolizer symbolizer = null;
        Fill fill = null;
        Stroke stroke = sf.createStroke(ff.literal(outlineColor),
                ff.literal(LINE_WIDTH));

        switch (geometryType) {
            case POLYGON:
                fill = sf.createFill(ff.literal(fillColor), ff.literal(OPACITY));
                symbolizer = sf.createPolygonSymbolizer(stroke, fill,
                        geometryAttributeName);
                break;

            case LINE:
                symbolizer = sf.createLineSymbolizer(stroke, geometryAttributeName);
                break;

            case POINT:
                fill = sf.createFill(ff.literal(fillColor), ff.literal(OPACITY));

                Mark mark = sf.getCircleMark();
                mark.setFill(fill);
                mark.setStroke(stroke);

                Graphic graphic = sf.createDefaultGraphic();
                graphic.graphicalSymbols().clear();
                graphic.graphicalSymbols().add(mark);
                graphic.setSize(ff.literal(POINT_SIZE));

                symbolizer = sf.createPointSymbolizer(graphic,
                        geometryAttributeName);
        }

        Rule rule = sf.createRule();
        rule.symbolizers().add(symbolizer);
        return rule;
    }

    private Style createSelectedStyle(Set<FeatureId> IDs) {
        Rule selectedRule = createRule(SELECTED_COLOUR, SELECTED_COLOUR);
        selectedRule.setFilter(ff.id(IDs));

        Rule otherRule = createRule(DEFAULT_LINE, DEFAULT_FILL);
        otherRule.setElseFilter(true);

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        fts.rules().add(selectedRule);
        fts.rules().add(otherRule);

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    public void selectFeatures(Rectangle rectangle) {
        // System.out.println("Rectangle: " + rectangle.getX() + ":"
        // + rectangle.getY());
        Layer layer = this.getSelectedLayer();
        if (layer == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a layer, to select from.");
            return;
        } else if (this.lastSelected != null) {
            if (!this.lastSelected.equals(layer)) {
                this.deSelect();
            }
        }
        lastSelected = layer;

        
        this.setGeometry(layer.getFeatureSource().getSchema().getGeometryDescriptor());
        AffineTransform screenToWorld = this.getMapPane().getScreenToWorldTransform();
        Rectangle2D worldRect = screenToWorld.createTransformedShape(rectangle).getBounds2D();
        // System.out.println(worldRect);
        ReferencedEnvelope bbox = new ReferencedEnvelope(worldRect, this.getMapContent().getCoordinateReferenceSystem());
     
        Filter filter = ff.intersects(ff.property(geometryAttributeName),
                ff.literal(bbox));

        
        try {
            SimpleFeatureCollection selectedFeatures = (SimpleFeatureCollection) layer.getFeatureSource().getFeatures(filter);
            this.selectedFeatures.addAll(selectedFeatures);

            FeatureCollectionTableModel tableModel = new FeatureCollectionTableModel(
                    this.selectedFeatures);
            this.infoTable.setModel(tableModel);
            FeatureIterator iter = this.selectedFeatures.features();
            Set<FeatureId> IDs = new HashSet<FeatureId>();
            try {
                while (iter.hasNext()) {
                    Feature feature = iter.next();
                    IDs.add(feature.getIdentifier());

                    // System.out.println("   " + feature.getIdentifier());
                }

            } finally {
                iter.close();
            }

            if (IDs.isEmpty()) {
                // System.out.println("   no feature selected");
            }

            displaySelectedFeatures(IDs);

        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

    }

    private void displaySelectedFeatures(Set<FeatureId> IDs) {
        Style style;

        if (IDs.isEmpty()) {
            style = createDefaultStyle();

        } else {
            style = createSelectedStyle(IDs);
        }

        ((FeatureLayer) lastSelected).setStyle(style);
        this.repaint();
    }

    private enum GeometryType {

        POINT, LINE, POLYGON
    };
    
    public JTable getInfoTable()
    {
        return infoTable;
    }
}
