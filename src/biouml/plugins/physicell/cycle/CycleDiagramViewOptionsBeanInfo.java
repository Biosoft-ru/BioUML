package biouml.plugins.physicell.cycle;

import ru.biosoft.util.bean.BeanInfoEx2;

public class CycleDiagramViewOptionsBeanInfo extends BeanInfoEx2<CycleDiagramViewOptions>
{
    public CycleDiagramViewOptionsBeanInfo()
    {
        super( CycleDiagramViewOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "gridOptions" );
        add( "autoLayout" );
        add( "drawOnFly" );
        add( "diagramTitleVisible" );
        add( "nodeTitleMargin" );
        add( "maxTitleSize" );
        add( "phaseBrush" );
        add( "transitionPen" );
        add( "styles" );
    }
}
