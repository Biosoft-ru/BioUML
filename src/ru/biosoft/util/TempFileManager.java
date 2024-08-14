package ru.biosoft.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import ru.biosoft.access.BiosoftSecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.SecurityManager;

import com.developmentontheedge.application.ApplicationUtils;

@CodePrivilege(CodePrivilegeType.SYSTEM)
public class TempFileManager
{
    private File applicationTemp;
    private final AtomicInteger fileNum = new AtomicInteger(0);

    private static final TempFileManager instance = new TempFileManager(null);
    private static Map<String, TempFileManager> managers = new HashMap<>();

    private static File createTempFile(String prefix, String suffix, File root)
    {
        if(root == null)
        {
            root = new File(System.getProperty( "java.io.tmpdir" ));
        }
        SimpleDateFormat df = new SimpleDateFormat( "yyyyMMddHHmmssSSS" );
        while(true)
        {
            String name = prefix+df.format( new Date() )+suffix;
            File tempFile = new File(root, name);
            if(!tempFile.exists())
                return tempFile;
            try
            {
                Thread.sleep( 1 );
            }
            catch( InterruptedException e )
            {
                // ignore
            }
        }
    }

    private TempFileManager(File tmpRoot)
    {
        try
        {
            if(tmpRoot == null)
            {
                applicationTemp = createTempFile("BioUML_", ".tmp", null);
            }
            else
            {
                if(!tmpRoot.exists())
                    tmpRoot.mkdir();
                applicationTemp = createTempFile("BioUML_", ".tmp", tmpRoot);
            }
            if(!applicationTemp.mkdir())
                throw new IOException("Cannot create "+applicationTemp);
            applicationTemp.deleteOnExit();
        }
        catch( IOException e )
        {
            throw new InternalError("Unable to initialize temp directory = " + applicationTemp, e );
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                ApplicationUtils.removeDir(applicationTemp);
            }
        }, "TempFilesRemover"));
    }

    public static TempFileManager getDefaultManager()
    {
        return instance;
    }

    public static synchronized TempFileManager getManager(File tmpRoot)
    {
        String key = tmpRoot.getAbsolutePath();
        TempFileManager result = managers.get(key);
        if(result == null)
        {
            result = new TempFileManager(tmpRoot);

            try
            {
                java.lang.SecurityManager securityManager = System.getSecurityManager();
                if(securityManager != null)
                    securityManager.checkWrite( key );
            }
            catch( SecurityException e )
            {
                BiosoftSecurityManager.addAllowedReadPath(key);
            }

            managers.put(key, result);
        }
        return result;
    }

    public @Nonnull File getTempDirectory()
    {
        String session = SecurityManager.getSession();
        File file = new File(applicationTemp, session);
        if(!file.isDirectory())
            file.mkdirs();
        if(!file.isDirectory())
        {
            throw ExceptionRegistry.translateException( new FileNotFoundException( file.getAbsolutePath() ) );
        }
        return file;
    }

    public @Nonnull File getResourcesTempDirectory()
    {
        File file = new File(applicationTemp, "resources");
        if(!file.isDirectory())
            file.mkdirs();
        if(!file.isDirectory())
        {
            throw ExceptionRegistry.translateException( new FileNotFoundException( file.getAbsolutePath() ) );
        }
        return file;
    }

    protected @Nonnull TempFile getTempFile(String suffix)
    {
        String fileName = String.format(Locale.ENGLISH, "%09d", fileNum.incrementAndGet());
        if(suffix.startsWith("."))
            fileName+=suffix;
        else
            fileName+="_"+suffix;
        TempFile tempFile = new TempFile(getTempDirectory(), fileName);
        return tempFile;
    }

    /**
     * Creates empty temporary file and returns it
     * @param suffix - suffix to add to temporary file
     * @return File
     * @throws IOException
     */
    public @Nonnull TempFile file(String suffix) throws IOException
    {
        TempFile tempFile = getTempFile(suffix);
        tempFile.createNewFile();
        return tempFile;
    }

    /**
     * Generates path to temp file or directory but doesn't create it. However it's guaranteed that path will be unique.
     * @param suffix - suffix to add to temporary file
     * @return File path
     * @throws IOException
     */
    public @Nonnull File path(String suffix)
    {
        return getTempFile(suffix);
    }

    /**
     * Creates temporary file with given content and returns it
     * @param suffix - suffix to add to temporary file
     * @param content - file content
     * @return File
     * @throws IOException
     */
    public @Nonnull TempFile file(String suffix, String content) throws IOException
    {
        TempFile tempFile = file(suffix);
        try
        {
            ApplicationUtils.writeString( tempFile, content );
        }
        catch( IOException e )
        {
            tempFile.delete();
            throw e;
        }
        return tempFile;
    }

    /**
     * Creates temporary file, fills it from the stream and returns it
     * @param suffix - suffix to add to temporary file
     * @param stream - stream to fill from (note: stream will be closed automatically)
     * @return File
     * @throws IOException
     */
    public @Nonnull TempFile file(String suffix, InputStream stream) throws IOException
    {
        TempFile tempFile = file(suffix);
        try
        {
            ApplicationUtils.copyStream(new FileOutputStream(tempFile), stream);
        }
        catch( IOException e )
        {
            tempFile.delete();
            throw e;
        }
        return tempFile;
    }

    /**
     * Creates temporary directory and returns it
     * @param suffix - suffix to add to temporary directory
     * @return File
     * @throws IOException
     */
    public @Nonnull File dir(String suffix) throws IOException
    {
        File tempFile = getTempFile(suffix);
        if(!tempFile.mkdirs())
            throw new IOException("Unable to create "+tempFile);
        return tempFile;
    }
}
