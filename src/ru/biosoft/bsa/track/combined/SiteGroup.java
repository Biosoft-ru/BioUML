package ru.biosoft.bsa.track.combined;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;

public class SiteGroup extends SiteImpl
{
    private List<Site> sites;

    public SiteGroup(DataCollection<?> parent, String name, Sequence sequence, List<Site> sites, int to)
    {
        super( parent, name, 0, 0, StrandType.STRAND_PLUS, sequence );
        //DataCollection<?> parent, String name, int start, int length, int strand, Sequence sequence
        this.sites = sites;
        if( sites.size() > 0 )
        {
            setStart( sites.get( 0 ).getFrom() );
            setTo( to );
        }
    }

    public SiteGroup(DataCollection<?> parent, String name, Sequence sequence)
    {
        super( parent, name, 0, 0, StrandType.STRAND_PLUS, sequence );
        //DataCollection<?> parent, String name, int start, int length, int strand, Sequence sequence
        sites = new ArrayList<>();
    }

    public void addSite(Site site)
    {
        //TODO: increase start and length depending on added site
        sites.add( site );
    }

    public List<Site> getSites()
    {
        return sites;
    }

}
