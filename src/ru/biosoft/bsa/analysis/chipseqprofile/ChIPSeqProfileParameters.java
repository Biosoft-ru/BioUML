package ru.biosoft.bsa.analysis.chipseqprofile;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class ChIPSeqProfileParameters extends AbstractAnalysisParameters
{
    private DataElementPath peakTrackPath, tagTrackPath, profileTrackPath;
    private int fragmentSize = 150;
    private double sigma = 75;
    private double errorRate = 0.3;

    public DataElementPath getPeakTrackPath()
    {
        return peakTrackPath;
    }

    public void setPeakTrackPath(DataElementPath peakTrackPath)
    {
        Object oldValue = this.peakTrackPath;
        this.peakTrackPath = peakTrackPath;
        firePropertyChange("peakTrackPath", oldValue, peakTrackPath);
    }

    public DataElementPath getTagTrackPath()
    {
        return tagTrackPath;
    }

    public void setTagTrackPath(DataElementPath tagTrackPath)
    {
        Object oldValue = this.tagTrackPath;
        this.tagTrackPath = tagTrackPath;
        firePropertyChange("tagTrackPath", oldValue, tagTrackPath);
    }

    public DataElementPath getProfileTrackPath()
    {
        return profileTrackPath;
    }

    public void setProfileTrackPath(DataElementPath profileTrackPath)
    {
        Object oldValue = this.profileTrackPath;
        this.profileTrackPath = profileTrackPath;
        firePropertyChange("profileTrackPath", oldValue, profileTrackPath);
    }

    public int getFragmentSize()
    {
        return fragmentSize;
    }

    public void setFragmentSize(int fragmentSize)
    {
        Object oldValue = this.fragmentSize;
        this.fragmentSize = fragmentSize;
        firePropertyChange("fragmentSize", oldValue, fragmentSize);
    }

    public double getSigma()
    {
        return sigma;
    }

    public void setSigma(double sigma)
    {
        Object oldValue = this.sigma;
        this.sigma = sigma;
        firePropertyChange("sigma", oldValue, sigma);
    }

    public double getErrorRate()
    {
        return errorRate;
    }

    public void setErrorRate(double errorRate)
    {
        Object oldValue = this.errorRate;
        this.errorRate = errorRate;
        firePropertyChange("errorRate", oldValue, errorRate);
    }

}
