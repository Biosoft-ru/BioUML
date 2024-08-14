package biouml.plugins.gtrd.master.sites.chipseq;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.util.bean.StaticDescriptor;

public class SISSRSPeak extends ChIPSeqPeak
{
    public static final String PEAK_CALLER = "sissrs";
    public static final String[] FIELDS = new String[]{"p-value", "fold", "tags"};
    
    protected int numTags;
    protected float fold = Float.NaN;//NaN if no control
    protected float pValue = Float.NaN;//NaN if no control
    
    @Override
    public String getPeakCaller()
    {
        return PEAK_CALLER;
    }

    public int getNumTags()
    {
        return numTags;
    }

    public void setNumTags(int numTags)
    {
        this.numTags = numTags;
    }

    public float getFold()
    {
        return fold;
    }

    public void setFold(float fold)
    {
        this.fold = fold;
    }

    public float getPValue()
    {
        return pValue;
    }

    public void setPValue(float pValue)
    {
        this.pValue = pValue;
    }
 
    @Override
    public double getScore()
    {
        return getPValue();
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 4*3;
    }
    
    protected static final PropertyDescriptor NUM_TAGS_PD = StaticDescriptor.create( "numTags", "Number of tags" );
    protected static final PropertyDescriptor FOLD_PD = StaticDescriptor.create( "fold", "Fold" );
    protected static final PropertyDescriptor PVAL_PD = StaticDescriptor.create( "pValue", "p-value" );
    
    @Override
    public DynamicPropertySet getProperties()
    {
        DynamicPropertySet dps = super.getProperties();
        dps.add( new DynamicProperty(NUM_TAGS_PD, Integer.class, getNumTags()) );
        dps.add( new DynamicProperty(FOLD_PD, Float.class, getFold()) );
        dps.add( new DynamicProperty(PVAL_PD, Float.class, getPValue()) );
        return dps;
    }
}