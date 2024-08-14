package biouml.workbench.graphsearch;

import java.awt.Component;

import javax.swing.JTextField;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.beans.editors.TagEditorSupport;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class QueryOptionsBeanInfo extends BeanInfoEx
{
    
    public QueryOptionsBeanInfo ( )
    {
        this ( QueryOptions.class, MessageBundle.class.getName ( ) );
    }

    protected QueryOptionsBeanInfo ( Class<?> beanClass, String messageBundle )
    {
        super ( beanClass, messageBundle == null ? MessageBundle.class
                .getName ( ) : messageBundle );
    }

    @Override
    public void initProperties ( ) throws Exception
    {
        initResources ( MessageBundle.class.getName ( ) );

        PropertyDescriptorEx pde = new PropertyDescriptorEx ( "direction", beanClass, "getDirection", "setDirection" );
        HtmlPropertyInspector.setDisplayName ( pde, "DI" );
        add( pde, DirectionTypeEditor.class,
                getResourceString ( "PN_QUERY_OPTIONS_DIRECTION" ),
                getResourceString ( "PD_QUERY_OPTIONS_DIRECTION" ) );
        
        pde = new PropertyDescriptorEx ( "depth", beanClass, "getDepth", "setDepth" );
        HtmlPropertyInspector.setDisplayName ( pde, "DE" );
        add( pde, DepthTypeEditor.class,
                getResourceString ( "PN_QUERY_OPTIONS_DEPTH" ),
                getResourceString ( "PD_QUERY_OPTIONS_DEPTH" ) );
    }

    public static class DirectionTypeEditor extends TagEditorSupport
    {
        public DirectionTypeEditor ( )
        {
            super ( MessageBundle.class.getName ( ), DirectionTypeEditor.class,
                    "DIRECTION_TYPES", 0 );
        }
    }
    
    public static class DepthTypeEditor extends CustomEditorSupport
    {
        protected JTextField field = new JTextField ( );

        public DepthTypeEditor ( )
        {
            field.addActionListener ( this );
        }

        @Override
        public Component getCustomEditor ( )
        {
            return field;
        }

        @Override
        public void setValue ( Object value )
        {
            field.setText ( "" + value );
            super.setValue ( value );
        }

        @Override
        public Object getValue ( )
        {
            Integer value = ( Integer ) super.getValue ( );

            if ( field == null || field.getText ( ).length ( ) == 0 )
            {
                value = 1;
                return value;
            }

            try
            {
                value = Integer.valueOf(field.getText ( ));
            }
            catch ( Throwable e )
            {
            }
            
            if ( value == null || value.intValue ( ) <= 0 ||
                    value.intValue ( ) > 10 )
                value = 1;
            
            return value;
        }
    }
    
}
