package biouml.plugins.keynodes._test;

import java.util.HashMap;
import java.util.Map;

import one.util.streamex.EntryStream;

import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;

public class DijkstraSearch
{
    private int direction;
    private final BioHub biohub;
    private final Map<String, Integer> numSources;

    private int maxRadius;
    private final TargetOptions dbOptions;
    private final String[] relTypes;

    public DijkstraSearch(BioHub hub, TargetOptions options, String[] relTypes)
    {
        biohub = hub;
        dbOptions = options;
        maxRadius = 0;
        numSources = new HashMap<>();
        this.relTypes = relTypes;
    }

    public void setDirection(int direction)
    {
        this.direction = direction;
    }

    public void setMaxRadius(int radius)
    {
        maxRadius = radius;
    }

    public void shortestPath(Element element)
    {
        Element[] nodes = biohub.getReference(element, dbOptions, relTypes, maxRadius, direction);
        if( nodes != null )
        {
            for( Element node : nodes )
            {
                numSources.compute( node.getAccession(), (k, v) -> v == null ? 1 : v + 1 );
            }
        }
    }

    public Map<String, Integer> getResult()
    {
        return numSources;
    }

    public Map<String, Integer> getResult(int minHits)
    {
        return EntryStream.of(numSources).filterValues( val -> val >= minHits ).toMap();
    }

}
