package biouml.plugins.wdl.web;

import biouml.model.Diagram;
import biouml.plugins.wdl.WDLEditor.WorkflowSettings;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;

public class WDLSettingsBeanProvider implements BeanProvider
{

    @Override
    public Object getBean(String path)
    {
        Diagram diagram = WebDiagramsProvider.getDiagram( path, false );
        WorkflowSettings settings = new WorkflowSettings();
        settings.initParameters( diagram );
        return settings;

    }
}
