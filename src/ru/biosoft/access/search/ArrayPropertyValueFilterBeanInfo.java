package ru.biosoft.access.search;

import java.awt.Component;
import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.PropertyEditorEx;

public class ArrayPropertyValueFilterBeanInfo extends BeanInfoEx
{
    public ArrayPropertyValueFilterBeanInfo()
    {
        super(ArrayPropertyValueFilter.class, MessageBundle.class.getName());
        try
        {
            setDisplayNameMethod(beanClass.getMethod("getDisplayName"));
        }
        catch( Throwable t )
        {
            t.printStackTrace();
        }
        setCompositeEditor("enabled;value",new java.awt.GridLayout(1, 2));
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx("value",beanClass);
        pde.setDisplayName(beanClass.getMethod("getDisplayName"));
        pde.setHideChildren(true);
        addHidden(pde, FilterPropertyEditor.class);
        pde = new PropertyDescriptorEx( "enabled",beanClass);
        addHidden(pde);
        setSubstituteByChild(true);
    }

    public static class FilterPropertyEditor implements PropertyEditorEx
    {
        PropertyEditor editor = null;

        @Override
        public String[] getTags()
        {
            return editor.getTags();
        }
        @Override
        public void setAsText(String text)
        {
            editor.setAsText(text);
        }
        @Override
        public String getAsText()
        {
            if( editor==null )
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
            editor.paintValue(gfx,box);
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
        public void setValue( Object value )
        {
            cachedValue = value;
            if( editor!=null )
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
            return editor.supportsCustomEditor();
        }
        @Override
        public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
        {
            if( editor instanceof PropertyEditorEx )
            {
                return ((PropertyEditorEx)editor).getCustomRenderer(parent,isSelected,hasFocus);
            }
            return null;
        }
        @Override
        public Component getCustomEditor(Component parent, boolean isSelected)
        {
            if( editor instanceof PropertyEditorEx )
                return ((PropertyEditorEx)editor).getCustomEditor(parent,isSelected);
            else
                return editor.getCustomEditor();
        }

        Object bean;
        @Override
        public void setBean(Object bean)
        {
            this.bean = bean;

            ArrayPropertyValueFilter filter = (ArrayPropertyValueFilter)bean;
            PropertyDescriptor desc = filter.getDescriptor();

            Class<?> propertyType = desc.getPropertyType();
            if( Object[].class.isAssignableFrom(propertyType) )
            {
                propertyType = propertyType.getComponentType();
            }

            Class<?> explicitEditorClass = desc.getPropertyEditorClass();
            if( editor==null && explicitEditorClass!=null && propertyType==desc.getPropertyType())
            {
                try
                {
                    editor = (PropertyEditor)explicitEditorClass.newInstance();
                }
                catch (IllegalAccessException ex)
                {
                    ex.printStackTrace();
                }
                catch (InstantiationException ex)
                {
                    ex.printStackTrace();
                }
            }
            if( editor==null )
            {
                Object nEditor = PropertyEditorManager.findEditor( propertyType );
                editor = (PropertyEditor)nEditor;
            }
            if( editor instanceof PropertyEditorEx )
            {
                ((PropertyEditorEx)editor).setBean(bean);
            }
            if( cachedValue!=null )
            {
                setValue(cachedValue);
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

        private java.util.Vector listeners;
        @Override
        public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
        {
            editor.addPropertyChangeListener( listener );
            if( listeners == null )
            {
                listeners = new java.util.Vector();
            }

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
            {
                return;
            }
            listeners.removeElement( listener );

        }
    }
}