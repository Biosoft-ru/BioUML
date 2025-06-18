package ru.biosoft.galaxy;

import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.support.SerializableAsText;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.galaxy.parameters.ArrayParameter;
import ru.biosoft.galaxy.parameters.BaseFileParameter;
import ru.biosoft.galaxy.parameters.BooleanParameter;
import ru.biosoft.galaxy.parameters.ConditionalParameter;
import ru.biosoft.galaxy.parameters.ConfigParameter;
import ru.biosoft.galaxy.parameters.DataColumnParameter;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.FloatParameter;
import ru.biosoft.galaxy.parameters.MultiFileParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.galaxy.parameters.SelectParameter;
import ru.biosoft.galaxy.parameters.StringParameter;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.Pair;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.TransformedIterator;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertyBuilder;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;

/**
 * Galaxy analysis parameters.
 * Extends {@link DynamicPropertySet} to provide dynamic bean structure.
 */
public class GalaxyAnalysisParameters extends AbstractAnalysisParameters implements DynamicPropertySet
{
    private static final long serialVersionUID = 1L;

    protected static final Logger log = Logger.getLogger(GalaxyAnalysisParameters.class.getName());

    protected ParametersContainer container;

    protected LinkedHashMap<String, DynamicProperty> parameters;
    
    protected String analysisName;
    
    protected OutputFilter outputFilter;

    public static final String GALAXY_PARAMETER_PROPERTY = "galaxyParameter";
    public static final String TABLE_PROPERTY = "table";
    public static final String NUMERICAL_PROPERTY = "numerical";
    
    public static final String NESTED_PARAMETER_DELIMETER = "|";

    public GalaxyAnalysisParameters(GalaxyMethodInfo methodInfo)
    {
        super(true);
        this.container = methodInfo.getParameters().clone();
        this.analysisName = methodInfo.getDisplayName();
        outputFilter = new OutputFilter(container);
        initParameters();
        initAutoProperties();
    }

    protected Map<String, DynamicProperty> getParameters()
    {
        if( parameters == null )
            parameters = new LinkedHashMap<>();
        return parameters;
    }

    public ParametersContainer getParametersContainer()
    {
        return container;
    }

    protected void initParameters()
    {
        try
        {
            Set<String> outputTemplates = new HashSet<>();
            String inputParameterName = null;
            for( Map.Entry<String, Parameter> entry : container.entrySet() )
            {
                String name = entry.getKey();
                Parameter parameter = entry.getValue();
                if( !parameter.isOutput() )
                {
                    for( DynamicProperty dp : getDynamicPropertiesForInputParameter(name, "", parameter) )
                        add(dp);

                    if( inputParameterName == null )
                    {
                        if( parameter instanceof FileParameter )
                            inputParameterName = name;
                        else if( parameter instanceof MultiFileParameter )
                            inputParameterName = name + "/path";
                    }
                }
            }

            for( Map.Entry<String, Parameter> entry : container.entrySet() )
            {
                Parameter parameter = entry.getValue();
                if( parameter.isOutput() )
                {
                    GalaxyParameter property = new GalaxyParameter(entry.getKey(), DataElementPath.class, parameter);
                    property.getDescriptor().setValue("isOutput", true);

                    String outputSuffix = "out";
                    Object formatObj = parameter.getAttributes().get("format");
                    if( formatObj != null )
                    {
                        Class<? extends DataElement> clazz = FormatRegistry.getClass(formatObj.toString());
                        PropertyName propertyName = clazz.getAnnotation(PropertyName.class);
                        if(propertyName != null) outputSuffix = propertyName.value();
                        property.getDescriptor().setValue(DataElementPathEditor.ELEMENT_CLASS,
                                clazz);
                    }
                    if( property.getDescriptor().getValue( OptionEx.TEMPLATE_PROPERTY ) == null && inputParameterName != null )
                    {
                        String baseTemplate = "$" + inputParameterName + "$ "+outputSuffix;
                        String template = baseTemplate;
                        int nOutput = 0;
                        while(outputTemplates.contains(template))
                        {
                            template = baseTemplate+" "+(++nOutput);
                        }
                        outputTemplates.add(template);
                        OptionEx.makeAutoProperty(property.getDescriptor(), template);
                    }
                    add(property);
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot initialize analysis parameters", e);
        }
    }

    protected List<DynamicProperty> getDynamicPropertiesForInputParameter(String name, String prefix, Parameter parameter)
            throws IntrospectionException
    {
        List<DynamicProperty> result = new ArrayList<>();
        if( parameter instanceof ConditionalParameter )
            for( DynamicProperty dp : getConditionalParameterObjects(name, prefix, (ConditionalParameter)parameter) )
                result.add(dp);
        else if( parameter instanceof ArrayParameter )
            result.add(getArrayParameterObjects(name, prefix, (ArrayParameter)parameter));
        else
            result.add(getInputParameterObject(name, prefix, parameter));
        return result;
    }
    
    //
    // Parameter to DynamicProperty conversion
    //

    protected List<DynamicProperty> getConditionalParameterObjects(String condName, String prefix, ConditionalParameter cp)
            throws IntrospectionException
    {
        List<DynamicProperty> result = new ArrayList<>();

        String whenName = cp.getKeyParameterName();
        DynamicProperty whenProperty = getInputParameterObject(whenName, prefix + condName + NESTED_PARAMETER_DELIMETER, cp.getKeyParameter());
        result.add(whenProperty);

        fillParameterValue(cp.getKeyParameter(), whenProperty.getValue());
        for( String key : cp.getWhenSet() )
        {
            Map<String, Parameter> params = cp.getWhenParameters(key);
            for( Map.Entry<String, Parameter> entry : params.entrySet() )
            {
                String newPrefix = prefix + condName + NESTED_PARAMETER_DELIMETER + key + NESTED_PARAMETER_DELIMETER;
                result.addAll(getDynamicPropertiesForInputParameter(entry.getKey(), newPrefix, entry.getValue()));
            }
        }

        return result;
    }

    protected DynamicProperty getArrayParameterObjects(String name, String prefix, ArrayParameter parameter) throws IntrospectionException
    {
        String pName = prefix + name;
        DynamicProperty result = new GalaxyParameter(pName, DynamicPropertySet[].class, parameter);

        ArrayItem prototype = new ArrayItem(pName, this);
        Map<String, Parameter> childs = parameter.getChildTypes();
        for( Map.Entry<String, Parameter> entry : childs.entrySet() )
        {
            for( DynamicProperty dp : getDynamicPropertiesForInputParameter(entry.getKey(), "", entry.getValue()) )
                prototype.add(dp);
        }
        result.setAttribute("item-prototype", prototype);

        return result;
    }
    
    protected DynamicProperty getInputParameterObject(String name, String prefix, Parameter parameter) throws IntrospectionException
    {
        String pName = prefix + name;
        Object type = parameter.getAttributes().get("type");
        GalaxyParameter result = null;
        if( type != null )
        {
            if( type.equals("data") )
            {
                result = new GalaxyParameter(pName, DataElementPath.class, parameter);
                result.getDescriptor().setValue("isInput", true);
                Object formatObj = parameter.getAttributes().get("format");
                if( formatObj != null )
                    result.getDescriptor().setValue(DataElementPathEditor.ELEMENT_CLASS, FormatRegistry.getClass(formatObj.toString()));
                result.getDescriptor().setValue(DataElementPathEditor.ELEMENT_MUST_EXIST, true);
            }
            else if( type.equals("data-multi") )
            {
                result = new GalaxyParameter(pName, DataElementPathSet.class, parameter);
                result.getDescriptor().setValue("isInput", true);
                Object formatObj = parameter.getAttributes().get("format");
                if( formatObj != null )
                    result.getDescriptor().setValue(DataElementPathEditor.ELEMENT_CLASS, FormatRegistry.getClass(formatObj.toString()));
                result.getDescriptor().setValue(DataElementPathEditor.ELEMENT_MUST_EXIST, true);
                result.getDescriptor().setValue(DataElementPathEditor.MULTI_SELECT, true);
            }
            else if( type.equals("integer") )
            {
                result = new GalaxyParameter(pName, Integer.class, parameter);
                Object value = parameter.getAttributes().get("value");
                if( value != null )
                {
                    try
                    {
                        result.setValue(Integer.parseInt(value.toString()));
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
            }
            else if( type.equals("text") || type.equals("hidden") )
            {
                result = new GalaxyParameter(pName, String.class, parameter);
                Object value = parameter.getAttributes().get("value");
                if( value != null )
                {
                    result.setValue(value.toString());
                }
            }
            else if( type.equals("float") )
            {
                result = new GalaxyParameter(pName, Float.class, parameter);
                Object value = parameter.getAttributes().get("value");
                if( value != null )
                {
                    try
                    {
                        result.setValue(Float.parseFloat(value.toString()));
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
            }
            else if( type.equals("select") || type.equals("drill_down") )
            {
                Object multiple = parameter.getAttributes().get("multiple");
                Map<String, String> availableValues = ( (SelectParameter)parameter ).getOptions();
                String valueStr = parameter.toString();
                if( multiple != null && (Boolean)multiple )
                {
                    SelectorOption[] selectorOptions = StreamEx.split(valueStr, ',').remove( TextUtil2::isEmpty )
                            .mapToEntry( availableValues::get ).mapKeyValue( SelectorOption::new ).toArray( SelectorOption[]::new );
                    result = new GalaxyParameter(pName, SelectorOption.class, parameter);
                    result.setValue(selectorOptions);
                    result.setSimple(true);
                    result.getDescriptor().setValue(GALAXY_PARAMETER_PROPERTY, parameter);
                    result.getDescriptor().setPropertyEditorClass(GalaxyOptionMultiSelector.class);
                }
                else
                {
                    String value = valueStr;
                    String displayName = null;
                    if( availableValues.containsKey(value) )
                    {
                        displayName = availableValues.get(value);
                    }
                    else if( availableValues.size() > 0 )
                    {
                        Entry<String, String> entry = availableValues.entrySet().iterator().next();
                        value = entry.getKey();
                        displayName = entry.getValue();
                    }
                    SelectorOption selectorOption = displayName != null ? new SelectorOption(value, displayName) : new SelectorOption("", "");
                    result = new GalaxyParameter(pName, SelectorOption.class, parameter);
                    result.setValue(selectorOption);
                    result.setSimple(true);
                    result.getDescriptor().setValue(GALAXY_PARAMETER_PROPERTY, parameter);
                    result.getDescriptor().setPropertyEditorClass(GalaxyOptionSelector.class);
                }
            }
            else if( type.equals("boolean") )
            {
                result = new GalaxyParameter(pName, Boolean.class, parameter);
                result.setValue( ( (BooleanParameter)parameter ).getValue());
            }
            else if( type.equals("data_column") )
            {
                Object multiple = parameter.getAttributes().get("multiple");
                if( multiple instanceof Boolean && ( (Boolean)multiple ) == true )
                {
                    // TODO: support initial value somehow
                    result = new GalaxyParameter(pName, SelectorOption.class, parameter);
                    result.setValue(new SelectorOption[0]);
                    result.setSimple(true);
                    GalaxyTableColumnMultiSelector.registerSelector(result.getDescriptor(), parameter.getAttributes().get("dataRef")
                            .toString(), ( (DataColumnParameter)parameter ).isNumerical());
                }
                else
                {
                    // TODO: support initial value somehow
                    result = new GalaxyParameter(pName, SelectorOption.class, parameter);
                    result.setValue(new SelectorOption("", ""));
                    result.setSimple(true);
                    GalaxyTableColumnSelector.registerSelector(result.getDescriptor(), parameter.getAttributes().get("dataRef").toString(),
                            ( (DataColumnParameter)parameter ).isNumerical());
                }
            }
            else
                log.warning("Unknown parameter type (" + type + ")");
        }
        if( result == null )
        {
            result = new GalaxyParameter(pName, String.class, parameter);
        }
        return result;
    }

    protected static void fillParameterValue(Parameter p, Object newValue)
    {
        if( p instanceof SelectParameter )
        {
            if( newValue.getClass().isArray() )
            {
                SelectorOption[] values = (SelectorOption[])newValue;
                p.setValue( StreamEx.of( values ).map( SelectorOption::getName ).joining( "," ) );
            }
            else
            {
                if( newValue instanceof SelectorOption )
                {
                    p.setValue( ( (SelectorOption)newValue ).getName());
                }
            }
        }
        else if( p instanceof FileParameter )
        {
            ( (FileParameter)p ).setValue((DataElementPath)newValue);
        }
        else if( p instanceof MultiFileParameter )
        {
            ( (MultiFileParameter)p ).setValue((DataElementPathSet)newValue);
        }
        else if( p instanceof StringParameter || p instanceof BooleanParameter || p instanceof FloatParameter )
        {
            String strValue = newValue == null ? "" : newValue.toString();
            p.setValue(strValue);
        }
        else if( p instanceof ArrayParameter )
        {
            ArrayParameter ap = (ArrayParameter)p;
            DynamicPropertySet[] values = (DynamicPropertySet[])newValue;
            ap.setEntriesCount(values.length);
            for( int i = 0; i < values.length; i++ )
            {
                ParametersContainer container = ap.getValues().get(i);

                for(DynamicProperty dp : values[i])
                {
                    Parameter parameter = container.getParameterByPath(dp.getName());
                    ((GalaxyParameter)dp).getDescriptor().setParameter(parameter);
                    fillParameterValue(parameter, dp.getValue());
                }
            }
        }
    }

    protected static String convertSelectValue(Parameter selectProperty, String value)
    {
        Object selectList = selectProperty.getAttributes().get("options");
        if( selectList instanceof List )
        {
            for( Pair<String, String> pair : (List<Pair<String, String>>)selectList )
            {
                if( pair.getSecond().equals(value) )
                {
                    return pair.getFirst();
                }
            }
        }
        return null;
    }
    
    private static boolean exportParameter(GalaxyParameter property)
    {
        Parameter p = property.getParameter();
        if( p instanceof BaseFileParameter )
        {
            if(p.isOutput() || property.isHidden())
                return true;
            if(  !( (BaseFileParameter)p ).exportFile() ) {
                log.warning("Unable to export parameter '" + property.getDisplayName() + "'");
                return false;
            }
        } else if( p instanceof ArrayParameter )
        {
            DynamicPropertySet[] values = (DynamicPropertySet[])property.getValue();
            if(values == null)
                return true;
            boolean ok = true;
            for( DynamicPropertySet item :  values )
                for(DynamicProperty child : item)
                    if(child instanceof GalaxyParameter)
                        ok &= exportParameter((GalaxyParameter)child);
            return ok;
        }
        return true;
    }
    
    public boolean exportParameters()
    {
        boolean ok = true;
        for(DynamicProperty dp : this)
            if(dp instanceof GalaxyParameter)
                ok &= exportParameter((GalaxyParameter)dp);
        return ok;
    }

    @Override
    public Class<?> getType(String name)
    {
        try
        {
            return getParameters().get(name).getType();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public Object getValue(String name)
    {
        try
        {
            return getParameters().get(name).getValue();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public String getValueAsString( String name )
    {
        Object val = getValue( name );
        if( val == null )
            return null;
        return val.toString();
    }

    @Override
    public void setValue(String name, Object value)
    {
        try
        {
            getParameters().get(name).setValue(value);
        }
        catch( Exception e )
        {
        }
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String name)
    {
        try
        {
            return getParameters().get(name).getDescriptor();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @Override
    public DynamicProperty getProperty(String name)
    {
        return getParameters().get(name);
    }

    public GalaxyParameter findPropertyBySimpleName(String simpleName)
    {
        for( DynamicProperty property : getParameters().values() )
        {
            if( property instanceof GalaxyParameter && !property.isHidden() && property.getName().equals(simpleName)
                    || property.getName().endsWith(NESTED_PARAMETER_DELIMETER + simpleName) )
                return (GalaxyParameter)property;
        }
        return null;
    }

    @Override
    public void renameProperty(String from, String to)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(DynamicProperty property)
    {
        getParameters().put(property.getName(), property);
        property.setParent(this);
        if(property instanceof GalaxyParameter)
        {
            Parameter parameter = ((GalaxyParameter)property).getParameter();
            if(parameter instanceof SelectParameter)
            {
                for(final String dependency : ( (SelectParameter)parameter ).getDependencies())
                {
                    addPropertyChangeListener( (PropertyChangeListener)evt -> {
                        if( evt.getPropertyName().endsWith( "|" + dependency ) )
                            firePropertyChange( "*", null, null );
                    } );
                }
                
            }
        }
    }

    @Override
    public boolean addBefore(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAfter(String propName, DynamicProperty property)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(DynamicProperty property)
    {
        return getParameters().containsValue(property);
    }

    @Override
    public Object remove(String name)
    {
        if( !getParameters().containsKey(name) )
            return null;
        return getParameters().remove(name);
    }

    @Override
    public boolean moveTo(String name, int index)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replaceWith(String name, DynamicProperty prop)
    {
        if( remove(name) != null)
        {
            add(prop);
            return true;
        }

        return false;
    }

    @Override
    public Iterator<String> nameIterator()
    {
        return getParameters().keySet().iterator();
    }

    @Override
    public Iterator<DynamicProperty> propertyIterator()
    {
        return getParameters().values().iterator();
    }

    @Override
    public Iterator<DynamicProperty> iterator()
    {
        return getParameters().values().iterator();
    }

    @Override
    public Map<String, Object> asMap()
    {
        return new AbstractMap<String, Object>()
        {
            @Override
            public Set<Entry<String, Object>> entrySet()
            {
                return new AbstractSet<Entry<String, Object>>()
                {
                    @Override
                    public Iterator<Entry<String, Object>> iterator()
                    {
                        return new TransformedIterator<String, Entry<String, Object>>(getParameters().keySet().iterator())
                        {
                            @Override
                            protected Entry<String, Object> transform(final String next)
                            {
                                return new Entry<String, Object>()
                                {
                                    @Override
                                    public Object setValue(Object value)
                                    {
                                        Object oldValue = getValue();
                                        getParameters().get(next).setValue(value);
                                        return oldValue;
                                    }

                                    @Override
                                    public Object getValue()
                                    {
                                        return getParameters().get(next).getValue();
                                    }

                                    @Override
                                    public String getKey()
                                    {
                                        return next;
                                    }
                                };
                            }
                        };
                    }

                    @Override
                    public int size()
                    {
                        return getParameters().size();
                    }
                };
            }

            @Override
            public int size()
            {
                return getParameters().size();
            }

            @Override
            public boolean containsKey(Object key)
            {
                return getParameters().containsKey(key);
            }

            @Override
            public Object get(Object key)
            {
                return getValue(key.toString());
            }
        };
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        // TODO Use propertyName
        addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        // TODO Use propertyName
        removePropertyChangeListener(listener);
    }

    @Override
    public boolean hasListeners(String propertyName)
    {
        // TODO Use propertyName
        return listenerList != null && listenerList.getListenerCount() > 0;
    }

    @Override
    public int size()
    {
        return getParameters().size();
    }

    @Override
    public boolean isEmpty()
    {
        return getParameters().isEmpty();
    }

    @Override
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        super.firePropertyChange(evt);
    }

    @Override
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }



    protected static SelectorOption[] getTableColumns(DataElementPath tablePath, boolean numerical)
    {
        DataElement dataElement = tablePath.optDataElement();
        if( dataElement instanceof TableDataCollection )
        {
            TableDataCollection table = (TableDataCollection)dataElement;
            boolean ids = !table.getInfo().getProperties().getProperty(TableDataCollection.GENERATED_IDS, "false").equals("true");
            List<SelectorOption> result = new ArrayList<>();
            if( ids && !numerical )
                result.add(new SelectorOption("c1", "ID"));
            ColumnModel columnModel = table.getColumnModel();
            for( int i = 0; i < columnModel.getColumnCount(); i++ )
            {
                TableColumn columnInfo = columnModel.getColumn(i);
                if( !numerical || columnInfo.getType().isNumeric() )
                    result.add(new SelectorOption("c" + ( ids ? i + 2 : i + 1 ), columnInfo.getName()));
            }
            return result.toArray(new SelectorOption[result.size()]);
        }
        return new SelectorOption[0];
    }

    protected static SelectorOption[] getSelectorOptions(SelectParameter parameter)
    {
        Map<String, String> options = parameter.getOptions();
        SelectorOption[] result = new SelectorOption[options.size()];
        int i = 0;
        for( Entry<String, String> entry : options.entrySet() )
        {
            result[i++] = new SelectorOption(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    public void setPropertyAttribute( String propName, String attrName, Object attrValue )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return getInputNames(this);
    }
    
    private static String[] getInputNames(DynamicPropertySet dps)
    {
        List<String> result = new ArrayList<>();
        for( DynamicProperty property: dps )
        {
            if(property.getType().equals(DynamicPropertySet[].class))
            {
                DynamicPropertySet[] arrayItems = (DynamicPropertySet[])property.getValue();
                if(arrayItems == null)
                    continue;
                for(int i = 0; i < arrayItems.length; i++)
                    for(String innerName : getInputNames(arrayItems[i]))
                        result.add(property.getName() + "/[" + i + "]/" + innerName);
            }
            if( property.getBooleanAttribute("isInput") && !property.isHidden() )
                result.add(property.getName());
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Default implementation gets all properties which has "isOutput" descriptor value set
     * Typically it's all the properties created by DataElementPathEditor.registerOutput
     * Override this if you need more complex behavior
     */
    @Override
    public @Nonnull String[] getOutputNames()
    {
        List<String> result = new ArrayList<>();
        for( DynamicProperty property: this )
        {
            if( !property.isHidden() && property.getBooleanAttribute("isOutput") )
                result.add(property.getName());
        }
        return result.toArray(new String[result.size()]);
    }

    public static class GalaxyTableColumnSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            try
            {
                Object bean = getBean();
                if( bean instanceof ArrayItem )
                    bean = ( (ArrayItem)bean ).getParent();
                return getTableColumns(
                        (DataElementPath)ComponentFactory.getModel(bean, Policy.DEFAULT, true)
                                .findProperty(getDescriptor().getValue(TABLE_PROPERTY).toString()).getValue(),
                        getDescriptor().getValue(NUMERICAL_PROPERTY).equals("true"));
            }
            catch( Exception e )
            {
                return new SelectorOption[0];
            }
        }

        public static void registerSelector(PropertyDescriptor pde, String tableProperty, boolean numerical)
        {
            pde.setValue(TABLE_PROPERTY, tableProperty);
            pde.setValue(NUMERICAL_PROPERTY, String.valueOf(numerical));
            pde.setPropertyEditorClass(GalaxyTableColumnSelector.class);
            pde.setValue(BeanInfoConstants.SIMPLE, Boolean.TRUE);
        }
    }

    public static class GalaxyTableColumnMultiSelector extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            try
            {
                Object bean = getBean();
                if( bean instanceof ArrayItem )
                    bean = ( (ArrayItem)bean ).getParent();
                return getTableColumns(
                        (DataElementPath)ComponentFactory.getModel(bean, Policy.DEFAULT, true)
                                .findProperty(getDescriptor().getValue(TABLE_PROPERTY).toString()).getValue(),
                        getDescriptor().getValue(NUMERICAL_PROPERTY).equals("true"));
            }
            catch( Exception e )
            {
                return new SelectorOption[0];
            }
        }

        public static void registerSelector(PropertyDescriptor pde, String tableProperty, boolean numerical)
        {
            pde.setValue(TABLE_PROPERTY, tableProperty);
            pde.setValue(NUMERICAL_PROPERTY, String.valueOf(numerical));
            pde.setPropertyEditorClass(GalaxyTableColumnMultiSelector.class);
            pde.setValue(BeanInfoConstants.SIMPLE, Boolean.TRUE);
        }
    }

    public static class SelectorOption implements SerializableAsText
    {
        private final String name;
        private final String displayName;

        public SelectorOption(String name, String displayName)
        {
            this.name = name;
            this.displayName = displayName;
        }

        public String getName()
        {
            return name;
        }

        public String getDisplayName()
        {
            return displayName;
        }

        @Override
        public String toString()
        {
            return getDisplayName();
        }

        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( obj == null )
                return false;
            if( getClass() != obj.getClass() )
                return false;
            SelectorOption other = (SelectorOption)obj;
            if( displayName == null )
            {
                if( other.displayName != null )
                    return false;
            }
            else if( !displayName.equals(other.displayName) )
                return false;
            return true;
        }
        
        @Override
        public String getAsText()
        {
            JSONObject json = new JSONObject();
            try
            {
                json.put("name", name);
                json.put("displayName", displayName);
            }
            catch( JSONException e )
            {
            }
            return json.toString();
        }
        
        public static SelectorOption createInstance(String str)
        {
            JSONObject json = new JSONObject( str );
            return new SelectorOption( json.getString( "name" ), json.getString( "displayName" ) );
        }
        
        
    }

    public static class GalaxyOptionMultiSelector extends GenericMultiSelectEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return getSelectorOptions((SelectParameter)getDescriptor().getValue(GALAXY_PARAMETER_PROPERTY));
        }
    }

    public static class GalaxyOptionSelector extends GenericComboBoxEditor
    {
        @Override
        protected SelectorOption[] getAvailableValues()
        {
            return getSelectorOptions((SelectParameter)getDescriptor().getValue(GALAXY_PARAMETER_PROPERTY));
        }
        
        @Override
        public void setAsText(String str) throws IllegalArgumentException
        {
            setValue( parse(str) );
        }
        
        private SelectorOption parse(String str)
        {
            for( SelectorOption opt : getAvailableValues() )
                if( str.equals( opt.getName() ) )
                    return opt;
            for( SelectorOption opt : getAvailableValues() )
                if( str.equals( opt.getDisplayName() ) )
                    return opt;
            return new SelectorOption( str, str );
        }
    }

    protected class GalaxyParameterDescriptor extends PropertyDescriptor implements Cloneable
    {
        private Parameter parameter;
        private String galaxyType;
        private final String[] parameterParts;

        public GalaxyParameterDescriptor(String name, Parameter parameter) throws IntrospectionException
        {
            super(name, null, null);
            this.parameterParts = name.split(Pattern.quote(NESTED_PARAMETER_DELIMETER));
            setParameter(parameter);
        }
        
        public Parameter getParameter()
        {
            return parameter;
        }
        
        public void setParameter(Parameter parameter)
        {
            this.parameter = parameter;
            this.galaxyType = parameter.getAttributes().get("type") == null ? "" : parameter.getAttributes().get("type").toString();
            fillParamDescriptions();
            fillTemplate();
        }

        @Override
        public boolean isHidden()
        {
            if( parameter instanceof ConfigParameter || galaxyType.equals("hidden") )
                return true;

            if( parameter.isOutput() )
                return outputFilter.isHiddenByFilter(parameter);
            
            Map<String, Parameter> parametersMap = parameter.getContainer();
            for( int i = 0; i < parameterParts.length - 2; i += 2 )
            {
                String parameterPart = parameterParts[i];
                ConditionalParameter parentParameter = (ConditionalParameter)parametersMap.get(parameterPart);
                String keyValue = parentParameter.getKeyParameter().toString();
                String conditionalPart = parameterParts[i + 1];
                if( !keyValue.equals(conditionalPart) )
                    return true;
                parametersMap = parentParameter.getWhenParameters(keyValue);
            }
            return false;
        }

        protected void fillParamDescriptions()
        {
            Object label = parameter.getAttributes().get("label");
            if( label != null && ( (String)label ).length() > 0 )
            {
                String displayName = ((String)label).replace("${tool.name}", analysisName);
                if(displayName.indexOf("${on_string}") >=0)
                {
                    String input = "(none)";
                    try
                    {
                        String paramName = getInputNames()[0];
                        input = getPropertyDescriptor( paramName ).getDisplayName();
                    }
                    catch( Exception e )
                    {
                    }
                    displayName = displayName.replace("${on_string}", input);
                }
                setDisplayName(displayName);
            }
            else
            {
                String displayName = getName().replace("_", " ");
                displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
                setDisplayName(displayName);
            }
            Object help = parameter.getAttributes().get("help");
            if( help != null )
            {
                setShortDescription(help.toString());
            }
            Object optional = parameter.getAttributes().get("optional");
            if(optional instanceof Boolean && (Boolean)optional)
            {
                super.setValue(BeanInfoConstants.CAN_BE_NULL, true);
            }
        }
        
        private void fillTemplate()
        {
            Object templateObj = parameter.getAttributes().get( OptionEx.TEMPLATE_PROPERTY );
            if(templateObj instanceof String)
            {
                String template = (String)templateObj;
                if(!template.isEmpty())
                {
                    setValue( OptionEx.TEMPLATE_PROPERTY, template );
                }
            }
        }
        
        @Override
        public GalaxyParameterDescriptor clone()
        {
            try
            {
                return (GalaxyParameterDescriptor)super.clone();
            }
            catch( CloneNotSupportedException e )
            {
                throw new RuntimeException(e);
            }
        }
    }

    public class GalaxyParameter extends DynamicProperty implements Cloneable
    {
        private static final long serialVersionUID = 1L;

        public GalaxyParameter(String name, Class<?> type, Parameter parameter) throws IntrospectionException
        {
            super(new GalaxyParameterDescriptor(name, parameter), type);
        }

        public void setSimple(boolean simple)
        {
            getDescriptor().setValue(BeanInfoConstants.SIMPLE, simple);
        }

        @Override
        public void setValue(Object value)
        {
            if(getParameter() instanceof ArrayParameter)
            {
                DynamicPropertySet[] dpsValues = (DynamicPropertySet[])value;
                if( dpsValues.length > 0 && ! ( dpsValues[0] instanceof ArrayItem ) )
                {
                    ArrayItem prototype = (ArrayItem)getAttribute("item-prototype");
                    for( int i = 0; i < dpsValues.length; i++ )
                    {
                        DynamicPropertySet dps = dpsValues[i];
                        ArrayItem item = prototype.clone();
                        Iterator<DynamicProperty> it = item.propertyIterator();
                        while( it.hasNext() )
                        {
                            GalaxyParameter gp = (GalaxyParameter)it.next();
                            DynamicProperty dp = dps.getProperty(gp.getName());
                            if( dp != null )
                                gp.setValue(dp.getValue());
                        }
                        dpsValues[i] = item;
                    }
                }
            }
            super.setValue(value);
            fillParameterValue(getParameter(), value);
        }
        
        @Override
        public GalaxyParameterDescriptor getDescriptor()
        {
            return (GalaxyParameterDescriptor)super.getDescriptor();
        }

        public Parameter getParameter()
        {
            return getDescriptor().getParameter();
        }

        public String getValueString()
        {
            if( DataElementPath.class.isAssignableFrom( getType() ) || DataElementPathSet.class.isAssignableFrom( getType() ) )
                return getValue() == null ? null : getValue().toString();
            return getParameter().toString();
        }

        public void setValueString(String value)
        {
            if( getParameter() instanceof SelectParameter )
            {
                getParameter().setValue(value);
            }
            else
            {
                setValue(TextUtil2.fromString(getType(), value));
            }
        }
        
        @Override
        protected GalaxyParameter clone()
        {
            try
            {
                GalaxyParameter result = (GalaxyParameter)super.clone();
                result.descriptor = getDescriptor().clone();
                return result;
            }
            catch( CloneNotSupportedException e )
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Array item as {@link DynamicPropertySet} with link to parent properties
     */
    protected class ArrayItem extends DynamicPropertySetSupport
    {
        private static final long serialVersionUID = 1L;
        protected DynamicPropertySet parent;
        private final String nameInParent;

        public ArrayItem(String nameInParent, DynamicPropertySet parent)
        {
            this.nameInParent = nameInParent;
            this.parent = parent;
        }

        public DynamicPropertySet getParent()
        {
            return parent;
        }
        
        @Override
        public ArrayItem clone()
        {
            ArrayItem retVal = new ArrayItem(nameInParent, parent);
            for(DynamicProperty dp : this)
                retVal.add(((GalaxyParameter)dp).clone());
            return retVal;
        }

        @Override
        public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
        {
            super.firePropertyChange(propertyName, oldValue, newValue);
            getParent().setValue(nameInParent, getParent().getValue(nameInParent));
        }
    }

    @Override
    public Long getValueAsLong(String name)
    {
        Object val = getValue( name );
        if( val == null )
            return null;
        return Long.parseLong( val.toString() );
    }

    @Override
    public DynamicPropertyBuilder getAsBuilder(String name)
    {
        throw new UnsupportedOperationException( "getAsBuilder is not supported" );
    }

    @Override
    public <T> T cast(String name, Class<T> clazz)
    {
        return clazz.cast( getValue( name ) );
    }

    @Override
    public String serializeAsXml(String beanName, String offset)
    {
        throw new UnsupportedOperationException( "serializeAsXml is not supported" );
    }
}
