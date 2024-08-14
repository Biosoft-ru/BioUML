package ru.biosoft.bsastats;

import java.util.Arrays;

import com.developmentontheedge.beans.annot.ExpertProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName ( "Trim low quality" )
@PropertyDescription ( "Trim low quality bases from the 3' end of reads" )
@ExpertProperty
public class TrimLowQuality extends TaskProcessor
{
    private int phredQualityThreashold = 20;
    private boolean from3PrimeEnd = true;
    private boolean from5PrimeEnd = false;

    @PropertyName ( "Phred quality threshold" )
    @PropertyDescription ( "Phred quality threshold" )
    public int getPhredQualityThreashold()
    {
        return phredQualityThreashold;
    }

    public void setPhredQualityThreashold(int phredQualityThreashold)
    {
        Object oldValue = this.phredQualityThreashold;
        this.phredQualityThreashold = phredQualityThreashold;
        firePropertyChange( "phredQualityThreashold", oldValue, phredQualityThreashold );
    }
    
    
    @PropertyName("From 3' end")
    @PropertyDescription("Trim bases from 3' end of the read")
    public boolean isFrom3PrimeEnd()
    {
        return from3PrimeEnd;
    }
    public void setFrom3PrimeEnd(boolean from3PrimeEnd)
    {
        boolean oldValue = this.from3PrimeEnd;
        this.from3PrimeEnd = from3PrimeEnd;
        firePropertyChange( "from3PrimeEnd", oldValue, from3PrimeEnd );
    }

    @PropertyName("From 5' end")
    @PropertyDescription("Trim bases from 5' end of the read")
    public boolean isFrom5PrimeEnd()
    {
        return from5PrimeEnd;
    }
    public void setFrom5PrimeEnd(boolean from5PrimeEnd)
    {
        boolean oldValue = this.from5PrimeEnd;
        this.from5PrimeEnd = from5PrimeEnd;
        firePropertyChange( "from5PrimeEnd", oldValue, from5PrimeEnd );
    }

    @Override
    public Task process(Task task)
    {
        if( task.getSequence().length == 0 )
            return task;
        byte[] qual = task.getQuality();

        int headTrimPoint = -1;
        int tailTrimPoint = qual.length;

        if( from3PrimeEnd )
            tailTrimPoint = findTailTrimPoint( qual );

        if( from5PrimeEnd )
            headTrimPoint = findHeadTrimPoint( qual );

        if( headTrimPoint + 1 >= tailTrimPoint )
            return new Task( new byte[0], new byte[0], task.getData() );

        return new Task(
                Arrays.copyOfRange( task.getSequence(), headTrimPoint + 1, tailTrimPoint ),
                Arrays.copyOfRange( task.getQuality(), headTrimPoint + 1, tailTrimPoint ), task.getData() );
    }
    
    private int findTailTrimPoint(byte[] qual)
    {
        int cumSum = 0;
        int minCumSum = 0;
        int trimPoint = qual.length;
        for(int i = qual.length - 1; i >= 0; i--)
        {
            cumSum += qual[i] - phredQualityThreashold;
            if(cumSum > 0)
                break;
            if(cumSum <= minCumSum)
            {
                minCumSum = cumSum;
                trimPoint = i;
            }
        }
        return trimPoint;
    }
    
    private int findHeadTrimPoint(byte[] qual)
    {
        int cumSum = 0;
        int minCumSum = 0;
        int trimPoint = -1;
        for(int i = 0; i < qual.length; i++)
        {
            cumSum += qual[i] - phredQualityThreashold;
            if(cumSum > 0)
                break;
            if(cumSum <= minCumSum)
            {
                minCumSum = cumSum;
                trimPoint = i;
            }
        }
        return trimPoint;
    }

}
