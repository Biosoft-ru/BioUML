package ru.biosoft.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.analysis.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.editors.TagEditorSupport;

public class MultipleTableJoinParametersBeanInfo extends BeanInfoEx2<MultipleTableJoinParameters>
{
    public MultipleTableJoinParametersBeanInfo()
    {
        super(MultipleTableJoinParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    
    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        property( "tablePaths" ).inputElement( TableDataCollection.class ).value( DataElementPathEditor.MULTI_SELECT, true )
                .title( "PN_MULTIJOIN_INPUT" ).description( "PD_MULTIJOIN_INPUT" ).add();
        property( "joinType" ).editor( JoinTypeEditor.class ).title( "PN_JOIN_TYPE" ).description( "PD_JOIN_TYPE" ).add();

        property( "mergeColumns" ).title( "PN_MERGE_COLUMNS" ).description( "PN_MERGE_COLUMNS" ).add();
        property( "ignoreNaNInAggregator" ).expert().titleRaw( "Ignore empty values" )
                .descriptionRaw( "Ignore empty values during aggregator work" ).add();
        property( "aggregator" ).expert().simple().editor( NumericAggregatorEditor.class ).title( "PN_JOIN_AGGREGATOR" )
                .description( "PD_JOIN_AGGREGATOR" ).add();
        
        property( "outputPath" ).outputElement( TableDataCollection.class ).auto( "$tablePaths/path$/Joined" )
                .value( DataElementPathEditor.ICON_ID, MultipleTableJoinParameters.class.getMethod( "getIcon" ) ).title( "PN_OUTPUT_TABLE" )
                .description( "PD_OUTPUT_TABLE" ).add();
    }

    public static class JoinTypeEditor extends TagEditorSupport
    {
        public JoinTypeEditor()
        {
            super(new String[] {"Intersect", "Join"}, 0);
        }
    }
}
