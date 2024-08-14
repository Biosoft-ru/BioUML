package biouml.plugins.sbgn;

import biouml.model.DiagramViewOptions;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SbgnCompositeDiagramViewOptionsBeanInfo extends BeanInfoEx2<SbgnCompositeDiagramViewOptions>
{
    public SbgnCompositeDiagramViewOptionsBeanInfo()
    {
        super(SbgnCompositeDiagramViewOptions.class);
    }

    @Override
    public void initProperties() throws Exception
    {
        add("gridOptions");
        add("autoLayout");
        add("drawOnFly");
        add("orientedReactions");
        add( "bioUMLPorts" );
        add("autoDeleteReactions");
        add("autoMoveReactions");
        add("addSourceSink");
        addWithTags( "varNameMode", DiagramViewOptions.VARIABLE_MODES );
        addWithTags( "multiplySign", DiagramViewOptions.MULTIPLY_SIGNS );
        add("nodeTitleLimit");
        add("sourceSinkSize");
        add( "cloneFont", FontEditor.class );
        add( "customTitleFont", FontEditor.class );
        add( "nodeTitleFont", FontEditor.class );
        add( "compartmentTitleFont", FontEditor.class );
        add( "portTitleFont", FontEditor.class );
        addWithoutChildren("compartmentPen", PenEditor.class);
        addWithoutChildren("nodePen", PenEditor.class);
        addWithoutChildren("edgePen", PenEditor.class);
        addWithoutChildren("noteLinkPen", PenEditor.class);
        addWithoutChildren("connectionPen", PenEditor.class);
        addWithoutChildren("modulePen", PenEditor.class);
        addWithoutChildren("modelDefinitionPen", PenEditor.class);
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
        add("modelDefBrush");
        add("moduleBrush");
        add("styles");
    }
}
