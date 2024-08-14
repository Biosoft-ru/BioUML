package biouml.plugins.pharm;

import java.awt.Color;
import java.awt.Font;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewOptions;

@PropertyName("View options")
public class PopulationModelDiagramViewOptions extends DiagramViewOptions
{
    public PopulationModelDiagramViewOptions(Option parent)
    {
        super(parent);
        setDiagramTitleVisible(false);
    }

    public Brush constantBrush = new Brush(Color.white, new Color(255, 255, 153));
    public Brush stochasticBrush = new Brush(Color.white, new Color(255, 153, 153));
    public Brush fucntionBrush = new Brush(Color.white, new Color(204, 229, 255));


    public Pen constantPen = new Pen(1, Color.black);
    public Pen stochasticPen = new Pen(1, Color.black);
    public Pen functionPen = new Pen(1, Color.black);

    protected ColorFont stateTitleFont = new ColorFont("Arial", Font.BOLD, 13, Color.red);
    protected Brush modelBrush = new Brush(Color.white, new Color(135, 206, 250));
    
    protected Brush parameterBrush = new Brush(Color.white, Color.green);
    protected Brush doseBrush = new Brush(Color.white, Color.gray);
    protected Brush observedBrush = new Brush(Color.white, Color.red);
}
