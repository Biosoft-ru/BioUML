package biouml.plugins.brain.sde;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.EModel.DelayedExpression;
import biouml.model.dynamics.Equation;
import biouml.plugins.brain.sde.SdeEModel.StochasticExpression;
import biouml.plugins.simulation.BooleanPreprocessor;
import biouml.plugins.simulation.ConstraintPreprocessor;
import biouml.plugins.simulation.DelayPreprocessor;
import biouml.plugins.simulation.EmptyMathPreprocessor;
import biouml.plugins.simulation.EquationTypePreprocessor;
import biouml.plugins.simulation.EventPreprocessor;
import biouml.plugins.simulation.FastReactionPreprocessor;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Preprocessor;
import biouml.plugins.simulation.RateAssignmentRulePreprocessor;
import biouml.plugins.simulation.RateOfPreprocessor;
import biouml.plugins.simulation.ScalarCyclesPreprocessor;
import biouml.plugins.simulation.SimulationEngineLogger;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.StateTransitionPreprocessor;
import biouml.plugins.simulation.StaticModelPreprocessor;
import biouml.plugins.simulation.TableElementPreprocessor;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import one.util.streamex.EntryStream;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.math.model.ASTVisitorSupport;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Formatter;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;
//import ru.biosoft.math.model.SimpleNode;
import ru.biosoft.util.ApplicationUtils;

//import ru.biosoft.math.parser.Parser;

@PropertyName ("Java SDE simulation engine")
@PropertyDescription ("Stochastic simulation engine to solve system of stochastic differential equations.")
public class JavaSdeSimulationEngine extends JavaSimulationEngine
{   
    private int seed = 0;
    private boolean customSeed = false;
    
    /**Mapping between variable CODE name and its index in stochastic array*/
    protected Map<String, Integer> varStochasticIndexMapping;

    /**Mapping between variable name and its index in stochastic array*/
    protected Map<String, Integer> varNameStochasticIndexMapping;
    
    public JavaSdeSimulationEngine()
    {
        simulatorType = "JAVA_SDE";
        log = new SimulationEngineLogger(biouml.plugins.simulation.java.MessageBundle.class.getName(), getClass());
        EventLoopSimulator javaSimulator = new EventLoopSimulator();
        javaSimulator.setSolver(new EulerStochastic());
        simulator = javaSimulator;
    }
    
    @Override
    public void init() throws Exception
    {
        super.init();
        initStochasticEquations();
    }
	
    @Override
    public Formatter getFormatter()
    {
        return new JavaSdeFormatter(varHistoricalIndexMapping, varStochasticIndexMapping, varNameStochasticIndexMapping);
    }
    
    @Override
    public List<Preprocessor> getDiagramPreprocessors()
    {
        //TODO: check that preprocessors order is correct
        List<Preprocessor> preprocessors = new ArrayList<>();
        preprocessors.add( new EmptyMathPreprocessor() );
        preprocessors.add( new ConstraintPreprocessor(constraintsViolation) );
        preprocessors.add( new BooleanPreprocessor() );
        preprocessors.add( new RateOfPreprocessor() );
        preprocessors.add( new StateTransitionPreprocessor() );
        preprocessors.add( new EventPreprocessor() );
        preprocessors.add( new EquationTypePreprocessor() );
        preprocessors.add( new StaticModelPreprocessor() );
        preprocessors.add( new FastReactionPreprocessor(fastReactions) );
        preprocessors.add( new DelayPreprocessor() );
        
        preprocessors.add(new StochasticPreprocessor());
        
        preprocessors.add( new TableElementPreprocessor() );
        preprocessors.add( new RateAssignmentRulePreprocessor() );
        preprocessors.add( new ScalarCyclesPreprocessor() );
        return preprocessors;
    }
    
    @Override
    public String simulate(Model model, ResultListener[] listeners) throws Exception
    {
        if (model == null) 
        {
            return "Can not simulate, model is null!";
        }

        if(!(model instanceof SdeModel))
        {
            throw new IllegalArgumentException( "Incorrect model class for JavaSdeSimulationEngine "
                    + (model == null ? null : model.getClass()) + ". Only SdeModel allowed" );   
        }

        simulator.setLogLevel(log.getLogger().getLevel());

        log.info("Model " + diagram.getName() + ": simulation started.");

        if(listeners != null) 
        {
            for(ResultListener listener : listeners)
            {
                listener.start(model); 
            }
        }

        if(span == null)
        {
            resetSpan();   
        }

        if (simulator instanceof SimulatorSupport)
        {
            ((SimulatorSupport)simulator).setPresimulateFastReactions(fastReactions.equals(ODE_SYSTEM) && Util.hasFastReactions(diagram));
        }
            
        ((JavaBaseModel)model).setAeSolver(getAlgebraicSolver());
        
        try
        {
            initializeSeed(model);
            simulator.start(model, span, listeners, jobControl);
        }
        catch(Throwable t)
        {
            log.info("Model " + diagram.getName() + ": simulation terminated with error.");
            log.info(t.getMessage());
            log.info("");
            return "Simulation error: " + t.getMessage();
        }
        finally
        {
            //if we do not do this then engine have changed diagram before next simulation and when user specify span properties (e.g. time step)
            //then this properties goes to changed diagram, and original is not updated. Maybe we should detach these properties from the diagram
            //and make them just properties of simulation engine
            restoreOriginalDiagram();
        }
        
        checkVariables(((JavaBaseModel)model).getCurrentState(), EntryStream.of(varIndexMapping).invert().toMap());

        log.info("Model " + diagram.getName() + ": simulation finished.");
        log.info("");

        if(simulator.getProfile().isStiff()) 
        {
            return STIFF_PROBLEM; //Special text for indicating test result as "stiffness detected" 
        }
        else if(simulator.getProfile().isUnstable())
        {
            return UNSTABLE_PROBLEM;
        }
        return simulator.getProfile().getErrorMessage();
    }
    
    @Override
    public Model createModel() throws Exception
    {	
        SdeModel model = (SdeModel)doGetModel();
        model.setNameToIndex(getGlobalIndexMap());
        return model;
    }
    
    @Override
    public @Nonnull File[] generateModel(boolean forceRewrite) throws Exception
    {
    	setLargeTemplate();
    	setGenerateVariableAsArray(ARRAY_MODE_ON);
    	
    	return super.generateModel(forceRewrite);
    }
    
    @Override
    protected InputStream getTemplateInputStream()
    {
        String templatePath = containsStochastic() ? "resources/sdeStochasticDelayLargeModelTemplate.vm" : "resources/sdeDelayLargeModelTemplate.vm";
    	//String templatePath = "resources/sdeParallelLargeModelTemplate.vm";

        log.info("Generating code with template " + templatePath);
        return JavaSdeSimulationEngine.class.getResourceAsStream(templatePath);
    }
    
    @Override
    protected List<PluginEntry> getClassPathEntries()
    {
        List<PluginEntry> result = new ArrayList<>(super.getClassPathEntries());
        try
        {
            result.add(ApplicationUtils.resolvePluginPath("biouml.plugins.brain:src.jar"));
        }
        catch( Exception e )
        {
        }
        return result;
    }
    
    /**
     * Is used for model generated by large template. It needs mapping between variable name and index in global array ("var")
     */
    private Map<String, Integer> getGlobalIndexMap()
    {
        return EntryStream.of(varNameMapping).filter(e -> e.getValue().startsWith("var"))
                .mapValues(val -> Integer.parseInt(val.substring(4, val.length() - 1))).toMap();
    }
    
    private void initStochasticEquations()
    {
        varStochasticIndexMapping = new HashMap<>();
        varNameStochasticIndexMapping = new HashMap<>();
        int stochasticVariableNumber = 0;
        if (containsStochastic())
        {
            List<StochasticExpression> stochasticExpressions = ((SdeEModel)executableModel).getStochasticExpressionsList();
            for (StochasticExpression expression : stochasticExpressions)
            {
                String varName = expression.varName;
                final String codeName = varNameMapping.get(varName);
                Variable var = executableModel.getVariable(varName);
                if (codeName != null && varName != null && !varStochasticIndexMapping.containsKey(codeName))
                {
                    varStochasticIndexMapping.put(codeName, stochasticVariableNumber);
                    varNameStochasticIndexMapping.put(var.getName(), stochasticVariableNumber);
                    stochasticVariableNumber++;
                }
            }
        }
        
    }
    
    /**Returns true if the equations contain stochastic terms.*/
    public boolean containsStochastic()
    {
        return EModel.isOfType(modelType, SdeEModel.SDE_TYPE)
                || EModel.isOfType(modelType, SdeEModel.STOCHASTIC_TYPE);
    }
    
    
    /**
     * Builds map with definitions of scalar-like equations (including initial assignments and stochastic scalars).
     * This map is used by expression extractors to inline scalar definitions in differential equations when needed.
     */
    private Map<String, AstStart> buildScalarDefinitionsMap()
    {
        SdeEModel model = (SdeEModel)getDiagram().getRole();
        Map<String, AstStart> scalarDefs = new HashMap<>();

        for (Equation eq : model.getEquations().toList())
        {
            String type = eq.getType();
            if (Equation.TYPE_SCALAR.equals(type)
                || Equation.TYPE_SCALAR_DELAYED.equals(type)
                || Equation.TYPE_SCALAR_INTERNAL.equals(type)
                || Equation.TYPE_INITIAL_ASSIGNMENT.equals(type)
                || StochasticEquation.TYPE_SCALAR_STOCHASTIC.equals(type))
            {
            	scalarDefs.put(eq.getVariable(), eq.getMath());
            }
        }

        return scalarDefs;
    }
    
    public String extractStochasticPart(Equation eq) throws Exception
    {
        Map<String, AstStart> scalarDefs = buildScalarDefinitionsMap();

        AstStart start = eq.getMath();
        StochasticPartExtractor stochasticPartExtractor = new StochasticPartExtractor(scalarDefs);
        stochasticPartExtractor.visitStart(start);
        return stochasticPartExtractor.getResultFormula();
    }
    
    public String extractDeterministicPart(Equation eq) throws Exception
    {
        Map<String, AstStart> scalarDefs = buildScalarDefinitionsMap();

        AstStart start = eq.getMath();
        DeterministicPartExtractor deterministicPartExtractor = new DeterministicPartExtractor(scalarDefs);
        deterministicPartExtractor.visitStart(start);
        return deterministicPartExtractor.getResultFormula();
    }
    
    private abstract class AbstractExpressionExtractor extends ASTVisitorSupport
    {
    	protected final Map<String, AstStart> scalarDefs;
    	
    	/** protects from cycles in scalar definitions: a -> b -> a */
    	protected final Set<String> expandingVars = new HashSet<>();
    	
        /** memoization for "contains stochastic" check for scalar variables */
        private final Map<String, Boolean> containsStochMemo = new HashMap<>();
        
        AbstractExpressionExtractor(Map<String, AstStart> scalarDefs)
        {
            this.scalarDefs = scalarDefs;
        }
        
        protected final boolean isAuxStochasticVariable(String name)
        {
            return name != null && name.contains(SdeEModel.STOCHASTIC_AUX_VARIABLE_NAME);
        }
        
        protected final boolean isStochasticFunction(Node node)
        {
            if (!(node instanceof AstFunNode))
            {
            	return false;
            }

            AstFunNode f = (AstFunNode)node;
            return "stochastic".equalsIgnoreCase(f.getFunction().getName());
        }
        
        /**
         * True if expression contains stochastic somewhere inside.
         */
        protected final boolean containsStochastic(Node node) throws Exception
        {
        	if (isStochasticFunction(node))
        	{
        		return true;
        	}
        	
            if (node instanceof AstVarNode)
            {
                String name = ((AstVarNode)node).getName();
                if (isAuxStochasticVariable(name))
                {
                    return true;
                }
                
                Boolean memo = containsStochMemo.get(name);
                if (memo != null)
                {
                	return memo.booleanValue();
                }

                AstStart def = scalarDefs.get(name);
                if (def != null)
                {
                    if (!expandingVars.add(name))
                    {
                    	// cycles protection: treat as non-stochastic
                    	containsStochMemo.put(name, Boolean.FALSE);
                    	return false;
                    }
                    boolean res = containsStochastic(def.jjtGetChild(0));
                    expandingVars.remove(name);
                    containsStochMemo.put(name, Boolean.valueOf(res));
                    return res;
                }
                
                containsStochMemo.put(name, Boolean.FALSE);
                return false;
            }

            for (int i = 0; i < node.jjtGetNumChildren(); i++)
            {
            	if (containsStochastic(node.jjtGetChild(i)))
            	{
            		return true;
            	}
            }

            return false;
        }
        
        protected final String formatNode(Node node)
        {
            LinearFormatter formatter = new LinearFormatter();
            AstStart start = Utils.createStart(Utils.cloneAST(node));
            return formatter.format(start)[1];
        }
    }
    
    private class StochasticPartExtractor extends AbstractExpressionExtractor
    {
        private String resultFormula = "0";
        
        StochasticPartExtractor(Map<String, AstStart> scalarDefs)
        {
            super(scalarDefs);
        }
        
        @Override
        public void visitStart(AstStart start) throws Exception
        {
            if (start.jjtGetNumChildren() == 0)
            {
                resultFormula = "0";
                return;
            }
            String stochExpr = extractStochasticExpression(start.jjtGetChild(0));
            resultFormula = (stochExpr == null || stochExpr.isEmpty()) ? "0" : stochExpr;
        }
        
        public String getResultFormula()
        {
            return resultFormula;
        }
        
        /**
         * Returns stochastic part of expression. Deterministic parts are replaced by 0.
         */
        private String extractStochasticExpression(Node node) throws Exception
        {
            if (!containsStochastic(node))
            {
            	return "0";
            }
            
            if (isStochasticFunction(node))
            {
                return formatNode(node);
            }

            if (node instanceof AstVarNode)
            {
                String name = ((AstVarNode)node).getName();
                if (isAuxStochasticVariable(name))
                {
                    return formatNode(node);
                }

                AstStart def = scalarDefs.get(name);
                if (def != null)
                {
                    if (!expandingVars.add(name))
                    {
                    	return "0";
                    }
                    String res = extractStochasticExpression(def.jjtGetChild(0));
                    expandingVars.remove(name);
                    return res;
                }

                return "0";
            }

            if (node instanceof AstFunNode)
            {
                AstFunNode f = (AstFunNode)node;
                String op = f.getFunction().getName();
                int n = f.jjtGetNumChildren();
                
                // Unary minus is encoded by the parser as function "u-" with single child var
                if (n == 1 && ("u-".equals(op) || "-".equals(op)))
                {
                    String a = extractStochasticExpression(f.jjtGetChild(0));
                    return "(-(" + a + "))";
                }
                if (n == 1 && ("u+".equals(op) || "+".equals(op)))
                {
                    String a = extractStochasticExpression(f.jjtGetChild(0));
                    return "(+(" + a + "))";
                }

                // linear operations: just extract stochastic parts from both sides
                if (n == 2 && ("+".equals(op) || "-".equals(op)))
                {
                    String a = extractStochasticExpression(f.jjtGetChild(0));
                    String b = extractStochasticExpression(f.jjtGetChild(1));
                    return "(" + a + op + b + ")";
                }
                
                // product/ratio: handle "stoch * det" and "stoch / det" specially
                if (n == 2 && ("*".equals(op) || "/".equals(op)))
                {
                    Node left = f.jjtGetChild(0);
                    Node right = f.jjtGetChild(1);

                    boolean leftSt = containsStochastic(left);
                    boolean rightSt = containsStochastic(right);

                    // (stoch * det) or (stoch / det) -> keep stochastic left and deterministic right
                    if (leftSt && !rightSt)
                    {
                        return "(" + extractStochasticExpression(left) + op + formatNode(right) + ")";
                    }
                    
                    // (det * stoch) -> keep deterministic left and stochastic right
                    // (det / stoch) -> cannot be expressed as a pure "linear stochastic part": keep full expression
                    if (!leftSt && rightSt)
                    {
                        if ("/".equals(op))
                        {
                        	return formatNode(node);
                        }

                        return "(" + formatNode(left) + op + extractStochasticExpression(right) + ")";
                    }

                    // other cases: keep full expression (contains stochastic anyway)
                    return formatNode(node);
                }

                // for other functions: if there is stochastic inside, keep full expression
                return formatNode(node);
            }

            // constants etc.
            return formatNode(node);
        }
    }
    
    private class DeterministicPartExtractor extends AbstractExpressionExtractor
    {
        private String resultFormula = "0";

        DeterministicPartExtractor(Map<String, AstStart> scalarDefs)
        {
            super(scalarDefs);
        }

        @Override
        public void visitStart(AstStart start) throws Exception
        {
            if (start.jjtGetNumChildren() == 0)
            {
                resultFormula = "0";
                return;
            }
            String detExpr = extractDeterministicExpression(start.jjtGetChild(0));
            resultFormula = (detExpr == null || detExpr.isEmpty()) ? "0" : detExpr;
        }

        public String getResultFormula()
        {
            return resultFormula;
        }

        /**
         * Returns deterministic part of expression (stochastic parts are replaced by 0).
         * Important: scalar variables are inlined ONLY if scalar definition contains stochastic,
         * otherwise we keep scalar variable reference to avoid duplicating blocks (like piecewise).
         */
        private String extractDeterministicExpression(Node node) throws Exception
        {
            if (isStochasticFunction(node))
            {
                return "0";
            }

            if (node instanceof AstVarNode)
            {
                String name = ((AstVarNode)node).getName();
                if (isAuxStochasticVariable(name))
                {
                	return "0";
                }

                AstStart def = scalarDefs.get(name);
                if (def != null)
                {
                	// Inline scalar only if it contains stochastic term (to remove it properly).
                    if (containsStochastic(def.jjtGetChild(0)))
                    {
                        if (!expandingVars.add(name)) // cycle protection
                            return "0";

                        String res = extractDeterministicExpression(def.jjtGetChild(0));
                        expandingVars.remove(name);
                        return res;
                    }

                    return formatNode(node);
                }

                return formatNode(node);
            }

            if (node instanceof AstFunNode)
            {
                AstFunNode f = (AstFunNode)node;
                String op = f.getFunction().getName();
                int n = f.jjtGetNumChildren();
                
                // Unary minus is encoded by the parser as function "u-"
                if (n == 1 && ("u-".equals(op) || "-".equals(op)))
                {
                    String a = extractDeterministicExpression(f.jjtGetChild(0));
                    return "(-(" + a + "))";
                }
                if (n == 1 && ("u+".equals(op) || "+".equals(op)))
                {
                    String a = extractDeterministicExpression(f.jjtGetChild(0));
                    return "(+(" + a + "))";
                }

                //if (n == 2 && ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op)))
                if (n == 2 && ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op) || "^".equals(op)))
                {
                    String a = extractDeterministicExpression(f.jjtGetChild(0));
                    String b = extractDeterministicExpression(f.jjtGetChild(1));
                    return "(" + a + op + b + ")";
                }

                StringBuilder sb = new StringBuilder();
                sb.append(op).append("(");
                for (int i = 0; i < n; i++)
                {
                    if (i > 0)
                    {
                    	sb.append(",");
                    }
                    sb.append(extractDeterministicExpression(f.jjtGetChild(i)));
                }
                sb.append(")");
                return sb.toString();
            }

            return formatNode(node);
        }
    }

    
    public Map<String, Integer> getVarStochasticIndexMapping()
    {
        return varStochasticIndexMapping;
    }

    public Map<String, Integer> getVarNameStochasticIndexMapping()
    {
        return varNameStochasticIndexMapping;
    }
    
    /**The number of "stochastic" variables, i.e. those for which the values at each grid node must be calculated exactly once.
     * Such variables are used in "stochastic" functions.*/
    public int getStochasticVariableNumber()
    {
        return varStochasticIndexMapping.size();
    }
    
    private void initializeSeed(Model model)
    {
        int modelSeed = isCustomSeed() ? getSeed() : (int)(new Date().getTime() / 1000);
        ((SdeModel)model).setSeed(modelSeed);
    }
    
    @PropertyName ("Seed")
    public int getSeed()
    {
        return seed;
    }
    public void setSeed(int seed)
    {
        this.seed = seed;
    }

    @PropertyName ("Use custom seed")
    public boolean isCustomSeed()
    {
        return customSeed;
    }
    public void setCustomSeed(boolean customSeed)
    {
        boolean oldValue = this.customSeed;
        this.customSeed = customSeed;
        firePropertyChange("customSeed", oldValue, customSeed);
        firePropertyChange("*", null, null);
    }
}
