package ru.biosoft.analysis.admin;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class ProjectsSelector extends GenericMultiSelectEditor
{
    @Override
    protected String[] getAvailableValues()
    {
        return CollectionFactoryUtils.getUserProjectsPath().getDataCollection().names().toArray(String[]::new);
    }
}