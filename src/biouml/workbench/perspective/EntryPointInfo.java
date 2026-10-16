package biouml.workbench.perspective;

import org.eclipse.core.runtime.IConfigurationElement;

import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.core.DataElementPath;

/**
 * Repository element (analysis, workflow, collection) from which work in a perspective usually starts.
 * Gives non-UI clients (e.g. MCP agents) what the perspective's intro page offers to the user.
 */
public class EntryPointInfo
{
    private static final String PATH_ATTR = "path";
    private static final String TITLE_ATTR = "title";
    private static final String DESCRIPTION_ATTR = "description";

    private final DataElementPath path;
    private final String title;
    private final String description;

    protected EntryPointInfo(IConfigurationElement entry)
    {
        this.path = DataElementPath.create(entry.getAttribute(PATH_ATTR));
        String titleStr = entry.getAttribute(TITLE_ATTR);
        this.title = titleStr != null ? titleStr : path.getName();
        this.description = entry.getAttribute(DESCRIPTION_ATTR);
    }

    /**
     * @return path of the repository element
     */
    public DataElementPath getPath()
    {
        return path;
    }

    /**
     * @return human-readable title (element name if not specified)
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @return what the entry point is for, or null
     */
    public String getDescription()
    {
        return description;
    }

    public JsonObject toJSON()
    {
        JsonObject result = new JsonObject().add(PATH_ATTR, path.toString()).add(TITLE_ATTR, title);
        if( description != null )
            result.add(DESCRIPTION_ATTR, description);
        return result;
    }
}
