package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.editors.TagEditorSupport;

public class JoinTableParametersBeanInfo extends BeanInfoEx2<JoinTableParameters>
{
    public JoinTableParametersBeanInfo()
    {
        super(JoinTableParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    
    public JoinTableParametersBeanInfo(Class<? extends JoinTableParameters> beanClass, String resourceBundleName)
    {
        super(beanClass, resourceBundleName);
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        property( "leftGroup" ).title( "PN_LEFT_TABLE" ).description( "PN_LEFT_TABLE" ).add();
        property( "rightGroup" ).title( "PN_RIGHT_TABLE" ).description( "PN_RIGHT_TABLE" ).add();
        property( "joinType" ).editor( JoinTypeEditor.class ).title( "PN_JOIN_TYPE" ).description( "PD_JOIN_TYPE" ).add();

        property( "mergeColumns" ).title( "PN_MERGE_COLUMNS" ).description( "PN_MERGE_COLUMNS" ).add();
        property( "ignoreNaNInAggregator" ).expert().titleRaw( "Ignore empty values" )
                .descriptionRaw( "Ignore empty values during aggregator work" ).add();
        property( "aggregator" ).expert().simple().editor( NumericAggregatorEditor.class ).title( "PN_JOIN_AGGREGATOR" )
                .description( "PD_JOIN_AGGREGATOR" ).add();
        
        property( "output" ).outputElement( TableDataCollection.class ).auto( "$leftGroup/tablePath/parent$/Joined" )
                .value( DataElementPathEditor.ICON_ID, JoinTableParameters.class.getMethod( "getIcon" ) ).title( "PN_OUTPUT_TABLE" )
                .description( "PD_OUTPUT_TABLE" ).add();
    }

    public static class JoinTypeEditor extends TagEditorSupport
    {
        public JoinTypeEditor()
        {
            super(new String[] {"Inner join", "Outer join", "Left join", "Right join","Left substraction","Right substraction", "Symmetric difference"}, 0);
        }
    }
}
