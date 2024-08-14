package biouml.plugins.bindingregions.analysis;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.SampleComparison;
import biouml.plugins.bindingregions.utils.SampleConstruction;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.exception.DataElementNotAcceptableException;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author yura
 * Comparison of site models: Read available AUCs in tables and summarize their differences
 *
 */
public class SummaryOnAUCs extends AnalysisMethodSupport<SummaryOnAUCs.SummaryOnAUCsParameters>
{
    public SummaryOnAUCs(DataCollection<?> origin, String name)
    {
        super(origin, name, new SummaryOnAUCsParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Comparison of site models: Read available AUCs in tables and summarize their differences");
        DataElementPath pathToCollectionOfFolders = parameters.getPathToCollectionOfFolders();
        int percentage = parameters.getBestSitesPercentage();
        boolean isRevised = parameters.getRevised();
        int minimalSize = parameters.minimalSize;
        final DataElementPath pathToOutputs = parameters.getOutputPath();
        
        // 1.
        log.info("Create samples");
        SampleComparison sc = getSamples(pathToCollectionOfFolders, percentage, isRevised, minimalSize, 0, 50);
        
        // 2.
        log.info("Create and write tables");
        String additionalName = "_percentage_" + percentage;
        if( isRevised )
            additionalName += "_" + SiteModelsComparisonUtils.REVISED;
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        sc.writeTableWithMeanAndSigma(pathToOutputs, "meanAndSigma" + additionalName);
        jobControl.setPreparedness(55);
        if( jobControl.isStopped() ) return null;
        sc.writeTableWithWilcoxonPaired(pathToOutputs, "wilcoxonSignedRankTest" + additionalName);
        jobControl.setPreparedness(70);
        if( jobControl.isStopped() ) return null;
        sc.writeTableWithFriedmanRankTest(pathToOutputs, "friedmanRankTest" + additionalName);
        jobControl.setPreparedness(80);
        if( jobControl.isStopped() ) return null;
        sc.writeTableWithBinomialTest(pathToOutputs, "binomialTest" + additionalName);
        jobControl.setPreparedness(90);
        if( jobControl.isStopped() ) return null;
        sc.writeChartsWithSmoothedDensitiesOfDifferences(true, pathToOutputs.getChildPath("densitiesOfDifferences" + additionalName + "_charts"));
        jobControl.setPreparedness(100);
        return pathToOutputs.getDataCollection();
    }

    private SampleComparison getSamples(DataElementPath pathToCollectionOfFolders, int percentage, boolean isRevised, int minimalSize, int fromJobControl, int toJobControl)
    {
        String inequalityColumnName = SiteModelsComparisonUtils.SIZE_OF_SEQUENCE_SET;
        SampleConstruction sConstruction = null;
        String inputTableName = isRevised ? SiteModelsComparisonUtils.AUCS_REVISED : SiteModelsComparisonUtils.AUCS;
        ru.biosoft.access.core.DataElementPath[] paths = pathToCollectionOfFolders.getChildrenArray();
        int difference = toJobControl - fromJobControl;
        for( int i = 0; i < paths.length; i++)
        {
            DataElement de = paths[i].getChildPath(inputTableName).optDataElement();
            if( ! (de instanceof TableDataCollection) )  continue;
            TableDataCollection table = (TableDataCollection)de;
            if( sConstruction == null )
                sConstruction = new SampleConstruction(table, SiteModelsComparisonUtils.AUC, SiteModelsComparisonUtils.PERCENTAGE_OF_BEST_SITES, percentage, inequalityColumnName, minimalSize);
            else
                sConstruction.subjointSamples(table, SiteModelsComparisonUtils.PERCENTAGE_OF_BEST_SITES, percentage, inequalityColumnName, minimalSize);
            jobControl.setPreparedness(fromJobControl + (i + 1) * difference / paths.length);
        }
        if( sConstruction == null )
            throw new DataElementNotAcceptableException(pathToCollectionOfFolders, "No tables found in folder");
        return sConstruction.transformToSampleComparison();
    }

    public static class SummaryOnAUCsParameters extends AbstractAnalysisParameters
    {
        private DataElementPath pathToCollectionOfFolders;
        private int bestSitesPercentage = 15;
        private DataElementPath outputPath;
        boolean revised = false;
        private int minimalSize = 500;
        
        @PropertyName(MessageBundle.PN_PATH_TO_COLLECTION_OF_FOLDERS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_COLLECTION_OF_FOLDERS)
        public DataElementPath getPathToCollectionOfFolders()
        {
            return pathToCollectionOfFolders;
        }
        public void setPathToCollectionOfFolders(DataElementPath pathToCollectionOfFolders)
        {
            Object oldValue = this.pathToCollectionOfFolders;
            this.pathToCollectionOfFolders = pathToCollectionOfFolders;
            firePropertyChange("pathToCollectionOfFolders", oldValue, pathToCollectionOfFolders);
        }
        
        @PropertyName(MessageBundle.PN_BEST_SITES_PERCENTAGE)
        @PropertyDescription(MessageBundle.PD_BEST_SITES_PERCENTAGE)
        public int getBestSitesPercentage()
        {
            return bestSitesPercentage;
        }

        public void setBestSitesPercentage(int bestSitesPercentage)
        {
            Object oldValue = this.bestSitesPercentage;
            this.bestSitesPercentage = bestSitesPercentage;
            firePropertyChange("bestSitesPercentage", oldValue, bestSitesPercentage);
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
        
        @PropertyName(MessageBundle.PN_REVISED)
        @PropertyDescription(MessageBundle.PD_REVISED)
        public boolean getRevised()
        {
            return revised;
        }
        public void setRevised(boolean revised)
        {
            Object oldValue = this.revised;
            this.revised = revised;
            firePropertyChange("revised", oldValue, revised);
        }
        
        @PropertyName(MessageBundle.PN_MINIMAL_SIZE)
        @PropertyDescription(MessageBundle.PD_MINIMAL_SIZE)
        public int getMinimalSize()
        {
            return minimalSize;
        }

        public void setMinimalSize(int minimalSize)
        {
            Object oldValue = this.minimalSize;
            this.minimalSize = minimalSize;
            firePropertyChange("minimalSize", oldValue, minimalSize);
        }
    }
    
    public static class SummaryOnAUCsParametersBeanInfo extends BeanInfoEx2<SummaryOnAUCsParameters>
    {
        public SummaryOnAUCsParametersBeanInfo()
        {
            super(SummaryOnAUCsParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "pathToCollectionOfFolders" ).inputElement( FolderCollection.class ).add();
            add("bestSitesPercentage");
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
            add("revised");
            add("minimalSize");
        }
    }
}
