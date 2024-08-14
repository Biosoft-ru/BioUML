package biouml.plugins.modelreduction;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MathContext;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Reaction;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * The method was programmed according to the article of J Choi et al.
 * "New time-scale criteria for model simplification of bio-reaction system."
 * (BMC Bioinformatics 2008, 9:338)
 */
@ClassIcon ( "resources/steady-state-analysis.gif" )
public class QuasiSteadyStateAnalysis extends AnalysisMethodSupport<QuasiSteadyStateAnalysisParameters>
{
    private double timeEpsilon = 0.001;
    private double ratioEpsilon = 0.001;
    private double dEpsilon = 1e-5;

    public QuasiSteadyStateAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new QuasiSteadyStateAnalysisParameters());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Analysis parameters properties
    //

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        if( parameters.getInput() == null || parameters.getInput().optDataElement() == null )
            throw new IllegalArgumentException("Please specify diagram path.");

        if( parameters.getOutput().isEmpty() || parameters.getOutput().getName().isEmpty() )
            throw new IllegalArgumentException("Please specify output table name.");

        if( parameters.getOutput().optParentCollection() == null )
            throw new IllegalArgumentException("Please specify output table origin.");
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = parameters.getInput().getDataElement(Diagram.class);

        if( diagram.getType() instanceof CompositeDiagramType )
            throw new IllegalArgumentException("The analysis can not be applied to the composite diagrams.");

        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(parameters.getOutput());

        Diagram diagramClone = diagram.clone(null, diagram.getName());

        SimulationEngine se = new JavaSimulationEngine();
        se.setDiagram(diagramClone);
        se.setInitialTime(parameters.getInitialTime());
        se.setCompletionTime(parameters.getCompletionTime());
        se.setTimeIncrement(parameters.getTimeIncrement());

        Map<String, List<double[]>> rangesMap = performAnalysis(se, tdc, parameters.getDEpsilon(), parameters
                .getTimeEpsilon(), parameters.getRatioEpsilon());

        if( rangesMap == null )
            return null;

        CollectionFactoryUtils.save(tdc);
        return tdc;
    }

    private String[] species;
    private double[] fastIntermediates;
    private String[] ranges;
    private View[] views;

    public Map<String, List<double[]>> performAnalysis(SimulationEngine simulationEngine, TableDataCollection tdc, double dEpsilon,
            double timeEpsilon, double ratioEpsilon) throws Exception
    {
        this.dEpsilon = dEpsilon;
        this.timeEpsilon = timeEpsilon;
        this.ratioEpsilon = ratioEpsilon;

        Map<String, List<double[]>> rangesMap = null;

        if( simulationEngine.getDiagram() != null && simulationEngine.getDiagram().getRole() instanceof EModel )
        {
            EModel emodel = simulationEngine.getDiagram().getRole( EModel.class );

            if( emodel.getEquations( new AnalysisUtils.MathRateEquationFilter() ).count() > 0 )
            {
                log.log(Level.SEVERE, "The analysis method is not applicable for a diagram with explicit rate equations.");
            }
            else
            {
                rangesMap = performAnalysis(simulationEngine);

                tdc.getColumnModel().addColumn(new TableColumn("Titles", String.class));
                tdc.getColumnModel().addColumn(new TableColumn("Highest absolute rate for time > 0", Double.class));
                tdc.getColumnModel().addColumn(new TableColumn("Fast dynamics ranges", String.class));
                tdc.getColumnModel().addColumn(new TableColumn("Views", CompositeView.class));

                for( int i = 0; i < species.length; ++i )
                {
                    VariableRole var = (VariableRole)emodel.getVariable(species[i]);

                    String title = species[i];
                    if( var.getDiagramElement() != null )
                        title = var.getDiagramElement().getTitle();

                    TableDataCollectionUtils.addRow(tdc, species[i], new Object[] {title, fastIntermediates[i], ranges[i], views[i]});
                }
            }
        }
        return rangesMap;
    }

    private Map<String, List<double[]>> performAnalysis(SimulationEngine simulationEngine) throws Exception
    {
        Diagram diagram = simulationEngine.getDiagram();
        TableDataCollection stoichiometry = StoichiometricAnalysis.getStoichiometricMatrix(diagram);

        species = new String[stoichiometry.getSize()];
        for( int i = 0; i < stoichiometry.getSize(); ++i )
        {
            species[i] = stoichiometry.getAt(i).getName();
        }

        String[] reactions = stoichiometry.columns().map( TableColumn::getName ).toArray( String[]::new );

        fastIntermediates = new double[species.length];

        String[] formulas = getReactionFormulas(reactions, diagram);

        AnalysisUtils.loadFunctionDeclarations( diagram.getRole( EModel.class ) );
        AnalysisUtils.loadEquations( diagram.getRole( EModel.class ) );

        SimulationResult simulationResult = new SimulationResult(null, "");
        simulationEngine.simulate(simulationEngine.createModel(), simulationResult);

        initTerms(stoichiometry);
        Map<String, List<double[]>> rangesMap = detectFastDynamicsRanges(formulas, simulationResult);
        generateRangesAndViews(rangesMap, simulationResult.getInitialTime(), simulationResult.getCompletionTime());
        return rangesMap;
    }

    private void generateRangesAndViews(Map<String, List<double[]>> rangesMap, double initialTime, double completionTime)
    {
        ranges = new String[species.length];
        views = new View[species.length];

        for( int i = 0; i < ranges.length; ++i )
        {
            List<double[]> range = rangesMap.get(species[i]);

            ranges[i] = generateRange(range);
            views[i] = generateView(range, initialTime, completionTime);
        }
    }

    private String generateRange(List<double[]> range)
    {
        if( range == null )
            return " ";

        return StreamEx.of( range ).map( interval -> "[" + interval[0] + ", " + interval[interval.length - 1] + "]" ).joining( ", " );
    }

    private static final int DEFAULT_WIDTH = 150;
    private static final int DEFAULT_HEIGHT = 20;

    private View generateView(List<double[]> range, double initialTime, double completionTime)
    {
        CompositeView view = new CompositeView();
        if( range != null )
        {
            view.add(new BoxView(new Pen(1, Color.WHITE), new Brush(Color.WHITE), 0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT));
            Pen pen = new Pen(2, Color.GREEN);
            for( int i = 0; i < range.size(); ++i )
            {
                double[] r = range.get(i);
                view.add(new LineView(pen, (float) ( DEFAULT_WIDTH * ( r[0] - initialTime ) / ( completionTime - initialTime ) ),
                        DEFAULT_HEIGHT / 2,
                        (float) ( DEFAULT_WIDTH * ( r[r.length - 1] - initialTime ) / ( completionTime - initialTime ) ),
                        DEFAULT_HEIGHT / 2));
            }
        }
        return view;
    }

    private String[] getReactionFormulas(String[] reactions, Diagram diagram) throws Exception
    {
        String[] formulas = new String[reactions.length];
        for( int i = 0; i < reactions.length; ++i )
        {
            String reactionName = reactions[i];
            Reaction reaction = (Reaction)diagram.findDiagramElement(reactionName).getKernel();
            formulas[i] = reaction.getFormula();
        }
        return formulas;
    }

    private int[][] productTerm;
    private int[][] lossTerm;

    private void initTerms(TableDataCollection stoichiometry)
    {
        int m = stoichiometry.getSize();
        int n = stoichiometry.getColumnModel().getColumnCount();

        productTerm = new int[m][n];
        lossTerm = new int[m][n];

        for( int i = 0; i < m; ++i )
        {
            for( int j = 0; j < n; ++j )
            {
                int value = (Integer)stoichiometry.getValueAt(i, j);
                productTerm[i][j] = Math.max(value, 0);
                lossTerm[i][j] = productTerm[i][j] - value;
            }
        }
    }

    private Map<String, List<double[]>> detectFastDynamicsRanges(String[] formulas, SimulationResult simulationResult) throws Exception
    {
        Map<String, List<double[]>> rangesMap = new HashMap<>();

        double[] times = simulationResult.getTimes();
        double[][] values = simulationResult.getValues();
        Map<String, Integer> variableMap = simulationResult.getVariableMap();

        MathContext variableValues = new MathContext();

        for( Map.Entry<String, Integer> entry : variableMap.entrySet() )
        {
            variableValues.put(entry.getKey(), values[0][entry.getValue()]);
        }

        for( int i = 0; i < times.length; ++i )
        {
            for( String var : species )
            {
                int index = variableMap.get(var);
                variableValues.put(var, values[i][index]);
            }
            variableValues.put("time", times[i]);

            double[] formulaValues = AnalysisUtils.calculateFormulas(formulas, variableValues);
            double[][] gradients = AnalysisUtils.calculateGradients(formulas, species, variableValues);

            boolean[] isFast = processScaleFactors(formulaValues, gradients, i);

            for( int j = 0; j < isFast.length; ++j )
            {
                if( isFast[j] )
                {
                    String var = species[j];
                    if( rangesMap.containsKey(var) )
                    {
                        List<double[]> timeInt = rangesMap.get(var);
                        double[] lastInt = timeInt.get(timeInt.size() - 1);
                        if( lastInt[lastInt.length - 1] == times[i - 1] )
                        {
                            lastInt[lastInt.length - 1] = times[i];
                        }
                        else
                        {
                            List<double[]> newTimeInt = rangesMap.get(var);
                            newTimeInt.add(new double[] {times[i], times[i]});
                            rangesMap.put(var, newTimeInt);
                        }
                    }
                    else
                    {
                        List<double[]> timeInt = new ArrayList<>();
                        timeInt.add(new double[] {times[i], times[i]});
                        rangesMap.put(var, timeInt);
                    }
                }
            }

            if( jobControl != null )
                jobControl.setPreparedness((int) ( ( (long)i * 100 ) / times.length ));
        }
        return rangesMap;
    }

    private boolean[] processScaleFactors(double[] formulaValues, double[][] gradients, int timeIndex)
    {
        boolean[] isFast = new boolean[species.length];

        double[] fluxes = new double[species.length];
        for( int i = 0; i < species.length; ++i )
        {
            for( int j = 0; j < formulaValues.length; ++j )
            {
                fluxes[i] += ( productTerm[i][j] - lossTerm[i][j] ) * formulaValues[j];
            }
        }

        for( int i = 0; i < species.length; ++i )
        {
            if( fastIntermediates[i] < Math.abs(fluxes[i]) && timeIndex > 0 )
                fastIntermediates[i] = Math.abs(fluxes[i]);

            double product = 0, loss = 0;

            double[] derivative = new double[species.length];
            for( int j = 0; j < formulaValues.length; ++j )
            {
                double st = productTerm[i][j] - lossTerm[i][j];

                double[] gradient = gradients[j];
                for( int k = 0; k < species.length; ++k )
                {
                    derivative[k] += st * gradient[k];
                }

                product += productTerm[i][j] * formulaValues[j];
                loss += lossTerm[i][j] * formulaValues[j];
            }

            double denominator = 0;
            for( int k = 0; k < species.length; ++k )
            {
                denominator += derivative[k] * fluxes[k];
            }

            if( Math.abs(denominator) > dEpsilon )
            {
                double timeScale = -fluxes[i] / denominator;
                double ratioFactor = Math.abs(fluxes[i]) / Math.max(product, loss);

                if( Math.abs(timeScale) <= timeEpsilon && ( timeScale >= 0 || ratioFactor <= ratioEpsilon ) )
                {
                    isFast[i] = true;
                }
            }
            else if( product != 0 && Math.abs(product - loss) < dEpsilon )
            {
                isFast[i] = true;
            }
        }
        return isFast;
    }
}
