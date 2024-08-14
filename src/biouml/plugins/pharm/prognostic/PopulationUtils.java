package biouml.plugins.pharm.prognostic;

import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.util.DiagramXmlConstants;
import biouml.plugins.agentmodeling.AgentModelDiagramType;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.DPSUtils;

public class PopulationUtils
{
    public static String SLOW_MODEL = "SlowModel";
    public static String FAST_MODEL = "FastModel";
    public static String COMPOSITE_MODEL = "Composite";

    public static Diagram initCompositeDiagram(Diagram smDiagram, Diagram fmDiagram, DataCollection<?> origin) throws Exception
    {
        Diagram smDiagram_ss = smDiagram.clone( origin, smDiagram.getName() + "_ss" );
        for( Equation eq : smDiagram_ss.getRole( EModel.class ).getInitialAssignments().toArray( Equation[]::new ) )
            smDiagram_ss.getType().getSemanticController().remove( eq.getDiagramElement() );

        Diagram fmDiagram_ss = fmDiagram.clone( origin, fmDiagram.getName() + "_ss" );
        for( Equation eq : fmDiagram_ss.getRole( EModel.class ).getInitialAssignments().toArray( Equation[]::new ) )
            fmDiagram_ss.getType().getSemanticController().remove( eq.getDiagramElement() );

        CollectionFactoryUtils.save( smDiagram_ss );
        CollectionFactoryUtils.save( fmDiagram_ss );

        DiagramType diagramType = new AgentModelDiagramType();
        Diagram diagram = diagramType.createDiagram( origin, COMPOSITE_MODEL, new DiagramInfo( COMPOSITE_MODEL ) );
        CollectionFactoryUtils.save( diagram );

        SubDiagram smSubDiagram_ss = new SubDiagram( diagram, smDiagram_ss, smDiagram_ss.getName() );
        SubDiagram fmSubDiagram_ss = new SubDiagram( diagram, fmDiagram_ss, fmDiagram_ss.getName() );

        diagram.put( smSubDiagram_ss );
        diagram.put( fmSubDiagram_ss );

        for( Node input : smSubDiagram_ss.stream( Node.class ).filter( n -> Util.isOutputPort( n ) ) )
        {
            Node output = fmSubDiagram_ss.findNode( input.getName() );
            DiagramUtility.createConnection( diagram, input, output, true );
        }
        for( Node input : fmSubDiagram_ss.stream( Node.class ).filter( n -> Util.isOutputPort( n ) ) )
        {
            Node output = smSubDiagram_ss.findNode( input.getName() );
            DiagramUtility.createConnection( diagram, input, output, true );
        }

        //Simulator settings
        AgentModelSimulationEngine se = new AgentModelSimulationEngine();
        se.setDiagram( diagram );
        se.setCompletionTime( 240000 );
        se.setTimeIncrement( 6000 );
        se.setLogLevel( Level.SEVERE );
        ( (AgentSimulationEngineWrapper)se.getMainEngine() ).getEngine().setLogLevel( Level.SEVERE );
        for( AgentSimulationEngineWrapper ase : se.getEngines() )
        {
            ase.getEngine().setLogLevel( Level.SEVERE );
            if( ase.getDiagram().getName().equals( smDiagram_ss.getName() ) )
            {
                ase.setTimeIncrement( 100 );
                ase.setTimeScale( 60 );
            }
            if( ase.getDiagram().getName().equals( fmDiagram_ss.getName() ) )
            {
                ase.setStepType( AgentSimulationEngineWrapper.TYPE_STEADY_STATE );
                ase.setControlTimeStart( Double.MAX_VALUE );
                ase.setTimeIncrement( 6000 );
                ase.setTimeScale( 1 );
                SimulationEngine engine = DiagramUtility.getPreferredEngine( fmDiagram_ss );
                ase.setTimeBeforeSteadyState(engine.getCompletionTime());
            }
        }
        diagram.getAttributes()
                .add( DPSUtils.createHiddenReadOnlyTransient( DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, se ) );

        return diagram;
    }
}
