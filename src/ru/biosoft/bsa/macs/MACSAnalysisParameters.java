package ru.biosoft.bsa.macs;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.Track;

@SuppressWarnings ( "serial" )
public class MACSAnalysisParameters extends AbstractAnalysisParameters
{
    protected DataElementPath trackPath, controlPath, outputPath;
    //MACS algorithm parameters
    protected boolean nolambda = false;
    protected int bw = 300;
    protected int shiftsize = 100;
    protected boolean nomodel = false;
    protected double gsize = 2.7e+9;
    protected int mfold = 32;
    protected int tsize = 25;
    protected double pvalue = 1e-5;
    protected boolean futureFDR = false;
    protected MACSLambdaSet lambdaSet;
    
    public MACSAnalysisParameters()
    {
        lambdaSet = new MACSLambdaSet(this, new int[]{1000,5000,10000});
        controlPath = null;
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
    public Integer getMfold()
    {
        return mfold;
    }
    public void setMfold(Integer mfold)
    {
        Object oldValue = this.mfold;
        this.mfold = mfold;
        firePropertyChange("mfold", oldValue, mfold);
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
    public Boolean isFutureFDR()
    {
        return futureFDR;
    }
    public void setFutureFDR(Boolean futureFDR)
    {
        Object oldValue = this.futureFDR;
        this.futureFDR = futureFDR;
        firePropertyChange("futureFDR", oldValue, this.futureFDR);
    }
    
    public MACSLambdaSet getLambdaSet()
    {
        return lambdaSet;
    }
    
    public void setLambdaSet(MACSLambdaSet lambdaSet)
    {
        Object oldValue = this.lambdaSet;
        this.lambdaSet = lambdaSet;
        firePropertyChange("lambdaSet", oldValue, this.lambdaSet);
    }
    
    public void setLambdaSetArray(int[] lambdaSet2)
    {
        setLambdaSet(new MACSLambdaSet(this, lambdaSet2));
    }
    
    public int[] getLambdaSetArray()
    {
        return lambdaSet.getLambdaSet();
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
    }
}
