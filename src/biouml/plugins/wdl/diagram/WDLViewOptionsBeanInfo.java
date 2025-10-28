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
        add("analysisBrush");
        add("taskBrush");
        add("analysisPen");
        add("expressionBrush");
        add("conditionBrush");
        add("conditionalBrush");
        add("conditionPen");
        add("expressionPen");
        add("parameterBrush");
        add("parameterPen");
        add("outputBrush");
        add("outputPen");
        add("defaultPen");
//        add( "defaultFont", FontEditor.class );
//        add( "nodeTitleFont", FontEditor.class );
//        add( "progressFont", FontEditor.class );
//        add( "expressionFont", FontEditor.class );
    }
}
