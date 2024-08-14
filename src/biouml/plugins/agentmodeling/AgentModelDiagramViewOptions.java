package biouml.plugins.agentmodeling;

import biouml.standard.diagram.CompositeDiagramViewOptions;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

import java.awt.Color;
import java.awt.Font;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class AgentModelDiagramViewOptions extends CompositeDiagramViewOptions
{
   private ColorFont scriptFont = new ColorFont("Arial", Font.PLAIN, 9, Color.black);
   private Brush scriptBrush = new Brush(new Color(226, 223, 206));
   private Brush adapterBrush = new Brush(Color.white);
   private Pen scriptPen = new Pen(1, new Color(196, 184, 168));
   
    public AgentModelDiagramViewOptions(Option parent)
    {
        super(parent);
        super.setAutoLayout(true);
    }

    @PropertyName("Script agent font")
    public ColorFont getScriptFont()
    {
        return scriptFont;
    }
    public void setScriptFont(ColorFont scriptFont)
    {
        this.scriptFont = scriptFont;
    }

    @PropertyName("Script agent brush")
    public Brush getScriptBrush()
    {
        return scriptBrush;
    }
    public void setScriptBrush(Brush scriptBrush)
    {
        this.scriptBrush = scriptBrush;
    }

    @PropertyName("Script agent pen")
    public Pen getScriptPen()
    {
        return scriptPen;
    }
    public void setScriptPen(Pen scriptPen)
    {
        this.scriptPen = scriptPen;
    }
    
    @PropertyName("Adapter agent brush")
    public Brush getAdapterBrush()
    {
        return adapterBrush;
    }
    public void setAdapterBrush(Brush adapterBrush)
    {
        this.adapterBrush = adapterBrush;
    }
}
