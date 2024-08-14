package ru.biosoft.bsa.analysis.ipsmodule;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.exception.ExceptionRegistry;

@ClassIcon( "resources/construct_IPS_cis_module.gif" )
public class IPSModule extends AnalysisMethodSupport<IPSModuleParameters>
{
    public static final String IPS_MODULE_SITE_COUNT_PROPERTY = "siteCount";
    public static final String IPS_MODULE_MODELS_PROPERTY = "models";

    public IPSModule(DataCollection origin, String name)
    {
        super(origin, name, new IPSModuleParameters());
    }

    @Override
    public SqlTrack justAnalyzeAndPut() throws Exception
    {
        final SqlTrack moduleTrack = createModuleTrack();

        jobControl.forCollection(getChromosomes(), chr -> {
            try
            {
                findModules(chr, moduleTrack);
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException(e);
            }
            return true;
        });

        moduleTrack.finalizeAddition();
        CollectionFactoryUtils.save( moduleTrack );
        return moduleTrack;
    }

    private void findModules(Sequence seq, WritableTrack result) throws Exception
    {
        BindingSite[] bindingSites = getBindingSites(seq.getName());
        if(bindingSites.length == 0)
            return;

        int chrLength = seq.getLength();
        TrackWriter trackWriter = new TrackWriter(result);

        int windowSize = parameters.getWindowSize();

        //Position of window start in the chromosome
        //Initialized to be the left most window containing first site
        int windowPos = bindingSites[0].position - windowSize + 1;
        if(windowPos < 1)
            windowPos = 1;

        //indexes of left most site and right most sites in the current window
        int leftSiteIndex = 0, rightSiteIndex = 0;
        while(rightSiteIndex + 1 < bindingSites.length && bindingSites[rightSiteIndex + 1].position < bindingSites[0].position)
            rightSiteIndex++;

        for(;;)
        {
            ModuleStats stats = new ModuleStats(bindingSites, leftSiteIndex, rightSiteIndex);
            if( stats.accept() )
            {
                DynamicPropertySet properties = new DynamicPropertySetAsMap();
                properties.add(new DynamicProperty(Site.SCORE_PD, Double.class, stats.averageScore));
                properties.add(new DynamicProperty(IPS_MODULE_SITE_COUNT_PROPERTY, Integer.class, stats.models.size()));
                properties.add(new DynamicProperty(IPS_MODULE_MODELS_PROPERTY, String.class, String.join(",", stats.models)));
                Site site = new SiteImpl(null, null, SiteType.TYPE_MISC_FEATURE, Basis.BASIS_PREDICTED, bindingSites[leftSiteIndex].position,
                        bindingSites[rightSiteIndex].position - bindingSites[leftSiteIndex].position + 1, Precision.PRECISION_EXACTLY,
                        Site.STRAND_NOT_APPLICABLE, seq, null, properties);
                trackWriter.writeSite(site);
            }

            //Shift to remove left most site from window
            int leftShift = bindingSites[leftSiteIndex].position - windowPos + 1;
            //Shift to add one site at the right
            int rightShift = rightSiteIndex + 1 < bindingSites.length ? bindingSites[rightSiteIndex + 1].position - (windowPos + windowSize - 1) : Integer.MAX_VALUE;
            //Move window so that it is the left most window among windows with the same set of sites
            windowPos += Math.min(leftShift, rightShift);

            if(windowPos + windowSize - 1 > chrLength)// window crosses right bound
                break;

            //Move left and right site indexes
            while(leftSiteIndex < bindingSites.length && bindingSites[leftSiteIndex].position < windowPos)
                leftSiteIndex++;
            if(leftSiteIndex >= bindingSites.length)// no more sites
                break;
            rightSiteIndex = Math.max(leftSiteIndex, rightSiteIndex);
            while(rightSiteIndex + 1 < bindingSites.length && bindingSites[rightSiteIndex + 1].position < windowPos + windowSize)
                rightSiteIndex++;
        }

        trackWriter.close();
    }

    private class ModuleStats
    {
        public final double averageScore;
        public final Set<String> models;

        public ModuleStats(BindingSite[] bindingSites, int start, int end)
        {
            Map<String, Double> nameToScore = new HashMap<>();
            for( int i = start; i <= end; i++ )
            {
                Double score = nameToScore.get(bindingSites[i].modelName);
                if( score == null )
                    score = bindingSites[i].score;
                nameToScore.put(bindingSites[i].modelName, Math.max(score, bindingSites[i].score));
            }

            /*
            List<Double> scores = new ArrayList<Double>(nameToScore.values());
            Collections.sort(scores, Collections.reverseOrder());
            double averageScore = 0;
            int n = Math.min(parameters.getMinSites(), scores.size());
            for( int i = 0; i < n; i++)
                averageScore += scores.get(i);
            averageScore /= n;
            */

            double averageScore = 0;
            for(double s : nameToScore.values())
                averageScore += s;
            averageScore /= nameToScore.size();

            this.averageScore = averageScore;

            models = nameToScore.keySet();

        }

        public boolean accept()
        {
            return models.size() >= parameters.getMinSites() && averageScore >= parameters.getMinAverageScore();
        }
    }

    private static class TrackWriter
    {
        private final WritableTrack track;
        private Site lastSite;

        public TrackWriter(WritableTrack track)
        {
            this.track = track;
        }

        public void writeSite(Site site) throws Exception
        {
            if( lastSite == null )
            {
                lastSite = site;
                return;
            }
            if( lastSite.getTo() >= site.getFrom() )
            {
                double lastScore = lastSite.getScore();
                double score = site.getScore();
                if( score > lastScore )
                    lastSite = site;
            }
            else
            {
                track.addSite(lastSite);
                lastSite = site;
            }
        }

        public void close() throws Exception
        {
            if( lastSite != null )
                track.addSite(lastSite);
            lastSite = null;
        }
    }

    private SqlTrack createModuleTrack() throws Exception
    {
        DataElementPath moduleTrackPath = parameters.getModuleTrack();
        if( moduleTrackPath.exists() )
            moduleTrackPath.remove();
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, moduleTrackPath.getName());
        properties.put(Track.SEQUENCES_COLLECTION_PROPERTY, parameters.getSiteTrack().getInfo().getProperty(
                Track.SEQUENCES_COLLECTION_PROPERTY));
        SqlTrack track = new SqlTrack(moduleTrackPath.optParentCollection(), properties);
        return track;
    }

    private List<Sequence> getChromosomes() throws Exception
    {
        List<Sequence> result = new ArrayList<>();

        SqlTrack track = parameters.getSiteTrack();
        DataCollection<AnnotatedSequence> sequenceCollection = TrackUtils.getTrackSequencesPath(track).getDataCollection(
                AnnotatedSequence.class);

        for(AnnotatedSequence as: sequenceCollection)
        {
            result.add(as.getSequence());
        }

        return result;
    }

    /**
     * Return binding sites for chromosome sorted by site centers in ascending order
     */
    private BindingSite[] getBindingSites(String chr) throws SQLException
    {
        List<BindingSite> result = new ArrayList<>();

        SqlTrack track = parameters.getSiteTrack();
        String[] modelNames = parameters.getSiteModels();


        String sql = "SELECT (start+end) DIV 2 x, prop_siteModel, prop_score FROM " + track.getTableId()
                   + " WHERE chrom=? AND prop_siteModel IN ("
                   + String.join(",", Collections.nCopies(modelNames.length, "?"))
                   + ") order by x";

        try (PreparedStatement st = track.getConnection().prepareStatement( sql ))
        {
            st.setString( 1, chr );
            for( int i = 0; i < modelNames.length; i++ )
                st.setString( i + 2, modelNames[i] );
            try (ResultSet rs = st.executeQuery())
            {
                while( rs.next() )
                    result.add( new BindingSite( rs.getInt( 1 ), rs.getString( 2 ), rs.getDouble( 3 ) ) );
            }
        }

        return result.toArray(new BindingSite[result.size()]);
    }
    private static class BindingSite
    {
        final int position;
        final String modelName;
        final double score;

        public BindingSite(int position, String modelName, double score)
        {
            super();
            this.position = position;
            this.modelName = modelName;
            this.score = score;
        }
    }
}
