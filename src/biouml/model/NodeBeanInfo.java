package biouml.model;

public class NodeBeanInfo extends DiagramElementBeanInfo
{
    public NodeBeanInfo()
    {
        super(Node.class);
    }

    public NodeBeanInfo(Class<? extends Node> beanClass)
    {
        super(beanClass);
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property("showTitle").add(2);
        property("location").expert().add(3);
        property("shapeSize2").hidden("isNotResizable").add(4);
        property("image").hidden("isImageHidden").add();
        add("fixed");
        add("visible");
    }
}
