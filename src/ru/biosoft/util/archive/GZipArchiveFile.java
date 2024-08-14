package ru.biosoft.util.archive;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.input.CountingInputStream;

/**
 * @author lan
 *
 */
public class GZipArchiveFile implements ArchiveFile
{
    private GzipCompressorInputStream inputStream;
    private ArchiveEntry entry;
    private boolean read = false;
    private CountingInputStream cis;
    
    public GZipArchiveFile(String fileName, BufferedInputStream bis)
    {
        try
        {
            if(fileName.toLowerCase().endsWith(".bam")) return; // Skip BAM files
            String name = fileName.replaceFirst("\\.(gz|GZ)$", "").replaceFirst("\\.(tgz|TGZ)$", ".tar");
            bis.mark(8192);
            byte[] header = new byte[10];
            bis.read(header);
            if( header[0] != (byte)0x1F || header[1] != (byte)0x8B )
                throw new UnsupportedOperationException();
            try
            {   // Try to read original file name if it's present
                // See http://www.gzip.org/zlib/rfc-gzip.html#file-format for details
                if((header[3] & 0x08) != 0) // File name is set
                {
                    StringBuilder sb = new StringBuilder();
                    for(int i=0; i<512; i++)
                    {
                        int ch = bis.read();
                        if (ch == -1) throw new EOFException();
                        if (ch == 0) break; // you read a NUL
                        sb.append((char)ch);
                    }
                    name = sb.toString();
                }
            }
            catch( Exception e )
            {
            }
            bis.reset();
            cis = new CountingInputStream(bis);
            inputStream = new GzipCompressorInputStream(cis, true);
            entry = new ArchiveEntry(name, false, -1, inputStream);
        }
        catch(Exception e)
        {
            try
            {
                bis.reset();
            }
            catch( IOException e1 )
            {
            }
        }
    }

    @Override
    public boolean isValid()
    {
        return entry != null;
    }

    @Override
    public void close()
    {
        try
        {
            inputStream.close();
        }
        catch( IOException e )
        {
        }
    }

    @Override
    public ArchiveEntry getNextEntry()
    {
        if(read) return null;
        read = true;
        return entry;
    }

    @Override
    public long offset()
    {
        return cis.getByteCount();
    }
}
