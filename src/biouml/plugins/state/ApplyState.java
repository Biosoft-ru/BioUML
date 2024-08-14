
package biouml.plugins.state;

import javax.swing.undo.UndoableEdit;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import biouml.model.Diagram;
import biouml.standard.state.DiagramStateUtility;
import biouml.standard.state.State;

/**
 * @author anna
 *
 */
@ClassIcon ( "resources/apply-state.gif" )
public class ApplyState extends AnalysisMethodSupport<ApplyStateParameters>
{
    public ApplyState(DataCollection origin, String name)
    {
        super( origin, name, new ApplyStateParameters() );
    }

    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        ApplyStateParameters params = getParameters();
        DataElementPath input = params.getInputDiagramPath();
        DataElementPath statePath = params.getStatePath();
        boolean sameDiagram = params.applyToSameDiagram();

        Diagram diagram = input.getDataElement( Diagram.class );
        State state = statePath.getDataElement( State.class );

        Diagram resultDiagram;
        if( !sameDiagram )
        {
            DataElementPath output = params.getOutputDiagramPath();
            if( output.exists() )
            {
                output.remove();
            }
            resultDiagram = diagram.clone( output.optParentCollection(), output.getName() );
        }
        else
        {
            resultDiagram = diagram;
        }
   
        DiagramStateUtility.applyState( resultDiagram, state, params.getStateName() );
        if( sameDiagram )
            resultDiagram.restore();
        else if( parameters.isWriteStateToDiagram() )
        {
            Diagram d = resultDiagram.clone( resultDiagram.getOrigin(), resultDiagram.getName() );
            State currentState = d.getCurrentState();
            d.removeStates();
            for(UndoableEdit edit : currentState.getStateUndoManager().getEdits())
                edit.redo();
            resultDiagram = d;
        }
        resultDiagram.save();
        return resultDiagram;
    }
}
