package biouml.plugins.gtrd.master.sites.chipexo;

public class ChIPexoGEMPeak extends ChIPexoPeak
{
    public static final String PEAK_CALLER = "gem";
    
    protected float control, expected, fold, ip, ipVsEmp, noise, pMLog10, pPoiss, qMLog10;
    
    public float getControl()
    {
        return control;
    }

    public void setControl(float control)
    {
        this.control = control;
    }

    public float getExpected()
    {
        return expected;
    }

    public void setExpected(float expected)
    {
        this.expected = expected;
    }

    public float getFold()
    {
        return fold;
    }

    public void setFold(float fold)
    {
        this.fold = fold;
    }

    public float getIp()
    {
        return ip;
    }

    public void setIp(float ip)
    {
        this.ip = ip;
    }

    public float getIpVsEmp()
    {
        return ipVsEmp;
    }

    public void setIpVsEmp(float ipVsEmp)
    {
        this.ipVsEmp = ipVsEmp;
    }

    public float getNoise()
    {
        return noise;
    }

    public void setNoise(float noise)
    {
        this.noise = noise;
    }

    public float getPMLog10()
    {
        return pMLog10;
    }

    public void setPMLog10(float pMLog10)
    {
        this.pMLog10 = pMLog10;
    }

    public float getPPoiss()
    {
        return pPoiss;
    }

    public void setPPoiss(float pPoiss)
    {
        this.pPoiss = pPoiss;
    }

    public float getQMLog10()
    {
        return qMLog10;
    }

    public void setQMLog10(float qMLog10)
    {
        this.qMLog10 = qMLog10;
    }

    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._childsSize() + 9*4;//9 float fields
    }
}