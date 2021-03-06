/*
 * Copyright 2012 Justin Wilcox
 * 
 * This file is part of GraphingCalculator.
 *
 * GraphingCalculator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GraphingCalculator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GraphingCalculator.  If not, see <http://www.gnu.org/licenses/>.
 */
package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import functions.Function;

/**
 * Implements a fully-functional graphing window for plotting functions
 */
public class Graph extends JPanel implements MouseListener,
        MouseMotionListener, ActionListener {
    private static final long serialVersionUID = -1661530749016007205L;

    /** Cached function plots valid for the current viewport */
    private ArrayList<Image> plots;

    /** Stores functions that will need to be replotted if the window changes */
    private ArrayList<Function> functions;

    /** Straight lines (segment or infinite) */
    private ArrayList<Line> lines;

    /** Shapes (for integration) */
    private ArrayList<Shape> shapes;

    /** The viewport of the graph */
    private GraphWindow window;

    /** Fonts */
    private Font smallFont;
    private Font mediumFont;

    /** Last known position of the mouse */
    private int mouseX, mouseY;

    /** The toolbox */
    private Image tools;

    /** Points with labels */
    private ArrayList<LabeledPoint> points;

    /** Manage clickable areas on the graph */
    private HitboxManager hitboxes;

    /** Are we in zoom-box-mode? */
    private boolean zoomBoxMode;

    /** First corner of a zoom box */
    private PointD zBoxPoint;

    /** Display text information on the graph */
    private NotificationArea notify;

    /** How clear the plots will be. Smaller is clearer */
    private int plotResolution;

    /** Default window size */
    private GraphWindow defaultWindow;

    public Graph() {
        super();
        plotResolution = 8;
        smallFont = new Font("Monospaced", Font.PLAIN, 10);
        mediumFont = new Font("Monospaced", Font.PLAIN, 14);

        functions = new ArrayList<Function>();

        try {
            tools = ImageIO.read(getClass().getClassLoader().getResource(
                    "tools.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        points = new ArrayList<LabeledPoint>();
        shapes = new ArrayList<Shape>();
        clearPlots();

        lines = new ArrayList<Line>();

        notify = new NotificationArea(new PointD(0, 0), smallFont, new Color(
                25, 110, 32));

        // For the toolbox
        hitboxes = new HitboxManager();
        hitboxes.setListener(new GraphToolbox(this));
        hitboxes.createBox(new Rectangle(653, 0, 22, 21), "zoomIn");
        hitboxes.createBox(new Rectangle(676, 0, 697 - 676, 21), "zoomOut");
        hitboxes.createBox(new Rectangle(653, 22, 22, 21), "zoomReset");
        hitboxes.createBox(new Rectangle(698, 0, 20, 21), "zoomBox");
        hitboxes.createBox(new Rectangle(683, 32, 10, 10), "left");
        hitboxes.createBox(new Rectangle(705, 32, 10, 10), "right");
        hitboxes.createBox(new Rectangle(694, 21, 10, 10), "up");
        hitboxes.createBox(new Rectangle(694, 43, 10, 10), "down");

        // this.setDoubleBuffered(true);
        this.setBackground(Color.WHITE);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    /**
     * Sets the graph to a normal viewport
     */
    public void setDefaultWindow(GraphWindow wind) {
        defaultWindow = wind;
    }

    public void defaultWindow() {
        setWindow(defaultWindow.clone());
    }

    public void setPlotResolution(int newish) {
        plotResolution = newish;
        replotAll();
    }

    public void clearAll() {
        clearLines();
        clearShapes();
        clearFunctions();
        clearPoints();
        setNotification("");
    }

    public void clearLines() {
        lines.clear();
    }

    public void setNotification(String msg) {
        notify.setMessage(msg);
    }

    public void addLine(Line line) {
        lines.add(line);
    }

    private void drawLines(Graphics g) {
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).drawLine(g, window);
        }
    }

    public void addShape(Shape shape) {
        shapes.add(shape);
    }

    public void clearShapes() {
        shapes.clear();
    }

    private void drawShapes(Graphics g) {
        for (int i = 0; i < shapes.size(); i++) {
            shapes.get(i).draw(g, window);
        }
    }

    private void clearPlots() {
        plots = new ArrayList<Image>();
    }

    /**
     * Force a repaint from a non-Swing thread
     */
    public void scheduleRepaint() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                repaint();
            }
        });
    }

    public void addPlot(Function f, Color color) {
        functions.add(f);
        plots.add(PlotFunction.plotFunction(f, window, 8, Color.BLACK));
    }

    public void clearFunctions() {
        functions.clear();
        clearPlots();
    }

    private void replotAll() {
        clearPlots();
        for (int i = 0; i < functions.size(); i++) {
            plots.add(PlotFunction.plotFunction(functions.get(i), window,
                    plotResolution, Color.BLACK));
        }
    }

    /**
     * Moves the viewport to the given window
     */
    public void setWindow(GraphWindow newWindow) {
        clearPlots();
        window = newWindow;
        replotAll();
        scheduleRepaint();
    }

    public void paint(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getSize().width, getSize().height);
        if (window == null) {
            return;
        }

        window.pixHeight = getSize().height;
        window.pixWidth = getSize().width;
        window.rescale();

        g.setColor(Color.WHITE);

        for (int i = 0; i < plots.size(); i++) {
            g.drawImage(plots.get(i), 0, 0, null);
        }

        drawAxes(g);

        drawTools(g);
        drawCursor(g);
        drawLines(g);
        drawShapes(g);
        drawPoints(g);

        notify.draw(g);

        if (zoomBoxMode && zBoxPoint != null) {
            drawZoomBox(g);
        }

        g.setColor(Color.WHITE);
    }

    private void drawTools(Graphics g) {
        g.drawImage(tools, window.pixWidth - tools.getWidth(null), 0, null);
    }

    private void drawCursor(Graphics g) {
        g.setColor(Color.red);
        final int radius = 2;

        g.drawOval(mouseX - radius, mouseY - radius, 4, 4);

        double xG = mouseX * window.xScale + window.xLow;
        double yG = (window.pixHeight - mouseY) * window.yScale + window.yLow;
        g.setColor(Color.BLUE);
        g.setFont(mediumFont);
        String xS = "X: " + xG;
        if (xS.length() > 12) {
            xS = xS.substring(0, 11);
        }
        String yS = "Y: " + yG;
        if (yS.length() > 11) {
            yS = yS.substring(0, 11);
        }
        g.drawString(xS, 0, window.pixHeight
                - g.getFontMetrics(smallFont).getHeight());
        g.drawString(yS, 0, window.pixHeight);
    }

    private void drawZoomBox(Graphics g) {
        int x1 = (int) ((zBoxPoint.x - window.xLow) / window.xScale);
        int y1 = window.pixHeight
                - (int) ((zBoxPoint.y - window.yLow) / window.yScale);

        int x = x1;
        int y = y1;
        if (x1 > mouseX) {
            x = mouseX;
        }

        if (y1 > mouseY) {
            y = mouseY;
        }
        g.setColor(Color.BLACK);
        g.drawRect(x, y, Math.abs(mouseX - x1), Math.abs(mouseY - y1));
    }

    private void drawAxes(Graphics g) {
        g.setFont(smallFont);
        drawVerticalLine(g, Color.BLACK, 0);
        drawHorizontalLine(g, Color.BLACK, 0);

        g.drawString((float) window.yHigh + "", window.pixWidth / 2, 8);
        g.drawString((float) window.yLow + "", window.pixWidth / 2,
                window.pixHeight);
        g.drawString((float) window.xLow + "", 0, window.pixHeight / 2);

        int width = g.getFontMetrics(smallFont).stringWidth(
                "" + (float) window.xHigh);
        g.drawString((float) window.xHigh + "", window.pixWidth - width,
                window.pixHeight / 2);
    }

    private void drawVerticalLine(Graphics g, Color color, double gX) {
        g.setColor(color);
        int x = (int) ((gX - window.xLow) / window.xScale);
        g.drawLine(x, 0, x, window.pixHeight);
    }

    private void drawHorizontalLine(Graphics g, Color color, double gY) {
        g.setColor(color);
        int y = window.pixHeight - (int) ((gY - window.yLow) / window.yScale);
        g.drawLine(0, y, window.pixWidth, y);
    }

    public void addPoint(LabeledPoint p) {
        points.add(p);
    }

    public void clearPoints() {
        points.clear();
    }

    private void drawPoints(Graphics g) {
        for (int i = 0; i < points.size(); i++) {
            points.get(i).draw(g, smallFont, window);
        }
    }

    /**
     * Clicks will go to creating a zoom-box instead of hitboxes
     */
    public void setZoomBoxMode() {
        zoomBoxMode = true;
    }

    public void mouseMoved(MouseEvent event) {
        mouseX = event.getX();
        mouseY = event.getY();
        repaint();
    }

    public void mouseClicked(MouseEvent event) {
        int x = event.getX();
        int y = event.getY();
        int button = event.getButton();
        if (button == 1) {
            if (!zoomBoxMode) {
                hitboxes.checkBoxes(x, y);
            } else {
                if (zBoxPoint == null) {
                    double xG = x * window.xScale + window.xLow;
                    double yG = (window.pixHeight - y) * window.yScale
                            + window.yLow;
                    zBoxPoint = new PointD(xG, yG);
                } else {
                    zoomBoxMode = false;
                    double xG = x * window.xScale + window.xLow;
                    double yG = (window.pixHeight - y) * window.yScale
                            + window.yLow;
                    PointD p2 = new PointD(xG, yG);
                    rebox(zBoxPoint, p2);
                    zBoxPoint = null;
                }
            }
        }
    }

    public void mouseEntered(MouseEvent arg0) {

    }

    public void mouseExited(MouseEvent arg0) {

    }

    public void mousePressed(MouseEvent arg0) {

    }

    public void mouseReleased(MouseEvent arg0) {

    }

    /**
     * Zoom the window by the given factor. 0 < factor < 1 to zoom in. 1 <
     * factor to zoom out.
     */
    public void zoom(double factor) {
        window.xHigh *= factor;
        window.xLow *= factor;
        window.yHigh *= factor;
        window.yLow *= factor;
        window.rescale();
        setWindow(window);
    }

    /**
     * Move the window by x and y. Note x and y are scaled to create the same
     * pixel movement regardless of viewport scale
     */
    public void translate(double x, double y) {
        window.xHigh += x * window.xScale;
        window.xLow += x * window.xScale;
        window.yHigh += y * window.yScale;
        window.yLow += y * window.yScale;
        window.rescale();
        setWindow(window);
    }

    /**
     * Moves the window to the rectangle given by p1 and p2
     */
    public void rebox(PointD p1, PointD p2) {
        if (p1.x > p2.x) {
            window.xHigh = p1.x;
            window.xLow = p2.x;
        } else {
            window.xHigh = p2.x;
            window.xLow = p1.x;
        }

        if (p1.y > p2.y) {
            window.yHigh = p1.y;
            window.yLow = p2.y;
        } else {
            window.yHigh = p2.y;
            window.yLow = p1.y;
        }

        window.rescale();
        setWindow(window);
    }

    public void mouseDragged(MouseEvent arg0) {
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getActionCommand().equals("EXTERMINATE")) {
            clearAll();
        }
        repaint();
    }
}
