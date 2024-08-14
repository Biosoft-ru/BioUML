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
        result.dirichletCondition = dirichletCondition;
        result.dirichletValue = dirichletValue;
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


    public double getxMin()
    {
        return xMin;
    }
    public void setxMin(double xMin)
    {
        this.xMin = xMin;
    }
    public double getxMax()
    {
        return xMax;
    }
    public void setxMax(double xMax)
    {
        this.xMax = xMax;
    }
    public double getyMin()
    {
        return yMin;
    }
    public void setyMin(double yMin)
    {
        this.yMin = yMin;
    }
    public double getyMax()
    {
        return yMax;
    }
    public void setyMax(double yMax)
    {
        this.yMax = yMax;
    }
    public double getzMin()
    {
        return zMin;
    }
    public void setzMin(double zMin)
    {
        this.zMin = zMin;
    }
    public double getzMax()
    {
        return zMax;
    }
    public void setzMax(double zMax)
    {
        this.zMax = zMax;
    }

    @PropertyName ( "Dirichlet condition" )
    public boolean isDirichletCondition()
    {
        return dirichletCondition;
    }
    public void setDirichletCondition(boolean dirichletCondition)
    {
        this.dirichletCondition = dirichletCondition;
    }

    @PropertyName ( "Dirichlet value" )
    public double getDirichletValue()
    {
        return dirichletValue;
    }
    public void setDirichletValue(double dirichletValue)
    {
        this.dirichletValue = dirichletValue;
    }

    public void setDiagramElement(DiagramElement diagramElement)
    {
        this.diagramElement = diagramElement;
    }
}