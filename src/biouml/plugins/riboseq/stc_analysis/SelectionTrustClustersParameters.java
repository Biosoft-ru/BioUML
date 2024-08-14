package biouml.plugins.riboseq.stc_analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class SelectionTrustClustersParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputPath;
    private DataElementPath pathToHousekeepingGenes;
    private DataElementPath outputPathYesTrack, outputPathNoTrack, outputPathUndefinedTrack;

    @PropertyName ( "Input Path Clusters Track" )
    @PropertyDescription ( "input track with clusters" )
    public DataElementPath getInputPath()
    {
        return inputPath;
    }

    public void setInputPath(DataElementPath inputPath)
    {
        Object oldValue = this.inputPath;
        this.inputPath = inputPath;
        firePropertyChange( "inputPath", oldValue, this.inputPath );
    }

    @PropertyName ( "Housekeeping genes" )
    @PropertyDescription ( "Path to housekeeping genes table" )
    public DataElementPath getPathToHousekeepingGenes()
    {
        return pathToHousekeepingGenes;
    }

    public void setPathToHousekeepingGenes(DataElementPath pathToHousekeepingGenes)
    {
        Object oldValue = this.pathToHousekeepingGenes;
        this.pathToHousekeepingGenes = pathToHousekeepingGenes;
        firePropertyChange( "pathToHousekeepingGenes", oldValue, this.pathToHousekeepingGenes );
    }

    @PropertyName ( "Output Yes Track" )
    @PropertyDescription ( "result of analysis" )
    public DataElementPath getOutputPathYesTrack()
    {
        return outputPathYesTrack;
    }

    public void setOutputPathYesTrack(DataElementPath outputPathYesTrack)
    {
        Object oldValue = this.outputPathYesTrack;
        this.outputPathYesTrack = outputPathYesTrack;
        firePropertyChange( "outputPathYesTrack", oldValue, this.outputPathYesTrack );
    }

    @PropertyName ( "Output No Track" )
    @PropertyDescription ( "result of analysis" )
    public DataElementPath getOutputPathNoTrack()
    {
        return outputPathNoTrack;
    }

    public void setOutputPathNoTrack(DataElementPath outputPathNoTrack)
    {
        Object oldValue = this.outputPathNoTrack;
        this.outputPathNoTrack = outputPathNoTrack;
        firePropertyChange( "outputPathNoTrack", oldValue, this.outputPathNoTrack );
    }

    @PropertyName ( "Output Undefined Track" )
    @PropertyDescription ( "result of analysis" )
    public DataElementPath getOutputPathUndefinedTrack()
    {
        return outputPathUndefinedTrack;
    }

    public void setOutputPathUndefinedTrack(DataElementPath outputPathUndefinedTrack)
    {
        Object oldValue = this.outputPathUndefinedTrack;
        this.outputPathUndefinedTrack = outputPathUndefinedTrack;
        firePropertyChange( "outputPathUndefinedTrack", oldValue, this.outputPathUndefinedTrack );
    }
}
