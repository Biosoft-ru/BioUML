package biouml.plugins.research.workflow.actions;

import javax.swing.JOptionPane;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.gui.DocumentManager;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.research.ResearchModuleType;
import biouml.plugins.research.workflow.WorkflowDiagramType;

/**
 * New workflow action
 */
public class NewWorkflowAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DataCollection parent = (DataCollection)de;
        
        String diagramName = "";
        while( ( 0 == diagramName.length() ) || ( parent.contains(diagramName) ) )
        {
            diagramName = JOptionPane.showInputDialog("Enter workflow name");
            if( null == diagramName )
                break;
        }
        if( diagramName != null )
        {
            WorkflowDiagramType wdt = new WorkflowDiagramType();
            Diagram diagram = wdt.createDiagram(parent, diagramName, null);
            parent.put(diagram);
            DocumentManager.getDocumentManager().openDocument(diagram);
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        if(!(de instanceof DataCollection)) return false;
        DataCollection dc = ( (DataCollection)de );
        Module module = Module.optModule(dc);
        if( module == null || !( module.getType() instanceof ResearchModuleType ) ) return false;
        return DataCollectionUtils.isAcceptable(dc, Diagram.class);
    }
}
