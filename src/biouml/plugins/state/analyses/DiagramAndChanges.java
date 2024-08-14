package biouml.plugins.state.analyses;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class DiagramAndChanges extends AbstractAnalysisParameters
{
    public DiagramAndChanges()
    {
        setChanges( new StateChange[] { new StateChange() } );
    }
    
    private DataElementPath diagramPath;
    @PropertyName ( "Diagram" )
    @PropertyDescription ( "Diagram" )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }

    public void setDiagramPath(DataElementPath diagramPath)
    {
        this.diagramPath = diagramPath;
    }

    private StateChange[] changes;
    @PropertyName ( "Changes" )
    @PropertyDescription ( "Changes" )
    public StateChange[] getChanges()
    {
        return changes;
    }
    public void setChanges(StateChange[] changes)
    {
        if( changes != null )
            for( StateChange change : changes )
                change.setParent( this );
        this.changes = changes;
    }
}
