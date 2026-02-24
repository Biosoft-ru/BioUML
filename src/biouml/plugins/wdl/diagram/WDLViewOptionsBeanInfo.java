package biouml.plugins.wdl.diagram;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.graphics.editor.FontEditor;

public class WDLViewOptionsBeanInfo extends BeanInfoEx
{
    public WDLViewOptionsBeanInfo()
    {
        super( WDLViewOptions.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add("autoLayout");
        add("showTasks");
        add("callBrush");
        add("callPen");
        add("taskBrush");
        add("workflowBrush");
        add("workflowPen");
        add("expressionBrush");
        add("expressionPen");
        add("conditionBrush");
        add("conditionalBrush");
        add("conditionPen");
        add("parameterBrush");
        add("parameterPen");
        add("outputBrush");
        add("outputPen");
        add("defaultPen");
        add( "expressionFont", FontEditor.class );
        add("labeledTags");
        add("clampInputs");
    }
}
