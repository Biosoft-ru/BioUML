package biouml.plugins.physicell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.physicell.cycle.CycleDiagramType;
import biouml.plugins.physicell.cycle.CycleEModel;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.core.CellDefinition;
import ru.biosoft.physicell.core.CycleData;
import ru.biosoft.physicell.core.CycleModel;
import ru.biosoft.physicell.core.Phase;
import ru.biosoft.physicell.core.PhaseLink;
import ru.biosoft.physicell.core.standard.StandardModels;

public class CycleProperties extends Option
{
    private String name;
    private DataElementPath customCycle;
    private TransitionProperties[] transitions = new TransitionProperties[0];
    private PhaseProperties[] phases = new PhaseProperties[0];
    private boolean isDeathModel = false;
    private static Map<String, CycleModel> models;
    private static Map<String, CycleModel> deaths;
    private static boolean basicModelsInit = false;
    private static boolean basicDeathsInit = false;

    private static synchronized void initBasicDeaths() throws Exception
    {
        if( basicDeathsInit )
            return;
        deaths = new HashMap<>();
        for( CycleModel model : StandardModels.getBasicDeathModels() )
            deaths.put( model.name, model );
        basicDeathsInit = true;
    }

    private static synchronized void initBasicModels() throws Exception
    {
        if( basicModelsInit )
            return;
        models = new HashMap<>();
        for( CycleModel model : StandardModels.getBasicModels() )
            models.put( model.name, model );
        basicModelsInit = true;
    }

    public int findPhaseIndex(String name)
    {
        for( int i = 0; i < phases.length; i++ )
        {
            PhaseProperties phase = phases[i];
            if( name.equals( phase.getName() ) )
                return i;
        }
        return -1;
    }

    private void initModels() throws Exception
    {
        initBasicDeaths();
        initBasicModels();
    }

    public CycleProperties(CycleModel model)
    {
        setCycle( model );
    }

    public CycleProperties(CycleModel model, boolean deathModel)
    {
        setCycle( model );
        this.isDeathModel = deathModel;
    }

    public CycleProperties(boolean deathModel)
    {
        try
        {
            this.isDeathModel = deathModel;
            initModels();
            setCycleName( getAvailableModels().findFirst().orElse( null ) );
        }
        catch( Exception ex )
        {
        }
    }

    public CycleProperties()
    {
        try
        {
            this.isDeathModel = false;
            initModels();
            setCycleName( getAvailableModels().findFirst().orElse( null ) );
        }
        catch( Exception ex )
        {
        }
    }

    public CycleProperties clone()
    {
        CycleProperties result = new CycleProperties();
        result.setDeathModel( isDeathModel );
        result.name = name;
        result.phases = new PhaseProperties[phases.length];
        for( int i = 0; i < phases.length; i++ )
            result.phases[i] = phases[i].clone();
        result.transitions = new TransitionProperties[transitions.length];
        for( int i = 0; i < transitions.length; i++ )
            result.transitions[i] = transitions[i].clone();
        return result;
    }

    public void createCycle(CellDefinition cd) throws Exception
    {
        cd.phenotype.cycle = createCycle();
    }

    public CycleModel createCycle() throws Exception
    {
        initModels();
        CycleModel model = this.findModel( name ).clone();
        CycleData data = model.data;
        for( Phase phase : model.phases )
        {
            for( PhaseProperties phaseProperties : phases )
            {
                if( phase.name.equals( phaseProperties.getName() ) )
                {
                    phase.divisionAtExit = phaseProperties.isDivisionAtExit();
                    phase.removalAtExit = phaseProperties.isRemovalAtExit();

                    for( List<PhaseLink> links : model.phaseLinks )
                    {
                        for( PhaseLink link : links )
                        {
                            String startName = link.getStartPhase().name;
                            String endName = link.getEndPhase().name;
                            for( TransitionProperties transition : transitions )
                            {
                                String nameFrom = transition.getFrom();
                                String nameTo = transition.getTo();
                                if( nameFrom.equals( startName ) && nameTo.equals( endName ) )
                                {
                                    link.fixedDuration = transition.isFixed();
                                    double rate = transition.getRate();
                                    int startIndex = findIndex( model.phases, link.getStartPhase() );
                                    int endIndex = findIndex( model.phases, link.getEndPhase() );
                                    data.setTransitionRate( startIndex, endIndex, rate );
                                }
                            }
                        }
                    }
                }
            }
        }
        return model;
    }

    private int findIndex(List<Phase> phases, Phase phase) throws IllegalArgumentException
    {
        if( phase == null )
            throw new IllegalArgumentException( "Can not search for null phase." );
        for( int i = 0; i < phases.size(); i++ )
        {
            if( phase.name.equals( phases.get( i ).name ) )
                return i;
        }
        throw new IllegalArgumentException( "Can not find phase " + phase.name );
    }

    public Stream<String> getAvailableModels()
    {
        try
        {
            initModels();
            if( isDeathModel )
                return StreamEx.of( deaths.keySet() ).append( PhysicellConstants.CUSTOM );
            else
                return StreamEx.of( models.keySet() ).append( PhysicellConstants.CUSTOM );//.stream();
        }
        catch( Exception ex )
        {
            return Stream.empty();
        }
    }

    public void initPhases(CycleModel cycleModel)
    {
        this.setPhases( cycleModel.phases.stream().map( p -> new PhaseProperties( p ) ).toArray( PhaseProperties[]::new ) );
        for( PhaseProperties phase : phases )
            phase.setDeathPhase( this.isDeathModel );

    }

    public void initTransitions(CycleModel cycleModel)
    {
        List<TransitionProperties> propertiesList = new ArrayList<>();
        for( int i = 0; i < cycleModel.phaseLinks.size(); i++ )
        {
            List<PhaseLink> links = cycleModel.phaseLinks.get( i );
            for( int j = 0; j < links.size(); j++ )
            {
                PhaseLink link = links.get( j );
                TransitionProperties properties = new TransitionProperties( link.toShortString() );
                properties.setFrom( link.getStartPhase().name );
                properties.setTo( link.getEndPhase().name );
                properties.setFixed( link.fixedDuration );
                properties.setRate( cycleModel.data.basicRates.get( i ).get( j ) );
                propertiesList.add( properties );
            }
        }
        this.setTransitions( propertiesList.toArray( new TransitionProperties[propertiesList.size()] ) );
    }

    private CycleModel findModel(String name) throws Exception
    {
        initModels();
        if( isDeathModel )
            return deaths.get( name );
        else
            return models.get( name );
    }

    @PropertyName ( "Name" )
    public String getCycleName()
    {
        return name;
    }
    public void setCycleName(String cycleName) throws Exception
    {
        Object oldValue = this.getCycleName();
        if( cycleName.equals( PhysicellConstants.CUSTOM ) )
        {
            this.name = PhysicellConstants.CUSTOM;
            this.transitions = new TransitionProperties[0];
            this.phases = new PhaseProperties[0];
        }
        else
        {
            this.customCycle = null;
            CycleModel cycle = findModel( cycleName );
            this.setCycle( cycle );
        }
        firePropertyChange( "cycleName", oldValue, cycleName );
        firePropertyChange( "*", null, null );
    }

    public void setCycle(CycleModel cycle)
    {
        this.name = cycle.name;
        this.initTransitions( cycle );
        this.initPhases( cycle );
    }

    @PropertyName ( "Transitions" )
    public TransitionProperties[] getTransitions()
    {
        return transitions;
    }
    public void setTransitions(TransitionProperties[] transitions)
    {
        Object oldValue = this.transitions;
        this.transitions = transitions;
        firePropertyChange( "transitions", oldValue, transitions );
    }

    public String getTransitionName(Integer i, Object obj)
    {
        return ( (TransitionProperties)obj ).getTitle();
    }

    @PropertyName ( "Phases" )
    public PhaseProperties[] getPhases()
    {
        return phases;
    }
    public void setPhases(PhaseProperties[] phases)
    {
        this.phases = phases;
    }
    public String getPhaseName(Integer i, Object obj)
    {
        return ( (PhaseProperties)obj ).getName();
    }

    public boolean isDeathModel()
    {
        return isDeathModel;
    }
    public void setDeathModel(boolean isDeathModel)
    {
        this.isDeathModel = isDeathModel;
        for( PhaseProperties phase : this.phases )
            phase.setDeathPhase( isDeathModel );
    }

    public boolean isDefaultCycle()
    {
        return !PhysicellConstants.CUSTOM.equals( getCycleName() );
    }

    @PropertyName ( "Custom cycle" )
    public DataElementPath getCustomCycle()
    {
        return customCycle;
    }

    public void setCustomCycle(DataElementPath customCycle)
    {
        if( customCycle == null || customCycle.isEmpty() )
            return;
        Diagram d = customCycle.getDataElement( Diagram.class );
        if( d.getType() instanceof CycleDiagramType )
        {
            CycleEModel emodel = d.getRole( CycleEModel.class );
            this.setTransitions( emodel.getTransitionProperties() );
            this.setPhases( emodel.getPhaseProperties() );
            this.customCycle = customCycle;
            firePropertyChange( "customCycle", null, null );
            firePropertyChange( "*", null, null );
        }
    }
}