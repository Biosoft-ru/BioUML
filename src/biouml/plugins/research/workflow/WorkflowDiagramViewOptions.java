package biouml.plugins.research.workflow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.Option;

import biouml.model.DiagramViewOptions;

/**
 * View options for workflow diagrams
 */
@PropertyName("View options")
public class WorkflowDiagramViewOptions extends DiagramViewOptions
{
    public WorkflowDiagramViewOptions(Option parent)
    {
        super(parent);
        diagramTitleVisible = false;
        autoLayout = true;
        setNotificationEnabled( false );
        setSmallFont( new ColorFont("Arial", Font.PLAIN, 9, Color.black) );
        setDeBrush( new Brush(new Color(176, 196, 222)) );
        setAnalysisBrush( new Brush(new Color(146, 223, 253), new Color(205, 241, 253)) );
        setAnalysisPen( new Pen(1, new Color(83, 180, 222)) );
        setScriptBrush( new Brush(new Color(226, 223, 206), new Color(242, 242, 232)) );
        setScriptPen( new Pen(1, new Color(196, 184, 168)) );
        setSqlBrush( new Brush(new Color(253, 206, 198), new Color(250, 230, 223)) );
        setSqlPen( new Pen(1, new Color(239, 162, 146)) );
        setExpressionBrush( new Brush(new Color(254, 214, 116), new Color(252, 245, 193)) );
        setExpressionPen( new Pen(1, new Color(236, 158, 29)) );
        setParameterBrush( new Brush(new Color(169, 237, 138), new Color(218, 250, 201)) );
        setParameterPen( new Pen(1, new Color(60, 160, 6)) );
        setProgressFont( new ColorFont("Arial", Font.BOLD, 18, Color.white) );
        setListenerPen( new Pen(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {9, 6}, 0),
            Color.magenta.darker().darker()) );
        setNotificationEnabled( true );
    }

    protected ColorFont smallFont;
    @PropertyName("Script body font")
    @PropertyDescription("Font to display script text")
    public ColorFont getSmallFont()
    {
        return smallFont;
    }
    public void setSmallFont(ColorFont smallFont)
    {
        ColorFont oldValue = this.smallFont;
        this.smallFont = smallFont;
        firePropertyChange("smallFont", oldValue, smallFont);
    }

    protected Brush deBrush;
    public Brush getDeBrush()
    {
        return deBrush;
    }
    public void setDeBrush(Brush deBrush)
    {
        Brush oldValue = this.deBrush;
        //if(this.deBrush != null)
        //    this.deBrush.setParent( null );
        this.deBrush = deBrush;
        //this.deBrush.setParent( this );
        firePropertyChange("deBrush", oldValue, deBrush);
    }

    protected Brush analysisBrush;
    
    @PropertyName("Analysis brush")
    @PropertyDescription("Brush to fill analysis boxes")
    public Brush getAnalysisBrush()
    {
        return analysisBrush;
    }
    public void setAnalysisBrush(Brush analysisBrush)
    {
        Brush oldValue = this.analysisBrush;
        //if(this.analysisBrush != null)
        //    this.analysisBrush.setParent( null );
        this.analysisBrush = analysisBrush;
        //this.analysisBrush.setParent( this );
        firePropertyChange("analysisBrush", oldValue, analysisBrush);
    }

    protected Brush scriptBrush;
    @PropertyName("Script brush")
    @PropertyDescription("Brush to fill script boxes")
    public Brush getScriptBrush()
    {
        return scriptBrush;
    }
    public void setScriptBrush(Brush scriptBrush)
    {
        Brush oldValue = this.scriptBrush;
        //if(this.scriptBrush != null)
        //    this.scriptBrush.setParent( null );
        this.scriptBrush = scriptBrush;
        //this.scriptBrush.setParent( this );
        firePropertyChange("scriptBrush", oldValue, scriptBrush);
    }

    protected Brush sqlBrush;
    @PropertyName("SQL brush")
    @PropertyDescription("Brush to fill SQL boxes")
    public Brush getSqlBrush()
    {
        return sqlBrush;
    }
    public void setSqlBrush(Brush sqlBrush)
    {
        Brush oldValue = this.sqlBrush;
        //if(this.sqlBrush != null)
        //    this.sqlBrush.setParent( null );
        this.sqlBrush = sqlBrush;
        //this.sqlBrush.setParent( this );
        firePropertyChange("sqlBrush", oldValue, sqlBrush);
    }

    protected Brush expressionBrush;
    @PropertyName("Expression brush")
    @PropertyDescription("Brush to fill expression boxes")
    public Brush getExpressionBrush()
    {
        return expressionBrush;
    }
    public void setExpressionBrush(Brush expressionBrush)
    {
        Brush oldValue = this.expressionBrush;
        //if(this.expressionBrush != null)
        //    this.expressionBrush.setParent( null );
        this.expressionBrush = expressionBrush;
        //this.expressionBrush.setParent( this );
        firePropertyChange("expressionBrush", oldValue, expressionBrush);
    }

    protected Brush parameterBrush;
    @PropertyName("Parameter brush")
    @PropertyDescription("Brush to fill parameter boxes")
    public Brush getParameterBrush()
    {
        return parameterBrush;
    }
    public void setParameterBrush(Brush parameterBrush)
    {
        Brush oldValue = this.parameterBrush;
        //if(this.parameterBrush != null)
        //    this.parameterBrush.setParent( null );
        this.parameterBrush = parameterBrush;
        //this.parameterBrush.setParent( this );
        firePropertyChange("parameterBrush", oldValue, parameterBrush);
    }
    
    protected ColorFont progressFont;
    @PropertyName("Progress font")
    @PropertyDescription("Font used to display progress of running analysis")
    public ColorFont getProgressFont()
    {
        return progressFont;
    }
    public void setProgressFont(ColorFont progressFont)
    {
        ColorFont oldValue = this.progressFont;
        this.progressFont = progressFont;
        firePropertyChange("progressFont", oldValue, progressFont);
    }

    protected Pen analysisPen;
    @PropertyName("Analysis pen")
    @PropertyDescription("Outline of analysis boxes")
    public Pen getAnalysisPen()
    {
        return analysisPen;
    }
    public void setAnalysisPen(Pen analysisPen)
    {
        Object oldValue = this.analysisPen;
        this.analysisPen = analysisPen;
        firePropertyChange("analysisPen", oldValue, analysisPen);
    }

    protected Pen parameterPen;
    @PropertyName("Parameter pen")
    @PropertyDescription("Outline of parameter boxes")
    public Pen getParameterPen()
    {
        return parameterPen;
    }
    public void setParameterPen(Pen parameterPen)
    {
        Object oldValue = this.parameterPen;
        this.parameterPen = parameterPen;
        firePropertyChange("parameterPen", oldValue, parameterPen);
    }

    protected Pen expressionPen;
    @PropertyName("Expression pen")
    @PropertyDescription("Outline of expression boxes")
    public Pen getExpressionPen()
    {
        return expressionPen;
    }
    public void setExpressionPen(Pen expressionPen)
    {
        Object oldValue = this.expressionPen;
        this.expressionPen = expressionPen;
        firePropertyChange("expressionPen", oldValue, expressionPen);
    }

    protected Pen scriptPen;
    @PropertyName("Script pen")
    @PropertyDescription("Outline of script boxes")
    public Pen getScriptPen()
    {
        return scriptPen;
    }
    public void setScriptPen(Pen scriptPen)
    {
        Object oldValue = this.scriptPen;
        this.scriptPen = scriptPen;
        firePropertyChange("scriptPen", oldValue, scriptPen);
    }

    protected Pen sqlPen;
    @PropertyName("SQL pen")
    @PropertyDescription("Outline of SQL boxes")
    public Pen getSqlPen()
    {
        return sqlPen;
    }
    public void setSqlPen(Pen sqlPen)
    {
        Object oldValue = this.sqlPen;
        this.sqlPen = sqlPen;
        firePropertyChange("slqPen", oldValue, sqlPen);
    }

    protected Pen listenerPen;
    @PropertyName("Note link pen")
    @PropertyDescription("Note edges style")
    public Pen getListenerPen()
    {
        return listenerPen;
    }
    public void setListenerPen(Pen listenerPen)
    {
        Object oldValue = this.listenerPen;
        this.listenerPen = listenerPen;
        firePropertyChange("listenerPen", oldValue, listenerPen);
    }
    
    @PropertyName("Expression font")
    @PropertyDescription("Font for expressions and variables")
    public ColorFont getExpressionFont()
    {
        return nodeTitleFont;
    }  
    public void setExpressionFont(ColorFont font)
    {
        this.nodeTitleFont = font;
    }
}
