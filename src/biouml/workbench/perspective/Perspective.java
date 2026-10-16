package biouml.workbench.perspective;

import java.util.Collections;
import java.util.List;

import com.eclipsesource.json.JsonObject;

/**
 * Class representing the perspective
 * @author lan
 */
public interface Perspective
{
    /**
     * @return user-readable title
     */
    public String getTitle();

    /**
     * @return perspective priority (they will be sorted according to the priority)
     */
    public int getPriority();

    /**
     * @return short description of what the perspective is for, or null
     */
    public default String getDescription()
    {
        return null;
    }

    /**
     * @return unmodifiable list of repository tabs
     */
    public List<RepositoryTabInfo> getRepositoryTabs();

    /**
     * @return unmodifiable list of elements (analyses, workflows, collections) the work in this perspective usually starts from
     */
    public default List<EntryPointInfo> getEntryPoints()
    {
        return Collections.emptyList();
    }

    /**
     * @param viewPartId identifier of the viewPart
     * @return true if viewPart can be displayed in this perspective
     */
    public boolean isViewPartAvailable(String viewPartId);

    /**
     * @param actionId identifier of the action
     * @return true if action is available in this perspective
     */
    public boolean isActionAvailable(String actionId);

    /**
     * @param importerId identifier of the importer
     * @return true if importer is available in this perspective
     */
    public boolean isImporterAvailable(String importerId);

    /**
     * @param exporterId identifier of the exporter
     * @return true if exporter is available in this perspective
     */
    public boolean isExporterAvailable(String exporterId);

    /**
     * @return template name default for this perspective
     */
    public String getDefaultTemplate();

    /**
     * @return name of 'start page' file
     */
    public String getIntroPage();

    /**
     * @return JsonObject-serialized form
     */
    public JsonObject toJSON();
}
