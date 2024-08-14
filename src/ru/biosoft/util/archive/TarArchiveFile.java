package ru.biosoft.util.archive;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.input.CountingInputStream;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

/**
 * @author lan
 *
 */
public class TarArchiveFile implements ArchiveFile
{
    private TarInputStream tarFile;
    private CountingInputStream cis;
    
    public TarArchiveFile(String name, BufferedInputStream bis)
    {
        try
        {
            if(!name.substring(name.length()-4).equalsIgnoreCase(".tar"))
            {   // Signature check
                bis.mark(0x200);
                byte[] signature = new byte[0x120];
                bis.read(signature);
                bis.reset();
                if(!new String(signature, 0x101, 7, StandardCharsets.ISO_8859_1).equals("ustar  ")) return;
            }
            cis = new CountingInputStream(bis);
            tarFile = new TarInputStream(cis);
        }
        catch(Exception e)
        {
        }
    }

    @Override
    public boolean isValid()
    {
        return tarFile != null;
    }

    @Override
    public void close()
    {
        try
        {
            tarFile.close();
        }
        catch( IOException e )
        {
        }
    }
    
    @Override
    public ArchiveEntry getNextEntry() throws IOException
    {
        final TarEntry entry = getNextValidEntry();
        return entry == null?null:new ArchiveEntry(entry.getName(), entry.isDirectory(), entry.getSize(), new InputStream()
        {   // The point of this wrapper is to disable close operation
            @Override
            public void close() throws IOException
            {
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException
            {
                return tarFile.read(b, off, len);
            }

            @Override
            public int read(byte[] b) throws IOException
            {
                return tarFile.read(b);
            }

            @Override
            public long skip(long len) throws IOException
            {
                byte[] buf = new byte[(int)len];
                // Skip is not implemented in TarInputStream, thus we need to emulate it
                return tarFile.read(buf);
            }

            @Override
            public int read() throws IOException
            {
                return tarFile.read();
            }
        });
    }

    private TarEntry getNextValidEntry() throws IOException
    {
        TarEntry entry = tarFile.getNextEntry();
        //Skip PaxHeaders files
        while( entry != null && ( entry.getHeader().linkFlag & 'x' ) == 'x' )
            entry = tarFile.getNextEntry();
        return entry == null ? null : entry;
    }

    @Override
    public long offset()
    {
        return cis.getByteCount();
    }
}
