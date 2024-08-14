package ru.biosoft.util.entry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Callable;

import org.osgi.framework.Bundle;

import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.TempFileManager;
import ru.biosoft.util.Util;

public class BundleEntry extends PluginEntry
{
    private final Bundle b;
    private final String subPath;
    private final boolean dir;

    public BundleEntry(Bundle b, String subPath)
    {
        if(!subPath.startsWith( "/" ))
            throw new InternalException( "Invalid subPath: "+subPath );
        this.b = b;
        if(subPath.endsWith( "/" ))
        {
            subPath = subPath.substring( 0, subPath.length()-1 );
            dir = true;
        } else
        {
            dir = false;
        }
        this.subPath = subPath;
    }
    
    @Override
    public PluginEntry[] children() throws IOException
    {
        Enumeration<String> paths = b.getEntryPaths( subPath );
        if(paths == null)
        {
            throw new FileNotFoundException( toString() );
        }
        return StreamEx.of( Collections.list( paths ) ).map( path -> path.startsWith( "/" ) ? path : "/" + path )
                .map( path -> new BundleEntry( b, path ) ).toArray( PluginEntry[]::new );
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        URL entry = b.getEntry( subPath );
        if(entry == null)
            throw new FileNotFoundException( subPath );
        return entry.openStream();
    }

    @Override
    public PluginEntry child(String name)
    {
        return new BundleEntry( b, subPath+'/'+name );
    }

    @Override
    public boolean is(File file)
    {
        return false;
    }

    @Override
    public String getName()
    {
        int lastIndexOf = subPath.lastIndexOf( '/' );
        if(lastIndexOf == -1)
            return subPath;
        return subPath.substring( lastIndexOf+1 );
    }

    @Override
    public String toString()
    {
        return b.getSymbolicName() + ":" + ( subPath.isEmpty() ? "" : subPath.substring( 1 ) );
    }

    @Override
    public boolean isDirectory()
    {
        return dir;
    }

    @Override
    public boolean exists()
    {
        return b.getEntry( subPath ) != null;
    }

    @Override
    public File getFile()
    {
        return null;
    }

    @Override
    public PluginEntry getParent()
    {
        if(subPath.isEmpty())
            return this;
        return new BundleEntry( b, subPath.substring( 0, subPath.lastIndexOf( '/' ) + 1 ) );
    }
    
    @CodePrivilege(CodePrivilegeType.TEMP_RESOURCES_ACCESS)
    public class Extractor implements Callable<File>
    {
        @Override
        public File call() throws IOException
        {
            File resourcesTempDirectory = TempFileManager.getDefaultManager().getResourcesTempDirectory();
            File file = new File(new File(resourcesTempDirectory, b.getSymbolicName()), subPath.substring( 1 ));
            if(file.exists())
                return file;
            File parent = file.getParentFile();
            File tempFile = new File(parent, file.getName()+".part."+Util.getUniqueId());
            if(!parent.isDirectory())
                parent.mkdir();
            if(!parent.isDirectory())
            {
                throw new FileNotFoundException( parent.getAbsolutePath() );
            }
            ApplicationUtils.copyStream( new FileOutputStream( tempFile ), getInputStream() );
            tempFile.renameTo( file );
            if(file.getName().endsWith( ".sh" ))
            {
                file.setExecutable( true );
            }
            return file;
        }
    }

    @Override
    public File extract() throws IOException
    {
        return new Extractor().call();
    }
}