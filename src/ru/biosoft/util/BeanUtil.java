package ru.biosoft.util;

import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Queue;

import javax.annotation.Nonnull;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.exception.InternalException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.journal.JournalRegistry;

import com.developmentontheedge.beans.annot.ExpertProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.util.Beans;
import com.developmentontheedge.beans.util.Beans.ObjectPropertyAccessor;

/**
 * Miscellaneous functions to process beans
 * @author lan
 */
@CodePrivilege(CodePrivilegeType.REFLECTION)
public class BeanUtil
{
    /**
     * Copies one bean to another
     * It tries to read all the properties values from src bean and store them into dst bean ignoring any possible errors
     * @param src bean to copy
     * @param dst destination bean
     */
    public static void copyBean(Object src, Object dst)
    {
        Beans.copyBean( src, dst );
    }

    /**
     * Sorts list of bean properties according to bean order.
     * Composite property paths like ("property1/property2") are sorted as well.
     * @param model - model for bean
     * @param properties - list of property names (will be sorted)
     */
    public static void sortProperties(final ComponentModel model, List<String> properties)
    {
        Collections.sort(properties, new Comparator<String>()
        {
            private int getPropertyIndex(Property parent, String name)
            {
                for( int i = 0; i < parent.getPropertyCount(); i++ )
                {
                    if( parent.getPropertyAt(i).getName().equals(name) )
                        return i;
                }
                return -1;
            }

            @Override
            public int compare(String prop1, String prop2)
            {
                String[] prop1fields = TextUtil2.split( prop1, '/' );
                String[] prop2fields = TextUtil2.split( prop2, '/' );
                int pos = 0;
                Property parent = model;

                while( true )
                {
                    int index1 = getPropertyIndex(parent, prop1fields[pos]);
                    int index2 = getPropertyIndex(parent, prop2fields[pos]);
                    if( index1 != index2 )
                        return index1 - index2;
                    if( index1 == -1 )
                        return 0;
                    if( pos == prop1fields.length - 1 || pos == prop2fields.length - 1 )
                        return prop1fields.length - prop2fields.length;
                    parent = parent.findProperty(prop1fields[pos]);
                    pos++;
                }
            }
        });
    }
    
    /**
     * Looks for
     * @param bean
     * @param propertyName
     * @return ObjectPropertyAccessor (like simplified PropertyDescriptor)
     * @throws IntrospectionException if property cannot be found
     */
    public static ObjectPropertyAccessor getBeanPropertyAccessor(Object bean, String propertyName) throws IntrospectionException
    {
        return Beans.getBeanPropertyAccessor( bean, propertyName );
    }

    /**
     * Faster equivalent of ComponentFactory.getModel(bean).findProperty(propertyName).getValue()
     * @param bean
     * @param propertyName path to the bean property
     * @return value of given bean property
     * @throws Exception
     */
    public static Object getBeanPropertyValue(Object bean, String propertyName) throws Exception
    {
        return getBeanPropertyAccessor( bean, propertyName ).get();
    }
    
    /**
     * Faster equivalent of ComponentFactory.getModel(bean).findProperty(propertyName).setValue(value)
     * @param bean
     * @param propertyName path to the bean property
     * @param value value to set
     * @throws Exception
     */
    public static void setBeanPropertyValue(Object bean, String propertyName, Object value) throws Exception
    {
        getBeanPropertyAccessor( bean, propertyName ).set( value );
    }
    
    /**
     * Faster equivalent of ComponentFactory.getModel(bean).findProperty(propertyName).getPropertyType()
     * @param bean
     * @param propertyName path to the bean property
     * @return property type
     * @throws Exception
     */
    public static Class<?> getBeanPropertyType(Object bean, String propertyName) throws Exception
    {
        return getBeanPropertyAccessor( bean, propertyName ).getType();
    }
    /**
     * For given element looks parent for the property representing the element
     * @param element to look up
     * @param parent parent where to look for property
     * @return property name in parent (probably with path); null if not found or parent is null
     */
    public static String findPropertyInParent(Object element, Object parent)
    {
        if(element == null) return null;
        if(parent == null) return null;
        ComponentModel model = ComponentFactory.getModel(parent);
        for(int i=0; i<model.getPropertyCount(); i++)
        {
            Property property = model.getPropertyAt(i);
            if(property == null) continue;
            Object value = property.getValue();
            if(value == null) continue;
            if(value == element) return property.getName();
            if(value.getClass().isArray() && value.getClass().getComponentType().isInstance(element))
            {
                int length = Array.getLength(value);
                for(int j=0; j<length; j++)
                {
                    if(Array.get(value, j) == element) return property.getName()+"/["+j+"]";
                }
            }
        }
        return null;
    }
    
    public static String findPropertyInParent(Option element)
    {
        if(element == null) return null;
        if(element.getNameInParent() != null) return element.getNameInParent();
        return findPropertyInParent(element, element.getParent());
    }
    
    public static String getPropertyPathFromParent(Option element, String property)
    {
        String parentPath = findPropertyInParent(element);
        if(parentPath == null) return null;
        return property == null?parentPath:parentPath+"/"+property;
    }
    
    public static String getPropertyPathFromRoot(Option element, Option root, String property)
    {
        if(element == null || root == null) return null;
        Option currentElement = element;
        while(currentElement != root)
        {
            property = getPropertyPathFromParent(currentElement, property);
            if(property == null) return null;
            currentElement = currentElement.getParent();
        }
        return property;
    }

    public static PropertyDescriptorEx createExpertDescriptor(String propertyName, Class<?> beanClass) throws IntrospectionException
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx(propertyName, beanClass);
        pde.setExpert(true);
        return pde;
    }

    public static PropertyInfo[] getRecursivePropertiesList(Object bean)
    {
        if( bean == null )
            return new PropertyInfo[0];
        ComponentModel model = ComponentFactory.getModel(bean);
        List<PropertyInfo> result = new ArrayList<>();
        Queue<PropertyInfo> toFetch = new ArrayDeque<>();
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            Property property = model.getPropertyAt(i);
            String propertyName = property.getName();
            PropertyInfo propertyInfo = new PropertyInfo(propertyName, property.getDisplayName());
            result.add(propertyInfo);
            toFetch.add(propertyInfo);
        }
        while( !toFetch.isEmpty() )
        {
            PropertyInfo propertyInfo = toFetch.poll();
            Property property = model.findProperty(propertyInfo.getName());
            if( property instanceof ArrayProperty || property.isLeaf() || property.isHideChildren() )
                continue;
            for( int i = 0; i < property.getPropertyCount(); i++ )
            {
                Property subProperty = property.getPropertyAt(i);
                PropertyInfo subPropertyInfo = new PropertyInfo(propertyInfo.getName() + "/" + subProperty.getName(), propertyInfo
                        .getDisplayName()
                        + "/" + subProperty.getDisplayName());
                result.add(subPropertyInfo);
                toFetch.add(subPropertyInfo);
            }
        }
        Collections.sort(result);
        return result.toArray(new PropertyInfo[result.size()]);
    }

    // TODO: convert to PropertyInfo too
    public static String[] getPropertiesList(Object bean) throws Exception
    {
        ComponentModel model = ComponentFactory.getModel(bean, Policy.UI, true);
        int size = model.getPropertyCount();
        String[] properties = new String[size];
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            properties[i] = model.getPropertyAt(i).getName();
        }
        return properties;
    }

    @SuppressWarnings ( "unchecked" )
    public static @Nonnull <T> List<T> mapBeanProperties(Iterable<?> beans, String property) throws Exception
    {
        List<T> result = new ArrayList<>();
        for(Object bean: beans)
        {
            result.add((T)getBeanPropertyValue(bean, property));
        }
        return result;
    }
    
    public static @Nonnull String joinBeanProperties(Iterable<?> beans, String property, String delimiter) throws Exception
    {
        StringBuilder result = new StringBuilder();
        for(Object bean: beans)
        {
            if(result.length() > 0)
                result.append(delimiter);
            result.append(String.valueOf(getBeanPropertyValue(bean, property)));
        }
        return result.toString();
    }
    
    public static @Nonnull String joinBeanProperties(Object[] beans, String property, String delimiter) throws Exception
    {
        return joinBeanProperties(Arrays.asList(beans), property, delimiter);
    }
    
    public static void readBeanFromProperties(Object bean, Properties properties, String prefix)
    {
        ComponentModel model = ComponentFactory.getModel(bean);
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            Property property = model.getPropertyAt(i);
            if( !properties.containsKey(prefix + property.getName()) )
                continue;
            String valueStr = properties.getProperty(prefix + property.getName());
            try
            {
                Object value = TextUtil2.fromString(property.getValueClass(), valueStr);
                if(value != null)
                {
                    property.setValue(value);
                }
            }
            catch( Exception e )
            {
            }
        }
    }
    
    public static void writeBeanToProperties(Object bean, Properties properties, String prefix)
    {
        ComponentModel model = ComponentFactory.getModel(bean);
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            Property property = model.getPropertyAt(i);
            Object value = property.getValue();
            if( value != null )
            {
                properties.put(prefix + property.getName(), TextUtil2.toString(value));
            } else
            {
                properties.remove(prefix + property.getName());
            }
        }
    }
    public static DynamicPropertySet createDPSForBeans(Object... beans) throws IntrospectionException
    {
        DynamicPropertySet result = new DynamicPropertySetSupport();
        for(Object bean : beans)
        {
            Class<? extends Object> type = bean.getClass();

            PropertyName pName = type.getAnnotation( PropertyName.class );
            String name = pName == null ? type.getSimpleName() : pName.value();
            PropertyDescription pDescription = type.getAnnotation( PropertyDescription.class );
            String description = pDescription == null ? name : pDescription.value();

            DynamicProperty property = new DynamicProperty( name, type, bean );
            
            property.setShortDescription( description );
            
            ExpertProperty expertProperty = type.getAnnotation( ExpertProperty.class );
            if(expertProperty != null)
                property.setExpert( true );
            
            result.add( property );
        }
        return result;
    }

    /**
     * Returns best-guess path to store results associated with given bean
     * Useful for auto-determination of analysis output path
     */
    public static DataElementPath getDefaultPath(Object bean)
    {
        ComponentModel model = ComponentFactory.getModel(bean);
        for(int i=0; i<model.getPropertyCount(); i++)
        {
            try
            {
                Property property = model.getPropertyAt(i);
                if( property.isVisible(Property.SHOW_USUAL) && property.getPropertyEditorClass().equals(DataElementPathEditor.class)
                        && property.getBooleanAttribute(DataElementPathEditor.ELEMENT_MUST_EXIST) )
                {
                    Object value = property.getValue();
                    DataElementPath path = null;
                    if(value instanceof DataElementPathSet)
                    {
                        path = ((DataElementPathSet)value).getPath();
                    } else if(value instanceof DataElementPath)
                    {
                        path = ((DataElementPath)value).getParentPath();
                    }
                    if(path != null)
                    {
                        DataCollection<?> dc = path.optDataCollection();
                        if(dc != null && dc.isMutable())
                            return path;
                    }
                }
            }
            catch( Exception e )
            {
            }
        }
        return JournalRegistry.getProjectPath().getChildPath("Data");
    }
    
    public static String getPropertySortingValue(Property model, String propertyPath)
    {
        String propertyName = propertyPath;
        int pos = propertyName.indexOf('/');
        if(pos >= 0)
        {
            propertyName = propertyName.substring(0, pos);
            propertyPath = propertyPath.substring(pos+1);
        }
        int count = model.getPropertyCount();
        for(int i=0; i<count; i++)
        {
            Property propertyAt = model.getPropertyAt(i);
            if(propertyAt.getName().equals(propertyName))
            {
                String result = String.format(Locale.ENGLISH, "%05d", i);
                if(pos >= 0)
                    return result+"/"+getPropertySortingValue(propertyAt, propertyPath);
                else
                    return result;
            }
        }
        return "-----";
    }
    
    public static String getBeanSortingValue(Object bean, String propertyPath)
    {
        return getPropertySortingValue(ComponentFactory.getModel(bean), propertyPath);
    }
    
    public static boolean getBooleanValue(FeatureDescriptor fd, Object bean, String key, boolean defaultValue)
    {
        Object valueObj = fd.getValue(key);
        if( valueObj instanceof Boolean )
            return (Boolean)valueObj;
        else if( valueObj instanceof Method )
        {
            try
            {
                return (Boolean) ( (Method)valueObj ).invoke(bean);
            }
            catch( Throwable t )
            {
            }
        }
        return defaultValue;
    }
    
    public static boolean getBooleanValue(CustomEditorSupport editor, String key, boolean defaultValue)
    {
        return getBooleanValue(editor.getDescriptor(), editor.getBean(), key, defaultValue);
    }
    
    public static boolean getBooleanValue(CustomEditorSupport editor, String key)
    {
        return getBooleanValue(editor, key, false);
    }

    public static String getStringValue(FeatureDescriptor fd, Object bean, String key)
    {
        Object valueObj = fd.getValue(key);
        if( valueObj instanceof Method )
        {
            try
            {
                return (String) ( (Method)valueObj ).invoke(bean);
            }
            catch( Throwable t )
            {
            }
        }
        else if( valueObj != null )
            return valueObj.toString();
        return null;
    }
    
    public static String getStringValue(CustomEditorSupport editor, String key)
    {
        return getStringValue(editor.getDescriptor(), editor.getBean(), key);
    }
    
    public static PropertyDescriptor createDescriptor(String name)
    {
        try
        {
            return new PropertyDescriptor(name, null, null);
        }
        catch( IntrospectionException e )
        {
            throw new InternalException(e);
        }
    }
    
    public static StreamEx<Property> properties(Object bean)
    {
        ComponentModel model = ComponentFactory.getModel( bean, Policy.UI, true );
        return properties(model);
    }

    public static StreamEx<Property> properties(Property model)
    {
        return IntStreamEx.range( model.getPropertyCount() ).mapToObj( model::getPropertyAt );
    }
}
