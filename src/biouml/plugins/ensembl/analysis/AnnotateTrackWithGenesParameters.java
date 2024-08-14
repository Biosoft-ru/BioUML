package biouml.plugins.ensembl.analysis;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

/**
 * @author lan
 *
 */
public class AnnotateTrackWithGenesParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputTrack, outputTrack;
    private Species species;
    private Integer from, to;
    
    public AnnotateTrackWithGenesParameters()
    {
        from = -1000;
        to = 1000;
        setSpecies(Species.getDefaultSpecies(null));
    }

    public DataElementPath getInputTrack()
    {
        return inputTrack;
    }

    public void setInputTrack(DataElementPath inputTrack)
    {
        Object oldValue = this.inputTrack;
        this.inputTrack = inputTrack;
        firePropertyChange("inputTrack", oldValue, inputTrack);
    }

    public DataElementPath getOutputTrack()
    {
        return outputTrack;
    }

    public void setOutputTrack(DataElementPath outputTrack)
    {
        Object oldValue = this.outputTrack;
        this.outputTrack = outputTrack;
        firePropertyChange("outputTrack", oldValue, outputTrack);
    }

    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange("species", oldValue, species);
    }

    public Integer getFrom()
    {
        return from;
    }

    public void setFrom(Integer from)
    {
        Object oldValue = this.from;
        this.from = from;
        firePropertyChange("from", oldValue, from);
    }

    public Integer getTo()
    {
        return to;
    }

    public void setTo(Integer to)
    {
        Object oldValue = this.to;
        this.to = to;
        firePropertyChange("to", oldValue, to);
    }

}
