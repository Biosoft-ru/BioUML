package ru.biosoft.util.archive;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.biosoft.jobcontrol.JobControl;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.util.FileItem;
import ru.biosoft.util.TempFiles;

/**
 * Factory which determines type of the archive and returns corresponding object
 * @author lan
 */
public class ArchiveFactory
{
    protected static ArchiveFile getArchiveFile(File file, String name, BufferedInputStream bis)
    {
        boolean myStream = false;
        ArchiveFile result = null;
        try
        {
            if(file != null)
            {
                if(bis == null)
                {
                    bis = new BufferedInputStream(new FileInputStream(file));
                    myStream = true;
                }
                if( name == null && file instanceof FileItem )
                    name = ((FileItem)file).getOriginalName();
                if( name == null )
                    name = file.getName();
            }
            result = new GZipArchiveFile(name, bis);
            if(!result.isValid()) result = new TarArchiveFile(name, bis);
            if(!result.isValid())
            {
                if(file == null && name.substring(name.length()-4).toLowerCase().equals(".zip"))
                {
                    file = TempFiles.file("archiveEntry_"+name, bis);
                }
                result = new ZipArchiveFile(file);
            }
            return result.isValid()?result:null;
        }
        catch( Exception e )
        {
            return null;
        }
        finally
        {
            if(myStream && (result == null || !result.isValid()))
            {
                try
                {
                    bis.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }
    
    public static ArchiveFile getArchiveFile(File file)
    {
        return getArchiveFile(file, null, null);
    }
    
    public static ArchiveFile getArchiveFile(ArchiveEntry entry)
    {
        return getArchiveFile(null, entry.getName(), entry.getInputStream());
    }
    
    public static void unpack(File archive, File targetDirectory, JobControl jobControl) throws Exception
    {
        ArchiveFile archiveFile = getArchiveFile(archive);
        if(archiveFile == null) throw new IllegalArgumentException("Supplied file is not an archive");
        archiveFile = new ComplexArchiveFile(archiveFile);
        long size = archive.length();
        ArchiveEntry entry;
        while((entry = archiveFile.getNextEntry()) != null)
        {
            if(jobControl != null)
            {
                if(size > 0)
                    jobControl.setPreparedness((int) ( archiveFile.offset()*100./size ));
                if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
                {
                    archiveFile.close();
                    throw new InterruptedException();
                }
            }
            if(entry.isDirectory()) continue;
            File file = new File(targetDirectory, entry.getName());
            file.getParentFile().mkdirs();
            InputStream is = entry.getInputStream();
            FileOutputStream os = new FileOutputStream(file);
            ApplicationUtils.copyStream(os, is);
        }
    }
}
