package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.CisModule;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.bindingregions.utils.TableUtils;
import gnu.trove.map.TObjectIntMap;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author yura
 *
 */
public class CisModuleSetsNearGenes extends AnalysisMethodSupport<CisModuleSetsNearGenes.CisModuleSetsNearGenesParameters>
{
    public CisModuleSetsNearGenes(DataCollection<?> origin, String name)
    {
        super(origin, name, new CisModuleSetsNearGenesParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Comparison of two sets of cis-regulatory modules that located near given genes");
        log.info("Namely, frequencies of TF-classes are compared");
        DataElementPath pathToCisModuleTrack = parameters.getTrackPath();
        DataElementPath pathToTable1 = parameters.getPathToTableWithGeneSet1();
        DataElementPath pathToTable2 = parameters.getPathToTableWithGeneSet2();
        DataElementPath pathToTableWithGenes = parameters.getPathToTableWithGenes();
        int minSizeOfCisModules = parameters.getMinSizeOfCisModules();
        int maxDistance = parameters.getMaxDistanceBetweenCisModulesAndGenes();
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // 1.
        log.info("Read all available protein-coding genes in table");
        Map<String, List<Gene>> chromosomesAndGenes = Gene.readGenesInTable(pathToTableWithGenes);
        Gene.removeAllNonProteinCodingGenes(chromosomesAndGenes);
        jobControl.setPreparedness(10);
        if( jobControl.isStopped() ) return null;
        
        // 2.
        log.info("Read all available cis-regulatory modules in track");
        Track track = pathToCisModuleTrack.getDataElement(Track.class);
        Map<String, List<CisModule>> allCisModules = CisModule.readCisModulesInSqlTrack(track, minSizeOfCisModules);
        Map<String, String> tfClassAndTfName = BindingRegion.getDistinctTfClassesAndNamesFromTrack(track);
        jobControl.setPreparedness(50);
        if( jobControl.isStopped() ) return null;

// test
        int[] ct = new int[] {1, 2, 11, 4};
        double corr = Stat.pearsonCorrelation(ct);
        log.info("test corr = " + corr);
        for( Entry<String, String> entry : tfClassAndTfName.entrySet() )
            log.info("tfClass = " + entry.getKey() + " TF-name = " + entry.getValue());
//test
        
        // 3.
        log.info("Create two sets of cis-regulatory modules");
        Map<String, List<CisModule>> cisModules1 = createCisModuleSet(pathToTable1, chromosomesAndGenes, allCisModules, maxDistance);
        log.info("Number of cis-modules in 1-st set = " + ListUtil.sumTotalSize(cisModules1));
        jobControl.setPreparedness(60);
        if( jobControl.isStopped() ) return null;
        Map<String, List<CisModule>> cisModules2 = createCisModuleSet(pathToTable2, chromosomesAndGenes, allCisModules, maxDistance);
        log.info("Number of cis-modules in 2-nd set = " + ListUtil.sumTotalSize(cisModules2));
        jobControl.setPreparedness(70);
        if( jobControl.isStopped() ) return null;
        
        // 4.
        log.info("Create and write table with frequencies and table with correlations");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        writeFrequenciesOfTfClasses(cisModules1, cisModules2, tfClassAndTfName, pathToOutputs, "frequencies_of_tfClasses");
        jobControl.setPreparedness(80);
        writeCorrelationsBetweenTfClasses(cisModules1, cisModules2, tfClassAndTfName, pathToOutputs, "correlation_between_tfClasses");
        return pathToOutputs.getDataCollection();
    }
    
    private Map<String, List<CisModule>> createCisModuleSet(DataElementPath pathToTable, Map<String, List<Gene>> chromosomesAndGenes, Map<String, List<CisModule>> allCisModules, int maxDistance)
    {
        Map<String, String> keyAndEnsemblId = TableUtils.readGivenColumnInStringTable(pathToTable, MessageBundle.GENE_ID_COLUMN);
        List<String> ensemblIds = new ArrayList<>();
        for( String id : keyAndEnsemblId.values() )
            ensemblIds.add(id);
        Map<String, List<Gene>> chromosomesAndGivenGenes = Gene.getGivenGenes(ensemblIds, chromosomesAndGenes);
        ListUtil.sortAll(chromosomesAndGivenGenes);
        return CisModule.getCisModulesNearGivenGenes(chromosomesAndGivenGenes, allCisModules, maxDistance);
    }
    
    private void writeFrequenciesOfTfClasses(Map<String, List<CisModule>> cisModules1, Map<String, List<CisModule>> cisModules2, Map<String, String> tfClassAndTfName, DataElementPath pathToOutputs, String tableName)
    {
        TObjectIntMap<String> freq1 = CisModule.getFrequenciesOfTfClasses(cisModules1);
        TObjectIntMap<String> freq2 = CisModule.getFrequenciesOfTfClasses(cisModules2);
        int size1 = ListUtil.sumTotalSize(cisModules1);
        int size2 = ListUtil.sumTotalSize(cisModules2);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(tableName));
        table.getColumnModel().addColumn("TF-name", String.class);
        table.getColumnModel().addColumn("Frequency in 1-st set, f1", Double.class);
        table.getColumnModel().addColumn("Frequency in 2-nd set, f2", Double.class);
        table.getColumnModel().addColumn("Ratio f1 : f2", Double.class);
        for( Map.Entry<String, String> entry : tfClassAndTfName.entrySet() )
        {
            String tfClass = entry.getKey();
            if( ! freq1.containsKey(tfClass) || ! freq2.containsKey(tfClass) ) continue;
            int n1 = freq1.get(tfClass);
            int n2 = freq2.get(tfClass);
            if( n1 == 0 && n2 == 0 ) continue;
            float f1 = (float)n1 / (float)size1;
            float f2 = (float)n2 / (float)size2;
            float ratio = n2 > 0 ? f1 / f2 : Float.MAX_VALUE;
            TableDataCollectionUtils.addRow(table, tfClass, new Object[] {entry.getValue(), f1, f2, ratio}, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    private void writeCorrelationsBetweenTfClasses(Map<String, List<CisModule>> cisModules1, Map<String, List<CisModule>> cisModules2, Map<String, String> tfClassAndTfName, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(tableName));
        table.getColumnModel().addColumn("TF-name1", String.class);
        table.getColumnModel().addColumn("TF-name2", String.class);
        table.getColumnModel().addColumn("p-value of independence", Double.class);
        table.getColumnModel().addColumn("Correlation in 1-nd set, corr1", Double.class);
        table.getColumnModel().addColumn("Correlation in 2-nd set, corr2", Double.class);
        table.getColumnModel().addColumn("Difference corr1 - corr2", Double.class);
        Set<String> distinctTfClasses = CisModule.getDistinctTfClasses(cisModules1);
        for( String tfClass1 : distinctTfClasses )
            for( String tfClass2 : distinctTfClasses )
            {
                if( tfClass1.equals(tfClass2) ) break;
                int[] cTable1 = CisModule.getContingencyTable(cisModules1, tfClass1, tfClass2);
                double stat1 = Stat.getStatisticOfChiSquared_2x2_testForIndependence(cTable1);
                double pValue1 = 1.0 - Stat.chiDistribution(stat1, 1.0);
                double corr1 = Stat.pearsonCorrelation(cTable1);
                int[] cTable2 = CisModule.getContingencyTable(cisModules2, tfClass1, tfClass2);
                double corr2 = Stat.pearsonCorrelation(cTable2);
                TableDataCollectionUtils.addRow(table, tfClass1 + CisModule.SEPARATOR_BETWEEN_TFCLASSES + tfClass2, new Object[] {tfClassAndTfName.get(tfClass1), tfClassAndTfName.get(tfClass2), pValue1, corr1, corr2, corr1 - corr2}, true);
            }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    
    public static class CisModuleSetsNearGenesParameters extends AbstractAnalysisParameters
    {
        private DataElementPath trackPath;
        private DataElementPath pathToTableWithGeneSet1;
        private DataElementPath pathToTableWithGeneSet2;
        private DataElementPath pathToTableWithGenes = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Vakhitova/ForArticle1/Software_Input/_genes");
        private int minSizeOfCisModules = 20;
        private int maxDistanceBetweenCisModulesAndGenes = 20000;
        private DataElementPath outputPath;
        
        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_CIS_MODULES)
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_GENE1)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_GENE1)
        public DataElementPath getPathToTableWithGeneSet1()
        {
            return pathToTableWithGeneSet1;
        }
        public void setPathToTableWithGeneSet1(DataElementPath pathToTableWithGeneSet1)
        {
            Object oldValue = this.pathToTableWithGeneSet1;
            this.pathToTableWithGeneSet1 = pathToTableWithGeneSet1;
            firePropertyChange("pathToTableWithGeneSet1", oldValue, pathToTableWithGeneSet1);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_GENE2)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_GENE2)
        public DataElementPath getPathToTableWithGeneSet2()
        {
            return pathToTableWithGeneSet2;
        }
        public void setPathToTableWithGeneSet2(DataElementPath pathToTableWithGeneSet2)
        {
            Object oldValue = this.pathToTableWithGeneSet2;
            this.pathToTableWithGeneSet2 = pathToTableWithGeneSet2;
            firePropertyChange("pathToTableWithGeneSet2", oldValue, pathToTableWithGeneSet2);
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
        
        @PropertyName(MessageBundle.PN_MINIMAL_SIZE)
        @PropertyDescription(MessageBundle.PD_MINIMAL_SIZE_CIS_MODULE)
        public int getMinSizeOfCisModules()
        {
            return minSizeOfCisModules;
        }
        public void setMinSizeOfCisModules(int minSizeOfCisModules)
        {
            Object oldValue = this.minSizeOfCisModules;
            this.minSizeOfCisModules = minSizeOfCisModules;
            firePropertyChange("minSizeOfCisModules", oldValue, minSizeOfCisModules);
        }
        
        @PropertyName(MessageBundle.PN_MAX_DISTANCE)
        @PropertyDescription(MessageBundle.PD_MAX_DISTANCE)
        public int getMaxDistanceBetweenCisModulesAndGenes()
        {
            return maxDistanceBetweenCisModulesAndGenes;
        }
        public void setMaxDistanceBetweenCisModulesAndGenes(int maxDistanceBetweenCisModulesAndGenes)
        {
            Object oldValue = this.maxDistanceBetweenCisModulesAndGenes;
            this.maxDistanceBetweenCisModulesAndGenes = maxDistanceBetweenCisModulesAndGenes;
            firePropertyChange("maxDistanceBetweenCisModulesAndGenes", oldValue, maxDistanceBetweenCisModulesAndGenes);
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
    
    public static class CisModuleSetsNearGenesParametersBeanInfo extends BeanInfoEx2<CisModuleSetsNearGenesParameters>
    {
        public CisModuleSetsNearGenesParametersBeanInfo()
        {
            super(CisModuleSetsNearGenesParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            property( "pathToTableWithGeneSet1" ).inputElement( TableDataCollection.class ).canBeNull().add();
            property( "pathToTableWithGeneSet2" ).inputElement( TableDataCollection.class ).canBeNull().add();
            property( "pathToTableWithGenes" ).inputElement( TableDataCollection.class ).canBeNull().add();
            add("minSizeOfCisModules");
            add("maxDistanceBetweenCisModulesAndGenes");
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
