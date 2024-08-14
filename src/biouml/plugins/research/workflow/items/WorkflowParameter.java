package biouml.plugins.research.workflow.items;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.util.List;

import javax.annotation.Nonnull;
import one.util.streamex.IntStreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.StaticDescriptor;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.PropertyEditorEx;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

@SuppressWarnings ( "serial" )
public class WorkflowParameter extends WorkflowExpression implements Comparable<WorkflowParameter>
{
    public static final String ITEM_PROPERTY = "item";

    private Object currentValue;
    
    public static final String ROLE_DEFAULT = "Default";
    public static final String ROLE_INPUT = "Input";
    public static final String ROLE_OUTPUT = "Output";
    public static final String[] ROLES = {ROLE_DEFAULT, ROLE_INPUT, ROLE_OUTPUT};
    
    public static final String PARAMETER_DEFAULT_VALUE = "default-value";
    public static final String PARAMETER_ROLE = "parameter-role";
    public static final String PARAMETER_RANK = "parameter-rank";
    public static final String PARAMETER_ELEMENT_TYPE = "parameter-element-type";
    public static final String PARAMETER_REFERENCE_TYPE = "parameter-reference-type";
    public static final String PARAMETER_DESCRIPTION = "parameter-description";
    public static final String PARAMETER_OPTIONS_MODE = "parameter-options-mode";
    public static final String PARAMETER_OPTIONS_EXPRESSION = "parameter-options-expression";
    
    protected static final PropertyDescriptor PARAMETER_DEFAULT_VALUE_PD = StaticDescriptor.create(PARAMETER_DEFAULT_VALUE);
    protected static final PropertyDescriptor PARAMETER_ROLE_PD = StaticDescriptor.create(PARAMETER_ROLE);
    protected static final PropertyDescriptor PARAMETER_RANK_PD = StaticDescriptor.create(PARAMETER_RANK);
    protected static final PropertyDescriptor PARAMETER_ELEMENT_TYPE_PD = StaticDescriptor.create(PARAMETER_ELEMENT_TYPE);
    protected static final PropertyDescriptor PARAMETER_REFERENCE_TYPE_PD = StaticDescriptor.create(PARAMETER_REFERENCE_TYPE);
    protected static final PropertyDescriptor PARAMETER_DESCRIPTION_PD = StaticDescriptor.create(PARAMETER_DESCRIPTION);
    protected static final PropertyDescriptor PARAMETER_OPTIONS_MODE_PD = StaticDescriptor.create(PARAMETER_OPTIONS_MODE);
    protected static final PropertyDescriptor PARAMETER_OPTIONS_EXPRESSION_PD = StaticDescriptor.create(PARAMETER_OPTIONS_EXPRESSION);

    public WorkflowParameter(Node node, Boolean canSetName)
    {
        super(node, canSetName);
    }

    @Override
    public void setType(VariableType type)
    {
        super.setType(type);
        property = null;
    }

    public void setRole(String role)
    {
        startTransaction("Set role");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_ROLE_PD, String.class, role));
        completeTransaction();
        property = null;
        firePropertyChange("role", null, role);
    }
    
    public String getRole()
    {
        if(isDataElementTypeHidden()) return ROLE_DEFAULT;
        Object roleObj = getNode().getAttributes().getValue(PARAMETER_ROLE);
        return roleObj == null?ROLE_DEFAULT:roleObj.toString();
    }
    
    public void setDescription(String description)
    {
        startTransaction("Set description");
        if(description == null || description.isEmpty())
            getNode().getAttributes().remove(PARAMETER_DESCRIPTION);
        else
            getNode().getAttributes().add(new DynamicProperty(PARAMETER_DESCRIPTION_PD, String.class, description));
        completeTransaction();
        property = null;
        firePropertyChange("description", null, description);
    }
    
    public String getDescription()
    {
        Object descriptionObj = getNode().getAttributes().getValue(PARAMETER_DESCRIPTION);
        return descriptionObj == null?"":descriptionObj.toString();
    }
    
    public void setDataElementType(DataElementType type)
    {
        startTransaction("Set data element type");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_ELEMENT_TYPE_PD, String.class, type.toString()));
        completeTransaction();
        property = null;
        firePropertyChange("dataElementType", null, type.toString());
        firePropertyChange("*", null, null);
    }
    
    public DataElementType getDataElementType()
    {
        Object roleObj = getNode().getAttributes().getValue(PARAMETER_ELEMENT_TYPE);
        return DataElementType.getType(roleObj == null?null:roleObj.toString());
    }
    
    public boolean isDataElementTypeHidden()
    {
        VariableType type = getType();
        return !type.getTypeClass().equals(DataElementPath.class) && !type.getTypeClass().equals(DataElementPathSet.class);
    }
    
    public void setReferenceType(String type)
    {
        startTransaction("Set reference type");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_REFERENCE_TYPE_PD, String.class, type));
        completeTransaction();
        property = null;
        firePropertyChange("referenceType", null, type);
    }
    
    public @Nonnull String getReferenceType()
    {
        Object roleObj = getNode().getAttributes().getValue(PARAMETER_REFERENCE_TYPE);
        ReferenceType type = roleObj == null ? null : ReferenceTypeRegistry.optReferenceType(roleObj.toString());
        if(type == null) type = ReferenceTypeRegistry.getDefaultReferenceType();
        return type.toString();
    }
    
    public boolean isReferenceTypeHidden()
    {
        return isDataElementTypeHidden() || !getDataElementType().getTypeClass().equals(TableDataCollection.class);
    }
    
    public String getDropDownOptions()
    {
        Object optionsMode = getNode().getAttributes().getValue(PARAMETER_OPTIONS_MODE);
        return optionsMode == null || optionsMode.toString().isEmpty() ? "(none)" : optionsMode.equals("auto") ? "(auto)" : optionsMode.toString();
    }
    
    public void setDropDownOptions(String options)
    {
        startTransaction("Set options mode");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_OPTIONS_MODE_PD, String.class, options));
        completeTransaction();
        property = null;
        firePropertyChange("dropDownOptions", null, options);
    }
    
    public boolean isDropDownOptionsHidden()
    {
        Class<?> type = getType().getTypeClass();
        return !type.equals(String.class) && !type.equals( String[].class );
    }
    
    public String getDropDownOptionsExpression()
    {
        Object optionsExpression = getNode().getAttributes().getValue(PARAMETER_OPTIONS_EXPRESSION);
        return optionsExpression == null ? "" : optionsExpression.toString();
    }
    
    public String evaluateDropDownOptionsExpression() throws CalculationException
    {
        return calculateExpression(getDropDownOptionsExpression(), new WorkflowVariable[] {});
    }
    
    public void setDropDownOptionsExpression(String optionsExpression)
    {
        startTransaction("Set options expression");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_OPTIONS_EXPRESSION_PD, String.class, optionsExpression));
        completeTransaction();
        property = null;
        firePropertyChange("dropDownOptionsExpression", null, optionsExpression);
    }

    public boolean isDropDownOptionsExpressionHidden()
    {
        if(isDropDownOptionsHidden()) return true;
        String options = getDropDownOptions();
        return options.equals("(none)") || options.equals("(auto)");
    }
    
    public void setRank(String rank)
    {
        startTransaction("Set rank");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_RANK_PD, String.class, rank));
        completeTransaction();
        firePropertyChange("rank", null, rank);
    }
    
    public String getRank()
    {
        Object rankObj = getNode().getAttributes().getValue(PARAMETER_RANK);
        return rankObj == null?"1":rankObj.toString();
    }
    
    public void setDefaultValueString(String value)
    {
        startTransaction("Set default value");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_DEFAULT_VALUE_PD, String.class, value));
        completeTransaction();
        firePropertyChange("*", null, null);
    }
    
    public String getDefaultValueString()
    {
        Object valueObj = getNode().getAttributes().getValue(PARAMETER_DEFAULT_VALUE);
        return valueObj == null?null:valueObj.toString();
    }
    
    public Object getDefaultValue()
    {
        try
        {
            return getType().fromString(calculateExpression(getExpression(), new WorkflowVariable[] {this}));
        }
        catch( CalculationException e )
        {
            return null;
        }
    }
    
    public Object getCurrentValue()
    {
        return currentValue == null?getDefaultValue():currentValue;
    }

    public void setCurrentValue(Object currentValue)
    {
        if(currentValue == null)
            this.currentValue = null;
        else
        {
            Object oldCurrentValue = getCurrentValue();
            if(oldCurrentValue == null || !oldCurrentValue.equals(currentValue))
                this.currentValue = currentValue;
        }
    }

    @Override
    public String getExpression()
    {
        return getDefaultValueString();
    }

    @Override
    public void setExpression(String value)
    {
        setDefaultValueString(value);
    }

    @Override
    protected Object getValueDependent(WorkflowVariable[] dependentVariables) throws CalculationException
    {
        return currentValue==null?getType().fromString(calculateExpression(getExpression(), dependentVariables)):currentValue;
    }
    
    public boolean isDefaultValueSet()
    {
        return currentValue==null;
    }

    private DynamicProperty property;
    public DynamicProperty getProperty()
    {
        if(property == null)
        {
            try
            {
                PropertyDescriptorEx pde = new PropertyDescriptorEx(getName());
                if(getType().getEditorType() != null)
                    pde.setPropertyEditorClass(getType().getEditorType());
                pde.setHideChildren(true);
                String description = getDescription();
                pde.setShortDescription(description);
                if(getType().getTypeClass().isAssignableFrom(DataElementPath.class) || getType().getTypeClass().isAssignableFrom(DataElementPathSet.class))
                {
                    if(getRole().equals(ROLE_INPUT))
                    {
                        pde.setValue(DataElementPathEditor.ELEMENT_MUST_EXIST, true);
                        pde.setValue( DataElementPathEditor.IS_INPUT, true );
                    }
                    else if(getRole().equals(ROLE_OUTPUT))
                    {
                        pde.setValue(DataElementPathEditor.PROMPT_OVERWRITE, true);
                        pde.setValue( DataElementPathEditor.IS_OUTPUT, true );
                    }
                    Class<? extends DataElement> typeClass = getDataElementType().getTypeClass();
                    if(!typeClass.equals(DataElement.class))
                    {
                        pde.setValue(DataElementPathEditor.ELEMENT_CLASS, typeClass);
                        if(typeClass.equals(TableDataCollection.class) && !getReferenceType().equals(ReferenceTypeRegistry.getDefaultReferenceType().toString()))
                            pde.setValue(DataElementPathEditor.REFERENCE_TYPE, ReferenceTypeRegistry.getReferenceType(getReferenceType()).getClass());
                    }
                } else if(getType().getTypeClass().equals(String.class) && !getDropDownOptions().equals("(none)"))
                {
                    pde.setPropertyEditorClass(WorkflowParameterAutoSelector.class);
                    pde.setValue("node", getNode());
                    pde.setValue("mode", getDropDownOptions());
                } else if(getType().getTypeClass().equals( String[].class ))
                {
                    pde.setHideChildren( false );
                    pde.setPropertyEditorClass(WorkflowParameterMultiStringSelector.class);
                    pde.setValue("node", getNode());
                    pde.setValue("mode", getDropDownOptions());
                }
                else if(getType().getTypeClass().equals(Species.class))
                {
                    DataElementComboBoxSelector.registerSelector(pde, Species.SPECIES_PATH);
                }
                property = new DynamicProperty(pde, getType().getTypeClass())
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getValue()
                    {
                        Object value = getCurrentValue();
                        if(getDescriptor().getPropertyEditorClass() == WorkflowParameterAutoSelector.class)
                        {
                            if(value == null) value = "";
                            WorkflowParameterAutoSelector editor = new WorkflowParameterAutoSelector();
                            editor.setDescriptor(getDescriptor());
                            String valueStr = value.toString();
                            String[] tags = editor.getTags();
                            for(String tag: tags)
                            {
                                if(tag.equals(valueStr)) return value;
                            }
                            if(tags.length > 0) return tags[0];
                        }
                        return value;
                    }

                    @Override
                    public void setValue(Object value)
                    {
                        setCurrentValue(value);
                        firePropertyChange("*", null, null);
                    }
                };
                property.getDescriptor().setValue(ITEM_PROPERTY, this);
            }
            catch( IntrospectionException e1 )
            {
            }
        }
        return property;
    }
    
    @Override
    public int compareTo(WorkflowParameter param)
    {
        return getRank().compareTo(param.getRank());
    }

    public static class WorkflowParameterRoleSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return ROLES;
        }
    }

    @Override
    protected WorkflowVariable getCustomVariable(String name, Diagram workflow)
    {
        if(name.equals("defaultPath") && getRole().equals(ROLE_OUTPUT) && getType().getTypeClass().equals(DataElementPath.class))
        {
            DataElementPath projectPath = JournalRegistry.getProjectPath();
            DataElementPath defaultPath = projectPath == null || projectPath.isEmpty() ? DataElementPath.EMPTY_PATH : projectPath
                    .getChildPath("Data");
            List<WorkflowParameter> parameters = workflow.stream( Node.class )
                    .map( WorkflowItemFactory::getWorkflowItem ).select( WorkflowParameter.class ).without( this )
                    .filter( parameter -> parameter.getRole().equals( WorkflowParameter.ROLE_INPUT )
                                    && parameter.getType().getTypeClass().equals( DataElementPath.class ) ).sorted().toList();
            for(WorkflowParameter parameter: parameters)
            {
                Object value = parameter.getCurrentValue();
                if(value instanceof DataElementPath)
                {
                    DataCollection<?> dc = ((DataElementPath)value).optParentCollection();
                    if(dc != null && dc.isAcceptable(getDataElementType().getTypeClass()))
                    {
                        defaultPath = dc.getCompletePath();
                        break;
                    }
                }
            }
            final DataElementPath finalDefaultPath = defaultPath;
            return new SystemVariable("defaultPath")
            {
                @Override
                public Object getValue() throws Exception
                {
                    return finalDefaultPath;
                }
            };
        }
        return super.getCustomVariable(name, workflow);
    }
    
    
    
    public static String[] getDropDownValues(Node n, String mode) throws CalculationException
    {
        if( mode.equals( "(auto)" ) )
        {
            for( Edge e : n.edges().filter( e -> e.getInput() == n ) )
            {
                try
                {
                    Node output = e.getOutput();
                    Object edgeAnalysisPropertyObj = e.getAttributes().getValue( WorkflowSemanticController.EDGE_ANALYSIS_PROPERTY );
                    if( edgeAnalysisPropertyObj == null )
                        continue;
                    AnalysisParameters parameters = WorkflowEngine.getAnalysisParametersByNode( output, true );
                    if( parameters == null )
                        continue;
                    ComponentModel model = ComponentFactory.getModel( parameters );
                    Property property = model.findProperty( edgeAnalysisPropertyObj.toString() );
                    if( property == null )
                        continue;
                    Object editor = property.getPropertyEditorClass().newInstance();
                    if( editor instanceof PropertyEditor )
                    {
                        if( editor instanceof PropertyEditorEx )
                        {
                            ( (PropertyEditorEx)editor ).setDescriptor( property.getDescriptor() );
                            ( (PropertyEditorEx)editor ).setBean( property.getOwner() );
                        }
                        ( (PropertyEditor)editor ).setValue( property.getValue() );
                        return ( (PropertyEditor)editor ).getTags();
                    }
                }
                catch( Exception e1 )
                {
                }
            }
        }
        else
        {
            WorkflowParameter parameter = (WorkflowParameter)WorkflowItemFactory.getWorkflowItem( n );
            String expression = parameter.evaluateDropDownOptionsExpression();
            for( CycleType type : WorkflowCycleVariable.CYCLE_TYPES )
            {
                if( type.getName().equals( mode ) )
                {
                    int count = type.getCount( expression );
                    return IntStreamEx.range( count ).mapToObj( i -> type.getValue( expression, i ) ).toArray( String[]::new );
                }
            }
        }
        return new String[0];
    }


}
