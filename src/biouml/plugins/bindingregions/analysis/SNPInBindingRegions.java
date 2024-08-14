package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.SNP;
import biouml.plugins.bindingregions.utils.TableUtils;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * Extracted from SnpsAndBindingSites/mode1
 * "1. SNPs in binding regions (creation of tables: 'snpsInBindingRegions')"
 */
public class SNPInBindingRegions extends AnalysisMethodSupport<SNPInBindingRegions.SNPInBindingRegionsParameters>
{
    public SNPInBindingRegions(DataCollection<?> origin, String name)
    {
        super(origin, name, new SNPInBindingRegionsParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("SNPs in binding regions");
        DataElementPath pathToOutputTable = parameters.getOutputPath();
        DataElementPath pathToSingleTrack = parameters.getTrack();
        log.info("Read binding regions and sort them");
        Map<String, List<BindingRegion>> allBindingRegions = BindingRegion.readBindingRegionsFromTrack(pathToSingleTrack);
        ListUtil.sortAll(allBindingRegions);
        jobControl.setPreparedness(10);
        if(jobControl.isStopped())
            return null;
        log.info("Read SNPs from track "+parameters.getSnpTrack());
        Map<String, List<SNP>> chromosomeAndSnps = SNP.getSnpsFromVcfTrack(parameters.getSnpTrack());
        jobControl.setPreparedness(18);
        if(jobControl.isStopped())
            return null;
        
        log.info("Calculate and write table "+pathToOutputTable.getName());
        Map<String, String> tfClassAndTfName = TableUtils.readGivenColumnInStringTable(parameters.getTfNamesPath(), MessageBundle.TF_NAME_COLUMN);
        if(tfClassAndTfName == null)
            tfClassAndTfName = Collections.emptyMap();
        jobControl.setPreparedness(20);
        if(jobControl.isStopped())
            return null;
        jobControl.pushProgress(20, 100);
        TableDataCollection table = writeSnpsInBindingRegionsIntoTable(chromosomeAndSnps, allBindingRegions, tfClassAndTfName, pathToOutputTable);
        jobControl.popProgress();
        return table;
    }

    private boolean areOverlappedTwoSites(int beginOfFirstSite, int endOfFirstSite, int beginOfSecondSite, int endOfSecondtSite)
    {
        return endOfFirstSite >= beginOfSecondSite && endOfSecondtSite >= beginOfFirstSite;
    }

    private TableDataCollection writeSnpsInBindingRegionsIntoTable(Map<String, List<SNP>> chromosomeAndSnps, Map<String, List<BindingRegion>> allBindingRegions, Map<String, String> tfClassAndTfName, DataElementPath pathToTable) throws Exception
    {
        Map<String, List<String>> snpIDandTfClasses = new HashMap<>();
        Map<String, String> snpIDandChromosome = new HashMap<>();
        Map<String, Integer> snpIDandStartPositionOfSnp = new HashMap<>();
        for( Map.Entry<String, List<SNP>> entry : chromosomeAndSnps.entrySet() )
        {
            String chromosome = entry.getKey();
            if( !allBindingRegions.containsKey(chromosome) )
                continue;
            List<SNP> snps = entry.getValue();
            List<BindingRegion> bindingRegions = allBindingRegions.get(chromosome);
            if( snps.size() == 0 || bindingRegions.size() == 0 ) continue;
            int minPosition = Integer.MAX_VALUE;
            int maxPosition = 0;
            for( SNP snp : snps )
            {
                int i = snp.getStartPosition();
                if( i < minPosition )
                    minPosition = i;
                if( i > maxPosition )
                    maxPosition = i;
            }
            for( BindingRegion br : bindingRegions )
            {
                int begin = br.getStartPosition();
                int end = br.getFinishPosition();
                if( ! areOverlappedTwoSites(begin, end, minPosition, maxPosition) ) continue;
                for( SNP snp : snps )
                {
                    int snpPosition = snp.getStartPosition();
                    if( ! areOverlappedTwoSites(begin, end, snpPosition, snpPosition) ) continue;
                    String snpID = snp.getSnpID();
                    snpIDandTfClasses.computeIfAbsent( snpID, k -> new ArrayList<>() ).add( br.getTfClass() );
                    snpIDandChromosome.put(snpID, chromosome);
                    snpIDandStartPositionOfSnp.put(snpID, snpPosition);
                }
            }
        }
        jobControl.setPreparedness(30);
        if(jobControl.isStopped())
            return null;
        
        Map<String, List<String>> snpIDandTfNames = new HashMap<>();
        for( Map.Entry<String, List<String>> entry : snpIDandTfClasses.entrySet() )
        {
            List<String> tfNames = new ArrayList<>();
            for( String tfClass : entry.getValue() )
            {
                if( tfClassAndTfName.containsKey(tfClass) )
                    tfNames.add(tfClassAndTfName.get(tfClass));
                else
                    tfNames.add(tfClass);
            }
            snpIDandTfNames.put(entry.getKey(), tfNames);
        }
        jobControl.setPreparedness(70);
        if(jobControl.isStopped())
            return null;
        
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
        table.getColumnModel().addColumn("snpID", String.class);
        table.getColumnModel().addColumn("chromosome", String.class);
        table.getColumnModel().addColumn("startPositionOfSnp", Integer.class);
        table.getColumnModel().addColumn("numberOfBindingRegions", Integer.class);
        table.getColumnModel().addColumn("tfClasses", StringSet.class);
        table.getColumnModel().addColumn("tfNames", StringSet.class);
        int iRow = 0;
        for( Map.Entry<String, List<String>> entry : snpIDandTfClasses.entrySet() )
        {
            String snpID = entry.getKey();
            List<String> tfClasses = entry.getValue();
            List<String> tfNames = snpIDandTfNames.get(snpID);
            int numberOfBindingRegions = tfClasses.size();
            Object[] values = new Object[6];
            values[0] = snpID;
            values[1] = snpIDandChromosome.get(snpID);
            values[2] = snpIDandStartPositionOfSnp.get(snpID);
            values[3] = numberOfBindingRegions;
            StringSet tfClassesSet = new StringSet();
            for( String tfClass : tfClasses )
                tfClassesSet.add(tfClass);
            values[4] = tfClassesSet;
            StringSet tfNamesSet = new StringSet();
            for( String tfName : tfNames )
                tfNamesSet.add(tfName);
            values[5] = tfNamesSet;
            TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), values, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }

    public static class SNPInBindingRegionsParameters extends AbstractAnalysisParameters
    {
        private DataElementPath outputPath;
        private DataElementPath track;
        private DataElementPath snpTrack;
        private DataElementPath tfNamesPath;
        
        @PropertyName(MessageBundle.PN_TF_NAMES_TABLE)
        @PropertyDescription(MessageBundle.PD_TF_NAMES_TABLE)
        public DataElementPath getTfNamesPath()
        {
            return tfNamesPath;
        }

        public void setTfNamesPath(DataElementPath tfNamesPath)
        {
            Object oldValue = this.tfNamesPath;
            this.tfNamesPath = tfNamesPath;
            firePropertyChange("tfNamesPath", oldValue, tfNamesPath);
        }

        @PropertyName(MessageBundle.PN_OUTPUT_TABLE_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_TABLE_PATH)
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
        
        @PropertyName(MessageBundle.PN_SNP_TRACK)
        @PropertyDescription(MessageBundle.PD_SNP_TRACK)
        public DataElementPath getSnpTrack()
        {
            return snpTrack;
        }

        public void setSnpTrack(DataElementPath snpTrack)
        {
            Object oldValue = this.snpTrack;
            this.snpTrack = snpTrack;
            firePropertyChange("snpTrack", oldValue, snpTrack);
        }

        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_MERGED)
        public DataElementPath getTrack()
        {
            return track;
        }

        public void setTrack(DataElementPath track)
        {
            Object oldValue = this.track;
            this.track = track;
            firePropertyChange("track", oldValue, track);
        }
    }
    
    public static class SNPInBindingRegionsParametersBeanInfo extends BeanInfoEx2<SNPInBindingRegionsParameters>
    {
        public SNPInBindingRegionsParametersBeanInfo()
        {
            super(SNPInBindingRegionsParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "track" ).inputElement( Track.class ).add();
            property( "snpTrack" ).inputElement( Track.class ).add();
            property( "tfNamesPath" ).inputElement( TableDataCollection.class ).canBeNull().add();
            property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$track$ snps" ).add();
        }
    }
}
