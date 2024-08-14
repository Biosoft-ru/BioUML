package ru.biosoft.bsastats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.NativeArray;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.html.ZipHtmlDataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsastats.processors.StatisticsProcessor;
import ru.biosoft.bsastats.processors.StatisticsProcessorsRegistry;
import ru.biosoft.plugins.jsreport.JavaScriptReport;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.j2html.TagCreator;

/**
 * @author lan
 *
 */
public class MergeStats extends AnalysisMethodSupport<MergeStatsParameters>
{
    public MergeStats(DataCollection<?> origin, String name)
    {
        super(origin, name, new MergeStatsParameters());
    }

    @Override
    public DataElement justAnalyzeAndPut() throws Exception
    {
        final Report report = new JavaScriptReport().create(parameters.getOutput().getName());
        final DataElementPathSet inputs = new DataElementPathSet();
        inputs.addAll(parameters.getInputStatistics());
        for(DataElementPath path: parameters.getInputStatistics())
        {
            ZipHtmlDataCollection stats = path.getChildPath("Report").optDataElement(ZipHtmlDataCollection.class);
            TableDataCollection qualities = path.getChildPath("Quality").optDataElement(TableDataCollection.class);
            if(stats == null || qualities == null)
            {
                log.warning("Report not found in "+path+"; skipping");
                inputs.remove(path);
            }
        }
        report.addHeader("Joined report");
        addQualities(inputs, report);
        jobControl.forCollection(Arrays.asList(StatisticsProcessorsRegistry.getValues()), processorName -> {
            try
            {
                StatisticsProcessor processor = StatisticsProcessorsRegistry.getProcessorClass(processorName).newInstance();
                processor.init(getLogger());
                report.addHTML(TagCreator.a().withName( processor.getName().replaceAll("\\W", ".") ));
                processor.mergeReports(inputs, report);
            }
            catch( Exception e )
            {
                log.warning(processorName+": "+e.toString());
            }
            return true;
        });
        report.store(parameters.getOutput().toString());
        return parameters.getOutput().optDataElement();
    }

    private void addQualities(DataElementPathSet inputReports, Report report) throws Exception
    {
        Set<String> rows = new LinkedHashSet<>();
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        for(DataElementPath path: inputReports)
        {
            try
            {
                TableDataCollection table = path.getChildPath("Quality").getDataElement( TableDataCollection.class );
                Map<String, String> tableData = new HashMap<>();
                for(String processorName: StatisticsProcessorsRegistry.getValues())
                {
                    RowDataElement row = table.get(processorName);
                    if(row != null)
                    {
                        rows.add(row.getName());
                        tableData.put(row.getName(), row.getValues()[0].toString());
                    }
                }
                data.put(path.getName(), tableData);
            }
            catch( Exception e )
            {
                log.warning(getName()+": unable to process "+path+"; skipping");
                data.remove(path.getName());
            }
        }
        String[] colNames = new String[data.size()+1];
        int i=0;
        colNames[i++] = "Processor";
        for(String colName: data.keySet()) colNames[i++] = colName;
        NativeArray[] rowsData = new NativeArray[rows.size()];
        int j=0;
        for(String row: rows)
        {
            String[] rowData = new String[colNames.length];
            i=0;
            rowData[i++] = TagCreator.a().withHref( "#" + row.replaceAll("\\W", ".") ).withText( row ).render();
            for(Map<String, String> values: data.values())
            {
                String rowValue = values.get(row);
                rowData[i++] = rowValue == null?"":rowValue;
            }
            rowsData[j++] = new NativeArray(rowData);
        }
        report.addSubHeader("Overview");
        report.addTable(new NativeArray(colNames), new NativeArray(rowsData), "data", false);
    }
}
