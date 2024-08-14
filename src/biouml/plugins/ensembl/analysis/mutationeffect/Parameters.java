package biouml.plugins.ensembl.analysis.mutationeffect;

import java.util.Objects;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Track;

public class Parameters extends AbstractAnalysisParameters
{
    private DataElementPath inputTrack, outputTrack;
    private BasicGenomeSelector genome;

    public Parameters()
    {
        setGenome( new BasicGenomeSelector() );
    }
    
    public DataElementPath getInputTrack()
    {
        return inputTrack;
    }

    public void setInputTrack(DataElementPath inputTrack)
    {
        Object oldValue = this.inputTrack;
        this.inputTrack = inputTrack;
        if( !Objects.equals( inputTrack, oldValue ) )
        {
            Track track = inputTrack.optDataElement( Track.class );
            if(track != null)
                genome.setFromTrack( track );
        }
        firePropertyChange( "inputTrack", oldValue, inputTrack );
    }
    
    public BasicGenomeSelector getGenome()
    {
        return genome;
    }

    public void setGenome(BasicGenomeSelector genome)
    {
        Object oldValue = this.genome;
        this.genome = genome;
        genome.setParent( this );
        firePropertyChange( "genome", oldValue, genome );
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
}
