package biouml.plugins.sbgn.extension;

import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.graphics.editor.ViewEditorPane;

import java.awt.Point;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.sbgn.Type;
import biouml.standard.diagram.CreateEdgeAction;
import biouml.standard.diagram.CreateEdgeAction.EdgeCreator;
import biouml.standard.type.Base;
import biouml.standard.type.SemanticRelation;

public class SbgnExSemanticController extends SbgnSemanticController
{
    public static Base createKernelByType(String type, String name)
    {
        if( RelationType.SEMANTIC.equals( type ) )
            return new SemanticRelation( null, name );
        return SbgnSemanticController.createKernelByType( type, name );
    }

    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        if( type.equals( SemanticRelation.class ) )
        {
            new CreateEdgeAction().createEdge( point, viewEditor, new SemanticRelationCreator() );
            return DiagramElementGroup.EMPTY_EG;
        }
        return super.createInstance( parent, type, point, viewEditor );
    }
    
    public static class SemanticRelationCreator implements EdgeCreator
    {
        @Override
        public Edge createEdge(@Nonnull Node in, @Nonnull Node out, boolean temporary)
        {
            Edge e =  new Edge(new SemanticRelation(null, in.getName() + " -> " + out.getName(), RelationType.SEMANTIC), in, out);
            e.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, Type.TYPE_CATALYSIS));
            return e;
        }
    }

}
