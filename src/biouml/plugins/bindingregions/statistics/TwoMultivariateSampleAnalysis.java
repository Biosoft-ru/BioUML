package biouml.plugins.bindingregions.statistics;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.MultivariateSamples;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.TableUtils;

/**
 * @author yura
 * Analysis of two multivariate samples
 */
public class TwoMultivariateSampleAnalysis extends AnalysisMethodSupport<TwoMultivariateSampleAnalysis.TwoMultivariateSampleAnalysisParameters>
{
    public TwoMultivariateSampleAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new TwoMultivariateSampleAnalysisParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Analysis of two multivariate samples");
        DataElementPath pathToTable1 = parameters.getPathToTableWithSample1();
        DataElementPath pathToTable2 = parameters.getPathToTableWithSample2();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        Object[] objects = TableUtils.readDoubleMatrixInTable(pathToTable1);
        String[] objectNames1 = (String[])objects[0];
        String[] variableNames = (String[])objects[1];
        double[][] sample1 = (double[][])objects[2];
        objects = TableUtils.readDoubleMatrixInTable(pathToTable2);
        String[] objectNames2 = (String[])objects[0];
        String[] variableNames2 = (String[])objects[1];
        double[][] sample2 = (double[][])objects[2];

        log.info("variableNames : " + variableNames.length + " variableNames2 : " + variableNames2.length);
        for( int i = 0; i < variableNames.length; i++ )
        {
            log.info(variableNames[i] + "; " + variableNames2[i]);
            if( ! variableNames[i].equals(variableNames2[i]) )
                log.info(" error!!! ");
        }
        MultivariateSamples ms = new MultivariateSamples("2010", objectNames1, sample1, "2014", objectNames2, sample2, variableNames);
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        ms.writeChartsWithVariablesDensities(pathToOutputs, true, null, DensityEstimation.WINDOW_WIDTH_03, null, jobControl, 0, 90);
        ms.writeTableWithWilcoxonAndStudent(pathToOutputs, jobControl, 90, 100);
        return pathToOutputs.getDataCollection();
    }

    public static class TwoMultivariateSampleAnalysisParameters extends AbstractAnalysisParameters
    {
        private DataElementPath pathToTableWithSample1;
        private DataElementPath pathToTableWithSample2;
        private DataElementPath outputPath;

        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_SAMPLE1)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_SAMPLE1)
        public DataElementPath getPathToTableWithSample1()
        {
            return pathToTableWithSample1;
        }
        public void setPathToTableWithSample1(DataElementPath pathToTableWithSample1)
        {
            Object oldValue = this.pathToTableWithSample1;
            this.pathToTableWithSample1 = pathToTableWithSample1;
            firePropertyChange("pathToTableWithSample1", oldValue, pathToTableWithSample1);
        }

        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_SAMPLE2)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_SAMPLE2)
        public DataElementPath getPathToTableWithSample2()
        {
            return pathToTableWithSample2;
        }
        public void setPathToTableWithSample2(DataElementPath pathToTableWithSample2)
        {
            Object oldValue = this.pathToTableWithSample2;
            this.pathToTableWithSample2 = pathToTableWithSample2;
            firePropertyChange("pathToTableWithSample2", oldValue, pathToTableWithSample2);
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

    public static class TwoMultivariateSampleAnalysisParametersBeanInfo extends BeanInfoEx2<TwoMultivariateSampleAnalysisParameters>
    {
        public TwoMultivariateSampleAnalysisParametersBeanInfo()
        {
            super(TwoMultivariateSampleAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "pathToTableWithSample1" ).inputElement( TableDataCollection.class ).add();
            property( "pathToTableWithSample2" ).inputElement( TableDataCollection.class ).add();
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
