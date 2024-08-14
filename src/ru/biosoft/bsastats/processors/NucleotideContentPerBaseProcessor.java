package ru.biosoft.bsastats.processors;

import java.awt.Color;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Nucleotide content per base")
@PropertyDescription("Distribution of individual nucleotides along the bases")
public class NucleotideContentPerBaseProcessor extends AbstractContentPerBaseProcessor
{
    @Override
    public void init(Logger log)
    {
        super.init(log);
        entries.add(new CountEntry("A", LetterCounter.getLetterCounter("Aa"), new Color(255, 0, 0)));
        entries.add(new CountEntry("C", LetterCounter.getLetterCounter("Cc"), new Color(58, 95, 216)));
        entries.add(new CountEntry("G", LetterCounter.getLetterCounter("Gg"), new Color(255, 210, 0)));
        entries.add(new CountEntry("T", LetterCounter.getLetterCounter("Tt"), new Color(13, 170, 65)));
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        super.save(resultsFolder);
        // Note that FastQC has an error in PerBaseSequenceContent.java: it measures C-A and T-G differences instead
        for(int i=0; i<totalCounts.size(); i++)
        {
            double diffAT = Math.abs(entries.get(0).counts.get(i)-entries.get(3).counts.get(i))*100.0/totalCounts.get(i);
            if(diffAT > 10)
                raiseWarning();
            if(diffAT > 20)
            {
                raiseError();
                break;
            }
            double diffGC = Math.abs(entries.get(1).counts.get(i)-entries.get(2).counts.get(i))*100.0/totalCounts.get(i);
            if(diffGC > 10)
                raiseWarning();
            if(diffGC > 20)
            {
                raiseError();
                break;
            }
        }
    }
}
