package biouml.plugins.research.workflow.items;

import java.beans.PropertyChangeEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import one.util.streamex.EntryStream;
import ru.biosoft.exception.InternalException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.BeanWithAutoProperties;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.research.workflow.engine.WorkflowEngine;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class WorkflowItemFactory
{
    private static final Map<String, Class<? extends WorkflowItem>> TYPES =
            EntryStream.<String, Class<? extends WorkflowItem>>of(
                    Type.ANALYSIS_PARAMETER, WorkflowParameter.class,
                    Type.ANALYSIS_EXPRESSION, WorkflowExpression.class,
                    Type.ANALYSIS_CYCLE_VARIABLE, WorkflowCycleVariable.class).toMap();
    private static final Map<Node, Reference<WorkflowItem>> itemMap = new WeakHashMap<>();

    public static WorkflowItem getWorkflowItem(Node n)
    {
        return getWorkflowItem(n, false, null);
    }

    public static WorkflowItem getWorkflowItem(String type)
    {
        return getWorkflowItem(null, type);
    }

    public static WorkflowItem getWorkflowItem(Compartment parent, String type)
    {
        return getWorkflowItem(new Node(parent, new Stub(null, "", type)), true, null);
    }

    public static WorkflowItem getWorkflowItem(Node n, ViewEditorPane pane)
    {
        return getWorkflowItem(n, false, pane);
    }

    public static WorkflowItem getWorkflowItem(Node n, boolean canSetName, ViewEditorPane pane)
    {
        if( !canSetName )
        {
            Reference<WorkflowItem> ref;
            synchronized( itemMap )
            {
                ref = itemMap.get(n);
            }
            WorkflowItem result = ref == null ? null : ref.get();
            if( result != null )
            {
                result.setViewEditorPane(pane);
                return result;
            }
        }
        Class<? extends WorkflowItem> type = TYPES.get(n.getKernel().getType());
        if( type == null )
            return null;
        try
        {
            WorkflowItem result = type.getConstructor(Node.class, Boolean.class).newInstance(n, canSetName);
            result.setViewEditorPane(pane);
            if( !canSetName )
            {
                synchronized( itemMap )
                {
                    itemMap.put(n, new WeakReference<>(result));
                }
            }
            return result;
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @SuppressWarnings ( "serial" )
    public static class WorkflowParameters extends DynamicPropertySetSupport implements BeanWithAutoProperties
    {
        /**
         * setValue should be reimplemented because DynamicPropertySetSupport implementation writes directly to property.value
         */
        @Override
        public void setValue(String name, Object value)
        {
            DynamicProperty property = findProperty(name);
            if( property == null )
                throw new IllegalArgumentException("Could not find property " + name + " in dynamic property set.");

            Object oldValue = property.getValue();
            property.setValue(value);
            if( hasListeners(name) )
                firePropertyChange(new PropertyChangeEvent(this, name, oldValue, value));
        }

        @Override
        public AutoPropertyStatus getAutoPropertyStatus(String name)
        {
            try
            {
                WorkflowParameter parameter = (WorkflowParameter)findProperty(name).getDescriptor().getValue(WorkflowParameter.ITEM_PROPERTY);
                return TextUtil.isEmpty(parameter.getDefaultValueString()) ? AutoPropertyStatus.NOT_AUTO_PROPERTY : parameter
                        .isDefaultValueSet() ? AutoPropertyStatus.AUTO_MODE_ON : AutoPropertyStatus.AUTO_MODE_OFF;
            }
            catch( Exception e )
            {
            }
            return AutoPropertyStatus.NOT_AUTO_PROPERTY;
        }
    }

    public static class WorkflowParametersBeanInfo extends BeanInfoEx
    {
        public WorkflowParametersBeanInfo()
        {
            super(WorkflowParameters.class, MessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("CN_PARAMETERS"));
            beanDescriptor.setShortDescription(getResourceString("CD_PARAMETERS"));
        }
    }

    /**
     * @param c compartment in workflow diagram
     * @return map of variables accessible for given compartment
     */
    public static Map<String, WorkflowVariable> getVariables(Compartment c)
    {
        Map<String, WorkflowVariable> vars = new HashMap<>();
        while(true)
        {
            c.stream( Node.class ).map( WorkflowItemFactory::getWorkflowItem ).select( WorkflowVariable.class )
                    .forEach( var -> vars.putIfAbsent( var.getName(), var ) );
            if(c instanceof Diagram) break;
            c = (Compartment)c.getOrigin();
            if(c == null)
            {
                throw new InternalException( "Compartment supplied doesn't belong to any diagram" );
            }
        }
        Diagram workflow = (Diagram)c;
        for( SystemVariable var : SystemVariable.getVariables(workflow) )
        {
            vars.putIfAbsent( var.getName(), var );
        }
        return vars;
    }

    public static DynamicPropertySet getWorkflowParameters(Diagram d) throws Exception
    {
        DynamicPropertySet result = new WorkflowParameters();
        d.stream( Node.class ).map( WorkflowItemFactory::getWorkflowItem ).select( WorkflowParameter.class )
                .peek( parameter -> parameter.setCurrentValue( null ) ).sorted().map( WorkflowParameter::getProperty )
                .forEach( result::add );

        DynamicProperty skipCompleted = d.getAttributes().getProperty(WorkflowEngine.SKIP_COMPLETED_PROPERTY);
        if(skipCompleted == null || !DPSUtils.isTransient(skipCompleted)) {
            PropertyDescriptorEx pde = new PropertyDescriptorEx(WorkflowEngine.SKIP_COMPLETED_PROPERTY);
            pde.setDisplayName("Skip completed");
            pde.setShortDescription("Skip already performed steps");
            pde.setExpert(true);
            pde.setHidden(SecurityManager.isExperimentalFeatureHiddenMethod());
            skipCompleted = new DynamicProperty(pde, Boolean.class);
            skipCompleted.setValue(Boolean.FALSE);
            DPSUtils.makeTransient(skipCompleted);
            d.getAttributes().add(skipCompleted);
        }
        result.add(skipCompleted);

        DynamicProperty ignoreFail = d.getAttributes().getProperty( WorkflowEngine.IGNORE_FAIL_PROPERTY );
        if( ignoreFail == null || !DPSUtils.isTransient( ignoreFail ) )
        {
            PropertyDescriptorEx pde = new PropertyDescriptorEx( WorkflowEngine.IGNORE_FAIL_PROPERTY );
            pde.setDisplayName( "Ignore failed steps" );
            pde.setShortDescription( "Ignore failed analysis and execute available ones" );
            pde.setExpert( true );
            pde.setHidden( SecurityManager.isExperimentalFeatureHiddenMethod() );
            ignoreFail = new DynamicProperty( pde, Boolean.class );
            ignoreFail.setValue( Boolean.FALSE );
            DPSUtils.makeTransient( ignoreFail );
            d.getAttributes().add( ignoreFail );
        }
        result.add( ignoreFail );

        return result;
    }
}
