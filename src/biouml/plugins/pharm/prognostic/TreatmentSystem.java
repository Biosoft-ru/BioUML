package biouml.plugins.pharm.prognostic;

import java.awt.Color;
import java.awt.Paint;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jfree.chart.ChartColor;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.access.ChartDataElement;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.jobcontrol.JobControlException;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.TempFiles;

@PropertyName ( "Прогностическая система" )
public class TreatmentSystem extends AnalysisMethodSupport<TreatmentSystemParameters>
{
    private TreatmentJobControl jobControl;
    private final boolean debug = false;
    private SimulationEngine engine;

    public TreatmentSystem(DataCollection<?> origin, String name)
    {
        super(origin, name, new TreatmentSystemParameters());
        jobControl = new TreatmentJobControl(this);
    }

    private double currentProgress = 0;
    private double step;

    private List<TreatmentResult> results;

    private static Map<String, String> pressureTitle = new HashMap()
    {
        {
            put("P_ma2", "Среднее давление");
            put("Heart/P_S", "Систолическое давление");
            put("Heart/P_D", "Диастолическое давление");
            put("Kidney/P_ma2", "Среднее давление");
        }
    };

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        results = new ArrayList<>();
        step = 100.0 / parameters.getPopulationSize() / parameters.getDrugs().length;
        jobControl.pushProgress(0, 100);
        
        //1. load and filter population to represent given patient
        TableDataCollection populationOriginal = null;
        
        if( parameters.getRegime().equals(TreatmentSystemParameters.NORMAL) )        
            populationOriginal = parameters.getSlowPopulationPath().getDataElement(TableDataCollection.class);        
        else
            populationOriginal = parameters.getFastPopulationPath().getDataElement(TableDataCollection.class);
        
        TableDataCollection population = populationOriginal.clone(populationOriginal.getGroups(), "population");

        if( parameters.getRegime().equals(TreatmentSystemParameters.FAST) )
            processPopulationTable(population);

        int psIndex = population.getColumnModel().getColumnIndex("Heart\\P_S");
        int pdIndex = population.getColumnModel().getColumnIndex("Heart\\P_D");

        //        int pmaIndex = population.getColumnModel().getColumnIndex("P_ma2");

        double ps = parameters.getPressure().getPs();
        double pd = parameters.getPressure().getPd();

        double[] rating = new double[population.getSize()];

        for( int i = 0; i < population.getSize(); i++ )
        {
            Object[] values = population.getAt(i).getValues();
            rating[i] = Math.abs(ps - (double)values[psIndex]) + Math.abs(pd - (double)values[pdIndex]);
        }

        int[] pos = ru.biosoft.analysis.Util.sort(rating);

        File subPopulation = TempFiles.file("Sub_population");

        try (BufferedWriter bw = ApplicationUtils.utfWriter(subPopulation))
        {
            bw.write(readHeader(population));
            bw.write("\n");
            for( int i = 0; i < Math.min(parameters.getPopulationSize(), pos.length); i++ )
            {
                bw.write(population.getName(i) + "\t" + StreamEx.of(population.getAt(pos[i]).getValues()).joining("\t"));
                bw.write("\n");
            }
        }

        Map<String, Double> thresholds = new HashMap<>();

        if( parameters.getRegime().equals(TreatmentSystemParameters.NORMAL) )
        {
            thresholds.put("Heart/P_S", 20.0);
            thresholds.put("Heart/P_D", 20.0);
            thresholds.put("Kidney/P_ma2", 15.0);
        }
        else if( parameters.getRegime().equals(TreatmentSystemParameters.FAST) )
        {
            thresholds.put("P_ma2", 15.0);
        }

        int step = parameters.getDrugs().length * population.getSize();
        
        //        //2. treat filtered population
        for( String drug : parameters.getDrugs() )
        {
            Diagram diagram = parameters.getDiagram(drug);

            File treated = TempFiles.file("Treated_" + drug);
            File info = TempFiles.file("Info_" + drug);
            log.info("Лечение с помощью " + drug);
            treatPopulation(subPopulation, diagram, treated, info);
            collectStatistics(subPopulation, thresholds, treated, info, drug);            
        }
        this.createChart( xValues, yValues, names );
        return doReport();
    }

    List<double[]> xValues = new ArrayList<>();
    List<double[]> yValues = new ArrayList<>();
    List<String> names = new ArrayList<>();
    
    private void collectStatistics(File populationFile, Map<String, Double> thresholds, File treatedFile, File info, String drug)
            throws Exception
    {

        String initialHeader = ApplicationUtils.utfReader(populationFile).readLine();
        String treatedHeader = ApplicationUtils.utfReader(treatedFile).readLine();

        String[] initialNames = initialHeader.split("\t");
        String[] treatedNames = treatedHeader.split("\t");

        Set<String> observed = thresholds.keySet();

        Map<String, Integer> initialObserved = new HashMap<>();
        for( int i = 0; i < initialNames.length; i++ )
        {
            if( observed.contains(initialNames[i]) )
            {
                initialObserved.put(initialNames[i], i);
            }
        }

        Map<String, Integer> treatedObserved = new HashMap<>();
        for( int i = 0; i < treatedNames.length; i++ )
        {
            if( observed.contains(treatedNames[i]) )
            {
                treatedObserved.put(treatedNames[i], i);
            }
        }

        Map<String, String[]> initialData = StreamEx.of(ApplicationUtils.readAsList(populationFile)).skip(1).map(s -> s.split("\t"))
                .toMap(arr -> arr[0], arr -> arr);
        Map<String, String[]> treatedData = StreamEx.of(ApplicationUtils.readAsList(treatedFile)).skip(1).map(s -> s.split("\t"))
                .toMap(arr -> arr[0], arr -> arr);

        int total = initialData.size();
        int treatedNumber = 0;
        int badNumber = 0;
        int failedNumber = 0;
        int weakNumber = 0;

        Map<String, Set<Double>> dropData = new HashMap<>();

        for( Entry<String, String[]> initialEntry : initialData.entrySet() )
        {
            boolean isTreated = true;
            boolean badEffect = false;

            String key = initialEntry.getKey();
            if( !treatedData.containsKey(key) )
            {
                failedNumber++;
                continue;
            }
            String[] treated = treatedData.get(key);
            String[] initial = initialEntry.getValue();

            Map<String, Double> initialSelected = StreamEx.of(observed).toMap(s -> s,
                    s -> Double.parseDouble(initial[initialObserved.get(s).intValue()]));

            Map<String, Double> treatedSelected = StreamEx.of(observed).toMap(s -> s,
                    s -> Double.parseDouble(treated[treatedObserved.get(s).intValue()]));

            Map<String, Double> dropSelected = StreamEx.of(observed).toMap(s -> s, s -> initialSelected.get(s) - treatedSelected.get(s));

            for( String s : observed )
            {
                dropData.computeIfAbsent( s, k -> new HashSet<>() ).add( dropSelected.get( s ) );
            }

            for( Entry<String, Double> drop : dropSelected.entrySet() )
            {
                if( drop.getValue() < 0 )
                {
                    badEffect = true;
                }
                if( drop.getValue() < thresholds.get(drop.getKey()) )
                {
                    isTreated = false;
                }
                //                System.out.println(key+"\t"+drop.getValue());
                //                String paramName = drop.getKey();
                //                paramName = paramName.substring(paramName.lastIndexOf("\\")+1, paramName.length());
                //                File paramResult = new File(drugFolder+"/"+paramName);
                //                double[] treatData = new double[] {initialSelected.get(drop.getKey()), treatedSelected.get(drop.getKey()), drop.getValue()};
                //                this.writePatient(paramResult, treatData, key);

            }
            if( badEffect )
                badNumber++;
            else if( isTreated )
                treatedNumber++;
            else
                weakNumber++;
        }

        TreatmentResult treatmentResult = new TreatmentResult();
        treatmentResult.drug = drug;
        treatmentResult.total = total;
        treatmentResult.success = (double)treatedNumber / total * 100.0;
        treatmentResult.weakEffect = (double)weakNumber / total * 100.0;
        treatmentResult.negativeEffect = (double)badNumber / total * 100.0;
        treatmentResult.incorrect = (double)failedNumber / total * 100.0;

        
//        List<double[]> xValues = new ArrayList<>();
//        List<double[]> yValues = new ArrayList<>();
//        List<String> names = new ArrayList<>();
        for( String s : observed )
        {
            double[] dropValues = DoubleStreamEx.of(dropData.get(s)).toArray();
            List<double[]> values = Stat.getEmpiricalDensitySmoothedByEpanechninkov(dropValues, 0.1 * Stat.mean(dropValues), true);
            
            xValues.add( values.get( 0 ));
            yValues.add( values.get( 1 ) );
            names.add(drug);
        }
        
        
        
        if( !dropData.isEmpty() )
        {
            for( String s : observed )
            {
                double[] dropValues = DoubleStreamEx.of(dropData.get(s)).toArray();
                treatmentResult.addDrop(pressureTitle.get(s), format(ru.biosoft.analysis.Stat.mean(dropValues)),
                        format(Math.sqrt(ru.biosoft.analysis.Stat.variance(dropValues))));
            }
        }
        else
        {
            log.info("Warning! No patients were treated.");
        }
        results.add(treatmentResult);
    }

    private String format(double val)
    {
        return String.format("%.2f", val);
    }
    Map<String, Integer> mapping;

    private void treatPopulation(File population, Diagram diagram, File treated, File info) throws Exception
    {
        removeInitialEquations(diagram);
        engine = DiagramUtility.getPreferredEngine(diagram);
        engine.setDiagram(diagram);
        engine.setLogLevel( Level.SEVERE );

        Model model = engine.createModel();
        model.init();

        double[] modelValues = model.getCurrentValues();

        Map<String, Integer> mapping = engine.getVarPathIndexMapping();
        write(treated, StreamEx.of(EntryStream.of(mapping).invert().toSortedMap().values()).prepend("ID").joining("\t") + "\n");
        Set<String> drugParameters = StreamEx.of(parameters.getDrugParameters()).toSet();
        
        try (BufferedReader br = ApplicationUtils.utfReader(population))
        {
            String header = br.readLine();
            String[] names = header.split("\t");
            
            String line = br.readLine();

            while( line != null  && !jobControl.isStopped())
            {
                String[] values = line.split("\t");
                double[] doubleVals = StreamEx.of(values).mapToDouble(v -> Double.parseDouble(v)).toArray();

                Set<String> missedFromTheModel = new HashSet<>();

                Set<Integer> setIndices = new HashSet<>();
               
                for( int i = 0; i < names.length; i++ )
                {
                    double paramValue = doubleVals[i];
                    String name = names[i];
                    
                    if (drugParameters.contains(name))
                        continue;
                    
                    
                    Integer index = mapping.get(name);
                    if( !names[i].contains("time") && index != null )
                    {
                        modelValues[index] = paramValue;
                        setIndices.add(index);
                    }
                    else if( debug )
                        missedFromTheModel.add(names[i]);
                }

                if( debug )
                {
                    Set<String> missedVariables = new HashSet<>();
                    for( Entry<String, Integer> e : mapping.entrySet() )
                    {
                        if( !setIndices.contains(e.getValue()) )
                        {
                            missedVariables.add(e.getKey());
                        }
                    }


                    log.info("Next variables were not specified: " + StreamEx.of(missedVariables).joining("\t"));
                    log.info("Next variables were not found in the model: " + StreamEx.of(missedFromTheModel).joining("\t"));
                }
                String id = values[0];

                model.init();
                model.setCurrentValues(modelValues);
//                engine.setCompletionTime(parameters.getTime() * 60 * 60 ); //convert to hours
//                engine.setTimeIncrement( 60 );
                engine.removeAllListeners();

                if( parameters.isShowPlots() )
                    engine.simulate(model, getResultListeners(engine));
                else
                    engine.simulate(model);
                String err = engine.getSimulator().getProfile().getErrorMessage();

                if( err != null ) //patient failed
                {
                    write(info, id + "\t" + err + "\n");
                }
                else
                {
                    this.writePatient(treated, model.getCurrentState(), id);
                }
                line = br.readLine();

                currentProgress += step;
                this.jobControl.setPreparedness((int) ( currentProgress ));
            }
        }

    }

    private void writeHeader(File result, String values) throws IOException
    {
        write(result, StreamEx.of(values).prepend("ID").joining("\t") + "\n");
    }

    private void writePatient(File result, double[] values, String id) throws IOException
    {
        write(result, id + "\t" + DoubleStreamEx.of(values).joining("\t") + "\n");
    }

    private void write(File result, String line) throws IOException
    {
        try (BufferedWriter bw = ApplicationUtils.utfAppender(result))
        {
            bw.append(line);
        }
    }

    public static void removeInitialEquations(Diagram d) throws Exception
    {
        for( Equation eq : d.getRole(EModel.class).getInitialAssignments() )
            d.getType().getSemanticController().remove(eq.getDiagramElement());

        for( SubDiagram subDiagram : Util.getSubDiagrams(d) )
        {
            Diagram diagram = subDiagram.getDiagram();
            for( Equation eq : diagram.getRole(EModel.class).getInitialAssignments() )
                diagram.getType().getSemanticController().remove(eq.getDiagramElement());
        }
    }


    private void processPopulationTable(TableDataCollection table)
    {
        for( int i = 0; i < table.getColumnModel().getColumnCount(); i++ )
        {
            TableColumn column = table.getColumnModel().getColumn(i);
            String name = column.getName();
            if( name.startsWith("Kidney\\") )
            {
                column.setName(name.substring(7));
            }
        }
    }

    private String readHeader(TableDataCollection table)
    {
        return StreamEx.of(table.getColumnModel().iterator()).map(c -> c.getName().replace("\\", "/")).prepend("ID").joining("\t");
    }

    Template velocityTemplate;

    private HtmlDataElement doReport() throws Exception
    {
        Properties p = new Properties();
        File templateFile = TempFiles.file("Report.vm");

        try (BufferedWriter bw = ApplicationUtils.utfWriter(templateFile);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(getClass().getResourceAsStream("report.vm"), StandardCharsets.UTF_8)))
        {
            bw.write(StreamEx.of(reader.lines()).joining("\n"));
        }

        p.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        p.setProperty("file.resource.loader.path", templateFile.getParentFile().getAbsolutePath());

        final VelocityEngine engine = new VelocityEngine(p);
        engine.init();

        Template velocityTemplate = engine.getTemplate(templateFile.getName(), "UTF-8");

        VelocityContext context = new VelocityContext();

        context.put("patient", this.getParameters());
        context.put("analysis", this);
        context.put( "figure", this.getParameters().getResultPath().getName()+"_chart" );
        File resultFile = TempFiles.file("Result");
        try (BufferedWriter bw = ApplicationUtils.utfWriter(resultFile))
        {
            velocityTemplate.merge(context, bw);
        }
        HtmlDataElement html = new HtmlDataElement(parameters.getResultPath().getName(), parameters.getResultPath().getParentCollection(),
                ApplicationUtils.readAsString(resultFile));
        parameters.getResultPath().getParentCollection().put(html);

        return html;
    }

    public TreatmentResult[] getResults()
    {
        return StreamEx.of(results).toArray(TreatmentResult[]::new);
    }

    Model model;

    protected ResultListener[] getResultListeners(SimulationEngine simulationEngine) throws Exception
    {
        PlotInfo[] plotInfos = simulationEngine.getPlots();
        ResultListener[] result = new ResultListener[plotInfos.length];
        for( int i = 0; i < plotInfos.length; i++ )
            result[i] = new ResultPlotPane(simulationEngine, null, plotInfos[i]);
        return result;
    }


    public class TreatmentJobControl extends AnalysisJobControl
    {
        public TreatmentJobControl(AnalysisMethodSupport<?> method)
        {
            super(method);
        }

        @Override
        protected void doRun() throws JobControlException
        {
            try
            {
                Object result = justAnalyzeAndPut();
                resultsAreReady(result == null ? null : new Object[] {result});
            }
            catch( Exception e )
            {
                log.severe(ExceptionRegistry.log(e));
                throw new JobControlException(e);
            }
        }

        @Override
        protected void setTerminated(int status)
        {
            if( engine != null )
                engine.stopSimulation();
            super.setTerminated(status);
        }
    }
    
    @Override
    public AnalysisJobControl getJobControl()
    {
        return jobControl;
    }
    
    public void createChart(List<double[]> xValuesForCurves, List<double[]> yValuesForCurves, List<String> curveNames)
    {
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        AxisOptions xAxis = new AxisOptions();
        xAxis.setLabel( "Pressure drop" );
        options.setXAxis( xAxis );
        AxisOptions yAxis = new AxisOptions();
        yAxis.setLabel( "Density" );
        options.setYAxis( yAxis );
        chart.setOptions( options );

        Color[] POSSIBLE_COLORS = new Color[] {Color.red, Color.blue, Color.green,
                Color.magenta, Color.cyan, Color.darkGray, Color.BLACK,
                Color.lightGray, Color.orange};
        
        for( int iCurve = 0; iCurve < xValuesForCurves.size(); iCurve++ )
        {
            double[] x = xValuesForCurves.get( iCurve );
            double[] y = yValuesForCurves.get( iCurve );

            int[] pos = ru.biosoft.analysis.Util.sort( x );
            double[] newValues = new double[x.length];
            for( int i = 0; i < y.length; i++ )
                newValues[i] = y[pos[i]];
            
            
            ChartSeries series = new ChartSeries( x, newValues );
            series.setLabel( curveNames.get( iCurve ) );
            series.setColor( POSSIBLE_COLORS[iCurve] );
            chart.addSeries( series );
        }
        ChartDataElement cde = new ChartDataElement(parameters.getResultPath().getName()+"_chart", parameters.getResultPath().getParentCollection(), chart);
        cde.getOrigin().put( cde );                
    }
}