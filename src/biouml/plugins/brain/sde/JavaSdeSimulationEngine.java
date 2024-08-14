package biouml.plugins.brain.sde;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import biouml.plugins.stochastic.StochasticSimulator;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.DiagramUtility;
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
        //return false;
        return EModel.isOfType(modelType, SdeEModel.SDE_TYPE)
                || EModel.isOfType(modelType, SdeEModel.STOCHASTIC_TYPE);
    }
    
    
    public String extractStochasticPart(Equation eq, String stochasticVarName) throws Exception
    {
        AstStart start = eq.getMath();
        StochasticPartExtractor stochasticPartExtractor = new StochasticPartExtractor();
        stochasticPartExtractor.visitStart(start);
        return stochasticPartExtractor.getResultFormula();
    }
    
    private class StochasticPartExtractor extends ASTVisitorSupport
    {
        private String resultFormula = "";
        
        @Override
        public void visitStart(AstStart start) throws Exception
        {
            for (int i = 0; i < start.jjtGetNumChildren(); i++)
            {
                visitNode(start.jjtGetChild(i));   
            }
        }
        
        @Override
        public void visitNode(Node node) throws Exception
        {
            Node parent = node.jjtGetParent();
            
            if (node instanceof AstFunNode)
            {
                AstFunNode funNode = (AstFunNode)node;
                for (int i = 0; i < funNode.jjtGetNumChildren(); i++)
                {
                    visitNode(funNode.jjtGetChild(i));   
                }
                return;
            }
            else if (node instanceof AstVarNode)
            {
                AstVarNode varNode = (AstVarNode)node;
                String name = varNode.getName();
                
                if (name.indexOf(SdeEModel.STOCHASTIUC_AUX_VARIABLE_NAME) != -1)
                {
//                    Node tmpNode = Utils.cloneAST(node);
//                    tmpNode.jjtSetParent(parent);
                    Node tmpNode = node;
                    Node tmpNodeParent = tmpNode.jjtGetParent();
                    while (tmpNodeParent instanceof AstFunNode 
                            && !"+-".contains(((AstFunNode)tmpNodeParent).getFunction().getName()))
                    {
                        tmpNode = tmpNode.jjtGetParent();
                        tmpNodeParent = tmpNode.jjtGetParent();
                    }
                    buildEquationFromAstNode(tmpNode);
                }                
            }
            return;
        }
        
        private void buildEquationFromAstNode(Node node)
        {
            LinearFormatter formatter = new LinearFormatter();
            
            if (node instanceof AstFunNode)
            {
                // arguments should be processed properly if stochastic terms can be used with non-standard functions.
                Node first = (node.jjtGetNumChildren() > 0) ? node.jjtGetChild(0) : null;
                Node second = (node.jjtGetNumChildren() > 1) ? node.jjtGetChild(1) : null;
                
                if (!resultFormula.isEmpty() && resultFormula.endsWith(")"))
                {
                    // sign should be parsed properly if several stochastic terms can be used within one equation with different signs.
                    resultFormula += "+";
                }
                
                resultFormula += "(";
                buildEquationFromAstNode(first);
                resultFormula += ((AstFunNode)node).getFunction().getName();
                buildEquationFromAstNode(second);    
                resultFormula += ")";
            }
            else
            {
                AstStart start = Utils.createStart(Utils.cloneAST(node)); // do not rewrite node parent
                resultFormula += formatter.format(start)[1];
            }
        }
        
        public String getResultFormula()
        {
            return resultFormula;
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
