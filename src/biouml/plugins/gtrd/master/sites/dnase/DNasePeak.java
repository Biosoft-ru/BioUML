package biouml.plugins.gtrd.master.sites.dnase;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.master.sites.Peak;

public abstract class DNasePeak extends Peak<DNaseExperiment>
{
    protected int replicate;

    @Override
    public String getStableId()
    {
        return "p." + exp.getName() + "_" + replicate +"." + getPeakCaller() + "." + id;//p.DEXP003098_1.macs2.10719
    }

    public int getReplicate()
    {
        return replicate;
    }

    public void setReplicate(int replicate)
    {
        this.replicate = replicate;
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 4;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(!super.equals( obj ))
            return false;
        DNasePeak other = (DNasePeak)obj;
        if( replicate != other.replicate )
            return false;
        return true;
    }
}