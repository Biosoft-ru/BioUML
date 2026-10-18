package ru.biosoft.bsa.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
public class FilterVCFAnalysisParameters extends AbstractAnalysisParameters
{
    public static final String MODE_HETEROZYGOTE = "heterozygote";
    public static final String MODE_HOMOZYGOTE = "homozygote";
    public static final String MODE_BOTH = "both";

    static final String[] MODES = {MODE_HETEROZYGOTE, MODE_HOMOZYGOTE, MODE_BOTH};

    private DataElementPath inputTrack, outputPath;
    private String mode = MODES[0];

    @PropertyName ( "Input track" )
    @PropertyDescription ( "Track which you want to filter" )
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

    @PropertyName ( "Output folder" )
    @PropertyDescription ( "Specify the location where to store results" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange( "outputPath", oldValue, outputPath );
    }


    @PropertyName ( "Filtering mode" )
    @PropertyDescription ( "Which variant calls to keep: heterozygote (0/1, 1/2), homozygote (1/1, haploid 1) or both. "
            + "Homozygous reference (0/0) and missing calls are dropped in every mode" )
    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        Object oldValue = this.mode;
        this.mode = mode;
        firePropertyChange( "mode", oldValue, mode );
    }
}
