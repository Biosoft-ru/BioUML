package biouml.model.dynamics.plot;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PlotVariableBeanInfo extends BeanInfoEx2<PlotVariable>
{
    public PlotVariableBeanInfo()
    {
        super(PlotVariable.class);
        //        this.setHideChildren( true );
        //        setCompositeEditor( "path;name;title", new java.awt.GridLayout( 1, 3 ) );
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "path" ).tags( bean -> bean.modules() ).hidden( "isPathHidden" ).add();
        property( "name" ).tags( bean -> bean.variables() ).structureChanging().add();
        property( "title" ).add();
    }
}