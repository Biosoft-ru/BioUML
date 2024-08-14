package biouml.plugins.riboseq.riboseqanalysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class RiboSeqParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputPath, outputPath;
    private int minNumberSites = 100;
    private int maxLengthCluster = 200;

    @PropertyName ( "Ribo-seq alignments" )
    @PropertyDescription ( "track with reads" )
    public DataElementPath getInputPath()
    {
        return inputPath;
    }

    public void setInputPath(DataElementPath inputPath)
    {
        Object oldValue = this.inputPath;
        this.inputPath = inputPath;
        firePropertyChange( "inputPath", oldValue, this.inputPath );
    }

    @PropertyName ( "Resulting clusters" )
    @PropertyDescription ( "cluster - set of sites with addition information" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange( "outputPath", oldValue, this.outputPath );
    }

    @PropertyName ( "Minimum number sites" )
    @PropertyDescription ( "how many sites can be minimum in the cluster( 0 - without limitation )" )
    public int getMinNumberSites()
    {
        return minNumberSites;
    }

    public void setMinNumberSites(int minNumberSites)
    {
        int oldValue = this.minNumberSites;
        this.minNumberSites = minNumberSites;
        firePropertyChange( "minNumberSites", oldValue, this.minNumberSites );
    }

    @PropertyName ( "Maximum length cluster" )
    @PropertyDescription ( "how long cluster can be maximum( 0 - without limitation )" )
    public int getMaxLengthCluster()
    {
        return maxLengthCluster;
    }

    public void setMaxLengthCluster(int maxLengthCluster)
    {
        int oldValue = this.maxLengthCluster;
        this.maxLengthCluster = maxLengthCluster;
        firePropertyChange( "maxLengthCluster", oldValue, this.maxLengthCluster );
    }
}
