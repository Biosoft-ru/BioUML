package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.CisModule;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;
import biouml.plugins.bindingregions.utils.LinearRegression.LSregression;
import biouml.plugins.bindingregions.utils.SitePrediction;
import gnu.trove.map.TObjectIntMap;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class SiteFiltration extends AnalysisMethodSupport<SiteFiltration.SiteFiltrationParameters>
{
    public SiteFiltration(DataCollection<?> origin, String name)
    {
        super(origin, name, new SiteFiltrationParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Filtration of predicted sites by filters (histone modifications)");
        DataElementPath pathToTrack = parameters.getTrackPath();
        DataElementPath pathToTracks = parameters.getPathToTracks();
        String[] trackNames = parameters.getTrackNames();
        boolean areTracksShuffled = parameters.getAreTracksShuffled();
        DataElementPath pathToTableWithChromosomeGaps = parameters.getChromosomeGapsPath();
        int maximalLengthOfFilters = parameters.getMaximalLengthOfFilters();
        int maximalDistanceToEdge = parameters.getMaximalDistanceToEdge();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // 1.
        log.info("Read track with site predictions and tracks with filters and table with chromosome gaps");
        Track siteTrack = pathToTrack.getDataElement(Track.class);
        List<Track> filterTracks = new ArrayList<>();
        for( String name : trackNames )
            filterTracks.add(pathToTracks.getChildPath(name).getDataElement(Track.class));
        List<Track> shuffledTracks = new ArrayList<>();
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        if( areTracksShuffled )
        {
            final Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(pathToTableWithChromosomeGaps);
            if( chromosomeNameAndGaps.isEmpty() )
                log.warning("No gaps found: check gaps table");
            DataCollectionInfo info = ((DataCollection<?>)siteTrack).getInfo();
            DataElementPath pathToSequences = DataElementPath.create(info.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
            Map<String, Integer> chromosomeAndLength = EnsemblUtils.getChromosomeLengths(pathToSequences);
            for( Track track : filterTracks )
                shuffledTracks.add(SitePrediction.getShuffledTrack(track, pathToOutputs, chromosomeAndLength, chromosomeNameAndGaps));
            filterTracks = shuffledTracks;
        }
        jobControl.setPreparedness(3);
        if( jobControl.isStopped() ) return null;

        // 2.
//      writeTrackWithFilteredSsites(siteTrack, filterTracks, maximalLengthOfFilters, maximalDistanceToEdge, pathToOutputs);
//      getRegression(siteTrack, filterTracks, maximalLengthOfFilters, maximalDistanceToEdge);

        DataElementPath pathToDnases = DataElementPath.create("data/Collaboration/yura_test/Data/GTRD_analysis/Human_Build37/Tracks/DNAse/ENCODE12/Dnase_HeLa-S3");
        getRegression(siteTrack, pathToDnases, filterTracks, maximalLengthOfFilters, maximalDistanceToEdge);
        return pathToOutputs.getDataCollection();
        
        
        /***
        // 3.
        log.info("Create correspondence between histone modifications and tracks");
        Map<String, List<Track>> histoneAndTracks = new HashMap<String, List<Track>>();
        for( Track tr : filterTracks )
        {
            String histone = TextUtil.split( tr.getName(), '_' )[0];
            List<Track> list;
            if( histoneAndTracks.containsKey(histone) )
                list = histoneAndTracks.get(histone);
            else
                list = new ArrayList<Track>();
            list.add(tr);
            histoneAndTracks.put(histone, list);
        }
        
        // 4.
        log.info("Calculate frequencies of histone modification patterns");
        TObjectIntMap<String> frequencies = new TObjectIntHashMap<String>();
        DataCollection<Site> sites = siteTrack.getAllSites();
        int size = sites.getSize();
        int iJobControl = 0;
        for( Site site : sites )
        {
            iJobControl++;
            String s = "";
            for( List<Track> tracks : histoneAndTracks.values() )
            {
                String code = "-";
                if( detectActualSite(site, tracks, maximalLengthOfFilter, maximalDistanceToEdge) )
                    code = "+";
                if( ! s.isEmpty() )
                    s += "_";
                s += code;
            }
            frequencies.adjustOrPutValue(s, 1, 1);
            if( s.contains("+") )
                log.info(" site : chr = " + site.getSequence().getName() + " start = " + site.getFrom() + " end = " + site.getTo() + " pattern = " + s);
            jobControl.setPreparedness(3 + 92 * iJobControl / size);
            if( jobControl.isStopped() ) return null;
        }
        
        // 5.
        log.info("Write table with histone modification patterns");
        TableDataCollection table = writeTable(frequencies, histoneAndTracks, pathToOutput);
        jobControl.setPreparedness(100);
        return table;
        ***/
    }
    
    private void writeTrackWithFilteredSites(Track siteTrack, List<Track> filterTracks, int maximalLengthOfFilters, int maximalDistanceToEdge, DataElementPath pathToOutputs) throws Exception
    {
        SqlTrack track = SqlTrack.createTrack(pathToOutputs.getChildPath(siteTrack.getName() + "_filtered"), siteTrack);
        DataCollection<Site> sites = siteTrack.getAllSites();
        double refinedMean = 0.0;
        int refinedSize = 0;
        double filteredMean = 0.0;
        int size = sites.getSize();
        int iJobControl = 0;
        for( Site site : sites )
        {
            iJobControl++;
            Map<String, Boolean> trackNameAndIndicator = detectFilteredSite(site, filterTracks, maximalLengthOfFilters, maximalDistanceToEdge);
            boolean isCovered = false;
            for( boolean indicator : trackNameAndIndicator.values() )
                if( indicator )
                    isCovered = true;
            if( ! isCovered )
            {
                filteredMean += site.getScore();
                continue;
            }
            DynamicPropertySet dps = site.getProperties();
            for( Entry<String, Boolean> entry : trackNameAndIndicator.entrySet() )
                dps.add(new DynamicProperty(entry.getKey(), Boolean.class, entry.getValue()));
            track.addSite(site);
            refinedMean += site.getScore();
            refinedSize++;
            jobControl.setPreparedness(3 + 97 * iJobControl / size);
            if( jobControl.isStopped() ) return;
        }
        track.finalizeAddition();
        if( refinedSize > 0 )
            refinedMean /= refinedSize;
        if( size > refinedSize)
            filteredMean /= size - refinedSize;
        log.info("Mean score of refined sites = " + refinedMean + "Mean score of filtered sites = " + filteredMean);
        track.getInfo().getProperties().setProperty("Mean score of refined sites", Double.toString(refinedMean));
        track.getInfo().getProperties().setProperty("Mean score of filtered sites", Double.toString(filteredMean));
        String filters = StreamEx.of(filterTracks).map( Track::getName ).joining( CisModule.SEPARATOR_BETWEEN_TFCLASSES );
        track.getInfo().getProperties().setProperty("Filters", filters);
        track.getInfo().getProperties().setProperty("Number of filtered out sites", Integer.toString(size - refinedSize));
        CollectionFactoryUtils.save(track);
    }
    


    // regression on site scores
    private void getRegression(Track siteTrack, List<Track> filterTracks, int maximalLengthOfFilters, int maximalDistanceToEdge) throws Exception
    {
        // 1. Determine data for regression
        int n = siteTrack.getAllSites().getSize();
        double[] scores = new double[n];
        double[][] dataMatrix = new double[n][1 + filterTracks.size()];
        int i = 0;
        for( Site site : siteTrack.getAllSites() )
        {
            scores[i] = site.getScore();
            dataMatrix[i][0] = 1.0;
            Map<String, Boolean> trackNameAndIndicator = detectFilteredSite(site, filterTracks, maximalLengthOfFilters, maximalDistanceToEdge);
            int j = 1;
            for( Boolean indicator : trackNameAndIndicator.values() )
            {
                if( indicator )
                    dataMatrix[i][j] = 1.0;
                else
                    dataMatrix[i][j] = 0.0;
                j++;
            }
            i++;
        }
        
        // 2. Regression
        LSregression lsr = new LSregression(null, null, dataMatrix, null, scores);
        Object[] objects = lsr.getMultipleLinearRegressionByJacobiMethod(300, 0.00001, false);
        double[] coeffs = (double[])objects[0];
        for( int m = 0; m < coeffs.length; m++ )
        {
            String name = "const : ";
            if( m > 0 )
                name = filterTracks.get(m - 1).getName() + " : ";
            log.info(name + coeffs[m]);
        }
        double[] predictions = (double[])objects[2];
        double corr = Stat.pearsonCorrelation(scores, predictions);
        log.info("correlation between scores and predicted scores = " + corr);
        double variance = (double)objects[3];
        double[] meanAndSigma = Stat.getMeanAndSigma(scores);
        double variance0 = meanAndSigma[1] * meanAndSigma[1];
        double ratio = variance0 > 0.0 ? variance / variance0 : -1;
        log.info("variance0 = " + variance0 + " variance = " + variance + " ratio = " + ratio);
    }
    
    // regression on site indicators: indicator = {1 if it is covered byDnase; 0 otherwise}
    private void getRegression(Track siteTrack, DataElementPath pathToDnases, List<Track> filterTracks, int maximalLengthOfFilters, int maximalDistanceToEdge) throws Exception
    {
        // 1. Determine data for regression
        Track dnaseTrack = pathToDnases.getDataElement(Track.class);
        String dnaseTrackName = dnaseTrack.getName();
        List<Track> list = new ArrayList<>();
        list.add(dnaseTrack);
        int n = siteTrack.getAllSites().getSize();
        double[] scores = new double[n];
        double[][] dataMatrix = new double[n][1 + filterTracks.size()];
        int i = 0;
        for( Site site : siteTrack.getAllSites() )
        {
            Map<String, Boolean> dnaseTrackNameAndIndicator = detectFilteredSite(site, list, 1000, 1);
            if( dnaseTrackNameAndIndicator.get(dnaseTrackName) )
                scores[i] = 1.0;
            else scores[i] = 0.0;

            dataMatrix[i][0] = 1.0;
            Map<String, Boolean> trackNameAndIndicator = detectFilteredSite(site, filterTracks, maximalLengthOfFilters, maximalDistanceToEdge);
            int j = 1;
            for( Boolean indicator : trackNameAndIndicator.values() )
            {
                if( indicator )
                    dataMatrix[i][j] = 1.0;
                else
                    dataMatrix[i][j] = 0.0;
                j++;
            }
            i++;
        }
        
        // 2. Regression
        LSregression lsr = new LSregression(null, null, dataMatrix, null, scores);
        Object[] objects = lsr.getMultipleLinearRegressionByJacobiMethod(300, 0.00001, false);
        double[] coeffs = (double[])objects[0];
        for( int m = 0; m < coeffs.length; m++ )
        {
            String name = "const : ";
            if( m > 0 )
                name = filterTracks.get(m - 1).getName() + " : ";
            log.info(name + coeffs[m]);
        }
        double[] predictions = (double[])objects[2];
        double corr = Stat.pearsonCorrelation(scores, predictions);
        log.info("correlation between scores and predicted scores = " + corr);
        double variance = (double)objects[3];
        double[] meanAndSigma = Stat.getMeanAndSigma(scores);
        double variance0 = meanAndSigma[1] * meanAndSigma[1];
        double ratio = variance0 > 0.0 ? variance / variance0 : -1;
        log.info("variance0 = " + variance0 + " variance = " + variance + " ratio = " + ratio);
    }

    
    private TableDataCollection writeTable(TObjectIntMap<String> frequencies, Map<String, List<Track>> histoneAndTracks, DataElementPath pathToTable)
    {
        int numberOfAllSites = 0;
        for( int num : frequencies.values() )
            numberOfAllSites += num;
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        table.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
        for( String histone : histoneAndTracks.keySet() )
            table.getColumnModel().addColumn(histone, DataType.BooleanType.class);
        table.getColumnModel().addColumn("countsOfSites", Integer.class);
        table.getColumnModel().addColumn("percentageOfSites", Double.class);
        int iRow = 0;
        for( String codes : frequencies.keySet() )
        {
            Object[] objects = new Object[histoneAndTracks.size() + 2];
            String[] array = TextUtil.split( codes, '_' );
            for( int i = 0; i < array.length; i++ )
                if( array[i].equals("+") )
                    objects[i] = true;
                else objects[i] = false;
            objects[histoneAndTracks.size()] = frequencies.get(codes);
            objects[histoneAndTracks.size() + 1] = (frequencies.get(codes)) * ((float)100.0) / (numberOfAllSites);
            TableDataCollectionUtils.addRow(table, String.valueOf(++iRow), objects, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }

    private Map<String, Boolean> detectFilteredSite(Site site, List<Track> filterTracks, int maximalLengthOfFilter, int maximalDistanceToEdge) throws Exception
    {
        Map<String, Boolean> result = new HashMap<>();
        String chromosome = site.getSequence().getName();
        Interval interval = site.getInterval();
        int siteCenter = interval.getCenter();
        for( Track filterTrack : filterTracks )
        {
            boolean isCoveredByFilter = false;
            for( Site filterSite : filterTrack.getSites(chromosome, interval.getFrom(), interval.getTo()) )
            {
                if( filterSite.getLength() <= maximalLengthOfFilter )
                {
                    isCoveredByFilter = true;
                    break;
                }
                if( Math.abs(siteCenter - filterSite.getFrom()) <= maximalDistanceToEdge )
                {
                    isCoveredByFilter = true;
                    break;
                }
                if( Math.abs(siteCenter - filterSite.getTo()) <= maximalDistanceToEdge )
                {
                    isCoveredByFilter = true;
                    break;
                }
            }
            result.put(filterTrack.getName(), isCoveredByFilter);
        }
        return result;
    }
    
    private boolean detectFilteredSite(Site site, Track filterTrack, int maximalLengthOfFilter, int maximalDistanceToEdge) throws Exception
    {
        String chromosome = site.getSequence().getName();
        Interval interval = site.getInterval();
        int siteCenter = interval.getCenter();
        for( Site filterSite : filterTrack.getSites(chromosome, interval.getFrom(), interval.getTo()) )
        {
            if( filterSite.getLength() <= maximalLengthOfFilter ) return true;
            if( Math.abs(siteCenter - filterSite.getFrom()) <= maximalDistanceToEdge ) return true;
            if( Math.abs(siteCenter - filterSite.getTo()) <= maximalDistanceToEdge ) return true;
        }
        return false;
    }

    
    
    public static class SiteFiltrationParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
        private DataElementPath pathToTracks;
        private String[] trackNames;
        private boolean areTracksShuffled;
        private DataElementPath chromosomeGapsPath;
        private int maximalLengthOfFilters = 500;
        private int maximalDistanceToEdge = 400;
        private DataElementPath outputPath;
        
        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_SITE_PREDICTIONS)
        public DataElementPath getTrackPath()
        {
            return trackPath;
        }
        public void setTrackPath(DataElementPath trackPath)
        {
            Object oldValue = this.trackPath;
            this.trackPath = trackPath;
            firePropertyChange("trackPath", oldValue, trackPath);
        }
        
        @PropertyName(MessageBundle.PN_TRACKS_FOLDER)
        @PropertyDescription(MessageBundle.PD_TRACKS_FOLDER)
        public DataElementPath getPathToTracks()
        {
            return pathToTracks;
        }
        public void setPathToTracks(DataElementPath pathToTracks)
        {
            Object oldValue = this.pathToTracks;
            this.pathToTracks = pathToTracks;
            firePropertyChange("pathToTracks", oldValue, pathToTracks);
        }
        
        @PropertyName(MessageBundle.PN_TRACK_NAMES)
        @PropertyDescription(MessageBundle.PD_TRACK_NAMES)
        public String[] getTrackNames()
        {
            return trackNames;
        }
        public void setTrackNames(String[] trackNames)
        {
            Object oldValue = this.trackNames;
            this.trackNames = trackNames;
            firePropertyChange("trackNames", oldValue, trackNames);
        }

        @PropertyName(MessageBundle.PN_ARE_TRACKS_SHUFFLED)
        @PropertyDescription(MessageBundle.PD_ARE_TRACKS_SHUFFLED)
        public boolean getAreTracksShuffled()
        {
            return areTracksShuffled;
        }
        public void setAreTracksShuffled(boolean areTracksShuffled)
        {
            Object oldValue = this.areTracksShuffled;
            this.areTracksShuffled = areTracksShuffled;
            firePropertyChange("areTracksShuffled", oldValue, areTracksShuffled);
            firePropertyChange("*", null, null);
        }
        
        @PropertyName(MessageBundle.PN_CHROMOSOME_GAPS_TABLE)
        @PropertyDescription(MessageBundle.PD_CHROMOSOME_GAPS_TABLE)
        public DataElementPath getChromosomeGapsPath()
        {
            return chromosomeGapsPath;
        }
        public void setChromosomeGapsPath(DataElementPath chromosomeGapsPath)
        {
            Object oldValue = this.chromosomeGapsPath;
            this.chromosomeGapsPath = chromosomeGapsPath;
            firePropertyChange("chromosomeGapsPath", oldValue, chromosomeGapsPath);
        }
        public boolean isChromosomeGapsPathHidden()
        {
            return ! getAreTracksShuffled();
        }
        
        @PropertyName(MessageBundle.PN_MAX_LENGTH_OF_FILTERS)
        @PropertyDescription(MessageBundle.PD_MAX_LENGTH_OF_FILTERS)
        public int getMaximalLengthOfFilters()
        {
            return maximalLengthOfFilters;
        }
        public void setMaximalLengthOfFilters(int maximalLengthOfFilters)
        {
            Object oldValue = this.maximalLengthOfFilters;
            this.maximalLengthOfFilters = maximalLengthOfFilters;
            firePropertyChange("maximalLengthOfFilters", oldValue, maximalLengthOfFilters);
        }

        @PropertyName(MessageBundle.PN_MAX_DISTANCE_TO_EDGE)
        @PropertyDescription(MessageBundle.PD_MAX_DISTANCE_TO_EDGE)
        public int getMaximalDistanceToEdge()
        {
            return maximalDistanceToEdge;
        }
        public void setMaximalDistanceToEdge(int maximalDistanceToEdge)
        {
            Object oldValue = this.maximalDistanceToEdge;
            this.maximalDistanceToEdge = maximalDistanceToEdge;
            firePropertyChange("maximalDistanceToEdge", oldValue, maximalDistanceToEdge);
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }
    }
    
    public static class TrackNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataElementPath dep = ((SiteFiltrationParameters)getBean()).getPathToTracks();
                return dep.getDataCollection().stream( Track.class )
                		.map( Track::getName ).sorted().toArray( String[]::new );
            }
            catch( Exception e )
            {
                return new String[] {"(please select tracks)"};
            }
        }
    }
    
    public static class SiteFiltrationParametersBeanInfo extends BeanInfoEx2<SiteFiltrationParameters>
    {
        public SiteFiltrationParametersBeanInfo()
        {
            super(SiteFiltrationParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            add(DataElementPathEditor.registerInputChild("pathToTracks", beanClass, Track.class));
            add("trackNames", TrackNamesSelector.class);
            add("areTracksShuffled");
            property( "chromosomeGapsPath" ).inputElement( TableDataCollection.class ).hidden( "isChromosomeGapsPathHidden" ).add();
            add("maximalLengthOfFilters");
            add("maximalDistanceToEdge");
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}