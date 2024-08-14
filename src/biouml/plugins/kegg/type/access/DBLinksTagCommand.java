package biouml.plugins.kegg.type.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import ru.biosoft.access.support.TagCommand;
import ru.biosoft.access.support.TagEntryTransformer;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Referrer;

public class DBLinksTagCommand implements TagCommand
{
    private static final char DB_NAME_DELIM = ':';
    private static final String DB_KEY_DELIM = " ";
    private static final String LN = System.getProperty("line.separator");

    private final String tag;
    private Map<String, Collection<String>> dbReferences;
    private final TagEntryTransformer<? extends Referrer> transformer;
    private String dbName;

    public DBLinksTagCommand(String tag, TagEntryTransformer<? extends Referrer> transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void start(String tag)
    {
        dbReferences = new LinkedHashMap<>();
        dbName = null;
    }

    @Override
    public void addValue(String value)
    {

        int pos = value.indexOf(DB_NAME_DELIM);
        if( pos > 0 )
        {
            dbName = value.substring(0, pos).trim();
            value = value.substring(pos + 1).trim();
        }
        dbReferences.computeIfAbsent( dbName, k -> new ArrayList<>() ).addAll( parseKeys(value) );
    }

    private Collection<String> parseKeys(String keyStr)
    {
        return StreamEx.split(keyStr, DB_KEY_DELIM).toList();
    }

    @Override
    public void complete(String tag)
    {
        Referrer referrer = transformer.getProcessedObject();

        DatabaseReference[] refs = EntryStream.of( dbReferences ).flatMapValues( Collection::stream ).mapKeyValue( DatabaseReference::new )
                .toArray( DatabaseReference[]::new );
        referrer.setDatabaseReferences(refs);
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        Referrer referrer = transformer.getProcessedObject();
        DatabaseReference[] refs = referrer.getDatabaseReferences();
        Map<String, String> dbRefs = StreamEx.of(refs)
            .groupingBy(DatabaseReference::getDatabaseName, Collectors.mapping(DatabaseReference::getId, Collectors.joining(DB_KEY_DELIM)));

        StringBuilder value = new StringBuilder();
        value.append(tag);
        for(Entry<String, String> entry : dbRefs.entrySet())
        {
            String key = entry.getKey();
            value.append('\t').append(key).append(DB_NAME_DELIM).append(DB_KEY_DELIM);
            value.append(entry.getValue()).append(LN);
        }
        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}
