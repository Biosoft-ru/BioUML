package biouml.plugins.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Function;
import one.util.streamex.StreamEx;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;

public class FunctionAnalyzer
{
    /**full list of model functions*/
    private Map<String, FunRef> references;
    private EModel emodel;

    public void init(EModel emodel)
    {
        this.emodel = emodel;
        references = initFunctionReferences();
    }

    public Map<String, Set<Integer>> findDelayedArguments()
    {
        Map<String, Set<Integer>> result = new HashMap<String, Set<Integer>>();
        FunRef delayFunction = references.get( "delay" );
        result.put( "delay", Collections.singleton( 0 ) );

        findDelayedArguments( delayFunction, StreamEx.of( new Integer[] {0} ).toSet(), result );
        return result;

    }

    private Map<String, FunRef> initFunctionReferences()
    {
        Map<String, FunRef> result = new HashMap<>();

        for( Function function : emodel.getFunctions() )
            result.put( function.getName(), new FunRef( function ) );

        result.put( "delay", new FunRef( new Function( null, "function delay(x,y) = x+y" ) ) ); //delay dummy function

        for( FunRef caller : result.values() )
        {
            AstStart math = emodel.readMath( caller.getFunction().getRightHandSide(), caller.getFunction() );
            getFunctionReferences( math, caller, result );
        }

        return result;
    }

    private void getFunctionReferences(Node node, FunRef caller, Map<String, FunRef> references)
    {
        if( node instanceof AstFunNode )
        {
            AstFunNode funNode = (AstFunNode)node;
            String name = funNode.getFunction().getName();

            FunRef callee = references.get( name );
            if( callee == null )
                return;

            for( int j = 0; j < funNode.jjtGetNumChildren(); j++ )
            {
                Node PassedFromCallerArg = funNode.jjtGetChild( j );
                if( PassedFromCallerArg instanceof AstVarNode )
                {
                    String passedFromCallerName = ( (AstVarNode)PassedFromCallerArg ).getName();
                    String[] callerArguments = caller.getFunction().getArguments();
                    for( int i = 0; i < callerArguments.length; i++ )
                    {
                        if( callerArguments[i].equals( passedFromCallerName ) )
                        {
                            callee.addCaller( caller, j, i );
                            break;
                        }
                    }
                }
            }
        }
        else
        {
            for( int j = 0; j < node.jjtGetNumChildren(); j++ )
            {
                getFunctionReferences( node.jjtGetChild( j ), caller, references );
            }
        }
    }

    private void findDelayedArguments(FunRef callee, Set<Integer> args, Map<String, Set<Integer>> result)
    {
        for( FunRef caller : callee.getCallers() )
        {
            for( Integer i : args )
            {
                Set<Integer> callerArguments = callee.getCallerArguments( caller, i );
                result.computeIfAbsent( caller.getName(), k -> new HashSet<>() ).addAll( callerArguments );
                findDelayedArguments( caller, callerArguments, result );
            }
        }
    }

    private class FunRef
    {
        private Function function;
        private Map<FunRef, ArgRef> callers = new HashMap<>();
        
        public FunRef(Function function)
        {
            this.function = function;
        }

        public Function getFunction()
        {
            return function;
        }

        public void addCaller(FunRef caller, Integer argCallee, Integer argCaller)
        {
            callers.computeIfAbsent( caller, k -> new ArgRef() ).addArgument( argCallee, argCaller );
        }

        public Set<FunRef> getCallers()
        {
            return callers.keySet();
        }

        public Set<Integer> getCallerArguments(FunRef caller, Integer argument)
        {
            return callers.get( caller ).getCallerArguments( argument );
        }

        public String getName()
        {
            return function.getName();
        }

        @Override
        public int hashCode()
        {
            return function.getName().hashCode();
        }

        @Override
        public String toString()
        {
            return function.getFormula();
        }

        private class ArgRef
        {
            Map<Integer, Set<Integer>> argumentReplacement = new HashMap<>();

            public Set<Integer> getCallerArguments(Integer argCallee)
            {
                return argumentReplacement.get( argCallee );
            }

            public void addArgument(Integer argCallee, Integer argCaller)
            {
                argumentReplacement.computeIfAbsent( argCallee, k -> new HashSet<Integer>() ).add( argCaller );
            }
        }
    }

    public void substituteDelaysEquations()
    {
        for (Equation eq: emodel.getEquations())
        {
            AstStart math = eq.getMath();
            for (AstFunNode child: Utils.deepChildren( math ).select( AstFunNode.class ))
            {
                Function f =  references.get( child.getFunction().getName() ).getFunction();
                Node replacement = emodel.readMath(f.getRightHandSide(), f);
                substitute(math, f.getName(), replacement.jjtGetChild( 0 ));
            }
            eq.setFormula( new LinearFormatter().format(math)[1] );
        }
    }
    
    public void substituteDelays()
    {
        FunRef delay = references.get( "delay" );
        for( FunRef caller : delay.getCallers() )
            substituteDelays(caller);
    }
    
    private void substituteDelays(FunRef callee)
    {
        for( FunRef caller : callee.getCallers() )
        {
            substitute(caller.getFunction(), callee.getFunction());
            substituteDelays(caller);
        }
    }
    
    public void substitute(Function parent, Function child)
    {
        AstStart parentMath = emodel.readMath( parent.getRightHandSide(), parent );
        AstStart childMath = emodel.readMath( parent.getRightHandSide(), parent );
        substitute(parentMath, child.getName(), childMath.jjtGetChild( 0 ));
        parent.setRightHandSide( new LinearFormatter().format( parentMath )[1] );
    }

    public void substitute(AstStart parent, String toReplace, Node replacement)
    {
        //look for all function calls inside parent that should be replaced
        for( AstFunNode node : Utils.deepChildren( parent ).select( AstFunNode.class )
                .filter( n -> n.getFunction().getName().equals( toReplace ) ) )
        {
            ru.biosoft.math.model.Function f = node.getFunction();

            //retrieve original function (which is called here)
            FunRef funRef = references.get( f.getName() );
            Function function = funRef.getFunction();
            String[] args = function.getArguments();
            
            //form mapping - every argument of called function should be substituted by expression passed to the function when it is called
            Map<String, Node> mapping = new HashMap<String, Node>();
            for( int i = 0; i < node.jjtGetNumChildren(); i++ )
                mapping.put( args[i], node.jjtGetChild( i) );
            
            //use mapping to perform substitution in formula of the called fuction
            Node formula = emodel.readMath( function.getRightHandSide(), function).jjtGetChild( 0 );
            substituteArgs(formula, mapping);
            
            //replace call of this function by its formula (in which arguments are already replaced with passed expressions)
            parent.jjtReplaceChild( node, formula );
        }
    }

    /**
     * Replaces all AstVarNode in given <b>node</b> according to <b>mapping</b>
     * Example: node a*x/(b*(y+x)) with mapping x->p^q, y->0 yields: a*p^q/(b*(0+p^q)  
     */
    public void substituteArgs(Node node, Map<String, Node> mapping)
    {
        for( AstVarNode child : Utils.deepChildren( node ).select( AstVarNode.class ) )
        {
            Node replacement = mapping.get( child.getName() );
            if( replacement != null )
            {
                child.jjtGetParent().jjtReplaceChild( child, replacement.cloneAST() );
            }
        }
    }
}
