package ru.biosoft.bsastats;

import com.developmentontheedge.beans.annot.ExpertProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Remove low quality reads")
@PropertyDescription("Remove low quality reads")
@ExpertProperty
public class FilterByQuality extends TaskProcessor
{
    private int minQuality = 20;
    private int maxLowQualityPercentage = 10;

    @PropertyName ( "Minimal phred quality" )
    @PropertyDescription ( "Read phred quality cutoff" )
    public int getMinQuality()
    {
        return minQuality;
    }

    public void setMinQuality(int minQuality)
    {
        Object oldValue = this.minQuality;
        this.minQuality = minQuality;
        firePropertyChange( "minQuality", oldValue, minQuality );
    }

    @PropertyName ( "Maximal % of low-quality bp" )
    @PropertyDescription ( "Reads where more bp's have quality below the cutoff will be excluded" )
    public int getMaxLowQualityPercentage()
    {
        return maxLowQualityPercentage;
    }

    public void setMaxLowQualityPercentage(int maxLowQualityPercentage)
    {
        Object oldValue = this.maxLowQualityPercentage;
        this.maxLowQualityPercentage = maxLowQualityPercentage;
        firePropertyChange( "maxLowQualityPercentage", oldValue, maxLowQualityPercentage );
    }


    @Override
    public Task process(Task task)
    {
        byte[] quality = task.getQuality();
        int bad = 0;
        for( int i = 0; i < quality.length; i++ )
            if( quality[i] < getMinQuality() )
                bad++;
        if( bad > quality.length * getMaxLowQualityPercentage() / 100 )
            return null;
        return task;
    }

}
