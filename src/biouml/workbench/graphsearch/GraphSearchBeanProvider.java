package biouml.workbench.graphsearch;

import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.access.core.CollectionFactory;

public class GraphSearchBeanProvider implements CacheableBeanProvider
{
    @Override
    public GraphSearchOptions getBean(String path)
    {
        return new GraphSearchOptions(CollectionFactory.getDataCollection("databases"));
    }
}
