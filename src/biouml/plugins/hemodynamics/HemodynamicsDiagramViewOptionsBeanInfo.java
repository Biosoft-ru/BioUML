package biouml.plugins.hemodynamics;

import ru.biosoft.graphics.BrushEditor;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class HemodynamicsDiagramViewOptionsBeanInfo extends BeanInfoEx2<HemodynamicsDiagramViewOptions>
{
    public HemodynamicsDiagramViewOptionsBeanInfo()
    {
        super(HemodynamicsDiagramViewOptions.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("gridOptions");
        add("autoLayout");
        add("drawOnFly");
        add("bifurcationRadius");
        addWithoutChildren("defaultPen", PenEditor.class);
        addWithoutChildren("vesselPen", PenEditor.class);
        addWithoutChildren("bifurcationColor", BrushEditor.class);
        add( "nodeTitleFont", FontEditor.class );
        add( "pathLayouterWrapper" );
    }
}
