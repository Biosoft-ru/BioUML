package ru.biosoft.bsastats.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.mutable.MutableInt;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Overrepresented prefixes")
@PropertyDescription("Search for read prefixes (starting from the read start) up to 15 bp long which are overrepresented in the set.")
public class OverrepresentedPrefixes extends AbstractStatisticsProcessor
{
    private static final byte[] LETTER_TO_CODE = Nucleotide5LetterAlphabet.getInstance().letterToCodeMatrix();
    public final static int MAX_COUNT = 200000;
    private int totalCount = 0;
    private Map<PackedRead15, MutableInt> dups = new HashMap<>();

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        totalCount++;
        int maxLength = Math.min(15, sequence.length);
        PackedRead15 currentRead = PackedRead15.NULL_READ;
        for(int i=0; i<=maxLength; i++)
        {
            MutableInt count = dups.get(currentRead);
            if(count != null) count.increment();
            else if(dups.size() < MAX_COUNT)
            {
                dups.put(currentRead, new MutableInt(1));
            }
            
            if(i<maxLength)
            {
                if(sequence[i]=='n' || sequence[i]=='N') break; // DO NOT count sequences containing 'n'
                currentRead = new PackedRead15(currentRead, LETTER_TO_CODE[sequence[i]]);
            }
        }
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        Map<PackedRead15, PackedRead15> parents = new HashMap<>();
        for(Entry<PackedRead15, MutableInt> entry: dups.entrySet())
        {
            int observed = entry.getValue().intValue();
            if(((double)observed)/totalCount < 0.001) continue;
            String seqString = entry.getKey().toString();
            if(seqString.isEmpty()) continue;
            PackedRead15 parent = entry.getKey();
            int parentObserved = observed;
            while(!parent.toString().isEmpty())
            {
                PackedRead15 newParent = new PackedRead15(parent);
                int newParentObserved = dups.get(newParent).intValue();
                if(parentObserved < newParentObserved*3/4) break;
                parent = newParent;
                parentObserved = newParentObserved;
            }
            if(parent != entry.getKey()) parents.put(entry.getKey(), parent);
        }
        for(PackedRead15 hitCandidate: parents.keySet().toArray(new PackedRead15[parents.size()]))
        {
            PackedRead15 parent = parents.get(hitCandidate);
            if(parent == null) continue;
            PackedRead15 curParent = new PackedRead15(hitCandidate);
            while(!curParent.equals(parent))
            {
                parents.remove(curParent);
                curParent = new PackedRead15(curParent);
            }
        }
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        ColumnModel columnModel = table.getColumnModel();
        columnModel.addColumn("Length", Integer.class);
        columnModel.addColumn("Parent length", Integer.class);
        columnModel.addColumn("Count", Integer.class);
        columnModel.addColumn("Parent count", Integer.class);
        columnModel.addColumn("Observed to expected ratio", Double.class);
        for(Entry<PackedRead15, PackedRead15> entry: parents.entrySet())
        {
            int parentObserved = dups.get(entry.getValue()).intValue();
            double expected = parentObserved;
            int observed = dups.get(entry.getKey()).intValue();
            String readString = entry.getKey().toString();
            String parentString = entry.getValue().toString();
            for(int i=parentString.length(); i<readString.length(); i++) expected/=4;
            if(observed/expected > 10)
                TableDataCollectionUtils.addRow(table, parentString + "+" + readString.substring(parentString.length()), new Object[] {
                        readString.length(), parentString.length(), observed, parentObserved, observed / expected}, true);
        }
        table.finalizeAddition();
        if(table.getSize() > 10) raiseError();
        if(table.getSize() > 0) raiseWarning();
        else
        {
            while(columnModel.getColumnCount() > 0)
                columnModel.removeColumn(0);
            TableDataCollectionUtils.addRow(table, "No overrepresented prefixes found", new Object[0]);
            log.info("No overrepresented prefixes found");
        }
        table.getOrigin().put(table);
    }

    @Override
    public String[] getReportItemNames()
    {
        return new String[] {getName()};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
        mergeTables(inputReports, outputReport, new String[] {"Input", "Number of overrepresented prefixes", "Prefix", "Length",
                "Parent length", "Count", "Parent count", "Observed to expected ratio"});
    }
}
