package ru.biosoft.bsa.finder;

import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

public abstract class TrackFinder
{
    protected DataElementPath databasePath;
    /**
     * All descendants should have public constructor with the same signature 
     */
    protected TrackFinder(DataElementPath databasePath)
    {
        this.databasePath = databasePath;
    }
    
    public DataElementPath getDatabasePath()
    {
        return databasePath;
    }
    
    public abstract List<ru.biosoft.access.core.DataElementPath> getSupportedGenomes();
    
    protected DataElementPath genome;
    public DataElementPath getGenome()
    {
        return genome;
    }
    public void setGenome(DataElementPath genome)
    {
        this.genome = genome;
    }
    
    public abstract DataCollection<? extends TrackSummary> findTracks();
}
