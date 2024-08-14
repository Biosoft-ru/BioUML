package biouml.plugins.bionetgen.diagram;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramTypeSupport;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;

@PropertyName ( "BioNetGen model" )
@PropertyDescription ( "BioNetGen Language (BNGL) model with special graphic notation." )
public class BionetgenDiagramType extends DiagramTypeSupport
{
    @Override
    public Object[] getNodeTypes()
    {
        return new Object[] {BionetgenConstants.TYPE_SPECIES, BionetgenConstants.TYPE_MOLECULE, BionetgenConstants.TYPE_MOLECULE_COMPONENT,
                BionetgenConstants.TYPE_MOLECULETYPE, BionetgenConstants.TYPE_OBSERVABLE, Reaction.class, BionetgenConstants.TYPE_EQUATION};
    }

    @Override
    public Object[] getEdgeTypes()
    {
        return new Object[] {BionetgenConstants.TYPE_EDGE};
    }

    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if( diagramViewBuilder == null )
            diagramViewBuilder = new BionetgenDiagramViewBuilder();

        return diagramViewBuilder;
    }

    @Override
    public SemanticController getSemanticController()
    {
        if( semanticController == null )
            semanticController = new BionetgenSemanticController();

        return semanticController;
    }

    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.setRole(new EModel(diagram));
        return diagram;
    }

    @Override
    public boolean isGeneralPurpose()
    {
        return true;
    }

    @Override
    public boolean needAutoLayout(Edge edge)
    {
        return true;
    }
}
