package biouml.plugins.bionetgen.diagram;

import java.awt.Color;
import java.awt.Dimension;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.ColorUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.diagram.PathwaySimulationDiagramViewOptions;

@SuppressWarnings ( "serial" )
@PropertyName("View options")
public class BionetgenDiagramViewOptions extends PathwaySimulationDiagramViewOptions
{
    public BionetgenDiagramViewOptions()
    {
        super( null );
        showReactionName = false;
        autoLayout = false;
    }

    private Brush speciesBrush = new Brush( ColorUtils.parsePaint( "#5ABEC8:#3CB4BE" ) );
    private Brush moleculeBrush = new Brush( ColorUtils.parsePaint( "#AAD223:#87C823" ) );
    private Brush observableBrush = new Brush( ColorUtils.parsePaint( "#F5E164:#F5C80A" ) );
    private Brush moleculeTypeBrush = new Brush( ColorUtils.parsePaint( "#F0F0FF:#C8B4FF" ) );
    private final Brush moleculeComponentBrush = new Brush( Color.white );
    private Brush edgeTipBrush = new Brush( ColorUtils.parseColor( "#787878" ) );

    private final Dimension moleculeComponentDefaultSize = new Dimension( 40, 20 );

    private Pen edgePen = new Pen( 1, ColorUtils.parseColor( "#646464" ) );

    private ColorFont moleculeTitleFont = new ColorFont( "Arial", 0, 14, Color.black );
    private ColorFont observableTitleFont = new ColorFont( "Arial", 0, 14, Color.black );
    private ColorFont moleculeTypeTitleFont = new ColorFont( "Arial", 0, 14, Color.black );
    private ColorFont moleculeComponentTitleFont = new ColorFont( "Arial", 0, 14, Color.black );

    @PropertyName ( "Species brush" )
    @PropertyDescription ( "Species brush." )
    public Brush getSpeciesBrush()
    {
        return speciesBrush;
    }
    public void setSpeciesBrush(Brush speciesBrush)
    {
        Object oldValue = this.speciesBrush;
        this.speciesBrush = speciesBrush;
        firePropertyChange( "speciesBrush", oldValue, speciesBrush );
    }

    @PropertyName ( "Molecule brush" )
    @PropertyDescription ( "Molecule brush." )
    public Brush getMoleculeBrush()
    {
        return moleculeBrush;
    }
    public void setMoleculeBrush(Brush moleculeBrush)
    {
        Object oldValue = this.moleculeBrush;
        this.moleculeBrush = moleculeBrush;
        firePropertyChange( "moleculeBrush", oldValue, moleculeBrush );
    }

    @PropertyName ( "Observable brush" )
    @PropertyDescription ( "Observable brush." )
    public Brush getObservableBrush()
    {
        return observableBrush;
    }
    public void setObservableBrush(Brush observableBrush)
    {
        Object oldValue = this.observableBrush;
        this.observableBrush = observableBrush;
        firePropertyChange( "observableBrush", oldValue, observableBrush );
    }

    @PropertyName ( "Molecule type brush" )
    @PropertyDescription ( "Molecule type brush." )
    public Brush getMoleculeTypeBrush()
    {
        return moleculeTypeBrush;
    }
    public void setMoleculeTypeBrush(Brush moleculeTypeBrush)
    {
        Object oldValue = this.moleculeTypeBrush;
        this.moleculeTypeBrush = moleculeTypeBrush;
        firePropertyChange( "moleculeTypeBrush", oldValue, moleculeTypeBrush );
    }

    public Brush getMoleculeComponentBrush()
    {
        return moleculeComponentBrush;
    }

    @PropertyName ( "Edge tip brush" )
    @PropertyDescription ( "Edge tip brush." )
    public Brush getEdgeTipBrush()
    {
        return edgeTipBrush;
    }
    public void setEdgeTipBrush(Brush edgeTipBrush)
    {
        Object oldValue = this.edgeTipBrush;
        this.edgeTipBrush = edgeTipBrush;
        firePropertyChange( "edgeTipBrush", oldValue, edgeTipBrush );
    }

    public Dimension getMoleculeComponentDefaultSize()
    {
        return moleculeComponentDefaultSize;
    }

    @PropertyName ( "Edge pen" )
    @PropertyDescription ( "Pen for edges." )
    public Pen getEdgePen()
    {
        return edgePen;
    }
    public void setEdgePen(Pen edgePen)
    {
        Object oldValue = this.edgePen;
        this.edgePen = edgePen;
        firePropertyChange( "edgePen", oldValue, edgePen );
    }

    @PropertyName ( "Molecule title font" )
    @PropertyDescription ( "Molecule title font." )
    public ColorFont getMoleculeTitleFont()
    {
        return moleculeTitleFont;
    }
    public void setMoleculeTitleFont(ColorFont moleculeTitleFont)
    {
        Object oldValue = this.moleculeTitleFont;
        this.moleculeTitleFont = moleculeTitleFont;
        firePropertyChange( "moleculeTitleFont", oldValue, moleculeTitleFont );
    }

    @PropertyName ( "Observable title font" )
    @PropertyDescription ( "Observable title font." )
    public ColorFont getObservableTitleFont()
    {
        return observableTitleFont;
    }
    public void setObservableTitleFont(ColorFont observableTitleFont)
    {
        Object oldValue = this.observableTitleFont;
        this.observableTitleFont = observableTitleFont;
        firePropertyChange( "observableTitleFont", oldValue, observableTitleFont );
    }

    @PropertyName ( "Molecule type title font" )
    @PropertyDescription ( "Molecule type title font." )
    public ColorFont getMoleculeTypeTitleFont()
    {
        return moleculeTypeTitleFont;
    }
    public void setMoleculeTypeTitleFont(ColorFont moleculeTypeTitleFont)
    {
        Object oldValue = this.moleculeTypeTitleFont;
        this.moleculeTypeTitleFont = moleculeTypeTitleFont;
        firePropertyChange( "moleculeTypeTitleFont", oldValue, moleculeTypeTitleFont );
    }

    @PropertyName ( "Molecule component title font" )
    @PropertyDescription ( "Molecule component title font." )
    public ColorFont getMoleculeComponentTitleFont()
    {
        return moleculeComponentTitleFont;
    }
    public void setMoleculeComponentTitleFont(ColorFont moleculeComponentTitleFont)
    {
        Object oldValue = this.moleculeComponentTitleFont;
        this.moleculeComponentTitleFont = moleculeComponentTitleFont;
        firePropertyChange( "moleculeComponentTitleFont", oldValue, moleculeComponentTitleFont );
    }
}
