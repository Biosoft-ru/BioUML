package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("View options")
public class PathwaySimulationDiagramViewOptions extends PathwayDiagramViewOptions
{

    public boolean showVariableValue = true;
    public boolean showReactionRate = false;

    public boolean createPortEdges = true;

    protected ColorFont portFont = new ColorFont("Arial", Font.PLAIN, 14, Color.black);
    protected ColorFont mathTitleFont = new ColorFont("Arial", Font.BOLD, 12, Color.black);
    protected Pen mathPen = new Pen(1, Color.black);

    protected ColorFont stateOnFont = new ColorFont("Arial", Font.ITALIC, 11, Color.black);

    protected Pen transitionPen = new Pen(1, Color.black);
    protected Brush transitionBrush = new Brush(Color.black);

    protected boolean mathAsText = false;
    protected ColorFont formulaFont = new ColorFont("Arial", Font.PLAIN, 11, Color.black);
    protected Point formulaOffset = new Point(0, 2);
    protected int borderOffset = 3;

    protected Brush outputConnectionPortBrush = new Brush(Color.red);
    private Brush inputConnectionPortBrush = new Brush(Color.green);
    protected Brush contactConnectionPortBrush = new Brush(Color.gray);

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates <code>PathwaySimulationDiagramViewOptions</code> and initializes it.
     *
     * @param parent parent property
     */
    public PathwaySimulationDiagramViewOptions(Option parent)
    {
        super(parent);

        showReactionName = true;
        connectionPen = new Pen(1, Color.black);
        stateTitleFont = new ColorFont("Arial", Font.BOLD, 12, Color.black);
    }

    public PathwaySimulationDiagramViewOptions()
    {
        this(null);
    }

    @PropertyName("Math title font")
    @PropertyDescription("Font for mathematical objects (equations, events,...) title.")
    public ColorFont getMathTitleFont()
    {
        return mathTitleFont;
    }
    public void setMathTitleFont(ColorFont mathTitleFont)
    {
        this.mathTitleFont = mathTitleFont;
    }

    @PropertyName ( "Port title font" )
    @PropertyDescription ( "Font for ports." )
    public ColorFont getPortTitleFont()
    {
        return portFont;
    }
    public void setPortTitleFont(ColorFont portFont)
    {
        this.portFont = portFont;
    }
    
    @PropertyName("Math pen")
    @PropertyDescription("Pen for mathematical objects (equations, events,...). ")
    public Pen getMathPen()
    {
        return mathPen;
    }
    public void setMathPen(Pen mathPen)
    {
        this.mathPen = mathPen;
    }

    @PropertyName("Contact port brush")
    @PropertyDescription("Contact port brush. ")
    public Brush getContactConnectionPortBrush()
    {
        return contactConnectionPortBrush;
    }

    public void setContactConnectionPortBrush(Brush contactConnectionPortBrush)
    {
        this.contactConnectionPortBrush = contactConnectionPortBrush;
    }

    public Brush getInputConnectionPortBrush()
    {
        return inputConnectionPortBrush;
    }

    public void setInputConnectionPortBrush(Brush inputConnectionPortBrush)
    {
        this.inputConnectionPortBrush = inputConnectionPortBrush;
    }
}
