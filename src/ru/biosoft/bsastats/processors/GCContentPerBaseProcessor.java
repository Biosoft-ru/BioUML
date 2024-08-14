package ru.biosoft.bsastats.processors;

import java.awt.Color;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.IntArray;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("GC content per base")
@PropertyDescription("Distribution of GC along the bases")
public class GCContentPerBaseProcessor extends AbstractContentPerBaseProcessor
{
    @Override
    public void init(Logger log)
    {
        super.init(log);
        entries.add(new CountEntry("GC", gcCounter, Color.RED));
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        super.save(resultsFolder);
        IntArray counts = entries.get(0).counts;
        long totalGC = 0;
        long totalLength = 0;
        for(int i=0; i<counts.size(); i++)
        {
            totalGC+=counts.get(i);
            totalLength+=totalCounts.get(i);
        }
        double meanGC = 100.0*totalGC/totalLength;
        for(int i=0; i<counts.size(); i++)
        {
            double diff = Math.abs(counts.get(i)*100.0/totalCounts.get(i)-meanGC);
            if(diff > 5)
                raiseWarning();
            if(diff > 10)
            {
                raiseError();
                break;
            }
        }
    }
}
