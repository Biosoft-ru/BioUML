package ru.biosoft.server;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import one.util.streamex.EntryStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.exception.InternalException;
import ru.biosoft.access.repository.JSONSerializable;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.server.custombean.ColorFontWrapper;
import ru.biosoft.server.custombean.DimensionWrapper;
import ru.biosoft.server.custombean.PointWrapper;
import ru.biosoft.util.FieldMap;
import ru.biosoft.util.JSONCompatibleEditor;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxItem;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;
import ru.biosoft.workbench.editors.GenericMultiSelectItem;

import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.beans.editors.PropertyEditorEx;
import com.developmentontheedge.beans.editors.StringTagEditorSupport;
import com.developmentontheedge.beans.editors.TagEditorSupport;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.model.SimpleProperty;

/**
 * Utility class for JSON manipulation
 */
public class JSONUtils
{
    public static final String NAME_ATTR = "name";
    public static final String DISPLAYNAME_ATTR = "displayName";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String TYPE_ATTR = "type";
    public static final String READONLY_ATTR = "readOnly";
    public static final String VALUE_ATTR = "value";
    public static final String CHILDREN_ATTR = "children";
    public static final String DICTIONARY_ATTR = "dictionary";

    private static final Map<Class<?>, Class<?>> customBeans = EntryStream.<Class<?>, Class<?>> of(
            Point.class, PointWrapper.class,
            ColorFont.class, ColorFontWrapper.class,
            Dimension.class, DimensionWrapper.class ).toMap();

    /**
     * Apply values from JSON to bean
     */
    public static void correctBeanOptions(Object bean, JSONArray jsonParams) throws JSONException, InternalException
    {
        correctBeanOptions( bean, jsonParams, true );
    }

    public static void correctBeanOptions(Object bean, JSONArray jsonParams, boolean jsonOrder) throws JSONException, InternalException
    {
        CompositeProperty model = resolveModel( bean );
        if( jsonOrder )
        {
            for( int j = 0; j < jsonParams.length(); j++ )
                for( int i = 0; i < model.getPropertyCount(); i++ )
                    setBeanProperty( model.getPropertyAt( i ), jsonParams.getJSONObject( j ), jsonOrder );
        }
        else
        {
            for( int i = 0; i < model.getPropertyCount(); i++ )
                for( int j = 0; j < jsonParams.length(); j++ )
                    setBeanProperty( model.getPropertyAt( i ), jsonParams.getJSONObject( j ), jsonOrder );
        }
    }

    private static void setBeanProperty(Property property, JSONObject jsonObject, boolean jsonOrder) throws JSONException {
        String propertyName = property.getName();
        if( property.isReadOnly() && ! ( property instanceof ArrayProperty ) )
            return;
        String name = jsonObject.getString( "name" );
        if( name == null || !name.equals( propertyName ))
            return;
        try
        {
            if( property instanceof CompositeProperty && ( !property.isHideChildren()
                    || property.getPropertyEditorClass() == PenEditor.class ) )
            {
                correctCompositeProperty( property, jsonObject, jsonOrder );
            }
            else if( property instanceof ArrayProperty )
            {
                correctArrayProperty( (ArrayProperty)property, jsonObject, jsonOrder );
            }
            else
            {
                convertSimpleProperty( property, jsonObject );
            }
        }
        catch( Exception e )
        {
            throw new InternalException( e, "Error updating bean property '" + name + "' (json = " + jsonObject + ")" );
        }
    }

    private static void initEditor(Property property, PropertyEditorEx editor)
    {
        Object owner = property.getOwner();
        if( owner instanceof Property.PropWrapper )
            owner = ( (Property.PropWrapper)owner ).getOwner();
        editor.setValue(property.getValue());
        editor.setBean(owner);
        editor.setDescriptor(property.getDescriptor());
    }

    private static CompositeProperty resolveModel(Object bean)
    {
        CompositeProperty model;
        if( bean instanceof CompositeProperty )
        {
            model = (CompositeProperty)bean;
        }
        else
        {
            model = ComponentFactory.getModel(bean, Policy.UI, true);
        }
        return model;
    }

    private static void convertSimpleProperty(Property property, JSONObject jsonObject) throws Exception
    {
        Class<?> c = property.getPropertyEditorClass();
        if( c != null )
        {
            if( JSONSerializable.class.isAssignableFrom(c) )
            {
                JSONSerializable editor = (JSONSerializable)c.newInstance();
                if( editor instanceof PropertyEditorEx )
                {
                    initEditor( property, (PropertyEditorEx)editor );
                    editor.fromJSON(jsonObject);
                    setValue( property, ( (PropertyEditorEx)editor ).getValue());
                    return;
                }
            }
        }
        Object value = getSimpleValueFromJSON(property.getValueClass(), jsonObject);
        setValue( property, value );
    }

    private static void correctArrayProperty(ArrayProperty property, JSONObject jsonObject, boolean jsonOrder) throws Exception
    {
        Class<?> c = property.getPropertyEditorClass();
        if( c != null )
        {
            if( GenericMultiSelectEditor.class.isAssignableFrom(c) )
            {
                GenericMultiSelectEditor editor = (GenericMultiSelectEditor)c.newInstance();
                initEditor( property, editor );

                Object jsonValue = jsonObject.get( "value" );
                if(jsonValue instanceof String)
                    jsonValue = new JSONArray( (String)jsonValue );
                JSONArray jsonArray = (JSONArray)jsonValue;

                String[] val = new String[jsonArray.length()];
                for( int index = 0; index < val.length; index++ )
                    val[index] = jsonArray.getString(index);
                editor.setStringValue(val);
                setValue(property, editor.getValue());
                return;
            }
            else if( JSONCompatibleEditor.class.isAssignableFrom(c) )
            {
                JSONCompatibleEditor editor = (JSONCompatibleEditor)c.newInstance();
                editor.fillWithJSON(property, jsonObject);
                return;
            }
        }

        JSONArray jsonArray = jsonObject.getJSONArray("value");
        //process array actions
        if( !property.isReadOnly() )
        {
            while( jsonArray.length() > property.getPropertyCount() )
            {
                property.insertItem(property.getPropertyCount(), null);
            }
            while( jsonArray.length() < property.getPropertyCount() )
            {
                property.removeItem(property.getPropertyCount() - 1);
            }
        }
        Object oldValue = property.getValue();
        if( oldValue != null && oldValue.getClass().isArray() )
        {
            Property element = property.getPropertyAt(0);
            if( element instanceof SimpleProperty ) //Simple Properties processing
            {
                for( int k = 0; k < property.getPropertyCount(); k++ )
                {
                    Property oldElement = property.getPropertyAt(k);
                    String elemName = oldElement.getName();
                    for( int m = 0; m < jsonArray.length(); m++ )
                    {
                        JSONArray jsonBean = jsonArray.getJSONArray(m);
                        for( int ind = 0; ind < jsonBean.length(); ind++ )
                        {
                            JSONObject jsonProperty = jsonBean.getJSONObject(ind);
                            if( jsonProperty.getString("name").equals(elemName) )
                            {
                                Object value = getSimpleValueFromJSON( oldElement.getValueClass(), jsonProperty);
                                setValue( oldElement, value );
                                break;
                            }
                        }
                    }
                }
            }
            else
            {
                Object[] oldArray = (Object[])oldValue;

                int index = 0;
                for( Object oldObject : oldArray )
                {
                    CompositeProperty elementModel = resolveModel( oldObject );
                    if( jsonArray.length() > index )
                    {
                        JSONArray jsonBean = jsonArray.getJSONArray(index);
                        correctBeanOptions(elementModel, jsonBean);
                    }
                    index++;
                }
            }
        }
        if( jsonObject.has("action") && !property.isReadOnly() )
        {
            String actionName = jsonObject.getString("action");
            if( actionName.equals("item-add") )
            {
                property.insertItem( property.getPropertyCount(), null);
            }
            else if( actionName.equals("item-remove") )
            {
                int size = property.getPropertyCount();
                if( size > 0 )
                {
                    property.removeItem(size - 1);
                }
            }
        }
    }

    private static void correctCompositeProperty(Property property, JSONObject jsonObject, boolean jsonOrder) throws Exception
    {
        Object oldValue = property.getValue();
        Class<?> valueClass = property.getValueClass();
        if( GenericComboBoxItem.class.isAssignableFrom( valueClass ) )
        {
            setValue(property, new GenericComboBoxItem((GenericComboBoxItem)oldValue, jsonObject.getString("value")));
        }
        else if( GenericMultiSelectItem.class.isAssignableFrom( valueClass ) )
        {
            JSONArray arr = jsonObject.getJSONArray("value");
            String[] vals = new String[arr.length()];
            for( int index = 0; index < arr.length(); index++ )
                vals[index] = arr.getString(index);
            setValue(property, new GenericMultiSelectItem((GenericMultiSelectItem)oldValue, vals));
        }
        else if( Color.class.isAssignableFrom( valueClass ) )
        {
            JSONArray jsonArray = jsonObject.getJSONArray("value");
            String obj = jsonArray.getString(0);
            Color newColor = parseColor(obj);
            setValue(property, newColor);
        }
        else if( oldValue != null && customBeans.containsKey( valueClass ))
        {
            Object wrapper = customBeans.get( valueClass ).getConstructor( valueClass ).newInstance( oldValue );
            correctBeanOptions( ComponentFactory.getModel( wrapper ), jsonObject.getJSONArray("value"), jsonOrder);
        }
        else
        {
            correctBeanOptions(property, jsonObject.getJSONArray("value"), jsonOrder);
        }
    }

    private static void setValue(Property property, Object value) throws NoSuchMethodException
    {
        property.setValue( value );
        if(property.getBooleanAttribute( BeanInfoEx2.STRUCTURE_CHANGING_PROPERTY ))
        {
            ComponentFactory.recreateChildProperties( property.getParent() );
        }
    }

    private static Object getSimpleValueFromJSON(Class<?> type, JSONObject jsonObject)
    {
        if( type == null || type.equals( String.class ) )
        {
            return jsonObject.optString("value");
        }
        else if( type.equals( Integer.class ) )
        {
            return jsonObject.optInt("value", 0);
        }
        else if( type.equals( Long.class ) )
        {
            return jsonObject.optLong( "value", 0L );
        }
        else if( type.equals( Double.class )  )
        {
            return jsonObject.optDouble("value", 0);
        }
        else if( type.equals( Float.class )  )
        {
            return (float)jsonObject.optDouble("value", 0);
        }
        else if( type.equals( Boolean.class ) )
        {
            return jsonObject.optBoolean("value");
        }
        return null;
    }

    /**
     * Parses Color string
     * @param str color in format [r,g,b] or empty string for absent color
     * @return
     * @throws JSONException
     */
    public static Color parseColor(String str) throws JSONException
    {
        Color newColor;
        if(str.isEmpty())
            newColor = new Color(0,0,0,0);
        else
        {
            JSONArray jsobj = new JSONArray(str);
            newColor = new Color(jsobj.getInt(0), jsobj.getInt(1), jsobj.getInt(2));
        }
        return newColor;
    }

    /**
     * Counterpart for parseColor
     * @param color color to encode
     * @return
     */
    private static String encodeColor(Color color)
    {
        if(color == null || color.getAlpha() == 0) return "";
        return "["+color.getRed()+","+color.getGreen()+","+color.getBlue()+"]";
    }

    protected static JSONArray createDictionary(Object[] strings, boolean byPosition)
    {
        if( strings == null )
            strings = new Object[] {};
        int position = 0;
        JSONArray values = new JSONArray();
        for( Object tagObj : strings )
        {
            String tag = tagObj.toString();
            if( byPosition )
                values.put(new JSONArray(Arrays.asList(String.valueOf(position), tag)));
            else
                values.put(new JSONArray(Arrays.asList(tag, tag)));
            position++;
        }
        return values;
    }

    /**
     * Returns true if model contains at least one expert option on any level
     * @param model - model to check
     */
    public static boolean isExpertAvailable(CompositeProperty model)
    {
        for( int i = 0; i < model.getPropertyCount(); i++ )
        {
            Property property = model.getPropertyAt(i);
            if( property.isVisible(Property.SHOW_EXPERT) && !property.isVisible(Property.SHOW_USUAL) )
                return true;
            if( property instanceof CompositeProperty && property.isVisible(Property.SHOW_USUAL)
                    && isExpertAvailable((CompositeProperty)property) )
                return true;
        }
        return false;
    }

    /**
     * Returns additional bean attributes
     * @param model - model of bean to get attributes
     * @return JSONObject containing attributes
     */
    public static JSONObject getBeanAttributes(CompositeProperty model)
    {
        JSONObject result = new JSONObject();
        try
        {
            result.put("expertOptions", isExpertAvailable(model));
        }
        catch( JSONException e )
        {
        }
        return result;
    }

    /**
     * Convert model to JSON
     * @param properties model to convert
     */
    public static JSONArray getModelAsJSON(CompositeProperty properties) throws Exception
    {
        return getModelAsJSON(properties, FieldMap.ALL, Property.SHOW_USUAL);
    }

    /**
     * Convert model to JSON
     * @param properties model to convert
     * @param fieldMap fieldMap of properties to include. Cannot be null. Use {@link FieldMap#ALL} to include all fields
     * @param showMode mode like {@link Property#SHOW_USUAL} which may filter some fields additionally
     */
    public static JSONArray getModelAsJSON(CompositeProperty properties, FieldMap fieldMap, int showMode)
            throws Exception
    {
        JSONArray result = new JSONArray();
        for( int i = 0; i < properties.getPropertyCount(); i++ )
        {
            Property property = properties.getPropertyAt(i);
            try
            {
                JSONObject object = convertSingleProperty( fieldMap, showMode, property );
                if(object != null)
                    result.put(object);
            }
            catch( Exception e )
            {
                throw new InternalException( e, "Unable to convert property: #" + i + ": "
                        + ( property == null ? null : property.getName() ) );
            }
        }
        return result;
    }

    private static JSONObject convertSingleProperty(FieldMap fieldMap, int showMode, Property property) throws Exception
    {
        String name = property.getName();
        if ( !property.isVisible(showMode) && !property.getBooleanAttribute(BeanInfoEx2.IMPLICIT_PROPERTY) || !fieldMap.contains(name) )
            return null;
        JSONObject p = new JSONObject();
        p.put(NAME_ATTR, name);
        p.put(DISPLAYNAME_ATTR, property.getDisplayName());
        p.put(DESCRIPTION_ATTR, property.getShortDescription().split("\n")[0]);
        p.put(READONLY_ATTR, property.isReadOnly());
        if ( property.getBooleanAttribute(BeanInfoEx2.IMPLICIT_PROPERTY) )
            p.put(BeanInfoEx2.IMPLICIT_PROPERTY, true);
        if( property instanceof CompositeProperty && ( !property.isHideChildren() || property.getPropertyEditorClass() == PenEditor.class ) )
        {
            return fillCompositeProperty( fieldMap, showMode, property, p );
        }
        if( property instanceof ArrayProperty && !property.isHideChildren() )
        {
            return fillArrayProperty( fieldMap, showMode, property, p );
        }
        return fillSimpleProperty( property, p );
    }

    private static JSONObject fillSimpleProperty(Property property, JSONObject p) throws InstantiationException, IllegalAccessException,
            JSONException
    {
        Class<?> editorClass = property.getPropertyEditorClass();
        if( editorClass != null )
        {
            if( JSONSerializable.class.isAssignableFrom(editorClass) )
            {
                JSONSerializable editor = (JSONSerializable)editorClass.newInstance();
                if( editor instanceof PropertyEditorEx )
                {
                    initEditor( property, (PropertyEditorEx)editor );
                    JSONObject p1 = editor.toJSON();
                    if( p1 != null )
                    {
                        Iterator<?> iterator = p1.keys();
                        while( iterator.hasNext() )
                        {
                            String key = iterator.next().toString();
                            if( key.equals("dictionary") )
                            {
                                JSONArray array = p1.optJSONArray("dictionary");
                                if( array != null )
                                {
                                    String[] elements = new String[array.length()];
                                    for( int index = 0; index < array.length(); index++ )
                                        elements[index] = array.optString(index);
                                    p.put(DICTIONARY_ATTR, createDictionary(elements, false));
                                }
                            }
                            else
                            {
                                p.put(key, p1.get(key));
                            }
                        }
                        return p;
                    }
                }
            }
            else if( TagEditorSupport.class.isAssignableFrom(editorClass) )
            {
                TagEditorSupport editor = (TagEditorSupport)editorClass.newInstance();
                String[] tags = editor.getTags();
                if( tags != null )
                {
                    p.put(DICTIONARY_ATTR, createDictionary(tags, true));
                }
            }
            else if( StringTagEditorSupport.class.isAssignableFrom(editorClass) )
            {
                StringTagEditorSupport editor = (StringTagEditorSupport)editorClass.newInstance();
                String[] tags = editor.getTags();
                if( tags != null )
                {
                    p.put(DICTIONARY_ATTR, createDictionary(tags, false));
                }
            }
            else if( CustomEditorSupport.class.isAssignableFrom(editorClass) )
            {
                CustomEditorSupport editor = null;
                //TODO: support or correctly process some editors
                //Some editors like biouml.model.util.ReactionEditor, biouml.model.util.FormulaEditor
                //use Application.getApplicationFrame(), so we got a NullPointerException here
                try
                {
                    editor = (CustomEditorSupport)editorClass.newInstance();
                    initEditor( property, editor );
                    String[] tags = editor.getTags();
                    if( tags != null )
                    {
                        p.put(DICTIONARY_ATTR, createDictionary(tags, false));
                    }
                }
                catch( Exception e )
                {
                }
            }
        }

        Object value = property.getValue();
        if( value != null )
        {
            p.put(TYPE_ATTR, (value instanceof Boolean) ? "bool" : "code-string");
            p.put(VALUE_ATTR, value.toString());
        }
        return p;
    }

    private static JSONObject fillArrayProperty(FieldMap fieldMap, int showMode, Property property, JSONObject p)
            throws Exception
    {
        Class<?> c = property.getPropertyEditorClass();
        if( c != null )
        {
            if( CustomEditorSupport.class.isAssignableFrom(c) )
            {
                CustomEditorSupport editor = (CustomEditorSupport)c.newInstance();
                initEditor( property, editor );
                String[] tags = editor.getTags();
                if( tags != null )
                {
                    p.put(DICTIONARY_ATTR, createDictionary(tags, false));
                    p.put(TYPE_ATTR, "multi-select");
                    Object[] vals = (Object[])property.getValue();
                    JSONArray value = new JSONArray();
                    if( vals != null )
                    {
                        for( Object val : vals )
                        {
                            value.put(val.toString());
                        }
                    }
                    p.put(VALUE_ATTR, value);
                    return p;
                }
                if( editor instanceof JSONCompatibleEditor )
                {
                    ( (JSONCompatibleEditor)editor ).addAsJSON(property, p, fieldMap, showMode);
                    return p;
                }
            }
        }
        JSONArray value = new JSONArray();
        ArrayProperty array = (ArrayProperty)property;
        for( int j = 0; j < array.getPropertyCount(); j++ )
        {
            Property element = array.getPropertyAt(j);
            if( element instanceof CompositeProperty )
            {
                value.put(getModelAsJSON((CompositeProperty)element, fieldMap.get(property), showMode));
            }
            else
            {
                JSONObject pCh = new JSONObject();
                Object val = element.getValue();
                if( val != null )
                {
                    pCh.put(TYPE_ATTR, (val instanceof Boolean) ? "bool" : "code-string");
                    pCh.put(NAME_ATTR, element.getName());
                    pCh.put(DISPLAYNAME_ATTR, element.getName());
                    pCh.put(VALUE_ATTR, val.toString());
                    pCh.put(READONLY_ATTR, element.isReadOnly());
                    value.put(new JSONArray().put(pCh));
                }
            }
        }
        p.put(TYPE_ATTR, "collection");
        if ( property.getBooleanAttribute(BeanInfoEx2.FIXED_LENGTH_PROPERTY) )
            p.put(BeanInfoEx2.FIXED_LENGTH_PROPERTY, true);
        p.put(VALUE_ATTR, value);
        return p;
    }

    private static JSONObject fillCompositeProperty(FieldMap fieldMap, int showMode, Property property, JSONObject p)
            throws Exception
    {
        Object value = property.getValue();
        Class<?> valueClass = property.getValueClass();
        if( GenericComboBoxItem.class.isAssignableFrom( valueClass ) )
        {
            p.put(TYPE_ATTR, "code-string");
            p.put(VALUE_ATTR, ( (GenericComboBoxItem)value ).getValue());
            ( (GenericComboBoxItem)value ).updateAvailableValues();
            p.put(DICTIONARY_ATTR, createDictionary( ( (GenericComboBoxItem)value ).getAvailableValues(), false));
        }
        else if( GenericMultiSelectItem.class.isAssignableFrom( valueClass ) )
        {
            p.put(TYPE_ATTR, "multi-select");
            p.put(VALUE_ATTR, ( (GenericMultiSelectItem)value ).getValues());
            ( (GenericMultiSelectItem)value ).updateAvailableValues();
            p.put(DICTIONARY_ATTR, createDictionary( ( (GenericMultiSelectItem)value ).getAvailableValues(), false));
        }
        else if( value != null && customBeans.containsKey( valueClass ) )
        {
            Object wrapper = customBeans.get( valueClass ).getConstructor( valueClass ).newInstance( value );
            p.put(TYPE_ATTR, "composite");
            p.put(VALUE_ATTR, getModelAsJSON(ComponentFactory.getModel( wrapper ), fieldMap.get(property), showMode));
        }
        else if( Color.class.isAssignableFrom( valueClass ) )
        {
            p.put(TYPE_ATTR, "color-selector");
            JSONArray valueEl = new JSONArray();
            valueEl.put(encodeColor((Color)value));
            p.put(VALUE_ATTR, valueEl);
        }
        else
        {
            p.put(TYPE_ATTR, "composite");
            p.put(VALUE_ATTR, getModelAsJSON((CompositeProperty)property, fieldMap.get(property), showMode));
        }
        return p;
    }

    public static JSONArray toSimpleJSONArray(Collection<?> collection)
    {
        JSONArray jsonArray = new JSONArray();
        if( collection != null )
        {
            for( Object o : collection )
            {
                jsonArray.put( o.toString() );
            }
        }
        return jsonArray;
    }

    public static JSONArray toSimpleJSONArray(Object[] array)
    {
        JSONArray jsonArray = new JSONArray();
        if( array != null )
        {
            for( Object element : array )
            {
                jsonArray.put( element.toString() );
            }
        }
        return jsonArray;
    }
}
