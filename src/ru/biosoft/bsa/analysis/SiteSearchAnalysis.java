package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MergedTrack;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.track.WholeSequenceTrack;
import ru.biosoft.bsa.view.TrackViewBuilder;
import ru.biosoft.util.TextUtil;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.Application;

@ClassIcon( "resources/site_search_track.gif" )
public class SiteSearchAnalysis extends AnalysisMethodSupport<SiteSearchAnalysisParameters>
{
    public static final String INTERVALS_COLLECTION_PROPERTY = "IntervalsCollection";
    public static final String TOTAL_LENGTH_PROPERTY = "TotalLength";
    public static final String SEQUENCES_COLLECTION_PROPERTY = "SiteSearchSequencesCollection";
    public static final String SEQUENCES_LIST_PROPERTY = "SiteSearchSequencesList";

    private int totalLength = 0;

    public SiteSearchAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new SiteSearchAnalysisParameters());
    }

    @Override
    public String generateJavaScript(Object params)
    {
        if( params == null || ! ( params instanceof SiteSearchAnalysisParameters ) )
            return null;
        SiteSearchAnalysisParameters parameters = (SiteSearchAnalysisParameters)params;
        String[] paramList = new String[4];
        DataElementPath sourceDC = parameters.getSeqCollectionPath();
        paramList[0] = sourceDC == null ? "null" : "data.get('" + StringEscapeUtils.escapeJavaScript(sourceDC.toString()) + "')";
        DataElementPath profile = parameters.getProfilePath();
        paramList[1] = profile == null ? "null" : "data.get('" + StringEscapeUtils.escapeJavaScript(profile.toString()) + "')";
        DataElementPath track = parameters.getTrackPath();
        paramList[2] = track == null ? "null" : "data.get('" + StringEscapeUtils.escapeJavaScript(track.toString()) + "')";
        DataElementPath output = parameters.getOutput();
        paramList[3] = output == null ? "null" : "'" + StringEscapeUtils.escapeJavaScript(output.toString()) + "'";
        return "bsa.siteSearch(" + StringUtils.join(paramList, ", ") + ");";
    }

    protected void doSiteSearch(DataCollection<?> seqCollection, String[] seqlist, DataCollection<SiteModel> profile, Track intervals,
            WritableTrack result) throws Exception
    {
        List<SiteModel> siteModels = new ArrayList<>();
        for(SiteModel model: profile)
            siteModels.add(model);

        result = new SynchronizedTrack(result);

        List<SiteSearchWorker> tasks = new ArrayList<>();
        for( String element : seqlist )
        {
            AnnotatedSequence inspected = (AnnotatedSequence)seqCollection.get(element);
            Sequence sequence = inspected.getSequence();
            SequenceAccessor sequenceAccessor = new SequenceAccessor(sequence);
            DataCollection<Site> intervalList = intervals.getSites(inspected.getCompletePath().toString(), sequence.getStart(), sequence.getLength());
            for(Site site: intervalList)
            {
                Interval interval = site.getInterval();
                if(!sequence.getInterval().inside(interval))
                {
                    Interval newInterval = sequence.getInterval().intersect(interval);
                    log.warning("Interval " + site.getName() + " "+interval + " does not fit the sequence '" + sequence.getName() + "' " + sequence.getInterval() + "; will be changed to "
                            + newInterval
                            + ". Please check that track and sequence collection were selected correctly.");
                    interval = newInterval;
                }
                if(interval.getLength() <= 0)
                {
                    log.warning("Interval "+site.getName()+" has invalid length; will be excluded from the search");
                    continue;
                }
                totalLength += interval.getLength();
                tasks.addAll(SiteSearchWorker.getWorkers(sequenceAccessor, interval.getFrom(), interval.getTo(), siteModels, result, 1000000));
            }
        }
        try
        {
            TaskPool.getInstance().iterate(tasks, SiteSearchWorker::call, jobControl);
        }
        catch( ExecutionException e )
        {
            throw ExceptionRegistry.translateException(e);
        }
        result.finalizeAddition();
    }

    @Override
    public Track justAnalyzeAndPut() throws Exception
    {
        DataElementPath trackPath = parameters.getTrackPath();
        Track intervals = trackPath == null ? null : trackPath.getDataElement(Track.class);
        DataCollection<SiteModel> profileDC = parameters.getProfilePath().getDataElement(DataCollection.class);
        Application.getPreferences().add(new DynamicProperty(Const.LAST_PROFILE_PREFERENCE, String.class, parameters.getProfilePath().toString()));
        DataCollection<?> sequences = parameters.getSeqCollectionPath().getDataElement(DataCollection.class);
        String[] seqlist = sequences.names().toArray(String[]::new);
        log.info("All sequences from " + sequences.getCompletePath() + " are used.");
        DataElementPath outputPath = parameters.getOutput();
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, outputPath.getName());

        if( intervals != null )
        {
            properties.put(INTERVALS_COLLECTION_PROPERTY, DataElementPath.create(intervals).toString());
            intervals = new MergedTrack(intervals);
        }
        else
        {
            intervals = new WholeSequenceTrack("track", null);
        }
        properties.put(SEQUENCES_COLLECTION_PROPERTY, sequences.getCompletePath().toString());
        SiteSearchAnalysis.serializeSequencesList(properties, seqlist);
        properties.put(Track.SEQUENCES_COLLECTION_PROPERTY, sequences.getCompletePath().toString());
        totalLength = 0;

        WritableTrack track = null;
        try
        {
            outputPath.remove();
            track = TrackUtils.createTrack(outputPath.optParentCollection(), properties);
            doSiteSearch(sequences, seqlist, profileDC, intervals, track);
            if(track instanceof DataCollection)
            {
                ((DataCollection<?>)track).getInfo().getProperties().setProperty(TOTAL_LENGTH_PROPERTY, String.valueOf(totalLength));
                Species species = Species.getDefaultSpecies(intervals instanceof DataCollection ? (DataCollection<?>)intervals : sequences);
                ((DataCollection<?>)track).getInfo().getProperties().setProperty(DataCollectionUtils.SPECIES_PROPERTY, species.getLatinName());
            }

            outputPath.save(track);
        }
        catch( Exception e )
        {
            outputPath.remove();
            throw e;
        }
        return track;
    }

    public static class SynchronizedTrack implements WritableTrack
    {
        WritableTrack track;
        private final java.util.Map<Thread, List<Site>> sites = new ConcurrentHashMap<>();

        public SynchronizedTrack(WritableTrack track)
        {
            this.track = track;
        }

        @Override
        public void addSite(Site site)
        {
            List<Site> siteList = sites.get(Thread.currentThread());
            if(siteList == null)
            {
                siteList = new ArrayList<>();
                sites.put(Thread.currentThread(), siteList);
            }
            siteList.add(site);
            if(siteList.size() > 10000)
            {
                synchronized(this)
                {
                    for(Site s: siteList)
                        track.addSite(s);
                }
                siteList.clear();
            }
        }

        @Override
        public void finalizeAddition()
        {
            for(List<Site> siteList: sites.values())
            {
                for(Site site: siteList)
                {
                    track.addSite(site);
                }
            }
            sites.clear();
            track.finalizeAddition();
        }

        @Override
        public int countSites(String sequence, int from, int to) throws Exception
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nonnull DataCollection<Site> getAllSites()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Site getSite(String sequence, String siteName, int from, int to) throws Exception
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataCollection<Site> getSites(String sequence, int from, int to)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public TrackViewBuilder getViewBuilder()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths(new String[] {"trackPath", "profilePath"}, parameters.getOutputNames());
    }

    public static void serializeSequencesList(Properties properties, String[] list)
    {
        properties.put(SEQUENCES_LIST_PROPERTY, new JSONArray(Arrays.asList(list)).toString());
    }

    protected static String[] deserializeSequencesList(String value)
    {
        try
        {
            JSONArray jsonArray = new JSONArray(value);
            String[] result = new String[jsonArray.length()];
            for( int i = 0; i < jsonArray.length(); i++ )
                result[i] = jsonArray.getString(i);
            return result;
        }
        catch( JSONException e )
        {
            // Fallback: old format
            return TextUtil.split( value, '/' );
        }
    }
}
