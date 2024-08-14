package biouml.plugins.sbgn;

import java.awt.BasicStroke;
import java.awt.Color;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class SbgnCompositeDiagramViewOptions extends SbgnDiagramViewOptions
{
    protected Brush modelDefinitionBrush = new Brush(new Color(198, 198, 198));
    protected Pen modelDefinitionPen = new Pen(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{9,6}, 0f), Color.black);
    
    @PropertyName("Model definition brush")
    @PropertyDescription("Model definition brush.")
    public Brush getModelDefBrush()
    {
        return modelDefinitionBrush;
    }
    public void setModelDefBrush(Brush modelDefBrush)
    {
        Brush oldValue = this.modelDefinitionBrush;
        this.modelDefinitionBrush = modelDefBrush;
        firePropertyChange("modelDefinitionBrush", oldValue, modelDefinitionBrush);
    }
    
    @PropertyName("Model definition pen")
    @PropertyDescription("Model definition pen.")
    public Pen getModelDefinitionPen()
    {
        return modelDefinitionPen;
    }
    public void setModelDefinitionPen(Pen modelDefinitionPen)
    {
        Pen oldValue = this.modelDefinitionPen;
        this.modelDefinitionPen = modelDefinitionPen;
        firePropertyChange("modelDefinitionPen", oldValue, modelDefinitionPen);
    }
}
