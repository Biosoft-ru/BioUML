package biouml.plugins.gtrd.master.index;

import java.util.List;

import ru.biosoft.bsa.Site;
import ru.biosoft.rtree.IntervalArray;

public class ListOfSitesWrapper implements IntervalArray
{
    private List<? extends Site> sites;
    public ListOfSitesWrapper(List<? extends Site> sites)
    {
        this.sites = sites;
    }

    @Override
    public int getFrom(int i)
    {
        return sites.get(i).getFrom();
    }

    @Override
    public int getTo(int i)
    {
        return sites.get( i ).getTo();
    }

    @Override
    public int size()
    {
        return sites.size();
    }

}
