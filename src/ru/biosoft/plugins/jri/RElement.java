package ru.biosoft.plugins.jri;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import one.util.streamex.EntryStream;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.ImageElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.access.script.ScriptJobControl;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.plugins.jri.rdirect.RDirectSession;
import ru.biosoft.plugins.jri.rdirect.RWriter;
import ru.biosoft.util.ImageUtils;
import ru.biosoft.util.TempFiles;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.application.Application;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlException;

@ClassIcon("resources/rscript.gif")
@PropertyName("R-script")
public class RElement extends ScriptDataElement
{
    private static final Pattern HELP_PATTERN_QUESTION = Pattern.compile("\\?\\s*([^\\s\\?]+)");
    private static final Pattern HELP_PATTERN_HELP = Pattern.compile("help\\s*\\(\\s*([^\\s\\?]+)\\s*\\)");

    public static final int DEFAULT_WIDTH = 600;
    public static final int DEFAULT_HEIGHT = 400;

    private static final String BEFORE_R_COMMAND =
            "options(biouml_connection=list(url='{serverPath}', sessionId='{sessionId}'))\n" +
            "options(device=function() {" +
            "  .BioUML.hook()\n"+
            "  png('{outputDir}/{launchId}%05d.png', width="+DEFAULT_WIDTH+", height="+DEFAULT_HEIGHT+");" +
            "  dev.control('enable');\n"+
            "})\n" +
            ".BioUML.savedPlots.num <<- 0\n"+
            ".BioUML.hook <<- function() {\n"+
            "  if(dev.cur() != 1) {\n"+
            "    .BioUML.savedPlot <- recordPlot()\n"+
            "    if(length(.BioUML.savedPlot[[1]]) > 0) {\n"+
            "      .BioUML.savedPlots.num <<- .BioUML.savedPlots.num + 1\n"+
            "      .BioUML.savedPlots[[sprintf(\"{launchId}%05d\", .BioUML.savedPlots.num)]] <<- .BioUML.savedPlot;\n"+
            "    }\n"+
            "  }\n"+
            "}\n";
    private static final String AFTER_R_COMMAND =
            ".BioUML.hook()\n"+
            "while(dev.cur() != 1) {" +
            "  dev.off()\n" +
            "}\n";

    public static final String RDIRECT_SESSION_CONNECTION_PROPERTY = "properties/Rdirect/connection";

    public RElement(DataCollection<?> parent, String name, String data)
    {
        super(name, parent, data);
    }

    @Override
    public String getContentType()
    {
        return "text/x-r";
    }

    @Override
    protected ScriptJobControl createJobControl(String content, ScriptEnvironment env, Map<String, Object> scope,
            Map<String, Object> outVars, boolean sessionContext)
    {
        return new RJobControl(content, env, scope, outVars, sessionContext);
    }

    private static class RImageElement extends DataElementSupport implements ImageElement, Closeable
    {
        private final RDirectSession rObject;
        private final File dir;
        private final String fileName;

        private RImageElement(RDirectSession rObject, File dir, String name)
        {
            super("", null);
            this.rObject = rObject;
            rObject.addRef();
            this.dir = dir;
            this.fileName = name;
        }

        @Override
        public Dimension getImageSize()
        {
            return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        }
        @Override
        public BufferedImage getImage(Dimension dimension)
        {
            Dimension size = dimension == null ? getImageSize() : ImageUtils.correctImageSize(dimension);
            if(size.width < 150)
                size.width = 150;
            if(size.height < 150)
                size.height = 150;
            File outputFile = new File(dir, fileName+"_"+size.width+"_"+size.height+".png");
            if(!outputFile.exists())
            {
                String command = "png('"+outputFile.getAbsolutePath().replace('\\', '/')+"', width="+size.width+", height="+size.height+")\n" +
                    "replayPlot(.BioUML.savedPlots[["+RWriter.getRString( fileName )+"]])\n" +
                    "dev.off()\n";
                synchronized(rObject)
                {
                    rObject.setEnvironment(new LogScriptEnvironment(Logger.getLogger(RElement.class.getName()), true));
                    rObject.eval(command);
                }
            }
            try
            {
                return ImageIO.read(outputFile);
            }
            catch( IOException e )
            {
                ExceptionRegistry.log(e);
                size = getImageSize();
                outputFile = new File(dir, fileName+"_"+size.width+"_"+size.height+".png");
                try
                {
                    return ImageIO.read(outputFile);
                }
                catch( IOException e1 )
                {
                    ExceptionRegistry.log(e1);
                    return null;
                }
            }
        }

        @Override
        public void close() throws IOException
        {
            File[] listFiles = dir.listFiles();
            if(listFiles != null)
            {
                for(File inFile: listFiles)
                {
                    if(inFile.getName().startsWith(fileName))
                        inFile.delete();
                }
            }
            dir.delete();
            rObject.evalSilent( ".BioUML.savedPlots[["+RWriter.getRString( fileName )+"]] <- NULL" );
            rObject.unlink();
        }
    }

    private static class RJobControl extends ScriptJobControl
    {
        private String content;
        private final ScriptEnvironment env;
        private final Map<String, Object> scope;
        private final Map<String, Object> outVars;
        private RDirectSession rObject;
        private BreakType type = BreakType.NONE;

        public RJobControl(String content, ScriptEnvironment env, Map<String, Object> scope, Map<String, Object> outVars,
                boolean sessionContext)
        {
            this.content = content;
            this.env = env;
            this.scope = scope;
            this.outVars = outVars;
            if(sessionContext)
            {
                SessionCache sessionCache = SessionCacheManager.getSessionCache();
                synchronized( sessionCache )
                {
                    rObject = (RDirectSession)sessionCache.getObject( RDIRECT_SESSION_CONNECTION_PROPERTY );
                    if( rObject == null || !rObject.isValid() )
                        sessionCache.addObject( RDIRECT_SESSION_CONNECTION_PROPERTY, rObject = RDirectSession.create(), true );
                }
                rObject.addRef();
            }else
                rObject = RDirectSession.create();
        }

        @Override
        public void terminate()
        {
            super.terminate();
            end();
            rObject.cancel();
        }

        private static String getHelpTopic(String content)
        {
            content = content.trim();
            Matcher matcher = HELP_PATTERN_HELP.matcher(content);
            if(matcher.matches())
                return matcher.group(1);
            matcher = HELP_PATTERN_QUESTION.matcher(content);
            if(matcher.matches())
                return matcher.group(1);
            return null;
        }

        @Override
        public String getResult()
        {
            return "";
        }

        @Override
        public int getCurrentLine()
        {
            return rObject.getCurrentLine();
        }

        @Override
        public void breakOn(BreakType type)
        {
            this.type = type;
        }

        @Override
        protected void doRun() throws JobControlException
        {
            File outputDir = null;
            try
            {
                content = content.replace("\r", "");
                rObject.setEnvironment(env);
                String helpTopic = getHelpTopic(content);
                if(helpTopic != null)
                {
                    rObject.help(helpTopic);
                    return;
                }
                outputDir = TempFiles.dir("rOutput");
                String serverPath = Application.getGlobalValue( "ServerPath", "http://localhost:8080/biouml" );
                String cmd = BEFORE_R_COMMAND;
                String session = SecurityManager.getSession();
                String launchId = outputDir.getName();
                if(session.equals(SecurityManager.SYSTEM_SESSION))
                    session = Application.getGlobalValue("ServerSession", session);
                rObject.evalSilent(cmd
                        .replace("{serverPath}", serverPath)
                        .replace("{sessionId}", session)
                        .replace("{launchId}", launchId)
                        .replace("{outputDir}", outputDir.getAbsolutePath().replace('\\', '/')));
                EntryStream.of(scope).forKeyValue(rObject::assign);
                if(type != BreakType.NONE)
                {
                    boolean inDebug = rObject.initDebug( content );
                    while(inDebug)
                    {
                        inDebug = rObject.step( type );
                        if(inDebug)
                        {
                            pause();
                            try
                            {
                                checkStatus();
                            }
                            catch( JobControlException e )
                            {
                                if(e.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                                {
                                    throw new CancellationException();
                                }
                                throw ExceptionRegistry.translateException( e );
                            }
                        }
                    }
                } else
                {
                    rObject.eval(content);
                }
                cmd = AFTER_R_COMMAND;
                rObject.evalSilent(cmd
                        .replace("{serverPath}", serverPath)
                        .replace("{sessionId}", session)
                        .replace("{launchId}", launchId)
                        .replace("{outputDir}", outputDir.getAbsolutePath().replace('\\', '/')));
                for(String outVarName : new ArrayList<>(outVars.keySet()))
                {
                    Object value = rObject.getAsObject( outVarName );
                    if(value != null)
                        outVars.put( outVarName, value );
                }
                int nPlots = Integer.parseInt( rObject.getAsString( ".BioUML.savedPlots.num" ) );
                for(int i=0; i<nPlots; i++)
                {
                    env.showGraphics(new RImageElement(rObject, outputDir, String.format( Locale.ENGLISH, "%s%05d", launchId, i+1 )));
                }
            }
            catch( CancellationException ex )
            {
                env.warn("Cancelled by user");
            }
            catch( Throwable t )
            {
                env.error(ExceptionRegistry.log(t));
            }
            finally
            {
                // We want to delete here only empty directory
                if(outputDir != null)
                    outputDir.delete();
                rObject.unlink();
            }
        }
    }
}
