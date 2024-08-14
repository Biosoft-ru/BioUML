package biouml.plugins.riboseq.ingolia;

import ru.biosoft.access.core.DataElementPath;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class PredictStartSitesParameters extends BasicParameters
{
    private DataElementPath modelFile;
    @PropertyName("SVM model")
    @PropertyDescription("Trained SVM model from BuildProfileModel analysis")
    public DataElementPath getModelFile()
    {
        return modelFile;
    }
    public void setModelFile(DataElementPath modelFile)
    {
        this.modelFile = modelFile;
    }
    
    private DataElementPath summaryTable;
    @PropertyName("Summary table")
    @PropertyDescription("Table with predicted translation start sites")
    public DataElementPath getSummaryTable()
    {
        return summaryTable;
    }
    public void setSummaryTable(DataElementPath summaryTable)
    {
        this.summaryTable = summaryTable;
    }
    
    
    private DataElementPath outputTrack;
    @PropertyName("Output track")
    @PropertyDescription("Track with prediced translation start sites")
    public DataElementPath getOutputTrack()
    {
        return outputTrack;
    }
    public void setOutputTrack(DataElementPath outputTrack)
    {
        DataElementPath oldValue = this.outputTrack;
        this.outputTrack = outputTrack;
        firePropertyChange( "outputTrack", oldValue, outputTrack );
    }
}
