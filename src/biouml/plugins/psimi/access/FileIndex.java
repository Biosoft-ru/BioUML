package biouml.plugins.psimi.access;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;

/**
 * @todo Document
 */
public class FileIndex extends HashMap<String, String> implements Index<String>
{
    private String indexName;

    /**
     * @todo Document.
     * @todo HIGH Implement.
     */
    public FileIndex(File dataFile, String indexName) throws IOException
    {
        if( dataFile == null )
            throw new IllegalArgumentException("dataFile not specified.");

        if( indexName == null )
            indexName = DEFAULT_INDEX_NAME;
        this.indexName = indexName;

        indexFileName = new File(dataFile.getPath() + "." + indexName);
        if( indexFileName.exists() )
        {
            if( !indexFileName.canRead() )
                throw new IOException("Can not read file " + indexFileName);
            if( !indexFileName.canWrite() )
                throw new IOException("Can not write file " + indexFileName);

            // read indexes
            readIndexFile();
        }
    }

    @Override
    public String getName()
    {
        return indexName;
    }

    @Override
    public Iterator nodeIterator(Key key)
    {
        throw new java.lang.UnsupportedOperationException("Method nodeIterator() not yet implemented.");
    }

    /**
     * @todo Document.
     * @todo Implement
     * @todo May compare timestamp of file and index file.
     */
    @Override
    public boolean isValid()
    {
        return valid;
    }

    /**
     * Returns index file. This information is essential to remove index files.
     *
     * @return index file.
     */
    @Override
    public File getIndexFile()
    {
        return indexFileName;
    }

    /**
     * @todo Document
     * @todo HIGH Uncomment when close will be work correctly.
     */
    @Override
    public void close() throws FileNotFoundException, IOException
    {
        try (PrintWriter file = new PrintWriter( new FileOutputStream( indexFileName ) ))
        {
            for( Entry<String, String> entry : entrySet() )
            {
                String key = entry.getKey();
                String idx = entry.getValue();
                file.write( key + "\n" );
                file.write( idx + "\n" );
            }
            file.flush();
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private

    /** Name of index file */
    private File indexFileName = null;

    /** @todo Document */
    private boolean valid = false;

    /**
     * @todo Document
     */
    private void readIndexFile() throws FileNotFoundException, IOException
    {
        try(BufferedReader file = ApplicationUtils.utfReader( indexFileName ))
        {
            String key = file.readLine();
            while( key != null && key.length() > 0 )
            {
                String value = file.readLine();
                put(key, value);
                
                key = file.readLine();
            }
            valid = true;
        }
    }
}