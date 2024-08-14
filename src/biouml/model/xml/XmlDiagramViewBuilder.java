package biouml.model.xml;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaPackage;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.DefaultDiagramViewBuilder;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.diagram.PathwayDiagramViewBuilder;
import biouml.standard.type.Base;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Type;
import ru.biosoft.graph.PortFinder;
import ru.biosoft.graph.ShapeChanger;
import ru.biosoft.graphics.ArrowView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.TextUtil;

/**
 * Prototype DiagramViewBuider is used:
 * <ul>
 *   <li> to build view for node types for which corresponding function is not defined in graphic notation;
 *   <li> to build view for edge types for which corresponding function is not defined in graphic notation;
 *   <li> prototype is available in JavaScript functions and it's methods
 *        can be used by JavaScript functions.
 *  </ul>
 *
 * If prototype DiagramViewBuider is not defined in graphic notation then
 * DefaultDiagramviewBuilder is used as prototype.
 */
public class XmlDiagramViewBuilder extends DefaultDiagramViewBuilder
{
    protected static final Logger log = Logger.getLogger(XmlDiagramViewBuilder.class.getName());

    ///////////////////////////////////////////////////////////////////
    // JavaScript issues
    //

    public DiagramViewBuilder getBaseViewBuilder()
    {
        return baseViewBuilder;
    }

    protected XmlDiagramType xmlDiagramType;
    public XmlDiagramViewBuilder(XmlDiagramType diagramType)
    {
        this.xmlDiagramType = diagramType;
        baseViewBuilder = new DefaultDiagramViewBuilder();
    }

    private final ThreadLocal<Context> contextMap = new ThreadLocal<>();
    public Context getContext()
    {
        Context context = contextMap.get();

        if( context == null )
        {
            context = Context.enter();
            context.initStandardObjects(getScope());
            contextMap.set(context);

            try
            {
                ImporterTopLevel scope = getScope();
                scope.put("prototype", scope, Context.toObject(baseViewBuilder, scope));

                //          import packages
                //          TODO: import packages by another way
                for( String packageName : XmlDiagramType.IMPORT_PACKAGES )
                {
                    NativeJavaPackage pack = new NativeJavaPackage(packageName, this.getClass().getClassLoader());
                    ScriptRuntime.setObjectProtoAndParent(pack, scope);
                    scope.importPackage(null, null, new Object[] {pack}, null);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not initialise JavaScript context for Xml Diagram View Builder");
            }
        }

        return context;
    }

    private final Map<String, ImporterTopLevel> scopeMap = new HashMap<>();
    public ImporterTopLevel getScope()
    {
        String threadKey = Thread.currentThread().getName();
        ImporterTopLevel scope;
        if( scopeMap.containsKey(threadKey) )
        {
            scope = scopeMap.get(threadKey);
        }
        else
        {
            scope = new ImporterTopLevel();
            scopeMap.put(threadKey, scope);
        }

        return scope;
    }

    protected Map<String, Function> functionsMap = new HashMap<>();
    protected Map<Object, Icon> icons = new HashMap<>();
    protected Map<String, String> functionCodesMap = new HashMap<>();


    public void addFunction(String type, String script)
    {
        Function f = getContext().compileFunction(getScope(), script, "view function for " + type, 0, null);

        functionsMap.put(type, f);
        functionCodesMap.put(type, script);
    }

    public void removeFunction(String type)
    {
        functionsMap.remove(type);
        functionCodesMap.remove(type);
    }

    public String findElementType(Base kernel)
    {
        if( kernel == null )
            return null;
        String type = xmlDiagramType.getKernelTypeName(kernel.getClass());
        if( type == null )
        {
            type = kernel.getType();
        }
        if( typeMapping != null && typeMapping.containsKey(type) )
        {
            return typeMapping.get(type);
        }
        return type;
    }
    @Override
    public boolean createCompartmentCoreView(CompositeView container, Compartment compartment, DiagramViewOptions options, Graphics g)
    {
        boolean showTitle = createNodeCoreView(container, compartment, options, g);
        container.setLocation(compartment.getLocation());
        container.setModel(compartment);
        container.setActive(true);
        return showTitle;
    }

    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        String type;
        Object typeObj = node.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE);
        if( typeObj != null )
        {
            type = typeObj.toString();
        }
        else
        {
            type = findElementType(node.getKernel());
        }
        if( type != null )
        {
            if( functionsMap.containsKey(type) )
            {
                Function f = functionsMap.get(type);
                try
                {
                    addMissingAttributes(node, type);
                    ScriptableObject scope = getScope();
                    Object functionArgs[] = {container, new DiagramElementJScriptWrapper(node), options, g};
                    Boolean result = (Boolean)f.call(getContext(), scope, scope, functionArgs);
                    return result.booleanValue();
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can not create view, type=" + type + ", JavaScript function=" + f + ", error=" + t, t);
                }
            }
        }
        return baseViewBuilder.createNodeCoreView(container, node, baseViewOptions, g);
    }
    
    public void addMissingAttributes(Node node, String type)
    {
        DynamicPropertySet oldAttributes = node.getAttributes();
        DynamicPropertySet newAttributes = xmlDiagramType.getType(type);
        Iterator<String> iter = newAttributes.nameIterator();
        while( iter.hasNext() )
        {
            String pName = iter.next();
            if( oldAttributes.getProperty(pName) == null )
            {
                DynamicProperty dp = newAttributes.getProperty(pName);
                Object value = null;
                
                Object kernelValue = node.getKernel().getAttributes().getValue( pName );
                if(dp.getType().isInstance( kernelValue ))
                {
                    value = kernelValue;
                } else
                {
                    //try to clone default value
                    try
                    {
                        value = dp.getType().getConstructor(String.class).newInstance(dp.getValue().toString());
                    }
                    catch( Exception ex2 )
                    {

                    }
                    
                    //try to create new value
                    if( value == null )
                    {
                        try
                        {
                            value = dp.getType().newInstance();
                        }
                        catch( Exception ex )
                        {

                        }
                    }
                    if( value == null )
                    {
                        value = TextUtil.fromString(dp.getType(), dp.getValue().toString());
                    }
                }
                oldAttributes.add(new DynamicProperty(pName, dp.getType(), value));
            }
        }
    }
    @Override
    public @Nonnull CompositeView createEdgeView(Edge edge, DiagramViewOptions options, Graphics g)
    {
        String type;
        if( isReactionEdge(edge) )
        {
            type = findElementType((Base)edge.getKernel().getOrigin());
        }
        else
        {
            Object typeObj = edge.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE);
            if( typeObj != null )
            {
                type = typeObj.toString();
            }
            else
            {
                type = findElementType(edge.getKernel());
            }
        }
        if( type != null && functionsMap.containsKey(type) )
        {
            CompositeView view = new CompositeView();
            Function f = functionsMap.get(type);

            Object functionArgs[] = {view, new DiagramElementJScriptWrapper(edge), options, g};
            try
            {
                ScriptableObject scope = getScope();
                f.call(getContext(), scope, scope, functionArgs);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not create view, type=" + type + ", JavaScript function=" + f + ", error=" + t, t);
            }

            initArrowViews(view, edge);

            return view;
        }
        if( edge.getKernel().getType().equals(Type.TYPE_DEPENDENCY) )
            {
                PathwayDiagramViewBuilder builder = new PathwayDiagramViewBuilder();
                return builder.createDependencyView(edge, g);
            }
        return baseViewBuilder.createEdgeView(edge, baseViewOptions, g);
    }

    private boolean isReactionEdge(Edge edge)
    {
        if( edge.getKernel() instanceof SpecieReference )
        {
            String inNodeType = findElementType(edge.getInput().getKernel());
            String outNodeType = findElementType(edge.getOutput().getKernel());
            String[] reactionTypes = xmlDiagramType.getReactionTypes();
            for( String rType : reactionTypes )
            {
                if( inNodeType.equals(rType) || outNodeType.equals(rType) )
                    return true;
            }

        }
        return false;
    }

    protected void initArrowViews(View view, Object model)
    {
        if( view instanceof ArrowView )
        {
            view.setModel(model);
            view.setActive(true);
        }
        else if( view instanceof CompositeView )
        {
            view.setModel(model);
            for( int i = 0; i < ( (CompositeView)view ).size(); i++ )
            {
                initArrowViews( ( (CompositeView)view ).elementAt(i), model);
            }
        }
    }

    private XmlDiagramViewOptions diagramViewOptions;
    @Override
    public DiagramViewOptions createDefaultDiagramViewOptions()
    {
        if( diagramViewOptions == null )
        {
            diagramViewOptions = new XmlDiagramViewOptions(null);
        }
        diagramViewOptions.setDiagramTitleVisible(false);
        return (DiagramViewOptions)diagramViewOptions.clone();
    }

    public DynamicPropertySet getViewOptions()
    {
        if( diagramViewOptions == null )
        {
            diagramViewOptions = new XmlDiagramViewOptions(null);
        }
        return diagramViewOptions.getOptions();
    }

    protected Point[] getPoints(Edge edge, Point point, Rectangle inBounds)
    {
        Point in = new Point();
        Point out = new Point();
        if( edge.getOutput().getView() == null )
        {
            log.warning("Node view is empty, node=" + edge.getOutput().getName());
            return null;
        }
        Rectangle rin = new Rectangle(point.x, point.y, inBounds.width, inBounds.height);
        calcAttachmentPoints(rin, edge.getOutput().getView().getBounds(), in, out, 1, 3);

        return new Point[] {in, out};
    }

    public void setIcon(Object key, Icon icon)
    {
        icons.put(key, icon);
    }

    @Override
    public Icon getIcon(Object type)
    {
        Icon icon = icons.get(type);
        if( icon == null )
        {
            String strType = xmlDiagramType.getKernelTypeName(type);
            if( strType != null )
            {
                icon = icons.get(strType);
            }
        }
        if( icon == null )
        {
            icon = new ImageIcon();
        }
        return icon;
    }

    //////////////////////////////////////////////////////////////////
    // Methods to build the diagram view
    //

    //    public CompositeView createNodeView(Node node, DiagramViewOptions options, Graphics g)
    //    {}

    /////////////
    /**
     * Creates the icon to be used in toolbar for creation corresponding type of diagram element.
     *
     public Icon getIcon(Object type);

     public CompositeView createCompartmentView(Compartment compartment, DiagramViewOptions options, Graphics g);
     */

    public Map<String, String> getFunctionCodes()
    {
        return functionCodesMap;
    }
    
    @Override
    public PortFinder getPortFinder(Node node)
    {
        return baseViewBuilder.getPortFinder(node);
    }
    
    @Override
    public ShapeChanger getShapeChanger(Node node)
    {
        return baseViewBuilder.getShapeChanger(node);
    }
}
