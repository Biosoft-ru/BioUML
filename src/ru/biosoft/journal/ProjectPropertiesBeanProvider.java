package ru.biosoft.journal;

import ru.biosoft.access.BeanProvider;

/**
 * @author lan
 */
public class ProjectPropertiesBeanProvider implements BeanProvider
{
    @Override
    public ProjectProperties getBean(String path)
    {
        return ProjectProperties
                .getProperties(path == null ? JournalRegistry.getCurrentJournal() : JournalRegistry.getCurrentJournal(path));
    }
}
