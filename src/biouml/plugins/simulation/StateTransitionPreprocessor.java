package biouml.plugins.simulation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.SemanticController;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.Variable;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.ParserTreeConstants;

/**
 * @author tolstyh
 * Converts states and transitions into events
 */
public class StateTransitionPreprocessor extends Preprocessor
{
    protected static final String CURRENT_STATE_NAME = "currentState";
    protected static final String CURRENT_STATE_TIME_NAME = "currentStateTime";

    protected Map<String, Integer> statesNumbers = new HashMap<>();
    protected int stateValue = 0;
    protected double startState = 0.0;
    
    public Map<String, Integer> getStateIndexMapping()
    {
        return statesNumbers;
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        EModel emodel = diagram.getRole( EModel.class );
        for (State state: emodel.getStates())
            registerState(state);
        
        Transition[] transitions = emodel.getTransitions();
        if( transitions.length > 0 )
        {
            String stateVarName = generateUniqueVar(emodel, CURRENT_STATE_NAME);
            String stateTimeVarName = generateUniqueVar(emodel, CURRENT_STATE_TIME_NAME);

            for( Transition st : transitions )
            {
                AstStart trigger = emodel.readMath(st.getWhen(), st);
                if( trigger == null )
                {
                    trigger = new AstStart(ParserTreeConstants.JJTSTART);                   
                    Node astVal1 = emodel.readMath(st.getAfter(), st).jjtGetChild(0);
                    Node plus = Utils.applyPlus( new Node[] {astVal1, Utils.createVariabl( stateTimeVarName )} );                    
                    Node fun = Utils.applyFunction( new Node[] {Utils.createVariabl( "time" ), plus}, new PredefinedFunction(">", Function.RELATIONAL_PRIORITY, -1) );                                       
                    trigger.jjtAddChild(fun, 0);
                }                                             
                Node eq = Utils.applyFunction( new Node[] {Utils.createVariabl( stateVarName ), Utils.createConstant( statesNumbers.get( st.getFrom().getName() ) )}, new PredefinedFunction("==", Function.RELATIONAL_PRIORITY, -1) );
                Node and = Utils.applyFunction( new Node[] {eq, trigger.jjtGetChild( 0 )}, new PredefinedFunction("&&", Function.RELATIONAL_PRIORITY, -1) );     
                AstStart updatedTrigger = new AstStart( ParserTreeConstants.JJTSTART );
                updatedTrigger.jjtAddChild( and, 0 );

                List<Assignment> assignments = new ArrayList<>();
                for( Assignment assignment : st.getFrom().getOnExitAssignment() )
                    assignments.add( assignment );

                Node constant = Utils.createConstant( statesNumbers.get( st.getTo().getName() ) );
                String line = new LinearFormatter().format( Utils.createStart( constant ) )[1];
                assignments.add( new Assignment( stateVarName, line ) );

                AstVarNode timeNode = Utils.createVariabl( "time" );
                line = new LinearFormatter().format( Utils.createStart( timeNode ) )[1];                
                assignments.add( new Assignment( stateTimeVarName, line ) );

                for( Assignment assignment : st.getTo().getOnEntryAssignment() )
                    assignments.add( assignment );

                SemanticController semanticController = diagram.getType().getSemanticController();
                biouml.model.Node eventNode = (biouml.model.Node)semanticController.createInstance( diagram, Event.class,
                        new Point(), null ).getElement();
                diagram.put( eventNode );
                semanticController.remove( st.getDiagramElement() );
                Event event = eventNode.getRole( Event.class );
                event.setTrigger( new LinearFormatter().format( updatedTrigger )[1] );
                event.setEventAssignment( assignments.toArray( new Assignment[assignments.size()] ) );
            }
            //add additional variables
            Variable stateVar = new Variable(stateVarName, emodel, null);
            stateVar.setInitialValue(startState);
            emodel.put(stateVar);
            Variable stateTimeVar = new Variable(stateTimeVarName, emodel, null);
            emodel.put(stateTimeVar);
        }
        return diagram;
    }
    
    private static String generateUniqueVar(EModel emodel, String varName)
    {
        emodel.declareVariable( varName, 1.0 );
        String result = varName;
        int i = 1;
        while( emodel.containsVariable( result ) )
            result = varName + ( i++ );
        return result;
    }

    protected void registerState(State state)
    {
        String name = state.getDiagramElement().getName();
        if( !statesNumbers.containsKey( name ) )
        {
            statesNumbers.put(name, stateValue++);
            if( state.isStart() )
                startState = stateValue;            
        }
    }
}