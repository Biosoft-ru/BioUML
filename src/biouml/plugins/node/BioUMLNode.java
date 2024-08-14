package biouml.plugins.node;

import biouml.plugins.server.access.ClientDataCollection;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.StreamEx;
		
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;

import ru.biosoft.access.ImageElement;
import ru.biosoft.access.QuerySystemRegistry;
import ru.biosoft.access.AccessCoreInit;
import ru.biosoft.access.BiosoftIconManager;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionListenerRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.Environment;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.View.ModelResolver;
import ru.biosoft.graphics.access.DataElementModelResolver;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.plugins.javascript.JScriptShellEnvironment;
import ru.biosoft.plugins.javascript.JScriptContext;

import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.tomcat.TomcatConnection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.NetworkConfigurator;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.BiosoftClassLoading;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.UserPermissions;

import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.util.CharPredicate;
import io.github.spencerpark.jupyter.kernel.util.SimpleAutoCompleter;
import io.github.spencerpark.jupyter.kernel.util.StringSearch;

import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.channels.JupyterSocket;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;

import io.github.spencerpark.jupyter.kernel.magic.CellMagicParseContext;
import io.github.spencerpark.jupyter.kernel.magic.LineMagicParseContext;
import io.github.spencerpark.jupyter.kernel.magic.MagicParser;
import io.github.spencerpark.jupyter.kernel.magic.registry.Magics;
import io.github.spencerpark.jupyter.kernel.magic.common.DisplayMagics;
import io.github.spencerpark.jupyter.kernel.magic.common.Load;
import io.github.spencerpark.jupyter.kernel.magic.common.Shell;


/**
 * Launcher for BioUML node
 * Parameters:
 * 1. master server URL (like: http://localhost:8080/biouml)
 * 2. session ID
 *
 * STDIN: JavaScript to execute
 *
 * @todo processFile
 */
public class BioUMLNode extends BaseKernel implements IApplication
{
    private static Logger log = Logger.getLogger( BioUMLNode.class.getName() );

    private static final ModelResolver viewModelResolver = new DataElementModelResolver();
    /** Top level function that starts the shell. */

    private String serverPath = "";

    @Override
    public Object start(IApplicationContext arg0)
    {
        try
        {
            AccessCoreInit.init();
            QuerySystemRegistry.initQuerySystems();
            DataCollectionListenerRegistry.initDataCollectionListeners();
            
            configureJUL();
             
            if( new File( "/BioUML_Server/" ).exists() )
            {
                System.setProperty( "biouml.server.path", serverPath = "/BioUML_Server/" );
            }

            NetworkConfigurator.initNetworkConfiguration();
            loadPreferences();

            List<String> commandLineArgs = Collections.emptyList();
            Object arg = arg0.getArguments().get( "application.args" );
            if( arg instanceof String[] )
            {
                commandLineArgs = Arrays.asList( ( String[] )arg );
            }

            boolean isJupyter = commandLineArgs.contains( "-jupyter" );

            if( isJupyter )
            {
                /*
                java.util.Properties securityProperties = new java.util.Properties();
                try( java.io.FileReader reader = new java.io.FileReader( "/security.properties" ) )
                {
                    securityProperties.load( reader );
                }

                SecurityManager.initSecurityManager( securityProperties );

                String sessionId = null;
                */
                String host = null;

                final java.util.HashMap<String,String> loginData = new java.util.HashMap<>();

                String userLoginFile = "/home/jovyan/work/.user.txt";  

                try
                { 
                    try( java.util.stream.Stream<String> stream = Files.lines(Paths.get( userLoginFile ) ) ) 
                    {
                        stream.forEach( line -> {
                            String[] parts = line.split( "\t" );
                            loginData.put( parts[ 0 ], parts[ 1 ] );
                        } );
                    }

                    host = loginData.get( "url" ) + "/biouml/";
                    log.info( "BioUML server: " + host );
                }
                catch( Exception exc )
                {
                    log.log( Level.SEVERE, userLoginFile, exc );
                }

                java.nio.file.Path currentRelativePath = java.nio.file.Paths.get("");
                String s = currentRelativePath.toAbsolutePath().toString();

                log.info( "Working directory = '" + s + "'" );
                
                /*
                Permission permission = SecurityManager.commonLogin( 
                    loginData.get( "user" ), loginData.get( "pass" ), loginData.get( "url" ), null );
                sessionId = permission.getSessionId();
                log.info( "Session ID: " + sessionId );
                */  

                if( loginData.get( "url" ) != null && loginData.get( "user" ) != null && loginData.get( "pass" ) != null )
                {
                    System.setProperty(TomcatConnection.CONNECTION_TIMEOUT_PROPERTY, String.valueOf(86_400_000)); // One day
                    log.info( "Creating remote repository 'databases'..." );
                    createRepository("databases", host, loginData.get( "user" ), loginData.get( "pass" ) );

                    if( commandLineArgs.contains( "-local" ) )
                    {
                        log.info( "Creating local repository 'data'..." );
                        CollectionFactory.createRepository( serverPath + "resources" );
                    }
                    else
                    {
                        log.info( "Creating remote repository 'data'..." );
                        createRepository("data", host, loginData.get( "user" ), loginData.get( "pass" ) );
                    }

                    log.info( "Creating local repository '" + serverPath + "analyses'..." );
                    CollectionFactory.createRepository( serverPath + "analyses" );
                }
                else
                {
                    log.severe( "Unable to find BioUML server login data" );
                } 

                Plugins.getPlugins();
                CollectionFactoryUtils.init();
                View.setModelResolver( viewModelResolver );
                //NodeConfig.getInstance();

                int juInd = commandLineArgs.indexOf( "-jupyter" );
                Path connectionFile = Paths.get( commandLineArgs.get( juInd + 1 ) );

                if( !Files.isRegularFile(connectionFile) )
                { 
                    throw new IllegalArgumentException("Connection file '" + connectionFile + "' isn't a file.");
                }

                System.out.println( "---BioUML Jupyter notebook started---" );
                log.info( "---BioUML Jupyter notebook started---" );

                String contents = new String( Files.readAllBytes( connectionFile ) );

                JupyterSocket.JUPYTER_LOGGER.setLevel( Level.WARNING );

                KernelConnectionProperties connProps = KernelConnectionProperties.parse( contents );
                JupyterConnection connection = new JupyterConnection( connProps );

                initKernel();
                becomeHandlerForConnection( connection );

                connection.connect();
                connection.waitUntilClose();
            }
            else
            {
                System.setProperty( "biouml.node", "true" );

                String host = commandLineArgs.get( 0 );

                String user;
                String pass;
                if(commandLineArgs.size() == 1)
                {
                  //empty user and pass means demo account
                    user = "";
                    pass = "";
                }else if(commandLineArgs.size() == 3)
                {
                    user = commandLineArgs.get( 1 );
                    pass = commandLineArgs.get( 2 );
                }else if(commandLineArgs.size() == 2)
                {
                    //String sessionId = commandLineArgs.get( 1 );
                    //Not supported after migration to ClientDataCollection
                    throw new RuntimeException("expecting arguments: url [user pass]");
                }else
                    throw new RuntimeException("expecting arguments: url [user pass]");
                
                System.setProperty(TomcatConnection.CONNECTION_TIMEOUT_PROPERTY, String.valueOf(86_400_000)); // One day
                createRepository("databases", host, user, pass);
                createRepository("data", host, user, pass);
                CollectionFactory.createRepository( "analyses" );
                Plugins.getPlugins();
                CollectionFactoryUtils.init();
                View.setModelResolver( viewModelResolver );
                //NodeConfig.getInstance();
                System.out.println("Node started");

                String script = ApplicationUtils.readAsString(System.in);
                ScriptTypeRegistry.execute("js", script, new JScriptShellEnvironment(System.out), false);
            }      
        }
        catch( Throwable t )
        {
            System.err.println(ExceptionRegistry.log(t));
        }
        return IApplication.EXIT_OK;
    }

    private void loadPreferences()
    {
        Preferences preferences = new Preferences();
        String prefFile = new File( serverPath + "preferences.xml").exists() ? serverPath + "preferences.xml" : null;
        prefFile = new File(serverPath + "conf/preferences.xml").exists() ? serverPath + "conf/preferences.xml" : prefFile;
        if( prefFile != null )
        {
            log.info( "Got preferences from " + prefFile );    
            preferences.load( prefFile );
            Application.setPreferences( preferences );
        } 
        else
        {
            log.warning( "Preferences file not found" );
        }   
    }

    /**
     * @todo change to ClinetDataCollection
     */
    private void createRepository(String name, String host, String sessionId)
    {
        Properties properties = new ExProperties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        properties.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.server;ru.biosoft.server.tomcat");
        properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
        properties.setProperty(DataCollectionConfigConstants.IS_ROOT, String.valueOf(true));
        properties.setProperty(ClientConnection.URL_PROPERTY, host);
        properties.setProperty(ClientConnection.CONNECTION_TYPE, TomcatConnection.class.getName());
        //properties.setProperty(RemoteCollection.SHARED_SQL_PROPERTY, String.valueOf(true));
        //properties.setProperty(RemoteCollection.SESSION_PROPERTY, sessionId);
        //new RemoteCollection(null, properties);
    }

    private void createRepository(String name, String host, String user, String pass) throws Exception
    {
        Properties properties = new ExProperties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        properties.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.server;ru.biosoft.server.tomcat");
        properties.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
        properties.setProperty(DataCollectionConfigConstants.IS_ROOT, String.valueOf(true));

        //properties.setProperty(ClientConnection.URL_PROPERTY, host);
        properties.setProperty(ClientDataCollection.SERVER_URL, host);
        properties.setProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME, name);

        ClientDataCollection data = new ClientDataCollection(null, properties);
        data.login( user, pass );   

        //properties.setProperty(ClientConnection.CONNECTION_TYPE, TomcatConnection.class.getName());
        //properties.setProperty(RemoteCollection.SHARED_SQL_PROPERTY, String.valueOf(true));
        //properties.setProperty(RemoteCollection.USERNAME_PROPERTY, user);
        //properties.setProperty(RemoteCollection.PASSWORD_PROPERTY, pass);
        //new RemoteCollection(null, properties);
    }


    private static void configureJUL()
    {
        //File configFile = new File( Platform.getBundle( "biouml.plugins.node" ).getLocation(), "node.lcf" );
        //try( FileInputStream fis = new FileInputStream( configFile ) )
        try( java.io.InputStream fis = Platform.getBundle( "biouml.plugins.node" ).getEntry( "node.lcf" ).openStream() )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( IOException e )
        {
            System.out.println( "Cannot configure java.util.logging.Logger: " + e.getMessage() );
            e.printStackTrace( System.out );
        }
    }

    @Override
    public void stop()
    {
    }

    // -----------------------------------------------------------------------
    // Stuff related to Jupyter Kernel
    // -----------------------------------------------------------------------

    public static class JupyterScriptEnviroment implements ScriptEnvironment
    {
        private DisplayData dd;

        boolean isEmpty = true;

        /**
         * @param out
         */
        public JupyterScriptEnviroment( DisplayData dd )
        {
            this.dd = dd;
        }
        
        protected void write(String str)
        {
            dd.putText( str );
            isEmpty = false;
        }

        protected void println(String message)
        {
            dd.putHTML( message + "<br />" );
            isEmpty = false;
        }

        @Override
        public void error( String msg )
        {
            dd.putHTML( "<b><font color=\"red\">" + msg + "</font></b>" );
            isEmpty = false;
        }

        @Override
        public void print( String msg )
        {
            //println( msg );
            dd.putText( msg );
            isEmpty = false;
        }

        @Override
        public void info( String msg )
        {
            // do nothing
        }

        @Override
        public void showGraphics( BufferedImage image )
        {
            try
            {
                java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write( image, "png", java.util.Base64.getEncoder().wrap( os ) );
                dd.putData( "image/png", os.toString( "UTF-8" ) ); 
            }
            catch( IOException ioe )
            {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter( sw );

                ioe.printStackTrace( pw );
                dd.putHTML( "<pre><font color=\"red\">" + sw + "</font></pre>" );
            }
            isEmpty = false;
        }

        @Override
        public void showGraphics(ImageElement element) 
        {
            showGraphics( element.getImage( null ) );
            isEmpty = false;
        }
        
        @Override
        public void showHtml( String html )
        {
            dd.putHTML( html );
            isEmpty = false;
        }

        @Override
        public void showTable( TableDataCollection dataCollection )
        {
            java.io.StringWriter sw = new java.io.StringWriter();
            sw.write( "<table>\n<th><td>" );
            sw.write( dataCollection.columns().map( TableColumn::getName ).joining( "</td><td>" ) );
            sw.write( "</td></th>\n" );

            for( RowDataElement id : dataCollection )
            {
                sw.write( "<tr><td>" );
                sw.write( StreamEx.of( id.getValues() ).prepend( id ).joining( "</td><td>" ) );
                sw.write( "</td></tr>\n" );
            }
            dd.putHTML( sw.toString() );
            isEmpty = false;
        }

        @Override
        public void warn(String msg)
        {
            dd.putHTML( "<b><font color=\"orange\">" + msg + "</font></b>" );
            isEmpty = false;
        }

        @Override
        public boolean isStopped()
        {
            return false;
        }

        @Override
        public String addImage(BufferedImage image)
        {
            try
            {
                java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write( image, "png", java.util.Base64.getEncoder().wrap( os ) );
                return "data:image/png;base64,"+os.toString("UTF-8");                
            }
            catch( IOException ioe )
            {
            }
            return null;
        }
    }

    private static final SimpleAutoCompleter autoCompleter = SimpleAutoCompleter.builder()
            .preferLong()
            //Keywords from a great poem at https://stackoverflow.com/a/12114140
            .withKeywords("let", "this", "long", "package", "float")
            .withKeywords("goto", "private", "class", "if", "short")
            .withKeywords("while", "protected", "with", "debugger", "case")
            .withKeywords("continue", "volatile", "interface")
            .withKeywords("instanceof", "super", "synchronized", "throw")
            .withKeywords("extends", "final", "export", "throws")
            .withKeywords("try", "import", "double", "enum")
            .withKeywords("false", "boolean", "abstract", "function")
            .withKeywords("implements", "typeof", "transient", "break")
            .withKeywords("void", "static", "default", "do")
            .withKeywords("switch", "int", "native", "new")
            .withKeywords("else", "delete", "null", "public", "var")
            .withKeywords("in", "return", "for", "const", "true", "char")
            .withKeywords("finally", "catch", "byte")
            .build();

    private static final CharPredicate idChar = CharPredicate.builder()
            .inRange('a', 'z')
            .inRange('A', 'Z')
            .match('_')
            .build();

    private MagicsSourceTransformer magicsTransformer;
    Magics magics;

    //private final ScriptEngine engine;
    private LanguageInfo languageInfo;

    private void initKernel() 
    {
        this.languageInfo = new LanguageInfo.Builder( "ECMAScript" )
                .version( "ECMA - 262 Edition 5.1" )
                .mimetype("text/javascript")
                .fileExtension(".js")
                .pygments("javascript")
                .codemirror("javascript")
                .build();

        JScriptContext.getContext();

        this.magicsTransformer = new MagicsSourceTransformer( this );
        this.magics = new Magics();
        this.magics.registerMagics( new Load( Arrays.asList( ".js", ".javascript" ), this::eval ) );
        this.magics.registerMagics( new Shell() );
        this.magics.registerMagics( new DisplayMagics( getRenderer(), getIO().display ) );
    }

    @Override
    public LanguageInfo getLanguageInfo() 
    {
        return languageInfo;
    }

    @Override
    public DisplayData eval(String script) throws Exception 
    {     
        DisplayData dd = new DisplayData();
        try
        {
            boolean isCellMagic = script.startsWith( "%%" );

            script = this.magicsTransformer.transformMagics( script );

            if( isCellMagic )
            {
                return null;
            }

            //ScriptTypeRegistry.execute("js", script, new JScriptShellEnvironment( ps ), false);
            //ScriptTypeRegistry.execute("js", script, new JupyterScriptEnviroment( dd ), false);
            JupyterScriptEnviroment jse = new JupyterScriptEnviroment( dd );
            Object result = JScriptContext.evaluateString( script, jse );
            if( jse.isEmpty && result != null )
            {
                dd.putText( result.toString() );
            }    
        }  
        catch( Exception exc )
        {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter( sw );

            exc.printStackTrace( pw );
            dd.putHTML( "<pre><font color=\"red\">" + sw + "</font></pre>" );
        }                    

        return dd;
    }

    @Override
    public DisplayData inspect(String code, int at, boolean extraDetail) throws Exception 
    {
        StringSearch.Range match = StringSearch.findLongestMatchingAt(code, at, idChar);
        String id = "";
        Object val = null;
        if (match != null) 
        {
            id = match.extractSubString(code);
            //val = this.engine.getContext().getAttribute(id);
        }

        return new DisplayData(val == null ? "No memory value for '" + id + "'" : val.toString());
    }

    @Override
    public ReplacementOptions complete(String code, int at) throws Exception
    {
        StringSearch.Range match = StringSearch.findLongestMatchingAt(code, at, idChar);
        if (match == null)
            return null;
        String prefix = match.extractSubString(code);
        return new ReplacementOptions(autoCompleter.autocomplete(prefix), match.getLow(), match.getHigh());
    }
}

class MagicsSourceTransformer 
{
    private static final Pattern UNESCAPED_QUOTE = Pattern.compile("(?<!\\\\)\"");

    private final MagicParser parser;
    private final BioUMLNode kernel;

    public MagicsSourceTransformer( BioUMLNode kernel ) 
    {
        this.parser = new MagicParser("(?<=(?:^|=))\\s*%", "%%");
        this.kernel = kernel;
    }

    public String transformMagics(String source) throws Exception
    {
        CellMagicParseContext ctx = this.parser.parseCellMagic( source );
        if (ctx != null)
            return this.transformCellMagic(ctx);

        return transformLineMagics(source);
    }

    public String transformLineMagics(String source)
    {
        return this.parser.transformLineMagics(source, ctx -> {
            boolean inString = false;
            Matcher m = UNESCAPED_QUOTE.matcher( ctx.getLinePrefix() );
            while (m.find())
                inString = !inString;

            // If in a string literal, don't apply the magic, just use the original
            if (inString)
                return ctx.getRaw();
            try
            {
                return transformLineMagic(ctx);
            }
            catch( Exception e )
            {
                throw new RuntimeException( e );
            } 
        });
    }

    // Poor mans string escape
    private String b64Transform(String arg)
    {
        String encoded = Base64.getEncoder().encodeToString( arg.getBytes() );

        return String.format("new String(Base64.getDecoder().decode(\"%s\"))", encoded);
    }

    private String transformLineMagic(LineMagicParseContext ctx) throws Exception  
    {
        Object res = kernel.magics.applyLineMagic( ctx.getMagicCall().getName(), ctx.getMagicCall().getArgs() );
        if( res == null )
        {
            return null;   
        }
        return res.toString();
    /* 
        return String.format(
                "lineMagic(%s,List.of(%s));{};",
                this.b64Transform(ctx.getMagicCall().getName()),
                ctx.getMagicCall().getArgs().stream()
                        .map(this::b64Transform)
                        .collect(Collectors.joining(","))
        );
    */
    }

    private String transformCellMagic(CellMagicParseContext ctx) throws Exception 
    {
         Object res = kernel.magics.applyCellMagic( 
             ctx.getMagicCall().getName(), ctx.getMagicCall().getArgs(), ctx.getMagicCall().getBody() );
         
         return res == null ? null : String.valueOf( res );
    /* 
        return String.format(
                "cellMagic(%s,List.of(%s),%s);{};",
                this.b64Transform(ctx.getMagicCall().getName()),
                ctx.getMagicCall().getArgs().stream()
                        .map(this::b64Transform)
                        .collect(Collectors.joining(",")),
                this.b64Transform(ctx.getMagicCall().getBody())
        );
    */
    }
}