package biouml.plugins.state.analyses;

import ru.biosoft.access.core.DataElementPath;
import com.developmentontheedge.beans.annot.PropertyName;

public class ChangeDiagramParameters extends DiagramAndChanges
{
    private DataElementPath outputDiagram;
    @PropertyName ( "Output diagram" )
    public DataElementPath getOutputDiagram()
    {
        return outputDiagram;
    }
    public void setOutputDiagram(DataElementPath outputDiagram)
    {
        this.outputDiagram = outputDiagram;
    }

}
