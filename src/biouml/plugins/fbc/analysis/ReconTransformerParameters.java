package biouml.plugins.fbc.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class ReconTransformerParameters extends AbstractAnalysisParameters
{
    private DataElementPath diagramPath;
    private DataElementPath resultPath;

    @PropertyName ( "Diagram path" )
    @PropertyDescription ( "Path to input diagram" )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }
    public void setDiagramPath(DataElementPath modelPath)
    {
        Object oldValue = this.diagramPath;
        this.diagramPath = modelPath;
        firePropertyChange( "diagramPath", oldValue, modelPath );
    }

    @PropertyName ( "Result path" )
    @PropertyDescription ( "Path to result diagram" )
    public DataElementPath getResultPath()
    {
        return resultPath;
    }
    public void setResultPath(DataElementPath modelPath)
    {
        Object oldValue = this.resultPath;
        this.resultPath = modelPath;
        firePropertyChange( "resultPath", oldValue, modelPath );
    }
}
