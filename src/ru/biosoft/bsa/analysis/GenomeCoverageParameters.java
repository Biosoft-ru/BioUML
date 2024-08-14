package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class GenomeCoverageParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputTrack;
    private DataElementPath regionsTrack;
    private DataElementPath outputTrack;

    private int siteLength = -1;

    @PropertyName ( "Input track" )
    @PropertyDescription ( "Compute coverage of this track" )
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

    @PropertyName ( "Regions" )
    @PropertyDescription ( "Compute coverage only inside these regions" )
    public DataElementPath getRegionsTrack()
    {
        return regionsTrack;
    }

    public void setRegionsTrack(DataElementPath regionsTrack)
    {
        Object oldValue = this.regionsTrack;
        this.regionsTrack = regionsTrack;
        firePropertyChange("regionsTrack", oldValue, regionsTrack);
    }

    @PropertyName ( "Output track" )
    @PropertyDescription ( "Where to store result" )
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

    @PropertyName ( "Site length" )
    @PropertyDescription ( "Set the length of sites from input track (-1 for real site length)" )
    public int getSiteLength()
    {
        return siteLength;
    }

    public void setSiteLength(int siteLength)
    {
        Object oldValue = this.siteLength;
        this.siteLength = siteLength;
        firePropertyChange("siteLength", oldValue, siteLength);
    }

}
