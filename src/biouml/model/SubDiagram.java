package biouml.model;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Optional;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;

import com.developmentontheedge.beans.awt.infos.ColorMessageBundle;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
* New version of subdiagram element
* @author Ilya
*/
@SuppressWarnings ( "serial" )
@PropertyName("Subdiagram")
@PropertyDescription("Element containing diagram to be included into another diagram.")
public class SubDiagram extends DiagramContainer
{
    public static final String ORIGINAL_PORT_ATTR = "originalPort";
    public static final String RELATIVE_SUBDIAGRAM = "relativeSubDiagram";
    public static final String SUBDIAGRAM_STATE_NAME = "SubdiagramState";
    public static final String SUBDIAGRAM_IS_MUTABLE = "subDiagramIsMutable";

    private static final String _DIAGRAM_ = "_diagram_";
    private final DiagramPropertyChangeListener listener;
    private DataElementPath diagramPath;

    @Override
    public DiagramElement get(String name)
    {
        return name.equals(_DIAGRAM_) ? diagram : super.get(name);
    }

    public SubDiagram(DataCollection<?> origin, Diagram diagram, String name) throws Exception
    {
        super(origin, diagram, new Stub.SubDiagramKernel(origin, name));
        
        synchronized( diagram )
        {
            //check if it is in repository
            diagramPath = diagram.getOrigin() instanceof TransformedDataCollection || diagram.getOrigin() instanceof FolderCollection
                    ? diagram.getCompletePath() : null;
            setTitle( name.equals( diagram.getName() ) ? name : name + " (" + diagram.getName() + ")" );

            attributes.add( DPSUtils.createHiddenReadOnlyTransient( Node.INNER_NODES_PORT_FINDER_ATTR, Boolean.class, true ) );

            State curState = diagram.getCurrentState();
            boolean isNodtificationEnabled = diagram.isNotificationEnabled();
            diagram.setNotificationEnabled( false );
            if( curState != null )
                diagram.restore();

            this.diagram = diagram.clone( diagram.getOrigin(), diagram.getName() );
            if( !this.diagram.getStateNames().contains( SUBDIAGRAM_STATE_NAME ) )
                this.diagram.addState( new State( null, this.diagram, SUBDIAGRAM_STATE_NAME ) );

            this.diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( RELATIVE_SUBDIAGRAM, SubDiagram.class, this ) );

            if( curState != null )
                this.diagram.setCurrentStateName( curState.getName() );

            listener = new DiagramPropertyChangeListener();
            this.diagram.addPropertyChangeListener( listener );
            this.diagram.addDataCollectionListener( listener );

            if( curState != null )
                diagram.setStateEditingMode( curState );
            diagram.setNotificationEnabled( isNodtificationEnabled );
            updatePorts();
        }
    }

    public String getStateName()
    {
        return this.diagram.getCurrentStateName();
    }
    public void setStateName(String stateName)
    {
        String oldValue = getStateName();
        diagram.setCurrentStateName(stateName);
        this.firePropertyChange("stateName", oldValue, stateName);
    }

    public State getState()
    {
        return diagram.getCurrentState();
    }
    public void setState(State state)
    {
        diagram.setStateEditingMode(state);
    }

    public void setDiagram(Diagram diagram) throws Exception
    {
        this.diagram.removeDataCollectionListener(listener);
        this.diagram.removePropertyChangeListener(listener);
        this.diagram.getAttributes().remove(RELATIVE_SUBDIAGRAM);
        diagramPath = diagram.getOrigin() instanceof TransformedDataCollection || diagram.getOrigin() instanceof FolderCollection
                ? diagram.getCompletePath() : null;

        this.diagram = diagram.clone(diagram.getOrigin(), diagram.getName());
        this.diagram.addPropertyChangeListener(listener);
        this.diagram.addDataCollectionListener(listener);
        this.diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient(RELATIVE_SUBDIAGRAM, SubDiagram.class, this));
        updatePorts(location);
    }

    public String getDiagramPath()
    {
        return diagramPath != null ? diagramPath.toString() : getCompletePath().getChildPath(_DIAGRAM_).toString();
    }

    public class DiagramPropertyChangeListener implements PropertyChangeListener, DataCollectionListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
//            SubDiagram.this.refresh();
            if( evt.getPropertyName().equals("currentStateName") )
            {
                boolean oldValue = isDiagramMutable;
                isDiagramMutable = SUBDIAGRAM_STATE_NAME.equals(evt.getNewValue().toString());
                diagram.firePropertyChange(new PropertyChangeEvent(SubDiagram.this, SUBDIAGRAM_IS_MUTABLE, oldValue, isDiagramMutable));
            }
        }

        @Override
        public void elementAdded(DataCollectionEvent e) throws Exception
        {
            SubDiagram.this.refresh();
        }

        @Override
        public void elementWillAdd(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
            SubDiagram.this.refresh();
        }

        @Override
        public void elementChanged(DataCollectionEvent e) throws Exception
        {
            SubDiagram.this.refresh();
        }

        @Override
        public void elementWillChange(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
            SubDiagram.this.refresh();
        }

        @Override
        public void elementRemoved(DataCollectionEvent e) throws Exception
        {
            SubDiagram.this.refresh();
        }

        @Override
        public void elementWillRemove(DataCollectionEvent e) throws DataCollectionVetoException, Exception
        {
            SubDiagram.this.refresh();
        }
    }

    public void refresh()
    {
        try
        {
            this.updatePorts();
            this.setView(null);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during subdiagram refreshing: " + ex.getMessage());
        }
    }

    public void updatePorts()
    {
        updatePorts(getLocation());
    }

    /**
     * Synchronizing ports in module and diagram associated with it
     * Keeps all common ports to save their locations in subdiagram compartment
     * @throws Exception
     */
    public void updatePorts(Point pt)
    {
        List<Node> deletedPorts = new ArrayList<>();
        for( Node node : getNodes() )
        {
            DynamicProperty dp = node.getAttributes().getProperty(ORIGINAL_PORT_ATTR);
            if( dp == null )
            {
                //try to find explicitly
                String variable = Util.getPortVariable(node);
                boolean containPort = diagram.recursiveStream().select(Node.class).filter(n -> Util.isPort(n))
                        .filter(n -> !Util.isPrivatePort(n)).map(n -> Util.getPortVariable(n)).has(variable);
                if( !containPort )
                    deletedPorts.add(node);
            }
            else
            {
                String originalPortName = dp.getValue().toString();
                Node originalPort = originalPortName == null ? null : diagram.findNode( originalPortName );
                if( originalPort == null ) //port was deleted from diagram
                {
                    deletedPorts.add(node);
                }
                else
                {
                    //sync variables
                    if( !originalPort.getKernel().getType().equals(node.getKernel().getType()) )
                    {
                        deletedPorts.add(node);
                    }
                    else
                    {
                        String portVariable = Util.getPortVariable(originalPort);
                        if( portVariable != null )
                            Util.setPortVariable(node, portVariable);
                    }
                }
            }
        }

        //deleting ports which are in module but not in diagram
        for( Node deletedPort : deletedPorts )
            forceRemove(deletedPort); // subdiagram ports may not be removed through semantic controller
        //otherwise it will be possible for user to delete them manually from module.

        //adding ports which are in diagram but not in module
        for( Node originalPortNode : Util.getPorts(diagram) )
        {
            if( !contains(originalPortNode.getName()) && !Util.isPrivatePort(originalPortNode) )
                createPort(originalPortNode, this, pt);
        }
    }

    private void forceRemove(DiagramElement de)
    {
        if( de instanceof Edge )
            ( (Edge)de ).nodes().forEach(n -> n.removeEdge((Edge)de));
        else if( de instanceof Node )
        {
            for( Edge e : ( (Node)de ).getEdges() )
                forceRemove(e);
        }

        DataCollection<?> origin = de.getOrigin();
        try
        {
            if( origin != null )
                origin.remove(de.getName());
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not remove port "+de.getName()+" :"+e.getMessage() );
        }
    }


    public static Node createPort(Node node, SubDiagram subDiagram, Point pt)
    {
        try
        {
            Node newNode = node.clone(subDiagram, node.getName());
            newNode.setLocation(pt); //default location: upper-left corner of subdiagram compartment
            newNode.getAttributes().add(DPSUtils.createHiddenReadOnly(ORIGINAL_PORT_ATTR, String.class, node.getCompleteNameInDiagram()));
            DynamicProperty dp = newNode.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR);
            if( dp != null )
                dp.setReadOnly(true);
            newNode.save();
            newNode.setVisible(subDiagram.isVisible());
            return newNode;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not create port " + node.getName() + " :" + e.getMessage());
            return null;
        }
    }

    @Override
    public @Nonnull SubDiagram clone(Compartment newParent, String newName)
    {
        return clone( newParent, newName, getKernel() );
    }

    @Override
    public @Nonnull SubDiagram clone(Compartment newParent, String newName, Base kernel)
    {
        if( newParent == this )
            throw new IllegalArgumentException("Can not clone compartment into itself, compartment=" + newParent);

        try
        {
            SubDiagram result = new SubDiagram(newParent, this.getDiagram(), newName);
            State state = this.getDiagram().getCurrentState();
            if( state != null )
                result.getDiagram().setCurrentStateName(state.getName());
            result.setNotificationEnabled(false);
            doClone(result);
            result.setNotificationEnabled(isNotificationEnabled());
            return result;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error while subdiagram " + this.getName() + " cloning: " + ex.getMessage());
            throw ExceptionRegistry.translateException( ex );
        }

    }


    /**
     * Possible port orientations
     */
    public static enum PortOrientation
    {
        LEFT, RIGHT, TOP, BOTTOM;

        /**
         * Orientation default property name
         */
        public static final String ORIENTATION_ATTR = "orientation";

        /**
         * Get {@link String} for current object value
         */
        @Override
        public String toString()
        {
            return name().toLowerCase();
        }

        public String toDirection()
        {
            switch( this )
            {
                case LEFT:
                    return "west";
                case RIGHT:
                    return "east";
                case TOP:
                    return "north";
                case BOTTOM:
                    return "south";
                default:
                    throw new InternalException();
            }
        }

        public boolean isVertical()
        {
            return equals(TOP) || equals(BOTTOM);
        }

        public PortOrientation opposite()
        {
            switch( this )
            {
                case TOP:
                    return BOTTOM;
                case BOTTOM:
                    return TOP;
                case LEFT:
                    return RIGHT;
                default:
                    return LEFT;
            }
        }

        public PortOrientation clockwise()
        {
            switch(this)
            {
                case TOP:
                    return RIGHT;
                case BOTTOM:
                    return LEFT;
                case LEFT:
                    return TOP;
                default:
                    return BOTTOM;
            }
        }

        public static PortOrientation createInstance(String value)
        {
            return getOrientation(value);
        }

        /**
         * Get {@link PortOrientation} object by {@link String}
         */
        public static PortOrientation getOrientation(String value)
        {
            switch( value )
            {
                case "left":
                    return PortOrientation.LEFT;
                case "right":
                    return PortOrientation.RIGHT;
                case "top":
                    return PortOrientation.TOP;
                case "bottom":
                    return PortOrientation.BOTTOM;
                default:
                    return RIGHT;//default orientation
            }
        }
    }

    public static class PortOrientationBeanInfo extends BeanInfoEx
    {
        public PortOrientationBeanInfo()
        {
            super(PortOrientation.class, ColorMessageBundle.class.getName());
            beanDescriptor.setDisplayName(getResourceString("DISPLAY_NAME"));
            beanDescriptor.setShortDescription(getResourceString("SHORT_DESCRIPTION"));
            setSimple(true);
            setBeanEditor(PortOrientationEditor.class);
        }
    }

    public static class OrientationEditorMessageBundle extends ListResourceBundle
    {
        @Override
        protected Object[][] getContents()
        {
            return new Object[][] {{"DISPLAY_NAME", "Orientation"}, {"SHORT_DESCRIPTION", "Orientation property"},};
        }
    }// end of class MessagesBundle

    public static class PortOrientationEditor extends GenericComboBoxEditor
    {
        @Override
        public Object[] getAvailableValues()
        {
            return new Object[] {PortOrientation.LEFT, PortOrientation.RIGHT, PortOrientation.TOP, PortOrientation.BOTTOM};
        }
    }

    private boolean isDiagramMutable = false;

    @Override
    public boolean isDiagramMutable()
    {
        return isDiagramMutable;
    }

    /**
     * @param diagram
     * @return SubDiagram which encapsulates current diagram or null if diagram does not belong to subDiagram
     */
    public static SubDiagram getParentSubDiagram(Diagram diagram)
    {
        try
        {
            DynamicProperty dp = diagram.getAttributes().getProperty(RELATIVE_SUBDIAGRAM);
            return (dp != null && dp.getValue() instanceof SubDiagram)? (SubDiagram)dp.getValue(): null;
        }
        catch( Exception ex )
        {
            return null;
        }
    }

    /**
     * @param diagram
     * @return Stream of given diagram and Diagrams linked to it via SubDiagram elements (if any)
     */
    public static StreamEx<Diagram> diagrams(Diagram diagram)
    {
        return diagram.recursiveStream().select(SubDiagram.class).map(SubDiagram::getDiagram).prepend(diagram);
    }

    public Optional<Node> findPort(String variableName)
    {
        return stream(Node.class).findAny(de->Util.isPort(de) && variableName.equals(Util.getPortVariable(de)));
    }
}
