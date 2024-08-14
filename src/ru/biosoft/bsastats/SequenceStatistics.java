package ru.biosoft.bsastats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mozilla.javascript.NativeArray;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsastats.processors.StatisticsProcessor;
import ru.biosoft.bsastats.processors.StatisticsProcessorsRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.plugins.jsreport.JavaScriptReport;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.j2html.TagCreator;

/**
 * @author lan
 *
 */
@ClassIcon("resources/TrackStatistics.gif")
public class SequenceStatistics extends AnalysisMethodSupport<SequenceStatisticsParameters>
{
    private static final int CHUNK_SIZE = 5000; // Number of sites to process in single chunk
    
    public SequenceStatistics(DataCollection<?> origin, String name)
    {
        super(origin, name, new SequenceStatisticsParameters());
    }

    protected List<StatisticsProcessor> getProcessors()
    {
        List<StatisticsProcessor> result = new ArrayList<>();
        for(String processorName: getParameters().getProcessors())
        {
            try
            {
                result.add(StatisticsProcessorsRegistry.getProcessorClass(processorName).newInstance());
            }
            catch( Exception e )
            {
                log.warning("Unable to init processor "+processorName+": will be switched off");
            }
        }
        return result;
    }
    
    @Override
    public DataElement justAnalyzeAndPut() throws Exception
    {
        final DataCollection<DataElement> result = DataCollectionUtils.createSubCollection(getParameters().getOutput());
        log.info("Initializing processors...");
        List<StatisticsProcessor> processors = getProcessors();
        for(StatisticsProcessor processor: processors) processor.init(getLogger());
        int nThreads = Math.min(processors.size(), SecurityManager.getMaximumThreadsNumber());
        ProgressIterator<Task> iterator = getParameters().getTasksIterator();
        if(jobControl.isStopped()) return null;
        log.info("Reading sites...");
        List<Task> tasks = new ArrayList<>(CHUNK_SIZE);
        while(iterator.hasNext())
        {
            tasks.add(iterator.next());
            if(tasks.size() == CHUNK_SIZE)
            {
                processTasks(tasks, processors, nThreads);
                tasks.clear();
                jobControl.setPreparedness((int) ( iterator.getProgress()*99.0 ));
                if(jobControl.isStopped()) return null;
            }
        }
        if(!tasks.isEmpty()) processTasks(tasks, processors, nThreads);
        log.info("Saving...");
        for(StatisticsProcessor processor: processors)
        {
            try
            {
                processor.save(result);
            }
            catch( Exception e )
            {
                log.warning("Error saving results of '"+processor.getName()+"': "+e.toString());
                if(e instanceof LoggedException)
                    ( (LoggedException)e ).log();
            }
        }
        log.info("Generating report...");
        writeProperties(result);
        return createReport(result, processors);
    }

    private DataElement createReport(DataCollection<DataElement> result, Collection<StatisticsProcessor> processors) throws Exception
    {
        TableDataCollection quality = TableDataCollectionUtils.createTableDataCollection(result, "Quality");
        quality.getColumnModel().addColumn("Quality", String.class);
        Report report = new JavaScriptReport().create("Statistics report "+result.getName());
        report.addHeader("Statistics report "+result.getName());
        for(StatisticsProcessor processor: processors)
        {
            String[] reportItemNames = processor.getReportItemNames();
            if(reportItemNames == null) continue;
            report.addHTML("[" + processor.getQuality().name() + "] <a href=\"#" + processor.getName().replaceAll("\\W", ".") + "\">"
                    + processor.getName() + "</a><br>");
            TableDataCollectionUtils.addRow(quality, processor.getName(), new Object[] {processor.getQuality().name()}, true);
        }
        quality.finalizeAddition();
        CollectionFactoryUtils.save(quality);
        for(StatisticsProcessor processor: processors)
        {
            String[] reportItemNames = processor.getReportItemNames();
            if(reportItemNames == null) continue;
            report.addHTML(TagCreator.a().withName( processor.getName().replaceAll("\\W", ".") ));
            report.addSubHeader(processor.getName()+" ("+processor.getQuality().name()+")");
            for(String name: reportItemNames)
            {
                if(!result.contains(name)) continue;
                DataElement de = result.get(name);
                if(de instanceof ImageElement)
                {
                    report.addImage(((ImageElement)de).getImage(null), de.getName(), de.getName());
                }
                if(de instanceof TableDataCollection)
                {
                    TableDataCollection table = (TableDataCollection)de;
                    NativeArray colNames = new NativeArray( table.columns().map( TableColumn::getName ).prepend( "" ).toArray() );
                    NativeArray[] data = table.stream()
                            .map( rde -> new NativeArray( StreamEx.of( rde.getValues() ).prepend( rde.getName() ).toArray() ) )
                            .toArray( NativeArray[]::new );
                    report.addTable(colNames, new NativeArray(data), "data");
                }
            }
        }
        DataElementPath reportPath = DataElementPath.create(result, "Report");
        report.store(reportPath.toString());
        return reportPath.optDataElement();
    }

    private void processTasks(final List<Task> tasks, List<StatisticsProcessor> processors, int nThreads) throws Exception
    {
        TaskPool.getInstance().iterate(processors, (Iteration<StatisticsProcessor>)processor -> {
            for(Task task: tasks) processor.update(task.getSequence(), task.getQuality());
            return true;
        }, nThreads);
    }
}
