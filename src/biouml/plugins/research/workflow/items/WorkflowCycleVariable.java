package biouml.plugins.research.workflow.items;

import java.awt.Point;
import java.beans.PropertyDescriptor;
import one.util.streamex.IntStreamEx;

import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.bean.StaticDescriptor;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class WorkflowCycleVariable extends WorkflowExpression
{
    static final CycleType[] CYCLE_TYPES = {new CollectionCycleType(), new SetCycleType(), new NameSetCycleType(), new RangeCycleType(), new EnumCycleType(), new TableColumnsCycleType(), new TableNumericalColumnsCycleType()};
    private static final String PARAMETER_CYCLE_TYPE = "cycle-type";
    private static final String PARAMETER_PARALLEL = "parallel";

    private static final PropertyDescriptor PARAMETER_CYCLE_TYPE_PD = StaticDescriptor.create(PARAMETER_CYCLE_TYPE);
    private static final PropertyDescriptor PARAMETER_PARALLEL_PD = StaticDescriptor.create(PARAMETER_PARALLEL);

    private int iteration = 0;
    private String fixedExpression = "";
    private String[] values;

    /**
     * @param node
     * @param canSetName
     */
    public WorkflowCycleVariable(Node node, Boolean canSetName)
    {
        super(node, canSetName);
    }

    public void reset()
    {
        values = null;
    }

    private void initCycle(WorkflowVariable[] vars) throws CalculationException
    {
        String expression = calculateExpression(getExpression(), vars);
        if(values != null && expression.equals(fixedExpression)) return;
        fixedExpression = expression;
        int count = getCycleType().getCount(expression);
        values = IntStreamEx.range( count ).mapToObj( i -> getCycleType().getValue( expression, i ) ).toArray( String[]::new );
    }

    public int getCount()
    {
        try
        {
            initCycle(new WorkflowVariable[] {this});
            return values.length;
        }
        catch( Exception e )
        {
            return 0;
        }
    }

    public void setIteration(int n)
    {
        iteration = n;
    }

    /**
     * @return the cycleType
     */
    public CycleType getCycleType()
    {
        Object valueObj = getNode().getAttributes().getValue(PARAMETER_CYCLE_TYPE);
        if(valueObj == null) return CYCLE_TYPES[0];
        String value = valueObj.toString();
        for(CycleType type: CYCLE_TYPES)
        {
            if(type.getClass().getSimpleName().equals(value)) return type;
        }
        return CYCLE_TYPES[0];
    }

    /**
     * @param cycleType the cycleType to set
     */
    public void setCycleType(CycleType cycleType)
    {
        startTransaction("Set cycle type");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_CYCLE_TYPE_PD, String.class, cycleType.getClass().getSimpleName()));
        completeTransaction();
        firePropertyChange("*", null, null);
    }

    public static CycleType getCycleTypeByName(String name)
    {
        for(CycleType ct : CYCLE_TYPES)
            if(ct.getName().equals( name ))
                return ct;
        throw new IllegalArgumentException("Unknown cycle type: " + name);
    }

    public boolean isParallel()
    {
        Object valueObj = getNode().getAttributes().getValue(PARAMETER_PARALLEL);
        if( valueObj == null || ! ( valueObj instanceof Boolean ) )
            return false;
        return ((Boolean)valueObj);
    }

    public void setParallel(boolean parallel)
    {
        startTransaction("Set cycle parallelism");
        getNode().getAttributes().add(new DynamicProperty(PARAMETER_PARALLEL_PD, Boolean.class, parallel));
        completeTransaction();
        firePropertyChange("*", null, null);
    }

    public static class CycleTypeSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return CYCLE_TYPES;
        }
    }

    @Override
    protected Object getValueDependent(WorkflowVariable[] dependentVariables) throws CalculationException
    {
        initCycle(dependentVariables);
        return getType().fromString(values[iteration]);
    }

    @Override
    public DiagramElementGroup createElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        WorkflowSemanticController semanticController = (WorkflowSemanticController)Diagram.getDiagram(c).getType().getSemanticController();
        DiagramElement cycle = semanticController.createCycleNode(c, this);
        if( semanticController.canAccept(c, cycle) )
        {
            viewPane.add(cycle, location);
        }
        return new DiagramElementGroup( cycle );
    }
}
