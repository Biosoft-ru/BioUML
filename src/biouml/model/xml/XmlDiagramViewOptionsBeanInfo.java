package biouml.model.xml;

import biouml.model.DiagramViewOptionsBeanInfo;

public class XmlDiagramViewOptionsBeanInfo extends DiagramViewOptionsBeanInfo
{
    public XmlDiagramViewOptionsBeanInfo()
    {
        super(XmlDiagramViewOptions.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "gridOptions" );
        add( "autoLayout" );
        add( "drawOnFly" );
        add( "dependencyEdges" );
        add( "options" );
        add( "styles" );
    }
}
