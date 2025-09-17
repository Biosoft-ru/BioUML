package ru.biosoft.access.generic;

import java.io.BufferedReader;
import java.util.Properties;

import org.apache.commons.text.StringEscapeUtils;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.Entry;

/**
 * Entry transformer for DataElementInfo
 */
public class DataElementInfoTransformer extends AbstractTransformer<Entry, DataElementInfo>
{
    protected static final String endl = System.getProperty("line.separator");

    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<DataElementInfo> getOutputType()
    {
        return DataElementInfo.class;
    }

    @Override
    public DataElementInfo transformInput(Entry input) throws Exception
    {
        Properties properties = new Properties();
        try( BufferedReader br = new BufferedReader( input.getReader() ) )
        {
            String line;
            while( ( line = br.readLine() ) != null )
            {
                int pos = line.indexOf( '\t' );
                if( pos > 0 )
                {
                    String key = line.substring( 0, pos ).trim();
                    if( key.equals( "DESCRIPTION" ) )
                    {
                        key = DataCollectionConfigConstants.DESCRIPTION_PROPERTY;
                    }
                    if( !key.equals( "ID" ) )
                    {
                        properties.put( key, loadConvert( line.substring( pos + 1 ) ) );
                    }
                }
            }
        }
        DataElementInfo dei = new DataElementInfo(input.getName(), getTransformedCollection(), properties);
        return dei;
    }
    @Override
    public Entry transformOutput(DataElementInfo dei) throws Exception
    {
        StringBuffer result = new StringBuffer();
        result.append("ID");
        result.append('\t');
        result.append(dei.getName());
        result.append(endl);
        for( Object key : dei.getProperties().keySet() )
        {
            String value = dei.getProperty((String)key);
            if(value == null) continue;
            result.append(key);
            result.append('\t');
            result.append(saveConvert(value));
            result.append(endl);
        }
        result.append("//");
        result.append(endl);
        return new Entry(dei.getOrigin(), dei.getName(), result.toString());
    }

    private String loadConvert(String value)
    {
        // Compatibility for GSEA results in pre-0.9.6 code
        if(value.contains("'{\\\"yaxis\\\":2,\\\"color\\\""))
            return value;
        // Compatibility for paths on Windows in pre-0.9.6 code
        if(value.startsWith("C:\\BioUML"))
            return value;
        return StringEscapeUtils.unescapeJava(value);
    }

    private String saveConvert(String value)
    {
        return StringEscapeUtils.escapeJava(value);
    }
}
