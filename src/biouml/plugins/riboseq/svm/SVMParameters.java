package biouml.plugins.riboseq.svm;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class SVMParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputYesTrack, inputNoTrack, inputUndefinedTrack;
    private DataElementPath outputClassifiedYesTrack;

    @PropertyName ( "Input Yes Track" )
    @PropertyDescription ( "sample track with only Yes clusters" )
    public DataElementPath getInputYesTrack()
    {
        return inputYesTrack;
    }

    public void setInputYesTrack(DataElementPath inputYesTrack)
    {
        Object oldValue = this.inputYesTrack;
        this.inputYesTrack = inputYesTrack;
        firePropertyChange( "inputYesTrack", oldValue, this.inputYesTrack );
    }

    @PropertyName ( "Input No Track" )
    @PropertyDescription ( "sample track with only No clusters" )
    public DataElementPath getInputNoTrack()
    {
        return inputNoTrack;
    }

    public void setInputNoTrack(DataElementPath inputNoTrack)
    {
        Object oldValue = this.inputNoTrack;
        this.inputNoTrack = inputNoTrack;
        firePropertyChange( "inputNoTrack", oldValue, this.inputNoTrack );
    }

    @PropertyName ( "Input Undefined Track" )
    @PropertyDescription ( "input track with unclassified clusters" )
    public DataElementPath getInputUndefinedTrack()
    {
        return inputUndefinedTrack;
    }

    public void setInputUndefinedTrack(DataElementPath inputUndefinedTrack)
    {
        Object oldValue = this.inputUndefinedTrack;
        this.inputUndefinedTrack = inputUndefinedTrack;
        firePropertyChange( "inputUndefinedTrack", oldValue, this.inputUndefinedTrack );
    }

    @PropertyName ( "Output classified Track" )
    @PropertyDescription ( "track with classified as Yes clusters from Undefined input Track" )
    public DataElementPath getOutputClassifiedYesTrack()
    {
        return outputClassifiedYesTrack;
    }

    public void setOutputClassifiedYesTrack(DataElementPath outputClassifiedYesTrack)
    {
        Object oldValue = this.outputClassifiedYesTrack;
        this.outputClassifiedYesTrack = outputClassifiedYesTrack;
        firePropertyChange( "outputClassifiedYesTrack", oldValue, this.outputClassifiedYesTrack );
    }
}
