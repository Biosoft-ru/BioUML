package ru.biosoft.plugins.graph;

import ru.biosoft.access.BeanProvider;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.Layouter;

/**
 * @author lan
 */
public class LayoutBeanProvider implements BeanProvider
{
    @Override
    public Layouter getBean(String name)
    {
        LayouterDescriptor layoutDescriptor = GraphPlugin.getLayouter(name);
        if(layoutDescriptor != null)
        {
            try
            {
                return layoutDescriptor.createLayouter();
            }
            catch( Exception e )
            {
                ExceptionRegistry.log(e);
            }
        }
        return null;
    }
}
