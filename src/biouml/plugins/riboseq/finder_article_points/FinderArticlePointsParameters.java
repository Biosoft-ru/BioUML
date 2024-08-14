package biouml.plugins.riboseq.finder_article_points;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class FinderArticlePointsParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputArticlePointTable;
    private DataElementPath inputAllClusterTrack;
    private DataElementPath inputFilteredTrack;
    private DataElementPath inputSvmYesTrack;

    private DataElementPath outputStatisticTable;

    @PropertyName ( "Article point table" )
    @PropertyDescription ( "table with nonintersecting point of comparison" )
    public DataElementPath getInputArticlePointTable()
    {
        return inputArticlePointTable;
    }

    public void setInputArticlePointTable(DataElementPath inputArticlePointTable)
    {
        Object oldValue = this.inputArticlePointTable;
        this.inputArticlePointTable = inputArticlePointTable;
        firePropertyChange( "inputArticlePointTable", oldValue, this.inputArticlePointTable );
    }

    @PropertyName ( "All Clusters Track" )
    @PropertyDescription ( "all clusters from reads" )
    public DataElementPath getInputAllClusterTrack()
    {
        return inputAllClusterTrack;
    }

    public void setInputAllClusterTrack(DataElementPath inputAllClusterTrack)
    {
        Object oldValue = this.inputAllClusterTrack;
        this.inputAllClusterTrack = inputAllClusterTrack;
        firePropertyChange( "inputAllClusterTrack", oldValue, this.inputAllClusterTrack );
    }

    @PropertyName ( "Filtered Track" )
    @PropertyDescription ( "track after filtered by max length and min number of sites" )
    public DataElementPath getInputFilteredTrack()
    {
        return inputFilteredTrack;
    }

    public void setInputFilteredTrack(DataElementPath inputFilteredTrack)
    {
        Object oldValue = this.inputFilteredTrack;
        this.inputFilteredTrack = inputFilteredTrack;
        firePropertyChange( "inputFilteredTrack", oldValue, this.inputFilteredTrack );
    }

    @PropertyName ( "SVM Track" )
    @PropertyDescription ( "track after using SVM on filtered track" )
    public DataElementPath getInputSvmYesTrack()
    {
        return inputSvmYesTrack;
    }

    public void setInputSvmYesTrack(DataElementPath inputSvmYesTrack)
    {
        Object oldValue = this.inputSvmYesTrack;
        this.inputSvmYesTrack = inputSvmYesTrack;
        firePropertyChange( "inputSvmYesTrack", oldValue, this.inputSvmYesTrack );
    }

    public DataElementPath getOutputStatisticTable()
    {
        return outputStatisticTable;
    }

    public void setOutputStatisticTable(DataElementPath outputStatisticTable)
    {
        Object oldValue = this.outputStatisticTable;
        this.outputStatisticTable = outputStatisticTable;
        firePropertyChange( "outputStatisticTable", oldValue, this.outputStatisticTable );
    }
}
