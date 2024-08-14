package biouml.plugins.research.workflow;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.graphics.editor.FontEditor;

public class WorkflowDiagramViewOptionsBeanInfo extends BeanInfoEx
{
    public WorkflowDiagramViewOptionsBeanInfo()
    {
        super( WorkflowDiagramViewOptions.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("autoLayout");
        add("analysisBrush");
        add("analysisPen");
        add("expressionBrush");
        add("expressionPen");
        add("parameterBrush");
        add("parameterPen");
        add("scriptBrush");
        add("scriptPen");
        add("noteBrush");
        add("connectionPen");
        add("connectionBrush");
        add("defaultPen");
        add( "defaultFont", FontEditor.class );
        add( "nodeTitleFont", FontEditor.class );
        add( "progressFont", FontEditor.class );
        add( "expressionFont", FontEditor.class );
    }
}
