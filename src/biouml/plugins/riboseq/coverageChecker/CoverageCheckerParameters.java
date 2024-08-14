package biouml.plugins.riboseq.coverageChecker;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class CoverageCheckerParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputBamTrackPath;

    private DataElementPath inputArticleCSV;
    private DataElementPath inputGeneInfo;

    private DataElementPath outputStatisticTablePath;

    @PropertyName ( "input BAM track" )
    @PropertyDescription ( "track with reads" )
    public DataElementPath getInputBamTrackPath()
    {
        return inputBamTrackPath;
    }

    public void setInputBamTrackPath(DataElementPath inputBamTrackPath)
    {
        Object oldValue = this.inputBamTrackPath;
        this.inputBamTrackPath = inputBamTrackPath;
        firePropertyChange( "inputBamTrackPath", oldValue, this.inputBamTrackPath );
    }

    @PropertyName ( "output statistic table" )
    @PropertyDescription ( "table with coverage result" )
    public DataElementPath getOutputStatisticTablePath()
    {
        return outputStatisticTablePath;
    }

    public void setOutputStatisticTablePath(DataElementPath outputStatisticTablePath)
    {
        DataElementPath oldValue = this.outputStatisticTablePath;
        this.outputStatisticTablePath = outputStatisticTablePath;
        firePropertyChange( "outputStatisticTablePath", oldValue, this.outputStatisticTablePath );
    }

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
}
