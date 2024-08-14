package biouml.plugins.bindingregions.analysis;

import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.IPSPrediction;
import biouml.plugins.bindingregions.utils.SNP;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * Extracted from SnpsAndBindingSites/mode0
 * "0. IPS-scores in SNPs (creation of tables: 'ipsScoresInSnps')"
 */
public class IPSInSNP extends AnalysisMethodSupport<IPSInSNP.IPSInSNPParameters>
{
    public IPSInSNP(DataCollection<?> origin, String name)
    {
        super(origin, name, new IPSInSNPParameters());
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("IPS-scores in SNPs");
        DataElementPath pathToTrack = parameters.getSnpTrack();
        DataElementPath pathToMatrices = parameters.getMatrixLibrary();
        log.info("Read SNPs from track "+pathToTrack.getName());
        Map<String, List<SNP>> chromosomeAndSnps = SNP.getSnpsFromVcfTrack(pathToTrack);
        
        jobControl.setPreparedness(10);
        if(jobControl.isStopped())
            return null;
        log.info("Read Ips site models");
        List<IPSSiteModel> ipsSiteModels = IPSPrediction.getIpsSiteModels(pathToMatrices);
        
        jobControl.setPreparedness(20);
        if(jobControl.isStopped())
            return null;

        jobControl.pushProgress(20, 100);
        log.info("Calculate and write IPS-scores in SNPs into table "+parameters.getOutputPath().getName());
        TableDataCollection table = writeIpsScoresInSnpsIntoTable(chromosomeAndSnps, ipsSiteModels, parameters.getDbSelector().getSequenceCollectionPath(), parameters.getOutputPath());
        jobControl.popProgress();
        return table;
    }

    private TableDataCollection writeIpsScoresInSnpsIntoTable(final Map<String, List<SNP>> chromosomeAndSnps, final List<IPSSiteModel> ipsSiteModels, final DataElementPath pathToSequences, DataElementPath dep) throws Exception
    {
        final TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
        table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        table.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
        table.getColumnModel().addColumn("snpID", String.class);
        table.getColumnModel().addColumn("chromosome", String.class);
        table.getColumnModel().addColumn("startPosition", Integer.class);
        table.getColumnModel().addColumn("tfName", String.class);
        table.getColumnModel().addColumn("ipsScoreForReferenceSequence", Double.class);
        table.getColumnModel().addColumn("ipsScoreForChangedSequence", Double.class);
        table.getColumnModel().addColumn("differenceOfIpss", Double.class);
        jobControl.forCollection(chromosomeAndSnps.keySet(), new Iteration<String>()
        {
            int iRow = 0;

            @Override
            public boolean run(final String chromosome)
            {
                AnnotatedSequence annotatedSequence = pathToSequences.getChildPath(chromosome).getDataElement(AnnotatedSequence.class);
                final Sequence sequence = annotatedSequence.getSequence();
                jobControl.forCollection(chromosomeAndSnps.get(chromosome), snp -> {
                    String snpID = snp.getSnpID();
                    int position = snp.getStartPosition();
                    for( IPSSiteModel ipsSiteModel : ipsSiteModels )
                    {
                        String modelName = ipsSiteModel.getName();
                        double[] ipsScores = getMaximalIpsScores(ipsSiteModel, sequence, snp);
                        if( ipsScores == null )
                            continue;
                        float x = (float)ipsScores[0];
                        Float score1 = x;
                        x = (float)ipsScores[1];
                        Float score2 = x;
                        double y = ipsScores[1] - ipsScores[0];
                        x = (float)y;
                        Float difference = x;
                        if( ( y >= 0.5 || y <= -0.5 ) && ( ipsScores[0] >= 3.0 || ipsScores[1] >= 3.0 ) )
                            TableDataCollectionUtils.addRow(table, String.valueOf(iRow++), new Object[] {snpID, chromosome, position,
                                    modelName, score1, score2, difference}, true);
                    }
                    return true;
                });
                return true;
            }
        });
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }

    /***
     * @param ipsSiteModel
     * @param pathToSequences
     * @param chromosome
     * @param snp
     * @return double[2] result :
     * result[0] = maximal IPS score in snpReferenceRegion, result[1] = maximal IPS score in snpChangedRegion
     */
    private static double[] getMaximalIpsScores(IPSSiteModel ipsSiteModel, Sequence sequence, SNP snp)
    {
        double[] result = new double[2];
        int matrixlength = ipsSiteModel.getMatrices()[0].getLength();
        int ipsWindow = ipsSiteModel.getWindow();
        Sequence snpReferenceRegion = snp.getReferenceRegion(sequence, matrixlength, ipsWindow);
        Sequence snpChangedRegion = snp.getChangedRegion(sequence, matrixlength, ipsWindow);
        if( snpReferenceRegion == null || snpChangedRegion == null )
            result[0] = result[1] = 0.0;
        else
        {
            result[0] = IPSPrediction.getMaximalIpsScore(snpReferenceRegion, ipsSiteModel);
            result[1] = IPSPrediction.getMaximalIpsScore(snpChangedRegion, ipsSiteModel);
        }
        return result;
    }

    public static class IPSInSNPParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath outputPath;
        private DataElementPath snpTrack;
        private DataElementPath matrixLibrary;
        
        public IPSInSNPParameters()
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

        @PropertyName(MessageBundle.PN_MATRIX_LIBRARY)
        @PropertyDescription(MessageBundle.PD_MATRIX_LIBRARY)
        public DataElementPath getMatrixLibrary()
        {
            return matrixLibrary;
        }

        public void setMatrixLibrary(DataElementPath matrixLibrary)
        {
            Object oldValue = this.matrixLibrary;
            this.matrixLibrary = matrixLibrary;
            firePropertyChange("matrixLibrary", oldValue, matrixLibrary);
        }
    }
    
    public static class IPSInSNPParametersBeanInfo extends BeanInfoEx2<IPSInSNPParameters>
    {
        public IPSInSNPParametersBeanInfo()
        {
            super(IPSInSNPParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            property( "snpTrack" ).inputElement( Track.class ).add();
            property( "matrixLibrary" ).inputElement( WeightMatrixCollection.class ).add();
            property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$snpTrack$ ips" ).add();
        }
    }
}
