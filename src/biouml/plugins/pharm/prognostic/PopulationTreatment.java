package biouml.plugins.pharm.prognostic;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jfree.chart.axis.CategoryLabelPositions;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.javascript.StatisticalBarChart;
import biouml.plugins.simulation.SimulationTaskParameters;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortProperties;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.standard.type.Stub.ConnectionPort;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.pharm.prognostic.PatientPhysiology.Gender;
import biouml.plugins.pharm.prognostic.PatientPhysiology.Race;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ParallelSimulationEngine;
import biouml.plugins.simulation.ParallelSimulationEngine.SimulationTaskFactory;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationTask;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.HtmlDataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TempFiles;

public class PopulationTreatment extends AnalysisMethodSupport<PopulationTreatmentParameters>
{
    private static final double FAST_MODEL_ATOL = 1E-6;
    private static final double FAST_MODEL_RTOL = 1E-6;
    private static final double SLOW_MODEL_ATOL = 1.0E-20;
    private static final double SLOW_MODEL_RTOL = 1.0E-12;

    private static final int IMG_HEIGHT = 500;
    private static final int IMG_BASE_WIDTH = 100;
    private static final int IMG_BAR_WIDTH = 40;

    private ParallelSimulationEngine parallelEngine;
    private int numOfVariables;

    private int currPatient;
    private int allPatients;
    private String currDrug;
    private int identifier;

    private FunctionJobControl nestedJobControl = null;

    public PopulationTreatment(DataCollection<?> origin, String name)
    {
        super( origin, name, new PopulationTreatmentParameters() );
        log = Logger.getLogger( PopulationTreatment.class.getName() );
    }

    @Override
    protected AnalysisJobControl createJobControl()
    {
        return new AnalysisJobControl( this )
        {
            @Override
            protected void setTerminated(int status)
            {
                super.setTerminated( status );
                if( nestedJobControl != null )
                    nestedJobControl.terminate();
            }
        };
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        if( parameters.getOutput() == null )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_OUTPUT" ) );

        if( parameters.getPopulation() == null )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_POPULATION" ) );

        if( parameters.getDrugs() == null || parameters.getDrugs().length == 0 )
            throw new IllegalArgumentException( MessageBundle.getMessage( "ILLEGAL_DRUGS" ) );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
    	DataCollection<?> input = parameters.getInput().getDataCollection();
        DataCollection<?> output = DataCollectionUtils.createSubCollection( parameters.getOutput() );

        log.info( MessageBundle.getMessage( "INFO_INITIALIZATION" ) );

        DataElementPath statisticsPath = DataElementPath.create( output, "Statistics" );
        TableDataCollection stat = TableDataCollectionUtils.createTableDataCollection( statisticsPath );

        stat.getColumnModel().addColumn( MessageBundle.getMessage("SBP"), DataType.Text );
        stat.getColumnModel().addColumn( MessageBundle.getMessage("DECREASE_IN_SBP"), DataType.Text );
        stat.getColumnModel().addColumn( MessageBundle.getMessage("DBP"), DataType.Text );
        stat.getColumnModel().addColumn( MessageBundle.getMessage("DECREASE_IN_DBP"), DataType.Text );

        TableDataCollection statForImage = new StandardTableDataCollection(null, "stat");
        statForImage.getColumnModel().addColumn("SBP_mean", DataType.Float );
        statForImage.getColumnModel().addColumn("SBP_sd", DataType.Float );
        statForImage.getColumnModel().addColumn("DBP_mean", DataType.Float );
        statForImage.getColumnModel().addColumn( "DBP_sd", DataType.Float );

        Diagram composite = initDiagram( input, output );

        TableDataCollection population = parameters.getPopulation().getDataElement( TableDataCollection.class );
        TableDataCollection tPopulation = truncPopulation( population, composite );

        TableDataCollection paramMapping = (TableDataCollection)input.get( "ParameterMapping" );

        addStatForImageRow( statForImage, "Baseline", population, paramMapping );

        allPatients = population.getSize();

        SimulationTaskParameters simulationParameters = initSimulationParameters( composite );

        //writing headers
        SimulationEngine engine = simulationParameters.getSimulationEngine();
        engine.createModel();
        Map<String, Integer> mapping = engine.getVarPathIndexMapping();

        numOfVariables = mapping.size();

        //removing the output of unnecessary messages to the log
        Logger sslog = Logger.getLogger( SimulatorSupport.class.getName() ); //logger in JVode
        Level level = sslog.getLevel();
        sslog.setLevel( Level.SEVERE );

        if( tPopulation.getColumnModel().getColumnCount() > 0 )
        {
            log.info( MessageBundle.getMessage( "INFO_TREATMENT_SIMULATION" ) );
            String[] varNames = tPopulation.getColumnModel().stream().map( column -> getVariableName( column.getName() ) )
                    .toArray( String[]::new );
            double[][] varValues = tPopulation.stream().map( row -> getValues( row ) ).toArray( double[][]::new );

            identifier = 0;
            for( int i = 0; i < parameters.getDrugs().length; ++i )
                if( !jobControl.isStopped() )
                {
                    currDrug = parameters.getDrugs()[i];
                    currPatient = 0;

                    DataElementPath resultPath = DataElementPath.create( output, generateName( currDrug ) );
                    TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( resultPath );

                    for( String column : EntryStream.of( mapping ).invert().toSortedMap().values() )
                    {
                        String id = column.replace( "_ss", "" ).replace( "/", "_" );
                        result.getColumnModel().addColumn( id, DataType.Float );
                    }

                    Diagram cloneComposite = composite.clone( composite.getOrigin(), composite.getName() );
                    String[] drugVariables = PopulationTreatmentParameters.getDrugVariables( currDrug );

                    setDrugVariables( drugVariables, cloneComposite );

                    parallelEngine = new ParallelSimulationEngine( cloneComposite, simulationParameters, initSimulationTaskFactory() );

                    nestedJobControl = new FunctionJobControl( log );
                    nestedJobControl.begin();
                    double[][] treated = parallelEngine.simulate( varValues, varNames, nestedJobControl, true );
                    nestedJobControl.end();
                    nestedJobControl = null;

                    for( int k = 0; k < treated.length; ++k )
                        if( !Double.isNaN( treated[k][0] ) )
                        {
                            String rowName = population.getAt( k ).getName();
                            Double[] values = ArrayUtils.toObject( treated[k] );
                            TableDataCollectionUtils.addRow( result, rowName, values, true );
                        }
                        else
                        {
                            log.info( MessageBundle.format( "INFO_PATIENT_IS_FAILED", population.getAt( k ).getName() ) );
                        }
                    result.finalizeAddition();
                    resultPath.save( result );

                    addStatRow(stat, currDrug, population, result, paramMapping);
                    addStatForImageRow( statForImage, currDrug, result, paramMapping );
                }

            stat.finalizeAddition();
            statisticsPath.save( stat );

            statForImage.finalizeAddition();

            sslog.setLevel( level );
            return getReport( output, population, stat, statForImage, paramMapping );
        }
        sslog.setLevel( level );
        return null;
    }

    private String generateName(String drugs)
    {
        String[] items = PopulationTreatmentParameters.getDrugVariables( drugs );
        String name = "";
        for( int i = 0; i < items.length; ++i )
        {
            if( i != items.length - 1 )
                name += items[i] + "_";
            else
                name += items[i];
        }
        return name;
    }

    private Diagram initDiagram(DataCollection<?> input, DataCollection<?> output) throws Exception
    {
        Diagram slowModel = (Diagram)input.get( PopulationUtils.SLOW_MODEL );
        Diagram fastModel = (Diagram)input.get( PopulationUtils.FAST_MODEL );

        Diagram composite = PopulationUtils.initCompositeDiagram( slowModel, fastModel, output );

        //To simulate the treatment with diuretics
        Node node = new Node( composite, new Stub( null, Type.MATH_EQUATION, Type.MATH_EQUATION ) );
        Equation eq = new Equation( node, Equation.TYPE_SCALAR, "time_composite", "time" );
        node.setRole( eq );
        node.setShowTitle( false );
        composite.put( node );

        PortProperties properties = new PortProperties( composite, Stub.OutputConnectionPort.class );
        properties.setAccessType( ConnectionPort.PRIVATE );
        properties.setModuleName( PopulationUtils.FAST_MODEL + "_ss" );
        properties.createElements( composite, new Point(), null );

        CollectionFactoryUtils.save( composite );
        return composite;
    }

    private TableDataCollection truncPopulation(TableDataCollection population, Diagram composite)
    {
        TableDataCollection tPopulation = population.clone( population.getOrigin(), population.getName() + "_clone" );

        String[] unknownColumns = getUnknownColumns( tPopulation, composite );
        for( String column : unknownColumns )
        {
            int index = tPopulation.getColumnModel().getColumnIndex( column );
            log.info( MessageBundle.format( "INFO_UNKNOWN_COLUMN", column ) );
            tPopulation.getColumnModel().removeColumn( index );
        }

        for( String item : StreamEx.of( PopulationTreatmentParameters.ALL_DRUGS ).map( drug -> drug.getVariables() )
                .filter( array -> array.length == 1 ).map( array -> array[0] ).append( "time" ) )
        {
            if( tPopulation.getColumnModel().hasColumn( PopulationUtils.SLOW_MODEL + "_" + item ) )
            {
                int index = tPopulation.getColumnModel().getColumnIndex( PopulationUtils.SLOW_MODEL + "_" + item );
                tPopulation.getColumnModel().removeColumn( index );
            }
            if( tPopulation.getColumnModel().hasColumn( PopulationUtils.FAST_MODEL + "_" + item ) )
            {
                int index = tPopulation.getColumnModel().getColumnIndex( PopulationUtils.FAST_MODEL + "_" + item );
                tPopulation.getColumnModel().removeColumn( index );
            }
        }

        return tPopulation;
    }

    private void setDrugVariables(String[] drugVariables, Diagram composite)
    {
        for( int k = 0; k < drugVariables.length; ++k )
        {
            boolean drugFound = false;

            EModel slowEModel = (EModel) ( (SubDiagram)composite.get( PopulationUtils.SLOW_MODEL + "_ss" ) ).getDiagram().getRole();
            EModel fastEModel = (EModel) ( (SubDiagram)composite.get( PopulationUtils.FAST_MODEL + "_ss" ) ).getDiagram().getRole();

            if( slowEModel.containsVariable( drugVariables[k] ) )
            {
                slowEModel.getVariable( drugVariables[k] ).setInitialValue( 1 );
                drugFound = true;
            }
            if( fastEModel.containsVariable( drugVariables[k] ) )
            {
                fastEModel.getVariable( drugVariables[k] ).setInitialValue( 1 );
                drugFound = true;
            }
            if( !drugFound )
            {
                log.severe( MessageBundle.format( "SEVERE_DRUG_NOT_FOUND", drugVariables[k] ) );
            }
        }
    }

    private String getVariableName(String name)
    {
        if( name.startsWith( PopulationUtils.SLOW_MODEL ) )
            return PopulationUtils.SLOW_MODEL + "_ss" + "/" + name.substring( PopulationUtils.SLOW_MODEL.length() + 1 );
        else if( name.startsWith( PopulationUtils.FAST_MODEL ) )
            return PopulationUtils.FAST_MODEL + "_ss" + "/" + name.substring( PopulationUtils.FAST_MODEL.length() + 1 );
        return name;
    }

    private double[] getValues(RowDataElement row)
    {
        return StreamEx.of( row.getValues() ).mapToDouble( val -> (double)val ).toArray();
    }

    private boolean isVariableValid(Diagram composite, String name)
    {
        if( name.startsWith( PopulationUtils.SLOW_MODEL ) )
        {
            String var = name.substring( PopulationUtils.SLOW_MODEL.length() + 1 );
            return ( (EModel) ( (SubDiagram)composite.get( PopulationUtils.SLOW_MODEL + "_ss" ) ).getDiagram().getRole() )
                    .containsVariable( var );
        }
        else if( name.startsWith( PopulationUtils.FAST_MODEL ) )
        {
            String var = name.substring( PopulationUtils.FAST_MODEL.length() + 1 );
            return ( (EModel) ( (SubDiagram)composite.get( PopulationUtils.FAST_MODEL + "_ss" ) ).getDiagram().getRole() )
                    .containsVariable( var );
        }
        return false;
    }

    private String[] getUnknownColumns(TableDataCollection population, Diagram composite)
    {
        return population.getColumnModel().stream().filter( column -> !isVariableValid( composite, column.getName() ) )
                .map( column -> column.getName() ).toArray( String[]::new );
    }

    private SimulationTaskFactory initSimulationTaskFactory()
    {
        return new SimulationTaskFactory()
        {
            @Override
            public MultipleSimulationTask createSimulationTask(String[] names)
            {
                try
                {
                    Constructor<MultipleSimulationTask> constructor = MultipleSimulationTask.class
                            .getConstructor( ParallelSimulationEngine.class, String[].class );
                    return constructor.newInstance( parallelEngine, names );
                }
                catch( Exception ex )
                {
                    log.severe( MessageBundle.format( "SEVERE_SIMULATION_TASK_CREATION", ex.getMessage() ) );
                    return null;
                }
            }

            @Override
            public double[] processResult(Object result)
            {
                if( !jobControl.isStopped() )
                {
                    log.info( MessageBundle.format( "INFO_CURRENT_PATIENT", currDrug, ++currPatient, allPatients ) );

                    int all = allPatients * parameters.getDrugs().length;
                    identifier++;
                    jobControl.setPreparedness( (int) ( ( (long)identifier * 100 ) / all ) );
                }

                SimulationResult sr = (SimulationResult)result;
                if( sr != null && !jobControl.isStopped() )
                {
                    int last = sr.getValues().length;
                    return sr.getValues()[last - 1];
                }

                double[] values = new double[numOfVariables];
                for( int i = 0; i < numOfVariables; ++i )
                    values[i] = Double.NaN;
                return values;
            }
        };
    }

    private SimulationTaskParameters initSimulationParameters(Diagram composite)
    {
        SimulationTaskParameters stp = new SimulationTaskParameters();
        stp.setDiagram( composite );
        SimulationEngine engine = DiagramUtility.getPreferredEngine( composite );
        engine.setDiagram( composite );
        engine.setCompletionTime( getParameters().getTreatmentTime() );

        for( AgentSimulationEngineWrapper innerEngine : ( (AgentModelSimulationEngine)engine ).getEngines() )
        {
            if( innerEngine.getSubDiagramPath().equals( PopulationUtils.FAST_MODEL + "_ss" ) )
            {
                JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();
                solver.getOptions().setDetectIncorrectNumbers(true);
                solver.getOptions().setAtol( FAST_MODEL_ATOL );
                solver.getOptions().setRtol( FAST_MODEL_RTOL );
            }
            if( innerEngine.getSubDiagramPath().equals( PopulationUtils.SLOW_MODEL + "_ss" ) )
            {
                JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();
                solver.getOptions().setDetectIncorrectNumbers(true);
                solver.getOptions().setAtol( SLOW_MODEL_ATOL );
                solver.getOptions().setRtol( SLOW_MODEL_RTOL );
            }
        }

        stp.setSimulationEngine( engine );
        return stp;
    }

    private double[] getArray(TableDataCollection tbl, String columnName)
    {   
        double[] arr = new double[tbl.getSize()];
        int i = 0;

        if( !tbl.getColumnModel().hasColumn( columnName ) )
            return null;
        
        Iterator<RowDataElement> it = tbl.iterator();
        while( it.hasNext() )
        {
            RowDataElement row = it.next();
            arr[i] = (double) row.getValue(columnName);
            i++;
        }
        return arr;
    }

    private double[] getResponse(TableDataCollection baseline, TableDataCollection treated, String param, int sign)
    {
        double[] response = new double[baseline.getSize()];
        for( int j = 0; j < response.length; ++j )
            response[j] = ( (double)treated.getAt( j ).getValue( param ) - (double)baseline.getAt( j ).getValue( param ) ) * sign;
        return response;
    }

    private double getMean(double[] arr)
    {
        double sum = 0;
        for(int i = 0; i < arr.length; ++i)
            sum = sum + arr[i];
        return sum / arr.length;
    }

    private double getSD(double[] arr)
    {
        double sd = 0;
        double meanValue = getMean(arr);
        for (int i = 0; i < arr.length; ++i)
            sd += java.lang.Math.pow(arr[i] - meanValue, 2);
        return java.lang.Math.sqrt(sd / (arr.length - 1));
    }

    private double getMin(double[] arr)
    {
        return Arrays.stream(arr).min().getAsDouble();
    }

    private double getMax(double[] arr)
    {
        return Arrays.stream(arr).max().getAsDouble();
    }

    private String getStatistics(double[] arr, String pattern)
    {
        if( arr == null )
            return "";

        for( int i = 0; i < arr.length; ++i )
            if( arr[i] != arr[0] )
                return format( getMean( arr ), pattern ) + " ± " + format( getSD( arr ), pattern ) + " (" + format( getMin( arr ), pattern )
                        + " – " + format( getMax( arr ), pattern ) + ")";

        return Double.toString( arr[0] );
    }

    public String getStatistics(TableDataCollection tbl, String columnName, String pattern)
    {
        if( columnName.equals( "Sex" ) )
        {
            if( !tbl.getColumnModel().hasColumn( "Sex" ) )
                return "";

            int men = 0;
            int women = 0;

            Iterator<RowDataElement> it = tbl.iterator();
            while( it.hasNext() )
            {
                RowDataElement row = it.next();
                if( row.getValue( "Sex" ).equals( Gender.MAN.toString() ) )
                    men++;
                else
                    women++;
            }
            return men + "/" + women;
        }
        if( columnName.equals( "Race" ) )
        {
            if( !tbl.getColumnModel().hasColumn( "Race" ) )
                return "";

            int caucasoid = 0;
            int negroid = 0;

            Iterator<RowDataElement> it = tbl.iterator();
            while( it.hasNext() )
            {
                RowDataElement row = it.next();
                if( row.getValue( "Race" ).equals( Race.CAUCASOID.toString() ) )
                    caucasoid++;
                else
                    negroid++;
            }
            return caucasoid + "/" + negroid;
        }
        return getStatistics( getArray( tbl, columnName ), pattern );
    }

    private String format(double value, String pattern)
    {
        return new DecimalFormat( pattern ).format( value );
    }

    public String getMessage(String key)
    {
        return MessageBundle.getMessage( key );
    }

    private HtmlDataElement getReport(DataCollection<?> output, TableDataCollection population, TableDataCollection statistics, TableDataCollection statisticsForImage, TableDataCollection paramMapping) throws Exception
    {
        Properties p = new Properties();
        File template = TempFiles.file( "treatment_report.vm" );

        try (BufferedWriter bw = ApplicationUtils.utfWriter( template );
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader( getClass().getResourceAsStream( "treatment_report.vm" ), StandardCharsets.UTF_8 ) ))
        {
            bw.write( StreamEx.of( reader.lines() ).joining( "\n" ) );
        }

        p.setProperty( "file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader" );
        p.setProperty( "file.resource.loader.path", template.getParentFile().getAbsolutePath() );

        final VelocityEngine engine = new VelocityEngine( p );
        engine.init();

        Template velocityTemplate = engine.getTemplate( template.getName(), "UTF-8" );

        VelocityContext context = new VelocityContext();

        DataElementPath imgPath = DataElementPath.create( output, "Response.png" );
        ImageDataElement img = new ImageDataElement( "Response.png", output, getImageForReport( statisticsForImage ), "PNG" );
        imgPath.save( img );

        context.put( "analysis", this );
        context.put( "population", population );
        context.put( "statistics", statistics );
        context.put( "paramMapping", paramMapping );
        if( parameters.getGenerationInfo() != null && parameters.getGenerationInfo().getDataElement( AnalysisMethodElement.class )
                .getAnalysisMethod() instanceof PopulationGeneration )
            context.put( "generationInfo",
                    parameters.getGenerationInfo().getDataElement( AnalysisMethodElement.class ).getAnalysisMethod().getParameters() );
        else
            context.put( "generationInfo", null );
        context.put( "imagePath", "\"" + "Response.png" + "\"" );

        File htmlFile = TempFiles.file( "Result" );
        try (BufferedWriter bw = ApplicationUtils.utfWriter( htmlFile ))
        {
            velocityTemplate.merge( context, bw );
        }

        DataElementPath htmlPath = DataElementPath.create( output, "Report.html" );
        HtmlDataElement html = new HtmlDataElement( "Report.html", output, ApplicationUtils.readAsString( htmlFile ) );
        htmlPath.save( html );

        return html;
    }
    
    private BufferedImage getImageForReport(TableDataCollection stTable)
    {
        int width = IMG_BASE_WIDTH + IMG_BAR_WIDTH * stTable.getSize();

        BufferedImage imgSBP = generateImage( stTable, MessageBundle.getMessage( "SYSTOLIC_BLOOD_PRESSURE" ), "SBP", width, IMG_HEIGHT );

        BufferedImage imgDBP = generateImage( stTable, MessageBundle.getMessage( "DIASTOLIC_BLOOD_PRESSURE" ), "DBP", width, IMG_HEIGHT );

        BufferedImage img = new BufferedImage( width, IMG_HEIGHT * 2, BufferedImage.TYPE_INT_ARGB );
        img.getGraphics().drawImage( imgSBP, 0, 0, null );
        img.getGraphics().drawImage( imgDBP, 0, IMG_HEIGHT, null );

        return img;
    }

    private BufferedImage generateImage(TableDataCollection stTable, String title, String imgVar, int width, int height)
    {
        StatisticalBarChart bc = new StatisticalBarChart();
        bc.setTitle( title );
        bc.setYTitle( MessageBundle.getMessage( "MMHG" ) );
        bc.getRenderer().setItemMargin( 0.5 );
        bc.setLablePosition( CategoryLabelPositions.createUpRotationLabelPositions( Math.PI / 3 ) );
        bc.addBars( title, stTable, imgVar + "_mean", imgVar + "_sd" );
        return bc.getImage( width, height );
    }

    private void addStatRow(TableDataCollection stat, String rowName, TableDataCollection baseline, TableDataCollection treated, TableDataCollection paramMapping) throws Exception
    {
        String[] row = new String[4];

        String psName = (String) paramMapping.get("SBP").getValue("Designation");
        String param = PopulationUtils.FAST_MODEL + "_" + psName;
        row[0] = getStatistics( getArray( treated, param ), "#0.0" );
        row[1] = getStatistics( getResponse( baseline, treated, param, -1 ), "#0.0" );

        String pdName = (String) paramMapping.get("DBP").getValue("Designation");
        param = PopulationUtils.FAST_MODEL + "_" + pdName;
        row[2] = getStatistics( getArray( treated, param ), "#0.0" );
        row[3] = getStatistics( getResponse( baseline, treated, param, -1 ), "#0.0" );

        TableDataCollectionUtils.addRow( stat, rowName, row, true );
    }

    private void addStatForImageRow(TableDataCollection statForImage, String rowName, TableDataCollection population, TableDataCollection paramMapping) throws Exception
    {
        Double[] row = new Double[4];

        String psName = (String) paramMapping.get("SBP").getValue("Designation");
        double[] arr = getArray( population, PopulationUtils.FAST_MODEL + "_" + psName );
        row[0] = getMean( arr );
        row[1] = getSD( arr );

        String pdName = (String) paramMapping.get("DBP").getValue("Designation");
        arr = getArray( population, PopulationUtils.FAST_MODEL + "_" + pdName );
        row[2] = getMean( arr );
        row[3] = getSD( arr );

        TableDataCollectionUtils.addRow( statForImage, rowName, row );
    }

    public static class MultipleSimulationTask extends SimulationTask
    {
        private static final int NUMBER_OF_ATTEMPTS = 20;

        public MultipleSimulationTask(ParallelSimulationEngine parallelEngine, String[] names)
        {
            super( parallelEngine, names );
        }

        @Override
        protected Object getResult(SimulationEngine engine, Model baseModel)
        {
            Object sr = super.getResult( engine, baseModel );
            if( sr == null && isStiffOrUnstable( engine ) )
            {
				int[] increments = new int[] { 6000, 3000, 1500 };
				for (int i = 0; i < increments.length; ++i)
            	{
            		engine.setTimeIncrement( increments[i] );
            		for( AgentSimulationEngineWrapper innerEngine : ( (AgentModelSimulationEngine)engine ).getEngines() )
                    {
                        if( innerEngine.getSubDiagramPath().equals( PopulationUtils.SLOW_MODEL + "_ss" ) )
                        	innerEngine.setTimeIncrement( increments[i]/60 );
                        if( innerEngine.getSubDiagramPath().equals( PopulationUtils.FAST_MODEL + "_ss" ) )
                        	innerEngine.setTimeIncrement( increments[i] );
                    }

                    int attemp = 1;
                    double currAtol = FAST_MODEL_ATOL;
                    double currRtol = FAST_MODEL_RTOL;
                    while( sr == null && attemp < NUMBER_OF_ATTEMPTS )
                    {
                        for( AgentSimulationEngineWrapper innerEngine : ( (AgentModelSimulationEngine)engine ).getEngines() )
                        {
                            if( innerEngine.getSubDiagramPath().equals( PopulationUtils.FAST_MODEL + "_ss" ) )
                            {
                                JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();
                                currAtol = solver.getOptions().getAtol() * 0.5;
                                currRtol = solver.getOptions().getRtol() * 0.5;
                                solver.getOptions().setAtol( currAtol );
                                solver.getOptions().setRtol( currRtol );
                            }
                            if( innerEngine.getSubDiagramPath().equals( PopulationUtils.SLOW_MODEL + "_ss" ) )
                            {
                                JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();
                                solver.getOptions().setAtol( currAtol );
                                solver.getOptions().setRtol( currRtol );
                            }
                        }
                        try
                        {
                            setValues( engine, baseModel, values, names );
                        }
                        catch( Exception e )
                        {
                            e.printStackTrace();
                        }
                        sr = super.getResult( engine, baseModel );
                        attemp++;
                    }
            	}
            	engine.setTimeIncrement( 6000 );
                for( AgentSimulationEngineWrapper innerEngine : ( (AgentModelSimulationEngine)engine ).getEngines() )
                {
                    if( innerEngine.getSubDiagramPath().equals( PopulationUtils.FAST_MODEL + "_ss" ) )
                    {
                        JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();
                        solver.getOptions().setAtol( FAST_MODEL_ATOL );
                        solver.getOptions().setRtol( FAST_MODEL_RTOL );
                        innerEngine.setTimeIncrement( 6000 );
                    }
                    if( innerEngine.getSubDiagramPath().equals( PopulationUtils.SLOW_MODEL + "_ss" ) )
                    {
                        JVodeSolver solver = (JVodeSolver)innerEngine.getSolver();
                        solver.getOptions().setAtol( SLOW_MODEL_ATOL );
                        solver.getOptions().setRtol( SLOW_MODEL_RTOL );
                        innerEngine.setTimeIncrement( 100 );
                    }
                }
            }
            return sr;
        }

        private boolean isStiffOrUnstable(SimulationEngine engine)
        {
            SimulatorProfile profile = ( (Simulator)engine.getSolver() ).getProfile();
            if( profile.isStiff() || profile.isUnstable() )
                return true;
            for( AgentSimulationEngineWrapper innerEngine : ( (AgentModelSimulationEngine)engine ).getEngines() )
            {
                profile = ( (Simulator)innerEngine.getSolver() ).getProfile();
                if( profile.isStiff() || profile.isUnstable() )
                    return true;
            }
            return false;
        }

        @Override
        public void run(FunctionJobControl jobControl) throws Exception
        {
            jobControl.begin();
            super.run( jobControl );
            jobControl.end();
        }
    }
}
