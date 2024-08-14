package ru.biosoft.server.servlets.webservices.messages;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import java.util.logging.Logger;

public class Queue
{
    public static final Logger log = Logger.getLogger( Queue.class.getName() );
    public static final int MAX_MESSAGES_PER_USER = 1000;

    private String user;
    private LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>( MAX_MESSAGES_PER_USER );
    private Set<String> subscriptions = ConcurrentHashMap.newKeySet();
    
    public Queue(String user) 
    {
        this.user = user;
    }

    public void enqueue(Message msg)
    {
        if(!subscriptions.contains( msg.getType() ))
            return;
        if( !queue.offer( msg ) )
            log.warning( "Queue is full for " + user + ", message will not be sent." );
    }
    
    public void subscribe(String msgType)
    {
        subscriptions.add( msgType );
    }
    
    public void unsubscribe(String msgType)
    {
        subscriptions.remove( msgType );
        queue.removeIf( m->m.getType().equals( msgType ) );
    }
    
    public Message take() throws InterruptedException
    {
        return queue.poll( 10, TimeUnit.SECONDS );
    }
}
