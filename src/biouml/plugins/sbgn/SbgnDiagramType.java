package biouml.plugins.sbgn;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.util.DiagramXmlReader;
import biouml.plugins.sbml.SbmlEModel;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationDiagramType.DiagramPropertyChangeListener;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

/**
 * 
 * Type for SBGN-SBML diagrams (created to replace old style xml notation "sbml-sbgn")
 * @author Ilya
 */
@PropertyName("SBML model in SBGN notation")
@PropertyDescription("Systems Biology Markup Language (SBML) model with Systems Biology Graphic Notation (SBGN).")
public class SbgnDiagramType extends DiagramTypeSupport
{
    private boolean layoutSpecie = false;
    
    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Type.TYPE_ENTITY, Type.TYPE_COMPLEX, Type.TYPE_COMPARTMENT, Reaction.class, Type.TYPE_PHENOTYPE,
                Type.TYPE_UNIT_OF_INFORMATION, Type.TYPE_VARIABLE, Type.TYPE_LOGICAL, Type.TYPE_EQUIVALENCE, Type.TYPE_EQUATION, Type.TYPE_CONSTRAINT,
                Type.TYPE_EVENT, Type.TYPE_FUNCTION, Type.TYPE_PORT, Type.TYPE_TABLE, Type.TYPE_NOTE};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {Stub.NoteLink.class, Type.TYPE_PORTLINK};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new SbgnDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new SbgnSemanticController();

        return semanticController;
    }

    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName) throws Exception
    {
        return createDiagram( origin, diagramName, new DiagramInfo( diagramName ) );
    }

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.setRole(new SbmlEModel(diagram));
        diagram.getViewOptions().addPropertyChangeListener(new SbgnDiagramPropertyChangeListener(diagram));
        diagram.addPropertyChangeListener(new SbgnComplexStructureManager());
        return diagram;
    }

    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return true;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }

    public void setLayoutSpecie(boolean layout)
    {
        layoutSpecie = layout;
    }
    
    @Override
    public boolean needLayout(Node node)
    {
        return super.needLayout(node) || (layoutSpecie && node.getKernel() instanceof Specie);
    }

    /**
     * listener for switching "dependencyEdges" view options
     */
    public static class SbgnDiagramPropertyChangeListener implements PropertyChangeListener
    {
        protected Logger log = Logger.getLogger(DiagramPropertyChangeListener.class.getName());

        private final Diagram diagram;
        public SbgnDiagramPropertyChangeListener(Diagram diagram)
        {
            this.diagram = diagram;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            if( evt.getSource() instanceof DiagramViewOptions && evt.getPropertyName().equals("addSourceSink") )
            {

                if( evt.getNewValue().equals(true) )
                {
                    DiagramUtility.getReactionNodes(diagram).forEach(n -> SbgnUtil.generateSourceSink(n, true));
                }
                else
                {
                    SemanticController controller = diagram.getType().getSemanticController();
                    for( DiagramElement de : diagram.recursiveStream().filter(de -> Type.TYPE_SOURCE_SINK.equals(de.getKernel().getType())) )
                    {
                        try
                        {
                            controller.remove(de);
                        }
                        catch( Exception ex )
                        {
                            log.log(Level.SEVERE, "Error during node " + de.getName() + " removing", ex);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public DiagramXmlReader getDiagramReader()
    {
        return new SbgnDiagramXmlReader();
    }

    @Override
    public void postProcessClone(Diagram diagramFrom, Diagram diagramTo)
    {
        diagramTo.recursiveStream().select( Node.class ).filter( n -> n.getKernel() instanceof Reaction ).forEach( n -> {
            Reaction r = (Reaction)n.getKernel();
            r.setSpecieReferences( new SpecieReference[0] );
            n.edges().filter( e -> e.getKernel() instanceof SpecieReference ).forEach( e -> {
                ( (Reaction)n.getKernel() ).put( (SpecieReference)e.getKernel() );
            } );
        } );
    }

    @Override
    public boolean needCloneKernel(Base base)
    {
        return base instanceof Reaction || base instanceof Specie;
    }
}
