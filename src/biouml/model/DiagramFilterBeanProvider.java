package biouml.model;

import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElementPath;

/**
 * @author lan
 *
 */
public class DiagramFilterBeanProvider implements BeanProvider
{
    @Override
    public Object getBean(String name)
    {
        DataElementPath path = DataElementPath.create(name);
        String filterName = path.getName();
        Diagram diagram = path.getParentPath().optDataElement( Diagram.class );
        if(diagram == null) return null;
        for(DiagramFilter filter: diagram.getFilterList())
        {
            if(filter.getName() != null && filter.getName().equals(filterName))
                return filter.getProperties();
        }
        return null;
    }
}
