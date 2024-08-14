package ru.biosoft.plugins.graph;

import java.awt.Color;
import java.awt.Font;

import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.Option;

/**
 * General class to store options for graph painting.
 */
public class GraphViewOptions extends Option
{
    /**
     * Creates <code>GraphViewOptions</code> and initializes it to the specified options.
     *
     * @param parent   parent property
     *
    public GraphViewOptions(Option parent)
    {
        super(parent);
    }*/

    public GraphViewOptions()
    {}

    protected boolean debugMode;
    public boolean isDebugMode()
    {
        return debugMode;
    }
    public void setDebugMode(boolean debugMode)
    {
        boolean oldValue = this.debugMode;
        this.debugMode = debugMode;
        firePropertyChange("debugMode", oldValue, debugMode);
    }

    /**
     * <code>Pen</code> to paint Node.
     */
    protected Pen graphBorderPen = new Pen(1, Color.red);


    public Pen getGraphBorderPen()
    {
        return graphBorderPen;
    }
    public void setGraphBorderPen(Pen graphBorderPen)
    {
        Pen oldValue = this.graphBorderPen;
        this.graphBorderPen = graphBorderPen;
        firePropertyChange("graphBorderPen", oldValue, graphBorderPen);
    }

    //////////////////////////////////////////////////////////////////
    // Node properties
    //

    /**
     * <code>Pen</code> to paint Node.
     */
    protected Pen nodePen = new Pen(1, Color.black);
    public Pen getNodePen()
    {
        return nodePen;
    }
    public void setNodePen(Pen nodePen)
    {
        Pen oldValue = this.nodePen;
        this.nodePen = nodePen;
        firePropertyChange("nodePen", oldValue, nodePen);
    }

    /**
     * <code>ColorFont</code> for Node title.
     */
    protected ColorFont nodeTitleFont = new ColorFont("Arial", Font.PLAIN, 12, Color.black);
    public ColorFont getNodeTitleFont()
    {
        return nodeTitleFont;
    }
    public void setNodeTitleFont(ColorFont nodeTitleFont)
    {
        ColorFont oldValue = this.nodeTitleFont;
        this.nodeTitleFont = nodeTitleFont;
        firePropertyChange("nodeTitle", oldValue, nodeTitleFont);
    }

    //////////////////////////////////////////////////////////////////
    // Edge properties
    //

    /**
     * <code>Pen</code> to paint Edge.
     */
    protected Pen edgePen = new Pen(1, Color.blue);
    public Pen getEdgePen()
    {
        return edgePen;
    }
    public void setEdgePen(Pen edgePen)
    {
        Pen oldValue = this.edgePen;
        this.edgePen = edgePen;
        firePropertyChange("edgePen", oldValue, edgePen);
    }

    //////////////////////////////////////////////////////////////////
    // Defaults
    //

    /**
     * Default <code>Pen</code>.
     */
    protected Pen defaultPen = new Pen(1, Color.black);
    public Pen getDefaultPen()
    {
        return defaultPen;
    }
    public void setDefaultPen(Pen defaultPen)
    {
        Pen oldValue = this.defaultPen;
        this.defaultPen = defaultPen;
        firePropertyChange("defaultPen", oldValue, defaultPen);
    }

    /**
     * Default <code>ColorFont</code>.
     */
    protected ColorFont defaultFont = new ColorFont("Arial", Font.PLAIN, 12, Color.black);
    public ColorFont getDefaultFont()
    {
        return defaultFont;
    }
    public void setDefaultFont(ColorFont defaultFont)
    {
        ColorFont oldValue = this.defaultFont;
        this.defaultFont = defaultFont;
        firePropertyChange("defaultFont", oldValue, defaultFont);
    }

}
