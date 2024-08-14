package biouml.model;

import com.developmentontheedge.beans.editors.TagEditorSupport;

public class CompartmentBeanInfo extends DiagramElementBeanInfo
{
    public CompartmentBeanInfo()
    {
        super(Compartment.class);
    }
    
    public CompartmentBeanInfo(Class<? extends Compartment> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property("showTitle").add(2);
        property("location").expert().add(3);
        property("shapeType").editor(ShapeTypeEditor.class).hidden("isShapeTypeHidden").add(4);
        property("shapeSize2").hidden("isNotResizable").add(5);
        add("useCustomImage");
        property("image").hidden("useDefaultView").add();//.hidden("isImageHidden").add();
        add("fixed");
        add("visible");
    }
    
    public static class ShapeTypeEditor extends TagEditorSupport
    {
        public ShapeTypeEditor()
        {
            super(new String[]{"rectangle", "round rectangle", "ellipse"}, 0);
        }
    }
}
