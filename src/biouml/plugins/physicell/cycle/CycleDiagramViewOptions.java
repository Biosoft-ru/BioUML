package biouml.plugins.physicell.cycle;

import java.awt.Color;
import java.awt.Font;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

public class CycleDiagramViewOptions extends DiagramViewOptions
{
    private Pen transitionPen = new Pen( 1, Color.black );
    private Brush phaseBrush = new Brush( Color.green );

    public CycleDiagramViewOptions(Option parent)
    {
        super( parent );
        setNodeTitleFont( new ColorFont( "Courier", Font.BOLD, 16, Color.black ) );
        setDefaultFont( new ColorFont( "Courier", Font.PLAIN, 14, Color.black ) );
        this.setDiagramTitleVisible( false );
    }

    @PropertyName ( "Transition pen" )
    public Pen getTransitionPen()
    {
        return transitionPen;
    }
    public void setTransitionPen(Pen transitionPen)
    {
        this.transitionPen = transitionPen;
    }

    @PropertyName ( "Phase Brush" )
    public Brush getPhaseBrush()
    {
        return phaseBrush;
    }
    public void setPhaseBrush(Brush phaseBrush)
    {
        this.phaseBrush = phaseBrush;
    }

}