package biouml.workbench.graphsearch;

import ru.biosoft.access.biohub.BioHub;

/**
 * Options for current search query engine
 */
public class QueryOptions
{
     
    /**
     * Default depth
     */
    public static final int DEFAULT_DEPTH = 1;

    private int depth;

    private int direction;
    
    /**
     * Create query options with default depth and direction
     */
    public QueryOptions ( )
    {
        this ( DEFAULT_DEPTH, BioHub.DIRECTION_DOWN );
    }

    public QueryOptions ( int depth, int direction )
    {
        this.depth = depth;
        this.direction = direction;
    }

    public int getDepth ( )
    {
        return depth;
    }

    public void setDepth ( int depth )
    {
        this.depth = depth;
    }

    public int getDirection ( )
    {
        return direction;
    }

    public void setDirection ( int direction )
    {
        this.direction = direction;
    }
    
}
