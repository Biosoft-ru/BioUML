package ru.biosoft.access.repository;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.IntrospectionException;
import java.lang.reflect.Method;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.BeanWithAutoProperties;
import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.CustomEditorSupport;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.application.ApplicationUtils;

public class DataElementPathEditor extends CustomEditorSupport implements JSONSerializable
{
    public static final String IS_OUTPUT = "isOutput";
    public static final String IS_INPUT = "isInput";
    private static final String NONE_VALUE = "(select element)";
    public static final String ELEMENT_MUST_EXIST = "elementMustExist";
    public static final String PROMPT_OVERWRITE = "promptOverwrite";
    public static final String ELEMENT_CLASS = "elementClass";
    public static final String MULTI_SELECT = "multiSelect";
    public static final String CHILD_CLASS = "childClass";
    public static final String REFERENCE_TYPE = "referenceType";
    public static final String ICON_ID = "iconId";

    private Class<? extends DataElement> elementClass = null;
    private Class<? extends DataElement> childClass = null;
    private Class<? extends ReferenceType> referenceType = null;
    private boolean canBeNull = false, elementMustExist = false, promptOverwrite = false, multiSelect = false;

    private void init()
    {
        Object elementClassObj = getDescriptor().getValue(ELEMENT_CLASS);
        if( elementClassObj instanceof Class<?> )
            elementClass = (Class<? extends DataElement>)elementClassObj;
        Object childClassObj = getDescriptor().getValue(CHILD_CLASS);
        if( childClassObj instanceof Class<?> )
            childClass = (Class<? extends DataElement>)childClassObj;
        Object referenceTypeObj = getDescriptor().getValue(REFERENCE_TYPE);
        if( referenceTypeObj instanceof Class<?> )
            referenceType = (Class<? extends ReferenceType>)referenceTypeObj;
        canBeNull = BeanUtil.getBooleanValue(this, BeanInfoConstants.CAN_BE_NULL);
        elementMustExist = BeanUtil.getBooleanValue(this, ELEMENT_MUST_EXIST);
        promptOverwrite = BeanUtil.getBooleanValue(this, PROMPT_OVERWRITE);
        multiSelect = BeanUtil.getBooleanValue(this, MULTI_SELECT);
        if(getValue() instanceof DataElementPathSet)
            multiSelect = true;
    }

    private JLabel createLabel()
    {
        String title = NONE_VALUE;
        if( getValue() instanceof DataElementPath && !((DataElementPath)getValue()).isEmpty())
        {
            title = getValue().toString();
        }
        if( getValue() instanceof DataElementPathSet && !((DataElementPathSet)getValue()).isEmpty())
        {
            DataElementPathSet value = (DataElementPathSet)getValue();
            try
            {
                title = value.size() == 0 ? NONE_VALUE : value.size() == 1 ? value.iterator().next().toString() : "[" + value.size() + "] "
                        + BeanUtil.joinBeanProperties(value, "name", ";");
            }
            catch( Exception e )
            {
                throw new InternalException(e);
            }
        }
        return new JLabel(title);
    }

    public DataElementPathDialog getDialog()
    {
        init();
        DataElementPathDialog dialog = new DataElementPathDialog();
        dialog.setElementMustExist(elementMustExist);
        dialog.setPromptOverwrite(promptOverwrite);
        dialog.setMultiSelect(multiSelect);
        dialog.setElementClass(elementClass);
        dialog.setChildClass(childClass);
        dialog.setReferenceType(referenceType);
        dialog.setAlwaysOnTop(true);
        if( getValue() instanceof DataElementPath )
            dialog.setValue(((DataElementPath)getValue()).isEmpty()?(DataElementPath)null:(DataElementPath)getValue());
        else if( getValue() instanceof DataElementPathSet )
            dialog.setValue(((DataElementPathSet)getValue()).isEmpty()?(DataElementPathSet)null:(DataElementPathSet)getValue());
        else
            dialog.setValue((DataElementPath)null);
        return dialog;
    }

    @Override
    public Component getCustomRenderer(final Component parent, boolean isSelected, boolean hasFocus)
    {
        return getCustomEditor(parent, isSelected);
    }

    @Override
    public Component getCustomEditor(Component parent, boolean isSelected)
    {
        init();
        final JLabel label = createLabel();
        label.setTransferHandler(new DataElementImportTransferHandler(new DataElementDroppable()
        {
            @Override
            public boolean doImport(DataElementPath path, Point point)
            {
                if( !path.exists() || !DataCollectionUtils.isAcceptable(path, childClass, elementClass, referenceType) )
                {
                    ApplicationUtils.errorBox("Error", "This element has inacceptable type: " + path.getName());
                    return false;
                }
                if(multiSelect)
                {
                    DataElementPathSet result = new DataElementPathSet();
                    result.add(path);
                    setValue(result);
                } else
                    setValue(path);
                return true;
            }
        }));
        Icon icon = null;
        if(referenceType != null)
        {
            icon = IconFactory.getIconById(IconFactory.getClassIconId(referenceType));
        } else if(elementClass != null)
        {
            icon = IconFactory.getIconById(IconFactory.getClassIconId(elementClass));
        }
        if(icon == null)
        {
            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
            if((elementClass != null && ru.biosoft.access.core.DataCollection.class.isAssignableFrom(elementClass)) || childClass != null)
            {
                icon = renderer.getClosedIcon();
            } else
            {
                icon = renderer.getLeafIcon();
            }
        }
        if(icon != null)
            label.setIcon(icon);
        final JCheckBox checkBox = new JCheckBox();
        MouseListener mouseListener = new MouseAdapter()
        {
            /**
             * Invoked when the mouse has been clicked on a component.
             */
            @Override
            public void mousePressed(MouseEvent e)
            {
                if(e.getSource() == checkBox && checkBox.isSelected()) return;
                DataElementPathDialog dialog = getDialog();
                if( dialog.doModal() )
                {
                    if( multiSelect )
                        setValue(dialog.getValues());
                    else
                        setValue(dialog.getValue());
                    checkBox.setSelected(true);
                    label.setText(createLabel().getText());
                }
            }
        };
        label.addMouseListener(mouseListener);
        if( canBeNull )
        {
            JPanel contentPane = new JPanel(new GridBagLayout());
            contentPane.setBackground(Color.WHITE);
            checkBox.setBackground(Color.WHITE);
            checkBox.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent evt)
                {
                    if( !checkBox.isSelected() )
                    {
                        setValue(null);
                        label.setText(createLabel().getText());
                    }
                }
            });
            checkBox.setSelected(getValue() != null && !getValue().toString().equals(""));
            checkBox.addMouseListener(mouseListener);
            contentPane.add(checkBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            contentPane.add(label, new GridBagConstraints(1, 0, 1, 1, 5.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0), 0, 0));
            contentPane.addMouseListener(mouseListener);
            return contentPane;
        }
        return label;
    }

    /*
     * Handy static methods to register editor with different parameters
     */

    public static PropertyDescriptorEx registerInput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType, boolean canBeNull)
    {
        pde.setValue(ELEMENT_MUST_EXIST, true);
        pde.setValue(ELEMENT_CLASS, wantedType);
        if(referenceType != null)
            pde.setValue(REFERENCE_TYPE, referenceType);
        pde.setValue(IS_INPUT, true);
        pde.setSimple(true);
        pde.setCanBeNull(canBeNull);
        return pde;
    }

    public static PropertyDescriptorEx registerInput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, boolean canBeNull)
    {
        return registerInput(pde, wantedType, null, canBeNull);
    }

    public static PropertyDescriptorEx registerInput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType, Method canBeNull)
    {
        pde.setValue(ELEMENT_MUST_EXIST, true);
        pde.setValue(ELEMENT_CLASS, wantedType);
        if(referenceType != null)
            pde.setValue(REFERENCE_TYPE, referenceType);
        pde.setValue(IS_INPUT, true);
        pde.setSimple(true);
        pde.setCanBeNull(canBeNull);
        return pde;
    }

    public static PropertyDescriptorEx registerInput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Method canBeNull)
    {
        return registerInput(pde, wantedType, null, canBeNull);
    }

    public static PropertyDescriptorEx registerInput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType)
    {
        return registerInput(pde, wantedType, referenceType, false);
    }

    public static PropertyDescriptorEx registerInput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType)
    {
        return registerInput(pde, wantedType, false);
    }

    public static PropertyDescriptorEx registerInput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType,
            boolean canBeNull) throws IntrospectionException
    {
        return registerInput(new PropertyDescriptorEx(property, beanClass), wantedType, canBeNull);
    }

    public static PropertyDescriptorEx registerInput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType,
            boolean canBeNull) throws IntrospectionException
    {
        return registerInput(new PropertyDescriptorEx(property, beanClass), wantedType, referenceType, canBeNull);
    }

    public static PropertyDescriptorEx registerInput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType,
            Method canBeNull) throws IntrospectionException
    {
        return registerInput(new PropertyDescriptorEx(property, beanClass), wantedType, referenceType, canBeNull);
    }

    public static PropertyDescriptorEx registerInput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType)
            throws IntrospectionException
    {
        return registerInput(new PropertyDescriptorEx(property, beanClass), wantedType, referenceType, false);
    }

    public static PropertyDescriptorEx registerInput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType,
            Method canBeNull) throws IntrospectionException
    {
        return registerInput(new PropertyDescriptorEx(property, beanClass), wantedType, canBeNull);
    }

    public static PropertyDescriptorEx registerInput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType)
            throws IntrospectionException
    {
        return registerInput(new PropertyDescriptorEx(property, beanClass), wantedType, false);
    }

    public static PropertyDescriptorEx registerInputMulti(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType,
            boolean canBeNull)
    {
        registerInput(pde, wantedType, canBeNull);
        pde.setValue(MULTI_SELECT, true);
        return pde;
    }

    public static PropertyDescriptorEx registerInputMulti(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType,
            Method canBeNull)
    {
        registerInput(pde, wantedType, canBeNull);
        pde.setValue(MULTI_SELECT, true);
        return pde;
    }

    public static PropertyDescriptorEx registerInputMulti(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType)
    {
        return registerInputMulti(pde, wantedType, false);
    }

    public static PropertyDescriptorEx registerInputMulti(String property, Class<?> beanClass, Class<? extends DataElement> wantedType,
            boolean canBeNull) throws IntrospectionException
    {
        return registerInputMulti(new PropertyDescriptorEx(property, beanClass), wantedType, canBeNull);
    }

    public static PropertyDescriptorEx registerInputMulti(String property, Class<?> beanClass, Class<? extends DataElement> wantedType,
            Method canBeNull) throws IntrospectionException
    {
        return registerInputMulti(new PropertyDescriptorEx(property, beanClass), wantedType, canBeNull);
    }

    public static PropertyDescriptorEx registerInputMulti(String property, Class<?> beanClass, Class<? extends DataElement> wantedType)
            throws IntrospectionException
    {
        return registerInputMulti(new PropertyDescriptorEx(property, beanClass), wantedType, false);
    }

    public static PropertyDescriptorEx registerInputChild(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType,
            boolean canBeNull)
    {
        pde.setValue(ELEMENT_MUST_EXIST, true);
        if( wantedType != null )
            pde.setValue(CHILD_CLASS, wantedType);
        pde.setValue(IS_INPUT, true);
        pde.setSimple(true);
        pde.setCanBeNull(canBeNull);
        return pde;
    }

    public static PropertyDescriptorEx registerInputChild(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType)
    {
        return registerInputChild(pde, wantedType, false);
    }

    public static PropertyDescriptorEx registerInputChild(String property, Class<?> beanClass, Class<? extends DataElement> wantedType,
            boolean canBeNull) throws IntrospectionException
    {
        return registerInputChild(new PropertyDescriptorEx(property, beanClass), wantedType, canBeNull);
    }

    public static PropertyDescriptorEx registerInputChild(String property, Class<?> beanClass, Class<? extends DataElement> wantedType)
            throws IntrospectionException
    {
        return registerInputChild(new PropertyDescriptorEx(property, beanClass), wantedType, false);
    }

    public static PropertyDescriptorEx registerOutput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType, boolean canBeNull)
    {
        if( wantedType != null )
            pde.setValue(ELEMENT_CLASS, wantedType);
        if(referenceType != null)
            pde.setValue(REFERENCE_TYPE, referenceType);
        pde.setValue(IS_OUTPUT, true);
        pde.setSimple(true);
        pde.setCanBeNull(canBeNull);
        return pde;
    }

    public static PropertyDescriptorEx registerOutput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, boolean canBeNull)
    {
        return registerOutput(pde, wantedType, null, canBeNull);
    }

    public static PropertyDescriptorEx registerOutput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType, Method canBeNull)
    {
        if( wantedType != null )
            pde.setValue(ELEMENT_CLASS, wantedType);
        if(referenceType != null)
            pde.setValue(REFERENCE_TYPE, referenceType);
        pde.setValue(IS_OUTPUT, true);
        pde.setSimple(true);
        pde.setCanBeNull(canBeNull);
        return pde;
    }

    public static PropertyDescriptorEx registerOutput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Method canBeNull)
    {
        return registerOutput(pde, wantedType, null, canBeNull);
    }

    public static PropertyDescriptorEx registerOutput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType)
    {
        return registerOutput(pde, wantedType, referenceType, false);
    }

    public static PropertyDescriptorEx registerOutput(PropertyDescriptorEx pde, Class<? extends DataElement> wantedType)
    {
        return registerOutput(pde, wantedType, false);
    }

    public static PropertyDescriptorEx registerOutput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType,
            boolean canBeNull) throws IntrospectionException
    {
        return registerOutput(new PropertyDescriptorEx(property, beanClass), wantedType, canBeNull);
    }

    public static PropertyDescriptorEx registerOutput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType,
            boolean canBeNull) throws IntrospectionException
    {
        return registerOutput(new PropertyDescriptorEx(property, beanClass), wantedType, referenceType, canBeNull);
    }

    public static PropertyDescriptorEx registerOutput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType,
            Method canBeNull) throws IntrospectionException
    {
        return registerOutput(new PropertyDescriptorEx(property, beanClass), wantedType, canBeNull);
    }

    public static PropertyDescriptorEx registerOutput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType,
            Method canBeNull) throws IntrospectionException
    {
        return registerOutput(new PropertyDescriptorEx(property, beanClass), wantedType, referenceType, canBeNull);
    }

    public static PropertyDescriptorEx registerOutput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType)
            throws IntrospectionException
    {
        return registerOutput(new PropertyDescriptorEx(property, beanClass), wantedType, false);
    }

    public static PropertyDescriptorEx registerOutput(String property, Class<?> beanClass, Class<? extends DataElement> wantedType, Class<? extends ReferenceType> referenceType)
            throws IntrospectionException
    {
        return registerOutput(new PropertyDescriptorEx(property, beanClass), wantedType, referenceType, false);
    }

    public String getIconId()
    {
        String iconId = BeanUtil.getStringValue(this, ICON_ID);
        if(iconId != null) return iconId;
        if( elementMustExist && !multiSelect && getValue() != null )
        {
            try
            {
                iconId = IconFactory.getIconId((DataElementPath)getValue());
            }
            catch( Exception e )
            {
            }
        }
        if(iconId != null) return iconId;
        if(referenceType != null) return IconFactory.getClassIconId(referenceType);
        Class<? extends DataElement> elementClass = ru.biosoft.access.core.DataElement.class;
        if(this.elementClass != null) elementClass = this.elementClass;
        else if(this.childClass != null) elementClass = ru.biosoft.access.core.DataCollection.class;
        return IconFactory.getClassIconId(elementClass);
    }

    /**
     * Returns ID of the icon which will represent given property in the best way (assuming that property's class is ru.biosoft.access.core.DataElementPath)
     * @param property property to get icon for
     * @return
     */
    public static String getIconId(Property property)
    {
        if(property == null || !property.getPropertyEditorClass().equals(DataElementPathEditor.class)) return null;
        DataElementPathEditor editor = new DataElementPathEditor();
        editor.setDescriptor(property.getDescriptor());
        editor.setBean(property.getOwner());
        editor.init();
        return editor.getIconId();
    }

    /*
     * JSONSerializable interface implementation
     */

    @Override
    public void fromJSON(JSONObject input) throws JSONException
    {
        init();
        if( multiSelect )
        {
            JSONArray vals = input.getJSONArray("value");
            DataElementPathSet value = new DataElementPathSet();
            for( int i = 0; i < vals.length(); i++ )
            {
                DataElementPath path = DataElementPath.create(vals.getString(i));
                if( elementMustExist )
                {
                    if( !path.exists() || !DataCollectionUtils.isAcceptable(path, childClass, elementClass, referenceType) )
                        continue;
                }
                else
                {
                    if( path.optParentCollection() == null || !DataCollectionUtils.isAcceptable(path.getParentPath(), elementClass, null) )
                        continue;
                }
                value.add(path);
            }
            setValue(value);
        }
        else
        {
            String pathStr = input.getString("value");
            if( pathStr == null || pathStr.isEmpty() )
            {
                setValue(null);
                return;
            }
            DataElementPath path = DataElementPath.create(pathStr);
            setValue(path);
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        init();
        JSONObject result = new JSONObject();
        Object value = getValue();
        if( multiSelect )
        {
            JSONArray vals = new JSONArray();
            if( value != null )
            {
                for( DataElementPath path : (DataElementPathSet)value )
                {
                    vals.put(path.toString());
                }
            }
            result.put("value", vals);
        }
        else
        {
            if( value instanceof DataElementPath )
            {
                result.put("value", value.toString());
            }
            else
            {
                result.put("value", NONE_VALUE);
            }
        }
        if( childClass != null )
            result.put(CHILD_CLASS, childClass.getName());
        if( elementClass != null )
            result.put(ELEMENT_CLASS, elementClass.getName());
        if( referenceType != null )
            result.put(REFERENCE_TYPE, ReferenceTypeRegistry.getReferenceType(referenceType).getDisplayName());
        result.put("canBeNull", canBeNull);
        result.put(ELEMENT_MUST_EXIST, elementMustExist);
        result.put(PROMPT_OVERWRITE, promptOverwrite);
        result.put(MULTI_SELECT, multiSelect);
        if(getBean() instanceof BeanWithAutoProperties)
        {
            switch(((BeanWithAutoProperties)getBean()).getAutoPropertyStatus(getDescriptor().getName()))
            {
                case AUTO_MODE_ON:
                    result.put("auto", "on");
                    break;
                case AUTO_MODE_OFF:
                    result.put("auto", "off");
                    break;
            }
        }
        String iconId = getIconId();
        if(iconId != null) result.put("icon", iconId);
        result.put("type", "data-element-path");
        return result;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        if(!multiSelect)
            setValue(DataElementPath.create(text));
        else
            setValue(new DataElementPathSet(text));
    }
}
