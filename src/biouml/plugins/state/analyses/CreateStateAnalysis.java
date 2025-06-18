package biouml.plugins.state.analyses;

import java.beans.IntrospectionException;

import javax.swing.undo.UndoableEdit;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.BeanUtil;
import com.developmentontheedge.beans.util.Beans.ObjectPropertyAccessor;
import ru.biosoft.util.TextUtil2;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.standard.state.State;
import biouml.standard.state.StatePropertyChangeUndo;

public class CreateStateAnalysis extends AnalysisMethodSupport<CreateStateAnalysisParameters>
{
    public CreateStateAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new CreateStateAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = parameters.getDiagramPath().getDataElement( Diagram.class );
        State state = new State( parameters.getStatePath().getParentCollection(), diagram, parameters.getStatePath().getName() );
        for( StateChange stateChange : parameters.getChanges() )
        {
            UndoableEdit propertyChangeEdit = createEdit( diagram, stateChange );
            state.getStateUndoManager().addEdit( propertyChangeEdit );
        }
        parameters.getStatePath().save( state );
        return state;
    }

    public static UndoableEdit createEdit(Diagram diagram, StateChange stateChange) throws IntrospectionException, Exception
    {
        DiagramElement bean = diagram.getDiagramElement( stateChange.getElementId() );
        ObjectPropertyAccessor accessor = BeanUtil.getBeanPropertyAccessor( bean, stateChange.getElementProperty() );
        Object newValue = TextUtil2.fromString( accessor.getType(), stateChange.getPropertyValue() );
        Object oldValue = accessor.get();
        return new StatePropertyChangeUndo( bean, stateChange.getElementProperty(), oldValue, newValue );
    }
}
