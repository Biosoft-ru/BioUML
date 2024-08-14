package ru.biosoft.util.archive;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;



/**
 * @author lan
 *
 */
public class ZipArchiveFile implements ArchiveFile
{
    ZipFile zipFile;
    Enumeration<?> entries;
    long readSize = 0;
    long totalSize = -1;
    long fileSize;
    
    public ZipArchiveFile(File file)
    {
        try
        {
            zipFile = new ZipFile(file);
            fileSize = file.length();
        }
        catch(Exception e)
        {
        }
    }

    @Override
    public boolean isValid()
    {
        return zipFile != null;
    }

    @Override
    public void close()
    {
        try
        {
            zipFile.close();
        }
        catch( IOException e )
        {
        }
    }
    
    @Override
    public ArchiveEntry getNextEntry()
    {
        if(entries == null)
            entries = zipFile.entries();
        if(!entries.hasMoreElements()) return null;
        ZipEntry entry = (ZipEntry)entries.nextElement();
        readSize+=entry.getCompressedSize();
        try
        {
            return new ArchiveEntry(entry.getName(), entry.isDirectory(), entry.getSize(), zipFile.getInputStream(entry));
        }
        catch( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long offset()
    {
        if(totalSize == -1)
        {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            totalSize = 0;
            while(entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                totalSize+=entry.getCompressedSize();
            }
        }
        return (long) (((double)readSize)/totalSize*fileSize);
    }
}
