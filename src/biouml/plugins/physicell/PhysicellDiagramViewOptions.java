package biouml.plugins.physicell;

import java.awt.Color;
import java.awt.Font;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

public class PhysicellDiagramViewOptions extends DiagramViewOptions
{
    private Brush cellDefinitionBrush = new Brush( new Color( 127, 127, 255 ) );
    private Brush substanceBrush = new Brush( new Color( 255, 127, 127 ) );

    private Pen secretionPen = new Pen( 1, new Color( 127, 127, 127 ) );
    private Pen chemotaxisPen = new Pen( 1, Color.magenta );
    private Pen interactionPen = new Pen( 1, new Color( 127, 0, 0 ) );
    private Pen transformationPen = new Pen( 1, new Color( 0, 0, 127 ) );

    public PhysicellDiagramViewOptions(Option parent)
    {
        super( parent );
        this.setDiagramTitleVisible( false );
        this.setNodeTitleFont( new ColorFont( "Arial", Font.BOLD, 14, Color.black ) );
    }

    @PropertyName ( "Cell Definition brush" )
    @PropertyDescription ( "Brush for Cell Definitions" )
    public Brush getCellDefinitionBrush()
    {
        return cellDefinitionBrush;
    }
    public void setCellDefinitionBrush(Brush cellDefinitionBrush)
    {
        Brush oldValue = this.cellDefinitionBrush;
        this.cellDefinitionBrush = cellDefinitionBrush;
        firePropertyChange( "cellDefinitionBrush", oldValue, cellDefinitionBrush );
    }

    @PropertyName ( "Substance brush" )
    @PropertyDescription ( "Brush for substances" )
    public Brush getSubstanceBrush()
    {
        return substanceBrush;
    }
    public void setSubstanceBrush(Brush substanceBrush)
    {
        Brush oldValue = this.substanceBrush;
        this.substanceBrush = substanceBrush;
        firePropertyChange( "substanceBrush", oldValue, substanceBrush );
    }

    @PropertyName ( "Secretion edge pen" )
    @PropertyDescription ( "Pen for secretion edges." )
    public Pen getSecretionPen()
    {
        return secretionPen;
    }
    public void setSecretionPen(Pen secretionPen)
    {
        this.secretionPen = secretionPen;
    }

    @PropertyName ( "Chemotaxis edge pen" )
    @PropertyDescription ( "Pen for chemotaxis edges." )
    public Pen getChemotaxisPen()
    {
        return chemotaxisPen;
    }
    public void setChemotaxisPen(Pen chemotaxisPen)
    {
        this.chemotaxisPen = chemotaxisPen;
    }

    @PropertyName ( "Interaction edge pen" )
    @PropertyDescription ( "Pen for interaction edges." )
    public Pen getInteractionPen()
    {
        return interactionPen;
    }
    public void setInteractionPen(Pen interactionPen)
    {
        this.interactionPen = interactionPen;
    }


    @PropertyName ( "Transformation edge pen" )
    @PropertyDescription ( "Pen for transformation edges." )
    public Pen getTransformationPen()
    {
        return transformationPen;
    }
    public void setTransformationPen(Pen transformationPen)
    {
        this.transformationPen = transformationPen;
    }
}