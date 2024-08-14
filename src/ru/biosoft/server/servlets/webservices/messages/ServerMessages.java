package ru.biosoft.server.servlets.webservices.messages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.biosoft.access.security.SecurityManager;

public class ServerMessages
{
    private static Map<String, Queue> queueBySession = new ConcurrentHashMap<>();


    public static void sendMessageToSession(Message msg, String session)
    {
        Queue queue = queueBySession.get( session );
        if( queue != null )
            queue.enqueue( msg );
    }

    public static void sendMessageToCurrentSession(Message msg)
    {
        String session = getCurrentSession();
        sendMessageToSession( msg, session );
    }
    
    
    public static void sendMessageToUser(Message msg, String user)
    {
        for(String session : SecurityManager.getSessionIdsForUser( user ))
            sendMessageToSession( msg, session );
    }
    
    public static void sendMessageToCurrentUser(Message msg)
    {
        for(String session : SecurityManager.getSessionIdsForCurrentUser())
            sendMessageToSession( msg, session );
    }

    public static void broadcastMessage(Message msg)
    {
        queueBySession.forEach( (session, queue) -> {
            queue.enqueue( msg );
        } );
    }
    

    public static void subscribe(String msgType)
    {
        String session = getCurrentSession();
        Queue queue = queueBySession.computeIfAbsent( session, k -> new Queue( k ) );
        queue.subscribe( msgType );
    }

    public static void unsubscribe(String msgType)
    {
        String session = getCurrentSession();
        Queue queue = queueBySession.get( session );
        if( queue != null )
            queue.unsubscribe( msgType );
    }

    public static void unsubscribeAll()
    {
        String session = getCurrentSession();
        queueBySession.remove( session );
    }
    
    static Message takeMessage() throws InterruptedException
    {
        String session = getCurrentSession();
        Queue queue = queueBySession.computeIfAbsent( session, k -> new Queue( k ) );
        return queue.take();
    }
    
    private static String getCurrentSession()
    {
        String session = SecurityManager.getSession();
        if( SecurityManager.SYSTEM_SESSION.equals( session ) )
            throw new IllegalStateException("Thread not associated with any session");
        return session;
    }
}
