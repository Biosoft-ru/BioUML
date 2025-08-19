package biouml.model.xml;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.swing.JFrame;

import java.util.logging.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaPackage;
import org.mozilla.javascript.ScriptRuntime;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.application.Application;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.support.IdGenerator;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.util.DiagramDependentProperty;
import biouml.standard.diagram.CreateDiagramElementDialog;
import biouml.standard.diagram.CreateEdgeDialog;
import biouml.standard.diagram.CreateReactionDialog;
import biouml.standard.diagram.MessageBundle;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class XmlDiagramSemanticController extends DefaultSemanticController
{
    protected static final Logger log = Logger.getLogger( XmlDiagramSemanticController.class.getName() );

    protected SemanticController prototype = new DefaultSemanticController();
    public SemanticController getPrototype()
    {
        return prototype;
    }

    public void setPrototype(SemanticController prototype)
    {
        this.prototype = prototype;
    }

    protected MessageBundle messageBundle = new MessageBundle();
    protected XmlDiagramType xmlDiagramType;

    public XmlDiagramSemanticController(XmlDiagramType diagramType)
    {
        this.xmlDiagramType = diagramType;
    }

    private final Map<String, Context> contextMap = new HashMap<>();
    public Context getContext()
    {
        String threadKey = Thread.currentThread().getName();
        Context context;

        if( contextMap.containsKey( threadKey ) )
        {
            context = contextMap.get( threadKey );
        }
        else
        {
            context = Context.enter();
            context.initStandardObjects( getScope() );
            contextMap.put( threadKey, context );

            try
            {
                ImporterTopLevel scope = getScope();
                scope.put( "prototype", scope, Context.toObject( prototype, scope ) );

                //          import packages
                //          TODO: import packages by another way
                for( String packageName : XmlDiagramType.IMPORT_PACKAGES )
                {
                    NativeJavaPackage pack = new NativeJavaPackage( packageName, this.getClass().getClassLoader() );
                    ScriptRuntime.setObjectProtoAndParent( pack, scope );
                    scope.importPackage( null, null, new Object[] {pack}, null );
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Can not initialise JavaScript context for Xml Diagram Semantic Controller" );
            }
        }

        return context;
    }

    private final Map<String, ImporterTopLevel> scopeMap = new HashMap<>();
    public ImporterTopLevel getScope()
    {
        String threadKey = Thread.currentThread().getName();
        ImporterTopLevel scope;
        if( scopeMap.containsKey( threadKey ) )
        {
            scope = scopeMap.get( threadKey );
        }
        else
        {
            scope = new ImporterTopLevel();
            scopeMap.put( threadKey, scope );
        }

        return scope;
    }

    protected Function canAcceptFunction = null;
    protected String canAcceptCode = null;

    public void setCanAcceptFunction(String script)
    {
        canAcceptFunction = getContext().compileFunction( getScope(), script, "canAccept function", 0, null );
        canAcceptCode = script;
    }

    public String getCanAcceptCode()
    {
        return canAcceptCode;
    }

    @Override
    public boolean canAccept(Compartment compartment, DiagramElement de)
    {
        if( canAcceptFunction != null )
        {
            /*try
            {
                prototype.validate(compartment, de);
            }
            catch( Exception e )
            {
                return false;
            }*/
            try
            {
                Object functionArgs[] = {new DiagramElementJScriptWrapper( compartment ), new DiagramElementJScriptWrapper( de )};
                Boolean result = (Boolean)canAcceptFunction.call( getContext(), getScope(), getScope(), functionArgs );
                return result.booleanValue();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE,  "Can not accept element " + de.getName() + " to compartment " + compartment.getName() + ": " + t );
            }
        }
        return prototype.canAccept( compartment, de );
    }

    protected Function isResizableFunction = null;
    protected String isResizableCode = null;

    public void setIsResizableFunction(String script)
    {
        isResizableFunction = getContext().compileFunction( getScope(), script, "isResizable function", 0, null );
        isResizableCode = script;
    }

    public String getIsResizableCode()
    {
        return isResizableCode;
    }

    @Override
    public boolean isResizable(DiagramElement diagramElement)
    {
        if( diagramElement instanceof SubDiagram )
        {
            return true;
        }
        if( diagramElement instanceof Diagram )
        {
            return false;
        }
        if( isResizableFunction != null )
        {
            try
            {
                Object functionArgs[] = {new DiagramElementJScriptWrapper( diagramElement )};
                return (Boolean)isResizableFunction.call( getContext(), getScope(), getScope(), functionArgs );
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE,  "Can not execute isResizable, JavaScript function=" + isResizableFunction + ", error=" + t, t );
            }
        }
        return prototype.isResizable( diagramElement );
    }

    protected Function moveFunction = null;
    protected String moveCode = null;
    public void setMoveFunction(String script)
    {
        moveFunction = getContext().compileFunction( getScope(), script, "move function", 0, null );
        moveCode = script;
    }
    public String getMoveCode()
    {
        return moveCode;
    }

    private boolean moveFunctionEnabled = true;
    public boolean isMoveFunctionEnabled()
    {
        return moveFunctionEnabled;
    }
    public void enableMoveFunction()
    {
        this.moveFunctionEnabled = true;
    }
    public void disableMoveFunction()
    {
        this.moveFunctionEnabled = false;
    }

    @Override
    public Dimension move(DiagramElement de, Compartment newParent, Dimension offset, Rectangle oldBounds) throws Exception
    {
        if( isMoveFunctionEnabled() && moveFunction != null )
        {
            if( de instanceof Node )
            {
                Node oldNode = (Node)de;

                try
                {
                    Node newNode = (Node)prototype.validate( newParent, oldNode );
                    Point location = oldNode.getLocation();

                    Compartment parent = (Compartment)oldNode.getOrigin();
                    if( oldNode == newParent )
                        newParent = (Compartment)oldNode.getOrigin();

                    //TODO: find out why this was here
//                    if( location.x == 0 && location.y == 0 && ! ( parent instanceof Diagram ) )
//                    {
//                        location = parent.getLocation();
//                    }
                    location.translate( offset.width, offset.height );

                    if( newParent != parent )
                    {
                        if( newParent.get( oldNode.getName() ) != null )
                            throw new Exception( ERROR_NODE_IS_DUPLICATED );
                        else if( canAccept( newParent, newNode ) )
                        {
                            newNode = changeNodeParent( newNode, newParent );
                        }
                        else
                        {
                            return new Dimension( 0, 0 );
                        }
                    }


                    Object functionArgs[] = {location, new DiagramElementJScriptWrapper( newNode ),
                            new DiagramElementJScriptWrapper( newParent )};
                    moveFunction.call( getContext(), getScope(), getScope(), functionArgs );

                    newNode.setLocation( location );
                    if( newNode instanceof Compartment /*&& ! ( newNode instanceof SubDiagram )*/ )
                    {
                        for(Node element: ((Compartment)newNode).stream( Node.class ))
                        {
                            move( element, element.getCompartment(), offset, null );
                        }
                    }

                    newNode.edges().forEach( this::recalculateEdgePath );

                    return offset;
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE,  "Can not move element: " + t );
                }
            }
        }
        return prototype.move( de, newParent, offset, oldBounds );
    }

    protected Function removeFunction = null;
    protected String removeCode = null;
    public void setRemoveFunction(String script)
    {
        removeFunction = getContext().compileFunction( getScope(), script, "remove function", 0, null );
        removeCode = script;
    }
    public String getRemoveCode()
    {
        return removeCode;
    }

    @Override
    public boolean remove(DiagramElement de) throws Exception
    {
        if( removeFunction != null )
        {
            try
            {
                Object functionArgs[] = {new DiagramElementJScriptWrapper( de )};
                return (Boolean)removeFunction.call( getContext(), getScope(), getScope(), functionArgs );
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE,  "Can not remove element: " + t );
            }
        }

        boolean result = super.remove( de );
        prototype.validate( (Compartment)de.getOrigin(), de );
        return result;
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        String typeStr = xmlDiagramType.getKernelTypeName( type );
        Diagram diagram = Diagram.getDiagram( parent );
        if( typeStr != null )
        {
            boolean isCompartment = false;

            if( xmlDiagramType.isCreateByPrototype( typeStr ) )
            {
                Object obj = xmlDiagramType.getKernelType( typeStr );
                if( obj instanceof Class )
                {
                    try
                    {
                        Class<?> clazz = (Class<?>)obj;
                        DiagramElement de = prototype.createInstance( parent, clazz, point, viewEditor ).get( 0 );
                        return ( de == null ) ? null : new DiagramElementGroup( prototype.validate( parent, de ) );
                    }
                    catch( Exception ex )
                    {
                        log.log(Level.SEVERE,  "Error during element creation by prototype: type = " + type+" error: "+ExceptionRegistry.translateException( ex) );
                    }
                }
            }

            if( xmlDiagramType.checkCompartment( typeStr ) )
            {
                isCompartment = true;
            }

            boolean isNode = Arrays.asList( xmlDiagramType.getNodes() ).contains( typeStr );
            boolean isReaction = Arrays.asList( xmlDiagramType.getReactionTypes() ).contains( typeStr );
            if( isNode )
            {
                try
                {
                    boolean standardDialog = false;
                    if( type instanceof Class )
                    {
                        Module module = Module.optModule( parent );
                        if( module != null )
                        {
                            if( module.getCategory( ((Class<?>)type).asSubclass(DataElement.class) ) != null )
                            {
                                standardDialog = true;
                            }
                        }
                    }
                    if( standardDialog )
                    {
                        CreateDiagramElementDialog dialog = new CreateDiagramElementDialog( parent, ((Class<?>)type).asSubclass(DataElement.class) );
                        if( dialog.doModal() )
                        {
                            DiagramElement diagramElement = dialog.getNode();
                            DynamicPropertySet attributes = createAttributes( typeStr, diagram );
                            for( DynamicProperty attribute : attributes )
                            {
                                diagramElement.getAttributes().add(attribute);
                            }
                            if (isNode)
                            {
                                ((Node)diagramElement).setLocation( point );
                            }
                            DiagramElement validatedNode = prototype.validate( parent, diagramElement, true );
                            return new DiagramElementGroup( validatedNode );
                        }
                    }
                    else
                    {
                        DynamicPropertySet attributes = createAttributes( typeStr, diagram );
                        XmlCreateDiagramElementDialog dialog = new XmlCreateDiagramElementDialog( "New element", parent, typeStr,
                                isCompartment, attributes );
                        if( !dialog.doModal() )
                            return null;
                        DiagramElement diagramElement = dialog.getNewDiagramElement();
                        if( isNode )
                        {
                            ( (Node)diagramElement ).setLocation( point );
                        }
                        DiagramElement validatedNode = prototype.validate( parent, diagramElement, true );
                        return new DiagramElementGroup( validatedNode );
                    }
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE,  "Can not create new node element: " + t );
                }
                return null;
            }
            else if( isReaction )
            {
                XmlReactionPane reactionPane = new XmlReactionPane( diagram, parent, point, viewEditor, typeStr, ( (Class<?>)type ).getName(),
                        createAttributes( typeStr, diagram ) );
                JFrame frame = Application.getApplicationFrame();
                CreateReactionDialog dialog = new CreateReactionDialog( frame, reactionPane );
                dialog.pack();
                dialog.setVisible( true );
                return null;
            }
            else
            {
                try
                {
                    String name = generateUniqueNodeName( parent, typeStr );
                    CreateEdgeDialog dialog = CreateEdgeDialog.getSimpleEdgeDialog( Module.optModule( parent ), point, viewEditor, name,
                            typeStr, createAttributes( typeStr, diagram ) );
                    dialog.setVisible( true );
                    return null;
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE,  "Can not create new diagram element, error: " + t, t );
                }
                return null;
            }
        }
        return null;
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, Object properties)
    {
        DiagramElement de = prototype.createInstance( parent, type, point, properties ).getElement();
        DynamicProperty property = de.getAttributes().getProperty( XmlDiagramTypeConstants.XML_TYPE );
        if( property != null )
        {
            String typeStr = (String)property.getValue();
            for(DynamicProperty attribute: createAttributes( typeStr, Diagram.getDiagram( parent ) ))
            {
                de.getAttributes().add( attribute );
            }
        }
        return new DiagramElementGroup( de );
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        Object bean = xmlDiagramType.getPropertiesBean(type.toString());
        if( bean != null)
        {
            if(bean instanceof InitialElementProperties)
                return  bean;
            else
            {
                XmlBeanInitialProperties properties = new XmlBeanInitialProperties();
                String idFormat = xmlDiagramType.getIdFormat( type.toString() );
                if( idFormat != null )
                {
                    properties.setName( IdGenerator.generateUniqueName( compartment, new DecimalFormat( idFormat ) ) );
                }
                properties.setAttributes( createAttributes(type.toString()) );
                return properties;
            }
        }
        return prototype.getPropertiesByType( compartment, type, point );
    }

    public @Nonnull DynamicPropertySet createAttributes(String typeStr)
    {
        return createAttributes(typeStr, null);
    }

    public @Nonnull DynamicPropertySet createAttributes(String typeStr, Diagram diagram)
    {
        DynamicPropertySet result = new DynamicPropertySetAsMap();
        DynamicPropertySet properties = xmlDiagramType.getType( typeStr );
        if( properties != null )
        {
            for(DynamicProperty property: properties)
            {
                String dpName = property.getName();
                DynamicProperty dp = xmlDiagramType.getProperties().getProperty( dpName );
                try
                {
                    Class<?> pType = dp.getType();
                    if( pType.isArray() )
                    {
                        DynamicProperty[] items = (DynamicProperty[])dp.getValue();
                        if( items.length > 0 )
                        {
                            DynamicProperty dpItem = items[0];
                            Object[] values = (Object[])Array.newInstance( dpItem.getType(), 0 );
                            result.add( new DynamicProperty( dp.getName(), values.getClass(), values ) );
                        }
                    }
                    else
                    {
                        Object dpValue = dp.getValue();
                        if (dpValue instanceof DiagramDependentProperty)
                        {
                            ((DiagramDependentProperty)dpValue).setDiagram(diagram);
                        }
                        DynamicProperty newProperty = new DynamicProperty( dp.getName(), pType, dpValue );

                        Object tagValues = dp.getDescriptor().getValue( StringTagEditor.TAGS_KEY );
                        if( tagValues instanceof String[] )
                        {
                            newProperty.getDescriptor().setValue( StringTagEditor.TAGS_KEY, tagValues );
                            newProperty.getDescriptor().setPropertyEditorClass( TestTagEditor.class );
                            newProperty.setValue( ( (String[])tagValues )[0] );
                            if( dp.getValue() != null && !dp.getValue().equals( "" ) )
                            {
                                String value = dp.getValue().toString();
                                if(Arrays.asList( (String[])tagValues ).contains( value ))
                                    newProperty.setValue( value );
                            }

                        }
                        result.add( newProperty );
                    }
                }
                catch( Throwable t )
                {
                }
            }
        }
        return result;
    }

    public static class TestTagEditor extends com.developmentontheedge.beans.editors.StringTagEditor
    {
        public TestTagEditor()
        {
            super();
        }
    }

    @Override
    public DiagramElementGroup createInstanceFromElement(Compartment compartment, DataElement element, Point point, ViewEditorPane viewEditor)
            throws Exception
    {
        DiagramElementGroup result = super.createInstanceFromElement( compartment, element, point, viewEditor );
        DiagramElementGroup validated = new DiagramElementGroup();
        for( DiagramElement de : result.getElements() )
        {
            validated.add( getPrototype().validate( compartment, de ) );
        }
        return validated;
    }


    @Override
    protected Node changeNodeParent(Node oldNode, Compartment newParent) throws Exception
    {
        Node node = super.changeNodeParent( oldNode, newParent );

        //adjust specie reference
        if( node.getKernel() instanceof Reaction )
            return node;

        for( Edge edge : node.getEdges() )
        {
            if( edge.getKernel() instanceof SpecieReference )
            {
                SpecieReference ref = (SpecieReference)edge.getKernel();
                ref.setSpecie( node.getCompleteNameInDiagram() );
            }
        }
        return node;
    }

    @Override
    public DiagramElement validate(Compartment compartment, @Nonnull DiagramElement de, boolean newElement) throws Exception
    {
        return prototype.validate( compartment, de, newElement );
    }

    @Override
    public Edge createEdge(Node fromNode, Node toNode, String edgeType, Compartment compartment)
    {
        Edge edge = super.createEdge( fromNode, toNode, edgeType, compartment );
        if( edge != null )
        {
            String xmlKernelType = "logic arc";
            if( RelationType.SEMANTIC.equals( edgeType ) )
            {
                xmlKernelType = "logic arc";
            }
            else if( RelationType.REACTANT.equals( edgeType ) )
            {
                xmlKernelType = "consumption";
            }
            else if( RelationType.PRODUCT.equals( edgeType ) )
            {
                xmlKernelType = "production";
            }
            else if( RelationType.MODIFIER.equals( edgeType ) )
            {
                xmlKernelType = "regulation";
            }
            else if( RelationType.NOTE_LINK.equals( edgeType ) )
            {
                xmlKernelType = "logic arc";
            }
            DynamicProperty dp = new DynamicProperty( XmlDiagramTypeConstants.XML_TYPE_PD, String.class, xmlKernelType );
            edge.getAttributes().add( dp );

            try
            {
                edge = (Edge)getPrototype().validate( compartment, edge );
            }
            catch( Exception e )
            {
            }
        }
        return edge;
    }
}
