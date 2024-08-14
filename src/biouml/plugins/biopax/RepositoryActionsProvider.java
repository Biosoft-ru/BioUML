// $ Id: $
package biouml.plugins.biopax;

import javax.swing.Action;

import biouml.model.Module;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionInitializer;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.ActionsProvider;

public class RepositoryActionsProvider implements ActionsProvider
{
    private boolean initialized = false;

    @Override
    public Action[] getActions(Object obj)
    {
        ActionManager actionManager = Application.getActionManager();
        if( !initialized )
        {
            Action action = new BioPAXExportAction();
            actionManager.addAction(BioPAXExportAction.KEY, new BioPAXExportAction());
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(action, BioPAXExportAction.KEY);
            
            action = new BioPAXImportAction();
            actionManager.addAction(BioPAXImportAction.KEY, new BioPAXImportAction());
            initializer.initAction(action, BioPAXImportAction.KEY);

            initialized = true;
        }

        Module module = null;

        if( obj instanceof Module )
        {
            module = (Module)obj;
            if( module.getType() != null && ( module.getType() instanceof BioPAXTextModuleType || module.getType() instanceof BioPAXSQLModuleType) )
            {
                Action biopaxExportAction = actionManager.getAction(BioPAXExportAction.KEY);
                biopaxExportAction.putValue(BioPAXExportAction.DATABASE, module);
                
                Action biopaxImportAction = actionManager.getAction(BioPAXImportAction.KEY);
                biopaxImportAction.putValue(BioPAXImportAction.DATABASE, module);

                return new Action[] {biopaxExportAction, biopaxImportAction};
            }
        }

        return null;
    }
}