package biouml.workbench.perspective;

import org.eclipse.core.runtime.IConfigurationElement;

import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.core.DataElementPath;

/**
 * Stores information about single tab in repository tabs
 * @author lan
 */
public class RepositoryTabInfo
{
    private static final String DATABASES_ATTR = "databases";
    private static final String PATH_ATTR = "path";
    private static final String TITLE_ATTR = "title";
    private static final String HELP_ID_ATTR = "helpId";
    private static final String VIRTUAL_ATTR = "virtual";
    
    private final String title;
    private final String helpId;
    private final DataElementPath rootPath;
    private final boolean databasesTab;
    private final boolean virtualTab;
    
    protected RepositoryTabInfo(IConfigurationElement tab)
    {
        this.title = tab.getAttribute(TITLE_ATTR);
        this.rootPath = DataElementPath.create(tab.getAttribute(PATH_ATTR));
        String databasesTabStr = tab.getAttribute(DATABASES_ATTR);
        this.databasesTab = databasesTabStr != null && databasesTabStr.equalsIgnoreCase("true");
        String virtualTabStr = tab.getAttribute( VIRTUAL_ATTR );
        this.virtualTab = virtualTabStr != null && virtualTabStr.equalsIgnoreCase( "true" );
        this.helpId = tab.getAttribute(HELP_ID_ATTR);
    }
    
    /**
     * @return the human-readable title of the tab
     */
    public String getTitle()
    {
        return title;
    }
    
    /**
     * @return the root path
     */
    public DataElementPath getRootPath()
    {
        return rootPath;
    }
    
    /**
     * @return identifier to bind help
     */
    public String getHelpId()
    {
        return helpId;
    }

    /**
     * @return true if this tab is databases tab
     */
    public boolean isDatabasesTab()
    {
        return databasesTab;
    }
    
    public JsonObject toJSON()
    {
        JsonObject result = new JsonObject().add( TITLE_ATTR, title ).add( PATH_ATTR, rootPath.toString() )
                .add( DATABASES_ATTR, this.databasesTab ).add( VIRTUAL_ATTR, this.virtualTab );
        if(this.helpId != null)
            result.add(HELP_ID_ATTR, this.helpId);
        return result;
    }
}
