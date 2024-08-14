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
@PropertyName("N content per base")
@PropertyDescription("Distribution of 'N' along the bases")
public class NContentPerBaseProcessor extends AbstractContentPerBaseProcessor
{
    @Override
    public void init(Logger log)
    {
        super.init(log);
        entries.add(new CountEntry("N", LetterCounter.getLetterCounter("Nn"), Color.BLACK));
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        super.save(resultsFolder);
        IntArray counts = entries.get(0).counts;
        for(int i=0; i<counts.size(); i++)
        {
            double val = counts.get(i)*100.0/totalCounts.get(i);
            if(val > 5)
                raiseWarning();
            if(val > 20)
            {
                raiseError();
                break;
            }
        }
    }
}
