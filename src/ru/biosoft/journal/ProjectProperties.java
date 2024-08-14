package ru.biosoft.journal;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class ProjectProperties
{
    private final DataCollection<?> project;
       
    private ProjectProperties(DataCollection<?> project)
    {
        this.project = project;
    }

    public static ProjectProperties getProperties(Journal journal)
    {
        DataElementPath projectPath = JournalRegistry.getProjectPath(journal);
        if(projectPath == null) return null;
        DataCollection<?> dataCollection = projectPath.optDataCollection();
        if(dataCollection == null) return null;
        return new ProjectProperties(dataCollection);
    }
    
    public static ProjectProperties getProperties()
    {
        return getProperties(JournalRegistry.getCurrentJournal());
    }
    
    @PropertyName("Default database versions")
    @PropertyDescription("Select database versions which will be used by some analyses when this project is active")
    public DynamicPropertySet getDefaultDatabaseVersions()
    {
        Map<String, SortedSet<String>> versionsMap = ProjectUtils.getAvailableDatabaseVersions();
        DynamicPropertySet dps = new DynamicPropertySetSupport();
        boolean readOnly = !SecurityManager.getPermissions(project.getCompletePath()).isWriteAllowed();
        for(Entry<String, SortedSet<String>> entry: versionsMap.entrySet())
        {
            String version = project.getInfo().getProperty(ProjectUtils.DATABASE_VERSION_PROPERTY_PREFIX+entry.getKey());
            if(version == null || !entry.getValue().contains(version)) version = ProjectUtils.NEWEST_VERSION;
            DynamicProperty property = new DynamicProperty(entry.getKey(), String.class, version);
            if(readOnly)
            {
                property.setReadOnly(true);
            }
            else
            {
                property.getDescriptor().setPropertyEditorClass(PropertyStringTagEditor.class);
                String[] tags = StreamEx.of(entry.getValue()).prepend(ProjectUtils.NEWEST_VERSION).toArray(String[]::new);
                property.getDescriptor().setValue(StringTagEditor.TAGS_KEY, tags);
            }

            dps.add(property);
        }
        dps.addPropertyChangeListener(evt -> {
            if( evt.getOldValue() == null || evt.getNewValue() == null || evt.getOldValue().equals(evt.getNewValue()) )
                return;
            if(evt.getNewValue().equals(ProjectUtils.NEWEST_VERSION))
                project.getInfo().getProperties().remove(ProjectUtils.DATABASE_VERSION_PROPERTY_PREFIX+evt.getPropertyName());
            else
                project.getInfo().getProperties().setProperty(ProjectUtils.DATABASE_VERSION_PROPERTY_PREFIX+evt.getPropertyName(), evt.getNewValue().toString());
            try
            {
                @SuppressWarnings ( "unchecked" )
                DataCollection<DataCollection<?>> origin = (DataCollection<DataCollection<?>>)project.getOrigin();
                origin.put(project);
            }
            catch( Exception e )
            {
            }
        });
        return dps;
    }

    public static class PropertyStringTagEditor extends StringTagEditor
    {
        public PropertyStringTagEditor()
        {
            
        }
    }
}
