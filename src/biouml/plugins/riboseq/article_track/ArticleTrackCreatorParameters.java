package biouml.plugins.riboseq.article_track;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class ArticleTrackCreatorParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputArticleCSV;
    private DataElementPath inputGeneInfo;

    private DataElementPath inputReferenceTrack;

    private DataElementPath outputTrack;

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

    @PropertyName ( "Reference track" )
    @PropertyDescription ( "reference track for only copy parameters" )
    public DataElementPath getInputReferenceTrack()
    {
        return inputReferenceTrack;
    }

    public void setInputReferenceTrack(DataElementPath inputReferenceTrack)
    {
        Object oldValue = this.inputReferenceTrack;
        this.inputReferenceTrack = inputReferenceTrack;
        firePropertyChange( "inputReferenceTrack", oldValue, this.inputReferenceTrack );
    }

    @PropertyName ( "Article track" )
    @PropertyDescription ( "track with article points" )
    public DataElementPath getOutputTrack()
    {
        return outputTrack;
    }

    public void setOutputTrack(DataElementPath outputTrack)
    {
        Object oldValue = this.outputTrack;
        this.outputTrack = outputTrack;
        firePropertyChange( "outputTrack", oldValue, this.outputTrack );
    }
}
