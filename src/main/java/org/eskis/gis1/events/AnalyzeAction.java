/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eskis.gis1.events;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;
import org.eskis.gis1.Core.Analyzer;
import org.eskis.gis1.GIS;
import org.geotools.map.Layer;
import org.geotools.swing.JMapFrame;

/**
 *
 * @author Marcus
 */
public class AnalyzeAction implements ActionListener {

    /**
     * Map Frame
     */
    protected GIS gisFrame;
    protected JFrame frame;
    /**
     * Result container
     */
    protected JTextPane textPane;

    /**
     * Constructor
     */
    public AnalyzeAction(GIS map) {
        this.gisFrame = map;
    }

    /**
     * Analyze button event
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        Layer layer = gisFrame.getSelected();

        if (layer != null) {
            createResultPanel();

            Analyzer analyzer = new Analyzer(gisFrame);

            analyzer.setOutput(this.textPane);

            try {
                analyzer.analyze();
                this.frame.setVisible(true);
            } catch (Exception err) {
                textPane.setText("Error: " + err.toString());
            }
        }
        
        
    }

    /**
     * Construct window
     */
    protected void createResultPanel() {
        
        if(textPane == null){
            textPane = new JTextPane();

            frame = new JFrame("City analysis results");
            frame.getContentPane().add(new JScrollPane(textPane), "Center");



            Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
            int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
            frame.setLocation(x, y);

            frame.setPreferredSize(new Dimension(300, 500));

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            frame.setVisible(true);
        }
    }
}
