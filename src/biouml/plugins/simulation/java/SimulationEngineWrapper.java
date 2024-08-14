package biouml.plugins.simulation.java;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstFunctionDeclaration;
import ru.biosoft.math.model.AstPiece;
import ru.biosoft.math.model.AstPiecewise;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Function;
import biouml.plugins.simulation.EventPreprocessor;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.SimulationEngine;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.Util;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class SimulationEngineWrapper
{

    OdeSimulationEngine engine;
    public static final int MAX_BYTE_CODE_PER_METHOD = 64000;
    //private static final int BYTE_CODE_PER_METHOD = 41;
    public static final int BYTE_CODE_PER_CALL = 3;
    public static final int BYTE_CODE_PER_ASSIGNMENT = 8;
    public static final int BYTE_CODE_PER_VARIABLE = 4;
    public static final int BYTE_CODE_PER_CONST = 4;
    public static final int BYTE_CODE_PER_FUNCTION = 3;
    public static final int BYTE_CODE_PER_EMBEDDED_OPERATION = 1;
    public static final int BYTE_CODE_PER_CONDITION = 7;
    public static final int BYTE_CODE_PER_ARRAY_VARIABLE = 5;
    
    public SimulationEngineWrapper(OdeSimulationEngine engine)
    {
        this.engine = engine;
    }

    public SimulationEngineWrapper()
    {

    }

    public OdeSimulationEngine getEngine()
    {
        return engine;
    }

    public SimulationEngine prepareEngine(Diagram diagram, String clazz) throws Exception
    {
        return this.prepareEngine( diagram, false, clazz );
    }

    public SimulationEngine prepareEngine(Diagram diagram) throws Exception
    {
        return this.prepareEngine( diagram, false, "biouml.plugins.simulation.java.JavaSimulationEngine" );
    }

    public SimulationEngine prepareEngine(Diagram diagram, boolean largeTemplate, String clazz) throws Exception
    {
        engine = (OdeSimulationEngine)ClassLoading.loadClass( clazz ).newInstance();
        engine.setDiagram( diagram );
        if( largeTemplate )
            engine.setGenerateVariableAsArray( OdeSimulationEngine.ARRAY_MODE_ON );
        engine.init();
        return engine;
    }

    // EModel ////////////////////////////////////////////////
    public int getEMODEL_ODE_TYPE()
    {
        return EModel.ODE_TYPE;
    }

    public int getEMODEL_ODE_DELAY_TYPE()
    {
        return EModel.ODE_DELAY_TYPE;
    }

    public int getEMODEL_ALGEBRAIC_TYPE()
    {
        return EModel.ALGEBRAIC_TYPE;
    }

    public int getEMODEL_EVENT_TYPE()
    {
        return EModel.EVENT_TYPE;
    }

    public int getEMODEL_STATE_TRANSITION_TYPE()
    {
        return EModel.STATE_TRANSITION_TYPE;
    }

    public boolean isOfType(int type, int suggestedType)
    {
        return ( type & suggestedType ) != 0;
    }
    //////////////////////////////////////////////////////////

    // Equation //////////////////////////////////////////////

    public boolean isEQUATION_TYPE_SCALAR_DELAYED(String type)
    {
        return type.equals(Equation.TYPE_SCALAR_DELAYED);
    }

    public boolean isEQUATION_TYPE_SCALAR_INTERNAL(String type)
    {
        return type.equals(Equation.TYPE_SCALAR_INTERNAL);
    }
    
    public boolean isEQUATION_TYPE_SCALAR(String type)
    {
        return type.equals(Equation.TYPE_SCALAR);
    }
    
    public boolean isEQUATION_TYPE_INITIAL_VALUE(String type)
    {
        return type.equals(Equation.TYPE_INITIAL_VALUE);
    }

    public boolean istEQUATION_TYPE_CONST(String type)
    {
        return type.equals(Equation.TYPE_CONST);
    }
    
    public boolean isDelayedEvent(Event event)
    {
        String val = event.getDiagramElement().getAttributes().getValueAsString( EventPreprocessor.EVENT_TYPE );
        return EventPreprocessor.DELAY_ASSIGNMENT_TYPE.equals(val);
    }
    
    public boolean isTriggerForDelayedEvent(Event event)
    {
        String val = event.getDiagramElement().getAttributes().getValueAsString( EventPreprocessor.EVENT_TYPE );
        return EventPreprocessor.DELAY_TRIGGER_TYPE.equals(val);
    }
    //////////////////////////////////////////////////////////

    public boolean isSpecieReference(Edge edge)
    {
        return edge.getKernel() != null && edge.getKernel() instanceof SpecieReference;
    }

    public boolean isVariable(Node node)
    {
        return node.getRole() != null && node.getRole() instanceof Variable;
    }
    
    public boolean isRate(Equation equation)
    {
        return Equation.isRate( equation.getType() );
    }
    
    public boolean isFast(Equation equation)
    {
        if( equation.getDiagramElement() instanceof Edge )
            return ( (Edge)equation.getDiagramElement() ).nodes().map(n -> n.getKernel()).select(Reaction.class).anyMatch(r -> r.isFast());
        return false;
    }
    
    public boolean hasFastReactions()
    {
        return engine.getDiagram().recursiveStream().anyMatch(de->Util.isFastReaction(de));
    }

    public boolean isScalar(Equation equation)
    {
        return Equation.isScalar( equation.getType() );
    }
    
    public boolean isMolar(String name)
    {
        Variable var = engine.getDiagram().getRole( EModel.class ).getVariable( name );
        if( var.getUnits().contains( "mole" ) )
        {
            return true;
        }
        return false;
    }

    public boolean isCompartment(Variable var)
    {
        return var instanceof VariableRole && ((VariableRole)var).getDiagramElement() instanceof Compartment;
    }
    
    public boolean isInitialConcentration(Variable var)
    {
        return var instanceof VariableRole && ((VariableRole)var).getInitialQuantityType() == VariableRole.CONCENTRATION_TYPE;
    }
    
    public boolean isOutputConcentration(Variable var)
    {
        return var instanceof VariableRole && ((VariableRole)var).getOutputQuantityType() == VariableRole.CONCENTRATION_TYPE;
    }
    
    public VariableRole getCompartmentVariable(Variable var)
    {
        if (var instanceof VariableRole)
        {
            Compartment compartment = ((VariableRole)var).getDiagramElement().getCompartment();
            if (compartment.getRole() instanceof VariableRole)
                return (VariableRole)compartment.getRole();   
        }
        return null;
    }
    
    public String getCode(double value)
    {
        if( value == Double.NEGATIVE_INFINITY )
            return "Double.NEGATIVE_INFINITY";
        if( value == Double.POSITIVE_INFINITY )
            return "Double.POSITIVE_INFINITY";
        if( Double.isNaN(value) )
            return "Double.NaN";
        return String.valueOf(value);
    }


    /**
     * Estimate the byte-code size corresponding to the given AST.
     * 
     * @param node
     * @return
     */
    public int estimateByteCodeLength(ru.biosoft.math.model.Node node)
    {
        return estimateByteCodeLength(node, 0);
    }


    /**
     * Estimate the byte-code size corresponding to the given AST.
     * 
     * @param node
     * @param additionalVariableBytes how many bytes should be added per variable access
     * @return
     */
    public int estimateByteCodeLength(ru.biosoft.math.model.Node node, int additionalVariableBytes)
    {
        if( node instanceof AstVarNode )
        {
            String variableName = ( (AstVarNode)node ).getName();
            return estimateVariableByteCodeLength(variableName)+additionalVariableBytes;
        }
    
        if( node instanceof AstConstant )
        {
            return estimateConstantByteCodeLength(((AstConstant)node).getValue());
        }
    
        if( node instanceof AstFunNode )
        {
            int whole_length;
            if( ( (AstFunNode)node ).getFunction().getPriority() == Function.FUNCTION_PRIORITY || 
                    ( (AstFunNode)node ).getFunction().getPriority() == Function.POWER_PRIORITY)
            {
                whole_length = BYTE_CODE_PER_CALL;
                if(((AstFunNode)node).getFunction() instanceof AstFunctionDeclaration)
                {
                    whole_length += additionalVariableBytes;
                }
            } else
            {
                whole_length = BYTE_CODE_PER_EMBEDDED_OPERATION;
            }
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
                whole_length += estimateByteCodeLength(node.jjtGetChild(i), additionalVariableBytes);

            return whole_length;
        }
    
        if( node instanceof AstPiecewise )
        {
            int whole_length = BYTE_CODE_PER_CALL;
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
                whole_length += estimateByteCodeLength(node.jjtGetChild(i), additionalVariableBytes);
    
            return whole_length;
        }
    
        if( node instanceof AstStart )
        {
            int whole_length = 0;
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
                whole_length += estimateByteCodeLength(node.jjtGetChild(i), additionalVariableBytes);
    
            return whole_length;
        }
    
        if( node instanceof AstPiece )
            return BYTE_CODE_PER_CONDITION;
    
        return 0;
    }


    public int estimateVariableByteCodeLength(String variableName)
    {
        String codeName = engine.getVariableCodeName(variableName);
        if( codeName.contains("[") )
        {
            int pos1 = codeName.indexOf("[");
            int pos2 = codeName.indexOf("]");
            return estimateConstantByteCodeLength(Integer.parseInt(codeName.substring(pos1+1, pos2)))+BYTE_CODE_PER_ARRAY_VARIABLE;
        }
        else
            return BYTE_CODE_PER_VARIABLE;
    }

    public int estimateConstantByteCodeLength(Object constant)
    {
        if( constant instanceof Integer )
        {
            Integer intConstant = (Integer)constant;
            if( ( -1 <= intConstant ) && ( intConstant <= 5 ) )
                return 1;
            if( ( Byte.MIN_VALUE <= intConstant ) && ( intConstant <= Byte.MAX_VALUE ) )
                return 2;
            return 3;
        }
        else if( constant instanceof Double )
        {
            Double doubleConstant = (Double)constant;
            if( ( doubleConstant == 0 ) || ( doubleConstant == 1 ) )
                return 1;
            return 3;
        }
        return BYTE_CODE_PER_CONST;
    }
    
    public StringBuilder createStringBuilder()
    {
        return new StringBuilder();
    }
    
    public boolean isTerminal(Event event)
    {
        return Boolean.TRUE.equals(event.getDiagramElement().getAttributes().getValue("Terminal"));
    }
}
