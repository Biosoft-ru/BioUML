package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.CisModule;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.bindingregions.utils.SampleComparison;
import biouml.plugins.bindingregions.utils.SampleConstruction;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.TrackInfo;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectIntMap;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class CisModuleWithGivenPatternIdentification extends AnalysisMethodSupport<CisModuleWithGivenPatternIdentification.CisModuleWithGivenPatternIdentificationParameters>
{
    public CisModuleWithGivenPatternIdentification(DataCollection<?> origin, String name)
    {
        super(origin, name, new CisModuleWithGivenPatternIdentificationParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Identification of cis-regulatory modules with pre-specified pattern");
        log.info("where pattern is defined as given set of TF-classes");
        DataElementPath pathToBindingRegionTrack = parameters.getTrackPath();
        int maxNumberOfMissingTfClasses = parameters.getMaxNumberOfMissingTfClasses();
        String[] cisModulePattern = parameters.getCisModulePattern();
        DataElementPath pathToTableWithGenes = parameters.getPathToTableWithGenes();
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        
        if( cisModulePattern.length < 2 )
            throw new IllegalArgumentException("Pattern has to contain two or more TF-classes");
        maxNumberOfMissingTfClasses = Math.max(0, maxNumberOfMissingTfClasses);
        maxNumberOfMissingTfClasses = Math.min(cisModulePattern.length - 2, maxNumberOfMissingTfClasses);
        int minimalNumberOfOverlaps = cisModulePattern.length - maxNumberOfMissingTfClasses;
 
        // Now pattern is array of strings and each string has following structure: "tfClass_itsName"
        // To transform this pattern into list of tfClasses; and also into map: tfClass <-> tfName
        List<String> tfClassesList = new ArrayList<>();
        Map<String, String> tfClassAndName = new HashMap<>();
        for( String s : cisModulePattern )
        {
            String[] array = s.split(BindingRegion.SEPARATOR_BETWEEN_TF_CLASS_AND_ITS_NAME);
            tfClassesList.add(array[0]);
            tfClassAndName.put(array[0], array[1]);
        }
        log.info("Size of pattern = " + tfClassesList.size() + " TF-classes");

        // Calculate cis-modules and write them into track
        log.info("Read TF-binding regions in track");
        Track track = pathToBindingRegionTrack.getDataElement(Track.class);
        jobControl.setPreparedness(5);
        log.info("Identification of cis-regulatory modules");
        Map<String, List<CisModule>> allCisModules = CisModule.getAllCisModules2(track, tfClassesList, minimalNumberOfOverlaps);
        jobControl.setPreparedness(60);
        log.info("Write the identified cis-regulatory modules into track");
        TrackInfo trackInfo = new TrackInfo(track);
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        CisModule.writeCisModulesIntoTrack(allCisModules, minimalNumberOfOverlaps, trackInfo.getSpecie(), trackInfo.getCellLine(), trackInfo.getPathToSequenceCollection(), tfClassAndName, pathToOutputs, "cis-regulatory_modules_track");
        jobControl.setPreparedness(70);
        
        // Calculate distances between cis-modules and genes and write them into table
        log.info("Read genes in table");
        Map<String, List<Gene>> chromosomesAndGenes = EnsemblUtils.Gene.readGenesInTable(pathToTableWithGenes);
        log.info("Remove all non-protein-coding genes");
        Gene.removeAllNonProteinCodingGenes(chromosomesAndGenes);
        log.info("To sort all remained genes");
        ListUtil.sortAll(chromosomesAndGenes);
        jobControl.setPreparedness(80);
        log.info("Identification of distances between cis-regulatory modules and protein-coding genes");
        writeDistancesBetweenCisModulesAndGenes(allCisModules, chromosomesAndGenes, pathToOutputs, "distancesBetweenCisModulesAndGenes");
        jobControl.setPreparedness(90);
        // Chart with distances
        log.info("Treatment of distributions of distances");
        writeDistancesDistributions(allCisModules, chromosomesAndGenes, pathToOutputs);
        jobControl.setPreparedness(95);
        // Table with tfClass frequencies
        log.info("Calculate frequencies of TF-classes and write them into table");
        TObjectIntMap<String> frequenciesOfTfClasses = CisModule.getFrequenciesOfTfClasses(allCisModules);
        writeFrequenciesOfTfClasses(frequenciesOfTfClasses, tfClassAndName, ListUtil.sumTotalSize(allCisModules), pathToOutputs, "frequenciesOfTfClasses");
        return pathToOutputs.getDataCollection();
    }
    
    private void writeFrequenciesOfTfClasses(TObjectIntMap<String> frequenciesOfTfClasses, Map<String, String> tfClassAndName, int totalSize, DataElementPath pathToOutputs, String tableName)
    {
        DataElementPath dep = pathToOutputs.getChildPath(tableName);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
        table.getColumnModel().addColumn("TF-name", String.class);
        table.getColumnModel().addColumn("Frequency", Integer.class);
        table.getColumnModel().addColumn("Relative frequency", Double.class);
        for( Map.Entry<String, String> entry : tfClassAndName.entrySet() )
        {
            String tfClass = entry.getKey();
            TableDataCollectionUtils.addRow(table, tfClass, new Object[] {entry.getValue(), frequenciesOfTfClasses.get(tfClass), (float)frequenciesOfTfClasses.get(tfClass) / (float)totalSize}, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    private void writeDistancesBetweenCisModulesAndGenes(Map<String, List<CisModule>> chromosomeAndCisModules, Map<String, List<Gene>> chromosomesAndGenes, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        DataElementPath dep = pathToOutputs.getChildPath(tableName);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
        table.getColumnModel().addColumn("Chromosome", String.class);
        table.getColumnModel().addColumn("Module begin", Integer.class);
        table.getColumnModel().addColumn("Module end", Integer.class);
        table.getColumnModel().addColumn("Number of Tf-classes", Integer.class);
        table.getColumnModel().addColumn("Distance between module and TSS", Integer.class);
        table.getColumnModel().addColumn("Gene with nearest TSS", String.class);
        table.getColumnModel().addColumn("Distance between module and 3'-end", Integer.class);
        table.getColumnModel().addColumn("Gene with nearest 3'-end", String.class);
        table.getColumnModel().addColumn("Tf-classes", StringSet.class);
        int iRow = 0;
        for( Entry<String, List<CisModule>> entry : chromosomeAndCisModules.entrySet() )
        {
            String chromosome = entry.getKey();
            List<CisModule> cisModules = entry.getValue();
            List<Gene> genes = chromosomesAndGenes.get(chromosome);
            int[] distanceToTss = new int[cisModules.size()];
            String[] idOfGeneWithNearestTsss = new String[cisModules.size()];
            getMinimalDistancesBetweenTssAndCisModules(cisModules, getDistinctTssInOneChromosome(genes, true), distanceToTss, idOfGeneWithNearestTsss);
            int[] distanceToTerminal = new int[cisModules.size()];
            String[] idOfGeneWithNearestTerminal = new String[cisModules.size()];
            getMinimalDistancesBetweenTssAndCisModules(cisModules, getDistinctTssInOneChromosome(genes, false), distanceToTerminal, idOfGeneWithNearestTerminal);
            int j = 0;
            for( CisModule cisModule : cisModules )
            {
                StringSet tfClassesSet = new StringSet();
                for( String tfClass : cisModule.getTfClasses() )
                    tfClassesSet.add(tfClass);
                TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {chromosome, cisModule.getStartPosition(), cisModule.getFinishPosition(), cisModule.getNumberOfTfClasses(), distanceToTss[j], idOfGeneWithNearestTsss[j], distanceToTerminal[j], idOfGeneWithNearestTerminal[j++], tfClassesSet}, true);
            }
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    private void writeDistancesDistributions(Map<String, List<CisModule>> chromosomeAndCisModules, Map<String, List<Gene>> chromosomesAndGenes, DataElementPath pathToOutputs) throws Exception
    {
        TDoubleList tssDistances = new TDoubleArrayList();
        TDoubleList terminalDistances = new TDoubleArrayList();
        for( Entry<String, List<CisModule>> entry : chromosomeAndCisModules.entrySet() )
        {
            List<CisModule> cisModules = entry.getValue();
            List<Gene> genes = chromosomesAndGenes.get(entry.getKey());
            int[] distanceToTss = new int[cisModules.size()];
            String[] idOfGeneWithNearestTsss = new String[cisModules.size()];
            getMinimalDistancesBetweenTssAndCisModules(cisModules, getDistinctTssInOneChromosome(genes, true), distanceToTss, idOfGeneWithNearestTsss);
            int[] distanceToTerminal = new int[cisModules.size()];
            String[] idOfGeneWithNearestTerminal = new String[cisModules.size()];
            getMinimalDistancesBetweenTssAndCisModules(cisModules, getDistinctTssInOneChromosome(genes, false), distanceToTerminal, idOfGeneWithNearestTerminal);
            for( int j = 0; j < cisModules.size(); j++ )
            {
                tssDistances.add(distanceToTss[j]);
                terminalDistances.add(distanceToTerminal[j]);
            }
        }
        Map<String, TDoubleList> map = new HashMap<>();
        map.put("Distance between CRM and TSS", tssDistances);
        map.put("Distance between CRM and 3'-end", terminalDistances);
        SampleConstruction preSamples = new SampleConstruction(map, "Distance");
        SampleComparison samples = preSamples.transformToSampleComparison();
        samples.writeTableWithMeanAndSigma(pathToOutputs, "meanAndSigma");
        Chart chart = samples.chartWithSmoothedDensities(true, null, DensityEstimation.WINDOW_WIDTH_01, null);
        SiteModelsComparisonUtils.writeChartsIntoTable(samples.getCommonName(), chart, "chart", pathToOutputs, "chart_with_densities");
    }
    
    private static class TSS implements Comparable<TSS>
    {
        int tssPosition;
        String geneId;
        
        private TSS(int tssPosition, String geneId)
        {
            this.tssPosition = tssPosition;
            this.geneId = geneId;
        }
        
        private int getTssPosition()
        {
            return tssPosition;
        }
        
        private String getGeneId()
        {
            return geneId;
        }

        @Override
        public int compareTo(TSS o)
        {
            return tssPosition - o.getTssPosition();
        }
    }

    private List<TSS> getDistinctTssInOneChromosome(List<Gene> genes, boolean isTss)
    {
        return StreamEx.of( genes )
                .mapToEntry( isTss ? Gene::getTranscriptionStarts : Gene::getTranscriptionEnds, Gene::getEnsemblId )
                .<TSS> flatMapKeyValue(
                        (positions, geneID) -> IntStreamEx.of( positions ).distinct().mapToObj( position -> new TSS( position, geneID ) ) )
                .sorted().toList();
    }

    /***
     * cisModules and tsss must be ordered
     * @param cisModules
     * @param tsss
     * @param minimalDistances the values of this array are determined by this method
     * @param geneIds the values of this array are determined by this method
     */
    private void getMinimalDistancesBetweenTssAndCisModules(List<CisModule> cisModules, List<TSS> tsss, int[] minimalDistances, String[] geneIds)
    {
        int iTss0 = 0;
        for( int iCisModule = 0; iCisModule < cisModules.size(); iCisModule++ )
        {
            minimalDistances[iCisModule] = Integer.MAX_VALUE;
            int cisModuleStart = cisModules.get(iCisModule).getStartPosition();
            for( int iTss = iTss0; iTss < tsss.size(); iTss++ )
            {
                TSS tss = tsss.get(iTss);
                int distance = Math.abs(cisModuleStart - tss.getTssPosition());
                if( distance <= minimalDistances[iCisModule] )
                {
                    iTss0 = iTss;
                    minimalDistances[iCisModule] = distance;
                    geneIds[iCisModule] = tss.getGeneId();
                }
                else break;
            }
        }
    }

    public static class CisModuleWithGivenPatternIdentificationParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
        private int maxNumberOfMissingTfClasses = 0;
        private String[] cisModulePattern;
        private DataElementPath pathToTableWithGenes;
        private DataElementPath outputPath;
        
        /*** It was used only in particular case (for Vakhitova)
        public CisModuleWithGivenPatternIdentificationParameters()
        {
            setTrackPath(DataElementPath.create("data/Collaboration/yura_test/Data/GTRD_analysis/Human_Build37/Tracks/tfClassesMergedBindingRegions2/bindingRegions_in_allCellLines"));
        }
        ***/
        
        // It was used only in particular case (for Vakhitova)
        private String[] getTrackDefaultPattern()
        {
            String[] distinctTfClassesAndNames0 = new String[]{"0.0.6.0.1", "1.1.1.1.1", "1.1.1.1.2", "1.1.1.3.1", "1.1.2.1.2", "1.1.2.1.3", "1.1.2.2.1", "1.1.2.2.2", "1.1.7.1.2", "1.2.6.3.1", "1.2.6.3.2", "1.3.1.0.1", "2.1.1.1.1", "2.1.1.2.3", "2.1.2.5.3", "2.1.3.5.2", "2.1.4.0.1", "2.2.1.1.1", "2.2.1.1.2", "2.3.1.1.1", "2.3.1.1.4", "2.3.1.3.1", "2.3.3.22.2", "2.3.3.9.1", "2.3.4.0.27", "2.3.4.15.1", "2.3.4.4.1", "2.3.4.8.1", "3.3.2.1.4", "3.3.3.0.5", "3.5.2.1.1", "3.5.2.1.4", "3.5.2.2.1", "3.5.2.2.4", "3.7.1.3.1", "4.1.3.0.3", "5.1.1.1.1", "5.1.1.1.3", "5.1.2.0.1", "6.1.1.2.1", "6.2.1.0.5", "6.2.1.0.6", "6.3.1.0.1", "6.3.1.0.3", "6.5.2.0.2", "7.1.1.1.1", "8.1.1.0.1"};
            try
            {
                Track track = trackPath.getDataElement(Track.class);
                Map<String, String> tfClassAndName = BindingRegion.getDistinctTfClassesAndNamesFromTrack(track);
                String[] distinctTfClassesAndNames = BindingRegion.getTfClassesAndNames(tfClassAndName, distinctTfClassesAndNames0);
                return distinctTfClassesAndNames;
            }
            catch( Exception e )
            {
                return new String[0];
            }
        }
        
        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_MERGED)
        public DataElementPath getTrackPath()
        {
            return trackPath;
        }
        public void setTrackPath(DataElementPath trackPath)
        {
            Object oldValue = this.trackPath;
            this.trackPath = trackPath;
            firePropertyChange("trackPath", oldValue, trackPath);
 //         setCisModulePattern(getTrackDefaultPattern());
        }
        
        @PropertyName(MessageBundle.PN_MAX_NUMBER_OF_MISSING_TFCLASSES)
        @PropertyDescription(MessageBundle.PD_MAX_NUMBER_OF_MISSING_TFCLASSES)
        public int getMaxNumberOfMissingTfClasses()
        {
            return maxNumberOfMissingTfClasses;
        }
        public void setMaxNumberOfMissingTfClasses(int maxNumberOfMissingTfClasses)
        {
            Object oldValue = this.maxNumberOfMissingTfClasses;
            this.maxNumberOfMissingTfClasses = maxNumberOfMissingTfClasses;
            firePropertyChange("maxNumberOfMissingTfClasses", oldValue, maxNumberOfMissingTfClasses);
        }
        
        @PropertyName(MessageBundle.PN_CIS_MODULE_PATTERN)
        @PropertyDescription(MessageBundle.PD_CIS_MODULE_PATTERN)
        public String[] getCisModulePattern()
        {
            return cisModulePattern;
        }
        public void setCisModulePattern(String[] cisModulePattern)
        {
            Object oldValue = this.cisModulePattern;
            this.cisModulePattern = cisModulePattern;
            firePropertyChange("cisModulePattern", oldValue, cisModulePattern);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_GENE)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_GENE)
        public DataElementPath getPathToTableWithGenes()
        {
            return pathToTableWithGenes;
        }
        public void setPathToTableWithGenes(DataElementPath pathToTableWithGenes)
        {
            Object oldValue = this.pathToTableWithGenes;
            this.pathToTableWithGenes = pathToTableWithGenes;
            firePropertyChange("pathToTableWithGenes", oldValue, pathToTableWithGenes);
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
    
    public static class CisModulePatternSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                Track track = ((CisModuleWithGivenPatternIdentificationParameters)getBean()).getTrackPath().getDataElement(Track.class);
                DataCollectionInfo info = ((DataCollection<?>)track).getInfo();
                String distinctTfClassesAndNames0 = info.getProperty(CisModule.DISTINCT_TFCLASSES_AND_NAMES);
                String[] distinctTfClassesAndNames = BindingRegion.splitStringToArrayByJSON(distinctTfClassesAndNames0);
                Arrays.sort(distinctTfClassesAndNames);
                return distinctTfClassesAndNames;
            }
            catch( RepositoryException e )
            {
                return new String[] {"(please select track)"};
            }
            catch( Exception e )
            {
                return new String[] {"(track doesn't contain TF classes info)"};
            }
        }
    }

    public static class CisModuleWithGivenPatternIdentificationParametersBeanInfo
            extends BeanInfoEx2<CisModuleWithGivenPatternIdentificationParameters>
    {
        public CisModuleWithGivenPatternIdentificationParametersBeanInfo()
        {
            super(CisModuleWithGivenPatternIdentificationParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            add("maxNumberOfMissingTfClasses");
            add("cisModulePattern", CisModulePatternSelector.class);
            property( "pathToTableWithGenes" ).inputElement( TableDataCollection.class ).add();
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
