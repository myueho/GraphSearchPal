package net.coderodde.gsp.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JPanel;
import net.coderodde.gsp.gui.data.ProgressListener;
import net.coderodde.gsp.model.GraphSearchListener;
import net.coderodde.gsp.model.support.DirectedGraphNode;
import net.coderodde.gsp.model.support.UndirectedGraphNode;
import net.coderodde.gsp.model.support.UndirectedGraphWeightFunction;

/**
 * This class implements the panel used for displaying the graph search progress
 * and for drawing the graph.
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6
 */
public class GraphPalPanel extends JPanel 
implements GraphSearchListener<UndirectedGraphNode> {
   
    /**
     * The default color for the nodes that are not walls.
     */
    private static final Color DEFAULT_NON_WALL_COLOR = Color.WHITE;
    
    /**
     * The default color for the walls.
     */
    private static final Color DEFAULT_WALL_COLOR = Color.BLACK;
    
    /**
     * The default color of the source node.
     */
    private static final Color DEFAULT_SOURCE_COLOR = Color.GREEN;
    
    /**
     * The default color of the target node.
     */
    private static final Color DEFAULT_TARGET_COLOR = Color.RED;
    
    /**
     * The default color of the open nodes.
     */
    private static final Color DEFAULT_OPEN_COLOR = Color.ORANGE;
    
    /**
     * The default color of the closed nodes.
     */
    private static final Color DEFAULT_CLOSED_COLOR = Color.LIGHT_GRAY;
    
    /**
     * The default color of the shortest paths.
     */
    private static final Color DEFAULT_PATH_COLOR = Color.BLUE;
    
    /**
     * The cost of diagonal edges.
     */
    private static final double SQRT2 = Math.sqrt(2.0);
    
    private final UndirectedGraphNode[][] graph;
    private final Set<UndirectedGraphNode> wallNodeSet;
    private final Set<UndirectedGraphNode> openNodeSet;
    private final Set<UndirectedGraphNode> closedNodeSet;
    private final Map<UndirectedGraphNode, Point> nodesToCoordinatesMap;
    private final UndirectedGraphWeightFunction weightFunction;
    
    private Color nonWallColor = DEFAULT_NON_WALL_COLOR;
    private Color wallColor    = DEFAULT_WALL_COLOR;
    private Color sourceColor  = DEFAULT_SOURCE_COLOR;
    private Color targetColor  = DEFAULT_TARGET_COLOR;
    private Color openColor    = DEFAULT_OPEN_COLOR;
    private Color closedColor  = DEFAULT_CLOSED_COLOR;
    
    private Point sourcePoint = new Point();
    private Point targetPoint = new Point();
    
    private List<DirectedGraphNode> path;
    
    public GraphPalPanel() {
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        this.graph = new UndirectedGraphNode[screenDimension.height]
                                            [screenDimension.width];
        this.wallNodeSet = new LinkedHashSet<>(screenDimension.height *
                                               screenDimension.width);
        this.openNodeSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.closedNodeSet = 
                Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.nodesToCoordinatesMap = new HashMap<>(screenDimension.height *
                                                   screenDimension.width);
        int nodeId = 0;
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        
        int id = 0;
        
        for (int y = 0; y < graph.length; ++y) {
            for (int x = 0; x < graph[0].length; ++x) {
                sb.delete(0, sb.length());
                sb.append(nodeId++);
                UndirectedGraphNode node = new UndirectedGraphNode(id++);
                graph[y][x] = node;
                nodesToCoordinatesMap.put(node, new Point(x, y));
                
                if (random.nextFloat() < 0.01f) {
                    wallNodeSet.add(node);
                }
            }
        }
        
        this.weightFunction = new UndirectedGraphWeightFunction();
        initializeWeightFunction(screenDimension);
    }
    
    private void initializeWeightFunction(Dimension screenDimension) {
        int width = screenDimension.width;
        int height = screenDimension.height;
        
        // Create weights for horizontal edges.
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width - 1; ++x) {
                weightFunction.put(graph[y][x], graph[y][x + 1], 1.0);
            }
        }
        
        // Create weights for vertical edges.
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height - 1; ++y) {
                weightFunction.put(graph[y][x], graph[y + 1][x], 1.0);
            }
        }
        
        // Create weights for diagonal (\) edges.
        for (int y = 0; y < height - 1; ++y) {
            for (int x = 0; x < width - 1; ++x) {
                weightFunction.put(graph[y][x], graph[y + 1][x + 1], SQRT2);
            }
        }
        
        
        // Create weights for diagonal (/) edges.
        for (int y = 0; y < height - 1; ++y) {
            for (int x = 1; x < width; ++x) {
                weightFunction.put(graph[y][x], graph[y + 1][x - 1], SQRT2);
            }
        }
    }
    
    public void setNonWallColor(Color c) {
        Objects.requireNonNull(c, "The color is null.");
        this.nonWallColor = c;
    }
    
    public void setWallColor(Color c) {
        Objects.requireNonNull(c, "The color is null.");
        this.wallColor = c;
    }
    
    public void setSourceColor(Color c) {
        Objects.requireNonNull(c, "The color is null.");
        this.sourceColor = c;
    }
    
    public void setTargetColor(Color c) {
        Objects.requireNonNull(c, "The color is null.");
        this.targetColor = c;
    }
    
    public void setOpenColor(Color c) {
        Objects.requireNonNull(c, "The color is null.");
        this.openColor = c;
    }
    
    public void setClosedColor(Color c) {
        Objects.requireNonNull(c, "The color is null.");
        this.closedColor = c;
    }
    
    @Override
    public void paint(Graphics g) {
        update(g);
    }
    
    @Override
    public void update(Graphics g) {
        g.setColor(Color.blue);
        int width  = getWidth();
        int height = getHeight();
        g.clearRect(0, 0, width, height);
        BufferedImage image = new BufferedImage(width, 
                                                height, 
                                                BufferedImage.TYPE_INT_RGB);
        Graphics2D imgg = image.createGraphics();
        imgg.setColor(nonWallColor);
        imgg.fillRect(0, 0, width, height);
        
        for (UndirectedGraphNode node : wallNodeSet) {
            Point point = nodesToCoordinatesMap.get(node);
            
            if (point.x < width && point.y < height) {
                image.setRGB(point.x, point.y, wallColor.getRGB());
            }
        }
        
        for (UndirectedGraphNode node : closedNodeSet) {
            Point point = nodesToCoordinatesMap.get(node);
            image.setRGB(point.x, point.y, closedColor.getRGB());
        }
        
        for (UndirectedGraphNode node : openNodeSet) {
            Point point = nodesToCoordinatesMap.get(node);
            image.setRGB(point.x, point.y, openColor.getRGB());
        }
        
        g.drawImage(image, 0, 0, this);
    }

    @Override
    public void begin() {
        openNodeSet.clear();
        closedNodeSet.clear();
        repaint();
    }

    @Override
    public void reached(UndirectedGraphNode node) {
        openNodeSet.add(node);
    }

    @Override
    public void closed(UndirectedGraphNode node) {
        closedNodeSet.add(node);
    }

    @Override
    public void done(List<UndirectedGraphNode> path) {
        System.out.println("Yeah!");
    }
}