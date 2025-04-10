package biouml.standard.state;

import java.awt.Dimension;
import java.awt.Point;
import java.beans.IntrospectionException;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.UndoableEdit;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.undo.DataCollectionAddUndo;
import ru.biosoft.access.core.undo.DataCollectionRemoveUndo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.exception.InternalException;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.XmlStream;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.util.DiagramReader;
import biouml.model.util.DiagramWriter;
import biouml.model.util.XmlSerializationUtils;

import com.developmentontheedge.beans.undo.Transaction;
import com.developmentontheedge.beans.undo.TransactionEvent;
import com.developmentontheedge.beans.util.Beans.ObjectPropertyAccessor;

public class StateXmlSerializer
{
    protected static final Logger log = Logger.getLogger(StateXmlSerializer.class.getName());

    public static final String EXPERIMENT_ELEMENT = "experiment"; //old name for state
    public static final String STATE_ELEMENT = "state";
    public static final String PROPERTYCHANGE_ELEMENT = "propertyChange";
    public static final String DELETED_ELEMENT = "deleted";
    public static final String ADDED_ELEMENT = "added";

    public static final String ID_ATTR = "id";
    public static final String TITLE_ATTR = "title";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String VERSION_ATTR = "version";
    public static final String ELEMENTID_ATTR = "elementID";
    public static final String PROPERTY_ATTR = "property";
    public static final String NEWVALUE_ATTR = "newValue";
    public static final String OLDVALUE_ATTR = "oldValue";
    public static final String COMMENT_ATTR = "comment";
    public static final String COMPARTMENT_ATTR = "compartment";
    public static final String TRANSACTION_ELEMENT = "transaction";
    public static final String CLASS_ATTR = "type";

    public static Element[] getXmlElements(Diagram diagram, Document document, DiagramWriter diagramWriter)
    {
        String currentStateName = diagram.getCurrentStateName();
        List<Element> elements = new ArrayList<>();
        for( State state : diagram.states() )
        {
            diagram.setStateEditingMode(state);
            elements.add(getStateXmlElement(state, document, diagramWriter));
            diagram.restore();
        }
        if(currentStateName != null)
            diagram.setCurrentStateName(currentStateName);
        return elements.isEmpty() ? null : elements.toArray( new Element[elements.size()] );
    }

    public static Element getStateXmlElement(State state, Document document, DiagramWriter diagramWriter)
    {
        Element element = document.createElement(STATE_ELEMENT);

        element.setAttribute(ID_ATTR, state.getName());
        element.setAttribute(TITLE_ATTR, state.getTitle());

        String description = state.getDescription();
        if( description != null )
        {
            element.setAttribute(DESCRIPTION_ATTR, description);
        }
        String version = state.getVersion();
        if( version != null )
        {
            element.setAttribute(VERSION_ATTR, version);
        }

        List<UndoableEdit> edits = state.getStateUndoManager().getEdits();
        serializeEdits(diagramWriter, element, edits);

        return element;
    }

    /**
     * @param document
     * @param diagramWriter
     * @param element
     * @param edits
     */
    protected static void serializeEdits(DiagramWriter diagramWriter, Element element, List<UndoableEdit> edits)
    {
        if( edits != null )
        {
            Document document = element.getOwnerDocument();
            for( UndoableEdit ce : edits )
            {
                if( ce instanceof Transaction )
                {
                    Element transactionElement = document.createElement(TRANSACTION_ELEMENT);
                    transactionElement.setAttribute(TITLE_ATTR, ce.getPresentationName());
                    transactionElement.setAttribute(COMMENT_ATTR, ( (Transaction)ce ).getComment());
                    serializeEdits(diagramWriter, transactionElement, ( (Transaction)ce ).getEdits());
                    element.appendChild(transactionElement);
                }
                else if( ce instanceof DataCollectionAddUndo )
                {
                    Element newElement = getAddedElement((DataCollectionAddUndo)ce, document, diagramWriter);
                    if( newElement != null )
                    {
                        element.appendChild(newElement);
                    }
                }
                else if( ce instanceof DataCollectionRemoveUndo )
                {
                    element.appendChild(getDeletedElement((DataCollectionRemoveUndo)ce, document));
                }
                else if( ce instanceof StatePropertyChangeUndo )
                {
                    Element changedElement = getPropertyChangeElement((StatePropertyChangeUndo)ce, document);
                    if( changedElement != null )
                    {
                        element.appendChild(changedElement);
                    }
                }
            }
        }
    }

    protected static Element getPropertyChangeElement(StatePropertyChangeUndo propertyChange, Document document)
    {
        if( ! ( propertyChange.getSource() instanceof DataElement ) )
            return null;
        DataElement source = (DataElement)propertyChange.getSource();

        if( ( source instanceof DiagramElement ) && ( propertyChange.getPropertyName().equals("role") ) )
            return null;

        Object newValue = propertyChange.getNewValue();
        if( newValue == null )
            newValue = "";
        String newValueStr = getString(newValue);
        Object oldValue = propertyChange.getOldValue();
        if( oldValue == null )
            oldValue = "";
        String oldValueStr = getString(oldValue);

        Element result = document.createElement(PROPERTYCHANGE_ELEMENT);

        String elementId = source instanceof Diagram ? "" : ( (DiagramElement)source ).getCompleteNameInDiagram();

        result.setAttribute(ELEMENTID_ATTR, elementId);
        result.setAttribute(PROPERTY_ATTR, propertyChange.getPropertyName());

        result.setAttribute(NEWVALUE_ATTR, newValueStr);

        result.setAttribute(OLDVALUE_ATTR, oldValueStr);

        try
        {
            Class<?> propertyType = BeanUtil.getBeanPropertyType( source, propertyChange.getPropertyName() );
            if( !propertyType.isPrimitive() && !propertyType.equals(newValue.getClass()) )
                result.setAttribute(CLASS_ATTR, newValue.getClass().getName());
        }
        catch( Exception e )
        {
        }

        return result;
    }

    protected static Element getDeletedElement(DataCollectionRemoveUndo ce, Document document)
    {
        Element result = document.createElement(DELETED_ELEMENT);

        String elementId = ce.getDataElement().getName();
        if( ce.getDataElement() instanceof Node )
        {
            elementId = ( (Node)ce.getDataElement() ).getCompleteNameInDiagram();
        }
        result.setAttribute(ELEMENTID_ATTR, elementId);

        return result;
    }

    protected static Element getAddedElement(DataCollectionAddUndo ce, Document document, DiagramWriter diagramWriter)
    {
        Element result = document.createElement(ADDED_ELEMENT);
        DataElement de = ce.getDataElement();

        if( de.getOrigin() != null && ( de.getOrigin() instanceof Compartment ) && ! ( de.getOrigin() instanceof Diagram ) )
        {
            String name = ( (DiagramElement)de ).getCompleteNameInDiagram();
            if( name.indexOf('.') != -1 )
            {
                name = name.substring(0, name.indexOf('.'));
            }
            result.setAttribute(COMPARTMENT_ATTR, name);
        }

        if( de instanceof Node )
        {
            diagramWriter.writeNode(result, (Node)de);
        }
        else if( de instanceof Edge )
        {
            diagramWriter.writeEdge(result, (Edge)de);
        }

        if( result.getFirstChild() == null )
            return null;
        return result;
    }
    private static String getString(Object value)
    {
        if( value != null )
        {
            Class<?> type = value.getClass();
            if( Pen.class.equals( type ) )
            {
                return XmlSerializationUtils.getPenString( (Pen)value );
            }
            else if( Brush.class.equals( type ) )
            {
                return XmlSerializationUtils.getBrushString( (Brush)value );
            }
            else if( Dimension.class.equals( type ) )
            {
                return XmlSerializationUtils.getDimensionString( (Dimension)value );
            }
            else if( Point.class.equals( type ) )
            {
                return XmlSerializationUtils.getPointString( (Point)value );
            }
            if( value instanceof Role ) // TODO: support non-default role values
            {
                return "role";
            }
        }
        return TextUtil2.toString(value);
    }

    public static State readXmlElement(Element element, Diagram diagram, DiagramReader diagramReader)
    {
        return readXmlElement(null, element, diagram, diagramReader);
    }
    
    public static State readXmlElement(DataCollection<?> origin, Element element, Diagram diagram, DiagramReader diagramReader)
    {
        if( !element.getNodeName().equals(STATE_ELEMENT) && !element.getNodeName().equals(EXPERIMENT_ELEMENT) )
            return null;

        String id = element.getAttribute(ID_ATTR);
        State state = new State(origin, diagram,  id);
        diagram.setStateEditingMode(state);
        boolean isNotify = diagram.isNotificationEnabled();
        
        EModel emodel = null;
        boolean isModelNotify = true;
        if (diagram.getRole() instanceof EModel)
        {
            emodel = diagram.getRole(EModel.class);
            isModelNotify = emodel.isNotificationEnabled();
            emodel.setNotificationEnabled( true );
        }
        diagram.setNotificationEnabled(true);
        
        fillStateFromElement(element, state, diagram, diagramReader);
        diagram.restore();
        diagram.setNotificationEnabled(isNotify);
        if (emodel != null)
        {
            emodel.setNotificationEnabled( isModelNotify );
        }
        return state;
    }

    public static void fillStateFromElement(Element element, State state, Diagram diagram, DiagramReader diagramReader)
    {
        String title = element.getAttribute(TITLE_ATTR);
        if( title.isEmpty() )
        {
            title = state.getName();
        }
        state.setTitle(title);
        state.setDescription(element.getAttribute(DESCRIPTION_ATTR));
        state.setVersion(element.getAttribute(VERSION_ATTR));

        readTransaction(element, state, diagram, diagramReader);
    }

    /**
     * @param element
     * @param state
     * @param diagram
     * @param diagramReader
     */
    protected static void readTransaction(Element element, State state, Diagram diagram, DiagramReader diagramReader)
    {
        for( Element changeElement : XmlStream.elements( element ) )
        {
            if( changeElement.getTagName().equals(TRANSACTION_ELEMENT) )
            {
                String name = changeElement.getAttribute(TITLE_ATTR);
                String comment = changeElement.getAttribute(COMMENT_ATTR);
                state.startTransaction(new TransactionEvent(state, name));
                state.setTransactionComment(comment);
                readTransaction(changeElement, state, diagram, diagramReader);
                state.completeTransaction();
            }
            if( changeElement.getTagName().equals(DELETED_ELEMENT) )
            {
                readDeleted(changeElement, diagram);
            }
            else if( changeElement.getTagName().equals(ADDED_ELEMENT) )
            {
                readAdded(changeElement, diagram, diagramReader);
            }
            else if( changeElement.getTagName().equals(PROPERTYCHANGE_ELEMENT) )
            {
                readPropertyChanged(changeElement, diagram);
            }
        }
    }

    protected static void readPropertyChanged(Element element, Diagram diagram)
    {
        String elementID = element.getAttribute(ELEMENTID_ATTR);
        String propertyName = element.getAttribute(PROPERTY_ATTR);
        String newValue = element.getAttribute(NEWVALUE_ATTR);
        String typeStr = element.getAttribute(CLASS_ATTR);

        try
        {
            Class<?> type = ( typeStr == null || typeStr.isEmpty() ) ? null : ClassLoading.loadClass( typeStr );
            Object source = diagram.getDiagramElement(elementID);
            if( source == null )
            {
                source = diagram.findObject(elementID);
            }
            if( source == null && diagram.getRole() instanceof EModel ) //parameters do not have any links among diagram components
            {
                EModel model = diagram.getRole(EModel.class);
                source = model.getParameters().get(elementID);
            }
            if( source != null )
            {
                try
                {
                    ObjectPropertyAccessor accessor = BeanUtil.getBeanPropertyAccessor( source, propertyName );
                    Object newObject = getObject( newValue, type == null ? accessor.getType() : type, source );
                    if(newObject != null || newValue == null)
                    {
                        accessor.set( newObject );
                    }
                }
                catch( IntrospectionException e )
                {
                    throw new InternalException( e );
                    // Ignore
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "change property failed, can't find element '" + elementID + "'", t);
        }
    }

    private static Object getObject(String value, Class<?> type, Object source)
    {
        if( type == null )
            return null;
        if( Pen.class.equals( type ) )
        {
            return XmlSerializationUtils.readPen( value );
        }
        else if( Brush.class.equals( type ) )
        {
            return XmlSerializationUtils.readBrush( value );
        }
        else if( Dimension.class.equals( type ) )
        {
            return XmlSerializationUtils.readDimension( value );
        }
        else if( Point.class.equals( type ) )
        {
            return XmlSerializationUtils.readPoint( value );
        }
        else if( Role.class.isAssignableFrom( type ) && source instanceof DiagramElement )
        {
            try
            {
                return type.getConstructor( DiagramElement.class ).newInstance( (DiagramElement)source );
            }
            catch( Exception e )
            {
                return null;
            }
        }
        return TextUtil2.fromString( type, value );
    }

    protected static void readDeleted(Element element, Diagram diagram)
    {
        String elementID = element.getAttribute(ELEMENTID_ATTR);

        try
        {
            Object source = diagram.getDiagramElement(elementID);
            if( source == null )
            {
                source = diagram.findObject(elementID);
            }
            if( source instanceof DiagramElement )
            {
                DiagramElement de = (DiagramElement)source;
                diagram.getType().getSemanticController().remove(de);
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't read deleted element '" + elementID + "' for state", t);
        }
    }

    protected static void readAdded(Element element, Diagram diagram, DiagramReader diagramReader)
    {
        try
        {
            String compartmentId = element.getAttribute(COMPARTMENT_ATTR);
            DiagramElement de = diagram.findDiagramElement(compartmentId);

            Compartment compartment = null;
            if( de != null && de instanceof Compartment )
                compartment = (Compartment)de;
            else
                compartment = diagram;

            diagramReader.readNodes(element, compartment);
            diagramReader.readEdges(element, compartment);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not read added element in state", e);
        }
    }
}
