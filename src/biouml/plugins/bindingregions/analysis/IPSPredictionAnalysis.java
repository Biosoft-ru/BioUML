package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.IPSPrediction;
import biouml.plugins.bindingregions.utils.SitePrediction;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * @author lan
 * Extracted from BindingRegions/mode18
 * "18. IPS-prediction and tag density"
 */
public class IPSPredictionAnalysis extends AnalysisMethodSupport<IPSPredictionAnalysis.IPSPredictionAnalysisParameters>
{
    public IPSPredictionAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new IPSPredictionAnalysisParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("IPS-prediction and tag density");
        int minimalLengthOfSequenceRegion = 300;
        double[] ipsThresholds = new double[] {4.0, 4.5, 5.0, 5.5, 6.0};
        int nThresholds = ipsThresholds.length;
        int numberOfGroups = 20;
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        DataElementPath pathToInputTracks = parameters.getChipSeqTracksPath();
        Species givenSpecie = parameters.getSpecies();
        String givenTfClass = parameters.getTfClass();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        
        log.info("givenTfClass = " + givenTfClass);
        IPSSiteModel ipsSiteModel = IPSPrediction.getIpsSiteModel(pathToMatrix);

        // read trackInfors
        log.info("Read trackInfos");
        List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(pathToInputTracks, givenSpecie, pathToSequences);
        List<TrackInfo> selectedTrackInfos = new ArrayList<>();
        for( TrackInfo trackInfo : trackInfos )
        {
            String tfClass = trackInfo.getTfClass();
            String specie = trackInfo.getSpecie();
            String pathToSequenceCollection = trackInfo.getPathToSequenceCollection();
            if( tfClass.equals(givenTfClass) && specie.equals(givenSpecie.getLatinName()) && pathToSequenceCollection.equals(pathToSequences.toString()) )
                selectedTrackInfos.add(trackInfo);
        }
        if( selectedTrackInfos.isEmpty() )
        {
            log.info("There are no suitable tracks");
            return null;
        }
        log.info("There are " + selectedTrackInfos.size() + " suitable tracks");
        
        // determination of individual charts
        Map<String, Chart> namesAndCharts = new HashMap<>();
        for( TrackInfo trackInfo : selectedTrackInfos )
        {
            String trackName = trackInfo.getTrackName();
            log.info("trackName = " + trackName);
            DataElementPath pathToTrack = pathToInputTracks.getChildPath(trackName);
            Track track = pathToTrack.getDataElement(Track.class);
            
            // identification of maximal scores
            Map<Integer, Map<String, List<BindingRegion>>> tagsAndBindingRegions = readTagDensityAndBindingRegionsFromTrack(track, givenTfClass);
            log.info("distinct numbers of tags = " + tagsAndBindingRegions.size());
            if( tagsAndBindingRegions.isEmpty() ) continue;
            SitePrediction sp = new SitePrediction( null, null, new SiteModel[] {ipsSiteModel}, null );
            Map<Integer, List<Double>> tagsAndMaximalScores = EntryStream.of(tagsAndBindingRegions)
                    .mapValues(br ->
                        BindingRegion.sequencesForBindingRegions(br, pathToSequences, minimalLengthOfSequenceRegion)
                            .map(sequence -> sp.findBestSite(sequence, true).getScore())
                            .toList())
                    .toMap();
            
            // initial initialization of xValuesForCurves and yValuesForCurves
            List<double[]> xValuesForCurves = new ArrayList<>();
            List<double[]> yValuesForCurves = new ArrayList<>();
            int nTags = tagsAndBindingRegions.size();
            for( int i = 0; i < nThresholds; i++ )
            {
                double[] xValues = new double[nTags];
                double[] yValues = new double[nTags];
                int index = 0;
                for( int tags : tagsAndBindingRegions.keySet())
                    xValues[index++] = tags;
                xValuesForCurves.add(i, xValues);
                yValuesForCurves.add(i, yValues);
            }
            
            // final determination of yValuesForCurves
            int index = 0;
            for( List<Double> maximalIpsScores : tagsAndMaximalScores.values() )
            {
                long size = maximalIpsScores.size();
                for( int i = 0; i < nThresholds; i++ )
                {
                    int count = 0;
                    for( Double x : maximalIpsScores )
                        if( x >= ipsThresholds[i] )
                            count++;
                    double[] yValues = yValuesForCurves.get(i);
                    yValues[index] = (double)count / (double)size;
                }
                index++;
            }
            
            // determination of individual charts
            String[] curveNames = new String[nThresholds];
            for( int i = 0; i < nThresholds; i++ )
                curveNames[i] = "IPS-threshold = " + ipsThresholds[i];
            Integer min = Integer.MAX_VALUE;
            for( int tags : tagsAndMaximalScores.keySet() )
            {
                if( tags < min )
                    min = tags;
            }
            Chart chart = TableUtils.createChart(xValuesForCurves, yValuesForCurves, curveNames, null, null, null, (double)min, 150.0, 0.0, 1.0, "Tag density", "Ratio of binding regions that contain the predictions");
            namesAndCharts.put(trackName, chart);

            // preliminary calculations for grouped graphs
            int[] distinctTags = IntStreamEx.of(tagsAndMaximalScores.keySet()).sorted().toArray();
            int size = StreamEx.ofValues(tagsAndMaximalScores).mapToInt(List::size).sum();
            
            // calculation charts for grouped graphs
            double[][] xAndYvaluesGrouped = new double[size][2];
            List<double[]> xValuesForGroupedCurves = new ArrayList<>();
            List<double[]> yValuesForGroupedCurves = new ArrayList<>();
            for( int i = 0; i < nThresholds; i++ )
            {
                index = 0;
                for( int j : distinctTags )
                {
                    xAndYvaluesGrouped[index][0] = j;
                    List<Double> scores = tagsAndMaximalScores.get(j);
                    for( Double score : scores )
                    {
                        xAndYvaluesGrouped[index][0] = j;
                        if( score >= ipsThresholds[i])
                            xAndYvaluesGrouped[index][1] = 1.0;
                        else
                            xAndYvaluesGrouped[index][1] = 0.0;
                        index++;
                    }
                }
                double[] xValuesGrouped = new double[numberOfGroups];
                double[] yValuesGrouped = new double[numberOfGroups];
                TableUtils.getGroupedGraph(xAndYvaluesGrouped, numberOfGroups, xValuesGrouped, yValuesGrouped);
                xValuesForGroupedCurves.add(i, xValuesGrouped);
                yValuesForGroupedCurves.add(i, yValuesGrouped);
            }
            Chart groupedChart = TableUtils.createChart(xValuesForCurves, yValuesForCurves, curveNames, null, null, null, (double)min, null, 0.0, 1.0, "Mean tag density", "Ratio of binding regions that contain the predictions");
            namesAndCharts.put(trackName + "_grouped", groupedChart);
        }
        return TableUtils.writeChartsIntoTable(namesAndCharts, "charts", pathToOutputs);
    }

    ///////// O.K.
    public static Map<Integer, Map<String, List<BindingRegion>>> readTagDensityAndBindingRegionsFromTrack(Track track, String givenTfClass)
    {
        Map<Integer, Map<String, List<BindingRegion>>> result = new HashMap<>();
        for(Site site: track.getAllSites())
        {
            String chromosome = site.getOriginalSequence().getName();
            if( site.getLength() <= 0 ) continue;
            DynamicPropertySet properties = site.getProperties();
            Integer tags = (Integer)properties.getValue("tags");
            if( tags == null ) continue;
            Map<String, List<BindingRegion>> chromosomeAndBindingRegions;
            if( result.containsKey(tags) )
                chromosomeAndBindingRegions = result.get(tags);
            else
                chromosomeAndBindingRegions = new HashMap<>();
            List<BindingRegion> bindingRegions;
            if( chromosomeAndBindingRegions.containsKey(chromosome) )
                bindingRegions = chromosomeAndBindingRegions.get(chromosome);
            else
                bindingRegions = new ArrayList<>();
            bindingRegions.add(new BindingRegion(givenTfClass, site.getInterval()));
            chromosomeAndBindingRegions.put(chromosome, bindingRegions);
            result.put(tags, chromosomeAndBindingRegions);
        }
        return result;
    }

    public static class IPSPredictionAnalysisParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath outputPath;
        private DataElementPath chipSeqTracksPath, matrixPath;
        private Species species = Species.getDefaultSpecies(null);
        private String tfClass;
        
        public IPSPredictionAnalysisParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_CHART_TABLE_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_CHART_TABLE_PATH)
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
        
        @PropertyName(MessageBundle.PN_DB_SELECTOR)
        @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
        public BasicGenomeSelector getDbSelector()
        {
            return dbSelector;
        }
        public void setDbSelector(BasicGenomeSelector dbSelector)
        {
            Object oldValue = this.dbSelector;
            this.dbSelector = dbSelector;
            dbSelector.setParent(this);
            firePropertyChange("dbSelector", oldValue, dbSelector);
        }

        @PropertyName(MessageBundle.PN_CHIPSEQ_FOLDER)
        @PropertyDescription(MessageBundle.PD_CHIPSEQ_FOLDER)
        public DataElementPath getChipSeqTracksPath()
        {
            return chipSeqTracksPath;
        }

        public void setChipSeqTracksPath(DataElementPath chipSeqTracksPath)
        {
            Object oldValue = this.chipSeqTracksPath;
            this.chipSeqTracksPath = chipSeqTracksPath;
            firePropertyChange("chipSeqTracksPath", oldValue, chipSeqTracksPath);
        }

        @PropertyName(MessageBundle.PN_MATRIX_PATH)
        @PropertyDescription(MessageBundle.PD_MATRIX_PATH)
        public DataElementPath getMatrixPath()
        {
            return matrixPath;
        }

        public void setMatrixPath(DataElementPath matrixPath)
        {
            Object oldValue = this.matrixPath;
            this.matrixPath = matrixPath;
            firePropertyChange("matrixPath", oldValue, matrixPath);
        }

        @PropertyName(MessageBundle.PN_SPECIES)
        @PropertyDescription(MessageBundle.PD_SPECIES)
        public Species getSpecies()
        {
            return species;
        }

        public void setSpecies(Species species)
        {
            Object oldValue = this.species;
            this.species = species;
            firePropertyChange("species", oldValue, species);
        }

        @PropertyName(MessageBundle.PN_TF_CLASS)
        @PropertyDescription(MessageBundle.PD_TF_CLASS)
        public String getTfClass()
        {
            return tfClass;
        }

        public void setTfClass(String tfClass)
        {
            Object oldValue = this.tfClass;
            this.tfClass = tfClass;
            firePropertyChange("tfClass", oldValue, tfClass);
        }
    }
    
    public static class IPSPredictionAnalysisParametersBeanInfo extends BeanInfoEx2<IPSPredictionAnalysisParameters>
    {
        public IPSPredictionAnalysisParametersBeanInfo()
        {
            super(IPSPredictionAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInputChild("chipSeqTracksPath", beanClass, Track.class));
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            property( "tfClass" ).auto( "$matrixPath/element/classReference$" ).add();
            property( "outputPath" ).outputElement( TableDataCollection.class )
                    .auto( "$chipSeqTracksPath/parent$/PredictionOf$matrixPath/name$_in_$tfClass$_andTags" ).add();
        }
    }
}
