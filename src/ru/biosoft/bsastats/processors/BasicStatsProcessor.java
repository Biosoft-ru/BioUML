package ru.biosoft.bsastats.processors;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mozilla.javascript.NativeArray;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
@PropertyName("Basic statistics")
@PropertyDescription("Gathers basic statistics like reads count and average read length")
public class BasicStatsProcessor extends AbstractStatisticsProcessor
{
    int count = 0;
    long totalGC = 0;
    long totalLength = 0;
    int minLength = Integer.MAX_VALUE;
    int maxLength = 0;

    @Override
    public void update(byte[] sequence, byte[] qualities)
    {
        count++;
        int length = sequence.length;
        totalLength+=length;
        minLength = Math.min(minLength, length);
        maxLength = Math.max(maxLength, length);
        totalGC+=getGCcount(sequence);
    }

    private void addRow(TableDataCollection table, String key, Object value)
    {
        TableDataCollectionUtils.addRow(table, key, new Object[] {value});
        log.info(key+": "+value);
    }

    @Override
    public void save(DataCollection<DataElement> resultsFolder) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(resultsFolder, getName());
        table.getColumnModel().addColumn("Value", String.class);
        addRow(table, "Count", count);
        addRow(table, "Min length", minLength);
        addRow(table, "Max length", maxLength);
        addRow(table, "Avg length", String.format(Locale.ENGLISH, "%.2f", ((double)totalLength)/count));
        addRow(table, "GC%", String.format(Locale.ENGLISH, "%.2f", ((double)totalGC*100/totalLength)));
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
        Set<String> rows = new LinkedHashSet<>();
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        for(DataElementPath path: inputReports)
        {
            try
            {
                TableDataCollection table = path.getChildPath(getName()).getDataElement(TableDataCollection.class);
                Map<String, String> tableData = new HashMap<>();
                for(RowDataElement row: table)
                {
                    rows.add(row.getName());
                    tableData.put(row.getName(), row.getValues()[0].toString());
                }
                data.put(path.getName(), tableData);
            }
            catch( Exception e )
            {
                log.warning(getName()+": unable to process "+path+"; skipping");
                data.remove(path.getName());
            }
        }
        String[] colNames = new String[rows.size()+1];
        int i=0;
        colNames[i++] = "Input";
        for(String rowName: rows) colNames[i++] = rowName;
        NativeArray[] rowsData = new NativeArray[data.size()];
        int j=0;
        for(Entry<String, Map<String, String>> entry: data.entrySet())
        {
            String[] rowData = new String[colNames.length];
            rowData[0] = entry.getKey();
            for(i=1; i<colNames.length; i++)
            {
                rowData[i] = entry.getValue().containsKey(colNames[i])?entry.getValue().get(colNames[i]):"";
            }
            rowsData[j++] = new NativeArray(rowData);
        }
        outputReport.addSubHeader(getName());
        outputReport.addTable(new NativeArray(colNames), new NativeArray(rowsData), "data");
    }
}
