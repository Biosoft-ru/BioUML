package biouml.plugins.sbgn;

import java.beans.IntrospectionException;

import biouml.plugins.sbgn.SBGNPropertyConstants.EdgeTypeEditor;
import one.util.streamex.StreamEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class LogicalOperatorPropertiesBeanInfo extends BeanInfoEx2<LogicalOperatorProperties>
{
    public LogicalOperatorPropertiesBeanInfo()
    {
        super(LogicalOperatorProperties.class);
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        add("name");
        add("properties");
        add( "nodeNames", InputNodesEditor.class );
        addWithTags( "reactionName",  b -> StreamEx.of( b.getAvailableReactions()) );
        add( "modifierType", EdgeTypeEditor.class);
    }  
    
    public static class InputNodesEditor extends GenericMultiSelectEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            return ((LogicalOperatorProperties)getBean()).getAvailableNames();
        }
    }
}