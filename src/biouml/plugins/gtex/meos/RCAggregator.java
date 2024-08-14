package biouml.plugins.gtex.meos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.bsa.analysis.maos.IResultHandler;
import ru.biosoft.bsa.analysis.maos.SiteMutation;

public class RCAggregator implements IResultHandler
{
    private IResultHandler parent;
    
    public RCAggregator(IResultHandler parent)
    {
        this.parent = parent;
    }
    
    @Override
    public void init()
    {
    }

    private Map<String, List<SiteMutation>> mutationsByType = new HashMap<>(); 
    @Override
    public void siteMutationEvent(SiteMutation siteMutation)
    {
        GTEXSiteMutation g = (GTEXSiteMutation)siteMutation;
        mutationsByType.computeIfAbsent( g.type, k->new ArrayList<>() ).add( g );
    }

    @Override
    public void finish()
    {
        for(String type : mutationsByType.keySet())
        {
            List<SiteMutation> list = mutationsByType.get( type );
            if(list.isEmpty())
                continue;
            if(list.size() == 1)
            {
                parent.siteMutationEvent( list.get( 0 ) );
                continue;
            }
            if(list.size() > 2)
                throw new IllegalArgumentException();
            SiteMutation m1 = list.get( 0 );
            SiteMutation m2 = list.get( 1 );
            SiteMutation bestM;
            if(type.equals( "BEST_REF" ))
                bestM = m1.refScore >= m2.refScore ? m1 : m2;
            else if(type.equals( "BEST_ALT" ))
                bestM = m1.altScore >= m2.altScore ? m1 : m2;
            else if(type.startsWith( "MAX_CHANGE" ))
                bestM = Math.abs( m1.pValueLogFC ) >= Math.abs( m2.pValueLogFC ) ? m1 : m2;
            else
                throw new IllegalArgumentException();
             
            parent.siteMutationEvent( bestM );
        }
    }

    @Override
    public Object[] getResults()
    {
        throw new UnsupportedOperationException();
    }

}
