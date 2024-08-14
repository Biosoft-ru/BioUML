package ru.biosoft.server.servlets.webservices.messages;

import java.io.IOException;

import java.util.logging.Level;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

public class ServerMessagesProvider extends WebJSONProviderSupport
{
    
    @Override
    public void process(BiosoftWebRequest args, JSONResponse resp) throws Exception
    {
        String action = args.getAction();
        switch(action)
        {
            case "subscribe":
                processSubscribe(args, resp);
                break;
            case "unsubscribe":
                processUnsubscribe(args, resp);
                break;
            case "listen":
                processListen(args, resp);
                break;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", "action" );
        }
    }

    private void processSubscribe(BiosoftWebRequest args, JSONResponse resp) throws WebException, IOException
    {
        String msgType = args.getString( "msgType" );
        ServerMessages.subscribe( msgType );
        resp.sendString( "ok" );
        log.log(Level.FINE,  "User " + SecurityManager.getSessionUser() + " subscribed to " + msgType );
    }
    
    private void processUnsubscribe(BiosoftWebRequest args, JSONResponse resp) throws IOException
    {
        String msgType = args.get( "msgType" );
        if(msgType == null)
        {
            ServerMessages.unsubscribeAll();
            log.log(Level.FINE,  "User " + SecurityManager.getSessionUser() + " unsubscribed from all messages" );
        }
        else
        {
            ServerMessages.unsubscribe( msgType );
            log.log(Level.FINE,  "User " + SecurityManager.getSessionUser() + " unsubscribed from " + msgType );
        }
        resp.sendString( "ok" );
        
    }

    private void processListen(BiosoftWebRequest args, JSONResponse resp) throws InterruptedException, IOException, JSONException
    {
        Message msg = ServerMessages.takeMessage();
        JSONObject json = new JSONObject();
        if(msg != null)
        {
            json.put( "type", msg.getType() );
            json.put( "content", msg.getContent() );
            log.log(Level.FINE,  "Message sent to " + SecurityManager.getSessionUser() + ": type=" + msg.getType() + " content=" + msg.getContent());
        }
        resp.sendJSON( json );
        
    }

}
