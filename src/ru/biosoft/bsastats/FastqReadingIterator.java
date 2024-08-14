package ru.biosoft.bsastats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.util.ReadAheadIterator;

public class FastqReadingIterator extends ReadAheadIterator<Task> implements ProgressIterator<Task>
{
    private BufferedReader input;
    private FileChannel ch;
    private byte offset;
    private boolean rightAlignment;

    public FastqReadingIterator(File file, String encoding, boolean rightAlignment) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        ch = fis.getChannel();
        InputStream is = fis;
        if( isGzipFile( file ) )
            is = new GZIPInputStream( is );

        input = new BufferedReader(new InputStreamReader(is));
        
        offset = EncodingSelector.ENCODING_TO_OFFSET.get(encoding);
        this.rightAlignment = rightAlignment;
    }

    private boolean isGzipFile(File file) throws IOException
    {
        try (FileInputStream fis = new FileInputStream( file ))
        {
            if( fis.read() != 0x1f || fis.read() != 0x8b )
                return false;
        }
        return true;
    }

    @Override
    protected Task advance()
    {
        try
        {
            String l1 = input.readLine();
            String l2 = input.readLine();
            String l3 = input.readLine();
            String l4 = input.readLine();
            if( l1 == null || l2 == null || l3 == null || l4 == null || !l1.startsWith("@") || !l3.startsWith("+") )
                throw new IllegalArgumentException("Supplied FASTQ file has an invalid format");
            byte[] sequence = l2.trim().getBytes();
            byte[] qualities = l4.trim().getBytes();
            for(int i=qualities.length-1; i>=0; i--) qualities[i]-=offset;
            if(rightAlignment)
            {
                ArrayUtils.reverse(sequence);
                ArrayUtils.reverse(qualities);
            }
            return new Task(sequence, qualities, l1+"\n"+l2+"\n"+l3+"\n"+l4+"\n");
        }
        catch( Exception e )
        {
            try
            {
                if(input != null) input.close();
            }
            catch( IOException e1 )
            {
            }
        }
        return null;
    }

    @Override
    public float getProgress()
    {
        if(!hasNext()) return 1;
        try
        {
            return ((float)ch.position())/ch.size();
        }
        catch( IOException e )
        {
            return 0;
        }
    }

}