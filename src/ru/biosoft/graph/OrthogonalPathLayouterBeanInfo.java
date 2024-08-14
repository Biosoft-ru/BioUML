package ru.biosoft.graph;

import ru.biosoft.graph.OrthogonalPathLayouter;
import ru.biosoft.graph.OrthogonalPathLayouter.Orientation;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

public class OrthogonalPathLayouterBeanInfo extends BeanInfoEx
{
    public OrthogonalPathLayouterBeanInfo()
    {
        super(OrthogonalPathLayouter.class, MessageBundle.class.getName());

        beanDescriptor.setDisplayName(getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription(getResourceString("CD_LAYOUTER"));
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");

        add(new PropertyDescriptorEx("gridX", beanClass), getResourceString("PN_ORTHOGONAL_GRID_X"),
                getResourceString("PD_ORTHOGONAL_GRID_X"));

        add(new PropertyDescriptorEx("gridY", beanClass), getResourceString("PN_ORTHOGONAL_GRID_Y"),
                getResourceString("PD_ORTHOGONAL_GRID_Y"));

        add(new PropertyDescriptorEx("iterationMax", beanClass), getResourceString("PN_ORTHOGONAL_ITER_MAX"),
                getResourceString("PD_ORTHOGONAL_ITER_MAX"));

        add(new PropertyDescriptorEx("iterationK", beanClass), getResourceString("PN_ORTHOGONAL_ITER_K"),
                getResourceString("PD_ORTHOGONAL_ITER_K"));

        add(new PropertyDescriptorEx("smoothEdges", beanClass), getResourceString("PN_ORTHOGONAL_SMOOTH"),
                getResourceString("PD_ORTHOGONAL_SMOOTH"));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("orientation", beanClass);
        pde.setPropertyEditorClass(OrientationEditor.class);
        add(pde, getResourceString("PN_ORTHOGONAL_ORIENTATION"),
                getResourceString("PD_ORTHOGONAL_ORIENTATION"));
    }

    public static class OrientationEditor extends StringTagEditorSupport
    {
        @Override
        public String[] getTags()
        {
            return Orientation.getAvailableTags();
        }
    }
}
