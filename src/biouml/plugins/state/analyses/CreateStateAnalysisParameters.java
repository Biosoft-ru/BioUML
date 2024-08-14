package biouml.plugins.state.analyses;

import ru.biosoft.access.core.DataElementPath;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class CreateStateAnalysisParameters extends DiagramAndChanges
{
    private DataElementPath statePath;
    @PropertyName ( "Resulting state" )
    @PropertyDescription ( "Resulting state" )
    public DataElementPath getStatePath()
    {
        return statePath;
    }
    public void setStatePath(DataElementPath statePath)
    {
        this.statePath = statePath;
    }
}