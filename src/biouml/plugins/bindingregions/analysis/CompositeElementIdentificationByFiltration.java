package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.CompositeElement;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;
import biouml.plugins.bindingregions.utils.SitePrediction;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author yura
 *
 */
public class CompositeElementIdentificationByFiltration extends AnalysisMethodSupport<CompositeElementIdentificationByFiltration.CompositeElementIdentificationByFiltrationParameters>
{
    public CompositeElementIdentificationByFiltration(DataCollection<?> origin, String name)
    {
        super(origin, name, new CompositeElementIdentificationByFiltrationParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Identification of composite elements by filtration (in particular, with the help of histone modifications)");
        DataElementPath pathToTrack1 = parameters.getTrackPath();
        DataElementPath pathToTrack2 = parameters.getTrackPath2();
        DataElementPath pathToFilterTrack = parameters.getPathToFilterTrack();
        boolean isFilterTrackShuffled = parameters.getIsFilterTrackShuffled();
        DataElementPath pathToTableWithChromosomeGaps = parameters.getChromosomeGapsPath();
        int maximalLengthOfFilters = parameters.getMaximalLengthOfFilters();
        int maximalDistanceToEdge = parameters.getMaximalDistanceToEdge();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // 1.
        log.info("Read two tracks with predicted sites of two transcription factors and filtration track and table with hromosome gaps");
        Track siteTrack1 = pathToTrack1.getDataElement(Track.class);
        Track siteTrack2 = pathToTrack2.getDataElement(Track.class);
        Track filterTrack = pathToFilterTrack.getDataElement(Track.class);
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        if( isFilterTrackShuffled )
        {
            final Map<String, List<Gap>> chromosomeNameAndGaps = EnsemblUtils.readChromosomeNameAndGapsInTable(pathToTableWithChromosomeGaps);
            if( chromosomeNameAndGaps.isEmpty() )
                log.warning("No gaps found: check gaps table");
            DataCollectionInfo info = ((DataCollection<?>)siteTrack1).getInfo();
            DataElementPath pathToSequences = DataElementPath.create(info.getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
            Map<String, Integer> chromosomeAndLength = EnsemblUtils.getChromosomeLengths(pathToSequences);
            filterTrack = SitePrediction.getShuffledTrack(filterTrack, pathToOutputs, chromosomeAndLength, chromosomeNameAndGaps);
        }
        jobControl.setPreparedness(3);
        if( jobControl.isStopped() ) return null;
        
        // 2.
        log.info("Create and write track with CEs (Composite elements)");
        SqlTrack track = SqlTrack.createTrack(pathToOutputs.getChildPath("CE_" + siteTrack1.getName() + "_" + siteTrack2.getName()), siteTrack1);
        DataCollection<Site> sites = siteTrack1.getAllSites();
        int size = sites.getSize();
        int iJobControl = 0;
        for( Site site : sites )
        {
            iJobControl++;
            List<Site> fittedFilters = getFittedFilters(site, filterTrack, maximalLengthOfFilters, maximalDistanceToEdge);
            List<CompositeElement> ces = CompositeElement.getDistinctCompositeElements(site, fittedFilters, siteTrack2, maximalLengthOfFilters, maximalDistanceToEdge);
            if( ces == null || ces.isEmpty() ) continue;
            for( CompositeElement ce : ces )
            {
                Site site1 = ce.getFirstSite();
                Site site2 = ce.getSecondSite();
                Site newSite = new SiteImpl(null, site1.getName(), site1.getType(), Site.BASIS_PREDICTED, site1.getStart(), site1.getLength(), Site.PRECISION_NOT_KNOWN, site1.getStrand(), site1.getSequence(), null);
                DynamicPropertySet dps = newSite.getProperties();
                dps.add(new DynamicProperty(Site.SCORE_PD, Float.class, site1.getScore()));
                DynamicPropertySet properties = site1.getProperties();
                Float commonScore = (Float)properties.getValue(IPSSiteModel.COMMON_SCORE_PROPERTY);
                if( commonScore != null )
                    dps.add(new DynamicProperty(IPSSiteModel.COMMON_SCORE_PROPERTY, Float.class, commonScore));
                dps.add(new DynamicProperty("secondSiteType", String.class, site2.getType()));
                dps.add(new DynamicProperty("secondSiteFrom", Integer.class, site2.getFrom()));
                dps.add(new DynamicProperty("secondSiteLength", Integer.class, site2.getLength()));
                dps.add(new DynamicProperty("secondSiteStrand", Integer.class, site2.getStrand()));
                dps.add(new DynamicProperty("secondSiteScore", Float.class, (float)(site2.getScore())));
                DynamicPropertySet properties2 = site2.getProperties();
                Float commonScore2 = (Float)properties2.getValue(IPSSiteModel.COMMON_SCORE_PROPERTY);
                if( commonScore2 != null )
                    dps.add(new DynamicProperty(IPSSiteModel.COMMON_SCORE_PROPERTY, Float.class, commonScore2));
                track.addSite(newSite);
            }
            jobControl.setPreparedness(3 + 97 * iJobControl / size);
            if( jobControl.isStopped() ) return null;
        }
        track.finalizeAddition();
        CollectionFactoryUtils.save(track);
        return pathToOutputs.getDataCollection();
    }

    private static List<Site> getFittedFilters(Site site, Track filterTrack, int maximalLengthOfFilter, int maximalDistanceToEdge) throws Exception
    {
        List<Site> result = new ArrayList<>();
        String chromosome = site.getSequence().getName();
        Interval interval = site.getInterval();
        int siteCenter = interval.getCenter();
        for( Site filterSite : filterTrack.getSites(chromosome, interval.getFrom(), interval.getTo()) )
            if( filterSite.getLength() <= maximalLengthOfFilter ||
                Math.abs(siteCenter - filterSite.getFrom()) <= maximalDistanceToEdge ||
                Math.abs(siteCenter - filterSite.getTo()) <= maximalDistanceToEdge
              )
                result.add(filterSite);
        return result;
    }
    
    public static class CompositeElementIdentificationByFiltrationParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
        private DataElementPath trackPath2;
        DataElementPath pathToFilterTrack;
        private boolean isFilterTrackShuffled;
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
        
        @PropertyName(MessageBundle.PN_TRACK_PATH_SECOND)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_SECOND_SITE_PREDICTIONS)
        public DataElementPath getTrackPath2()
        {
            return trackPath2;
        }
        public void setTrackPath2(DataElementPath trackPath2)
        {
            Object oldValue = this.trackPath2;
            this.trackPath2 = trackPath2;
            firePropertyChange("trackPath2", oldValue, trackPath2);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FILTER_TRACK)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FILTER_TRACK)
        public DataElementPath getPathToFilterTrack()
        {
            return pathToFilterTrack;
        }
        public void setPathToFilterTrack(DataElementPath pathToFilterTrack)
        {
            Object oldValue = this.pathToFilterTrack;
            this.pathToFilterTrack = pathToFilterTrack;
            firePropertyChange("pathToFilterTrack", oldValue, pathToFilterTrack);
        }
        
        @PropertyName(MessageBundle.PN_IS_FILTER_TRACK_SHUFFLED)
        @PropertyDescription(MessageBundle.PD_IS_FILTER_TRACK_SHUFFLED)
        public boolean getIsFilterTrackShuffled()
        {
            return isFilterTrackShuffled;
        }
        public void setIsFilterTrackShuffled(boolean isFilterTrackShuffled)
        {
            Object oldValue = this.isFilterTrackShuffled;
            this.isFilterTrackShuffled = isFilterTrackShuffled;
            firePropertyChange("isFilterTrackShuffled", oldValue, isFilterTrackShuffled);
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
            return ! getIsFilterTrackShuffled();
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
    
    public static class CompositeElementIdentificationByFiltrationParametersBeanInfo
            extends BeanInfoEx2<CompositeElementIdentificationByFiltrationParameters>
    {
        public CompositeElementIdentificationByFiltrationParametersBeanInfo()
        {
            super(CompositeElementIdentificationByFiltrationParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            property( "trackPath2" ).inputElement( Track.class ).add();
            property( "pathToFilterTrack" ).inputElement( Track.class ).add();
            add("isFilterTrackShuffled");
            property( "chromosomeGapsPath" ).inputElement( TableDataCollection.class ).hidden( "isChromosomeGapsPathHidden" ).add();
            add("maximalLengthOfFilters");
            add("maximalDistanceToEdge");
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
