package ru.biosoft.bsa.analysis.sitecountsinrepeats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author yura
 * Read all available predicted binding sites from track.
 * Read all available repeats (satellites) from another track.
 * Count the binding sites that are covered by particular types of repeats (the counts are in table).
 * Count the binding sites that are covered by arbitrary types of repeats (the counts are in table2).
 * The table2  is estimated approximately because table2 is derived from table while
 * some repeats (small number) are overlapped.
 */
public class SiteCountsInRepeats extends AnalysisMethodSupport<SiteCountsInRepeatsParameters>
{
    public SiteCountsInRepeats(DataCollection origin, String name)
    {
        super(origin, name, new SiteCountsInRepeatsParameters());
    }
    
    private static class BindingSite implements Comparable<BindingSite>
    {
        final int position;
        final String siteType;
        final double siteScore;
        
        public BindingSite(int position, String siteType, double siteScore)
        {
            this.position = position;
            this.siteType = siteType;
            this.siteScore = siteScore;
        }

        public int getBindingSitePosition()
        {
            return position;
        }

        public String getBindingSiteType()
        {
            return siteType;
        }
        public double getBindingSiteScore()
        {
            return siteScore;
        }

        @Override
        public int compareTo(BindingSite o)
        {
            return position - o.position;
        }
    }
    
    // Return  all available binding sites on given chromosome chr
    private List<BindingSite> getBindingSites (String chr) throws SQLException
    {
        List<BindingSite> result = new ArrayList<>();
        SqlTrack track = parameters.getSiteTrack();
        String sql = "SELECT (start+end) DIV 2 x, prop_siteModel, prop_score FROM " + track.getTableId() + " WHERE chrom=? ORDER BY x";
        try (PreparedStatement st = track.getConnection().prepareStatement( sql ))
        {
            st.setString( 1, chr );
            try (ResultSet rs = st.executeQuery())
            {
                while( rs.next() )
                    result.add( new BindingSite( rs.getInt( 1 ), rs.getString( 2 ), rs.getDouble( 3 ) ) );
            }
        }
        return result;
    }

    static class SiteCount
    {
        static final double IPS_THRESHOLD_WEAK = 4.0;
        static final double IPS_THRESHOLD_MODERATE = 5.0;
        static final double IPS_THRESHOLD_STRONG = 6.0;
        int weakSiteCounts, moderateSiteCounts, strongSiteCounts;
        
        public void addSiteCount (double siteScore)
        {
           if (siteScore >= IPS_THRESHOLD_WEAK) weakSiteCounts++;
           if (siteScore >= IPS_THRESHOLD_MODERATE) moderateSiteCounts++;
           if (siteScore >= IPS_THRESHOLD_STRONG) strongSiteCounts++;
        }
        
        public void addSiteCount (int weakSiteCounts, int moderateSiteCounts, int strongSiteCounts)
        {
            this.weakSiteCounts += weakSiteCounts;
            this.moderateSiteCounts += moderateSiteCounts;
            this.strongSiteCounts += strongSiteCounts;
        }

        public int getWeakSiteCounts()
        {
            return weakSiteCounts;
        }

        public int getModerateSiteCounts()
        {
            return moderateSiteCounts;
        }
        
        public int getStrongSiteCounts()
        {
            return strongSiteCounts;
        }
    }
    
     private Map<String, SiteCount> countAllBindingSites()
    {
        Map<String, SiteCount> result = new HashMap<>();
        
        Track siteTrack = parameters.getSiteTrack();
        DataCollection<Site> allSites = siteTrack.getAllSites();
        for(Site s: allSites)
        {
            SiteModel model = (SiteModel)s.getProperties().getValue(SiteModel.SITE_MODEL_PROPERTY);
            double score = s.getScore();
            result.computeIfAbsent(model.getName(), k -> new SiteCount()).addSiteCount(score);
        }
        return result;
    }
    
    private Map<String, Map<String, SiteCount>> countBindingSitesInRepeats() throws Exception
    {
        SqlTrack siteTrack = parameters.getSiteTrack();
        ru.biosoft.access.core.DataElementPath[] chrPaths = TrackUtils.getTrackSequencesPath(siteTrack).getChildrenArray();
        Map<String, Map<String, SiteCount>> counts = new HashMap<>();
        int chrsProcessed = 0;
        for (DataElementPath chrPath : chrPaths)
        {
            Track repeatTrack = getRepeatTrack();
            DataCollection<Site> repeatCollection = repeatTrack.getSites(chrPath.toString(), 0, Integer.MAX_VALUE);
            List<BindingSite> bindingSites = getBindingSites(chrPath.getName());
            for(Site s: repeatCollection)
            {
                int siteIndex = Collections.binarySearch(bindingSites, new BindingSite(s.getFrom(), null, 0));
                if (siteIndex >= 0)
                    while(siteIndex > 0 && bindingSites.get(siteIndex - 1).getBindingSitePosition() == s.getFrom())
                        siteIndex--;
                else
                    siteIndex = -siteIndex - 1;
                
                for (int i = siteIndex; i < bindingSites.size(); i++)
                {
                    BindingSite bs = bindingSites.get(i);
                    int position = bs.getBindingSitePosition();
                    if (position > s.getTo())
                        break;
                    String repeatType = s.getType();
                    String siteType = bs.getBindingSiteType();
                    counts.computeIfAbsent(repeatType, k -> new HashMap<>())
                            .computeIfAbsent(siteType, k -> new SiteCount()).addSiteCount(bs.getBindingSiteScore());
                }
            }
            chrsProcessed++;
            getJobControl().setPreparedness(chrsProcessed*100/chrPaths.length);
        }
        return counts;
    }
    
    private Track getRepeatTrack()
    {
        return DataElementPath.create("databases/EnsemblHuman64_37/Tracks/RepeatTrack").getDataElement(Track.class);
    }

    @Override
    public TableDataCollection[] justAnalyzeAndPut() throws Exception
    {
        log.info("Site counts in repeats analysis started");
        Map<String, Map<String, SiteCount>> countsInRepeats = countBindingSitesInRepeats();
        log.info("Counts in repeats are ready, counting all binding sites");
        Map<String, SiteCount> countsAll = countAllBindingSites();
        log.info("All binding sites are ready");
        // Construction of 1-st table
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getSiteCountsInRepeatsTable());
        table.getColumnModel().addColumn("SiteType", String.class);
        table.getColumnModel().addColumn("RepeatType", String.class);
        table.getColumnModel().addColumn("FrequencyOfWeakSites", Integer.class);
        table.getColumnModel().addColumn("PercentageOfWeakSites", Double.class);
        table.getColumnModel().addColumn("FrequencyOfModerateSites", Integer.class);
        table.getColumnModel().addColumn("PercentageOfModerateSites", Double.class);
        table.getColumnModel().addColumn("FrequencyOfStrongSites", Integer.class);
        table.getColumnModel().addColumn("PercentageOfStrongSites", Double.class);
        int iRow = 0;
        for( Map.Entry<String, Map<String, SiteCount>> countInRepeatsEntry : countsInRepeats.entrySet() )
        {
            for( Map.Entry<String, SiteCount> countsBySiteTypeEntry : countInRepeatsEntry.getValue().entrySet() )
            {
                String siteType = countsBySiteTypeEntry.getKey();
                int weakSiteCountsInRepeats = countsBySiteTypeEntry.getValue().getWeakSiteCounts();
                int moderateSiteCountsInRepeats = countsBySiteTypeEntry.getValue().getModerateSiteCounts();
                int strongSiteCountsInRepeats = countsBySiteTypeEntry.getValue().getStrongSiteCounts();
                SiteCount siteCount = countsAll.get(siteType);
                int weakSiteCountsAll = siteCount.getWeakSiteCounts();
                int moderateSiteCountsAll = siteCount.getModerateSiteCounts();
                int strongSiteCountsAll = siteCount.getStrongSiteCounts();
                double percentageOfWeakSiteCountsInRepeats = weakSiteCountsAll == 0 ? 0 : 100.0 * weakSiteCountsInRepeats / weakSiteCountsAll;
                double percentageOfModerateSiteCountsInRepeats = moderateSiteCountsAll == 0 ? 0 : 100.0 * moderateSiteCountsInRepeats / moderateSiteCountsAll;
                double percentageOfStrongSiteCountsInRepeats = strongSiteCountsAll == 0 ? 0 : 100.0 * strongSiteCountsInRepeats / strongSiteCountsAll;
                TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {siteType, countInRepeatsEntry.getKey(),
                        weakSiteCountsInRepeats, percentageOfWeakSiteCountsInRepeats, moderateSiteCountsInRepeats,
                        percentageOfModerateSiteCountsInRepeats, strongSiteCountsInRepeats, percentageOfStrongSiteCountsInRepeats});
            }
        }
        table.getOrigin().put(table);
        // Construction of 2-nd table
        TableDataCollection table2 = TableDataCollectionUtils.createTableDataCollection(parameters.getSummaryTablePath());
        table2.getColumnModel().addColumn("SiteType", String.class);
        table2.getColumnModel().addColumn("FrequencyOfWeakSites", Integer.class);
        table2.getColumnModel().addColumn("PercentageOfWeakSites", Double.class);
        table2.getColumnModel().addColumn("FrequencyOfModerateSites", Integer.class);
        table2.getColumnModel().addColumn("PercentageOfModerateSites", Double.class);
        table2.getColumnModel().addColumn("FrequencyOfStrongSites", Integer.class);
        table2.getColumnModel().addColumn("PercentageOfStrongSites", Double.class);
        Map<String, SiteCount> forTable2 = new HashMap<>();
        for( Map<String, SiteCount> countsBySiteType : countsInRepeats.values() )
        {
            for( Map.Entry<String, SiteCount> entry : countsBySiteType.entrySet() )
            {
                String siteType = entry.getKey();
                if (!forTable2.containsKey(siteType))
                    forTable2.put(siteType, new SiteCount());
                SiteCount siteCount = entry.getValue();
                int weakSiteCountsInRepeats = siteCount.getWeakSiteCounts();
                int moderateSiteCountsInRepeats = siteCount.getModerateSiteCounts();
                int strongSiteCountsInRepeats = siteCount.getStrongSiteCounts();
                forTable2.get(siteType).addSiteCount(weakSiteCountsInRepeats, moderateSiteCountsInRepeats, strongSiteCountsInRepeats);
            }
        }
        iRow = 0;
        for( Map.Entry<String, SiteCount> entry : forTable2.entrySet() )
        {
            String siteType = entry.getKey();
            SiteCount value = entry.getValue();
            int weakSiteCountsInRepeats = value.getWeakSiteCounts();
            int moderateSiteCountsInRepeats = value.getModerateSiteCounts();
            int strongSiteCountsInRepeats = value.getStrongSiteCounts();
            SiteCount siteCount = countsAll.get(siteType);
            int weakSiteCountsAll = siteCount.getWeakSiteCounts();
            int moderateSiteCountsAll = siteCount.getModerateSiteCounts();
            int strongSiteCountsAll = siteCount.getStrongSiteCounts();
            double percentageOfWeakSiteCountsInRepeats = weakSiteCountsAll == 0 ? 0 : 100.0 * weakSiteCountsInRepeats / weakSiteCountsAll;
            double percentageOfModerateSiteCountsInRepeats = moderateSiteCountsAll == 0 ? 0 : 100.0 * moderateSiteCountsInRepeats / moderateSiteCountsAll;
            double percentageOfStrongSiteCountsInRepeats = strongSiteCountsAll == 0 ? 0 : 100.0 * strongSiteCountsInRepeats / strongSiteCountsAll;
            TableDataCollectionUtils.addRow(table2, String.valueOf(iRow++), new Object[]{siteType, weakSiteCountsInRepeats, percentageOfWeakSiteCountsInRepeats, moderateSiteCountsInRepeats, percentageOfModerateSiteCountsInRepeats, strongSiteCountsInRepeats, percentageOfStrongSiteCountsInRepeats});
        }
        table2.getOrigin().put(table2);
        return new TableDataCollection[] {table, table2};
    }
 }