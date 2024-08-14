package biouml.plugins.sbgn;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SbgnDiagramViewOptionsBeanInfo extends BeanInfoEx2<SbgnDiagramViewOptions>
{
    public SbgnDiagramViewOptionsBeanInfo()
    {
        super(SbgnDiagramViewOptions.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("gridOptions");
        add("autoLayout");
        add("drawOnFly");
        add( "bioUMLPorts" );
        add("dependencyEdges");
        add("orientedReactions");
        add("autoDeleteReactions");
        add("autoMoveReactions");
        add("addSourceSink");
        addWithTags( "varNameMode", DiagramViewOptions.VARIABLE_MODES );
        addWithTags( "multiplySign", DiagramViewOptions.MULTIPLY_SIGNS );
        add( "shrinkNodeTitleSize" );
        add("nodeTitleLimit");
        add("sourceSinkSize");
        add("reactionSize");
        add( "cloneFont", FontEditor.class );
        add( "customTitleFont", FontEditor.class );
        add( "nodeTitleFont", FontEditor.class );
        add( "compartmentTitleFont", FontEditor.class );
        add( "portTitleFont", FontEditor.class );
        addWithoutChildren("compartmentPen", PenEditor.class);
        addWithoutChildren("nodePen", PenEditor.class);
        addWithoutChildren("edgePen", PenEditor.class);
        addWithoutChildren("noteLinkPen", PenEditor.class);
        addWithoutChildren("highlightPen", PenEditor.class);
        add("compartmentBrush");
        add("edgeTipBrush");
        add("cloneBrush");
        add("macromoleculeBrush");
        add("nucleicBrush");
        add("complexBrush");
        add("perturbingBrush");
        add("unspecifiedBrush");
        add("simpleChemicalBrush");
        add("phenotypeBrush");
        add("outputPortBrush");
        add("contactPortBrush");
        add("inputPortBrush");
        add("sourceSinkBrush");
        add( "pathLayouterWrapper" );
        add("styles");
    }
}
