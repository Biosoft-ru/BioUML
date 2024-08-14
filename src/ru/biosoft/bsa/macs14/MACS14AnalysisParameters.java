package ru.biosoft.bsa.macs14;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.Track;

public class MACS14AnalysisParameters extends AbstractAnalysisParameters
{
    private static final long serialVersionUID = 1L;

    static final String[] KEEP_DUPLICATES_VALUES = {"auto", "all", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    protected DataElementPath trackPath, controlPath, outputPath;
    //MACS algorithm parameters
    protected boolean nolambda = false;
    protected int bw = 300;
    protected int shiftsize = 100;
    protected boolean nomodel = false;
    protected double gsize = 2.7e+9;
    protected int mfoldLower = 10;
    protected int mfoldUpper = 30;
    protected int sLocal = 1000;
    protected int lLocal = 10000;
    protected boolean autoOff = false;
    protected boolean toSmall = false;
    protected String keepDup = "auto";
    protected int tsize = 0;
    protected double pvalue = 1e-5;
    protected boolean computePeakProfile = true;

    public MACS14AnalysisParameters()
    {
    }

    public DataElementPath getTrackPath()
    {
        return trackPath;
    }
    public void setTrackPath(DataElementPath trackPath)
    {
        Object oldValue = this.trackPath;
        this.trackPath = trackPath;
        firePropertyChange("trackPath", oldValue, trackPath);
    }
    public Boolean getNolambda()
    {
        return nolambda;
    }
    public void setNolambda(Boolean nolambda)
    {
        Object oldValue = this.nolambda;
        this.nolambda = nolambda;
        firePropertyChange("nolambda", oldValue, nolambda);
    }
    public Integer getBw()
    {
        return bw;
    }
    public void setBw(Integer bw)
    {
        Object oldValue = this.bw;
        this.bw = bw;
        firePropertyChange("bw", oldValue, bw);
    }
    public Integer getShiftsize()
    {
        return shiftsize;
    }
    public void setShiftsize(Integer shiftsize)
    {
        Object oldValue = this.shiftsize;
        this.shiftsize = shiftsize;
        firePropertyChange("shiftsize", oldValue, shiftsize);
    }
    public Boolean getNomodel()
    {
        return nomodel;
    }
    public void setNomodel(Boolean nomodel)
    {
        Object oldValue = this.nomodel;
        this.nomodel = nomodel;
        firePropertyChange("nomodel", oldValue, nomodel);
    }
    public Double getGsize()
    {
        return gsize;
    }
    public void setGsize(Double gsize)
    {
        Object oldValue = this.gsize;
        this.gsize = gsize;
        firePropertyChange("gsize", oldValue, gsize);
    }
    public Integer getTsize()
    {
        return tsize;
    }
    public void setTsize(Integer tsize)
    {
        Object oldValue = this.tsize;
        this.tsize = tsize;
        firePropertyChange("tsize", oldValue, tsize);
    }

    public Track getTrack()
    {
        return trackPath == null ? null : (Track)trackPath.optDataElement();
    }

    public void setTrack(Track track)
    {
        setTrackPath(DataElementPath.create(track));
    }

    public Track getControlTrack()
    {
        return controlPath == null ? null : (Track)controlPath.optDataElement();
    }

    public void setControlTrack(Track track)
    {
        setControlPath(DataElementPath.create(track));
    }

    public Double getPvalue()
    {
        return pvalue;
    }

    public void setPvalue(Double pvalue)
    {
        Object oldValue = this.pvalue;
        this.pvalue = pvalue;
        firePropertyChange("pvalue", oldValue, pvalue);
    }
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, this.outputPath);
    }

    public DataElementPath getControlPath()
    {
        return controlPath;
    }

    public void setControlPath(DataElementPath controlPath)
    {
        Object oldValue = this.controlPath;
        this.controlPath = controlPath;
        firePropertyChange("controlPath", oldValue, this.controlPath);
        firePropertyChange("*", null, null);
    }

    /**
     * @return the mfoldLower
     */
    public int getMfoldLower()
    {
        return mfoldLower;
    }

    /**
     * @param mfoldLower the mfoldLower to set
     */
    public void setMfoldLower(int mfoldLower)
    {
        Object oldValue = this.mfoldLower;
        this.mfoldLower = mfoldLower;
        firePropertyChange("mfoldLower", oldValue, mfoldLower);
    }

    /**
     * @return the mfoldUpper
     */
    public int getMfoldUpper()
    {
        return mfoldUpper;
    }

    /**
     * @param mfoldUpper the mfoldUpper to set
     */
    public void setMfoldUpper(int mfoldUpper)
    {
        Object oldValue = this.mfoldUpper;
        this.mfoldUpper = mfoldUpper;
        firePropertyChange("mfoldUpper", oldValue, mfoldUpper);
    }

    /**
     * @return the sLocal
     */
    public int getSLocal()
    {
        return sLocal;
    }

    /**
     * @param sLocal the sLocal to set
     */
    public void setSLocal(int sLocal)
    {
        Object oldValue = this.sLocal;
        this.sLocal = sLocal;
        firePropertyChange("sLocal", oldValue, sLocal);
    }

    /**
     * @return the lLocal
     */
    public int getLLocal()
    {
        return lLocal;
    }

    /**
     * @param lLocal the lLocal to set
     */
    public void setLLocal(int lLocal)
    {
        Object oldValue = this.lLocal;
        this.lLocal = lLocal;
        firePropertyChange("lLocal", oldValue, lLocal);
    }

    /**
     * @return the autoOff
     */
    public boolean isAutoOff()
    {
        return autoOff;
    }

    /**
     * @param autoOff the autoOff to set
     */
    public void setAutoOff(boolean autoOff)
    {
        Object oldValue = this.autoOff;
        this.autoOff = autoOff;
        firePropertyChange("autoOff", oldValue, autoOff);
    }

    /**
     * @return the toSmall
     */
    public boolean isToSmall()
    {
        return toSmall;
    }

    /**
     * @param toSmall the toSmall to set
     */
    public void setToSmall(boolean toSmall)
    {
        Object oldValue = this.toSmall;
        this.toSmall = toSmall;
        firePropertyChange("toSmall", oldValue, toSmall);
    }

    /**
     * @return the keepDup
     */
    public String getKeepDup()
    {
        return keepDup;
    }

    /**
     * @param keepDup the keepDup to set
     */
    public void setKeepDup(String keepDup)
    {
        Object oldValue = this.keepDup;
        this.keepDup = keepDup;
        firePropertyChange("keepDup", oldValue, keepDup);
    }

    public boolean isNoControl()
    {
        return controlPath == null || controlPath.isEmpty();
    }
    
    public boolean isComputePeakProfile()
    {
        return computePeakProfile;
    }
    
    public void setComputePeakProfile(boolean computePeakProfile)
    {
        boolean oldValue = this.computePeakProfile;
        this.computePeakProfile = computePeakProfile;
        firePropertyChange("computePeakProfile", oldValue, computePeakProfile);
    }
}
