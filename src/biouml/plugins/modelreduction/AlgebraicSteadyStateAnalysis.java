package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.Maps;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.ae.AeLevenbergMarquardSolver;
import biouml.plugins.simulation.ae.AeSolver;
import biouml.plugins.simulation.java.JavaBaseModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

import com.developmentontheedge.application.Application;

@ClassIcon ( "resources/steady-state-analysis.gif" )
public class AlgebraicSteadyStateAnalysis extends AnalysisMethodSupport<AlgebraicSteadyStateParameters>
{
    public static final String SUPPORTING_VAR = "SupportingVarForAnalysis";

    public AlgebraicSteadyStateAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new AlgebraicSteadyStateParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        jobControl.pushProgress(0, 10);
        AeSolver solver = parameters.getSolver();
        Diagram prototypeDiagram = parameters.getInputPath().getDataElement(Diagram.class);
        Diagram diagram = prototypeDiagram.clone(null, prototypeDiagram.getName());
        if( jobControl.isStopped() )
            return null;

        preprocess(diagram, parameters.isOnlyConstantParameters());
        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        String[] events = parameters.getEvents();
        if( events != null && events.length >0 )
        {
            int fromProgress = 10;
            int step = 80 / events.length + 1;
            for( String event : events )
            {
                jobControl.pushProgress(fromProgress, fromProgress + step);

                //TODO: implement events applying

                jobControl.popProgress();
                fromProgress += step;
                if( jobControl.isStopped() )
                    return null;
            }
            if( fromProgress < 90 )
            {
                jobControl.pushProgress(fromProgress, 90);
                jobControl.popProgress();
            }
        }
        else
            jobControl.pushProgress(10, 90);

        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(diagram);
        JavaBaseModel model = (JavaBaseModel)engine.createModel();
        if( jobControl.isStopped() )
            return null;

        if( solver instanceof AeLevenbergMarquardSolver )
        {
            ( (AeLevenbergMarquardSolver)solver ).createInfluenceMap(engine);
        }

        model.setAeSolver(solver);
        model.init();
        double[] values = model.extendResult(0, model.getInitialValues());
        jobControl.popProgress();
        if( jobControl.isStopped() )
            return null;

        if( !solver.isSuccess() )
        {
            String title = "WARNING";
            String message = solver.getMessage() + " Do you want to save obtained result anyway?";
            int res = JOptionPane.showConfirmDialog(Application.getApplicationFrame(), message, title, JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if( res != JOptionPane.YES_OPTION )
                return null;
        }

        jobControl.pushProgress(90, 100);


        Map<String, Integer> variableMap = getVariableMap( diagram, engine );
        Object result;
        if(parameters.getOutputType().equals( AlgebraicSteadyStateParameters.OUTPUT_TABLE_TYPE ))
            result = saveToTable( variableMap, values );
        else
            result = saveToSimulationResult( variableMap, values );

        jobControl.popProgress();
        return result;
    }

    private Map<String, Integer> getVariableMap(Diagram diagram, JavaSimulationEngine engine)
    {
        EModel emodel = diagram.getRole( EModel.class );
        return Maps.filterKeys( engine.getVarIndexMapping(), varName -> !"time".equals( varName ) && !varName.startsWith( "$$" )
                && !SUPPORTING_VAR.equals( varName ) && emodel.containsVariable( varName ) );
    }

    private TableDataCollection saveToTable(Map<String, Integer> variableMap, double[] values) throws Exception
    {
        DataElementPath outputPath = parameters.getOutputTable();

        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( outputPath );

        tdc.getColumnModel().addColumn("Value", Double.class);
        for( Map.Entry<String, Integer> entry : variableMap.entrySet() )
        {
            String varName = entry.getKey();
            Object[] value = {values[entry.getValue()]};
            TableDataCollectionUtils.addRow(tdc, varName, value);
        }

        outputPath.save(tdc);
        return tdc;
    }

    private SimulationResult saveToSimulationResult(Map<String, Integer> variableMap, double[] values)
    {
        Map<String, Integer> compactVariableMap = new HashMap<>();
        double[] compactValues = new double[variableMap.size()];
        int i = 0;
        for( Map.Entry<String, Integer> entry : variableMap.entrySet() )
        {
            compactValues[i] = values[entry.getValue()];
            compactVariableMap.put( entry.getKey(), i++ );
        }

        DataElementPath outputPath = parameters.getOutputSimulationResult();
        SimulationResult simulationResult = new SimulationResult( outputPath.getParentCollection(), outputPath.getName() );
        simulationResult.setDiagramPath( parameters.getInputPath() );
        simulationResult.setVariableMap( compactVariableMap );
        simulationResult.add( 0, compactValues );
        outputPath.save( simulationResult );
        return simulationResult;
    }

    private Map<String, String> getConservationLaws(Diagram diagram)
    {
        Map<String, String> result = new HashMap<>();
        TableDataCollection stoichiometry = StoichiometricAnalysis.getStoichiometricMatrix(diagram);
        if( stoichiometry.getColumnModel().getColumnCount() == 0 )
            return result;

        StoichiometricMatrixDecomposition smd = new StoichiometricMatrixDecomposition(stoichiometry);
        List<String> dependentSpecies = smd.getDependentSpecies();
        List<String> independentSpecies = smd.getIndependentSpecies();
        EModel emodel = diagram.getRole( EModel.class );
        double[][] linkMatrix = smd.getLinkMatrix();
        for( int i = 0; i < stoichiometry.getSize(); ++i )
        {
            String species = stoichiometry.getName(i);
            if( dependentSpecies.contains(species) )
            {
                StringBuilder expression = new StringBuilder();
                double amount = emodel.getVariable(species).getInitialValue();

                for( int j = 0; j < independentSpecies.size(); ++j )
                {
                    if( linkMatrix[i][j] != 0.0 )
                    {
                        String title = independentSpecies.get(j);
                        expression.append(generateTerm(title, linkMatrix[i][j]));
                        amount -= linkMatrix[i][j] * emodel.getVariable(title).getInitialValue();
                    }
                }

                if(amount > 0)
                    expression.append( " + " + amount );
                else if(amount < 0)
                    expression.append( " - " + Math.abs( amount ) );

                result.put( species, expression.toString() );
            }
        }
        return result;
    }

    private String generateTerm(String variable, double factor)
    {
        String str = " ";
        str += ( factor < 0 ) ? "- " : "+ ";
        if( Math.abs(factor) != 1.0 )
        {
            str += Math.abs(factor) + " * ";
        }
        str += variable;
        return str;
    }

    private void preprocess(Diagram diagram, boolean onlyConstantParameters) throws Exception
    {
        removeEdgesFromConstants(diagram);

        List<Node> speciesNodes = new ArrayList<>();
        List<Node> toRemove = new ArrayList<>();
        List<Equation> rateEquations = new ArrayList<>();
        List<Equation> equationsToRemove = new ArrayList<>();
        fillLists(diagram, speciesNodes, toRemove, rateEquations);

        Map<String, String> conservationLaws = getConservationLaws( diagram );
        for( Node node : speciesNodes )
        {
            createEquation(diagram, node, rateEquations, equationsToRemove, conservationLaws);
        }
        EModel emodel = diagram.getRole( EModel.class );
        for( Node node : toRemove )
        {
            diagram.getType().getSemanticController().remove( node );
            String variable = node.getRole( Equation.class ).getVariable();
            emodel.getVariables().remove( variable );
        }
        for( Equation eq : rateEquations )
            eq.setType(Equation.TYPE_ALGEBRAIC);
        for( Equation eq : equationsToRemove )
        {
            diagram.getType().getSemanticController().remove(eq.getDiagramElement());
        }
        emodel.removeNotUsedParameters();
        if( onlyConstantParameters )
        {
            List<Variable> changingVars = new ArrayList<>();
            for( Equation eq : emodel.getEquations() )
                changingVars.add(emodel.getVariable(eq.getVariable()));
            for( Variable var : emodel.getVariables() )
            {
                if( var instanceof VariableRole || "time".equals(var.getName()) || changingVars.contains(var) )
                    continue;
                var.setConstant(true);
            }
        }
    }

    private void removeEdgesFromConstants(Compartment diagram)
    {
        for( Node node : diagram.getNodes() )
        {
            Role role = node.getRole();
            if( role instanceof VariableRole )
            {
                VariableRole varRole = (VariableRole)role;
                if( varRole.isBoundaryCondition() )
                {
                    varRole.setBoundaryCondition( false );
                    varRole.setConstant( true );
                }
                if( varRole.isConstant() )
                {
                    for( Edge e : node.getEdges() )
                        node.removeEdge( e );
                }
            }
        }
    }

    private void fillLists(Compartment diagram, List<Node> speciesNodes, List<Node> toRemove, List<Equation> rateEquations)
    {
        for( Node node : diagram.recursiveStream().select( Node.class ) )
        {
            Role role = node.getRole();
            if( role instanceof VariableRole && !((VariableRole)role).isBoundaryCondition())
                speciesNodes.add(node);
            else if( isReaction(node) )
                toRemove.add(node);
            else if( role instanceof Equation && Equation.TYPE_RATE.equals( ( (Equation)role ).getType()) )
                rateEquations.add((Equation)role);
        }
    }

    private boolean isReaction(Node node)
    {
        Base kernel = node.getKernel();
        return kernel instanceof Reaction || ( kernel instanceof Stub && "reaction".equals(kernel.getType()) );
    }

    private void createEquation(Diagram diagram, Node node, List<Equation> otherRateEquations, List<Equation> equationsToRemove, Map<String, String> conservationLaws)
            throws Exception
    {
        String nodeRoleName = node.getRole(VariableRole.class).getName();

        if(conservationLaws.containsKey( nodeRoleName ))
        {
            putScalarEquation(diagram, nodeRoleName, conservationLaws.get( nodeRoleName ));
            return;
        }

        StringBuilder formula = new StringBuilder();
        for( Edge edge : node.getEdges() )
        {
            if( edge.getRole() instanceof Equation )
                formula.append( " + " ).append( processEdgeFormula( edge ) );
        }

        for( Equation eq : otherRateEquations )
        {
            if( eq.getVariable().equals(nodeRoleName) )
            {
                formula.append(" + ").append(eq.getFormula());
                equationsToRemove.add(eq);
            }
        }
        otherRateEquations.removeAll(equationsToRemove);

        if( formula.length() != 0 )
            putAlgebraicEquation(diagram, substitute( formula.toString(), conservationLaws ));
    }

    private String substitute(String expression, Map<String, String> varMap)
    {
        StringBuilder result = new StringBuilder();
        String delimiters = " ()+-/%*^";
        StringTokenizer tokens = new StringTokenizer(expression, delimiters, true);
        while( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            if( varMap.containsKey( token ) )
                result.append( "(" + varMap.get( token ) + ")" );
            else
                result.append( token );
        }
        return result.toString();
    }

    private void putAlgebraicEquation(Diagram diagram, String formula) throws Exception
    {
        Node eqNode = new Node(diagram, new Stub(null, DefaultSemanticController.generateUniqueNodeName(diagram, "algebraicEq"),
                Type.MATH_EQUATION));
        diagram.setNotificationEnabled(false);
        Equation eq = new Equation(eqNode, Equation.TYPE_ALGEBRAIC, SUPPORTING_VAR, formula);
        eqNode.setRole(eq);
        diagram.setNotificationEnabled(true);
        diagram.put(eqNode);
    }

    private void putScalarEquation(Diagram diagram, String variable, String formula) throws Exception
    {
        Node eqNode = new Node(diagram, new Stub(null, DefaultSemanticController.generateUniqueNodeName(diagram, "scalarEq"),
                Type.MATH_EQUATION));
        diagram.setNotificationEnabled(false);
        Equation eq = new Equation(eqNode, Equation.TYPE_SCALAR, variable, formula);
        eqNode.setRole(eq);
        diagram.setNotificationEnabled(true);
        diagram.put(eqNode);
    }

    private String processEdgeFormula(Edge edge)
    {
        if( ! ( edge.getRole() instanceof Equation ) )
            return "0";
        String edgeFormula = edge.getRole( Equation.class ).getFormula();
        Node node = edge.nodes().findFirst( this::isReaction ).orElse( null );
        if( node != null && node.getRole() instanceof Equation )
        {
            Equation eq = node.getRole( Equation.class );
            Map<String, String> varMap = Collections.singletonMap( eq.getVariable(), eq.getFormula() );
            return substitute( edgeFormula.trim(), varMap );
        }
        return edgeFormula;
    }

}