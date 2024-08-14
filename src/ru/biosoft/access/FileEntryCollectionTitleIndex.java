package ru.biosoft.access;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DerivedDataCollection;
import biouml.standard.type.access.TitleIndex;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * Fast index for collection based on FileEntryCollection2
 * TODO: properly support edited/deleted entries
 * @author lan
 */
@SuppressWarnings ( "serial" )
public class FileEntryCollectionTitleIndex extends TitleIndex
{
    public static final String INDEX_TITLE_PREFIX = "index.title.prefix";
    
    private final String titleStart;
    
    /**
     * @param dc
     * @param indexName
     * @throws Exception
     */
    public FileEntryCollectionTitleIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        super(dc, indexName);
        titleStart = dc.getInfo().getProperty(INDEX_TITLE_PREFIX);
    }
    
    private FileEntryCollection2 getFileEntryCollection()
    {
        DataCollection<?> fec = dc;
        while(!(fec instanceof FileEntryCollection2))
            fec = ((DerivedDataCollection)fec).getPrimaryCollection();
        return (FileEntryCollection2)fec;
    }

    @Override
    protected void doInit() throws Exception
    {
        FileEntryCollection2 dc = getFileEntryCollection();
        List<String> strings = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        try(BufferedReader br = ApplicationUtils.utfReader( dc.getFile() ))
        {
            String key = null;
            while(br.ready())
            {
                String line = br.readLine();
                if(line == null)
                    break;
                if(line.startsWith(dc.getStartKey()))
                {
                    key = dc.extractKey(line);
                } else if(key != null && line.startsWith(titleStart))
                {
                    strings.add(key);
                    String title = line.substring(titleStart.length()).trim();
                    if(titles.contains(title)) title = getCompositeName(title, key);
                    strings.add(title);
                    titles.add(title);
                    key = null;
                }
            }
        }
        for(List<String> slide : StreamEx.ofSubLists( strings, 2 ))
        {
            id2title.put(slide.get(0), slide.get(1));
            title2id.put(slide.get(1), slide.get(0));
        }
    }
}
