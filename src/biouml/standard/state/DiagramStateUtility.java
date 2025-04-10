
package biouml.standard.state;

import java.awt.Point;
import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.swing.undo.UndoableEdit;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil2;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.standard.diagram.CreatorElementWithName;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.undo.Transaction;

/**
 * @author anna
 *
 */
public class DiagramStateUtility
{
    /**
     * Create state by comparing original and changed diagrams
     *
     * @param original
     * @param changed
     * @return
     * @throws Exception
     */
    public static @Nonnull State createState(Diagram original, Diagram changed, String name) throws Exception
    {
        Diagram base = original.clone(original.getOrigin(), original.getName());
        State state = new State(null, base, name);
        base.addState(state);
        base.setStateEditingMode(state);
        copyChanges(base, changed);
        base.restore();
        base.removeState(state);
        return state;
    }

    public static void copyChanges(Diagram base, Diagram changed) throws Exception
    {
        boolean notificationEnabled = base.isNotificationEnabled();
        base.setNotificationEnabled(true);

        /*
         * TODO: compare diagram kernels
        Base ck = changed.getKernel();
        Base bk = base.getKernel();
        if( !sameNull(ck, bk) || !sameClass(ck, bk) || !sameKernels(bk, ck))
        {
            if( ck instanceof DiagramInfo )
            {
                base.setKernel((DiagramInfo) ( (DiagramInfo)ck ).clone(changed.getName()));
            }
        }*/
        //TODO: compare filters
        //TODO: compare states

        Object bl = base.getPathLayouter();
        Object cl = changed.getPathLayouter();
        if( !sameNull(cl, bl) || cl != null && !sameClass(cl, bl) )
        {
            base.setPathLayouter(changed.getPathLayouter());
        }

        Object bll = base.getLabelLayouter();
        Object cll = changed.getLabelLayouter();
        if( !sameNull(cll, bll) || cll != null && !sameClass(cll, bll) )
        {
            base.setLabelLayouter(changed.getLabelLayouter());
        }
        //TODO: compare view options?

        //compare as compartments
        Set<String> exclude = new HashSet<>();
        exclude.add("role");
        exclude.add("kernel");
        exclude.add("attributes");
        exclude.add("origin");
        exclude.add("diagramType");
        exclude.add("layouter");
        exclude.add("currentStateName");
        copyChanges(base, changed, exclude);

        base.setNotificationEnabled(notificationEnabled);
    }

    private static void copyChanges(Compartment base, Compartment changed, Set<String> exclude) throws Exception
    {
        try
        {
            // remove Edges absent in changed
            for( String name : base.getNameList() )
            {
                DataElement debaseElement = base.get(name);
                if(!(debaseElement instanceof Edge )) continue;
                Edge debase = (Edge)debaseElement;
                DiagramElement de = changed.get(debase.getName());
                if( de == null || !sameClass(de, debase) || !sameKernels(debase.getKernel(), de.getKernel()) )
                {
                    //found element with different class or different kernel
                    base.remove(debase.getName());
                }
            }
            // remove Nodes absent in changed
            for( Node debase : base.getNodes() )
            {
                DiagramElement de = changed.get(debase.getName());
                if( de == null || !sameClass(de, debase) || !sameKernels(debase.getKernel(), de.getKernel()) )
                {
                    //found element with different class or different kernel
                    base.remove(debase.getName());
                }
            }
            // compare nodes
            for( Node de : changed.getNodes() )
            {
                DiagramElement debase = base.get(de.getName());
                if( debase != null && (!sameClass(de, debase) || !sameKernels(debase.getKernel(), de.getKernel())) )
                {
                    //found element with different class or different kernel
                    debase = null;
                }

                if( de instanceof Compartment )
                {
                    Compartment node = (Compartment)de;
                    if( debase == null )
                    {
                        //absent compartment
                        base.setNotificationEnabled(false);
                        Compartment n = node.clone(base, node.getName());
                        base.setNotificationEnabled(true);
                        base.put(n);
                    }
                    else
                    {
                        //compare compartments, copy changes
                        copyChanges((Compartment)debase, node, null);
                    }
                }
                else
                {
                    if( debase == null )
                    {
                        base.setNotificationEnabled(false);
                        DiagramElement newElement = de.clone(base, de.getName());
                        base.setNotificationEnabled(true);
                        base.put(newElement);
                    }
                    else
                    {
                        //compare nodes, copy changes
                        copyChanges(debase, de, null);
                    }
                }
            }
            // compare edges
            for( Edge de : changed.stream( Edge.class ) )
            {
                DiagramElement debase = base.get(de.getName());
                if( debase != null && (!sameClass(de, debase) || !sameKernels(debase.getKernel(), de.getKernel())) )
                {
                    //found element with different class or different kernel
                    debase = null;
                }
                if( debase == null )
                {
                    base.setNotificationEnabled(false);
                    DiagramElement newElement = de.clone(base, de.getName()) ;
                    base.setNotificationEnabled(true);
                    base.put(newElement);
                }
                else
                {
                    //compare edges, copy changes
                    copyChanges(debase, de, null);
                }
            }
            copyChanges((Node)base, (Node)changed, exclude);
        }
        catch( Exception exc )
        {
            throw new Exception("Comparing compartments error", exc);
        }
    }

    private static void copyChanges(DiagramElement base, DiagramElement changed, Set<String> exclude)
    {
        if( exclude == null )
            exclude = new HashSet<>();

        //copy properties as bean
        exclude.add("role");
        exclude.add("kernel");
        exclude.add("attributes");
        exclude.add("origin");
        exclude.add("note_background");
        exclude.add("image");
        copyChanges(ComponentFactory.getModel(base, Policy.UI, true), ComponentFactory.getModel(changed, Policy.UI, true), exclude);

        //copy attributes
        copyChanges(base.getAttributes(), changed.getAttributes());

        //copy role
        copyRoleChanges(base, changed);

        //origin is skipped in comparison!
    }

    private static void copyRoleChanges(DiagramElement base, DiagramElement changed)
    {
        Role role = base.getRole();
        Role chRole = changed.getRole();
        if(chRole == null && role != null)
        {
            base.setRole(null);
            return;
        }
        if(chRole == null && role == null) return;
        if( !sameClass(role, chRole) )
            base.setRole(chRole.clone(base));
        CompositeProperty baseRole = ComponentFactory.getModel(base.getRole(), Policy.UI, true);
        CompositeProperty changedRole = ComponentFactory.getModel(chRole, Policy.UI, true);
        Set<String> exclude2 = new HashSet<>();

        if( chRole instanceof EModel )
        {
            DataCollection<Variable> chvars = ( (EModel)chRole ).getVariables();
            DataCollection<Variable> vars = ( (EModel)role ).getVariables();
            copyVariablesChanges(vars, chvars);
            exclude2.add("vars");
        }
        copyChanges(baseRole, changedRole, exclude2);
    }

    private static void copyChanges(CompositeProperty base, CompositeProperty changed, Set<String> excludeNames)
    {
        for( int i = 0; i < changed.getPropertyCount(); i++ )
        {
            Property property = changed.getPropertyAt(i);
            if( excludeNames.contains(property.getName()) )
                continue;
            Property baseProp = base.getPropertyAt(i);
            if( baseProp != null )
            {
                try
                {
                    Object value = property.getValue();
                    Object baseValue = baseProp.getValue();
                    if(value == null && baseValue == null) continue;
                    if(value == null)
                    {
                        baseProp.setValue(null);
                        continue;
                    }
                    if(baseValue != null && baseValue.equals(value)) continue;
                    if( property instanceof CompositeProperty )
                    {
                        if(value instanceof Cloneable)
                        {
                            baseProp.setValue(value.getClass().getMethod("clone").invoke(value));
                            continue;
                        }
                        Object newValue = TextUtil2.fromString(value.getClass(), TextUtil2.toString(value));
                        if(newValue != null)
                        {
                            baseProp.setValue(newValue);
                            continue;
                        }
                        copyChanges((CompositeProperty) property, (CompositeProperty)baseProp, excludeNames);
                    }
                    else if( property instanceof ArrayProperty )
                    {
                        //TODO:
                    }
                    else
                    {
                        baseProp.setValue(value);
                    }
                }
                catch( Exception e )
                {
                }
            }
        }
    }

    private static void copyChanges(DynamicPropertySet base, DynamicPropertySet changed)
    {
        if( changed != null )
        {
            Iterator<String> iter = changed.nameIterator();
            while( iter.hasNext() )
            {
                DynamicProperty oldProp = changed.getProperty(iter.next());
                DynamicProperty prop = base.getProperty(oldProp.getName());
                if( prop != null )
                {
                    if( !oldProp.getValue().equals(prop.getValue()) )
                        prop.setValue(oldProp.getValue());
                }
                else
                {
                    try
                    {
                        prop = DynamicPropertySetSupport.cloneProperty(oldProp);
                        base.add(prop);
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
        }
    }

    private static void copyVariablesChanges(DataCollection<?> base, DataCollection<?> changed)
    {
        List<String> names = changed.getNameList();
        Iterator<String> iter = names.iterator();
        while( iter.hasNext() )
        {
            String name = iter.next();
            DataElement de = null;
            DataElement chde = null;
            try
            {
                de = base.get(name);
                chde = changed.get(name);
            }
            catch( Exception e )
            {
            }
            if( de == null )
            {
                //if variable is not found, it shoud be also missed as node and will be added in AddNode edit
                /*try
                {
                    base.put((Variable) ( (Variable)changed.get(name) ).clone());
                }
                catch( Exception e )
                {
                }*/
            }
            else
            {
                copyChanges(ComponentFactory.getModel(de, Policy.UI, true), ComponentFactory.getModel(chde, Policy.UI, true), new HashSet<String>());
            }
        }

        names = base.getNameList();
        iter = names.iterator();
        List<String> toRemove = new ArrayList<>();
        while( iter.hasNext() )
        {
            String name = iter.next();
            DataElement de = null;
            try
            {
                de = changed.get(name);
            }
            catch( Exception e )
            {
            }
            if( de == null )
            {
                toRemove.add(name);
            }
        }
        for( String name : toRemove )
        {
            try
            {
                base.remove(name);
            }
            catch( Exception e )
            {
            }
        }
    }

    private static boolean sameNull(Object o1, Object o2)
    {
        return ( o1 == null && o2 == null ) || ( o1 != null && o2 != null );
    }

    private static boolean sameClass(Object o1, Object o2)
    {
        return (o1 == null && o2 == null) || (o1 != null && o2 != null && o1.getClass().equals(o2.getClass()));
    }

    private static boolean sameModels(CompositeProperty base, CompositeProperty changed, Set<String> excludeNames)
    {
        if( base == null )
            return ( changed == null );
        if( changed == null )
            return false;
        for( int i = 0; i < changed.getPropertyCount(); i++ )
        {
            Property property = changed.getPropertyAt(i);
            if( excludeNames.contains(property.getName()) )
                continue;
            Property baseProp = base.getPropertyAt(i);
            if( baseProp != null )
            {
                if(baseProp.getValue() == null && property.getValue() == null)
                    continue;
                if( !sameClass(baseProp.getValue(), property.getValue()) )
                    return false;
                if( property instanceof CompositeProperty )
                {
                    if(!sameModels((CompositeProperty) property, (CompositeProperty)baseProp, excludeNames))
                        return false;
                }
                else if( property instanceof ArrayProperty )
                {
                    //TODO:
                }
                else if( property.getValue() != null && !property.getValue().equals(baseProp.getValue()) )
                {
                    return false;
                }
            }
            else
                return false;
        }
        return true;
    }

    private static boolean sameKernels(Base kernel, Base changedKernel)
    {
        if( !sameNull(kernel, changedKernel) || !sameClass(kernel, changedKernel) )
            return false;
        if(kernel == null) return true;
        //If kernels are stubs, or have null parent, compare them as model; compare as objects otherwise
        if( kernel.getOrigin() != null && changedKernel.getOrigin() != null && ! ( kernel instanceof Stub ) && !(kernel instanceof SpecieReference))
            return kernel == changedKernel;
        //copy changes from kernel to kernel
        Set<String> exclude = new HashSet<>();
        exclude.add("role");
        exclude.add("kernel");
        exclude.add("attributes");
        exclude.add("origin");
        exclude.add("note_background");
        exclude.add("image");
        copyChanges(ComponentFactory.getModel(kernel), ComponentFactory.getModel(changedKernel), exclude);
        copyChanges(ComponentFactory.getModel(kernel.getAttributes()), ComponentFactory.getModel(changedKernel.getAttributes()), new HashSet<String>());
        //return sameModels(ComponentFactory.getModel(kernel), ComponentFactory.getModel(changedKernel), exclude) && sameModels(ComponentFactory.getModel(kernel.getAttributes()), ComponentFactory.getModel(changedKernel.getAttributes()), new HashSet<String>());
        return true;
    }

    public static void applyState(Diagram diagram, State state) throws Exception
    {
        applyState(diagram, state, state.getName());
    }

    public static void applyState(Diagram diagram, State state, String newName) throws Exception
    {
        State oldState = diagram.getState( newName );

        if (oldState != null)
            diagram.removeState(oldState);

        State newState = state.clone(diagram, newName);
        newState.setTitle( newName );
        diagram.addState(newState);
        diagram.setCurrentStateName(newState.getName());
    }

    public static void applyStateEdits(Diagram diagram, State state) throws Exception
    {
        state.cloneEdits( diagram, state.getStateUndoManager().getEdits() );
    }

    public static Diagram getDiagramCloneWithUndo(Diagram diagram, List<UndoableEdit> edits) throws Exception
    {
        Diagram result = diagram.clone(diagram.getOrigin(), diagram.getName());
        result.setNotificationEnabled(false);
        undoEdits(result, edits);
        result.setNotificationEnabled(true);
        return result;
    }

    public static Diagram getDiagramCloneWithRedo(Diagram diagram, List<UndoableEdit> edits) throws Exception
    {
        Diagram result = diagram.clone(diagram.getOrigin(), diagram.getName());
        result.setNotificationEnabled(false);
        redoEdits(result, edits);
        result.setNotificationEnabled(true);
        return result;
    }

    /**
     * Undo edits which came from another diagram
     * @param diagram
     * @param edits
     * @throws Exception
     */
    public static void undoEdits(Diagram diagram, List<UndoableEdit> edits) throws Exception
    {
        for(int i = edits.size()-1; i>=0; i--)
        {
            UndoableEdit edit = edits.get(i);
            if( edit instanceof Transaction )
            {
                undoEdits(diagram, ( (Transaction)edit ).getEdits());
            }
            else if( edit instanceof DataCollectionRemoveUndo )
            {
                undoRemoveEdit(diagram, (DataCollectionRemoveUndo)edit);
            }
            else if( edit instanceof DataCollectionAddUndo )
            {
                undoAddEdit(diagram, (DataCollectionAddUndo)edit);
            }
            else if( edit instanceof StatePropertyChangeUndo )
            {
                undoPropertyChangeEdit(diagram, (StatePropertyChangeUndo)edit);
            }
        }
    }

    /**
     * Redo edits which came from another diagram
     * @param diagram
     * @param edits
     * @throws Exception
     */
    public static void redoEdits(Diagram diagram, List<UndoableEdit> edits) throws Exception
    {
        for( UndoableEdit edit : edits )
        {
            if( edit instanceof Transaction )
            {
                redoEdits(diagram, ( (Transaction)edit ).getEdits());
            }
            else if( edit instanceof DataCollectionRemoveUndo )
            {
                redoRemoveEdit(diagram, (DataCollectionRemoveUndo)edit);
            }
            else if( edit instanceof DataCollectionAddUndo )
            {
                redoAddEdit(diagram, (DataCollectionAddUndo)edit);
            }
            else if( edit instanceof StatePropertyChangeUndo )
            {
                redoPropertyChangeEdit(diagram, (StatePropertyChangeUndo)edit);
            }
        }
    }

    protected static void undoAddEdit(Diagram diagram, DataCollectionAddUndo edit) throws Exception
    {
        removeElement(diagram, edit.getDataElement());
    }

    protected static void redoAddEdit(Diagram diagram, DataCollectionAddUndo edit) throws Exception
    {
        addElement(diagram, edit.getDataElement());
    }

    protected static void undoRemoveEdit(Diagram diagram, DataCollectionRemoveUndo edit) throws Exception
    {
        addElement(diagram, edit.getDataElement());
    }

    protected static void redoRemoveEdit(Diagram diagram, DataCollectionRemoveUndo edit) throws Exception
    {
        removeElement(diagram, edit.getDataElement());
    }

    protected static void undoPropertyChangeEdit(Diagram diagram, StatePropertyChangeUndo edit) throws Exception
    {
        changeProperty(diagram, edit.getSource(), edit.getPropertyName(), edit.getOldValue());
    }

    protected static void redoPropertyChangeEdit(Diagram diagram, StatePropertyChangeUndo edit) throws Exception
    {
        changeProperty(diagram, edit.getSource(), edit.getPropertyName(), edit.getNewValue());
    }

    private static void changeProperty(Diagram diagram, Object oldSource, String propertyName, Object newValue) throws Exception
    {
        if( oldSource instanceof DataElement )
        {
            DataElement de = (DataElement)oldSource;
            String elementID = de.getName();
            if( de instanceof DiagramElement )
            {
                elementID = ( de instanceof Diagram ) ? "" : ( (DiagramElement)de ).getCompleteNameInDiagram();
            }

            Object source = diagram.getDiagramElement(elementID);
            if( source == null )
            {
                source = diagram.findObject(elementID);
            }
            if( source instanceof DiagramElement && de instanceof Base )
            {
                source = ( (DiagramElement)source ).getKernel();
            }
            Role role = diagram.getRole();
            if( source == null && role instanceof EModel ) //parameters do not have any links among diagram components
            {
                EModel model = (EModel)role;
                source = model.getParameters().get(elementID);
            }
            if( source != null )
            {
                if( newValue instanceof Role )
                {
                    newValue = ( (Role)newValue ).clone((DiagramElement)source);
                }
                try
                {
                    BeanUtil.setBeanPropertyValue( source, propertyName, newValue );
                }
                catch( IntrospectionException e )
                {
                    throw new InternalException( e );
                    // Ignore -- TODO: check this
                }
            }
        }
    }

    private static void removeElement(Diagram diagram, DataElement oldElement) throws Exception
    {
        if( oldElement instanceof DiagramElement )
        {
            DiagramElement de = (DiagramElement)oldElement;
            DiagramElement source = diagram.getDiagramElement(de.getCompleteNameInDiagram());
            if( source != null )
            {
                source.getOrigin().remove(source.getName());
            }
        }
    }

    private static void addElement(Diagram diagram, DataElement oldElement) throws Exception
    {
        if( oldElement instanceof DiagramElement )
        {
            DiagramElement de = (DiagramElement)oldElement;
            if( de.getParent() instanceof Compartment )
            {
                Compartment oldComp = de.getCompartment();
                Compartment newComp = (Compartment)diagram.findDiagramElement(oldComp.getCompleteNameInDiagram());
                if( newComp == null )
                    newComp = diagram;

                SemanticController sc = diagram.getType().getSemanticController();
                Point location = de instanceof Node ? ( (Node)de ).getLocation() : null;
                Object properties = de.getKernel() instanceof Reaction ? de.getKernel() : de.getRole();

                DiagramElement newElement = null;
                if( sc instanceof CreatorElementWithName )
                    newElement = ( (CreatorElementWithName)sc ).createInstance( newComp, de.getKernel().getClass(), de.getName(), location,
                            properties ).getElement();

                if( newElement == null )
                    newElement = de.clone( newComp, de.getName() );

                if( newElement != null && !newComp.contains( newElement ) && sc.canAccept( newComp, newElement ) )
                        newComp.put(newElement);
            }
        }
    }

    /**
     * @param state
     * @return native diagram to this state (i.e. diagram for which state was created initially) may return null.
     */
    public static Diagram getNativeDiagram(State state)
    {
        DynamicProperty dp = state.getAttributes().getProperty( State.DIAGRAM_REF );
        if (dp == null || dp.getType() != ru.biosoft.access.core.DataElementPath.class)
           return null;
        DataElement de = ( (DataElementPath)dp.getValue() ).optDataElement();
        return de instanceof Diagram ? (Diagram)de : null;
    }
}
