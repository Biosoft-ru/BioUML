package biouml.standard.type;

import ru.biosoft.util.bean.BeanInfoEx2;

//TODO: clean and use "source" field
public class ImageDescriptorBeanInfo extends BeanInfoEx2<ImageDescriptor>
{
    public ImageDescriptorBeanInfo()
    {
        super(ImageDescriptor.class);
    }

    @Override
    public void initProperties() throws Exception
    {
//        PropertyDescriptorEx pde;

        //        pde = new PropertyDescriptorEx("source", beanClass);
        //        pde.setChildReadOnly(true);
        //        add(pde, NodeImagePropertyEditor.class,
        //            getResourceString("PN_SOURCE"),
        //            getResourceString("PD_SOURCE"));

        add("path");
        addReadOnly("originalSize");
        add("scale");
//        add(DataElementPathEditor.registerInput("path", beanClass, ImageDataElement.class, beanClass.getMethod("canBeNull")));
        
//        add("path");
//        pde = new PropertyDescriptorEx("path", beanClass);
////        pde.setChildReadOnly(true);
//        add(pde, NodeImagePropertyEditor.class, getResourceString("PN_SOURCE"), getResourceString("PD_SOURCE"));

        //        pde = new PropertyDescriptorEx("size", beanClass);
        //        add(pde,
        //            getResourceString("PN_IMAGE_SIZE"),
        //            getResourceString("PD_IMAGE_SIZE"));
        //
        //        pde = new PropertyDescriptorEx("originalSize", beanClass, "getOriginalSize", null );
        //        add(pde,
        //            getResourceString("PN_ORIGINAL_IMAGE_SIZE"),
        //            getResourceString("PD_ORIGINAL_IMAGE_SIZE"));
    }
}
