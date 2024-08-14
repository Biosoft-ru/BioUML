package biouml.plugins.gtrd.master.sites.chipseq;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.util.bean.StaticDescriptor;

public class MACS2ChIPSeqPeak extends ChIPSeqPeak
{
    public static final String PEAK_CALLER = "macs2";
    public static final String[] FIELDS = new String[]{"-log10(pvalue)", "-log10(qvalue)", "fold_enrichment", "pileup"};
    
    protected float foldEnrichment;
    protected float mLog10PValue;
    protected float mLog10QValue;
    protected int summit;
    protected float pileup;
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

    public float getFoldEnrichment()
    {
        return foldEnrichment;
    }

    public void setFoldEnrichment(float foldEnrichment)
    {
        this.foldEnrichment = foldEnrichment;
    }

    public float getMLog10PValue()
    {
        return mLog10PValue;
    }

    public void setMLog10PValue(float mLog10PValue)
    {
        this.mLog10PValue = mLog10PValue;
    }

    public float getMLog10QValue()
    {
        return mLog10QValue;
    }

    public void setMLog10QValue(float mLog10QValue)
    {
        this.mLog10QValue = mLog10QValue;
    }

    public boolean hasSummit()
    {
        return true;
    }
    
    @Override
    public int getSummit()
    {
        return summit;
    }

    public void setSummit(int summit)
    {
        this.summit = summit;
    }

    public float getPileup()
    {
        return pileup;
    }

    public void setPileup(float pileup)
    {
        this.pileup = pileup;
    }
    
    @Override
    public double getScore()
    {
        return getMLog10QValue();
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 5*4;
    }
    
    protected static final PropertyDescriptor FOLD_ENRICHMENT_PD = StaticDescriptor.create( "foldEnrichment", "Fold enrichment" );
    protected static final PropertyDescriptor M_LOG10_PVALUE_PD = StaticDescriptor.create( "mLog10PValue", "-log10(p-value)" );
    protected static final PropertyDescriptor M_LOG10_QVALUE_PD = StaticDescriptor.create( "mLog10QValue", "-log10(q-value)" );
    protected static final PropertyDescriptor PILEUP_PD = StaticDescriptor.create("pileup", "Pileup");
    
    @Override
    public DynamicPropertySet getProperties()
    {
        DynamicPropertySet dps = super.getProperties();
        dps.add( new DynamicProperty( FOLD_ENRICHMENT_PD, Float.class, getFoldEnrichment() ) );
        dps.add( new DynamicProperty( M_LOG10_PVALUE_PD, Float.class, getMLog10PValue() ) );
        dps.add( new DynamicProperty( M_LOG10_QVALUE_PD, Float.class, getMLog10QValue() ) );
        dps.add( new DynamicProperty( PILEUP_PD, Float.class, getPileup() ) );
        return dps;
    }
}