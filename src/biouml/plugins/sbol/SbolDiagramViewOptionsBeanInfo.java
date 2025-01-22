package biouml.plugins.sbol;

import biouml.model.DiagramViewOptions;
import biouml.model.DiagramViewOptionsBeanInfo;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.editor.FontEditor;

public class SbolDiagramViewOptionsBeanInfo extends DiagramViewOptionsBeanInfo
{

    public SbolDiagramViewOptionsBeanInfo()
    {
        super(SbolDiagramViewOptions.class);
    }

    public SbolDiagramViewOptionsBeanInfo(Class<? extends SbolDiagramViewOptions> beanClass)
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
        addWithTags("varNameMode", DiagramViewOptions.VARIABLE_MODES);
        addWithTags("multiplySign", DiagramViewOptions.MULTIPLY_SIGNS);
        add("nodeTitleMargin");
        add("maxTitleSize");
        add("defaultFont", FontEditor.class);
        add("nodeTitleFont", FontEditor.class);
        add("compartmentTitleFont", FontEditor.class);
        add("diagramTitleFont", FontEditor.class);
        addWithoutChildren("defaultPen", PenEditor.class);
        addWithoutChildren("backbonePen", PenEditor.class);
        addWithoutChildren("highlightPen", PenEditor.class);
        add("pathLayouterWrapper");
        add("styles");
    }

}
