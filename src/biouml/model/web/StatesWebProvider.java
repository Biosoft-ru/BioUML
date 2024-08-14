package biouml.model.web;

import com.developmentontheedge.beans.undo.Transactable;
import com.developmentontheedge.beans.undo.TransactionUndoManager;

import biouml.model.Diagram;
import biouml.standard.state.State;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.diagram.ViewEditorPaneStub;
import ru.biosoft.access.core.undo.DataCollectionUndoListener;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.BiosoftWebResponse;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebProviderSupport;

public class StatesWebProvider extends WebProviderSupport
{
    private static final String APPLY_STATE = "apply";
    private static final String RESTORE_DIAGRAM = "restore";
    private static final String ADD_STATE = "add";
    private static final String REMOVE_STATE = "remove";
    private static final String GET_CURRENT_STATE = "current";

    @Override
    public void process(BiosoftWebRequest req, BiosoftWebResponse resp) throws Exception
    {
        String action = req.getAction();
        Diagram diagram = WebDiagramsProvider.getDiagramChecked(req.getDataElementPath());
        JSONResponse response = new JSONResponse(resp);
        if ( APPLY_STATE.equals(action) )
        {
            String stateName = req.getString("state");
            State state = diagram.getState(stateName);
            if ( state != null )
            {
                diagram.setStateEditingMode(state, getTransactable(diagram));
            }
            response.sendString("changed");
        }
        else if ( RESTORE_DIAGRAM.equals(action) )
        {
            diagram.restore();
            response.sendString("changed");
        }
        else if ( ADD_STATE.equals(action) )
        {
            String stateName = req.getString("state");
            State oldState = diagram.getState(stateName);
            if ( oldState != null )
                response.error("State \"" + stateName + "\" already exists");
            else
            {
                State newState = new State(diagram, stateName);
                diagram.addState(newState);
                response.sendString(stateName);
            }
        }
        else if ( REMOVE_STATE.equals(action) )
        {
            String stateName = req.getString("state");
            String respStr = "ok";
            State state = diagram.getState(stateName);
            if ( diagram.getCurrentState() == state )
            {
                diagram.restore();
                respStr = "changed";
            }
            diagram.removeState(state);
            response.sendString(respStr);
        }
        else if ( GET_CURRENT_STATE.equals(action) )
        {
            String curStateName = diagram.getCurrentStateName();
            response.sendString(curStateName);
        }
        else
        {
            response.error("Unexpected state action '" + action + "'.");
        }

    }

    private Transactable getTransactable(Diagram diagram)
    {
        DiagramEditorHelper helper = new DiagramEditorHelper(diagram);
        DataCollectionUndoListener diagramListener = WebDiagramsProvider.getUndoListener(diagram);
        ViewEditorPaneStub viewEditorStub = new ViewEditorPaneStub(helper, diagram, (TransactionUndoManager) diagramListener.getTransactionListener());
        return viewEditorStub;
    }

}
