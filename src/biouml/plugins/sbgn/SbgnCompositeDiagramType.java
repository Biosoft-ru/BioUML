package biouml.plugins.sbgn;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.DiagramViewBuilder;
import biouml.model.ModelDefinition;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.standard.type.Reaction;
import biouml.standard.type.Stub;

/**
 * @author Ilya
 * Type for composite SBGN-SBML diagrams (created to replace old style xml notation "sbml_sbgn_composite")
 */
@PropertyName ( "SBML 'comp' model in SBGN notation" )
@PropertyDescription("Systems Biology Markup Language (SBML) hierarchic model with Systems Biology Graphic Notation (SBGN).")
public class SbgnCompositeDiagramType extends SbgnDiagramType
{
    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {Type.TYPE_ENTITY, Type.TYPE_COMPLEX, Type.TYPE_COMPARTMENT, Reaction.class, Type.TYPE_PHENOTYPE,
                Type.TYPE_UNIT_OF_INFORMATION, Type.TYPE_VARIABLE, Type.TYPE_LOGICAL, Type.TYPE_EQUATION, Type.TYPE_EVENT,
                Type.TYPE_FUNCTION, Type.TYPE_CONSTRAINT, Type.TYPE_PORT, Type.TYPE_TABLE, SubDiagram.class, ModelDefinition.class, Stub.Bus.class, Stub.Note.class};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {Stub.DirectedConnection.class, Stub.UndirectedConnection.class, Stub.NoteLink.class};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new SbgnCompositeDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new SbgnCompositeSemanticController();

        return semanticController;
    }
}
