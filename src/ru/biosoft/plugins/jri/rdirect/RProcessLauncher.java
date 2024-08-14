package ru.biosoft.plugins.jri.rdirect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import ru.biosoft.exception.LoggedException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.plugins.jri.rdirect.Message.MessageType;
import ru.biosoft.tasks.process.LocalProcessLauncher;

class RProcessLauncher extends LocalProcessLauncher
{
    class StreamCopyThread extends Thread
    {
        private final InputStream in;
        private final MessageType type;

        public StreamCopyThread(String name, InputStream in, MessageType type)
        {
            super(name);
            this.in = in;
            this.type = type;
        }

        @Override
        public void run()
        {
            byte[] buf = new byte[64000];
            while(true)
            {
                try
                {
                    int len = in.read( buf );
                    if(len == -1)
                    {
                        addMessage( new Message( MessageType.EXIT, null ) );
                        break;
                    }
                    addMessage( new Message( type, new String( buf, 0, len, encoding ) ) );
                }
                catch( IOException e )
                {
                    addMessage( new Message( MessageType.EXIT, null ) );
                    if(!e.getMessage().equals( "Stream closed" ))
                        ExceptionRegistry.log(e);
                    return;
                }
            }
        }
    }

    final LinkedBlockingDeque<Message> messages;
    private StreamCopyThread outThread;
    private StreamCopyThread errThread;
    final Charset encoding;
    private final long timeOutMillis;

    public RProcessLauncher(Charset encoding, long timeOutMillis)
    {
        this.encoding = encoding;
        this.timeOutMillis = timeOutMillis;
        this.messages = new LinkedBlockingDeque<>();
    }

    @Override
    public void execute() throws LoggedException
    {
        try
        {
            ProcessBuilder processBuilder = getProcessBuilder();
            proc = processBuilder.start();
            running = true;
            InputStream inputStream = proc.getInputStream();
            InputStream errorStream = proc.getErrorStream();
            outThread = new StreamCopyThread(Thread.currentThread().getName()+"-R input reader", inputStream, MessageType.OUT);
            outThread.start();
            errThread = new StreamCopyThread(Thread.currentThread().getName()+"-R error reader", errorStream, MessageType.ERR);
            errThread.start();
        }
        catch( IOException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    public void addMessage(Message msg)
    {
        while( true )
        {
            try
            {
                messages.put( msg );
                break;
            }
            catch( InterruptedException e1 )
            {
            }
        }
    }

    public void write(String str) throws IOException
    {
        if(proc == null)
        {
            throw new IllegalStateException( "Process is not running" );
        }
        OutputStream os = proc.getOutputStream();
        if(os == null)
        {
            throw new IllegalStateException( "Process has no standard input" );
        }
        os.write( str.getBytes( encoding ) );
        os.flush();
    }

    @Override
    public void terminate()
    {
        if(proc != null)
        {
            try
            {
                proc.getInputStream().close();
            }
            catch( IOException e )
            {
            }
            try
            {
                proc.getErrorStream().close();
            }
            catch( IOException e )
            {
            }
            try
            {
                proc.getOutputStream().close();
            }
            catch( IOException e )
            {
            }
        }
        super.terminate();
    }

    public Message read()
    {
        while( true )
        {
            try
            {
                Message message = timeOutMillis > 0 ? messages.poll( timeOutMillis, TimeUnit.MILLISECONDS ) : messages.take();
                return message == null ? new Message( MessageType.TIME_OUT, String.valueOf(timeOutMillis) ) : message;
            }
            catch( InterruptedException e1 )
            {
            }
        }
    }

    public Message readNonBlocking()
    {
        return messages.poll();
    }
}