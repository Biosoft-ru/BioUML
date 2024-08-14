package biouml.plugins.research.web;

import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElementPath;

public class WorkflowElementPropertiesBeanProvider implements BeanProvider
{
    @Override
    public Object getBean(String path)
    {
        return WebResearchProvider.getWorkflowElementProperties(DataElementPath.create(path));
    }
    
    @Override
    public void saveBean(String path, Object bean)
    {
        WebResearchProvider.saveWorkflowElementProperties(bean, path);
    }

}
