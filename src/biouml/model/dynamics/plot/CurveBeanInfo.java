package biouml.model.dynamics.plot;

import ru.biosoft.graphics.PenEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CurveBeanInfo extends BeanInfoEx2<Curve>
{
    public CurveBeanInfo()
    {
        super(Curve.class);
        setHideChildren(true);
        setCompositeEditor("path;name;title;pen", new java.awt.GridLayout(1, 5));
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "path" ).structureChanging().tags( bean -> bean.modules() ).hidden( "isPathHidden" ).add();
        property( "name" ).tags( bean -> bean.variables() ).structureChanging().add();
        add( "title" );
        //property("type").tags(bean -> bean.types()).add();
        add("pen", PenEditor.class);
    }
}