package biouml.plugins.lucene;

import javax.swing.Action;

import ru.biosoft.access.core.DataElement;
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
            Action action = new LuceneSearchAction();
            actionManager.addAction(LuceneSearchAction.KEY, new LuceneSearchAction());
            ActionInitializer initializer = new ActionInitializer(MessageBundle.class);
            initializer.initAction(action, LuceneSearchAction.KEY);

            action = new IndexEditorAction();
            actionManager.addAction(IndexEditorAction.KEY, new IndexEditorAction());
            initializer.initAction(action, IndexEditorAction.KEY);

            action = new RebuildIndexAction();
            actionManager.addAction(RebuildIndexAction.KEY, new RebuildIndexAction());
            initializer.initAction(action, RebuildIndexAction.KEY);

            initialized = true;
        }

        Module module = null;
        if( obj instanceof Module )
            module = (Module)obj;
        else if( obj instanceof DataElement )
            module = Module.optModule((DataElement)obj);
        if( module == null )
            return null;
        if( module.getType() == null )
            return null;
        LuceneQuerySystem luceneFacade = getLuceneFacade(module);
        if( luceneFacade == null )
            return null;

        if( obj instanceof Module )
        {
            if( luceneFacade.testHaveLuceneDir() )
            {
                boolean canBeEdited = !obj.getClass().getName().equals("biouml.plugins.server.ClientModule");

                Action luceneSearchAction = actionManager.getAction(LuceneSearchAction.KEY);
                luceneSearchAction.putValue(LuceneSearchAction.LUCENE_FACADE, luceneFacade);
                luceneSearchAction.putValue(LuceneSearchAction.DATA_COLLECTION, module);

                if( !canBeEdited )
                {
                    return new Action[] {luceneSearchAction};
                }

                Action indexEditorAction = actionManager.getAction(IndexEditorAction.KEY);
                indexEditorAction.putValue(IndexEditorAction.LUCENE_FACADE, luceneFacade);
                indexEditorAction.putValue(IndexEditorAction.DATA_COLLECTION, module);

                Action rebuildIndexAction = actionManager.getAction(RebuildIndexAction.KEY);
                rebuildIndexAction.putValue(RebuildIndexAction.LUCENE_FACADE, luceneFacade);
                rebuildIndexAction.putValue(RebuildIndexAction.DATA_COLLECTION, module);

                return new Action[] {luceneSearchAction, indexEditorAction, rebuildIndexAction};
            }
        }
        else if( obj instanceof DataElement )
        {
            if( luceneFacade.testHaveLuceneDir() )
            {
                boolean canBeEdited = !Module.getModule((DataElement)obj).getClass().getName().equals("biouml.plugins.server.ClientModule");

                Action luceneSearchAction = actionManager.getAction(LuceneSearchAction.KEY);
                luceneSearchAction.putValue(LuceneSearchAction.LUCENE_FACADE, luceneFacade);
                luceneSearchAction.putValue(LuceneSearchAction.DATA_COLLECTION, module);

                if( !canBeEdited )
                {
                    return new Action[] {luceneSearchAction};
                }

                Action indexEditorAction = actionManager.getAction(IndexEditorAction.KEY);
                indexEditorAction.putValue(IndexEditorAction.LUCENE_FACADE, luceneFacade);
                indexEditorAction.putValue(IndexEditorAction.DATA_COLLECTION, module);

                return new Action[] {luceneSearchAction, indexEditorAction};
            }
        }

        return null;
    }

    protected LuceneQuerySystem getLuceneFacade(Module module)
    {
        //System.out.println(module.getInfo().getQuerySystem().getClass().getCanonicalName());
        if( module.getInfo().getQuerySystem() instanceof LuceneQuerySystem )
            return (LuceneQuerySystem)module.getInfo().getQuerySystem();
        return null;
    }

}
