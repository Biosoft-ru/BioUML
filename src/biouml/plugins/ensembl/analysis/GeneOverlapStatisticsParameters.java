package biouml.plugins.ensembl.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.standard.type.Species;

public class GeneOverlapStatisticsParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputTrack;
    private Species species = Species.getDefaultSpecies( null );
    private int fivePrimeFlankSize = 1000;
    private int threePrimeFlankSize = 1000;

    private DataElementPath outputFolder;
   

    @PropertyName("Input track")
    @PropertyDescription("Overlap this track with genes")
    public DataElementPath getInputTrack()
    {
        return inputTrack;
    }

    public void setInputTrack(DataElementPath inputTrack)
    {
        Object oldValue = this.inputTrack;
        this.inputTrack = inputTrack;
        firePropertyChange( "inputTrack", oldValue, inputTrack );
    }

    @PropertyName("Species")
    @PropertyDescription("Species")
    public Species getSpecies()
    {
        return species;
    }

    public void setSpecies(Species species)
    {
        Object oldValue = this.species;
        this.species = species;
        firePropertyChange( "species", oldValue, species );
    }

    @PropertyName("5' region size")
    @PropertyDescription("Size of 5' region (promoter)")
    public int getFivePrimeFlankSize()
    {
        return fivePrimeFlankSize;
    }

    public void setFivePrimeFlankSize(int fivePrimeFlankSize)
    {
        Object oldValue = this.fivePrimeFlankSize;
        this.fivePrimeFlankSize = fivePrimeFlankSize;
        firePropertyChange( "fivePrimeFlankSize", oldValue, fivePrimeFlankSize );
    }

    @PropertyName("3' region size")
    @PropertyDescription("Size of 3' region")
    public int getThreePrimeFlankSize()
    {
        return threePrimeFlankSize;
    }

    public void setThreePrimeFlankSize(int threePrimeFlankSize)
    {
        Object oldValue = this.threePrimeFlankSize;
        this.threePrimeFlankSize = threePrimeFlankSize;
        firePropertyChange( "threePrimeFlankSize", oldValue, threePrimeFlankSize );
    }

    @PropertyName( "Output folder" )
    @PropertyDescription( "Output folder" )
    public DataElementPath getOutputFolder()
    {
        return outputFolder;
    }

    public void setOutputFolder(DataElementPath outputFolder)
    {
        Object oldValue = this.outputFolder;
        this.outputFolder = outputFolder;
        firePropertyChange( "outputFolder", oldValue, outputFolder );
    }


}
