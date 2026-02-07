package biouml.plugins.wdl.diagram;

import java.awt.Color;
import java.awt.Font;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

/**
 * View options for workflow diagrams
 */
@PropertyName("View options")
public class WDLViewOptions extends DiagramViewOptions
{
    private Brush outputBrush;
    private Pen outputPen;
    private Brush structBrush = new Brush(Color.white);
    protected Brush deBrush;
    protected Brush callBrush;
    protected Brush taskBrush;
    protected Brush expressionBrush;
    protected Brush conditionBrush =  new Brush(new Color(128, 64, 128), new Color(196, 98, 196));
    protected Brush conditionalBrush =  new Brush(new Color(200, 128, 200), new Color(250, 125, 250));

    protected Pen conditionPen =  new Pen(1, new Color(96, 48, 96));
    protected Brush parameterBrush;
    protected Pen parameterPen;
    protected Pen expressionPen;
    protected Pen analysisPen;
    
    public WDLViewOptions(Option parent)
    {
        super(parent);
        diagramTitleVisible = false;
        autoLayout = true;
        setNotificationEnabled( false );
        setOutputBrush( new Brush(new Color(248, 190, 133), new Color(250, 210, 200))) ;
        setOutputPen( new Pen(1, new Color(160, 60, 6)) );
        setExpressionFont( new ColorFont("Arial", Font.PLAIN, 12, Color.black)  );
        setDeBrush( new Brush(new Color(176, 196, 222)) );
        setTaskBrush( new Brush(new Color(96, 96, 96), new Color(186, 186, 186)) );
        setCallBrush( new Brush(new Color(146, 223, 253), new Color(205, 241, 253)) );
        setAnalysisPen( new Pen(1, new Color(83, 180, 222)) );
        setExpressionBrush( new Brush(new Color(254, 214, 116), new Color(252, 245, 193)) );
        setExpressionPen( new Pen(1, new Color(236, 158, 29)) );
        setParameterBrush( new Brush(new Color(169, 237, 138), new Color(218, 250, 201)) );
        setParameterPen( new Pen(1, new Color(60, 160, 6)) );
        setNotificationEnabled( true );
    }
    
    @PropertyName("Struct brush")
    @PropertyDescription("Struct brush")
    public Brush getStructBrush()
    {
        return structBrush;
    }
    public void setSmallFont(Brush structBrush)
    {
        Brush oldValue = this.structBrush;
        this.structBrush = structBrush;
        firePropertyChange("structBrush", oldValue, structBrush);
    }

    public Brush getDeBrush()
    {
        return deBrush;
    }
    public void setDeBrush(Brush deBrush)
    {
        Brush oldValue = this.deBrush;
        this.deBrush = deBrush;
        firePropertyChange("deBrush", oldValue, deBrush);
    }

    @PropertyName("Call brush")
    @PropertyDescription("Brush to fill call boxes")
    public Brush getCallBrush()
    {
        return callBrush;
    }
    public void setCallBrush(Brush callBrush)
    {
        Brush oldValue = this.callBrush;
        this.callBrush = callBrush;
        firePropertyChange("callBrush", oldValue, callBrush);
    }
    
    @PropertyName("Task brush")
    @PropertyDescription("Brush to fill task boxes")
    public Brush getTaskBrush()
    {
        return taskBrush;
    }
    public void setTaskBrush(Brush brush)
    {
        Brush oldValue = this.taskBrush;
        this.taskBrush = brush;
        firePropertyChange("taskBrush", oldValue, taskBrush);
    }

    @PropertyName("Expression brush")
    @PropertyDescription("Brush to fill expression boxes")
    public Brush getExpressionBrush()
    {
        return expressionBrush;
    }
    public void setExpressionBrush(Brush expressionBrush)
    {
        Brush oldValue = this.expressionBrush;
        this.expressionBrush = expressionBrush;
        firePropertyChange("expressionBrush", oldValue, expressionBrush);
    }
    
    @PropertyName("Condition brush")
    @PropertyDescription("Brush to fill condition expressions")
    public Brush getConditionBrush()
    {
        return conditionBrush;
    }
    public void setConditionBrush(Brush conditionBrush)
    {
        Brush oldValue = this.conditionBrush;
        this.conditionBrush = conditionBrush;
        firePropertyChange("conditionBrush", oldValue, conditionBrush);
    }
    
    @PropertyName("Conditional block brush")
    @PropertyDescription("Brush to fill conditional blocks")
    public Brush getConditionalBrush()
    {
        return conditionalBrush;
    }

    public void setConditionalBrush(Brush conditionalBrush)
    {
        Brush oldValue = this.conditionalBrush;
        this.conditionalBrush = conditionalBrush;
        firePropertyChange("conditionalBrush", oldValue, conditionalBrush);
    }
    
    @PropertyName("Condition pen")
    @PropertyDescription("Outline of condition boxes")
    public Pen getConditionPen()
    {
        return conditionPen;
    }
    public void setConditionPen(Pen conditionPen)
    {
        Object oldValue = this.conditionPen;
        this.conditionPen = conditionPen;
        firePropertyChange("conditionPen", oldValue, conditionPen);
    }

    @PropertyName("Parameter brush")
    @PropertyDescription("Brush to fill parameter boxes")
    public Brush getParameterBrush()
    {
        return parameterBrush;
    }
    public void setParameterBrush(Brush parameterBrush)
    {
        Brush oldValue = this.parameterBrush;
        this.parameterBrush = parameterBrush;
        firePropertyChange("parameterBrush", oldValue, parameterBrush);
    }

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
    
    @PropertyName("Output brush")
    @PropertyDescription("Brush to fill output boxes")
    public Brush getOutputBrush()
    {
        return outputBrush;
    }
    public void setOutputBrush(Brush outputBrush)
    {
        Brush oldValue = this.outputBrush;
        this.outputBrush = outputBrush;
        firePropertyChange("outputBrush", oldValue, outputBrush);
    }
    
    @PropertyName("Output pen")
    @PropertyDescription("Outline of output boxes")
    public Pen getOutputPen()
    {
        return outputPen;
    }
    public void setOutputPen(Pen outputPen)
    {
        Pen oldValue = this.outputPen;
        this.outputPen = outputPen;
        firePropertyChange("outputPen", oldValue, outputPen);
    }
}
