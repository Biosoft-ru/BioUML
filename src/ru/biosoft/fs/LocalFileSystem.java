package ru.biosoft.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang.SystemUtils;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.exception.InternalException;

public class LocalFileSystem implements FileSystem
{
    private final Path root;
    private static final Pattern FILE_NAME_REGEX =
            SystemUtils.IS_OS_WINDOWS ? Pattern.compile( "^[^/\\:*?\"<>|]+$" )
                    : Pattern.compile( "^[^/]+$" );
            
    private static final Pattern FILE_NAME_FILTER = Pattern.compile("^(.git|.gitignore)$");
    
    public LocalFileSystem(Properties properties)
    {
        String rootPath = properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        if(rootPath == null)
        {
            throw new InternalException( "Root path is not specified" );
        }
        root = Paths.get( rootPath );
    }

    @Override
    public boolean isValid(@Nonnull String name)
    {
        return FILE_NAME_REGEX.matcher( name ).matches() && !FILE_NAME_FILTER.matcher( name ).matches();
    }

    @Override
    public @Nonnull
    FileSystemEntry[] list(FileSystemPath fsPath) throws IOException
    {
        Path path = Paths.get( root.toString(), fsPath.components() );
        return Files.list( path ).filter( p -> 
                !FILE_NAME_FILTER.matcher( p.getFileName().toString() ).matches() )
                .map( p -> new FileSystemEntry( p.getFileName().toString(), Files.isDirectory( p ) ) ).toArray( FileSystemEntry[]::new );
    }

    @Override
    public void createDirectory(FileSystemPath path) throws IOException
    {
        Files.createDirectories( Paths.get( root.toString(), path.components()) );
    }

    @Override
    public void delete(FileSystemPath path) throws IOException
    {
        File file = Paths.get( root.toString(), path.components()).toFile();
        if(!file.exists())
            throw new FileNotFoundException( file.toString() );
        if(file.isDirectory())
        {
            ApplicationUtils.removeDir( file );
        }
        if(!file.delete())
        {
            if(file.exists())
                throw new IOException( "Unable to delete file "+file.toString() );
        }
    }
    
    @Override
    public void writeFile(FileSystemPath path, long numBytes, InputStream is) throws IOException
    {
        Path localPath = Paths.get( root.toString(), path.components());
        try(OutputStream os = Files.newOutputStream( localPath ))
        {
            ApplicationUtils.copyStreamNoClose( os, is );
        }
        if(FileSystemCollection.METAFILE_NAME.equals( path.name()))
        {
            try
            {
                Files.setAttribute( localPath, "dos:hidden", true );
            }
            catch( UnsupportedOperationException | IllegalArgumentException e )
            {
                // Ignore
            }
        }
    }

    @Override
    public void readFile(FileSystemPath path, OutputStream os) throws IOException
    {
        try(InputStream is = Files.newInputStream( Paths.get( root.toString(), path.components()) ))
        {
            ApplicationUtils.copyStreamNoClose( os, is );
        }
    }
}
