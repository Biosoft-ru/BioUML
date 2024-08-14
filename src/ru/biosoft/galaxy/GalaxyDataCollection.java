package ru.biosoft.galaxy;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.LinkedVectorDataCollection;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.generic.GenericTitleIndex;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.galaxy.filters.DataTablesPool;
import ru.biosoft.util.XmlUtil;

/**
 * {@link ru.biosoft.access.core.DataCollection} of {@link AnalysisMethodInfo} info based on 'tool_conf.xml' file of GALAXY
 */
public class GalaxyDataCollection extends LinkedVectorDataCollection<DataElement>
{
    protected static final Logger log = Logger.getLogger(GalaxyDataCollection.class.getName());

    public static final String GALAXY_PATH_ATTR = "galaxy.path"; //path to galaxy-dist
    public static final String GALAXY_SCRIPT_ATTR = "galaxy.script";//path to systems scripts to work with user accounts
    private static final String GALAXY_TOOL_DEPENDECY_DIR = "galaxy.tools";

    /**
     * If true, then analysis files will be preserved after analysis finished incorrectly.
     * Server administrator will have to remove them manually
     */
    public static final String GALAXY_PRESERVE_FILES_ON_ERROR_ATTR = "galaxy.preserveOnError";
    public static final String GALAXY_LAUNCHER_ATTR = "galaxy.launcher";


    public static final String SECTION_ELEMENT = "section";
    public static final String TOOL_ELEMENT = "tool";
    public static final String LABEL_ELEMENT = "label";

    public static final String ID_ATTR = "id";
    public static final String TEXT_ATTR = "text";
    public static final String NAME_ATTR = "name";
    public static final String FILE_ATTR = "file";
    private static final String TOOL_PATH_ATTR = "tool_path";

    static GalaxyDistFiles galaxyDistFiles;
    static File galaxySystemScriptsFile;
    static File galaxyToolDependencyDir;
    static boolean preserveOnError;
    static String launcherName;

    protected List<String> initMessages;

    protected DocumentBuilder builder;

    public GalaxyDataCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        String galaxyPath = properties.getProperty(GALAXY_PATH_ATTR);
        if( galaxyPath != null )
        {
            galaxyDistFiles = new GalaxyDistFiles( new File( galaxyPath ) );
        }
        BiosoftSecurityManager.addAllowedReadPath(galaxyPath);

        String galaxyScripts = properties.getProperty(GALAXY_SCRIPT_ATTR);
        if( galaxyScripts != null )
        {
            galaxySystemScriptsFile = new File(galaxyScripts);
        }

        String galaxyToolDependencyPath = properties.getProperty( GALAXY_TOOL_DEPENDECY_DIR );
        if(galaxyToolDependencyPath != null)
        {
            galaxyToolDependencyDir = new File( galaxyToolDependencyPath );
            BiosoftSecurityManager.addAllowedReadPath( galaxyToolDependencyPath );
        }


        preserveOnError = Boolean.valueOf(properties.getProperty(GALAXY_PRESERVE_FILES_ON_ERROR_ATTR));
        launcherName = properties.getProperty(GALAXY_LAUNCHER_ATTR);
        reinit();

        //set title index support
        getInfo().getProperties().setProperty(QuerySystem.INDEX_LIST, "title");
        getInfo().getProperties().setProperty("index.title", GenericTitleIndex.class.getName());
        DefaultQuerySystem qs = new DefaultQuerySystem(this);
        getInfo().setQuerySystem(qs);
        GalaxyFactory.initConstants();
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    /**
     * Get galaxy root folder. Not null after GalaxyDataCollection initialization.
     */
    public static GalaxyDistFiles getGalaxyDistFiles()
    {
        return galaxyDistFiles;
    }

    /**
     * Get path to bash scripts folder with user control scripts
     */
    public static File getSystemScriptsPath()
    {
        return galaxySystemScriptsFile;
    }

    public static File getGalaxyToolDependencyDir()
    {
        return galaxyToolDependencyDir;
    }

    public static boolean isPreserveOnError()
    {
        return preserveOnError;
    }

    public static String getLauncherName()
    {
        return launcherName;
    }

    protected void addInitError(String message)
    {
        log.log(Level.SEVERE, "Galaxy initialization: "+message);
        initMessages.add(message);
    }

    public void reinit()
    {
        clear();
        initMessages = new ArrayList<>();
        if( galaxyDistFiles == null )
        {
            addInitError("Galaxy folder not found");
            return;
        }

        DataTablesPool.reInit();

        int confFileCount = 0;
        for( File file : galaxyDistFiles.getToolConfFiles() )
        {
            try
            {
                parseToolConf( file );
                confFileCount++;
            }
            catch( Exception e )
            {
                addInitError( "Cannot parse tool configuration file " + file.getAbsolutePath() + ": " + e.getMessage() );
            }
        }
        if(confFileCount == 0)
            addInitError("No valid tool configuration files found");
    }

    public Collection<String> getInitMessages()
    {
        return Collections.unmodifiableCollection(initMessages);
    }

    protected void parseToolConf(File file) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        builder.setErrorHandler( new ErrorHandler()
        {
            @Override
            public void warning(SAXParseException e) throws SAXException
            {
                String message = "Can not parse: ";
                if(e.getSystemId() != null)
                  message += " URI=" + e.getSystemId();
                if(e.getLineNumber() != -1)
                  message += " Line=" + e.getLineNumber();
                if(e.getColumnNumber() != -1)
                    message += " Column=" + e.getColumnNumber();
                message += ": " + e.getMessage();
                addInitError( message );
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException
            {
                throw e;
            }

            @Override
            public void error(SAXParseException e) throws SAXException
            {
                warning(e);
            }
        } );
        Document doc = builder.parse(file);

        Element root = doc.getDocumentElement();

        setNotificationEnabled(false);
        parseToolbox(root);
        setNotificationEnabled(true);
    }

    protected void parseToolbox(Element toolbox) throws Exception
    {
        File toolPath = getGalaxyDistFiles().getToolsFolder();
        if( toolbox.hasAttribute( TOOL_PATH_ATTR ) )
            toolPath = new File( getGalaxyDistFiles().getRootFolder(), toolbox.getAttribute( TOOL_PATH_ATTR ) );
        for(Element element : XmlUtil.elements(toolbox, SECTION_ELEMENT))
        {
            parseSection(element, toolPath);
        }
        parseTools(toolbox, toolPath, this);
    }

    protected void parseSection(Element section, File toolPath) throws Exception
    {
        String name = section.getAttribute(ID_ATTR);
        String title = section.getAttribute(NAME_ATTR);

        DataCollection<DataElement> sectionDC = (DataCollection<DataElement>)get(name);

        if(sectionDC == null)
        {
            Properties prop = new Properties();
            prop.put( DataCollectionConfigConstants.NAME_PROPERTY, name );
            prop.put( ru.biosoft.access.core.DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY, title );
            sectionDC = new LinkedVectorDataCollection<DataElement>( this, prop )
            {
                @Override
                protected boolean sortNameList(List<String> list)
                {
                    return false;
                }
            };
            sectionDC.getInfo().getProperties().setProperty( QuerySystem.INDEX_LIST, "title" );
            sectionDC.getInfo().getProperties().setProperty( "index.title", GenericTitleIndex.class.getName() );
            DefaultQuerySystem qs = new DefaultQuerySystem( sectionDC );
            sectionDC.getInfo().setQuerySystem( qs );
            doPut( sectionDC, true );
        }

        parseTools(section, toolPath, sectionDC);
    }

    protected void parseTools(Element parentElement, File toolPath, DataCollection<DataElement> parentDC) throws Exception
    {
        for(Element element : XmlUtil.elements(parentElement))
        {
            if( element.getTagName().equals(TOOL_ELEMENT))
            {
                parseTool(element, toolPath, parentDC);
            } else if(element.getTagName().equals(LABEL_ELEMENT))
            {
                parseLabel(element, parentDC);
            }
        }

    }

    protected void parseLabel(Element element, DataCollection<DataElement> sectionDC) throws Exception
    {
        String id = element.getAttribute(ID_ATTR);
        String text = element.getAttribute(TEXT_ATTR);
        sectionDC.put(new GalaxyLabel(id, text, sectionDC));
    }

    protected void parseTool(Element tool, File toolPath, DataCollection<DataElement> parent)
    {
        String file = tool.getAttribute(FILE_ATTR);
        File toolXml = new File(toolPath, file);
        if( toolXml.exists() && AnalysesOverridesRegistry.getAnalysisOverrides(file).isVisible() )
        {
            GalaxyMethodInfo info = parseGalaxyMethod(toolXml, parent, file);
            if(info != null)
            {
                if(XmlUtil.getChildElement( tool, "tool_shed" ) != null)
                {
                    ToolShedElement toolShedElement = new ToolShedElement( tool );
                    info.setToolShedElement( toolShedElement );
                }
                
                AnalysisMethodRegistry.addMethod(info.getName(), info);
                for(String group : AnalysesOverridesRegistry.getAnalysisOverrides( file ).getGroups())
                    AnalysisMethodRegistry.addMethodToGroup( info.getName(), group, info );
            }
        }
    }

    protected GalaxyMethodInfo parseGalaxyMethod(File xmlFile, DataCollection<DataElement> parent, String id)
    {
        GalaxyMethodInfo info = null;
        try
        {
            String fileContent = FileUtils.readFileToString(xmlFile);
            fileContent = fileContent.replaceFirst("<[?]xml version=\"[^\"]*\"[?]>", "<?xml version=\"1.0\"?>");
            Document doc = builder.parse(new InputSource(new StringReader(fileContent)));
            Element tool = doc.getDocumentElement();

            info = MethodInfoParser.parseTool(tool, parent, xmlFile.getParentFile(), id);
            if( info != null )
            {
                parent.put(info);
            }
        }
        catch( SAXParseException t )
        {
            addInitError("Cannot parse: " + id + " (" + xmlFile.getAbsolutePath() + ", line#" + t.getLineNumber() + ", column#"
                    + t.getColumnNumber() + "): " + t.getMessage());
        }
        catch( Throwable t )
        {
            addInitError("Cannot parse: " + id + " (" + xmlFile.getAbsolutePath() + "): " + t.getMessage());
        }
        return info;
    }

    @ClassIcon("resources/label.gif")
    public static class GalaxyLabel implements DataElement
    {
        private final String name;
        private final String text;
        private final DataCollection<?> origin;

        public GalaxyLabel(String name, String text, DataCollection<?> origin)
        {
            this.name = name;
            this.text = text+" ---";
            this.origin = origin;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            return origin;
        }

        public String getDisplayName()
        {
            return text;
        }
    }

    @Override
    protected boolean sortNameList(List<String> list)
    {
        return false;
    }
}
