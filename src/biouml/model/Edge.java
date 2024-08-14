package biouml.model;

import java.awt.Point;
import java.beans.IntrospectionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetSupport;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.Base;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.SimplePath;
import ru.biosoft.util.WeakPropertyChangeForwarder;

/**
 * Edge is directed relation between two diagram nodes, called "input" and "output".
 * @pending notifications for inPort & outPort.
 * @pending inPort & outPort may duplicate information stored by general path.
 * Whether we should consider them as some constraints for PortFinder?
 */
@SuppressWarnings ( "serial" )
@ClassIcon ( "resources/edge.gif" )
@PropertyName("Edge")
@PropertyDescription("Diagram edge.")
public class Edge extends DiagramElement
{
    protected static final Logger log = Logger.getLogger( Node.class.getName() );
    @Nonnull
    private Node input;
    @Nonnull
    private Node output;

    /** Line path that connects input and output nodes.*/
    private Path path;

    private Point inPort;

    private Point outPort;

    private boolean fixedInOut;

    public Edge(Base kernel, @Nonnull Node input, @Nonnull Node output)
    {
        this(Node.findCommonOrigin(input, output), kernel, input, output);
    }

    public Edge(DataCollection<?> parent, Base kernel, @Nonnull Node input, @Nonnull Node output)
    {
        this(parent, getUniqEdgeName(parent, kernel, input, output), kernel, input, output);
    }

    public Edge(String id, Base kernel, @Nonnull Node input, @Nonnull Node output)
    {
        this(Node.findCommonOrigin(input, output), id, kernel, input, output);
    }

    public Edge(DataCollection<?> parent, String id, Base kernel, @Nonnull Node input, @Nonnull Node output)
    {
        super(parent, id, kernel);
        Assert.notNull( "input", input );
        Assert.notNull( "output", output );
        this.input = input;
        this.output = output;

        if( kernel instanceof Option )
        {
            Option option = (Option)kernel;
            new WeakPropertyChangeForwarder(evt -> {
                if( evt.getPropertyName().equals("title") )
                    setTitle((String)evt.getNewValue());
            }, option);
        }
    }

    public static String getUniqEdgeName(DataCollection<?> parent, Base kernel, Node input, Node output)
    {
        return getUniqEdgeName(parent, ( kernel == null ) ? "" : kernel.getName(), input, output);
    }

    public static String getUniqEdgeName(DataCollection<?> parent, String kernelName, Node input, Node output)
    {
        String result = kernelName;
        //removed for compatibility with previously created sbml-sbgn diagrams (Biomodels)
        //TODO: do something with this inconsistency - two variants for reaction edge id:
        //1. "reaction_1: B as product: reaction_1 to B"
        //2. "reaction_1: B as product"

        //        if( input != null && output != null )
        //        {
        //String suffix = ": " + input.getName() + " to " + output.getName();
        //            if( kernelName.indexOf(suffix) == -1 )
        //            {
        //                result = kernelName + suffix;
        //            }
        //        }
        if( parent != null )
        {
            String name = result;
            int i = 0;
            while( parent.contains(name) )
                name = result + "_" + ( ++i );
            result = name;
        }
        return result;
    }

    public @Nonnull Node getInput()
    {
        return input;
    }
    public void setInput(@Nonnull Node input)
    {
        Object oldValue = this.input;
        Assert.notNull( "input", input );
        this.input = input;
        this.firePropertyChange( "input", oldValue, input );
    }

    public @Nonnull Node getOutput()
    {
        return output;
    }
    public void setOutput(@Nonnull Node output)
    {
        Object oldValue = this.output;
        Assert.notNull( "output", output );
        this.output = output;
        this.firePropertyChange( "output", oldValue, output );
    }

    @PropertyName("Path")
    @PropertyDescription("Path.")
    public Path getPath()
    {
        return path;
    }
    public void setPath(Path path)
    {
        Path oldValue = this.path;
        this.path = path;
        firePropertyChange("path", oldValue, path);
        if( path !=null && path.npoints > 0 )
        {
            this.inPort = new Point(path.xpoints[0], path.ypoints[0]);
            this.outPort = new Point(path.xpoints[path.npoints - 1], path.ypoints[path.npoints - 1]);
        }
    }

    public SimplePath getSimplePath()
    {
        return path == null? null: new SimplePath(path.xpoints, path.ypoints, path.pointTypes, path.npoints);
    }

    @PropertyName("Input")
    @PropertyDescription("Input.")
    public Point getInPort()
    {
        return inPort;
    }
    public void setInPort(Point inPort)
    {
        Point oldValue = this.inPort;
        this.inPort = inPort;
        firePropertyChange("inPort", oldValue, inPort);
    }

    @PropertyName("Output")
    @PropertyDescription("Output.")
    public Point getOutPort()
    {
        return outPort;
    }
    public void setOutPort(Point outPort)
    {
        Point oldValue = this.outPort;
        this.outPort = outPort;
        firePropertyChange("outPort", oldValue, outPort);
    }

    @PropertyName ( "Fixed edge ends" )
    @PropertyDescription ( "Fix input and output points of edge." )
    public boolean isFixedInOut()
    {
        return fixedInOut;
    }

    public void setFixedInOut(boolean fixedInOut)
    {
        Object oldValue = this.fixedInOut;
        this.fixedInOut = fixedInOut;
        firePropertyChange( "fixedInOut", oldValue, fixedInOut );
    }

    public @Nonnull Node getOtherEnd(Node n)
    {
        if(n == getInput()) return getOutput();
        if(n == getOutput()) return getInput();
        throw new IllegalArgumentException(this+": Supplied node "+n+" is not my input or output");
    }


    public Edge clone(Compartment newParent, String newName, boolean cloneKernel)
    {
        Base kernel = getKernel();
        try
        {
            if( cloneKernel && getKernel() instanceof CloneableDataElement )
                kernel = (Base) ( (CloneableDataElement)kernel ).clone( kernel.getOrigin(), newName );
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Cloning node error", ex );
        }
        return clone( newParent, newName, kernel );
    }

    @Override
    public Edge clone(Compartment newParent, String newName)
    {
        return clone( newParent, newName, getKernel() );
    }

    public Edge clone(Compartment newParent, String newName, Base kernel)
    {
        try
        {
            //TODO - advanced testing of this clone method
            String inName = CollectionFactory.getRelativeName( getInput(), getOrigin() );
            String outName = CollectionFactory.getRelativeName( getOutput(), getOrigin() );

            Node newInput = CollectionFactory.getDataElement( inName, newParent, Node.class );
            Node newOutput = CollectionFactory.getDataElement( outName, newParent, Node.class );

            Edge edge = newName != null ? new Edge( newParent, newName, kernel, newInput, newOutput )
                    : new Edge( newParent, kernel, newInput, newOutput );
            edge.setTitle( getTitle() );
            Role role = getRole();
            if( role != null )
            {
                Role newRole = role.clone(edge);
                edge.setRole(newRole);
            }

            if( path != null )
                edge.setPath( path.clone() );

            if( inPort != null )
                edge.setInPort((Point)inPort.clone());

            if( outPort != null )
                edge.setOutPort((Point)outPort.clone());

                edge.setFixed( fixed );
            if( isFixedInOut() )
                edge.setFixedInOut( true );

            if( attributes != null )
            {
                for(DynamicProperty oldProp : attributes)
                {
                    DynamicProperty prop = null;
                    try
                    {
                        prop = DynamicPropertySetSupport.cloneProperty(oldProp);
                    }
                    catch( IntrospectionException e )
                    {
                        prop = oldProp;
                    }
                    edge.getAttributes().add(prop);
                }
            }

            edge.setPredefinedStyle(getPredefinedStyle());
            if( edge.getPredefinedStyle().equals(DiagramElementStyle.STYLE_NOT_SELECTED) )
                edge.setCustomStyle(getCustomStyle().clone());
            return edge;
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    /**
     * @return stream of two nodes: input and output
     */
    public StreamEx<Node> nodes()
    {
        return StreamEx.of(getInput(), getOutput());
    }
}
