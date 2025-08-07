package biouml.plugins.physicell;

import ru.biosoft.graphics.PenEditor;
import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PhysicellDiagramViewOptionsBeanInfo extends BeanInfoEx2<PhysicellDiagramViewOptions>
{
    public PhysicellDiagramViewOptionsBeanInfo()
    {
        super( PhysicellDiagramViewOptions.class );
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
        add( "cellDefinitionBrush" );
        add( "substanceBrush" );
        add( "nodeTitleFont", FontEditor.class );
        add( "diagramTitleFont", FontEditor.class );
        addWithoutChildren( "secretionPen", PenEditor.class );
        addWithoutChildren( "chemotaxisPen", PenEditor.class );
        addWithoutChildren( "interactionPen", PenEditor.class );
        addWithoutChildren( "transformationPen", PenEditor.class );
        addWithoutChildren( "nodePen", PenEditor.class );
        addWithoutChildren( "noteLinkPen", PenEditor.class );
        add( "styles" );
    }
}
