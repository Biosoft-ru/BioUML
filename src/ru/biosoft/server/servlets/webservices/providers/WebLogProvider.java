package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.log.PatternFormatter;
import com.developmentontheedge.log.WriterHandler;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.util.LimitedTextBuffer;

public class WebLogProvider extends WebJSONProviderSupport
{
    public static ConcurrentHashMap<String, LimitedTextBuffer> logs;
    private static final int DEFAULT_LOG_LIMIT = 100;
    private static final Level DEFAULT_LOG_LEVEL = Level.SEVERE;

    public static void initWebLogger(Preferences logPreferences)
    {
        if( SecurityManager.isExperimentalFeatureHidden() )
            return;
        logs = new ConcurrentHashMap<>();
        int logLimit = logPreferences.getIntValue( "LogLimit", DEFAULT_LOG_LIMIT );
        WriterHandler webLogHandler = new WriterHandler( new LogWriter( logs, logLimit ), new PatternFormatter( "%4$s - %5$s%n" ) );
        String strLevel = logPreferences.getStringValue( "Level", null );
        Level level = strLevel != null ? Level.parse( strLevel ) : DEFAULT_LOG_LEVEL;
        webLogHandler.setLevel( level );
        Logger cat = Logger.getLogger( "" );
        cat.addHandler( webLogHandler );
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        switch( arguments.getAction() )
        {
            case "get":
                getLog( response );
                return;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION );
        }
    }


    private void getLog(JSONResponse response) throws IOException
    {
        String curSession = SecurityManager.getSession();
        if( logs == null )
        {
            response.error( "Log is disabled" );
            return;
        }
        if( logs.containsKey( curSession ) )
        {
            String log = logs.get( curSession ).toString();
            response.sendString( log );
        }
        else
        {
            response.error( "Can not find log for session " + curSession );
        }

    }

    public static class LogWriter extends Writer
    {
        private static ConcurrentHashMap<String, LimitedTextBuffer> logMap;
        private int limit;
        public LogWriter(ConcurrentHashMap<String, LimitedTextBuffer> logs, int limit)
        {
            this.limit = limit;
            logMap = logs;
        }
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException
        {
            String curSession = SecurityManager.getSession();
            LimitedTextBuffer buf = logMap.computeIfAbsent( curSession, s -> new LimitedTextBuffer( limit ) );
            buf.add( new String( cbuf, off, len ) );
        }

        @Override
        public void flush() throws IOException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void close() throws IOException
        {
            // TODO Auto-generated method stub

        }

    }


}
