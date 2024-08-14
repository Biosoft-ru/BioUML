package biouml.model;

import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramViewOptionsBeanInfo extends BeanInfoEx2<DiagramViewOptions>
{
    public DiagramViewOptionsBeanInfo()
    {
        this(DiagramViewOptions.class);
    }

    public DiagramViewOptionsBeanInfo(Class<? extends DiagramViewOptions> beanClass)
    {
        super(beanClass);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("gridOptions");
        add("autoLayout");
        add("drawOnFly");
        add("dependencyEdges");
        add("diagramTitleVisible");
        addWithTags( "varNameMode", DiagramViewOptions.VARIABLE_MODES );
        addWithTags( "multiplySign", DiagramViewOptions.MULTIPLY_SIGNS );
        add("nodeTitleMargin");
        add( "maxTitleSize" );
        add( "defaultFont", FontEditor.class );
        add( "nodeTitleFont", FontEditor.class );
        add( "compartmentTitleFont", FontEditor.class );
        add( "diagramTitleFont", FontEditor.class );
        addWithoutChildren("defaultPen", PenEditor.class);
        addWithoutChildren("nodePen", PenEditor.class);
        addWithoutChildren( "highlightPen", PenEditor.class );
        add( "pathLayouterWrapper" );
        add("styles");
    }
}
