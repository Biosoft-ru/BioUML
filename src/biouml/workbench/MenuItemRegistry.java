package biouml.workbench;

import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;

import org.eclipse.core.runtime.IConfigurationElement;
import ru.biosoft.util.ExtensionRegistrySupport;

public class MenuItemRegistry extends ExtensionRegistrySupport<MenuItemRegistry.ItemInfo>
{
    /** Utility class that stores information about <code>ModuleImporters</code>. */
    public static class ItemInfo
    {
        public ItemInfo(Class<? extends Action> action, String title, String parent)
        {
            this.action = action;
            this.title = title;
            this.parent = parent;
        }

        protected Class<? extends Action> action;
        public Action getAction() throws Exception
        {
            return action.newInstance();
        }

        protected String title;
        public String getTitle()
        {
            return title;
        }

        protected String parent;
        public String getParent()
        {
            return parent;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final MenuItemRegistry instance = new MenuItemRegistry();

    public static final String TITLE = "title";
    public static final String ACTION_CLASS = "action";
    public static final String PARENT = "parent";
    
    private MenuItemRegistry()
    {
        super("biouml.workbench.menuItem", TITLE);
    }
    
    public static List<ItemInfo> getMenuItems()
    {
        return instance.stream().toList();
    }

    @Override
    protected ItemInfo loadElement(IConfigurationElement element, String title) throws Exception
    {
        Class<? extends AbstractAction> actionClass = getClassAttribute(element, ACTION_CLASS, AbstractAction.class);
        String parent = element.getAttribute(PARENT);
        if( parent == null )
        {
            parent = BioUMLApplication.getMessageBundle().getResourceString("MENU_FILE");
        }
        return new ItemInfo(actionClass, title, parent);
    }
}
