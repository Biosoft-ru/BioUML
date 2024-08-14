package ru.biosoft.bsa.analysis.trackutil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public abstract class TrackPropertiesMultiSelector extends GenericMultiSelectEditor
{
    DataElementPath lastPath;
    DataElementPath lastSkipPath;
    String[] lastResult;

    protected abstract DataElementPath getTrackPath();
    protected abstract DataElementPath getSkipTrackPath();

    @Override
    protected Object[] getAvailableValues()
    {
        DataElementPath path = null;
        DataElementPath skipPath = null;
        try
        {
            path = getTrackPath();
            skipPath = getSkipTrackPath();
            boolean eqSkip = ( skipPath == null && lastSkipPath == null ) || ( skipPath != null && skipPath.equals( lastSkipPath ) )
                    || ( lastSkipPath != null && lastSkipPath.equals( skipPath ) );
            if( lastPath != null && path != null && path.equals( lastPath ) && eqSkip )
                return lastResult;
        }
        catch( Exception e )
        {
            return new String[0];
        }
        List<String> result = new ArrayList<>();
        try
        {
            if( path != null )
            {
                DataCollection<Site> allSites = path.getDataElement( Track.class ).getAllSites();
                Iterator<String> nameIterator = allSites.iterator().next().getProperties().nameIterator();
                while( nameIterator.hasNext() )
                {
                    result.add( nameIterator.next() );
                }
                if( skipPath != null )
                {
                    lastSkipPath = skipPath;
                    allSites = skipPath.getDataElement( Track.class ).getAllSites();
                    nameIterator = allSites.iterator().next().getProperties().nameIterator();
                    while( nameIterator.hasNext() )
                    {
                        result.remove( nameIterator.next() );
                    }
                }
            }
        }
        catch( Exception e )
        {
        }
        lastPath = path;
        lastResult = result.toArray( new String[result.size()] );
        return lastResult;
    }
}
