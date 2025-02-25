package biouml.plugins.physicell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorInfo;
import biouml.plugins.simulation.SimulatorProfile;
import biouml.plugins.simulation.Span;
import biouml.standard.simulation.ResultListener;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.VideoFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.GIFGenerator;
import ru.biosoft.physicell.ui.ResultGenerator;
import ru.biosoft.physicell.ui.Visualizer;
import ru.biosoft.physicell.ui.Visualizer2D;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TempFiles;

public class PhysicellSimulator implements Simulator
{
    private PhysicellModel model;
    private PhysicellOptions options = new PhysicellOptions();
    private boolean running = false;
    protected static final Logger log = Logger.getLogger( Simulator.class.getName() );
    private double nextReport = 0;
    private double nextImage = 0;
    private DataCollection<DataElement> resultFolder;
    private StringBuffer simulationLog;

    private Map<Visualizer, DataCollection<DataElement>> visualizerResult;
    private Map<Visualizer, DataCollection<DataElement>> visualizerImageResult;

    private String format;

    private VisualizerTextTable tableVisualizer;
    private VisualizerText textVisualizer;

    @Override
    public SimulatorInfo getInfo()
    {
        return null;
    }

    @Override
    public Object getDefaultOptions()
    {
        return new PhysicellOptions();
    }

    public void setResultFolder()
    {

    }

    public void addTableVisualizer(VisualizerTextTable tableVisualizer)
    {
        this.tableVisualizer = tableVisualizer;
    }
    
    public void addTextVisualizer(VisualizerText textVisualizer)
    {
        this.textVisualizer = textVisualizer;
    }

    @Override
    public void init(Model model, double[] x0, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        visualizerResult = new HashMap<>();
        visualizerImageResult = new HashMap<>();
        this.model = (PhysicellModel)model;
        this.options = (PhysicellOptions)getOptions();
        this.running = true;
        this.nextReport = 0;
        this.nextImage = 0;
        this.simulationLog = new StringBuffer();

        DataElementPath dep = options.getResultPath();
        if( dep == null )
            throw new IllegalArgumentException( "Please set output folder" );
        resultFolder = DataCollectionUtils.createSubCollection( options.getResultPath() );

        TextDataElement logElement = new TextDataElement( "model.txt", resultFolder );
        logElement.setContent( this.model.display() );
        resultFolder.put( logElement );

        if( options.isSaveReport() )
        {
            DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Cells" ) );
            DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Density" ) );
        }

        DataCollection<DataElement> videoCollection = null;
        DataCollection<DataElement> imagesCollection = null;

        if( options.isSaveVideo() || options.isSaveGIF() )
            videoCollection = DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Video" ) );
        if( options.isSaveImage() )
            imagesCollection = DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Image" ) );

        if( options.isSaveImageText() )
            textVisualizer.init(); //TODO: refactor and unify all visualizers
        if (options.isSaveImageTable())
            tableVisualizer.init();

        for( Visualizer v : this.model.getVisualizers() )
        {
            if( v instanceof Visualizer2D )
                ( (Visualizer2D)v ).setSaveImage( options.isSaveImage() );

            if( options.isSaveGIF() )
                v.addResultGenerator( new GIFGenerator( TempFiles.file( v.getName() + ".gif" ) ) );

            if( options.isSaveVideo() )
                v.addResultGenerator( new VideoGenerator( TempFiles.file( v.getName() + ".mp4" ) ) );
            String name = v.getName();

            visualizerResult.put( v, videoCollection );

            if( options.isSaveImage() )
                visualizerImageResult.put( v,
                        DataCollectionUtils.createSubCollection( imagesCollection.getCompletePath().getChildPath( name ) ) );
        }

        if( !this.model.isInit() )
            this.model.init();

        int nums = String.valueOf( Math.round( options.getFinalTime() ) ).length() + 1;
        format = "%0" + nums + "d";

        saveAllResults( this.model );
    }

    private static void uploadMP4(File f, DataCollection<DataElement> dc, String name) throws Exception
    {
        VideoFileImporter importer = new VideoFileImporter();
        importer.getProperties( dc, f, name ).setResolution( "1280 x 720 (High definition)" );
        importer.doImport( dc, f, name, null, log );
    }

    private static void uploadGeneric(File f, DataCollection<DataElement> dc, String name) throws Exception
    {
        FileImporter importer = new FileImporter();
        importer.doImport( dc, f, name, null, log );
    }

    @Override
    public void setInitialValues(double[] x0) throws Exception
    {
        // TODO Auto-generated method stub
    }


    @Override
    public boolean doStep() throws Exception
    {
        double curTime = model.getCurrentTime();
        while( curTime < options.getFinalTime() && running )
        {
            model.doStep();

            model.executeEvents();
            saveAllResults( model );
            curTime += options.getDiffusionDt();
        }
        return false;
    }

    private void saveAllResults(PhysicellModel model) throws Exception
    {
        double curTime = model.getCurrentTime();
        if( curTime >= nextReport )
        {
            nextReport += options.getReportInterval();
            saveResults( curTime );
            log.info( model.getLog() );
            simulationLog.append( "\n" + model.getLog() );

        }

        if( ( options.isSaveImageText() || options.isSaveImageTable() || options.isSaveImage() || options.isSaveVideo() || options.isSaveGIF() ) && curTime >= nextImage )
        {

            if( options.isSaveImageText() )
                textVisualizer.saveResult( model.getMicroenvironment(), curTime );
            if( options.isSaveImageTable() )
                tableVisualizer.saveResult( model.getMicroenvironment(), curTime );
            if( options.isSaveImage() || options.isSaveVideo() || options.isSaveGIF() )
                saveImages( curTime );

            nextImage += options.getImageInterval();
        }
    }

    private void saveImages(double curTime) throws Exception
    {
        String suffix = print( curTime, 0 );

        for( Visualizer vis : model.getVisualizers() )
            updateResult( vis, "Figure_" + suffix );

    }

    private void saveResults(double curTime) throws Exception
    {
        String suffix;
        if( options.getReportInterval() >= 1 )
        {
            int t = (int)Math.round( curTime );
            suffix = String.format( format, t );
        }
        else
        {
            suffix = Double.toString( Math.round( curTime * 100 ) / 100 );
        }

        DataCollection<DataElement> densityCollection = (DataCollection)resultFolder.get( "Density" );
        if( this.options.isSaveReport() )
        {
            TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( densityCollection, "Density_" + suffix );
            writeDensity( tdc, model.getMicroenvironment() );
            densityCollection.put( tdc );

            DataCollection<DataElement> subDC = (DataCollection)resultFolder.get( "Cells" );
            TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( subDC, "Report_" + suffix );
            Microenvironment m = model.getMicroenvironment();
            for( String s : model.getReportHeader() )
                result.getColumnModel().addColumn( s, DataType.Float );
            for( Cell cell : m.getAgents( Cell.class ) )
                TableDataCollectionUtils.addRow( result, String.valueOf( cell.ID ), model.getReport( cell ) );
            subDC.put( result );
        }
    }

    public void updateResult(Visualizer visualizer, String name) throws Exception
    {
        BufferedImage image = visualizer.getImage( model.getMicroenvironment(), model.getCurrentTime() );
        if( options.isSaveImage() )
        {
            DataCollection<DataElement> dc = visualizerImageResult.get( visualizer );
            dc.put( new ImageDataElement( name, dc, image ) );
        }
        visualizer.update( image );
    }

    public void writeDensity(TableDataCollection tdc, Microenvironment m)
    {
        int dataEntries = m.mesh.voxels.length;
        tdc.getColumnModel().addColumn( "X", DataType.Float );
        tdc.getColumnModel().addColumn( "Y", DataType.Float );
        tdc.getColumnModel().addColumn( "Z", DataType.Float );
        for( String density : m.densityNames )
            tdc.getColumnModel().addColumn( density, DataType.Float );

        int size = 3 + m.densityNames.length;
        for( int i = 0; i < dataEntries; i++ )
        {
            Object[] row = new Object[size];
            row[0] = m.mesh.voxels[i].center[0];
            row[1] = m.mesh.voxels[i].center[1];
            row[2] = m.mesh.voxels[i].center[2];
            for( int j = 0; j < m.densityNames.length; j++ )
            {
                row[3 + j] = m.density[i][j];
            }
            TableDataCollectionUtils.addRow( tdc, String.valueOf( i ), row );
        }
    }

    public String print(double v, int accuracy)
    {
        double factor = Math.pow( 10, accuracy );
        Integer value = (int) ( Math.round( v * factor ) / factor );
        return String.format( format, value );
        //        return String.valueOf( );
    }

    @Override
    public void start(Model model, Span tspan, ResultListener[] listeners, FunctionJobControl jobControl) throws Exception
    {
        try
        {
            init( model, null, tspan, listeners, jobControl );

            while( this.model.getCurrentTime() < options.getFinalTime() && running )
                doStep();

            finish();
        }
        catch( Exception ex )
        {
            log.info( "Simulation failed: " + ex.getMessage() );
            ex.printStackTrace();
        }
    }

    public void finish() throws Exception
    {
        for( Visualizer vis : this.model.getVisualizers() )
        {
            vis.finish();
            DataCollection<DataElement> dc = this.visualizerResult.get( vis );
            for( ResultGenerator generator : vis.getGenerators() )
            {
                File f = generator.getResult();
                String name = f.getName();
                String ext = name.substring( name.lastIndexOf( "." ) + 1 );
                String elementName = vis.getName() + "." + ext;
                if( ext.equals( "mp4" ) )
                    uploadMP4( f, dc, elementName );
                else
                    uploadGeneric( f, dc, elementName );
            }
        }

        TextDataElement logElement = new TextDataElement( "log.txt", resultFolder );
        logElement.setContent( simulationLog.toString() );
        resultFolder.put( logElement );
    }

    @Override
    public void stop()
    {
        running = false;
    }

    @Override
    public SimulatorProfile getProfile()
    {
        return null;
    }

    @Override
    public PhysicellOptions getOptions()
    {
        return options;
    }

    @Override
    public void setOptions(Options options)
    {
        this.options = (PhysicellOptions)options;
    }

    @Override
    public void setLogLevel(Level level)
    {
        // TODO Auto-generated method stub
    }


}