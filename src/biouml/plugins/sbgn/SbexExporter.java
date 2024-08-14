package biouml.plugins.sbgn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;
import biouml.model.SubDiagram;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.util.DiagramDMLExporter;
import biouml.plugins.optimization.Optimization;
import biouml.plugins.optimization.OptimizationExperiment;
import biouml.plugins.sbml.SbmlExporter;
import biouml.plugins.sbml.SbmlExporter.SbmlExportProperties;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.FileExporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericZipExporter;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * Exports diagram into archive with all related data elements
 */
public class SbexExporter implements DataElementExporter
{
    protected static final Logger log = Logger.getLogger( SbexExporter.class.getName() );

    private SbexExporterProperties exporterProperties;

    @Override
    public int accept(DataElement de)
    {
        if( de instanceof Diagram )
            return DataElementExporter.ACCEPT_LOW_PRIORITY;
        else if (de instanceof Optimization)
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        else
            return DataElementExporter.ACCEPT_UNSUPPORTED;

    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement dataElement, @Nonnull File file) throws Exception
    {
        Map<String, String> replacements = new HashMap<>();

        Set<DataElement> des = getRelatedElements( dataElement );

        log.info( "Next elements will be exported:" );
        for( ru.biosoft.access.core.DataElement de : des )
        {
            log.info( de.getCompletePath().toString() );
            replacements.put( de.getCompletePath().toString(), de.getName() + "." + generateExtension( de ) );
        }

        try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( file ) ))
        {
            for( ru.biosoft.access.core.DataElement de : des )
            {            
                DataElementExporter exporter = getExporter( de );

                if( exporter instanceof DiagramExporter )
                {
                    ( (DiagramExporter)exporter ).setNewPaths( replacements );
                }

                if (de instanceof Optimization)
                {
                    de = ( (Optimization)de ).clone( de.getOrigin(), de.getName() );
                    ( (Optimization)de ).getParameters().getOptimizerParameters().setResultPath( DataElementPath.EMPTY_PATH );
                    ( (Optimization)de ).getParameters().getStateInfos().clear();
                    ( (OptimizationExporter)exporter ).setNewPaths( replacements );
                }
                String extension = generateExtension( de );

                File elementFile = null;
                try
                {
                    elementFile = TempFiles.file( "zipExport." + extension );
                    exporter.doExport( de, elementFile );
                }
                catch( Exception e )
                {
                    if( elementFile != null )
                        elementFile.delete();
                    continue;
                }

                if( elementFile.exists() )
                {
                    ZipEntry entry = new ZipEntry( de.getName() + "." + extension );
                    zip.putNextEntry( entry );
                    GenericZipExporter.copyStream( new FileInputStream( elementFile ), zip );
                    zip.closeEntry();
                    elementFile.delete();
                }
            }

            File manifest = generateManifest( des, dataElement );
            ZipEntry entry = new ZipEntry( "manifest.xml" );
            zip.putNextEntry( entry );
            GenericZipExporter.copyStream( new FileInputStream( manifest ), zip );
            zip.closeEntry();
            manifest.delete();
        }
        catch( Exception e )
        {
            file.delete();
            throw e;
        }
    }
    
    private DataElementExporter getExporter(DataElement de)
    {
        if( de instanceof TableDataCollection )
        {
            Properties properties = new Properties();
            properties.setProperty( DataElementExporterRegistry.SUFFIX, exporterProperties.getTableFormat() );
            TableElementExporter exporter = new TableElementExporter();
            exporter.init( properties );
            return exporter;
        }
        else if( de instanceof Diagram )
        {
            if( (isSBML( (Diagram)de ) && exporterProperties.getDiagramFormat().equals( "Auto" ) )
                    || exporterProperties.getDiagramFormat().equals( "SBML" ) )
            {
                SbmlExporter exporter = new SbmlExporter();
                SbmlExportProperties properties = (SbmlExportProperties)exporter.getProperties( de, null );
                properties.setSaveBioUMLAnnotation( exporterProperties.keepBioUMLAnnotation );
                return exporter;
            }
            else
                return new DiagramDMLExporter();
        }
        else if (de instanceof Optimization)
        {
            return new OptimizationExporter();
        }
        return new FileExporter();
    }
    
    private boolean isSBML(Diagram diagram)
    {
        return SbgnDiagramType.class.isAssignableFrom( diagram.getType().getClass());
    }

    /**
     * Generates file extension by data elements type
     */
    private String generateExtension(DataElement de)
    {
        if( de instanceof TableDataCollection )
        {
            return exporterProperties.getTableFormat();
        }
        else if( de instanceof Diagram )
        {
            if( (isSBML( (Diagram)de ) && exporterProperties.getDiagramFormat().equals( "Auto" ) )
                    || exporterProperties.getDiagramFormat().equals( "SBML" ) )            
                return "xml";
            else
                return "dml";
        } 
        return "dml";
    }

    private File generateManifest(Set<DataElement> des, DataElement main) throws Exception
    {
        File f = TempFiles.file( "manifest.xml" );

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" ); // set because default indent amount is zero

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element omex = document.createElement( "omexManifest" );
        omex.setAttribute( "xmlns", "http://identifiers.org/combine.specifications/omex-manifest" );

        Element content = document.createElement( "content" );
        content.setAttribute( "location", "." );
        content.setAttribute( "format", "http://identifiers.org/combine.specifications/omex" );
        omex.appendChild( content );

        for( DataElement de : des )
        {
            content = document.createElement( "content" );
            content.setAttribute( "location", de.getName() + "." + generateExtension( de ) );
            content.setAttribute( "format", getURL( de ) );
            if (de.equals( main ))
                content.setAttribute( "master", "true" );
            omex.appendChild( content );
        }

        document.appendChild( omex );
        DOMSource source = new DOMSource( document );

        try (OutputStream os = new FileOutputStream( f ))
        {
            StreamResult result = new StreamResult( os );
            transformer.transform( source, result );
        }

        return f;
    }

    private String getURL(DataElement de)
    {
        if( de instanceof Diagram )
        {
            if( exporterProperties.getDiagramFormat().equals( SbexExporterProperties.FORMAT_SBML) )
            {
                return "http://identifiers.org/combine.specifications/sbml";
            }
            else if( exporterProperties.getDiagramFormat().equals( SbexExporterProperties.FORMAT_AUTO ) )
            {
                if( SbgnDiagramType.class.isAssignableFrom( ( (Diagram)de ).getType().getClass() ) )
                    return "http://identifiers.org/combine.specifications/sbml";
                return "dml";
            }
            else
                return "dml";
        }
        return "";

    }

    private Set<String> getRelatedPaths(DataElement de)
    {
        Set<String> paths = new HashSet<>();

        if( de instanceof Diagram )
        {
            Diagram diagram = (Diagram)de;
            PlotsInfo infos = DiagramUtility.getPlotsInfo( diagram );
            if( infos != null )
            {
                for( PlotInfo plot : infos.getPlots() )
                {
                    if( plot.getExperiments() != null )
                    {
                        for( Experiment experiment : plot.getExperiments() )
                            paths.add( experiment.getPath().toString() );
                    }
                }
            }

            for( SubDiagram s : Util.getSubDiagrams( diagram ) )
                paths.add( s.getDiagramPath() );

            for( SimpleTableElement ste : diagram.recursiveStream().map( n -> n.getRole() ).select( SimpleTableElement.class ) )
                paths.add( ste.getTablePath().toString() );
        }
        else if (de instanceof Optimization)
        {
            Optimization opt = (Optimization)de;
            for (OptimizationExperiment exp: opt.getParameters().getOptimizationExperiments())            
                paths.add( exp.getTableSupport().getFilePath().toString());
            paths.add( opt.getParameters().getOptimizerParameters().getStartingParameters().toString() );
            Diagram diagram = opt.getDiagram();
            paths.add( opt.getDiagram().getCompletePath().toString() );
            paths.addAll( getRelatedPaths( diagram ) );
            paths.add( opt.getOptimizationDiagram().getCompletePath().toString() );
        }
        return paths;
    }

    private Set<DataElement> getRelatedElements(DataElement de)
    {
        Set<DataElement> result = new HashSet<DataElement>();
        Set<String> paths = getRelatedPaths( de );
        result.add( de );
        for( String path : paths )
        {
            if( !path.isEmpty() )
                result.add( DataElementPath.create( path ).getDataElement() );
        }
        return result;
    }

    @Override
    public Object getProperties(DataElement de, File file)
    {
        exporterProperties = new SbexExporterProperties();
        return exporterProperties;
    }

    public static class SbexExporterProperties extends Option
    {
        public static String FORMAT_SBML  = "SBML";
        public static String FORMAT_DML  = "DML";
        public static String FORMAT_AUTO  = "Auto";
        
        private String diagramFormat = FORMAT_AUTO;
        private boolean keepBioUMLAnnotation = true;
        private String tableFormat = "txt";
        private String archiveFormat = ".omex";

        @PropertyName ( "Archive format" )
        public String getArchiveFormat()
        {
            return archiveFormat;
        }
        public void setArchiveFormat(String archiveFormat)
        {
            this.archiveFormat = archiveFormat;
        }
        
        @PropertyName ( "Keep BioUML annotation" )
        public boolean isKeepBioUMLAnnotation()
        {
            return keepBioUMLAnnotation;
        }
        public void setKeepBioUMLAnnotation(boolean keepBioUMLAnnotation)
        {
            this.keepBioUMLAnnotation = keepBioUMLAnnotation;
        }

        @PropertyName ( "Table format" )
        public String getTableFormat()
        {
            return tableFormat;
        }
        public void setTableFormat(String tableFormat)
        {
            this.tableFormat = tableFormat;
        }
        
        @PropertyName ( "Diagram format" )
        public String getDiagramFormat()
        {
            return diagramFormat;
        }
        public void setDiagramFormat(String diagramFormat)
        {
            this.diagramFormat = diagramFormat;
        }

        public Stream<String> getDiagramFormats()
        {
            return Stream.of( FORMAT_SBML, FORMAT_DML , FORMAT_AUTO);
        }
        
        public Stream<String> getTableFormats()
        {
            return Stream.of( "txt", "csv", "html" );
        }
        
        public Stream<String> getArchiveFormats()
        {
            return Stream.of( ".omex", ".sbex" );
        }
    }

    public static class SbexExporterPropertiesBeanInfo extends BeanInfoEx2<SbexExporterProperties>
    {
        public SbexExporterPropertiesBeanInfo()
        {
            super( SbexExporterProperties.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "keepBioUMLAnnotation" );
            addWithTags( "diagramFormat", bean -> bean.getDiagramFormats() );
            addWithTags( "tableFormat", bean -> bean.getTableFormats() );
//            addWithTags( "archiveFormat", bean -> bean.getArchiveFormats() );
        }
    }

    @Override
    public void doExport(DataElement de, File file, FunctionJobControl jobControl) throws Exception
    {
        this.doExport( de, file );
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( ru.biosoft.access.core.DataCollection.class );
    }
}