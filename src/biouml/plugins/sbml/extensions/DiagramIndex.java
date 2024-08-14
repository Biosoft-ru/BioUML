package biouml.plugins.sbml.extensions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import ru.biosoft.access.core.Index;
import ru.biosoft.access.core.Key;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * @todo Document
 */
@SuppressWarnings ( "serial" )
public class DiagramIndex extends HashMap<String, String> implements Index<String>
{
    private String indexName;

    /**
     * @todo Document.
     * @todo HIGH Implement.
     */
    public DiagramIndex(File directory, String indexName) throws IOException
    {
        if( directory == null )
            throw new IllegalArgumentException("target directory not specified.");

        if( indexName == null )
            indexName = DEFAULT_INDEX_NAME;
        this.indexName = indexName;

        indexFile = new File(directory.getPath() + "/" + indexName);
        if( indexFile.exists() )
        {
            if( !indexFile.canRead() )
                throw new IOException("Can not read file " + indexFile.getName());
            if( !indexFile.canWrite() )
                throw new IOException("Can not write file " + indexFile.getName());

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
        return indexFile;
    }

    /**
     * @todo Document
     * @todo HIGH Uncomment when close will be work correctly.
     */
    @Override
    public void close() throws FileNotFoundException, IOException
    {
        try (PrintWriter file = new PrintWriter( new OutputStreamWriter( new FileOutputStream( indexFile ), StandardCharsets.UTF_8 ) ))
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

    /** Index file */
    private File indexFile = null;

    /** @todo Document */
    private boolean valid = false;

    /**
     * @todo Document
     */
    private void readIndexFile() throws IOException
    {
        try(BufferedReader file = ApplicationUtils.utfReader( indexFile ))
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