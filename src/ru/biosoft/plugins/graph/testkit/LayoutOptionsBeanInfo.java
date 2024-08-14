package ru.biosoft.plugins.graph.testkit;

import ru.biosoft.graph.ClusteredGreedyLayouter;
import ru.biosoft.graph.DiagonalPathLayouter;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.GreedyLayouter;
import ru.biosoft.graph.HierarchicLayouter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;

public class LayoutOptionsBeanInfo
        extends BeanInfoEx
{
    public LayoutOptionsBeanInfo ( )
    {
        super ( LayoutOptions.class, "ru.biosoft.graph.testkit.MessageBundle" );
    }

    @Override
    public void initProperties ( )
            throws Exception
    {
        add ( new PropertyDescriptorEx ( "layouter", beanClass ), LayouterTypeEditor.class, getResourceString ( "PN_LAYOUTER" ),
                getResourceString ( "PD_LAYOUTER" ) );
    }

    public static class LayouterTypeEditor
            extends StringTagEditorSupport
    {
        private static final String[] layouterTypes = { "", "ForceDirectedLayouter", "HierarchicLayouter", "GreedyLayouter",
                "DiagonalPathLayouter", "ClusteredGreedyLayouter", "LayeredLayouter" };

        @Override
        public String[] getTags ( )
        {
            return layouterTypes.clone();
        }

        @Override
        public String getAsText ( )
        {
            if (getValue ( ) == null)
                return "";
            return getValue ( ).getClass ( ).getName ( );
        }

        @Override
        public void setAsText ( String text )
        {
            if ( "HierarchicLayouter".equals ( text ) )
                setValue ( new HierarchicLayouter ( ) );
            else if ( "ForceDirectedLayouter".equals ( text ) )
                setValue ( new ForceDirectedLayouter ( ) );
            else if ( "GreedyLayouter".equals ( text ) )
                setValue ( new GreedyLayouter ( ) );
            else if ( "DiagonalPathLayouter".equals ( text ) )
                setValue ( new DiagonalPathLayouter ( ) );
            else if ( "ClusteredGreedyLayouter".equals ( text ) )
                setValue ( new ClusteredGreedyLayouter ( ) );

        }
    }

}