package biouml.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.event.EventListenerList;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.OrthogonalPathLayouter;
import ru.biosoft.graph.PathLayouterWrapper;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.editor.GridOptions;
import ru.biosoft.graphics.editor.GridOptionsMessageBundle;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.beans.model.Property;

import one.util.streamex.StreamEx;

import com.developmentontheedge.application.Application;

/**
 * General class to store options for diagram painting.
 */
@PropertyName ( "Diagram view options" )
public class DiagramViewOptions extends Option implements Cloneable
{
    protected ColorFont diagramTitleFont = new ColorFont("Arial", Font.BOLD, 14, Color.black);
    protected ColorFont compartmentTitleFont = new ColorFont("Arial", Font.BOLD, 13, Color.black);
    protected ColorFont defaultFont = new ColorFont("Arial", Font.PLAIN, 12, Color.black);

    protected Pen defaultPen = new Pen(1, Color.black);
    protected Pen noteLinkPen;
    protected Pen highlightPen = new Pen( 2, new Color( 96, 171, 247 ) );
    protected Pen nodePen = new Pen(1, Color.black);

    protected Brush connectionBrush = new Brush(Color.black);
    protected Brush noteBrush = new Brush(Color.white);

    protected Point noteMargin = new Point(3, 3);
    public int compartmentTitleAlignment = CompositeView.X_CC | CompositeView.Y_TT;
    public int diagramTitleAlignment = CompositeView.X_CC | CompositeView.Y_TB;

    private PathLayouterWrapper pathLayouterWrapper = new PathLayouterWrapper( new OrthogonalPathLayouter() );

    protected boolean dependencyEdges = false;
    protected boolean autoLayout = false;
    private boolean drawOnFly = false;
    protected boolean designMode = false;
    private int nodeTitleLimit = 1000;

    protected GridOptions gridOptions;

    protected Map<String, ColorFont> fontRegistry = new HashMap<>();

    private DiagramElementStyleDeclaration[] styles = new DiagramElementStyleDeclaration[0];

    public static String MULTIPLY_DOT = "·";
    public static String MULTIPLY_ASTERISK = "*";
    public static String MULTIPLY_CROSS = "×";
    public static String[] VARIABLE_MODES = {"ID", "title(complete)", "title(brief)", "ID (brief)", "ID (no $)"};
    public static String[] MULTIPLY_SIGNS = {MULTIPLY_CROSS, MULTIPLY_DOT, MULTIPLY_ASTERISK};

    private String multiplySign = MULTIPLY_CROSS;
    private String varNameMode = VARIABLE_MODES[0];
    private int maxTitleSize = 30;

    public DiagramViewOptions(Option parent)
    {
        super(parent);
    }

    @PropertyName("Auto-layout edges")
    @PropertyDescription("Layout diagram edges automatically")
    public boolean isAutoLayout()
    {
        //return autoLayout;
        return false;
    }

    public void setAutoLayout(boolean autoLayout)
    {
        this.autoLayout = autoLayout;
    }

    @PropertyName("Draw on the fly")
    @PropertyDescription("If true then diagram elements will be automatically redrawn on mouse drag.")
    public boolean isDrawOnFly()
    {
        return drawOnFly;
    }
    public void setDrawOnFly(boolean drawOnFly)
    {
        this.drawOnFly = drawOnFly;
    }

    @PropertyName("Dependency edges")
    @PropertyDescription("If true then depndency edges will be automatically generated for equations.")
    public boolean isDependencyEdges()
    {
        return dependencyEdges;
    }
    public void setDependencyEdges(boolean dependencyEdges)
    {
        boolean oldValue = this.dependencyEdges;
        this.dependencyEdges = dependencyEdges;
        firePropertyChange("dependencyEdges", oldValue, dependencyEdges);
    }

    public Layouter getPathLayouter()
    {
        return pathLayouterWrapper.getPathLayouter();
    }

    @PropertyName ( "Path layouter" )
    @PropertyDescription ( "Path layouter properties" )
    public PathLayouterWrapper getPathLayouterWrapper()
    {
        return pathLayouterWrapper;
    }

    public void setPathLayouterWrapper(PathLayouterWrapper pathLayouterWrapper)
    {
        this.pathLayouterWrapper = pathLayouterWrapper;
    }

    public Map<String, ColorFont> getFontRegistry()
    {
        return fontRegistry;
    }

    @PropertyName("Grid options")
    @PropertyDescription("Diagram editor's grid options.")
    public GridOptions getGridOptions()
    {
        if( gridOptions == null )
        {
            Preferences preferences = Application.getPreferences();
            if( preferences != null )
            {
                Preferences gridPreferences = (Preferences)preferences.getValue(GridOptions.GRID_OPTIONS);
                if( gridPreferences == null )
                {
                    gridOptions = new GridOptions();
                    try
                    {
                        GridOptionsMessageBundle messageBundle = new GridOptionsMessageBundle();
                        preferences.add(new DynamicProperty(GridOptions.GRID_OPTIONS, messageBundle.getString("DISPLAY_NAME"),
                                messageBundle.getString("SHORT_DESCRIPTION"), Preferences.class, gridOptions.getAsPreferences()));
                    }
                    catch( Exception e )
                    {
                    }
                }
                else
                {
                    gridOptions = new GridOptions(gridPreferences);
                }

                gridOptions.setUseDefault(true);
                gridOptions.setParent(this);
            }
            else
                gridOptions = new GridOptions();
        }
        return gridOptions;
    }

    public void setGridOptions(GridOptions gridOptions)
    {
        if( this.gridOptions != null )
            this.gridOptions.setParent(null);

        GridOptions oldValue = this.gridOptions;
        this.gridOptions = gridOptions;

        if( this.gridOptions != null )
            this.gridOptions.setParent(this);
        firePropertyChange("gridOptions", oldValue, gridOptions);
    }

    public boolean isDesignMode()
    {
        return designMode;
    }
    public void setDesignMode(boolean designMode)
    {
        boolean oldValue = this.designMode;
        this.designMode = designMode;
        firePropertyChange("designMode", oldValue, designMode);
    }

    @PropertyName("Diagram title font")
    @PropertyDescription("Diagram title font.")
    public ColorFont getDiagramTitleFont()
    {
        return diagramTitleFont;
    }
    public void setDiagramTitleFont(ColorFont diagramTitleFont)
    {
        ColorFont oldValue = this.diagramTitleFont;
        this.diagramTitleFont = diagramTitleFont;
        firePropertyChange("diagramTitleFont", oldValue, diagramTitleFont);
    }

    protected boolean diagramTitleVisible = true;
    @PropertyName("Title visible")
    @PropertyDescription("Check this box if diagram title should be visible.")
    public boolean isDiagramTitleVisible()
    {
        return diagramTitleVisible;
    }
    public void setDiagramTitleVisible(boolean diagramTitleVisible)
    {
        boolean oldValue = this.diagramTitleVisible;
        this.diagramTitleVisible = diagramTitleVisible;
        firePropertyChange("diagramTitleVisible", oldValue, diagramTitleVisible);
    }

    public int getDiagramTitleAlignment()
    {
        return diagramTitleAlignment;
    }
    public void setDiagramTitleAlignment(int diagramTitleAlignment)
    {
        int oldValue = this.diagramTitleAlignment;
        this.diagramTitleAlignment = diagramTitleAlignment;
        firePropertyChange("diagramTitleAlignment", oldValue, diagramTitleAlignment);
    }

    @PropertyName("Compartment title font")
    @PropertyDescription("Compartment title font.")
    public ColorFont getCompartmentTitleFont()
    {
        return compartmentTitleFont;
    }
    public void setCompartmentTitleFont(ColorFont compartmentTitleFont)
    {
        ColorFont oldValue = this.compartmentTitleFont;
        this.compartmentTitleFont = compartmentTitleFont;
        firePropertyChange("compartmentTitleFont", oldValue, compartmentTitleFont);
    }

    public int getCompartmentTitleAlignment()
    {
        return compartmentTitleAlignment;
    }
    public void setCompartmentTitleAlignment(int compartmentTitleAlignment)
    {
        int oldValue = this.compartmentTitleAlignment;
        this.compartmentTitleAlignment = compartmentTitleAlignment;
        firePropertyChange("compartmentTitleAlignment", oldValue, compartmentTitleAlignment);
    }

    @PropertyName("Note brush")
    @PropertyDescription("Brush for notes")
    public Brush getNoteBrush()
    {
        return noteBrush;
    }
    public void setNoteBrush(Brush noteBrush)
    {
        Brush oldValue = this.noteBrush;
        this.noteBrush = noteBrush;
        firePropertyChange("noteBrush", oldValue, noteBrush);
    }

    public Point getNoteMargin()
    {
        return noteMargin;
    }
    public void setNoteMargin(Point noteMargin)
    {
        Point oldValue = this.noteMargin;
        this.noteMargin = noteMargin;
        firePropertyChange("noteMargin", oldValue, noteMargin);
    }

    @PropertyName("Note link pen")
    @PropertyDescription("Note link pen.")
    public Pen getNoteLinkPen()
    {
        if( noteLinkPen == null )
            noteLinkPen = new Pen(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {7, 3}, 0),
                    Color.magenta.darker().darker());

        return noteLinkPen;
    }
    public void setNoteLinkPen(Pen noteLinkPen)
    {
        Pen oldValue = this.noteLinkPen;
        this.noteLinkPen = noteLinkPen;
        firePropertyChange("noteLinkPen", oldValue, noteLinkPen);
    }

    //////////////////////////////////////////////////////////////////
    // Node properties
    //

    @PropertyName("Node pen")
    @PropertyDescription("Pen for nodes border.")
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

    @PropertyName("Node title font")
    @PropertyDescription("Default Node title font.")
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

    /**
     * Margin between node image and node title.
     */
    protected Point nodeTitleMargin = new Point(0, 3);
    public Point getNodeTitleMargin()
    {
        return nodeTitleMargin;
    }
    public void setNodeTitleMargin(Point nodeTitleMargin)
    {
        Point oldValue = this.nodeTitleMargin;
        this.nodeTitleMargin = nodeTitleMargin;
        firePropertyChange("nodeTitleMargin", oldValue, nodeTitleMargin);
    }

    //////////////////////////////////////////////////////////////////
    // Connection properties
    //
    protected Pen connectionPen = new Pen(1, Color.black);
    @PropertyName("Connection pen")
    @PropertyDescription("Pen for connection edges")
    public Pen getConnectionPen()
    {
        return connectionPen;
    }
    public void setConnectionPen(Pen connectionPen)
    {
        Pen oldValue = this.connectionPen;
        this.connectionPen = connectionPen;
        firePropertyChange("connectionPen", oldValue, connectionPen);
    }

    @PropertyName("Connection brush")
    @PropertyDescription("Brush for arrow tips of connection edges")
    public Brush getConnectionBrush()
    {
        return connectionBrush;
    }
    public void setConnectionBrush(Brush connectionBrush)
    {
        Brush oldValue = this.connectionBrush;
        this.connectionBrush = connectionBrush;
        firePropertyChange("connectionBrush", oldValue, connectionBrush);
    }

    @PropertyName("Default pen")
    @PropertyDescription("Pen used for other elements")
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

    @PropertyName("Default font")
    @PropertyDescription("Commonly used font")
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

    @PropertyName("Styles")
    @PropertyDescription("Diagram element styles.")
    public DiagramElementStyleDeclaration[] getStyles()
    {
        return styles;
    }

    public void setStyles(DiagramElementStyleDeclaration[] styles)
    {
        Object oldValue = this.styles;
        this.styles = styles;
        firePropertyChange("styles", oldValue, styles);
    }

    public void addStyleIfAbsent(DiagramElementStyleDeclaration newStyle)
    {
        DiagramElementStyleDeclaration style = StreamEx.of( styles ).findAny( s -> s.getName().equals( newStyle.getName() ) )
                .orElse( null );
        if( style == null )
        {
            DiagramElementStyleDeclaration[] newStyles = Arrays.copyOf( styles, styles.length + 1 );
            newStyles[newStyles.length - 1] = newStyle;
            setStyles( newStyles );
        }
    }

    public void addStylesIfAbsent(Map<String, DiagramElementStyleDeclaration> newStyles)
    {
        StreamEx.of( styles ).forEach( s -> newStyles.remove( s.getName() ) );
        if( newStyles.isEmpty() )
            return;
        int i = styles.length;
        DiagramElementStyleDeclaration[] newStylesWithAdded = Arrays.copyOf( styles, styles.length + newStyles.size() );
        Iterator<DiagramElementStyleDeclaration> iterator = newStyles.values().iterator();
        while( iterator.hasNext() )
        {
            newStylesWithAdded[i++] = iterator.next();
        }
        setStyles( newStylesWithAdded );
    }

    public DiagramElementStyle getStyle(String name)
    {
        DiagramElementStyleDeclaration predefined = StreamEx.of(styles).findAny(s -> s.getName().equals(name)).orElse(null);
        return predefined != null? predefined.getStyle(): null;
    }

    @PropertyName ( "Highlight color" )
    @PropertyDescription ( "Color to highlight nodes with specific properties." )
    public Pen getHighlightPen()
    {
        return highlightPen;
    }

    public void setHighlightPen(Pen highlightPen)
    {
        this.highlightPen = highlightPen;
    }

    @PropertyName("Node title limit")
    @PropertyDescription("Titles exceeding this limit wll be splitted into several lines.")
    public int getNodeTitleLimit()
    {
        return nodeTitleLimit;
    }

    public void setNodeTitleLimit(int nodeTitleLimit)
    {
        Object oldValue = this.nodeTitleLimit;
        this.nodeTitleLimit = nodeTitleLimit;
        firePropertyChange("nodeTitleLimit", oldValue, nodeTitleLimit);
    }

    public void initFromJSON(JSONObject from) throws JSONException
    {
        for( Property property : BeanUtil.properties( this ) )
        {
            Object val = from.get(property.getName());
            try
            {
                property.setValue(TextUtil.fromString(property.getValueClass(), val.toString()));
            }
            catch( NoSuchMethodException e )
            {
            }
        }
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        for( Property property : BeanUtil.properties( this ) )
        {
            try
            {
                Object value = property.getValue();
                if( value != null )
                {
                    String name = property.getName();
                    Object object = TextUtil.toString(value);
                    json.putOpt(name, object);
                }
            }
            catch( Exception ex )
            {
            }
        }
        return json;
    }

    @Override
    public Object clone()
    {
        try
        {
            DiagramViewOptions clone = (DiagramViewOptions)super.clone();
            clone.listenerList = new EventListenerList();
            return clone;
        }
        catch( CloneNotSupportedException e )
        {
            return null;
        }
    }

    @PropertyName ( "Multiplication sign" )
    public String getMultiplySign()
    {
        return multiplySign;
    }
    public void setMultiplySign(String multiplySign)
    {
        this.multiplySign = multiplySign;
    }

    @PropertyName ( "Variable name mode" )
    public String getVarNameMode()
    {
        return varNameMode;
    }
    public void setVarNameMode(String varNameMode)
    {
        this.varNameMode = varNameMode;
    }

    public int getVarNameCode()
    {
        return ArrayUtils.indexOf( VARIABLE_MODES, varNameMode );
    }

    @PropertyName ( "Wrap title limit" )
    public int getMaxTitleSize()
    {
        return maxTitleSize;
    }
    public void setMaxTitleSize(int maxTitleSize)
    {
        this.maxTitleSize = maxTitleSize;
    }
}