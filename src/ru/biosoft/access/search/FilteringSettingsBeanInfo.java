package ru.biosoft.access.search;

import java.awt.Component;
import java.beans.FeatureDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.PropertyEditorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;
import com.developmentontheedge.beans.editors.TagEditorSupport;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.exception.LoggedClassNotFoundException;

public class FilteringSettingsBeanInfo extends BeanInfoEx
{
    protected static final Logger log = Logger.getLogger(FilteringSettingsBeanInfo.class.getName());

    public FilteringSettingsBeanInfo()
    {
        super(FilteringSettings.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("collectionName", beanClass, "getCollectionName", "setCollectionName"), ModuleEditor.class,
                getResourceString("PN_FILTERING_SETTINGS_COLLECTION_NAME"), getResourceString("PD_FILTERING_SETTINGS_COLLECTION_NAME"));
        add(new PropertyDescriptorEx("type", beanClass, "getType", "setType"), TypeEditor.class,
                getResourceString("PN_FILTERING_SETTINGS_NODE_TYPE"), getResourceString("PD_FILTERING_SETTINGS_NODE_TYPE"));
        add(new PropertyDescriptorEx("filter", beanClass));
    }

    public static class ModuleEditor extends StringTagEditorSupport
    {
        @Override
        public String[] getTags()
        {
            try
            {
                return CollectionFactoryUtils.getDatabases().names().toArray( String[]::new );
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, t.getMessage(), t);
                return null;
            }
        }
    }

    public static class TypeEditor extends TagEditorSupport implements PropertyEditorEx
    {
        @Override
        public String getAsText()
        {
            return ( (Class)getValue() ).getName();
        }

        @Override
        public void setAsText(String text)
        {
            Class clazz = null;
            try
            {
                clazz = ClassLoading.loadClass( text, null );
            }
            catch( LoggedClassNotFoundException ex )
            {
            }
            setValue(clazz);
        }

        @Override
        public String[] getTags()
        {
            /*
                        try
                        {
                            FilteringSettings bean = (FilteringSettings)getBean();
                            Module module = (Module)Framework.getModules().get( bean.getModule() );
                            Class diagramTypeClass = module.getType().getDiagramTypes()[0];
                            Class[] nodeTypes = ((DiagramType)diagramTypeClass.newInstance()).getNodeTypes();
                            String[] tags = new String[nodeTypes.length];
                            for (int i = 0; i < nodeTypes.length; i++)
                            {
                                tags[i] = nodeTypes[i].getName();
                            }
                            return tags;
                        }
                        catch( Throwable t )
                        {
                            log.log(Level.SEVERE, t.getMessage(), t);
                            return null;
                        }
            */
            return null;
        }

        private Object bean;
        @Override
        public Object getBean()
        {
            return bean;
        }
        @Override
        public void setBean(Object bean)
        {
            this.bean = bean;
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

        @Override
        public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
        {
            throw new UnsupportedOperationException("not used");
        }
        @Override
        public Component getCustomEditor(Component parent, boolean isSelected)
        {
            return null;
        }
    }
}