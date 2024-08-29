package biouml.plugins.physicell;

import java.awt.Dimension;
import java.awt.Point;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.Role;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;

@PropertyName ( "Substrate" )
public class SubstrateProperties extends InitialElementPropertiesSupport implements Role
{
    private DiagramElement diagramElement;
    private boolean isCompleted = true;

    private String name;
    private double initialCondition;
    private double decayRate;
    private double diffusionCoefficient;
    private double dirichletValue;
    private boolean dirichletCondition;

    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private double zMin;
    private double zMax;

    public SubstrateProperties(DiagramElement de)
    {
        this.diagramElement = de;
        this.name = de.getName();
        this.isCompleted = true;
    }

    public SubstrateProperties(String name)
    {
        this.name = name;
        this.isCompleted = false;
    }

    public boolean isCompleted()
    {
        return isCompleted;
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return diagramElement;
    }

    @Override
    public SubstrateProperties clone(DiagramElement de)
    {
        SubstrateProperties result = new SubstrateProperties( de );
        result.initialCondition = initialCondition;
        result.diffusionCoefficient = diffusionCoefficient;
        result.decayRate = decayRate;
        result.xMin = xMin;
        result.xMax = xMax;
        result.yMin = yMin;
        result.yMax = yMax;
        result.zMin = zMin;
        result.zMax = zMax;
//        result.dirichletCondition = dirichletCondition;
//        result.dirichletValue = dirichletValue;
        return result;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( c, new Stub( null, name, PhysicellConstants.TYPE_SUBSTRATE ) );
        result.setShapeSize( new Dimension( 75, 75 ) );
        result.setLocation( location );
        this.isCompleted = true;
        result.setRole( this );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( "substrate", SubstrateProperties.class, this ) );
        return new DiagramElementGroup( result );
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Initial condition" )
    public double getInitialCondition()
    {
        return initialCondition;
    }
    public void setInitialCondition(double initialCondition)
    {
        this.initialCondition = initialCondition;
    }

    @PropertyName ( "Decay rate" )
    public double getDecayRate()
    {
        return decayRate;
    }
    public void setDecayRate(double decayRate)
    {
        this.decayRate = decayRate;
    }

    @PropertyName ( "Diffusion coefficient" )
    public double getDiffusionCoefficient()
    {
        return diffusionCoefficient;
    }
    public void setDiffusionCoefficient(double diffusionCoefficient)
    {
        this.diffusionCoefficient = diffusionCoefficient;
    }

    @PropertyName("X min")
    public double getXMin()
    {
        return xMin;
    }
    public void setXMin(double xMin)
    {
        this.xMin = xMin;
    }
    
    @PropertyName("X max")
    public double getXMax()
    {
        return xMax;
    }
    public void setXMax(double xMax)
    {
        this.xMax = xMax;
    }
    
    @PropertyName("Y min")
    public double getYMin()
    {
        return yMin;
    }
    public void setYMin(double yMin)
    {
        this.yMin = yMin;
    }
    
    @PropertyName("Y max")
    public double getYMax()
    {
        return yMax;
    }
    public void setYMax(double yMax)
    {
        this.yMax = yMax;
    }
    
    @PropertyName("Z min")
    public double getZMin()
    {
        return zMin;
    }
    public void setZMin(double zMin)
    {
        this.zMin = zMin;
    }
    
    @PropertyName("Z max")
    public double getZMax()
    {
        return zMax;
    }
    public void setZMax(double zMax)
    {
        this.zMax = zMax;
    }

    @PropertyName ( "Dirichlet condition" )
    public boolean isDirichletCondition()
    {
        return getXMin() > 0 || getXMax() > 0 || getYMin() > 0 || getYMax() > 0 || getZMin() > 0 || getZMax() > 0;
    }

    public void setDiagramElement(DiagramElement diagramElement)
    {
        this.diagramElement = diagramElement;
    }
}