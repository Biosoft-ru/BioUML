package biouml.plugins.physicell;

import java.awt.Dimension;
import java.awt.Point;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.Role;
import biouml.plugins.physicell.cycle.CycleConstants;
import biouml.plugins.physicell.cycle.CycleEModel;
import biouml.standard.type.Stub;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.physicell.core.Phase;
import ru.biosoft.physicell.core.PhaseEntry;
import ru.biosoft.util.DPSUtils;

public class PhaseProperties extends InitialElementPropertiesSupport implements Role
{
    private String name;
    private boolean divisionAtExit;
    private boolean removalAtExit;
    private PhaseEntry entryFunction;
    private DiagramElement de;
    private boolean isCompleted = true;
    private boolean isDeathPhase = false;

    public PhaseProperties(boolean isCompleted, boolean isDeath)
    {
        this.isCompleted = isCompleted;
        this.isDeathPhase = isDeath;
    }

    public PhaseProperties()
    {

    }

    public PhaseProperties(Phase phase)
    {
        this.name = phase.name;
        this.divisionAtExit = phase.divisionAtExit;
        this.removalAtExit = phase.removalAtExit;
    }

    public void setDeathPhase(boolean isDeathPhase)
    {
        this.isDeathPhase = isDeathPhase;
    }
    public boolean isDeathPhase()
    {
        return isDeathPhase;
    }
    public boolean isLivePhase()
    {
        return !isDeathPhase;
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

    @PropertyName ( "Title" )
    public String getTitle()
    {
        return name;
    }
    public void setTitle(String name)
    {
    }

    @PropertyName ( "Division on exit" )
    public boolean isDivisionAtExit()
    {
        return divisionAtExit;
    }
    public void setDivisionAtExit(boolean divisionAtExit)
    {
        this.divisionAtExit = divisionAtExit;
    }

    @PropertyName ( "Remove on Exit" )
    public boolean isRemovalAtExit()
    {
        return removalAtExit;
    }
    public void setRemovalAtExit(boolean removalAtExit)
    {
        this.removalAtExit = removalAtExit;
    }

    @PropertyName ( "Entry function" )
    public PhaseEntry getEntryFunction()
    {
        return entryFunction;
    }
    public void setEntryFunction(PhaseEntry entryFunction)
    {
        this.entryFunction = entryFunction;
    }

    public boolean isCompleted()
    {
        return isCompleted;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( c, new Stub( null, name, CycleConstants.TYPE_PHASE ) );
        isDeathPhase = Diagram.getDiagram( c ).getRole( CycleEModel.class ).isDeathModel();
        result.setShapeSize( new Dimension( 75, 75 ) );
        this.isCompleted = true;
        this.de = result;
        result.setRole( this );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( CycleConstants.TYPE_PHASE, PhaseProperties.class, this ) );
        return new DiagramElementGroup( result );
    }

    @Override
    public DiagramElement getDiagramElement()
    {
        return de;
    }
    public void setDiagramElement(DiagramElement de)
    {
        this.de = de;
        isDeathPhase = Diagram.getDiagram( de ).getRole( CycleEModel.class ).isDeathModel();
    }

    @Override
    public Role clone(DiagramElement de)
    {
        PhaseProperties result = new PhaseProperties();
        result.name = name;
        result.divisionAtExit = divisionAtExit;
        result.removalAtExit = removalAtExit;
        result.de = de;
        return result;
    }

    public PhaseProperties clone()
    {
        PhaseProperties result = new PhaseProperties();
        result.name = name;
        result.divisionAtExit = divisionAtExit;
        result.removalAtExit = removalAtExit;
        return result;
    }
}