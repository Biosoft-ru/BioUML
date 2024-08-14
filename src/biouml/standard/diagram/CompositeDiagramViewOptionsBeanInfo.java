package biouml.standard.diagram;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CompositeDiagramViewOptionsBeanInfo extends BeanInfoEx2<CompositeDiagramViewOptions>
{
    public CompositeDiagramViewOptionsBeanInfo()
    {
        super(CompositeDiagramViewOptions.class);
    }

    public CompositeDiagramViewOptionsBeanInfo(Class beanClass)
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
       add("arrows");
       add("nodeTitleMargin");
       addWithTags( "varNameMode", DiagramViewOptions.VARIABLE_MODES );
       addWithTags( "multiplySign", DiagramViewOptions.MULTIPLY_SIGNS );
       add( "maxTitleSize" );
       add("defaultFont", FontEditor.class);
       add("connectionTitleFont", FontEditor.class);
       add("nodeTitleFont",FontEditor.class);
       add("compartmentTitleFont",FontEditor.class);
       add("diagramTitleFont",FontEditor.class);
       addWithoutChildren("defaultPen", PenEditor.class);
       addWithoutChildren("connectionPen", PenEditor.class);
       addWithoutChildren("nodePen",PenEditor.class);
       addWithoutChildren("modulePen",PenEditor.class);
       add("moduleBrush");
       add("contactConnectionPortBrush");
       add( "pathLayouterWrapper" );
       add("styles");
    }
}
