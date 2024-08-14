package biouml.plugins.riboseq.comparison_article;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class ComparatorParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputArticleCSV;
    private DataElementPath inputGeneInfo;
    private DataElementPath inputYesTrack;

    private DataElementPath outputStatistic;
    private DataElementPath outputUniqueArticlePointTable;

    @PropertyName ( "Article csv" )
    @PropertyDescription ( "input file with article's result" )
    public DataElementPath getInputArticleCSV()
    {
        return inputArticleCSV;
    }

    public void setInputArticleCSV(DataElementPath inputArticleCSV)
    {
        Object oldValue = this.inputArticleCSV;
        this.inputArticleCSV = inputArticleCSV;
        firePropertyChange( "inputArticleCSV", oldValue, this.inputArticleCSV );
    }

    @PropertyName ( "Gene Information" )
    @PropertyDescription ( "input file with information about genes" )
    public DataElementPath getInputGeneInfo()
    {
        return inputGeneInfo;
    }

    public void setInputGeneInfo(DataElementPath inputGeneInfo)
    {
        Object oldValue = this.inputGeneInfo;
        this.inputGeneInfo = inputGeneInfo;
        firePropertyChange( "inputGeneInfo", oldValue, this.inputGeneInfo );
    }

    @PropertyName ( "Yes Track" )
    @PropertyDescription ( "input track - result of workflow" )
    public DataElementPath getInputYesTrack()
    {
        return inputYesTrack;
    }

    public void setInputYesTrack(DataElementPath inputYesTrack)
    {
        Object oldValue = this.inputYesTrack;
        this.inputYesTrack = inputYesTrack;
        firePropertyChange( "inputYesTrack", oldValue, this.inputYesTrack );
    }

    @PropertyName ( "Comparison result" )
    @PropertyDescription ( "output file with result of comparison" )
    public DataElementPath getOutputStatistic()
    {
        return outputStatistic;
    }

    public void setOutputStatistic(DataElementPath outputStatistic)
    {
        Object oldValue = this.outputStatistic;
        this.outputStatistic = outputStatistic;
        firePropertyChange( "outputStatistic", oldValue, this.outputStatistic );
    }

    @PropertyName ( "Unique Article Point Table" )
    @PropertyDescription ( "output article points nonintersecting after comparison" )
    public DataElementPath getOutputUniqueArticlePointTable()
    {
        return outputUniqueArticlePointTable;
    }

    public void setOutputUniqueArticlePointTable(DataElementPath outputUniqueArticlePointTable)
    {
        Object oldValue = this.outputUniqueArticlePointTable;
        this.outputUniqueArticlePointTable = outputUniqueArticlePointTable;
        firePropertyChange( "outputUniqueArticlePointTable", oldValue, this.outputUniqueArticlePointTable );
    }
}
