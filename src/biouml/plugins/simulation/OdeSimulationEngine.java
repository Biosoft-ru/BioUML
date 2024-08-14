package biouml.plugins.simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.math.model.ASTVisitor;
import ru.biosoft.math.model.ASTVisitorSupport;
import ru.biosoft.math.model.AstConstant;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Formatter;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.Node;
import ru.biosoft.math.model.Utils;
import ru.biosoft.math.parser.ParserTreeConstants;
import ru.biosoft.util.TempFiles;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Edge;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.EModel.DelayedExpression;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

/**
 * General class for simulation engine. It defines common contract
 * (properties and methods) for any simulation engine.
 */
abstract public class OdeSimulationEngine extends SimulationEngine
{
    public static final String ARRAY_MODE_AUTO = "array_auto";
    public static final String ARRAY_MODE_ON = "array_on";
    public static final String ARRAY_MODE_OFF = "array_off";
    public static final int ARRAY_MODE_THRESHOLD = 100;
    public static final double DEFAULT_RELATIVE_TOLERANCE = 1e-11;
    public static final double DEFAULT_ABSOLUTE_TOLERANCE = 1e-10;

    protected String arrayMode = ARRAY_MODE_OFF;

    public static final String ALGEBRAIC_SYSTEM = "Algebraic system";
    public static final String ODE_SYSTEM = "ODE system";

    protected String fastReactions = ODE_SYSTEM;

    protected String constraintsViolation = ConstraintPreprocessor.CONSTRAINTS_STOP;
    
    /**Mapping between variable name in diagram and in generated code.*/
    protected Map<String, String> varNameMapping;

    /**Set of function names in code (for uniqueness check)*/
    protected Set<String> functionNames;

    /**set of variable names in code (for uniqueness check)*/
    protected Set<String> varNames;

    protected Map<String, List<Equation>> definingEquations;

    /**If true then reaction rate variables will appear in simulation result (e.g. $$rate_reactionName)*/
    private boolean reactionVariables = false;

    /**Mapping between variable name and its initial value*/
    private Map<String, Double> varInitialMapping = new HashMap<>();

    /**Size of all variables array in case of all variables generated as the one array elements.*/
    protected int variableArraySize;

    /**Mapping between variable name and its index in results array.*/
    protected Map<String, Integer> varIndexMapping;
    
    /**Mapping between variable path and its index in results array.*/
    protected Map<String, Integer> varPathIndexMapping;
    
    protected Map<String, Integer> varNameIndexMapping;

    protected Map<String, String> varPathMapping;

    /**Mapping between variable CODE name and its index in history array*/
    protected Map<String, Integer> varHistoricalIndexMapping;

    /**Mapping between variable name and its index in history array*/
    protected Map<String, Integer> varNameHistoryIndexMapping;

    /**Mapping between variable name and its index in dy/dt array.*/
    protected Map<String, Integer> varNameRateIndexMapping;

    private Map<String, Map<String, String>> codeVariableNames; //TODO: replace with varPathMapping 

    /** Output directory where the code will be generated. */
    protected String outputDir = TempFiles.path( "simulation" ).getAbsolutePath();

    /**
     * Initialize internal structures for model generation:
     * 1) map: variable - List(equations)
     * 2) variables type.
     * 3) status, error
     */
    public void init() throws Exception
    {
        boolean autodetect = executableModel.isAutodetectTypes();
        executableModel.setAutodetectTypes( false );
        try
        {
            log.info( "Model " + diagram.getName() + ": preprocessing..." );

            new File( getOutputDir() ).mkdirs();

            diagram = originalDiagram.clone( originalDiagram.getOrigin(), originalDiagram.getName() );

            if( DiagramUtility.containModules( diagram ) )
            {
                CompositeModelPreprocessor preprocessor = new CompositeModelPreprocessor();
                this.diagram = preprocessor.preprocess( diagram, diagram.getOrigin() );
                this.executableModel = (EModel)this.diagram.getRole();
                codeVariableNames = preprocessor.getCodeVariableNames();
                varPathMapping = preprocessor.getVarPathMapping( "" );

            }

            Logger.getLogger( EModel.class.getName() ).setLevel( log.getLogger().getLevel() );

            preprocess( diagram );

            modelType = executableModel.getModelType();
            executableModel.detectVariableTypes();

            initDefiningEquations();
            initVariableMappings();
            initDelayedEquations();
            initFunctions();

            Logger.getLogger( EModel.class.getName() ).setLevel( Level.ALL );
        }
        finally
        {
            executableModel.setAutodetectTypes( autodetect );
        }
    }

    public String simulate(File[] files, SimulationResult result) throws Exception
    {
        initSimulationResult( result );
        return simulate( files, new ResultListener[] {new ResultWriter( result )} );
    }

    ////////////////////////////////////////////////////////////////////////////
    // Abstract methods
    //
    public abstract String generateVariableCodeName(int n);
    public abstract String simulate(File[] files, ResultListener[] resultListeners) throws Exception;
    public abstract double getAbsTolerance();
    public abstract void setAbsTolerance(double absTolerance);
    public abstract double getRelTolerance();
    public abstract void setRelTolerance(double relTolerance);
    protected abstract String getAsArrayName(int number);

    /**Returns formatter corresponding to given engine.*/
    public abstract Formatter getFormatter();

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
        preprocessors.add( new TableElementPreprocessor() );
        preprocessors.add( new RateAssignmentRulePreprocessor() );
        preprocessors.add( new ScalarCyclesPreprocessor() );
        return preprocessors;
    }

    private void initFunctions()
    {
        for (Function function: executableModel.getFunctions())
        {
            String normalized = normalizeFunction(function.getName());
            function.setName(normalized);
            functionNames.add(normalized);
            normalizeFunction(function);
        }
    }

    public void normalizeFunction(Function function)
    {
        Set<String> args = StreamEx.of( function.getArguments() ).toSet();
        Map<String, String> replacements = StreamEx.of( function.getArguments() ).filter( arg -> isForbidden( arg ) )
                .toMap( n -> normalizeAndUpdate( n, args ) );
        AstStart start = getMath( function.getFormula(), function );
        Utils.renameVariableInAST( start, replacements );
        String newFormula = new LinearFormatter().format( start )[1];
        function.setFormula( newFormula );
    }
    
    /**
     * inits variable mappings:
     * <ul>
     * <li>varNameRateIndexMapping
     * <li>varNameMapping
     * <li>varIndexMapping
     * <li>varInitialMapping
     * </ul>
     */
    private void initVariableMappings()
    {
        varNames = new HashSet<>();
        varNameMapping = new HashMap<>();
        varNameIndexMapping = new HashMap<>();
        varIndexMapping = new HashMap<>();
        varNameRateIndexMapping = new HashMap<>();
        varInitialMapping = new HashMap<>();
        variableArraySize = 0;
        functionNames = new HashSet<>();

        boolean generateAsArray = isArrayModeOn();
        int index = 0;
        int rateEquationNumber = 0;
        for( Variable v : executableModel.getVariables() )
        {
            boolean arrayModeVariable = isArrayModeVariable( v );

            final String name = v.getName();

            if( name != null )
            {
                String codeName;
                if( arrayModeVariable )
                {
                    codeName = generateVariableCodeName( rateEquationNumber );
                    varNameRateIndexMapping.put( name, rateEquationNumber++ );
                }
                else
                    codeName = generateAsArray && !"time".equals( name ) ? getAsArrayName( variableArraySize++ ) : normalize( name );

                varNameMapping.put( name, codeName );
                varNames.add(codeName);
                if( !isAutogenerated( v ) )
                {
                    varInitialMapping.put( name, v.getInitialValue() );
                    //                if( isInternal( v.getName() ) )
                    varIndexMapping.put( name, index );
                    varNameIndexMapping.put( getShortName( name ), index );
                    index++;
                }
            }
        }
 
        varPathIndexMapping = new HashMap<>();
                //path to unique name
        if( varPathMapping != null )
        {
            Set<String> usedVariables = new HashSet<String>();
            for( Entry<String, String> e : EntryStream.of( varPathMapping ) )
            {
                    usedVariables.add( e.getValue() );
                    varPathIndexMapping.put( e.getKey(), varIndexMapping.get( e.getValue() ) );
            }
            for (Entry<String, Integer> e: varIndexMapping.entrySet())
            {
                if (!usedVariables.contains( e.getKey() ))
                    varPathIndexMapping.put( e.getKey(), e.getValue() );
            }
            if( this.result != null )
            {
                this.result.setVariableMap( varIndexMapping );
            }
        }
        else
        {
            varPathIndexMapping.putAll( varIndexMapping );
        }
        
        if( this.result != null )
        {
            this.result.setVariablePathMap( varPathIndexMapping );
            this.result.setVariableMap( getVarPathIndexMapping() );
        }
    }

    private String getShortName(String fullName)
    {
        if( fullName.startsWith( "$$" ) )
            fullName = fullName.substring( 2 );
        if( fullName.startsWith( "$" ) )
            fullName = fullName.substring( 1 );
        fullName = fullName.substring( fullName.lastIndexOf( "." ) + 1 );
        return fullName;
    }

    private void initDelayedEquations()
    {
        varHistoricalIndexMapping = new HashMap<>();
        varNameHistoryIndexMapping = new HashMap<>();
        int historicalVariableNumber = 0;
        if( containsDelays() )
        {
            List<DelayedExpression> delayedExpressions = executableModel.getDelayedExpressionsList();
            for( DelayedExpression expression : delayedExpressions )
            {
                ru.biosoft.math.model.Node delayedExpression = expression.node;
                String varName = ( (AstVarNode)delayedExpression ).getName();
                final String codeName = varNameMapping.get( varName );
                Variable var = executableModel.getVariable( varName );
                if( codeName != null && varName != null && !varHistoricalIndexMapping.containsKey( codeName ))
                {
                    varHistoricalIndexMapping.put( codeName, historicalVariableNumber );
                    varNameHistoryIndexMapping.put( var.getName(), historicalVariableNumber );
                    historicalVariableNumber++;
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    public int getRateEquationNumber()
    {
        return varNameRateIndexMapping.size();
    }

    public Integer getVariableRateIndex(String name)
    {
        if( !varNameRateIndexMapping.containsKey( name ) )
        {
            log.error( "WARNING_UNDEFINED_VARIABLE", new String[] {diagram.getName(), name} );
            return null;
        }
        return varNameRateIndexMapping.get( name );
    }

    public String[] getSpeciesNames()
    {
        Set<String> keys = varNameRateIndexMapping.keySet();
        return keys.toArray( new String[keys.size()] );
    }

    public synchronized String generateUniqueLocalVariable(String base)
    {
        int index = 1;
        String v = base;
        while( executableModel.getVariable( v ) != null )
            v = base + index++;
        return v;
    }

    public AstStart getMath(String expression, Role role)
    {
        return executableModel.readMath( expression, role,
                executableModel.getVariableResolver( EModel.VARIABLE_NAME_BY_ID ) );
    }

    public String[] formatMath(String expression, Role role) throws Exception
    {
        List<ASTVisitor> visitors;
//        if (role instanceof Equation && ((Equation)role).getType().equals( Equation.TYPE_INITIAL_VALUE ))
//            visitors = getInitialValuesASTVisitors(((Equation)role).getVariable());
//        else 
            visitors = getGenericASTVisitors();
        return formatMath( getMath(expression, role), visitors );
    }

    public String[] formatMath(String expression, Role role, Collection<ASTVisitor> astVisitors) throws Exception
    {
        return formatMath( getMath(expression, role), astVisitors );
    }

    public String[] formatMath(AstStart start) throws Exception
    {
        return formatMath( start, getGenericASTVisitors() );
    }

    public String[] formatMath(AstStart start, Collection<ASTVisitor> astVisitors) throws Exception
    {
        if( start == null )
            return new String[] {"", ""};//throw new Exception("Can't generate code. Equation for some parameter is empty.");
        final AstStart cloneAST = (AstStart)Utils.cloneAST( start );
        for( ASTVisitor visitor : astVisitors )
            Utils.visitAST( cloneAST, visitor );
        return getFormatter().format( cloneAST );
    }

    /**Get usual AST processing tools*/
    protected List<ASTVisitor> getGenericASTVisitors()
    {
        List<ASTVisitor> visitors = new ArrayList<>();
        visitors.add( new VariableSubstituteASTVisitor() );
        visitors.add( new OptimizerASTVisitor() );
        return visitors;
    }

    /**Get AST processing tools for function declarations.
     * Here we will not divide by compartment volume and will not substitute variable names, because they are place-holders.*/
    public static List<ASTVisitor> getFunctionDeclarationASTVisitors()
    {
        List<ASTVisitor> visitors = new ArrayList<>();
        visitors.add( new OptimizerASTVisitor() );
        return visitors;
    }

    /**Get AST processors list which also contains. Initial values substituter for generating initial values for derived variables*/
    public List<ASTVisitor> getInitialValuesASTVisitors(String varName)
    {
        List<ASTVisitor> visitors = new ArrayList<>();
        visitors.add( new InitialValueCalculationSubstituteASTVisitor(varName) );
        visitors.add( new InitialVariableSubstituteASTVisitor() );
        visitors.add( new OptimizerASTVisitor() );
        return visitors;
    }
    
    public List<ASTVisitor> getInitialValuesASTVisitors()
    {
        List<ASTVisitor> visitors = new ArrayList<>();
        visitors.add( new InitialVariableSubstituteASTVisitor() );
        visitors.add( new OptimizerASTVisitor() );
        return visitors;
    }

    public List<ASTVisitor> getInitialEquationASTVisitors()
    {
        List<ASTVisitor> visitors = new ArrayList<>();
        visitors.add( new InitialValuesSubstituteMathProcessor() );
        visitors.add( new VariableSubstituteASTVisitor() );
        visitors.add( new OptimizerASTVisitor() );
        return visitors;
    }

    public List<ASTVisitor> getPrehistoryASTVisitors()
    {
        List<ASTVisitor> visitors = new ArrayList<>();
        visitors.add( new AssignmentSubstitutionVisitor() );
        visitors.add( new VariableSubstituteASTVisitor() );
        visitors.add( new OptimizerASTVisitor() );
        return visitors;
    }

    private class VariableSubstituteASTVisitor extends ASTVisitorSupport
    {
        @Override
        public void visitNode(Node node) throws Exception
        {
            if( node instanceof AstVarNode )
            {
                AstVarNode varNode = (AstVarNode)node;
                varNode.setName( getVariableCodeName( varNode.getName() ) );
            }
        }
    }
    
    private class InitialVariableSubstituteASTVisitor extends ASTVisitorSupport
    {
        @Override
        public void visitNode(Node node) throws Exception
        {
            if( node instanceof AstVarNode )
            {
                AstVarNode varNode = (AstVarNode)node;
                String codeName = getVariableCodeName( varNode.getName() );
                codeName = codeName.replace( "x_values", "initialValues" );
                varNode.setName( codeName );
            }
        }
    }


    private class InitialValueCalculationSubstituteASTVisitor extends ASTVisitorSupport
    {
        String replacement = null;
        public InitialValueCalculationSubstituteASTVisitor(String varName)
        {
            Variable var = executableModel.getVariable( varName );
            if (var instanceof VariableRole && ((VariableRole)var).getInitialQuantityType() == VariableRole.CONCENTRATION_TYPE)
            {
                Integer index = getVariableRateIndex( varName );
                if (index != null)
                    replacement = "initialValues["+index+"]";
            }
        }
        
        @Override
        public void visitNode(Node node) throws Exception
        {
            if( node instanceof AstConstant )
            {
                if (replacement == null)
                    return;
                AstConstant varNode = (AstConstant)node;
                String name = varNode.getName();
                if( name.equals( "__INITIAL_VALUE__" ) )   
                    varNode.setName( replacement );
            }
        }
    }

    private class InitialValuesSubstituteMathProcessor extends ASTVisitorSupport
    {
        @Override
        public void visitStart(AstStart start) throws Exception
        {
            Utils.calculateVariables( start, varInitialMapping );
        }
    }

    private class AssignmentSubstitutionVisitor extends ASTVisitorSupport
    {
        @Override
        public void visitNode(Node node) throws Exception
        {
            Node parent = node.jjtGetParent();

            if( parent instanceof AstFunNode && "delay".equals( ( (AstFunNode)parent ).getFunction().getName() ) )
            {
                return;
            }
            if( node instanceof AstFunNode )
            {
                AstFunNode funNode = (AstFunNode)node;
                if( !"delay".equals( funNode.getFunction().getName() ) )
                {
                    for( int i = 0; i < funNode.jjtGetNumChildren(); i++ )
                        visitNode( funNode.jjtGetChild( i ) );
                    return;
                }
            }
            else if( node instanceof AstVarNode )
            {
                AstVarNode varNode = (AstVarNode)node;
                String name = varNode.getName();
                Variable var = executableModel.getVariable( name );

                //Process(x) = c, where x is constant and c - initial value for x
                if( var.isConstant() )
                {
                    AstConstant constant = new AstConstant( ParserTreeConstants.JJTCONSTANT );
                    constant.setValue( var.getInitialValue() );
                    constant.jjtSetParent( parent );
                    parent.jjtReplaceChild( node, constant );
                }
                else
                {
                    List<Equation> equations = definingEquations.get(name);
                    if (equations == null)
                        return ;
                    //Process(x) = Process(assignment), where "x=assignment" - initial assignment
                    for( Equation simpleEquation : equations )
                    {
                            if( simpleEquation.getType().equals( Equation.TYPE_INITIAL_ASSIGNMENT )
                                    || simpleEquation.getType().equals( Equation.TYPE_SCALAR )
                                    || simpleEquation.getType().equals( Equation.TYPE_SCALAR_DELAYED ) )
                            {
                                AstStart start = simpleEquation.getMath();
                                Node replacement = Utils.cloneAST( start.jjtGetChild( 0 ) );
                                replacement.jjtSetParent( parent );
                                parent.jjtReplaceChild( node, replacement );
                                visitNode( replacement ); //recursive processing assignment
                            }
                    }
                }
            }
        }
    }

    public static class OptimizerASTVisitor extends ASTVisitorSupport
    {
        @Override
        public void visitStart(AstStart start) throws Exception
        {
            Utils.pruneFunctions( start );
            Utils.optimizeDummyExpressions( start );
        }
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return varIndexMapping;
    }

    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return varPathIndexMapping;
    }

    public int getVarIndex(String name) throws Exception
    {
        return varIndexMapping.get( name );
    }

    public Map<String, Double> getVarInitialMapping()
    {
        return varInitialMapping;
    }

    @Override
    public SimulationEngine clone()
    {
        OdeSimulationEngine clone = (OdeSimulationEngine)super.clone();
        clone.setOutputDir( outputDir );
        return clone;
    }

    public void initDefiningEquations()
    {
        definingEquations = new HashMap<>();
        for( Equation eq : executableModel.getEquations() )
        {
            if( Equation.TYPE_ALGEBRAIC.equals( eq.getType() ) || Equation.TYPE_SCALAR_INTERNAL.equals(eq.getType()) )
                continue;

            String varName = eq.getVariable();
            Variable variable = executableModel.getVariable( varName );

            if( variable.isConstant() && !Equation.TYPE_INITIAL_ASSIGNMENT.equals( eq.getType() ) )
                continue;

            if( variable instanceof VariableRole && ( (VariableRole)variable ).isBoundaryCondition()
                    && Equation.TYPE_RATE.equals( eq.getType() ) && eq.getParent() instanceof Edge )
                continue;

            definingEquations.computeIfAbsent( varName, k -> new ArrayList<>() ).add( eq );
        }
    }

    public List<Equation> getDefiningEquations(String varName)
    {
        List<Equation> result = definingEquations.get( varName );
        return result != null ? result : new ArrayList<>();
    }

    @Override
    public String getOutputDir()
    {
        return outputDir;
    }

    @Override
    public void setOutputDir(String outputDir)
    {
        String oldValue = this.outputDir;
        this.outputDir = outputDir;
        firePropertyChange( "outputDir", oldValue, outputDir );
    }

    public Map<String, Integer> getvarHistoricalIndexMapping()
    {
        return varHistoricalIndexMapping;
    }

    public Map<String, Integer> getVarNameHistoryIndexMapping()
    {
        return varNameHistoryIndexMapping;
    }

    public String getArrayMode()
    {
        return arrayMode;
    }
    public void setGenerateVariableAsArray(String arrayMode)
    {
        this.arrayMode = arrayMode;
    }

    protected boolean isArrayModeOn()
    {
        return arrayMode == null? false: arrayMode.equals( ARRAY_MODE_ON ) || arrayMode.equals( ARRAY_MODE_AUTO );
    }


    /**Returns true if the differential equations contain delays when derivatives computing.*/
    public boolean containsDelays()
    {
        return EModel.isOfType( modelType, EModel.ODE_DELAY_TYPE );
    }

    public boolean isInternal(String variable)
    {
        return !isReactionVariables() || variable.startsWith( "$$" );
    }

    public static boolean isReactionEquation(Equation eq)
    {
        return eq.getParent() instanceof Edge;
    }

    public boolean isSignificant(Variable variable)
    {
        return !isTemp( variable ) && !isAutogenerated( variable );
    }

    public boolean isTemp(Variable variable)
    {
        return Boolean.TRUE.equals( variable.getAttributes().getValue( "temp" ) );
    }

    public boolean isAutogenerated(Variable variable)
    {
        return Boolean.TRUE.equals( variable.getAttributes().getValue( Preprocessor.AUTOGENERATED_VAR ) );
    }

    public boolean isAlgebraic(String variable)
    {
        return executableModel.getVariable(variable).getType().equals(Variable.TYPE_ALGEBRAIC);
    }

    @PropertyName("Reaction rate variables")
    @PropertyDescription("If true then reaction rate variables will appear in simulation result (e.g. $$rate_reactionName)")
    public boolean isReactionVariables()
    {
        return reactionVariables;
    }

    public void setReactionVariables(boolean reactionVariables)
    {
        this.reactionVariables = reactionVariables;
    }

    public boolean isArrayModeVariable(Variable v)
    {
        return v.getType().equals(Variable.TYPE_DIFFERENTIAL);
    }

    /**Number of "historical" variables, i.e. ones, for which values for all time slices must be stored during simulation.
     * Such variables are used in "delay" functions.*/
    public int getHistoricalVariableNumber()
    {
        return varHistoricalIndexMapping.size();
    }

    @Override
    public String getVariableCodeName(String varName)
    {
        String codeName = varNameMapping.get( varName );
        if( codeName == null )
        {
            log.error( "ERROR_UNDEFINED_VARIABLE", new String[] {diagram.getName(), varName} );
            return varName;
        }
        return codeName;
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        if( codeVariableNames != null && codeVariableNames.containsKey( diagramName )
                && codeVariableNames.get( diagramName ).containsKey( varName ) )
        {
            return codeVariableNames.get( diagramName ).get( varName );
        }
        return varName;
    }

    /**Normalize name to be valid simulation script name.*/
    @Override
    public String normalize(String name)
    {
        return changeToUniqueName(super.normalize(name)); //do we really need to check uniqueness here?
    }

    public String normalizeFunction(String name)
    {
        return name;
    }

    protected String changeToUniqueName(String name)
    {
        if( varNames.contains( name ) )
        {
            name = name + "_";
            return changeToUniqueName( name );
        }
        return name;
    }   
    
   public boolean isForbidden(String name)
   {
       return false;
   }
   
   public String normalizeAndUpdate(String name, Set<String> existing)
   {
//       name = super.normalize(name);
       while( isForbidden(name) || existing.contains(name) )
           name = name + "_";
       existing.add( name );
       return name;
   }

    @PropertyName("Constraints violation")
    @PropertyDescription("How to handle constraints violation in the model.")
    public String getConstraintsViolation()
    {
        return constraintsViolation;
    }
    public void setConstraintsViolation(String constraintsViolation)
    {
        this.constraintsViolation = constraintsViolation;
        this.diagramModified = true;
    }
    
    public static String[] getFastReactionsMethods()
    {
        return new String[] {ODE_SYSTEM, ALGEBRAIC_SYSTEM};
    }

    /**
     * Utility method to get all model variable names without "$" sign 
     */
    public String[] getAllVariables()
    {
        return varNameIndexMapping.keySet().toArray( new String[varNameIndexMapping.size()] );
    }

    public int getIndexByShortName(String shortName)
    {
        if( !varNameIndexMapping.containsKey( shortName ) )
            return -1;
        return varNameIndexMapping.get( shortName );
    }

    /**
     * Returns mapping: short name to index.
     * Short name example: "S1" instead of "$Compartment.S1"
     */
    public Map<String, Integer> getShortNameMapping()
    {
        return new HashMap<String, Integer>( varNameIndexMapping );
    }
}