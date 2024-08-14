package biouml.model;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.net.URL;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.SuppressHuntBugsWarning;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Type;

/**
 * Stub implementation of DiagramType interface.
 *
 * DefaultDiagramViewBuilder and DefaultSemanticController are used.
 * The class also defines some useful procedure to get the diagram legend.
 */
public class DiagramTypeSupport implements DiagramType
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        DiagramInfo info;
        if( kernel instanceof DiagramInfo )
        {
            info = (DiagramInfo)kernel;
        }
        else
        {
            info = new DiagramInfo(origin, diagramName);
        }
        return new Diagram(origin, info, this);
    }

    @Override
    public Object[] getNodeTypes()
    {
        return null;
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return null;
    }

    protected DiagramViewBuilder diagramViewBuilder;
    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new DefaultDiagramViewBuilder();
        return diagramViewBuilder;
    }
    @Override
    public void setDiagramViewBuilder(DiagramViewBuilder viewBuilder)
    {
        this.diagramViewBuilder = viewBuilder;
    }

    @Override
    public DiagramFilter getDiagramFilter(Diagram diagram)
    {
        return null;
    }

    protected SemanticController semanticController;
    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new DefaultSemanticController();

        return semanticController;
    }

    /**
     * Returns URL for HTML file that contains the legend for the diagram type.
     *
     * It is suggested that legend located in the directory "resources" relative
     * diagram class and legend HTML file has name equals to diagram type class name.
     * For example legend file "OntologyDiagramType.html" for
     * biouml.standard.diagram.OntologyDiagramType class should be located
     * at biouml/standard/diagram/resources subdirectory.
     */
    @Override
    @SuppressHuntBugsWarning({"UnsafeGetResource"})
    public URL getLegend()
    {
        String name = getClass().getName();
        int ind = name.lastIndexOf('.');
        name = name.substring(ind + 1);

        URL url = getClass().getResource("resources/" + name + ".html");
        return url;
    }

    protected DynamicPropertySet properties = new DynamicPropertySetSupport();
    @Override
    public DynamicPropertySet getProperties()
    {
        return properties;
    }

    @Override
    public boolean needLayout(Node node)
    {
        return node.getKernel().getType().equals(Type.TYPE_COMPARTMENT);
    }

    @Override
    public DiagramType clone() throws CloneNotSupportedException
    {
        return (DiagramType)super.clone();
    }

    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return false;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return false;
    }

    @Override
    public String getTitle()
    {
        try
        {
            return Introspector.getBeanInfo(getClass()).getBeanDescriptor().getDisplayName();
        }
        catch( IntrospectionException e )
        {
            ExceptionRegistry.log( e );
            return getClass().getSimpleName();
        }
    }

    @Override
    public String getDescription()
    {
        try
        {
            return Introspector.getBeanInfo(getClass()).getBeanDescriptor().getShortDescription();
        }
        catch( IntrospectionException e )
        {
            ExceptionRegistry.log( e );
            return getClass().getSimpleName();
        }
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    @Override
    public DiagramXmlReader getDiagramReader()
    {
        return new DiagramXmlReader();
    }
    
    @Override
    public DiagramXmlWriter getDiagramWriter()
    {
        return new DiagramXmlWriter();
    }
    
    private static boolean isCompletelyFixed(Compartment compartment)
    {
        return compartment.isFixed() && compartment.recursiveStream().allMatch( n -> n.isFixed() );
    }
    @Override
    public boolean needCloneKernel(Base base)
    {
        return false;
    }

    @Override
    public void postProcessClone(Diagram diagramFrom, Diagram diagramTo)
    {
        //do nothing by default;
    }
}
