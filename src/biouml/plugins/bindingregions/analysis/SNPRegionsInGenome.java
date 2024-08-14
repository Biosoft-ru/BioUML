package biouml.plugins.bindingregions.analysis;

import java.util.List;
import java.util.Map;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.SNP;
import biouml.plugins.bindingregions.utils.TableUtils;

import ru.biosoft.jobcontrol.Iteration;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.ListUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 * Extracted from SnpsAndBindingSites/mode2
 * "2. SNP-regions in genome (creation of tables: snpRegions)"
 */
public class SNPRegionsInGenome extends AnalysisMethodSupport<SNPRegionsInGenome.SNPRegionsInGenomeParameters>
{
    public SNPRegionsInGenome(DataCollection<?> origin, String name)
    {
        super(origin, name, new SNPRegionsInGenomeParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("SNP-regions in genome");
        final DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        final int regionLength = parameters.getSnpRegionLength();
        log.info("Read SNPs in track 'snp' (vcf format)");
        final Map<String, List<SNP>> chromosomeAndSnps = SNP.getSnpsFromVcfTrack(parameters.getSnpTrack());
        log.info("Calculate and write table 'snpRegions'");
        int numberOfSnps = ListUtil.sumTotalSize(chromosomeAndSnps);
        final String[][] table = new String[numberOfSnps][2];
        final String[] namesOfRows = new String[numberOfSnps];
        jobControl.forCollection(chromosomeAndSnps.keySet(), new Iteration<String>()
        {
            int i = 0;

            @Override
            public boolean run(String chromosome)
            {
                AnnotatedSequence annotatedSequence = pathToSequences.getChildPath(chromosome).getDataElement(AnnotatedSequence.class);
                Sequence sequence = annotatedSequence.getSequence();
                List<SNP> snps = chromosomeAndSnps.get(chromosome);
                for( SNP snp : snps )
                {
                    String snpID = snp.getSnpID();
                    namesOfRows[i] = snpID;
                    Sequence[] sequences = snp.getRegions(sequence, regionLength);
                    table[i][0] = sequences[0].toString();
                    table[i++][1] = sequences[1].toString();
                }
                return true;
            }
        });
        if(jobControl.isStopped())
            return null;
        return TableUtils.writeStringTable(table, namesOfRows, new String[] {"genomeRegionBeforeSnp", "genomeRegionAfterSnp"}, parameters.getOutputPath());
    }

    public static class SNPRegionsInGenomeParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath outputPath;
        private DataElementPath snpTrack;
        private int snpRegionLength = 10;

        public SNPRegionsInGenomeParameters()
        {
            setDbSelector(new BasicGenomeSelector());
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

        @PropertyName(MessageBundle.PN_SNP_REGION_LENGTH)
        @PropertyDescription(MessageBundle.PD_SNP_REGION_LENGTH)
        public int getSnpRegionLength()
        {
            return snpRegionLength;
        }

        public void setSnpRegionLength(int snpRegionLength)
        {
            Object oldValue = this.snpRegionLength;
            this.snpRegionLength = snpRegionLength;
            firePropertyChange("snpRegionLength", oldValue, snpRegionLength);
        }
    }

    public static class SNPRegionsInGenomeParametersBeanInfo extends BeanInfoEx2<SNPRegionsInGenomeParameters>
    {
        public SNPRegionsInGenomeParametersBeanInfo()
        {
            super(SNPRegionsInGenomeParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            property( "snpTrack" ).inputElement( Track.class ).add();
            add("snpRegionLength");
            property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$snpTrack$ regions" ).add();
        }
    }
}
