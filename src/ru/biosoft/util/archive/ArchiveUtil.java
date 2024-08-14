package ru.biosoft.util.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArchiveUtil
{
    public static void addDirectoryToZip(ZipOutputStream out, File path, String prefix) throws Exception
    {
        for( File file : path.listFiles() )
        {
            if( file.isDirectory() )
                addDirectoryToZip( out, file, prefix + file.getName() + "/" );
            else
            {
                ZipEntry entry = new ZipEntry( prefix + file.getName() );
                out.putNextEntry( entry );
                copyStream( new FileInputStream( file ), out );
                out.closeEntry();
            }
        }
    }

    //TODO: rework
    private static final int BUFF_SIZE = 100000;
    private static final byte[] buffer = new byte[BUFF_SIZE];

    private static void copyStream(InputStream in, OutputStream out) throws IOException
    {
        try
        {
            while( true )
            {
                synchronized( buffer )
                {
                    int amountRead = in.read( buffer );
                    if( amountRead == -1 )
                    {
                        break;
                    }
                    out.write( buffer, 0, amountRead );
                }
            }
        }
        finally
        {
            if( in != null )
            {
                in.close();
            }
        }
    }
}
