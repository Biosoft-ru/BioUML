package biouml.plugins.physicell;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseListener;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.Role;
import biouml.plugins.physicell.ode.IntracellularODEBioUML;
import biouml.plugins.physicell.ode.IntracellularProperties;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.HypothesisRuleset;
import ru.biosoft.physicell.core.standard.StandardModels;
import ru.biosoft.util.DPSUtils;

@PropertyName ( "Cell Definition" )
@PropertyDescription ( "Cell Definition." )
public class CellDefinitionProperties extends InitialElementPropertiesSupport implements Role, DataCollectionListener
{
    private String name;
    private int initialNumber = 0;
    private String comment;
    private Brush color;
    
    private Node node;
    private boolean isCompleted;
    private CellDefinition cd = new CellDefinition();

    private MotilityProperties motility = new MotilityProperties();
    private MechanicsProperties mechanics = new MechanicsProperties();
    private VolumeProperties volume = new VolumeProperties();
    private GeometryProperties geometry = new GeometryProperties();
    private CycleProperties cycle = new CycleProperties();
    private DeathProperties death = new DeathProperties();
    private FunctionsProperties functions = new FunctionsProperties();
    private SecretionsProperties secretions = new SecretionsProperties();
    private InteractionsProperties interactions = new InteractionsProperties();
    private TransformationsProperties transformations = new TransformationsProperties();
    private CustomDataProperties customData = new CustomDataProperties();
    private IntracellularProperties intracellular = new IntracellularProperties();
    private RulesProperties rules = new RulesProperties();

    protected MouseListener selectionListener;

    public CellDefinitionProperties(String name, CellDefinition cd)
    {
        this( name );
    }

    public void setDefinition(CellDefinition definition)
    {
        cd = definition.clone( name, initialNumber );
        functions = new FunctionsProperties( cd.functions );
        motility = new MotilityProperties( cd.phenotype.motility );
        motility.setDiagramElement( node );

        mechanics = new MechanicsProperties( cd.phenotype.mechanics );
        volume = new VolumeProperties( cd.phenotype.volume );
        geometry = new GeometryProperties( cd.phenotype.geometry );
        customData = new CustomDataProperties( cd.custom_data );
        cycle = new CycleProperties( cd.phenotype.cycle );
        death = new DeathProperties( cd.phenotype.death );
        secretions = new SecretionsProperties( node );
        this.interactions = new InteractionsProperties( node );
        interactions.setDamageRate( cd.phenotype.cellInteractions.damageRate );
        interactions.setDeadPhagocytosisRate( cd.phenotype.cellInteractions.deadPhagocytosisRate );
        this.transformations = new TransformationsProperties( node );
        if( cd.phenotype.intracellular instanceof IntracellularODEBioUML )
            this.intracellular = new IntracellularProperties( (IntracellularODEBioUML)cd.phenotype.intracellular, node );
    }

    public void setRules(HypothesisRuleset ruleset)
    {
        this.rules.init( ruleset );
    }

    public CellDefinitionProperties(DiagramElement de)
    {
        this.name = de.getName();
        this.isCompleted = true;
        setDiagramElement( de );
    }

    public CellDefinitionProperties(String name)
    {
        this.name = name;
        this.isCompleted = false;
    }

    @Override
    public Role clone(DiagramElement de)
    {
        CellDefinitionProperties result = new CellDefinitionProperties( de );

        result.cycle = cycle.clone();
        result.motility = motility.clone( de );
        result.geometry = geometry.clone();
        result.mechanics = mechanics.clone();
        result.functions = functions;
        result.volume = volume.clone();
        result.death = death.clone();
        result.cd = cd.clone( cd.name, cd.type );
        result.initialNumber = initialNumber;
        result.secretions = secretions.clone( de );
        result.interactions = interactions.clone( de );
        result.transformations = transformations.clone( de );
        result.customData = customData.clone();
        result.rules = rules.clone();
        if( result.intracellular != null )
            result.intracellular = intracellular.clone( de );
        return result;
    }

    public boolean isCompleted()
    {
        return isCompleted;
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

    @PropertyName ( "Initial number" )
    public int getInitialNumber()
    {
        return initialNumber;
    }
    public void setInitialNumber(int number)
    {
        this.initialNumber = number;
    }

    @Override
    public DiagramElementGroup doCreateElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
    {
        if( initialNumber < 0 )
            throw new Exception( "Invalid negative initial value of agents " + name + " ( " + initialNumber + " )" );
        CellDefinition cd = StandardModels.createDefaultCellDefinition();
        cd.phenotype.cycle = StandardModels.live.clone();
        this.setDefinition( cd );
        Node result = new Node( compartment, new Stub( null, name, PhysicellConstants.TYPE_CELL_DEFINITION ) );
        result.setLocation( location );
        result.setShapeSize( new Dimension( 75, 75 ) );
        this.isCompleted = true;
        result.setRole( this );
        this.setDiagramElement( result );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( "cellDefinition", CellDefinitionProperties.class, this ) );
        return new DiagramElementGroup( result );
    }

    public void update()
    {
        motility.update();
        secretions.update();
        interactions.update();
        transformations.update();
        intracellular.update();
    }

    @Override
    public Node getDiagramElement()
    {
        return node;
    }
    public void setDiagramElement(DiagramElement de)
    {
        if( this.node != null )
        {
            Diagram d = Diagram.getDiagram( this.node );
            d.removeDataCollectionListener( this );
        }
        this.node = (Node)de;

        Diagram d = Diagram.getDiagram( this.node );
        d.addDataCollectionListener( this );
        de.setRole( this );

        this.intracellular.setDiagramElement( de );
        this.secretions.setDiagramElement( de );
        this.motility.setDiagramElement( de );
        this.interactions.setDiagramElement( de );
        this.transformations.setDiagramElement( de );
        this.rules.setDiagramElement( de );
    }

    public VolumeProperties getVolumeProperties()
    {
        return volume;
    }
    public void setVolumeProperties(VolumeProperties volume)
    {
        this.volume = volume;
    }

    public FunctionsProperties getFunctionsProperties()
    {
        return functions;
    }
    public void setFunctionsProperties(FunctionsProperties functions)
    {
        this.functions = functions;
    }

    public MotilityProperties getMotilityProperties()
    {
        return motility;
    }
    public void setMotilityProperties(MotilityProperties motility)
    {
        this.motility = motility;
    }

    public MechanicsProperties getMechanicsProperties()
    {
        return mechanics;
    }
    public void setMechanicsProperties(MechanicsProperties mechanics)
    {
        this.mechanics = mechanics;
    }

    public CycleProperties getCycleProperties()
    {
        return cycle;
    }
    public void setCycleProperties(CycleProperties cycle)
    {
        this.cycle = cycle;
    }

    public GeometryProperties getGeometryProperties()
    {
        return geometry;
    }
    public void setGeometryProperties(GeometryProperties geometry)
    {
        this.geometry = geometry;
    }

    public DeathProperties getDeathProperties()
    {
        return death;
    }
    public void setDeathProperties(DeathProperties death)
    {
        this.death = death;
    }

    public SecretionsProperties getSecretionsProperties()
    {
        return secretions;
    }
    public void setSecretionsProperties(SecretionsProperties secretions)
    {
        this.secretions = secretions;
    }

    public InteractionsProperties getInteractionsProperties()
    {
        return interactions;
    }
    public void setInteractionsProperties(InteractionsProperties interactions)
    {
        this.interactions = interactions;
    }

    public TransformationsProperties getTransformationsProperties()
    {
        return transformations;
    }
    public void setTransformationsProperties(TransformationsProperties transformations)
    {
        this.transformations = transformations;
    }

    public CustomDataProperties getCustomDataProperties()
    {
        return this.customData;
    }
    public void setCustomDataProperties(CustomDataProperties customData)
    {
        this.customData = customData;
    }

    public IntracellularProperties getIntracellularProperties()
    {
        return this.intracellular;
    }
    public void setIntracellularProperties(IntracellularProperties intracellular)
    {
        this.intracellular = intracellular;
    }

    public RulesProperties getRulesProperties()
    {
        return this.rules;
    }
    public void setRulesProperties(RulesProperties rules)
    {
        this.rules = rules;
    }

    @Override
    public void elementAdded(DataCollectionEvent e) throws Exception
    {
        update();
    }

    @Override
    public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementChanged(DataCollectionEvent e) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void elementRemoved(DataCollectionEvent e) throws Exception
    {
        update();
    }


    @Override
    public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
    {
        // TODO Auto-generated method stub

    }
    
    @PropertyName("Color")
    public Brush getColor()
    {
        return color;
    }
    public void setColor(Brush color)
    {
        this.color = color;
    }

    @PropertyName("Comment")
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        this.comment = comment;
    }
}