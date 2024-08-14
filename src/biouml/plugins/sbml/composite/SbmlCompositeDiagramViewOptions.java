package biouml.plugins.sbml.composite;

import java.awt.Color;
import java.awt.Font;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.font.ColorFont;

import biouml.standard.diagram.CompositeDiagramViewOptions;

import com.developmentontheedge.beans.Option;

public class SbmlCompositeDiagramViewOptions extends CompositeDiagramViewOptions
{
    /**
     * Creates <code>CompositeDiagramViewOptions</code> and initializes it.
     *
     * @param parent parent property
     */
    public SbmlCompositeDiagramViewOptions(Option parent)
    {
        super( parent );

        connectionTitleFont = new ColorFont( "Arial", Font.BOLD, 12, Color.black );
        setStateTitleFont( new ColorFont( "Arial", Font.BOLD, 13, Color.red ) );
    }

    protected Brush modelDefBrush = new Brush( new Color( 198, 198, 198 ) );
    public Brush getModelDefBrush()
    {
        return modelDefBrush;
    }
    public void setModelDefBrush(Brush modelDefBrush)
    {
        Brush oldValue = this.modelDefBrush;
        this.modelDefBrush = modelDefBrush;
        firePropertyChange( "modelDefBrush", oldValue, modelDefBrush );
    }

}
