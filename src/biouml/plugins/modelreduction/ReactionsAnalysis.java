package biouml.plugins.modelreduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.modelreduction.ReactionsAnalysisParameters.AnalysisTarget;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.AxisOptions.Transform;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.math.model.AstFunNode;
import ru.biosoft.math.model.AstPiecewise;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.Node;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class ReactionsAnalysis extends AnalysisMethodSupport<ReactionsAnalysisParameters>
{
    private int iterationsNumber;
    private int currentIteration;

    public ReactionsAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new ReactionsAnalysisParameters());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Analysis properties
    //
    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        if( parameters.getInput() == null || parameters.getInput().optDataElement() == null )
            throw new IllegalArgumentException("Please specify diagram path.");

        if( parameters.getOutput() == null || ! ( parameters.getOutput().optParentCollection() instanceof FolderCollection ) )
            throw new IllegalArgumentException("Please specify the collection to save results of the analysis.");
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataCollection<?> origin = DataCollectionUtils.createSubCollection(parameters.getOutput());

        Diagram diagram = parameters.getInput().getDataElement(Diagram.class);
        TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection(origin, "Reactions classification");
        AnalysisTarget target = parameters.getAnalysisTarget();

        Span span = ( parameters.getTimeIncrement() != 0.0 ) ? new UniformSpan(parameters.getInitialTime(), parameters.getCompletionTime(),
                parameters.getTimeIncrement()) : new ArraySpan(parameters.getInitialTime(), parameters.getCompletionTime());

        SimulationResult sr = performAnalysis(diagram, tdc, target, span, parameters.getThreshold());
        if( sr != null && sr.getOrigin() != null )
        {
            CollectionFactoryUtils.save(sr);

            Plot plot = AnalysisUtils.generateTimePlot(origin, "Ratios plot", sr, "Concentrations ratios");
            CollectionFactoryUtils.save(plot);
        }
        CollectionFactoryUtils.save(tdc);
        return tdc;
    }

    private List<DiagramElement> reactionNodes;
    public SimulationResult performAnalysis(Diagram diagram, TableDataCollection tdc, AnalysisTarget target, Span span, double threshold)
    {
        if( diagram == null || ! ( diagram.getRole() instanceof EModel ) )
        {
            return null;
        }

        List<Object[]> values = new ArrayList<>();

        reactionNodes = new ArrayList<>();
        List<String> formulas = new ArrayList<>();
        getReactions(diagram, diagram, formulas);

        currentIteration = 0;
        iterationsNumber = 0;
        if( target.isLinearity() )
            iterationsNumber += reactionNodes.size();
        if( target.isMonomolecularity() )
            iterationsNumber += reactionNodes.size();
        if( target.isPseudoMonomolecularity() )
            iterationsNumber += reactionNodes.size();

        tdc.getColumnModel().addColumn(new TableColumn("Reactions", String.class));
        values.add(formulas.toArray(new String[formulas.size()]));

        if( target.isLinearity() )
        {
            tdc.getColumnModel().addColumn(new TableColumn("Linearity", String.class));
            values.add(getLinearReactions(diagram));
        }

        if( target.isMonomolecularity() )
        {
            tdc.getColumnModel().addColumn(new TableColumn("Monomolecularity", String.class));
            values.add(getMonomolecularReactions(diagram));
        }

        SimulationResult sr = null;

        if( target.isPseudoMonomolecularity() )
        {
            sr = new SimulationResult(tdc.getOrigin(), "Ratios simulation result");
            tdc.getColumnModel().addColumn(new TableColumn("Pseudo-monomolecularity", String.class));
            values.add(getPseudoMonomolecularReactions(diagram, sr, span, threshold));

            tdc.getColumnModel().addColumn(new TableColumn("Ranges", String.class));
            tdc.getColumnModel().addColumn(new TableColumn("Plot", Chart.class));

            String[] ranges = generateRanges(sr, threshold);
            values.add(ranges);
            values.add(generateCharts(sr, threshold, ranges));
        }

        fillTable(tdc, values);
        return sr;
    }

    private void getReactions(Diagram diagram, Compartment compartment, List<String> formulas)
    {
        for(DiagramElement de : compartment)
        {
            if( de.getKernel() instanceof Reaction )
            {
                Reaction reaction = (Reaction)de.getKernel();
                formulas.add(AnalysisUtils.printReaction(diagram, reaction));
                reactionNodes.add(de);
            }
            else if( de.getKernel().getType().equals(Type.TYPE_COMPARTMENT) )
            {
                getReactions(diagram, (Compartment)de, formulas);
            }
            else if( de.getKernel() instanceof DiagramInfo )
            {
                getReactions((Diagram)de, (Compartment)de, formulas);
            }
        }
    }

    private void fillTable(TableDataCollection tdc, List<Object[]> values)
    {
        for( int i = 0; i < reactionNodes.size(); ++i )
        {
            Object[] row = new Object[tdc.getColumnModel().getColumnCount()];
            for( int j = 0; j < row.length; ++j )
            {
                row[j] = values.get(j)[i];
            }
            TableDataCollectionUtils.addRow(tdc, reactionNodes.get(i).getName(), row);
        }
    }

    private List<String[]> reactionRatios;

    private Chart[] generateCharts(SimulationResult simulationResult, double threshold, String[] ranges)
    {
        Chart[] charts = new Chart[reactionNodes.size()];

        ChartOptions options = new ChartOptions();
        AxisOptions xAxis = new AxisOptions();
        xAxis.setLabel("Time");
        xAxis.setTransform(Transform.LOGARITHM);
        options.setXAxis(xAxis);
        AxisOptions yAxis = new AxisOptions();
        yAxis.setLabel("Concentrations ratios");
        yAxis.setMax(1E5 * threshold);
        yAxis.setMin(1E-5 / threshold);
        yAxis.setTransform(Transform.LOGARITHM);
        options.setYAxis(yAxis);

        SimulationResult srApp = simulationResult;

        if( srApp.getTimes().length > 1001 )
        {
            double inTime = srApp.getInitialTime();
            double complTime = srApp.getCompletionTime();
            double timeInc = ( complTime - inTime ) / 1000;
            srApp = srApp.approximate(inTime, complTime, timeInc);
        }

        double[] times = srApp.getTimes();
        double[][] values = srApp.getValues();
        Map<String, Integer> variableMap = srApp.getVariableMap();

        for( int i = 0; i < reactionNodes.size(); ++i )
        {
            if( ranges[i] != null && !ranges[i].equals(" ") )
            {
                charts[i] = new Chart();
                charts[i].setOptions(options);

                String[] ratios = reactionRatios.get(i);
                if( ratios != null )
                {
                    for( String ratio : ratios )
                    {
                        int index = variableMap.get(ratio);

                        double[][] seriesValues = IntStreamEx.ofIndices( times )
                                .mapToObj( j -> new double[] {times[j], values[j][index]} ).toArray( double[][]::new );

                        ChartSeries series = new ChartSeries(seriesValues);
                        series.setLabel(ratio);
                        charts[i].addSeries(series);
                    }
                }
            }
        }
        return charts;
    }

    private String[] generateRanges(SimulationResult simulationResult, double threshold)
    {
        double[] times = simulationResult.getTimes();
        double[][] values = simulationResult.getValues();
        Map<String, Integer> variableMap = simulationResult.getVariableMap();

        String[] ranges = new String[reactionNodes.size()];
        for( int i = 0; i < reactionNodes.size(); ++i )
        {
            StringBuffer range = new StringBuffer();
            String[] ratios = reactionRatios.get(i);
            if( ratios != null )
            {
                for( String ratio : ratios )
                {
                    int index = variableMap.get(ratio);

                    double in = -1;
                    double out = -1;

                    for( int j = 0; j < times.length; ++j )
                    {
                        if( values[j][index] < threshold && values[j][index] > 1 / threshold )
                        {
                            if( in < 0 )
                                in = out = times[j];
                            else if( out == times[j - 1] )
                                out = times[j];
                            else
                            {
                                range.append("[" + in + ", " + out + "], ");
                                in = out = times[j];
                            }
                        }
                    }
                    if( in > -1 )
                    {
                        range.append("[" + in + ", " + out + "]; ");
                    }
                }

                if( range.length() > 0 )
                    ranges[i] = range.toString();
            }

            if( ranges[i] == null )
                ranges[i] = " ";
        }
        return ranges;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Linear/nonlinear reactions
    //

    /**
     * Reaction is linear if it has linear kinetic law.
     */
    private String[] getLinearReactions(Diagram diagram)
    {
        calculatedParameters = getCalculatedParameters(diagram);

        String[] isLinear = new String[reactionNodes.size()];
        processReactions(diagram, (EModel)diagram.getRole(), isLinear);
        return isLinear;
    }

    /**
     * Gets diagram parameters (not variables!) which values are calculated by equations.
     * 
     * @return map of calculated parameters with the key set of parameter names and the value set of parsed formulas.
     */
    private Map<String, AstStart> getCalculatedParameters(Diagram diagram)
    {
        Map<String, AstStart> calculatedParameters = new HashMap<>();

        EModel emodel = diagram.getRole( EModel.class );
        for( Equation equation : emodel.getEquations() )
        {
            String var = equation.getVariable();
            if( !var.startsWith("$") )
            {
                AstStart math = equation.getMath();
                if( math != null )
                    calculatedParameters.put(var, math);
            }
        }
        return calculatedParameters;
    }

    private void processReactions(Diagram diagram, EModel emodel, String[] isLinear)
    {
        for( int i = 0; i < reactionNodes.size(); ++i )
        {
            DiagramElement de = reactionNodes.get(i);
            Reaction reaction = (Reaction)de.getKernel();
            String formula = reaction.getFormula();

            processParameters = new TreeSet<>();

            AstStart math = emodel.readMath(formula, de.getRole());
            if( math != null )
            {
                boolean isNonlinear = isNonlinear(emodel, math);
                if( isNonlinear )
                    isLinear[i] = "-";
                else
                    isLinear[i] = "+";
            }

            if( jobControl != null )
                jobControl.setPreparedness((int) ( ( (long)currentIteration++ * 100 ) / iterationsNumber ));
        }
    }

    private Map<String, AstStart> calculatedParameters;
    private Set<String> processParameters;
    private Set<String> processParametersToFindVariable;

    private boolean isNonlinear(EModel emodel, Node node)
    {
        if( node instanceof AstPiecewise )
            return true;

        if( node instanceof AstVarNode )
        {
            String name = ( (AstVarNode)node ).getName();
            if( calculatedParameters.containsKey(name) && !processParameters.contains(name) )
            {
                processParameters.add(name);
                AstStart math = calculatedParameters.get(name);
                if( isNonlinear(emodel, math) )
                    return true;
            }
        }

        if( node instanceof AstFunNode )
        {
            Function function = ( (AstFunNode)node ).getFunction();
            String name = function.getName();
            processParametersToFindVariable = new TreeSet<>();
            if( "*".equals(name) )
            {
                if( findVariable(emodel, node.jjtGetChild(0)) && findVariable(emodel, node.jjtGetChild(1)) )
                {
                    return true;
                }
            }
            else if( "/".equals(name) )
            {
                if( findVariable(emodel, node.jjtGetChild(1)) )
                {
                    return true;
                }
            }
            else if( "^".equals(name) )
            {
                if( findVariable(emodel, node.jjtGetChild(0)) || findVariable(emodel, node.jjtGetChild(1)) )
                {
                    return true;
                }
            }
        }

        int n = node.jjtGetNumChildren();
        for( int i = 0; i < n; i++ )
        {
            if( isNonlinear(emodel, node.jjtGetChild(i)) )
                return true;
        }
        return false;
    }

    private boolean findVariable(EModel emodel, Node node)
    {
        if( node instanceof AstVarNode )
        {
            String name = ( (AstVarNode)node ).getName();
            if( ( emodel.getVariable(name) instanceof VariableRole ) )
            {
                return true;
            }
            else if( calculatedParameters.containsKey(name) && !processParametersToFindVariable.contains(name) )
            {
                processParametersToFindVariable.add(name);
                AstStart math = calculatedParameters.get(name);
                return findVariable(emodel, math);
            }
        }
        else
        {
            int n = node.jjtGetNumChildren();
            for( int i = 0; i < n; i++ )
            {
                if( findVariable(emodel, node.jjtGetChild(i)) )
                    return true;
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Monomolecular/non-monomolecular reactions
    //

    /**
     * Reaction is monomolecular if it has one reactant or one modifier.
     */
    private String[] getMonomolecularReactions(Diagram diagram)
    {
        String[] isMonomolecular = new String[reactionNodes.size()];
        processReactions(diagram, isMonomolecular);
        return isMonomolecular;
    }

    private void processReactions(Diagram diagram, String[] isMonomolecular)
    {
        for( int i = 0; i < reactionNodes.size(); ++i )
        {
            Reaction reaction = (Reaction)reactionNodes.get(i).getKernel();

            int ind = 0;
            for( SpecieReference sr : reaction.getSpecieReferences() )
            {
                if( sr.getRole().equals(SpecieReference.REACTANT) || sr.getRole().equals(SpecieReference.MODIFIER) )
                {
                    ind++;
                }
            }

            if( ind > 1 )
                isMonomolecular[i] = "-";
            else if( ind == 1 )
                isMonomolecular[i] = "+";
            else
                isMonomolecular[i] = " ";

            if( jobControl != null )
                jobControl.setPreparedness((int) ( ( (long)currentIteration++ * 100 ) / iterationsNumber ));
        }
    }

    private JavaSimulationEngine simulationEngine;

    /**
     * Reaction is pseudo-monomolecular if ratios of its reactants and modifiers
     * greater than the specified threshold or smaller than 1/threshold.
     */
    private String[] getPseudoMonomolecularReactions(Diagram diagram, SimulationResult ratiosSR, Span span, double threshold)
    {
        String[] isPseudoMonomolecular = new String[reactionNodes.size()];

        try
        {
            Diagram diagramClone = diagram.clone(null, diagram.getName());

            simulationEngine = new JavaSimulationEngine();
            simulationEngine.setDiagram(diagramClone);
            simulationEngine.setSpan(span);

            SimulationResult diagramSR = new SimulationResult(null, "");

            simulationEngine.simulate(simulationEngine.createModel(), diagramSR);

            Map<String, double[]> ratios = new HashMap<>();

            processReactions(diagramSR, ratios, threshold, isPseudoMonomolecular);
            AnalysisUtils.fillSimulationResult(simulationEngine, ratiosSR, ratios);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not get pseudo-monomolecular reactions, reason: " + e);
        }

        return isPseudoMonomolecular;
    }

    private void processReactions(SimulationResult simulationResult, Map<String, double[]> ratios, double threshold,
            String[] isPseudoMonomolecular)
    {
        reactionRatios = new ArrayList<>();

        ratios.put("time", simulationResult.getTimes());

        for( int i = 0; i < reactionNodes.size(); ++i )
        {
            DiagramElement de = reactionNodes.get(i);
            Reaction reaction = (Reaction)de.getKernel();

            List<String> species = new ArrayList<>();
            for( SpecieReference sr : reaction.getSpecieReferences() )
            {
                if( sr.getRole().equals(SpecieReference.REACTANT) || sr.getRole().equals(SpecieReference.MODIFIER) )
                {
                    species.add(sr.getSpecie());
                }
            }

            if( species.size() > 1 )
            {
                List<String> codeNames = new ArrayList<>();
                List<String> titles = new ArrayList<>();
                getCodeNamesAndTitles(de, species, codeNames, titles);

                Map<String, double[]> newRatios = new HashMap<>();
                isPseudoMonomolecular[i] = getRatios(simulationResult, newRatios, codeNames, titles, threshold);
                ratios.putAll(newRatios);
                reactionRatios.add(newRatios.keySet().toArray(new String[newRatios.keySet().size()]));
            }
            else
            {
                reactionRatios.add(null);
            }

            if( isPseudoMonomolecular[i] == null )
                isPseudoMonomolecular[i] = " ";

            if( jobControl != null )
                jobControl.setPreparedness((int) ( ( (long)currentIteration++ * 100 ) / iterationsNumber ));
        }
    }

    private List<String> getCodeNamesAndTitles(DiagramElement reactionNode, List<String> species, List<String> codeNames,
            List<String> titles)
    {
        try
        {
            Diagram diagram = Diagram.getDiagram(reactionNode);
            for( String sr : species )
            {
                DiagramElement de = diagram.findDiagramElement(sr);

                if( de != null && de.getRole() instanceof VariableRole )
                {
                    String name = de.getRole( VariableRole.class ).getName();
                    String codeName = simulationEngine.getVariableCodeName(diagram.getName(), name);
                    codeNames.add(codeName);
                    int mode = diagram.getViewOptions().getVarNameCode();
                    String title = diagram.getRole( EModel.class ).getQualifiedName( name, de, mode );
                    titles.add(title);
                }
                else
                {
                    codeNames.add(sr);
                    titles.add(sr);
                }
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        return codeNames;
    }

    private String getRatios(SimulationResult simulationResult, Map<String, double[]> ratios, List<String> codeNames, List<String> titles,
            double threshold)
    {
        boolean isSuitable = false;

        double[] times = simulationResult.getTimes();
        double[][] values = simulationResult.getValues();
        Map<String, Integer> variableMap = simulationResult.getVariableMap();

        for( int i = 0; i < codeNames.size(); ++i )
        {
            for( int j = i + 1; j < codeNames.size(); ++j )
            {
                String name1 = codeNames.get(i);
                String name2 = codeNames.get(j);

                int ind1 = variableMap.get(name1);
                int ind2 = variableMap.get(name2);

                double[] ratio = new double[times.length];
                for( int k = 0; k < times.length; ++k )
                {
                    if( values[k][ind1] != 0 )
                    {
                        if( values[k][ind2] <= 0 )
                        {
                            ratio[k] = Double.MAX_VALUE;
                        }
                        else
                        {
                            ratio[k] = values[k][ind1] / values[k][ind2];
                            if( ratio[k] < threshold && ratio[k] > 1 / threshold )
                            {
                                isSuitable = true;
                            }
                        }
                    }
                    // else if( values[k][ind2] == 0 )
                    // {
                    //   ratio[k] = Double.NaN;
                    // }
                }

                String name = titles.get(i) + "_to_" + titles.get(j);
                name = name.replace("$", "");
                name = name.replace("\"", "");
                name = name.replace(" ", "_");
                ratios.put(name, ratio);
            }
        }
        if( isSuitable )
            return "-";
        else
            return "+";
    }
}
