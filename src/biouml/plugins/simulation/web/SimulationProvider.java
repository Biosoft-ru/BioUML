package biouml.plugins.simulation.web;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.JSONArray;

import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.WriterHandler;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import biouml.model.Diagram;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.server.access.AccessProtocol;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.SimulationAnalysis;
import biouml.plugins.simulation.SimulationAnalysisParameters;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineWrapper;
import biouml.plugins.simulation.document.InputParameter;
import biouml.plugins.simulation.document.InteractiveSimulation;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.simulation.StochasticSimulationResult;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.jobcontrol.ClassJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListenerAdapter;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebJob;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.DPSUtils;

public class SimulationProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        switch(arguments.getAction())
        {
            case "simulate":
            {
                //simulateDiagram(arguments.getDataElement( Diagram.class ), arguments.getJSONArray("engine"), response);
                String jobID = arguments.get("jobID");
                simulateDiagramWithAnalysis(arguments.getDataElement(Diagram.class), jobID, arguments.getJSONArray("engine"), response);
                return;
            }
            case "status":
                sendSimulationStatus(arguments.getString(AccessProtocol.KEY_DE), response);
                return;
            case "stop":
                stopSimulation(arguments.getString(AccessProtocol.KEY_DE), response);
                return;
            case "result":
                sendSimulationResult(arguments.getString(AccessProtocol.KEY_DE), response);
                return;
            case "save_options":
                saveSimulatorOptions( arguments.getDataElement( Diagram.class ), arguments.getString( "engineBean" ), response );
                return;
            case "save_result":
                saveSimulationResult( arguments.getDataElementPath( AccessProtocol.KEY_DE ), arguments.getString( "jobID" ), response );
                return;
            case "simulation_document_create":
                createSimulationDocument( arguments.getDataElement( Diagram.class ), response );
                return;
            case "simulation_document_plot":
                sendSimulationPlot( arguments.getString( AccessProtocol.KEY_DE ), response );
                return;
            case "parameters_reset":
                resetSimulationParameters( arguments.getString( AccessProtocol.KEY_DE ),
                        Arrays.asList( arguments.getStrings( "jsonrows" ) ), response );
                return;
            case "parameters_increase":
                increaseSimulationParameters( arguments.getString( AccessProtocol.KEY_DE ),
                        Arrays.asList( arguments.getStrings( "jsonrows" ) ), response );
                return;
            case "parameters_decrease":
                decreaseSimulationParameters( arguments.getString( AccessProtocol.KEY_DE ),
                        Arrays.asList( arguments.getStrings( "jsonrows" ) ), response );
                return;
            case "simulation_document_update":
                updateSimulation( arguments.getString( AccessProtocol.KEY_DE ), response );
                return;
            case "save_to_diagram":
                saveParametersToDiagram( arguments.getString( AccessProtocol.KEY_DE ), response );
                return;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION );
        }
    }

    private boolean logInitialized = false;
    private static ConcurrentHashMap<String, StringBuffer> buffers;
    private static ConcurrentHashMap<String, SimulationWebResultHandler> job2handler;

    private void initLogging()
    {
        if ( logInitialized )
            return;

        synchronized (this)
        {
            if ( logInitialized )
                return;
            job2handler = new ConcurrentHashMap<String, SimulationWebResultHandler>();
            logInitialized = true;
        }

    }

    public static class SessionWriter extends Writer
    {
        StringBuffer buffer = new StringBuffer();

        @Override
        public void write(char[] bytes, int offset, int len) throws IOException
        {
            String curSession = SecurityManager.getSession();
            buffers.computeIfAbsent( curSession, s -> new StringBuffer() ).append( bytes, offset, len );
        }

        @Override
        public void flush() throws IOException
        {

        }

        @Override
        public void close() throws IOException
        {
        }
    }

    /**
     * Start diagram simulation via SimulationAnalysis
     * @throws IOException
     */

    private void simulateDiagramWithAnalysis(Diagram diagram, String jobID, JSONArray jsonParams, JSONResponse response) throws IOException
    {
        initLogging();
        SimulationEngineWrapper simulationEngineWrapper = new SimulationEngineWrapper(diagram);
        JSONUtils.correctBeanOptions(simulationEngineWrapper, jsonParams);
        SimulationEngine simulationEngine = simulationEngineWrapper.getEngine();

        SimulationAnalysis method = new SimulationAnalysis(diagram, "Simulation analysis");
        SimulationAnalysisParameters simulationParameters = method.getParameters();
        simulationParameters.setSimulationEngine(simulationEngine);

        SimulationWebResultHandler swrh = new SimulationWebResultHandler(simulationEngine);
        for ( ResultListener listener : swrh.getResultListeners(diagram.getCompletePath()) )
            method.addResultListenerList(listener);
        job2handler.put(jobID, swrh);

        final WebJob webJob = WebJob.getWebJob(jobID);
        final Logger log = webJob.getJobLogger();
        method.setLogger(log);
        ClassJobControl jobControl = method.getJobControl();
        Writer writer = new Writer()
        {
            StringBuffer buffer = new StringBuffer();

            @Override
            public void close() throws IOException
            {
            }

            @Override
            public void flush() throws IOException
            {
                webJob.addJobMessage(buffer.toString());
                buffer = new StringBuffer();
            }

            @Override
            public void write(char[] bytes, int offset, int len) throws IOException
            {
                buffer.append(bytes, offset, len);
            }
        };

        final Handler webLogHandler = new WriterHandler(writer, new PatternFormatter("%4$s - %5$s%n"));
        webLogHandler.setLevel(Level.ALL);

        final Logger cat = Logger.getLogger("biouml.plugins.simulation");
        cat.addHandler(webLogHandler);

        log.setLevel(Level.ALL);
        log.addHandler(webLogHandler);

        TaskManager taskManager = TaskManager.getInstance();
        TaskInfo taskInfo = taskManager.addAnalysisTask(method, false);
        webJob.setTask(taskInfo);
        taskInfo.setTransient("parameters", method.getParameters());

        jobControl.addListener(new JobControlListenerAdapter()
        {
            @Override
            public void jobTerminated(JobControlEvent event)
            {
                log.removeHandler(webLogHandler);
                cat.removeHandler(webLogHandler);
            }
        });
        taskManager.runTask(taskInfo);
        response.sendStringArray(new String[] { jobID, String.valueOf(simulationEngine.getPlots().length) });
    }

    /**
     * Get simulation status by web job ID
     * 
     * @throws IOException
     */
    private static void sendSimulationStatus(String jobId, JSONResponse response) throws IOException
    {
        WebJob webJob = WebJob.getWebJob(jobId);
        JobControl jobControl = webJob.getJobControl();
        SimulationWebResultHandler swrh = job2handler.get(jobId);
        if ( swrh != null )
        {
            int status = jobControl.getStatus();
            String[] imageNames = null;
            BufferedImage[] resultImages = swrh.generateResultImage();
            if ( resultImages != null )
            {
                imageNames = new String[resultImages.length + 1];
                for ( int i = 0; i < resultImages.length; i++ )
                {
                    imageNames[i] = jobId + "_img_" + i;
                    WebSession.getCurrentSession().putImage(imageNames[i], resultImages[i]);
                }
                String message = webJob.getJobMessage();
                imageNames[imageNames.length - 1] = message;
            }

            if ( status < JobControl.COMPLETED )
            {
                int percent = swrh.getPreparedness();
                response.sendStatus(status, percent, imageNames);
            }
            else if ( status == JobControl.COMPLETED || status == JobControl.TERMINATED_BY_REQUEST )
            {
                //job2handler.remove(jobId);
                response.sendStatus(status, 100, imageNames);
            }
            else if ( status == JobControl.TERMINATED_BY_ERROR ) //ended with non-processed error in log
            {
                //job2handler.remove(jobId);
                String message = webJob.getJobMessage();
                if ( message == null )
                    message = "Can not simulate model. Error not specified.";
                response.error(message);
            }
        }
        else
        {
            response.error("Invalid process ID specified: "+jobId);
        }
    }
    
    private static void stopSimulation(String jobId, JSONResponse response) throws IOException
    {
        WebJob webJob = WebJob.getWebJob(jobId);
        JobControl jobControl = webJob.getJobControl();
        SimulationWebResultHandler swrh = job2handler.get(jobId);
        if ( swrh != null )
        {
            jobControl.terminate();
            String message = webJob.getJobMessage();
            if( message == null )
                message = "Can not simulate model. Error not specified.";
            //job2handler.remove(jobId);
            response.sendStringArray( "Simulation terminated", message );
        }
        else
        {
            response.error("Invalid process ID specified: "+jobId);
        }
    }
    
    private static void sendSimulationResult(String jobId, JSONResponse response) throws IOException
    {
        SimulationWebResultHandler swrh = job2handler.get(jobId);
        SimulationResult simulationResult = null;
        if ( swrh != null )
        {
            simulationResult = swrh.getSimulationResult();
        }
        else
        {
            DataElementPath simulationResultPath = DataElementPath.create(jobId);
            simulationResult = simulationResultPath.optDataElement( SimulationResult.class );
        }

        if( simulationResult != null )
        {
            JsonValue json = simulationResultToJSON(simulationResult);
            response.sendJSON( json );
        }
        else
        {
            response.error("Invalid process ID specified: " + jobId);
        }
    }

    private static JsonValue simulationResultToJSON(SimulationResult simulationResult)
    {
        JsonObject res = Json.object();
        
        JsonObject vars = Json.object();
        simulationResult.getVariablePathMap().forEach(vars::add);
        res.add( "vars", vars );
        
        JsonArray times = Json.array( simulationResult.getTimes() );
        res.add( "times", times );
        
        int nData = simulationResult.getValue(0).length;
        int nPoints = times.size();
        //TODO for @axec: fix getCount() in StochasticSimulationResult
        double[][] values = new double[nData][nPoints];
        for(int t = 0; t < nPoints; t++)
        {
            double[] valuesAtT = simulationResult.getValue( t );
            for (int i=0; i<nData; i++)                
                values[i][t] = valuesAtT[i];                    
        }
        JsonArray jsonValues = new JsonArray();
        for(int i = 0; i < nData; i++)
        {
            jsonValues.add( Json.array( values[i] ) );
        }
        res.add( "values", jsonValues );

        if (simulationResult instanceof StochasticSimulationResult)
        {
            res.add("Q1", toJsonArray(transpose(( (StochasticSimulationResult)simulationResult ).getQ1())));
            res.add("Q2", toJsonArray(transpose(( (StochasticSimulationResult)simulationResult ).getMedian())));
            res.add("Q3", toJsonArray(transpose(( (StochasticSimulationResult)simulationResult ).getQ3())));            
        }
        return res;
    }
    
    private static JsonArray toJsonArray(double[][] array)
    {
        JsonArray jsonValues = new JsonArray();
        for(int i = 0; i < array.length; i++)
            jsonValues.add( Json.array( array[i] ) );
        return jsonValues;
    }
    
    private static double[][] transpose(double[][] array)
    {
        double[][] result = new double[array[0].length][array.length];
        for (int i=0; i< result.length; i++)
            for (int j=0; j<array.length; j++)
                result[i][j] = array[j][i];
        return result;
    }
    
    /**
     * Save simulation options to diagram attributes
     * Diagram should be saved as document from interface to store changes
     */
    private void saveSimulatorOptions(Diagram diagram, String engineBean, JSONResponse response) throws Exception
    {
        WebServicesServlet.getSessionCache().setObjectChanged( diagram.getCompletePath().toString(), diagram );
        Object bean = WebBeanProvider.getBean( engineBean );
        if( bean != null && bean instanceof SimulationEngineWrapper )
        {
            diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( "simulationOptions", SimulationEngine.class,
                    ( (SimulationEngineWrapper)bean ).getEngine() ) );
            response.sendString( "ok" );
        }
        else
            response.error( "Can not find bean for simulation engine" );
    }


    /**
     * Save simulation result from cache to tree
     */
    private void saveSimulationResult(DataElementPath dataElementPath, String jobID, JSONResponse response) throws IOException
    {
        SimulationWebResultHandler swrh = job2handler.get(jobID);
        if ( swrh != null )
        {
            SimulationResult tmpResult = swrh.getSimulationResult();
            SimulationResult simulationResult = tmpResult.clone( dataElementPath.getParentCollection(), dataElementPath.getName( ));            
            CollectionFactoryUtils.save( simulationResult );
            response.sendString( "ok" );
        }
        else
            response.error( "Can not find simulation result. Please, run simulation again." );
    }

    public static InteractiveSimulation getInteractiveSimulation(String simulationDe) throws WebException
    {
        Object cachedObj = WebServicesServlet.getSessionCache().getObject( simulationDe );
        if( cachedObj instanceof InteractiveSimulation )
            return (InteractiveSimulation)cachedObj;
        else
            throw new WebException( "EX_QUERY_NO_ELEMENT", simulationDe );
    }

    private static void createSimulationDocument(Diagram diagram, JSONResponse response) throws Exception
    {
        String simulationName = "Simulation " + diagram.getName();
        //TODO: think of nicer naming of simulation documents
        String completeSimulationName = DataElementPath.create( simulationName )
                .getChildPath( diagram.getCompletePath().getPathComponents() ).toString();
        InteractiveSimulation simulation = new InteractiveSimulation( null, simulationName, diagram );
        PlotsInfo plotsInfo = DiagramUtility.getPlotsInfo( simulation.getDiagram() );
        if( plotsInfo == null )
        {
            response.error( "No plots specified for diagram. Can not create document!" );
            return;
        }
        Set<String> names = new HashSet<String>();
        PlotInfo[] plotInfos = plotsInfo.getActivePlots();
        for( PlotInfo plotInfo : plotInfos )
        {
            for( Curve c : plotInfo.getYVariables() )
                names.add( c.getCompleteName() );
            PlotVariable xVar = plotInfo.getXVariable();
            names.add( xVar.getCompleteName() );
        }

        simulation.setOutputNames( names );
        simulation.doSimulation();
        WebServicesServlet.getSessionCache().addObject( completeSimulationName, simulation, true );
        response.sendString( completeSimulationName );
    }
    //TODO: move doSimulation to plot method and remove from all other points
    private static void updateSimulation(String simulationDe, JSONResponse response) throws IOException, WebException
    {
        InteractiveSimulation simulation = getInteractiveSimulation( simulationDe );
        simulation.getInputParameters().stream().forEach( parameter -> simulation.updateValue( parameter ) );
        simulation.doSimulation();
        response.sendString( "ok" );
    }

    private static void sendSimulationPlot(String simulationDe, JSONResponse response) throws IOException, WebException
    {
        InteractiveSimulation simulation = getInteractiveSimulation( simulationDe );
        PlotsInfo plotsInfo = DiagramUtility.getPlotsInfo( simulation.getDiagram() );
        PlotInfo[] plotInfos = plotsInfo.getActivePlots();
        String[] imageNames = null;
        Map<String, double[]> values = simulation.getResult();
        BufferedImage[] resultImages = StreamEx.of( plotInfos ).map( p -> {
            WebSimplePlotPane wp = new WebSimplePlotPane( 700, 500, p );
            wp.redrawChart( values );
            return wp;
        } ).map( p -> p.getImage() ).toArray( BufferedImage[]::new );

        if( resultImages != null )
        {
            imageNames = new String[resultImages.length];
            for( int i = 0; i < resultImages.length; i++ )
            {
                imageNames[i] = simulationDe + "_img_" + i;
                WebSession.getCurrentSession().putImage( imageNames[i], resultImages[i] );
            }
            response.sendStringArray( imageNames );
        }
        else
            response.sendStringArray( new String[0] );
    }

    private void resetSimulationParameters(String simulationDe, List<String> asList, JSONResponse response) throws IOException, WebException
    {
        InteractiveSimulation simulation = getInteractiveSimulation( simulationDe );
        if( asList.isEmpty() )
            simulation.resetParameters();
        else
            asList.stream().forEach( parameter -> {
                InputParameter param = simulation.getParameter( parameter );
                if( param != null )
                    simulation.resetParameter( param );
            } );
        simulation.doSimulation();
        response.sendString( "ok" );

    }

    private void increaseSimulationParameters(String simulationDe, List<String> asList, JSONResponse response)
            throws IOException, WebException
    {
        InteractiveSimulation simulation = getInteractiveSimulation( simulationDe );
        asList.stream().forEach( parameter -> {
            InputParameter selected = simulation.getParameter( parameter );
            selected.setValue( selected.getValue() + selected.getValueStep() );
            simulation.updateValue( selected );
        } );
        simulation.doSimulation();
        response.sendString( "ok" );
    }

    private void decreaseSimulationParameters(String simulationDe, List<String> asList, JSONResponse response)
            throws IOException, WebException
    {
        InteractiveSimulation simulation = getInteractiveSimulation( simulationDe );
        asList.stream().forEach( parameter -> {
            InputParameter selected = simulation.getParameter( parameter );
            selected.setValue( selected.getValue() - selected.getValueStep() );
            simulation.updateValue( selected );
        } );
        simulation.doSimulation();
        response.sendString( "ok" );
    }

    private void saveParametersToDiagram(String simulationDe, JSONResponse response) throws IOException, WebException
    {
        InteractiveSimulation simulation = getInteractiveSimulation( simulationDe );
        simulation.saveParametersToDiagram();
        response.sendString( "ok" );
    }

    private static class WebSimplePlotPane
    {
        protected static final Logger log = Logger.getLogger( WebSimplePlotPane.class.getName() );
        protected JFreeChart chart;
        private XYLineAndShapeRenderer renderer;
        private PlotInfo plotInfo;
        private String xVariable;
        private int width, height;

        public WebSimplePlotPane(int ix, int iy, PlotInfo plotInfo)
        {
            this.width = ix;
            this.height = iy;

            try
            {
                chart = ChartFactory.createXYLineChart( plotInfo.getTitle(), "Axis (X)", "Axis (Y)", null, //dataset,
                        PlotOrientation.VERTICAL, true, // legend
                        true, // tool tips
                        false // URLs
                );

                this.plotInfo = plotInfo;
                xVariable = this.plotInfo.getXVariable().getName();
                chart.getXYPlot().setBackgroundPaint( Color.white );
                chart.setBackgroundPaint( Color.white );
                XYSeriesCollection dataset = new XYSeriesCollection();
                chart.getXYPlot().setDataset( dataset );
                renderer = new XYLineAndShapeRenderer();
                renderer.setDrawSeriesLineAsPath( true );
                if( plotInfo.getExperiments() != null )
                    ResultPlotPane.addExperiments( plotInfo.getExperiments(), renderer, dataset, Double.MAX_VALUE );
                int counter = chart.getXYPlot().getDataset().getSeriesCount();
                for( Curve c : plotInfo.getYVariables() )
                {
                    renderer.setSeriesPaint( counter, c.getPen().getColor() );
                    renderer.setSeriesStroke( counter, c.getPen().getStroke() );
                    renderer.setSeriesShapesVisible( counter, false );
                    renderer.setSeriesLinesVisible( counter, true );
                    renderer.setSeriesShape( counter, null );
                    dataset.addSeries( new XYSeries( c.getCompleteName(), false, true ) );
                    counter++;
                }
                chart.getXYPlot().setRenderer( renderer );
            }
            catch( Exception ex )
            {
                log.log( Level.SEVERE, "Error occured while creating chart panel: " + ex );
            }
        }

        public void redrawChart(Map<String, double[]> values)
        {
            renderer.setDrawSeriesLineAsPath( true );

            double[] x = values.get( xVariable );
            XYSeriesCollection xyDataset = (XYSeriesCollection)chart.getXYPlot().getDataset();
            for( Curve c : plotInfo.getYVariables() )
            {
                XYSeries series = xyDataset.getSeries( c.getCompleteName() );
                series.clear();
                double[] y = values.get( c.getCompleteName() );

                for( int i = 0; i < x.length; i++ )
                    series.add( x[i], y[i] );
            }
        }

        public BufferedImage getImage()
        {
            return chart.createBufferedImage( width, height );
        }
    }
}
