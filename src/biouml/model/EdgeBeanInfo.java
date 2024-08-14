package biouml.model;

public class EdgeBeanInfo extends DiagramElementBeanInfo
{
    public EdgeBeanInfo()
    {
        super(Edge.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        addExpert("path");
        addExpert("inPort");
        addExpert("outPort");
        add("fixed");
        add( "fixedInOut" );
        addHidden("input");
        addHidden("output");
    }

}
