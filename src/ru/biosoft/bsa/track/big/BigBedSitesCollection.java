package ru.biosoft.bsa.track.big;

import java.io.IOException;
import java.util.AbstractList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.zip.DataFormatException;

import org.jetbrains.bio.big.BedEntry;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.bsa.Site;

public class BigBedSitesCollection extends AbstractDataCollection<Site>
{
    private BigBedTrack<?> bbTrack;
    private BigBedRandomAccess randomAccess;

    public BigBedSitesCollection(BigBedTrack<?> bbTrack) throws IOException, DataFormatException
    {
        super( null, new Properties() );
        this.bbTrack = bbTrack;
        randomAccess = new BigBedRandomAccess( bbTrack.getBBFile() );
        if(bbTrack.siteCount != randomAccess.getSiteCount())
        {
            //Maybe we are wrong in randomAccess, that rely on itemsPerSlot
            //But most probably we have a bigBed file produces by original jetbrains library that didn't write siteCount to bigBed
            log.warning( "BigBed site count field didn't match number of indexed sites (" + bbTrack.siteCount + "!=" + randomAccess.getSiteCount() + ")" );
            bbTrack.siteCount = randomAccess.getSiteCount();
        }
    }

    @Override
    public List<String> getNameList()
    {
        return new AbstractList<String>()
        {
            @Override
            public String get(int index)
            {
               return String.valueOf(index);
            }

            @Override
            public int size()
            {
                if(randomAccess.getSiteCount() > Integer.MAX_VALUE-100)
                    return Integer.MAX_VALUE-100;
                return (int)randomAccess.getSiteCount();
            }
        };
    }
    
    
    @Override
    protected Site doGet(String name) throws Exception
    {
        int idx;
        try {
            idx = Integer.parseInt( name );
        } catch(NumberFormatException e)
        {
            throw new NoSuchElementException( name );
        }
        List<BedEntry> bedList = randomAccess.fetch( idx, 1 );
        if(bedList.isEmpty())
            throw new NoSuchElementException(name);
        List<?> objList = bbTrack.fromBedList( bedList );
        Object obj = objList.get( 0 );
        Site site = bbTrack.transformToSite( obj );
        return new SiteWrapper( site, name );
    }
}
