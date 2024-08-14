
package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.awt.Component;
import java.beans.BeanDescriptor;
import java.beans.DefaultPersistenceDelegate;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.graphics.Brush;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.util.FieldMap;
import ru.biosoft.util.JSONCompatibleEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.IndexedPropertyDescriptorEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.ColorComboBox;
import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;

/**
 * This BeanInfo has really sofisticated settings.
 *
 * <p>The main trick is following: if KeyColorGroup has childs,
 * then bean will be replaced by groups property,
 * and for group property we set up its name as "name" property, and its editor
 * is really used to edit "brush" property;
 * otherwise, only bean will be shown, its name controoled by "getName" method
 * and compositeEditor is used to edit its "brush" property.
 */
public class KeyColorGroupBeanInfo extends BeanInfoEx
{
    public KeyColorGroupBeanInfo()
    {
        super(KeyColorGroup.class, MessageBundle.class.getName() );
        beanDescriptor.setShortDescription(getResourceString("CD_KEY_COLOR_GROUP"));
        setCompositeEditor("brush", new java.awt.GridLayout(1, 1));
        setNoRecursionCheck(true);
        setSubstituteByChild(true);
    }

    @Override
    protected void initProperties() throws Exception
    {
        setDisplayNameMethod(beanClass.getMethod("getName"));
        
        addHidden(new PropertyDescriptorEx("name", beanClass, "getName", null));
        addHidden(new PropertyDescriptorEx("brush", beanClass));

        IndexedPropertyDescriptorEx ipde = new IndexedPropertyDescriptorEx("groups", beanClass);
        ipde.setHidden(beanClass.getMethod("isGroupArrayEmpty"));
        ipde.setDisplayName(beanClass.getMethod("getName"));
        ipde.setPropertyEditorClass(ColorGroupBrushEditor.class);
        add(ipde, getResourceString("PN_GROUPS"), getResourceString("PD_GROUPS"));
    }

    @Override
    public BeanDescriptor getBeanDescriptor()
    {
        BeanDescriptor descriptor = super.getBeanDescriptor();
        try
        {
            descriptor.setValue("persistenceDelegate",
                new DefaultPersistenceDelegate(
                new String[]
                {
                    "name", "brush"
                })
                {
                    @Override
                    protected boolean mutatesTo(Object oldInstance, Object newInstance)
                    {
                        return (newInstance != null &&
                        oldInstance.getClass() == newInstance.getClass());
                    }
                });
        }
        catch (Exception ex)
        {
            logError("Error during setting PersistenceDelegate", ex);
        }
        return descriptor;
    }

    ////////////////////////////////////////
    // Color group brush editor
    //

    public static class ColorGroupBrushEditor extends CustomEditorSupport implements JSONCompatibleEditor
    {
        public Color getBrush()
        {
            KeyColorGroup group = (KeyColorGroup)getBean();
            return (Color)group.getBrush().getPaint();
        }

        public void setBrush(Color brush)
        {
            brush = new Color(brush.getRed(),brush.getGreen(),brush.getBlue()); // Special for serialise
            KeyColorGroup group = (KeyColorGroup)getBean();
            group.setBrush(new ru.biosoft.graphics.Brush(brush));
        }

        @Override
        public Component getCustomRenderer(Component parent, boolean isSelected, boolean hasFocus)
        {
            return ColorComboBox.getValueRenderer(getBrush());
        }

        @Override
        public Component getCustomEditor()
        {
            ColorComboBox comboBox = ColorComboBox.getInstance(getBrush());
            comboBox.addColorPropertyChangeListener(
                new PropertyChangeListener()
                {
                    @Override
                    public void propertyChange( PropertyChangeEvent evt )
                    {
                        setBrush((Color)evt.getNewValue());
                        setValue(getValue());
                    }
                } );

            return comboBox;
        }

        /** This method should return Object that is result of editing in custom editor. */
        @Override
        protected Object processValue()
        {
            return null;
        }
        
        @Override
        public void fillWithJSON(Property property, JSONObject jsonObject) throws Exception
        {
            Object owner = property.getOwner();
            setValue(property.getValue());
            setBean(owner);

            Object oldValue = property.getValue();
            Object[] oldArray = (Object[])oldValue;
            Color oldColor = getBrush();
            JSONArray jsonArray = jsonObject.getJSONArray("value");
            Color newColor = JSONUtils.parseColor(jsonArray.getString(0));
            if( oldColor.equals(newColor) )
            {
                int index = 0;
                for( Object oldObject : oldArray )
                {
                    CompositeProperty elementModel = null;
                    if( oldObject instanceof CompositeProperty )
                    {
                        elementModel = (CompositeProperty)oldObject;
                    }
                    else
                    {
                        elementModel = ComponentFactory.getModel(oldObject, Policy.DEFAULT, true);
                    }
                    JSONArray jsonBean = jsonArray.getJSONArray(index + 1);
                    JSONUtils.correctBeanOptions(elementModel, jsonBean);
                    index++;
                }
            }
            else
            {
                setBrush(newColor);
            }
        }
        
        @Override
        public void addAsJSON(Property property, JSONObject p, FieldMap fieldMap, int showMode) throws Exception
        {
            Color color = getBrush();
            JSONArray value = new JSONArray();

            JSONArray colorjs = new JSONArray();
            colorjs.put(color.getRed());
            colorjs.put(color.getGreen());
            colorjs.put(color.getBlue());

            value.put(colorjs.toString());

            ArrayProperty array = (ArrayProperty)property;
            for( int j = 0; j < array.getPropertyCount(); j++ )
            {
                Property element = array.getPropertyAt(j);
                if( element instanceof CompositeProperty )
                {
                    JSONArray elementModel = JSONUtils.getModelAsJSON((CompositeProperty)element,
                            fieldMap.get(property.getName()), showMode);
                    /*
                     * We can not get model for "leaf" elements of KeyColorGroup in normal way,
                     * so process them manually
                     */
                    if( elementModel.length() == 0 )
                    {
                        Class cEl = property.getPropertyEditorClass();
                        if( cEl != null )
                        {
                            if( CustomEditorSupport.class.isAssignableFrom(cEl) )
                            {
                                CustomEditorSupport editorEl = (CustomEditorSupport)cEl.newInstance();
                                if( editorEl instanceof ColorGroupBrushEditor )
                                {
                                    JSONObject pCh = new JSONObject();
                                    pCh.put(JSONUtils.NAME_ATTR, property.getName());
                                    pCh.put(JSONUtils.DISPLAYNAME_ATTR, element.getDisplayName());
                                    pCh.put(JSONUtils.TYPE_ATTR, "color-selector");
                                    Color colorEl = null;

                                    for( int k = 0; k < element.getPropertyCount(); k++ )
                                    {
                                        Property propertyEl = element.getPropertyAt(k);
                                        Object valueEl = propertyEl.getValue();
                                        if( valueEl instanceof Brush )
                                        {
                                            colorEl = (Color) ( (Brush)valueEl ).getPaint();
                                            continue;
                                        }
                                    }
                                    if( colorEl == null )
                                    {
                                        //set parent's color if not found
                                        colorEl = getBrush();
                                    }
                                    JSONArray colorjsEl = new JSONArray();
                                    colorjsEl.put(colorEl.getRed());
                                    colorjsEl.put(colorEl.getGreen());
                                    colorjsEl.put(colorEl.getBlue());
                                    JSONArray valueEl = new JSONArray();
                                    valueEl.put(colorjsEl.toString());
                                    pCh.put(JSONUtils.VALUE_ATTR, valueEl);
                                    elementModel.put(pCh);
                                    value.put(elementModel);
                                }
                            }
                        }
                    }
                    else
                    {
                        value.put(elementModel);
                    }
                }
            }
            p.put(JSONUtils.TYPE_ATTR, "color-selector");
            p.put(JSONUtils.VALUE_ATTR, value);
        }
    }
}
