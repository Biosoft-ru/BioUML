package ru.biosoft.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;


public class StreamGobbler extends Thread
{
    StringBuilder streamData;
    InputStream is;
    boolean done;

    public StreamGobbler(InputStream is)
    {
        this(is, false);
    }

    public StreamGobbler(InputStream is, boolean save)
    {
        super("StreamGobbler-" + is);
        this.is = is;
        if( save )
        {
            streamData = new StringBuilder();
        }
        start();
    }

    public @Nonnull String getData()
    {
        try
        {
            synchronized( this )
            {
                while( !done )
                    wait();
            }
        }
        catch( InterruptedException e )
        {

        }
        return streamData.toString();
    }



    @Override
    public void run()
    {
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while( ( line = br.readLine() ) != null )
            {
                if( streamData != null )
                    streamData.append(line).append("\n");
            }
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        finally
        {
            synchronized( this )
            {
                done = true;
                notify();
            }
        }
    }
}