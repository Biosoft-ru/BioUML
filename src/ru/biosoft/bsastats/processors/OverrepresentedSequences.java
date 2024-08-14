package ru.biosoft.bsastats.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.mutable.MutableInt;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPathSet;
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
@PropertyName("Overrepresented sequences")
@PropertyDescription("Look for sequences which appear in more than 0.1% cases")
public class OverrepresentedSequences extends AbstractStatisticsProcessor
{
    public final static int MAX_COUNT = 200000;
    private int totalCount = 0;
    private int totalCountAtLimit = 0;
    private Map<PackedRead, MutableInt> dups = new HashMap<>();

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        totalCount++;
        int length = sequence.length;
        if(length > 75) length = 50;
        PackedRead packedRead = new PackedRead(sequence, length);
        MutableInt count = dups.get(packedRead);
        if(count != null)
            count.increment();
        if(dups.size() < MAX_COUNT)
        {
            totalCountAtLimit++;
            if(count == null)
                dups.put(packedRead, new MutableInt(1));
        }
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        boolean found = false;
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        ColumnModel columnModel = table.getColumnModel();
        columnModel.addColumn("Count", Integer.class);
        columnModel.addColumn("Percentage", Double.class);
        columnModel.addColumn("Contaminant match", String.class);
        for(Entry<PackedRead, MutableInt> entry: dups.entrySet())
        {
            double percentage = entry.getValue().doubleValue()/totalCount*100;
            if(percentage>0.1)
            {
                if(percentage>1) raiseError(); else raiseWarning();
                String read = entry.getKey().toString();
                TableDataCollectionUtils.addRow(table, read, new Object[] {entry.getValue(), percentage,
                        ContaminantsFinder.search(read).toString()});
                found = true;
            }
        }
        if(!found)
        {
            while(columnModel.getColumnCount() > 0)
                columnModel.removeColumn(0);
            TableDataCollectionUtils.addRow(table, "No overrepresented sequences found", new Object[0]);
            log.info("No overrepresented sequences found");
        } else
        {
            TableDataCollectionUtils.setSortOrder(table, "Percentage", false);
        }
        resultsFolder.put(table);
    }

    @Override
    public String[] getReportItemNames()
    {
        return new String[] {getName()};
    }

    @Override
    public void mergeReports(DataElementPathSet inputReports, Report outputReport) throws Exception
    {
        mergeTables(inputReports, outputReport, new String[] {"Input", "Number of overrepresented sequences", "Sequence", "Count",
                "Percentage", "Contaminant match"});
    }
}
