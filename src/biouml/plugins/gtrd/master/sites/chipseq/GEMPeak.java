package biouml.plugins.gtrd.master.sites.chipseq;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.util.bean.StaticDescriptor;

public class GEMPeak extends ChIPSeqPeak
{
    public static final String PEAK_CALLER = "gem";
    public static final String[] FIELDS = new String[]{"Fold", "P-lg10", "Noise", "P_poiss", "Q_-lg10"};
    
    protected float control, expected, fold, ip, ipVsEmp, noise, pMLog10, pPoiss, qMLog10;
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

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
    public double getScore()
    {
        return getQMLog10();
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 4*9;
    }

    protected static final PropertyDescriptor CONTROL_PD = StaticDescriptor.create("control", "Control");
    protected static final PropertyDescriptor EXPECTED_PD = StaticDescriptor.create("expected", "Expected");
    protected static final PropertyDescriptor FOLD_PD = StaticDescriptor.create("fold", "Fold");
    protected static final PropertyDescriptor IP_PD = StaticDescriptor.create("ip", "IP");
    protected static final PropertyDescriptor IP_VS_EMP_PD = StaticDescriptor.create("ipVsEmp", "IP vs EMP");
    protected static final PropertyDescriptor NOISE_PD = StaticDescriptor.create("noise", "Noise");
    protected static final PropertyDescriptor PMLOG10_PD = StaticDescriptor.create("pMLog10", "-log10(p-value)");
    protected static final PropertyDescriptor PPOISS_PD = StaticDescriptor.create("pPoiss", "P(poisson)");
    protected static final PropertyDescriptor QMLOG10_PD = StaticDescriptor.create("qMLog10", "-log10(q-value)");
    
    @Override
    public DynamicPropertySet getProperties()
    {
        DynamicPropertySet dps = super.getProperties();
        dps.add( new DynamicProperty( CONTROL_PD, Float.class, getControl() ) );
        dps.add( new DynamicProperty( EXPECTED_PD, Float.class, getExpected() ) );
        dps.add( new DynamicProperty( FOLD_PD, Float.class, getFold() ) );
        dps.add( new DynamicProperty( IP_PD, Float.class, getIp() ) );
        dps.add( new DynamicProperty( IP_VS_EMP_PD, Float.class, getIpVsEmp() ) );
        dps.add( new DynamicProperty( NOISE_PD, Float.class, getNoise() ) );
        dps.add( new DynamicProperty( PMLOG10_PD, Float.class, getPMLog10() ) );
        dps.add( new DynamicProperty( PPOISS_PD, Float.class, getPPoiss() ) );
        dps.add( new DynamicProperty( QMLOG10_PD, Float.class, getQMLog10() ) );
        return dps;
    }
}