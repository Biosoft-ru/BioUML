package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author lan
 *
 */
public class TrackCoverageAnalysis extends AnalysisMethodSupport<TrackCoverageAnalysisParameters>
{
    public TrackCoverageAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new TrackCoverageAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkGreater("step", 1000);
        checkGreater("window", 0);
        DataCollection<AnnotatedSequence> seq = parameters.getSequences().getSequenceCollectionPath().optDataCollection(AnnotatedSequence.class);
        if(seq == null )
            throw new IllegalArgumentException("Invalid sequences collection specified");
    }
    
    private static class CoverageBucket
    {
        Interval interval;
        int totalCoverage;
        
        public CoverageBucket(Interval interval)
        {
            this.interval = interval;
        }
        
        public void update(Site site)
        {
            Interval intersect = site.getInterval().intersect(interval);
            if(intersect != null)
                totalCoverage += intersect.getLength();
        }

        public double getAverageCoverage()
        {
            return ((double)totalCoverage)/interval.getLength();
        }
        
        public Interval getInterval()
        {
            return interval;
        }
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        Track track = parameters.getTrack().getDataElement(Track.class);
        DataCollection<AnnotatedSequence> sequences = parameters.getSequences().getSequenceCollectionPath().getDataCollection(AnnotatedSequence.class);

        log.info("Gathering sequences statistics...");
        long totalLength = 0;
        List<AnnotatedSequence> sequenceList = new ArrayList<>();
        for(String name: sequences.getNameList())
        {
            AnnotatedSequence sequence = sequences.get(name);
            totalLength+=sequence.getSequence().getLength();
            sequenceList.add(sequence);
        }
        
        log.info("Gathering coverage info...");
        
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(parameters.getOutput());
        result.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        result.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
        ColumnModel model = result.getColumnModel();
        model.addColumn("Sequence", String.class);
        model.addColumn("From", Integer.class);
        model.addColumn("To", Integer.class);
        model.addColumn("Length", Integer.class);
        model.addColumn("Coverage", Double.class);
        long currentLength = 0;
        int rowId = 0;
        for(AnnotatedSequence sequence: sequenceList)
        {
            List<CoverageBucket> buckets = new ArrayList<>();
            Sequence seq = sequence.getSequence();
            for(Interval interval: seq.getInterval().splitByStep(parameters.getStep()))
            {
                Interval bucketInterval = new Interval(interval.getFrom(), interval.getFrom() + parameters.getWindow() - 1)
                        .intersect(seq.getInterval());
                buckets.add(new CoverageBucket(bucketInterval));
                if(bucketInterval.getLength() < parameters.getWindow()) break;
            }
            for(Site site: track.getSites(sequence.getCompletePath().toString(), sequence.getSequence().getStart(), sequence.getSequence().getStart()+sequence.getSequence().getLength()))
            {
                int firstBucket = Math.max(0, (site.getFrom()-parameters.getWindow()+parameters.getStep()-1)/parameters.getStep());
                int lastBucket = Math.min(buckets.size()-1, site.getTo()/parameters.getStep());
                for(int i=firstBucket; i<=lastBucket; i++)
                {
                    buckets.get(i).update(site);
                }
            }
            for(CoverageBucket bucket: buckets)
            {
                if(parameters.isOutputEmptyIntervals() || bucket.getAverageCoverage() > 0)
                {
                    TableDataCollectionUtils.addRow(result, String.valueOf(++rowId),
                            new Object[] {sequence.getName(), bucket.getInterval().getFrom(), bucket.getInterval().getTo(),
                                    bucket.getInterval().getLength(), bucket.getAverageCoverage()}, true);
                }
            }
            if(jobControl.isStopped())
            {
                result.getCompletePath().remove();
                return null;
            }
            currentLength+=sequence.getSequence().getLength();
            jobControl.setPreparedness((int) ( 100.0*currentLength/totalLength ));
        }
        result.finalizeAddition();
        parameters.getOutput().save(result);
        jobControl.setPreparedness(100);
        return result;
    }

}
