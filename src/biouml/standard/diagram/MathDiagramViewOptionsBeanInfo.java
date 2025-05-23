package biouml.standard.diagram;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.editor.FontEditor;

public class MathDiagramViewOptionsBeanInfo extends PathwaySimulationDiagramViewOptionsBeanInfo
{
    public MathDiagramViewOptionsBeanInfo()
    {
        super(MathDiagramViewOptions.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("gridOptions");
        add("autoLayout");
        add("drawOnFly");
        add("dependencyEdges");
        add("diagramTitleVisible");
        add("nodeTitleMargin");
        addWithTags( "varNameMode", DiagramViewOptions.VARIABLE_MODES );
        addWithTags( "multiplySign", DiagramViewOptions.MULTIPLY_SIGNS );
        addWithTags("equationStyle", EquationStyle.getAvailableTags());
        add( "maxTitleSize" );
        add( "defaultFont", FontEditor.class );
        add( "nodeTitleFont", FontEditor.class );
        add( "mathTitleFont", FontEditor.class );
        add( "compartmentTitleFont", FontEditor.class );
        add( "diagramTitleFont", FontEditor.class );
        addWithoutChildren("defaultPen", PenEditor.class);
        addWithoutChildren("nodePen", PenEditor.class);
        addWithoutChildren("mathPen", PenEditor.class);
        add( "pathLayouterWrapper" );
        add("styles");
    }
}
