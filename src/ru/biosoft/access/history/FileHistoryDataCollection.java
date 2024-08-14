package ru.biosoft.access.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;

/**
 * Implementation of {@link HistoryDataCollection} based on files
 */
public class FileHistoryDataCollection extends TransformedDataCollection implements HistoryDataCollection
{
    protected static final Logger log = Logger.getLogger(FileHistoryDataCollection.class.getName());

    public FileHistoryDataCollection(DataCollection parent, Properties properties) throws Exception
    {
        super(parent, properties);
    }

    @Override
    public String getNextID()
    {
        List<String> names = getNameList();
        if( names.size() == 0 )
            return "0";
        String previous = names.get(names.size() - 1);
        return Integer.toString(Integer.parseInt(previous) + 1);
    }

    @Override
    public int getNextVersion(DataElementPath elementPath)
    {
        //TODO: use indexes for this purpose
        try
        {
            List<String> names = getNameList();
            for( int i = names.size() - 1; i >= 0; i-- )
            {
                HistoryElement he = (HistoryElement)get(names.get(i));
                if( he.getDePath().equals(elementPath) )
                {
                    return he.getVersion() + 1;
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Get history element error", e);
        }
        return 0;
    }

    @Override
    public List<String> getHistoryElementNames(DataElementPath elementPath, int minVersion)
    {
        //TODO: use indexes for this purpose
        List<String> result = new ArrayList<>();
        try
        {
            List<String> names = getNameList();
            for( int i = names.size() - 1; i >= 0; i-- )
            {
                HistoryElement he = (HistoryElement)get(names.get(i));
                if( he.getDePath().equals(elementPath) )
                {
                    if( he.getVersion() < minVersion )
                    {
                        return result;
                    }
                    result.add(names.get(i));
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Get history element error", e);
        }
        return result;
    }
}
