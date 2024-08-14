package ru.biosoft.access.search;

import java.awt.Component;
import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.Vector;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.PropertyEditorEx;

public class PropertyValueFilterBeanInfo extends BeanInfoEx
{
    public PropertyValueFilterBeanInfo()
    {
        super( PropertyValueFilter.class, MessageBundle.class.getName() );
        try
        {
            setDisplayNameMethod( beanClass.getMethod( "getDisplayName" ) );
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
        setCompositeEditor( "enabled;value", new java.awt.GridLayout( 1, 2 ) );
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx( "value", beanClass );
        pde.setDisplayName( beanClass.getMethod( "getDisplayName" ) );
        pde.setHideChildren( true );
        addHidden( pde, FilterPropertyEditor.class );

        pde = new PropertyDescriptorEx( "enabled", beanClass );
        addHidden( pde );
        setSubstituteByChild( true );
    }

    public static class FilterPropertyEditor implements PropertyEditorEx
    {
        PropertyEditor editor = null;

        @Override
        public String[] getTags()
        {
            return editor == null ? null : editor.getTags();
        }
        @Override
        public void setAsText(String text)
        {
            editor.setAsText( text );
        }
        @Override
        public String getAsText()
        {
            if( editor == null )
            {
                return "";
            }
            return editor.getAsText();
        }
        @Override
        public String getJavaInitializationString()
        {
            return editor.getJavaInitializationString();
        }
        @Override
        public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box)
        {
            editor.paintValue( gfx, box );
        }
        @Override
        public boolean isPaintable()
        {
            return true;
            //            if( editor==null )
            //                return false;
            //            return editor.isPaintable();
        }
        @Override
        public Object getValue()
        {
            return editor.getValue();
        }
        private Object cachedValue;
        @Override
        public void setValue(Object value)
        {
            cachedValue = value;
            if( editor != null )
                editor.setValue( value );
        }
        @Override
        public Component getCustomEditor()
        {
            return editor.getCustomEditor();
        }
        @Override
        public boolean supportsCustomEditor()
        {
            return editor == null ? false : editor.supportsCustomEditor();
        }
        @Override
        public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
        {
            if( editor instanceof PropertyEditorEx )
            {
                return ( (PropertyEditorEx)editor ).getCustomRenderer( parent, isSelected, hasFocus );
            }
            return null;
        }
        @Override
        public Component getCustomEditor(Component parent, boolean isSelected)
        {
            return editor.getCustomEditor();
        }

        Object bean;
        @Override
        public void setBean(Object bean)
        {
            this.bean = bean;

            PropertyValueFilter filter = (PropertyValueFilter)bean;
            PropertyDescriptor desc = filter.getDescriptor();

            Class<?> explicitEditorClass = desc.getPropertyEditorClass();
            if( editor == null && explicitEditorClass != null )
            {
                try
                {
                    editor = (PropertyEditor)explicitEditorClass.newInstance();
                }
                catch( IllegalAccessException ex )
                {
                }
                catch( InstantiationException ex )
                {
                }
            }
            if( editor == null )
            {
                Object nEditor = PropertyEditorManager.findEditor( desc.getPropertyType() );
                editor = (PropertyEditor)nEditor;
            }
            if( editor instanceof PropertyEditorEx )
            {
                ( (PropertyEditorEx)editor ).setBean( bean );
            }
            if( cachedValue != null )
            {
                setValue( cachedValue );
            }
        }
        @Override
        public Object getBean()
        {
            return bean;
        }

        protected FeatureDescriptor descriptor;
        @Override
        public FeatureDescriptor getDescriptor()
        {
            return descriptor;
        }

        @Override
        public void setDescriptor(FeatureDescriptor descriptor)
        {
            this.descriptor = descriptor;
        }

        private Vector<PropertyChangeListener> listeners;
        @Override
        public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
        {
            editor.addPropertyChangeListener( listener );
            if( listeners == null )
                listeners = new Vector<>();

            listeners.addElement( listener );
        }

        /**
         * Remove a listener for the PropertyChange event.
         *
         * @param listener  The PropertyChange listener to be removed.
         */
        @Override
        public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
        {
            editor.removePropertyChangeListener( listener );
            if( listeners == null )
                return;

            listeners.removeElement( listener );
        }
    }
}