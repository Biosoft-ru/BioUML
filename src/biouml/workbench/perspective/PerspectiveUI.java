package biouml.workbench.perspective;

import java.awt.BorderLayout;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.Iterator;

import java.util.logging.Logger;

import ru.biosoft.access.repository.RepositoryPane;
import ru.biosoft.access.repository.RepositoryTabs;
import ru.biosoft.gui.EditorsManager;
import ru.biosoft.gui.EditorsTabbedPane;
import ru.biosoft.gui.GUI;
import ru.biosoft.gui.Document;
import ru.biosoft.gui.DocumentsPane;
import biouml.workbench.DatabaseRenderer;
import biouml.workbench.SearchPane;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationFrame;

/**
 * @author lan
 *
 */
public class PerspectiveUI
{
    private static Logger log = Logger.getLogger(PerspectiveUI.class.getName());
    private static Perspective currentPerspective = null;
    
    public static void initPerspective()
    {
        initPerspectivePreference();
        setPerspective(getCurrentPerspective());
    }
    
    public synchronized static void setPerspective(Perspective perspective)
    {
        if(currentPerspective == perspective) return;
        initRepository(perspective);
        initViewParts(perspective);
        currentPerspective = perspective;
        initToolbar();
    }

    /**
     * Enables/disables view parts
     * @param perspective
     */
    private static void initViewParts(Perspective perspective)
    {
        EditorsManager manager = (EditorsManager)((EditorsTabbedPane)Application.getApplicationFrame().getPanelManager().getPanel(
                ApplicationFrame.EDITOR_PANE_NAME)).getEditorsManager();
        Iterator<DynamicProperty> iterator = manager.getEditors().propertyIterator();
        while(iterator.hasNext())
        {
            DynamicProperty property = iterator.next();
            String viewPartId = manager.getViewPartId(property.getName());
            if(viewPartId == null)
            {
                log.log(Level.SEVERE, "No id for viewPart "+property.getName());
                property.setValue(true);
                continue;
            }
            property.setValue(perspective.isViewPartAvailable(viewPartId));
        }
        ComponentFactory.getModel(manager).propertyChange(new PropertyChangeEvent(manager, "*", null, null));
    }

    /**
     * Initializes repository tabs
     * @param perspective
     */
    private static void initRepository(Perspective perspective)
    {
        RepositoryTabs tabs = GUI.getManager().getRepositoryTabs();
        tabs.removeAll();
        for(RepositoryTabInfo tabInfo: perspective.getRepositoryTabs())
        {
            RepositoryPane pane = tabs.addRepositoryPane(tabInfo.getTitle(), tabInfo.getRootPath().optDataCollection());
            // TODO: more sane way to check whether tab was added before
            if(tabInfo.isDatabasesTab() && !(pane.getTree().getCellRenderer() instanceof DatabaseRenderer))
            {
                SearchPane searchPane = new SearchPane(pane);
                pane.add(searchPane, BorderLayout.SOUTH);
                pane.setCellRenderer(new DatabaseRenderer(pane));
            }
        }
    }

    /**
     * Initializes dynamic actions
     */
    private static void initToolbar()
    {
        Document document = Document.getCurrentDocument();
        if( document != null )
        {
            DocumentsPane.updateActions( document, document );
        }
    }


    public static final String PERSPECTIVE_PREFERENCE = "Perspective";

    public synchronized static void initPerspectivePreference()
    {
        String perspective = Application.getPreferences().getStringValue(PERSPECTIVE_PREFERENCE, PerspectiveRegistry.getDefaultPerspective().toString());
        if(PerspectiveRegistry.getPerspective(perspective) == null)
            perspective = PerspectiveRegistry.getDefaultPerspective().toString();
        try
        {
            PropertyDescriptor pd = new PropertyDescriptor(PERSPECTIVE_PREFERENCE, String.class, null, null);
            pd.setPropertyEditorClass(PerspectiveSelector.class);
            Application.getPreferences().add(new DynamicProperty(pd, String.class, perspective)
            {
                @Override
                public void setValue(Object value)
                {
                    super.setValue(value);
                    setPerspective(PerspectiveRegistry.getPerspective(value.toString()));
                }
            });
        }
        catch( IntrospectionException e )
        {
        }
    }

    public static Perspective getCurrentPerspective()
    {
        Object perspectiveObject = Application.getPreferences().getValue(PERSPECTIVE_PREFERENCE);
        return perspectiveObject instanceof String ? PerspectiveRegistry.getPerspective((String)perspectiveObject) : PerspectiveRegistry.getDefaultPerspective();
    }
}
