package biouml.model;

import java.net.URL;

import javax.annotation.Nonnull;

import biouml.model.util.DiagramXmlReader;
import biouml.model.util.DiagramXmlWriter;
import biouml.standard.type.Base;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataCollection;

/**
 * Formal definition of diagram type (graphic notation) via Java interface.
 *
 * @pending for compatibility with version 0.7.7 methods <code>getNodeTypes</code>
 * and <code>getEdgeTypes</code> return array of objects.
 * The following convention is used:
 * <ul>
 *   <li> generally methods <code>getNodeTypes</code> and <code>getEdgeTypes</code>
 *        should return <code>Class[]</code>, classes correspond to classes for biological entities
 *        defined at <code>biouml.standard.type</code> package
 *   <li> methods <code>getNodeTypes</code> and <code>getEdgeTypes</code> should return
 *        <code>String[]</code> when graphic notation is defined via XML file {@link biouml.model.xml.XmlDiagramType}
 * </ul>
 */
public interface DiagramType extends Cloneable
{
    /** Creates new empty diagram of corresponding type. Kernel parameter is non-obligatory and can be null in that case new kernel should be created.*/
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String name, Base kernel) throws Exception;

    /** Returns node types that can be added to the diagram of this type. */
    public Object[] getNodeTypes();

    /** Returns edge types that can be added to the diagram of this type. */
    public Object[] getEdgeTypes();

    /** Returns {@link DiagramViewBuilder} that creates view of the diagram. */
    public DiagramViewBuilder getDiagramViewBuilder();

    /** Sets {@link DiagramViewBuilder} to create view of the diagram. */
    public void setDiagramViewBuilder(DiagramViewBuilder viewBuilder);

    /**
     * Returns {@link DiagramFilter} that allows hide or highlight diagram elements according to some criteria.
     *
     * @pending while 1) several filters can be used for the same diagram and 2) filters can be loaded
     * from plug-ins this concept will be refactored.
     */
    public DiagramFilter getDiagramFilter(Diagram diagram);

    /** Returns {@link SemanticController} that allows edit the diagram. */
    public SemanticController getSemanticController();

    /**
     * Returns URL to diagram legend.
     *
     * @pending diagram legend can be a part of formal graphic notation
     * defined via XML file, so this concept possibly will be refined.
     */
    public URL getLegend();

    /**
     * 
     * @return Properties
     */
    public DynamicPropertySet getProperties();
    
    /**
     * Indicates where node should be layouted like compartment
     */
    public boolean needLayout(Node node);
    
    public boolean needCloneKernel(Base base);

    public void postProcessClone(Diagram diagramFrom, Diagram diagramTo);

    public boolean needAutoLayout(Edge edge);
    
    public DiagramType clone() throws CloneNotSupportedException;
    
    /**
     * Return true if this diagram type is not specific to any database
     * and can be used in user projects or any other collections
     */
    public boolean isGeneralPurpose();
    
    public String getTitle();
    
    public String getDescription();
    
    public DiagramXmlReader getDiagramReader();
    
    public DiagramXmlWriter getDiagramWriter();

    /**
     * Indicates if element should be converted before adding
     * it on the diagram.
     * Should be used e.g. before drag-and-drop elements
     * on web or before adding search results to the diagram.
     * @return
     */
    default public boolean useConverterOnAdd()
    {
        return true;
    }
}
