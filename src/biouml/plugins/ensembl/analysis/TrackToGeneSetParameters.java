package biouml.plugins.ensembl.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import biouml.standard.type.Species;

public class TrackToGeneSetParameters extends AbstractAnalysisParameters
{
    protected Integer from, to;
    protected SiteAggregator[] resultTypes = new SiteAggregator[] {new CountSiteAggregator()};
    protected DataElementPath destPath;
    protected DataElementPathSet sourcePaths;
    protected Species species;

    public TrackToGeneSetParameters()
    {
        from = -1000;
        to = 1000;
        setSpecies(Species.getDefaultSpecies(null));
    }

    public Integer getFrom()
    {
        return from;
    }

    public void setFrom(Integer from)
    {
        Integer oldValue = this.from;
        this.from = from;
        firePropertyChange("from", oldValue, from);
    }

    public Integer getTo()
    {
        return to;
    }

    public void setTo(Integer to)
    {
        Integer oldValue = this.to;
        this.to = to;
        firePropertyChange("to", oldValue, to);
    }

    public DataElementPath getDestPath()
    {
        return destPath;
    }

    public void setDestPath(DataElementPath destPath)
    {
        Object oldValue = this.destPath;
        this.destPath = destPath;
        firePropertyChange("destPath", oldValue, this.destPath);
    }

    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, this.species);
    }

    public DataElementPathSet getSourcePaths()
    {
        return sourcePaths;
    }

    public void setSourcePaths(DataElementPathSet sourcePaths)
    {
        Object oldValue = this.sourcePaths;
        this.sourcePaths = sourcePaths;
        firePropertyChange("sourcePaths", oldValue, sourcePaths);
    }

    public SiteAggregator[] getResultTypes()
    {
        return resultTypes;
    }

    public void setResultTypes(SiteAggregator[] resultTypes)
    {
        Object oldValue = this.resultTypes;
        this.resultTypes = resultTypes;
        firePropertyChange("resultTypes", oldValue, resultTypes);
    }


    private boolean allGenes = false;
    public boolean isAllGenes()
    {
        return allGenes;
    }
    public void setAllGenes(boolean allGenes)
    {
        boolean oldValue = this.allGenes;
        this.allGenes = allGenes;
        firePropertyChange( "allGenes", oldValue, allGenes );
    }
    
}
