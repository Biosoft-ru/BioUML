package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.table.TableDataCollection;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.EModel.NodeFilter;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.MathCalculator;
import biouml.model.dynamics.MathContext;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import biouml.standard.simulation.plot.Series.SourceNature;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;

public class AnalysisUtils
{
    private static final double DELTA = 1e-8;

    private static MathCalculator mathCalculator = new MathCalculator();

    private static Logger log = Logger.getLogger(AnalysisUtils.class.getName());

    public static void loadFunctionDeclarations(EModel emodel)
    {
        mathCalculator.addFunctionDeclarations(emodel.getFunctions());
    }

    public static void loadEquations(EModel emodel)
    {
        try
        {
            List<Equation> equations = emodel.getEquations(new MathScalarEquationFilter()).toList();
            mathCalculator.addEquations(equations);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    public static class MathScalarEquationFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            Equation role = de.getRole(Equation.class);
            String type = de.getKernel().getType();
            return role.getType().equals(Equation.TYPE_SCALAR)
                    && ( type.equals(Type.MATH_EQUATION) || type.equals("equation") );
        }
    }

    public static class MathRateEquationFilter extends NodeFilter
    {
        @Override
        protected boolean isNodeAcceptable(Node de)
        {
            Equation role = de.getRole(Equation.class);
            String type = de.getKernel().getType();
            return role.getType().equals(Equation.TYPE_RATE)
                    && ( type.equals(Type.MATH_EQUATION) || type.equals("equation") );
        }
    }

    /**
     * Calculates an array of formulas with specified values of variables.
     */
    public static double[] calculateFormulas(String[] math, MathContext variableValues)
    {
        return StreamEx.of( math ).mapToDouble( m -> mathCalculator.calculateMath( m, variableValues )[0] ).toArray();
    }

    /**
     * Calculates partial derivatives of the <b>formula</b> with respect to variables specified by <b>variableNames</b>
     * at the point given by <b>variableValues</b>.
     */
    public static double[] calculateGradient(String formula, String[] names, MathContext context)
    {
        double formulaValue = mathCalculator.calculateMath(formula, context)[0];

        MathContext shiftValues = new MathContext(context);

        double[] gradient = new double[names.length];
        for( int i = 0; i < gradient.length; ++i )
        {
            double curValue = context.get(names[i], 0);
            shiftValues.put(names[i], curValue + DELTA);
            gradient[i] = ( mathCalculator.calculateMath(formula, shiftValues)[0] - formulaValue ) / DELTA;
            shiftValues.put(names[i], curValue);
        }
        return gradient;
    }

    public static double[][] calculateGradients(String[] formulas, String[] names, MathContext variableValues)
    {
        return StreamEx.of( formulas ).map( formula -> AnalysisUtils.calculateGradient( formula, names, variableValues ) )
                .toArray( double[][]::new );
    }

    public static void fillSimulationResult(SimulationEngine simulationEngine, SimulationResult simulationResult,
            Map<String, double[]> valuesMap)
    {
        Diagram diagram = simulationEngine.getDiagram();

        simulationResult.setInitialTime(simulationEngine.getInitialTime());
        simulationResult.setCompletionTime(simulationEngine.getCompletionTime());
        simulationResult.setSimulatorName(simulationEngine.getSolverName());
        simulationResult.setDiagramPath(diagram.getCompletePath());

        if( valuesMap.size() > 0 )
        {
            double[] times = valuesMap.get("time");
            simulationResult.setTimes(times);

            double[][] values = new double[times.length][valuesMap.size()];
            Map<String, Integer> variableMap = new HashMap<>();

            int number = 0;
            for( Map.Entry<String, double[]> entry : valuesMap.entrySet() )
            {
                String key = entry.getKey();
                variableMap.put(key, number);
                double[] ratioValues = entry.getValue();

                Variable var = new Variable(key, null, null);
                var.setInitialValue(ratioValues[0]);
                simulationResult.addInitialValue(var);

                for( int i = 0; i < times.length; ++i )
                {
                    values[i][number] = ratioValues[i];
                }
                number++;
            }

            simulationResult.setValues(values);
            simulationResult.setVariableMap(variableMap);
        }
    }

    public static double[][] getMatrix(TableDataCollection tdc)
    {
        int n = tdc.getSize();
        int m = tdc.getColumnModel().getColumnCount();

        double[][] matrix = new double[n][m];
        for( int i = 0; i < n; ++i )
        {
            for( int j = 0; j < m; ++j )
            {
                if( tdc.getValueAt(i, j) instanceof Number )
                    matrix[i][j] = ( (Number)tdc.getValueAt(i, j) ).doubleValue();
                else
                    throw new IllegalArgumentException();
            }
        }
        return matrix;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Visualisation methods
    //

    public static String printReactions(List<Reaction> reactions) throws Exception
    {
        StringBuffer sb = new StringBuffer();
        for( Reaction reaction : reactions )
        {
            Diagram diagram = Diagram.getDiagram((DiagramElement)reaction.getParent());
            sb.append(printReaction(diagram, reaction) + "\n");
            sb.append("Formula: " + printFormula(diagram, reaction) + "\n");
            sb.append("\n");
        }
        String str = sb.toString();
        log.info(str);
        return str;
    }

    public static String printReaction(Diagram diagram, Reaction reaction)
    {
        StringBuilder reactantsSB = new StringBuilder();
        StringBuilder productsSB = new StringBuilder();
        StringBuilder modifiersSB = new StringBuilder();

        if( diagram != null && reaction != null )
        {
            try
            {
                for( SpecieReference sr : reaction.getSpecieReferences() )
                {
                    String species = sr.getSpecie();
                    species = diagram.findNode(species).getTitle();

                    if( sr.getRole().equals(SpecieReference.PRODUCT) )
                    {
                        if( productsSB.length() == 0 )
                            productsSB.append( species );
                        else
                            productsSB.append( " + " ).append( species );
                    }
                    else if( sr.getRole().equals(SpecieReference.REACTANT) )
                    {
                        if( reactantsSB.length() == 0 )
                            reactantsSB.append( species );
                        else
                            reactantsSB.append( " + " ).append( species );
                    }
                    else
                    {
                        if( modifiersSB.length() == 0 )
                            modifiersSB.append( " -" ).append( species );
                        else
                            modifiersSB.append( ", " ).append( species );
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not print reaction '" + reaction.getName() + "', reason: " + e);
            }
        }

        return reactantsSB.toString() + modifiersSB.toString() + " -> " + productsSB.toString();
    }

    private static String printFormula(Diagram diagram, Reaction reaction) throws Exception
    {
        final EModel emodel = diagram.getRole(EModel.class);

        LinearFormatter formatter = new LinearFormatter()
        {
            @Override
            protected void processVariable(AstVarNode node)
            {
                String name = node.getName();
                String title = name;

                Variable var = emodel.getVariable(name);
                if( var instanceof VariableRole )
                {
                    int mode = emodel.getDiagramElement().getViewOptions().getVarNameCode();
                    title = emodel.getQualifiedName( name, ( (VariableRole)var ).getDiagramElement(), mode );
                }

                String nodeTitle = node.getTitle();
                nodeTitle = nodeTitle.replace(name, title);
                result.append(nodeTitle);
            }
        };

        String formula = reaction.getFormula();
        Role eq = diagram.findDiagramElement(reaction.getName()).getRole();
        return formatter.format(emodel.readMath(formula, eq))[1];
    }

    public static @Nonnull Plot generateTimePlot(DataCollection origin, String name, @Nonnull SimulationResult simulationResult, String yTitle)
    {
        List<Series> seriesList = new ArrayList<>();

        Map<String, Integer> variablesMap = simulationResult.getVariableMap();
        for( String key : variablesMap.keySet() )
        {
            Series series = new Series();
            series.setXVar("time");
            series.setYVar(key);
            series.setSource(DataElementPath.create(simulationResult).toString());
            series.setSourceNature(SourceNature.SIMULATION_RESULT);
            seriesList.add(series);
        }

        Plot plot = new Plot(origin, name, seriesList);
        double[] times = simulationResult.getTimes();
        plot.setXFrom(times[0]);
        plot.setXTo(times[times.length - 1]);
        plot.setXTitle("Time");
        plot.setYTitle(yTitle);
        return plot;
    }
}
