
package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.MapAsVectorWithLazyFTInit;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.util.TextUtil;

/**
 * Support class for sequence transformers that process Fearture Table
 * used in EMBL/GenBank/DDBJ for sequence features description.
 *
 * @pending high MAX_LENGTH is limit for the sequence length to be written.
 * This is due to that sequence is stored in memory using StringBuffer and only then is copied to disk.
 */
public abstract class SequenceWithFeatureTableTransformer extends SequenceTransformer
{
    protected String sequenceStartTag;
    protected static final int MAX_LENGTH = 10000000; // Maximum length of sequence to writed

    @Override
    protected Properties createMapProperties(Sequence seq, Entry entry) throws Exception
    {
        Properties properties = super.createMapProperties(seq, entry);
        properties.put(DataCollectionConfigConstants.CLASS_PROPERTY, "ru.biosoft.bsa.MapAsVectorWithLazyFTInit");
        properties.put(MapAsVectorWithLazyFTInit.FEATURE_TABLE_CONTENT, entry);

        return properties;
    }

    /**
     * Positionate sequence reader to the sequence beginning.
     * @return sequence start line number relative the entry beginning.
     */
    @Override
    protected int seekSequenceStartLine(BufferedReader seqReader) throws Exception
    {
        String line;
        int lineNumber = 1;
        while( ( line = seqReader.readLine() ) != null )
        {
            if( line.startsWith(sequenceStartTag) )
                break;

            lineNumber++;
        }

        return lineNumber;
    }

    protected void writeEntry(String fileName, String data) throws IOException
    {
        ApplicationUtils.writeString(new FileOutputStream(fileName), data);
    }

    public static String itoa(int j, int fieldSize)
    {
        String str = Integer.toString( j );
        int cnt = fieldSize - str.length();
        return TextUtil.whiteSpace( cnt ) + str;
    }
}
