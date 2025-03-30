package biouml.plugins.physicell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import biouml.plugins.simulation.Simulator;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.VideoFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Cell;
import ru.biosoft.physicell.ui.ResultGenerator;
import ru.biosoft.physicell.ui.Visualizer;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class PhysicellResultWriter
{
    private DataCollection<DataElement> resultFolder;
    private PhysicellOptions options;
    private String format;
    private Map<Visualizer, DataCollection<DataElement>> visualizerResult;
    private Map<Visualizer, DataCollection<DataElement>> visualizerImageResult;
    private VisualizerTextTable tableVisualizer;
    private VisualizerText textVisualizer;
    private double nextReport = 0;
    private double nextImage = 0;
    private StringBuffer simulationLog;
    private PhysicellModel model;
    protected static final Logger log = Logger.getLogger( Simulator.class.getName() );

    public void init(PhysicellModel model, PhysicellOptions options) throws Exception
    {
        visualizerResult = new HashMap<>();
        visualizerImageResult = new HashMap<>();
        this.model = model;
        this.options = options;
        this.nextReport = 0;
        this.nextImage = 0;
        this.simulationLog = new StringBuffer();

        DataElementPath dep = options.getResultPath();
        if( dep == null )
            throw new IllegalArgumentException( "Please set output folder" );
        resultFolder = DataCollectionUtils.createSubCollection( options.getResultPath() );

        TextDataElement logElement = new TextDataElement( "model.txt", resultFolder );
        logElement.setContent( model.display() );
        resultFolder.put( logElement );

        if( options.isSaveReport() )
            DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Reports" ) );
        if( options.isSaveDensity() )
            DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Density" ) );

        DataCollection<DataElement> videoCollection = null;
        DataCollection<DataElement> imagesCollection = null;

        if( options.isSaveVideo() || options.isSaveGIF() )
            videoCollection = DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Video" ) );
        if( options.isSaveImage() )
            imagesCollection = DataCollectionUtils.createSubCollection( resultFolder.getCompletePath().getChildPath( "Image" ) );

        if( options.isSaveCellsText() )
            textVisualizer.init(); //TODO: refactor and unify all visualizers
        if( options.isSaveCellsTable() )
            tableVisualizer.init();

        for( Visualizer v : model.getVisualizers() )
        {
            visualizerResult.put( v, videoCollection );

            if( options.isSaveImage() )
                visualizerImageResult.put( v,
                        DataCollectionUtils.createSubCollection( imagesCollection.getCompletePath().getChildPath( v.getName() ) ) );
        }

        int nums = String.valueOf( Math.round( options.getFinalTime() ) ).length() + 1;
        format = "%0" + nums + "d";

        saveAllResults( model );
        writeInfo( resultFolder, model.getMicroenvironment() );
    }

    public void saveAllResults(PhysicellModel model) throws Exception
    {
        double curTime = model.getCurrentTime();
        if( curTime >= nextReport )
        {
            nextReport += options.getReportInterval();
            saveResults( curTime );
            log.info( model.getLog() );
            simulationLog.append( "\n" + model.getLog() );

        }

        if( ( options.isSaveCellsText() || options.isSaveCellsTable() || options.isSaveImage() || options.isSaveVideo()
                || options.isSaveGIF() ) && curTime >= nextImage )
        {

            if( options.isSaveCellsText() )
                textVisualizer.saveResult( model.getMicroenvironment(), curTime );
            if( options.isSaveCellsTable() )
                tableVisualizer.saveResult( model.getMicroenvironment(), curTime );
            if( options.isSaveImage() || options.isSaveVideo() || options.isSaveGIF() )
                saveImages( curTime );

            nextImage += options.getImageInterval();
        }
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
        if( this.options.isSaveDensity() )
        {
            TableDataCollection tdc = TableDataCollectionUtils.createTableDataCollection( densityCollection, "Density_" + suffix );
            writeDensity( tdc, model.getMicroenvironment() );
            densityCollection.put( tdc );
        }
        if( this.options.isSaveReport() )
        {
            DataCollection<DataElement> subDC = (DataCollection)resultFolder.get( "Reports" );
            TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( subDC, "Report_" + suffix );
            Microenvironment m = model.getMicroenvironment();
            for( String s : model.getReportHeader() )
                result.getColumnModel().addColumn( s, DataType.Float );
            for( Cell cell : m.getAgents( Cell.class ) )
                TableDataCollectionUtils.addRow( result, String.valueOf( cell.ID ), model.getReport( cell ) );
            subDC.put( result );
        }
    }

    public void writeInfo(DataCollection dc, Microenvironment m)
    {
        TextDataElement tde = new TextDataElement( "info.txt", dc );
        StringBuffer buffer = new StringBuffer();
        buffer.append( "X:\t" + m.mesh.boundingBox[0] + "\t" + m.mesh.boundingBox[3] + "\t" + m.mesh.dx + "\n" );
        buffer.append( "Y:\t" + m.mesh.boundingBox[1] + "\t" + m.mesh.boundingBox[4] + "\t" + m.mesh.dy + "\n" );
        buffer.append( "Z:\t" + m.mesh.boundingBox[2] + "\t" + m.mesh.boundingBox[5] + "\t" + m.mesh.dz + "\n" );
        buffer.append( "2D:\t" + m.options.simulate2D + "\n" );
        buffer.append( "Substrates:\t" + StreamEx.of( m.densityNames ).joining( "\t" ) + "\n" );
        tde.setContent( buffer.toString() );
        dc.put( tde );
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

    private void saveImages(double curTime) throws Exception
    {
        String suffix = print( curTime, 0 );

        for( Visualizer vis : model.getVisualizers() )
            updateResult( vis, "Figure_" + suffix );
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

    public String print(double v, int accuracy)
    {
        double factor = Math.pow( 10, accuracy );
        Integer value = (int) ( Math.round( v * factor ) / factor );
        return String.format( format, value );
        //        return String.valueOf( );
    }

    public void addTableVisualizer(VisualizerTextTable tableVisualizer)
    {
        this.tableVisualizer = tableVisualizer;
    }

    public void addTextVisualizer(VisualizerText textVisualizer)
    {
        this.textVisualizer = textVisualizer;
    }

    public static void uploadMP4(File f, DataCollection<DataElement> dc, String name) throws Exception
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

    public static void shift(DataCollection dc, int xShift, int yShift, int zShift) throws Exception
    {
        DataCollection cells = (DataCollection)dc.get( "Cells" );
        for( Object name : cells.getNameList() )
        {
            TextDataElement tde = (TextDataElement)cells.get( name.toString() );
            String s = tde.getContent();
            StringBuffer newString = new StringBuffer();
            String[] lines = s.split( "\n" );
            for( int i=1; i<lines.length; i++ )
            {
                String line = lines[i];
                String[] parts = line.split( "\t" );
                parts[0] = print( Double.parseDouble( parts[0] ) - xShift );
                parts[1] = print( Double.parseDouble( parts[1] ) - yShift );
                parts[2] = print( Double.parseDouble( parts[2] ) - zShift );
                newString.append( StreamEx.of( parts ).joining( "\t" ) );
                newString.append( "\n" );
            }
            tde.setContent( newString.toString() );
            tde.getOrigin().put( tde );
        }
        
        DataCollection density = (DataCollection)dc.get( "Density" );
        for( Object name : density.getNameList() )
        {
            TableDataCollection tdc = (TableDataCollection)density.get( name.toString() );
            for (String rowName: tdc.getNameList())
            {
                RowDataElement rde = tdc.get( rowName );
                Object[] values = rde.getValues();
                values[0] = print(Double.parseDouble(values[0].toString()) - xShift);
                values[1] = print(Double.parseDouble(values[1].toString()) - yShift);
                values[2] = print(Double.parseDouble(values[2].toString()) - zShift);
                rde.setValues( values );
            }
            tdc.getOrigin().put( tdc );
        }
    }
    
    private static String print(double val)
    {
        return String.valueOf( Math.round(val * 10)/10);
    }
}