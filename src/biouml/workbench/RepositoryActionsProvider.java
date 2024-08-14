package biouml.workbench;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.Action;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.TextUtil;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.workbench.diagram.NewDiagramAction;
import biouml.workbench.module.ExportModuleAction;
import biouml.workbench.module.NewModuleAction;
import biouml.workbench.module.RemoveModuleAction;
import biouml.workbench.module.xml.EditModuleAction;
import biouml.workbench.module.xml.NewCompositeModuleAction;
import biouml.workbench.module.xml.XmlModule;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.action.ActionManager;
import com.developmentontheedge.beans.ActionsProvider;

public class RepositoryActionsProvider implements ActionsProvider
{
    private static final @Nonnull DataElementPath ANALYSES_PATH = DataElementPath.create("analyses");

    @Override
    public Action[] getActions(Object obj)
    {
        if( ! ( obj instanceof DataElement ) )
            return null;

        final DataElement de = (DataElement)obj;
        ActionManager actionManager = Application.getActionManager();

        if( de.getOrigin() == null || de.getOrigin().getCompletePath().isDescendantOf(ANALYSES_PATH) )
            return null;

        List<Action> actions = new ArrayList<>();

        Action importDocumentAction = actionManager.getAction(ImportElementAction.KEY);
        if( de instanceof DataCollection && DataElementImporterRegistry.isImportAvailableForCollection((DataCollection<?>)de) )
        {
            importDocumentAction.putValue(ImportElementAction.DATABASE, de);
            importDocumentAction.setEnabled(true);
        }
        else
        {
            importDocumentAction.setEnabled(false);
            importDocumentAction = null;
        }

        // New module
        if( TextUtil.isFullPath(de.getName()) )
        {
            actions.add(actionManager.getAction(NewModuleAction.KEY));
            actions.add(actionManager.getAction(NewCompositeModuleAction.KEY));
        }
        else if( de instanceof Module ) // Module actions
        {
            Action removeModuleAction = actionManager.getAction(RemoveModuleAction.KEY);
            removeModuleAction.putValue(RemoveModuleAction.DATABASE, de);
            actions.add(removeModuleAction);

            Action exportModuleAction = actionManager.getAction(ExportModuleAction.KEY);
            exportModuleAction.putValue(ExportModuleAction.DATABASE, de);
            actions.add(exportModuleAction);

            if( importDocumentAction != null )
            {
                actions.add(importDocumentAction);
                importDocumentAction = null;
            }

            if( de instanceof XmlModule )
            {
                Action editModuleAction = actionManager.getAction(EditModuleAction.KEY);
                editModuleAction.putValue(EditModuleAction.DATABASE, de);
                actions.add(editModuleAction);
            }
        }
        else if( de.getOrigin().getName().equalsIgnoreCase(Module.IMAGES) ) // Image Files actions
        {
            Action removeDataElementAction = actionManager.getAction(RemoveDataElementAction.KEY);
            removeDataElementAction.putValue(RemoveDataElementAction.DATA_ELEMENT, de);
            actions.add(removeDataElementAction);
        }
        else if( de.getName().equalsIgnoreCase(Module.IMAGES) ) // Images Data Collection actions
        {
            Action importImageDataAction = actionManager.getAction(ImportImageDataElementAction.KEY);
            importImageDataAction.putValue(ImportImageDataElementAction.IMAGES_COLLECTION, de);
            actions.add(importImageDataAction);
        }
        else if( de.getName().equalsIgnoreCase(Module.GRAPHIC_NOTATIONS) ) // Graphic notations collection actions
        {
        }
        else if( de instanceof DataCollection && !de.getName().equalsIgnoreCase(Module.DIAGRAM)
                && ( de.getOrigin().getName().equals(Module.DATA) || de.getOrigin().getName().equals(Module.METADATA) )
                && ( (DataCollection<?>)de ).isMutable() )
        {
            Action newDataElementAction = actionManager.getAction(NewDataElementAction.KEY);
            newDataElementAction.putValue(NewDataElementAction.DATA_COLLECTION, de);
            actions.add(newDataElementAction);
        }
        else if( de instanceof Diagram ) // Diagram actions
        {
            if( importDocumentAction != null )
            {
                actions.add(importDocumentAction);
                importDocumentAction = null;
            }

            Action removeDataElementAction = actionManager.getAction(RemoveDataElementAction.KEY);
            removeDataElementAction.putValue(RemoveDataElementAction.DATA_ELEMENT, de);
            actions.add(removeDataElementAction);
        }
        
        if( DataCollectionUtils.isAcceptable( DataElementPath.create(de), Diagram.class, null ) && Module.optModule(de) != null )
        {
            if( de instanceof DataCollection && ( (DataCollection<?>)de ).isMutable() )
            {
                Action newDiagramAction = actionManager.getAction(NewDiagramAction.KEY);
                newDiagramAction.putValue(NewDiagramAction.COLLECTION, de);
                actions.add(newDiagramAction);
            }
        }
        if( ( de.getOrigin() instanceof FileBasedCollection || de.getOrigin() instanceof FolderCollection || ( de.getOrigin().getOrigin() != null && ( de
                .getOrigin().getOrigin().getName().equals(Module.DATA) || de.getOrigin().getOrigin().getName().equals(Module.METADATA) ) )
                && de.getOrigin().isMutable() ) ) // Data element actions for some data type
        {
            Action removeDataElementAction = actionManager.getAction(RemoveDataElementAction.KEY);
            removeDataElementAction.putValue(RemoveDataElementAction.DATA_ELEMENT, de);
            actions.add(removeDataElementAction);
        }
        if( importDocumentAction != null )
            actions.add(importDocumentAction);

        return actions.toArray(new Action[actions.size()]);
    }
}
