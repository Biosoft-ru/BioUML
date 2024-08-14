package biouml.plugins.sbgn;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.DiagramImporter;
import biouml.model.DiagramType;
import biouml.model.util.DiagramDMLImporter;
import biouml.model.util.DiagramXmlReader;
import biouml.plugins.sbml.MessageBundle;
import biouml.plugins.sbml.SbmlConstants;
import biouml.plugins.sbml.SbmlImporter;
import biouml.plugins.sbml.SbmlModelReader_31;
import biouml.plugins.sbml.composite.SbmlCompositeReader;
import biouml.standard.diagram.CompositeDiagramType;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.ImportProperties;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.XmlUtil;
import ru.biosoft.util.archive.ArchiveEntry;
import ru.biosoft.util.archive.ArchiveFactory;
import ru.biosoft.util.archive.ArchiveFile;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SbexImporter implements DataElementImporter
{
    private static final String EXTENSION = ".omex";

    protected static final Logger log = Logger.getLogger( SbexImporter.class.getName() );

    private static final String MY_FORMAT = "Sbex-archive (*.omex)";

    private static final String TYPE_SBML = "sbml";
    private static final String TYPE_DML = "dml";
    private static final String TYPE_TABLE = "table";
    private static final String TYPE_OPTIMIZATION = "optimization";

    private List<ImportedFile> files;

    private SbexImportProperties properties;

    @Override
    public int accept(DataCollection parent, File file)
    {
        if( parent == null || !DataCollectionUtils.isAcceptable( parent, getResultType() ) || !parent.isMutable() )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;
        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile( file );
        if( archiveFile != null )
        {
            archiveFile.close();
            if( file.getName().endsWith( EXTENSION ) )
                return ACCEPT_HIGHEST_PRIORITY;
            return ACCEPT_MEDIUM_PRIORITY;
        }
        return ACCEPT_UNSUPPORTED;
    }

    private String detectType(File file)
    {
        String name = file.getName();
        String ext = name.substring( name.lastIndexOf( "." ) + 1 );
        switch( ext )
        {
            case "txt":
                return TYPE_TABLE;
            case "dml":
            {
                if( new DiagramDMLImporter().accept( file ) != 0 )
                    return TYPE_DML;
                else
                    return TYPE_OPTIMIZATION;
            }
            case "xml":
            case "sbml":
                return TYPE_SBML;
            default:
                return TYPE_DML;
        }
    }

    private DataElementImporter getImporter(String type)
    {
        switch( type )
        {
            case TYPE_TABLE:
            {
                TableCSVImporter importer = new TableCSVImporter();
                return importer;
            }
            case TYPE_DML:
                return new DiagramDMLImporter();
            case TYPE_OPTIMIZATION:
                return new OptimizationImporter();
            case TYPE_SBML:
                return new SbmlImporter();
            default:
                return new DiagramDMLImporter();
        }
    }

    private Set<String> getDependencies(File file, String fileType) throws Exception
    {
        Set<String> result = new HashSet<>();
        if( fileType.equals( TYPE_TABLE ) )
        {
            //tables do not depend on anything
        }
        else if( fileType.equals( TYPE_DML ) )
        {
            Element diagramElement = readXmlDiagramElement( file );
            if( diagramElement == null )
                throw new Exception( "Diagram element not found while reading diagram " + file );

            DiagramType type = DiagramXmlReader.readDiagramType( diagramElement, null, null );
            if( type instanceof CompositeDiagramType )
            {
                Element nodesElement = DiagramXmlReader.getElement( diagramElement, DiagramXmlReader.NODES_ELEMENT );
                NodeList list = nodesElement.getChildNodes();
                XmlUtil.elements( list, DiagramXmlReader.SUBDIAGRAM_ELEMENT )
                        .forEach( e -> result.add( e.getAttribute( DiagramXmlReader.DIAGRAM_REF_ATTR ) ) );
            }

            result.addAll( getExperiments( diagramElement ) );

        }
        else if( fileType.equals( TYPE_SBML ) )
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse( file );

            Element root = document.getDocumentElement();
            String level = root.getAttribute( SbmlConstants.SBML_LEVEL_ATTR );

            boolean isComposite = false;
            if( level.equals( "3" ) )
            {
                List<String> packages = SbmlModelReader_31.readPackages( document );
                for( String s : packages )
                {
                    if( s.equals( "comp" ) )
                    {
                        isComposite = true;
                        break;
                    }
                }
            }

            if( isComposite )
            {
                Set<String> submodels = SbmlCompositeReader.getSubModelList( root );
                result.addAll( submodels );
            }
        }
        else if( fileType.equals( TYPE_OPTIMIZATION ) )
        {
            Document document = DiagramXmlReader.createDocument( file.getName(), new FileInputStream( file ), null );
            Element root = document.getDocumentElement();

            for( Element element : getRecursiveElements( root, "property" ) )
            {
                System.out.println( element.getTagName() );
                if( diagramPathProperties.contains( element.getAttribute( "name" ) ) )
                {
                    String val = element.getAttribute( "value" );
                    if( !val.isEmpty() )
                        result.add( val );
                }
            }
        }
        return result;
    }

    private static Set<String> diagramPathProperties = new HashSet()
    {
        {
            add( "diagramPath" );
            add( "filePath" );
            add( "startingParameters" );
        }
    };

    public List<org.w3c.dom.Element> getRecursiveElements(org.w3c.dom.Element element, String tag)
    {
        List<org.w3c.dom.Element> result = new ArrayList<>();
        for( org.w3c.dom.Element innerElement : XmlUtil.elements( element ) )
        {
            if( innerElement.getTagName().equals( tag ) )
                result.add( innerElement );
            result.addAll( getRecursiveElements( innerElement, tag ) );
        }
        return result;
    }

    private Set<String> getExperiments(Element element)
    {
        Set<String> results = new HashSet<>();
        Element plotsElement = DiagramXmlReader.getElement( element, DiagramXmlReader.PLOTS_ELEMENT );
        if( plotsElement == null )
            return results;
        for( Element plotElement : XmlUtil.elements( plotsElement, "plot" ) )
        {
            for( Element cElement : XmlUtil.elements( plotElement, "experiment" ) )
            {
                results.add( cElement.getAttribute( "path" ) );
            }
        }
        return results;
    }
    
    private Set<String> getTableElements(Element element)
    {
        Set<String> results = new HashSet<>();
        Element plotsElement = DiagramXmlReader.getElement( element, DiagramXmlReader.PLOTS_ELEMENT );
        if( plotsElement == null )
            return results;
        for( Element plotElement : XmlUtil.elements( plotsElement, "plot" ) )
        {
            for( Element cElement : XmlUtil.elements( plotElement, "experiment" ) )
            {
                results.add( cElement.getAttribute( "path" ) );
            }
        }
        return results;
    }

    private Element readXmlDiagramElement(File file) throws Exception
    {
        Document document = DiagramXmlReader.createDocument( file.getName(), new FileInputStream( file ), null );
        Element root = document.getDocumentElement();
        return DiagramXmlReader.getElement( root, DiagramXmlReader.DIAGRAM_ELEMENT );
    }

    @Override
    public SbexImportProperties getProperties(DataCollection parent, File file, String elementName)
    {
        properties = new SbexImportProperties( DataElementPath.create( parent ) );
        return properties;
    }

    @PropertyName ( "Import properties" )
    @PropertyDescription ( "Import properties." )
    public static class SbexImportProperties extends Option
    {
        protected boolean oneFolder = true;
        private DataElementPath path = null;

        public SbexImportProperties(DataElementPath path)
        {
            this.path = path;
        }

        @PropertyName ( "Import to one folder" )
        @PropertyDescription ( "If true then everything in archive will be imported to one folder." )
        public boolean isOneFolder()
        {
            return oneFolder;
        }
        public void setOneFolder(boolean oneFolder)
        {
            this.oneFolder = oneFolder;
        }

        @PropertyName ( "Folder path" )
        @PropertyDescription ( "Folder path." )
        public DataElementPath getPath()
        {
            return path;
        }
        public void setPath(DataElementPath path)
        {
            this.path = path;
        }
    }

    public static class SbexImportPropertiesBeanInfo extends BeanInfoEx2<SbexImportProperties>
    {
        public SbexImportPropertiesBeanInfo()
        {
            super( SbexImportProperties.class, MessageBundle.class.getName() );
        }
        @Override
        public void initProperties() throws Exception
        {
            addReadOnly( "oneFolder" );
            add( "path" );
        }
    }

    @Override
    public DataElement doImport(DataCollection<?> parent, File file, String elementName, FunctionJobControl jobControl, Logger log)
            throws Exception
    {
        if( jobControl != null )
            jobControl.functionStarted();

        DataElementPath path = properties.getPath();
        DataCollection<?> collection = null;
        try
        {
            if( !path.exists() )
            {
                collection = DataCollectionUtils.createSubCollection( path );
                if( collection == null )
                    throw new Exception( "Unable to create " + path );
            }
            else
                collection = path.getDataCollection();

            files = prepareFiles( file );

            Map<String, String> newPaths = new HashMap<>();
            for( ImportedFile importedFile : files )
                newPaths.put( importedFile.shortName, DataElementPath.create( collection, importedFile.title ).toString() );

            files = generateOrder( files );

            log.info( "Files to import in order: " );
            for( ImportedFile f : files )
                log.info( f.shortName );

            //first import all tables
            for( ImportedFile importedFile : files )
            {
                DataElementImporter importer = this.getImporter( importedFile.type );

                if( importer instanceof DiagramImporter )
                    ( (DiagramImporter)importer ).setNewPaths( newPaths );
                else if( importer instanceof OptimizationImporter )
                    ( (OptimizationImporter)importer ).setNewPaths( newPaths );
                else if( importer instanceof TableCSVImporter )
                {
                    ImportProperties properties = (ImportProperties) ( (TableCSVImporter)importer ).getProperties( collection,
                            importedFile.f, importedFile.title );

                    properties.setHeaderRow( 1 );
                    properties.setDataRow( 2 );
                    properties.setColumnForID( TableCSVImporter.GENERATE_UNIQUE_ID );
                    for( String column : properties.getPossibleColumns() )
                    {
                        if( column.equals( "ID" ) )
                        {
                            properties.setColumnForID( elementName );
                            break;
                        }
                    }
                }
                log.info( "Importing file " + importedFile.shortName + "..." );
                importer.doImport( collection, importedFile.f, importedFile.title, jobControl, log );
            }
        }
        catch( Exception e )
        {
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError( e );
                return null;
            }
            throw e;
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness( 100 );
            jobControl.functionFinished();
        }
        return collection;
    }

    private List<ImportedFile> generateOrder(List<ImportedFile> files) throws Exception
    {
        List<ImportedFile> result = new ArrayList<>();
        Set<String> alreadyAdded = new HashSet<>();

        while( true )
        {
            boolean added = false;           
            for( ImportedFile file : files )
            {
                Set<String> dependencies = file.dependencies;
                if( alreadyAdded.containsAll( dependencies ) )
                {
                    result.add( file );
                    alreadyAdded.add( file.shortName );
                    added = true;                    
                }                
            }
            files.removeAll( result ); //do not add files second time
            
            if( files.isEmpty() )
                return result;
            
            if( !added ) //can not add - there are cycled dependencies
                throw new Exception( "There are cycled dependencies between files to import: " + StreamEx.of( files ).joining( "," ) );
        }
    }

    private List<ImportedFile> prepareFiles(File file) throws Exception
    {
        List<ImportedFile> result = new ArrayList<>();
        ArchiveFile archiveFile = ArchiveFactory.getArchiveFile( file );
        if( archiveFile == null )
            throw new Exception( "Specified file is not an archive" );
        ArchiveEntry entry;
        while( ( entry = archiveFile.getNextEntry() ) != null )
        {
            String[] pathFields = entry.getName().split( "[\\\\\\/]+" );
            String entryName = pathFields[pathFields.length - 1];
            if (entryName.toLowerCase().equals( "manifest.xml" ))
                continue;
            String entryNameNoExt = entryName.replaceFirst( "^(.+)\\..+", "$1" );
            InputStream is = entry.getInputStream();
            File tmpFile = TempFiles.file( "zipImport" + entryName, is );
            String type = detectType( tmpFile );
            Set<String> dependencies = getDependencies( tmpFile, type );
            result.add( new ImportedFile( tmpFile, entry.getName(), entryNameNoExt, type, dependencies ) );
        }
        return result;
    }

    private DataElementPath doImport(ImportedFile file, DataCollection collection) throws Exception
    {
        DataElementImporter importer = this.getImporter( file.type );
        DataElement de = importer.doImport( collection, file.f, file.shortName, null, log );
        return de.getCompletePath();
    }

    @Override
    public boolean init(Properties properties)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    private class ImportedFile
    {
        File f;
        String type;
        String shortName;
        String title;
        Set<String> dependencies;

        public ImportedFile(File file, String shortName, String title, String type, Set<String> dependencies)
        {
            this.f = file;
            this.shortName = shortName;
            this.title = title;
            this.type = type;
            this.dependencies = dependencies;
        }
    }
}
