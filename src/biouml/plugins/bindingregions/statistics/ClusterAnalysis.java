
package biouml.plugins.bindingregions.statistics;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.ClusteringQualityIndexNamesSelector;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.ClusterizationTypesEditor;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.DataTransformationTypeSelector;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.DistanceTypeSelector;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.VariableNamesSelector;
import biouml.plugins.bindingregions.statisticsutils.ClusterizationEngine;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.MultivariateSample.Transformation;
import biouml.plugins.bindingregions.utils.TableUtils;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class ClusterAnalysis extends AnalysisMethodSupport<ClusterAnalysis.ClusterAnalysisParameters>
{
    public ClusterAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new ClusterAnalysisParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Cluster analysis");
        
        String clusterizationType = parameters.getClusterizationType();
        DataElementPath pathToTableWithDataMatrix = parameters.getPathToTableWithDataMatrix();
        String[] variableNames = parameters.getVariableNames();
        String dataTransformationType = parameters.getDataTransformationType();
        String distanceType = parameters.getDistanceType();
        int numberOfClusters = Math.max(2, parameters.getNumberOfClusters());
        String[] outputOptions = parameters.getOutputOptions();
        String[] clusteringQualityIndexNames = parameters.getClusteringQualityIndexNames();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // 1. Input parameter correction on hiddenness
        if( parameters.isDistanceTypeHidden() ) distanceType = null;
        if( parameters.areClusteringQualityIndexNamesHidden() ) clusteringQualityIndexNames = null;

        // 2. Data processing
        Object[] objects = TableUtils.readDataSubMatrix(pathToTableWithDataMatrix, variableNames);
        objects = MatrixUtils.removeRowsWithMissingData((String[])objects[0], (double[][])objects[1]);
        String[] objectNames = (String[])objects[0];
        double[][] dataMatrix = (double[][])objects[1];
        jobControl.setPreparedness(10);
        if( jobControl.isStopped() ) return null;

        // 3. Implementation of clustering
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        ClusterizationEngine ce = new ClusterizationEngine(clusterizationType, dataTransformationType, distanceType, objectNames, variableNames, dataMatrix, numberOfClusters, outputOptions, clusteringQualityIndexNames);
        ce.implementClusterization(pathToOutputs, log, jobControl, 10, 100);
        return pathToOutputs.getDataCollection();
    }
    
    public static class ClusterAnalysisParameters extends AbstractStatisticalAnalysisParameters
    {
        public boolean areClusteringQualityIndexNamesHidden()
        {
            String[] outputOptions = getOutputOptions();
            return ! ArrayUtils.contains(outputOptions, ClusterizationEngine.OUTPUT_5_WRITE_QUALITY_INDICES) && ! ArrayUtils.contains(outputOptions, ClusterizationEngine.OUTPUT_6_WRITE_QUALITY_INDICES_TRANSFORMED); 
        }
    }
    
    public static class OutputOptionsSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                String[] outputOptions = new String[]{ClusterizationEngine.OUTPUT_1_WRITE_INDICES_OF_CLUSTERS_AND_DATA_MATRIX, ClusterizationEngine.OUTPUT_3_WRITE_MEANS_AND_SIGMAS, ClusterizationEngine.OUTPUT_5_WRITE_QUALITY_INDICES, ClusterizationEngine.OUTPUT_7_WRITE_DISTANCES_BETWEEN_CENTERS, ClusterizationEngine.OUTPUT_9_WRITE_DENSITIES};
                String clusterizationType = ((ClusterAnalysisParameters)getBean()).getClusterizationType();
                if( clusterizationType.equals(ClusterizationEngine.CLUSTERIZATION_2_FUNNY) || clusterizationType.equals(ClusterizationEngine.CLUSTERIZATION_3_MCLUST) )
                    outputOptions = (String[])ArrayUtils.add(outputOptions, outputOptions.length, ClusterizationEngine.OUTPUT_2_WRITE_MEMBERSHIP_PROBABILITIES);
                String dataTransformationType = ((ClusterAnalysisParameters)getBean()).getDataTransformationType();
                if( ! dataTransformationType.equals(Transformation.NO_TRANSFORMATION) )
                {
                    outputOptions = (String[])ArrayUtils.add(outputOptions, outputOptions.length, ClusterizationEngine.OUTPUT_4_WRITE_MEANS_AND_SIGMAS_TRANSFORMED);
                    outputOptions = (String[])ArrayUtils.add(outputOptions, outputOptions.length, ClusterizationEngine.OUTPUT_6_WRITE_QUALITY_INDICES_TRANSFORMED);
                    outputOptions = (String[])ArrayUtils.add(outputOptions, outputOptions.length, ClusterizationEngine.OUTPUT_8_WRITE_DISTANCES_BETWEEN_CENTERS_TRANSFORMED);
                    outputOptions = (String[])ArrayUtils.add(outputOptions, outputOptions.length, ClusterizationEngine.OUTPUT_10_WRITE_DENSITIES_TRANSFORMED);
                }
                return outputOptions;
            }
            catch( Exception e )
            {
                return new String[]{"(please select output options)"};
            }
        }
    }

    public static class ClusterAnalysisParametersBeanInfo extends BeanInfoEx2<ClusterAnalysisParameters>
    {
        public ClusterAnalysisParametersBeanInfo()
        {
            super(ClusterAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("clusterizationType", beanClass), ClusterizationTypesEditor.class);
            add(DataElementPathEditor.registerInput("pathToTableWithDataMatrix", beanClass, TableDataCollection.class));
            add("variableNames", VariableNamesSelector.class);
            add(new PropertyDescriptorEx("dataTransformationType", beanClass), DataTransformationTypeSelector.class);
            add("numberOfClusters");
            add("outputOptions", OutputOptionsSelector.class);
            addHidden(new PropertyDescriptorEx("distanceType", beanClass), DistanceTypeSelector.class, "isDistanceTypeHidden");
            addHidden("clusteringQualityIndexNames", ClusteringQualityIndexNamesSelector.class, "areClusteringQualityIndexNamesHidden");
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class));
        }
    }
}