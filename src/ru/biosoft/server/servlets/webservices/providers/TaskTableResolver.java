package ru.biosoft.server.servlets.webservices.providers;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.providers.WebTablesProvider.CommonTableResolver;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.tasks.TaskManager;

public class TaskTableResolver extends TableResolver implements CommonTableResolver
{
    public TaskTableResolver(BiosoftWebRequest arguments)
    {

    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        return TaskManager.getInstance().getTasksInfo();
    }

}
