package ru.biosoft.analysis.diagram;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.DataElementNotAcceptableException;
import ru.biosoft.analysiscore.AnalysisMethodSupport;

@ClassIcon ( "resources/GetMoleculesFromDiagram.png" )
public class ShareComplexMolecule extends AnalysisMethodSupport<ShareComplexMoleculeParameters>
{
    public ShareComplexMolecule(DataCollection<?> origin, String name)
    {
        super(origin, name, new ShareComplexMoleculeParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        if(!(parameters.getDiagramPath().getDataElement( Diagram.class ).getRole() instanceof EModel))
        {
            throw new DataElementNotAcceptableException( parameters.getDiagramPath(), "Diagram role must be EModel" );
        }
    }

    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        DataElementPath path = parameters.getDiagramPath();
        Diagram diagram = path.getDataElement(Diagram.class);
        Diagram changedDiagram = diagram.clone(parameters.getOutputPath().getParentCollection(), parameters.getOutputPath().getName());
        changedDiagram.setType(diagram.getType().clone());
        EModel emodel = changedDiagram.getRole(EModel.class);
        String[] elementNames = parameters.getElementNames();

        for( String name : elementNames )
        {
            name = name.substring(name.lastIndexOf(" (id:") + 5, name.length() - 1);
            Variable var = emodel.getVariable(name);
            if( var instanceof VariableRole )
            {
                try
                {
                    Node node = (Node) ( (VariableRole)var ).getDiagramElement();
                    shareNode(node);
                }
                catch( Exception e )
                {
                    log.warning("Can't share " + name + ". " + e.getMessage());
                }
            }
        }

        CollectionFactoryUtils.save( changedDiagram );
        return changedDiagram;
    }

    public static void shareNode(Node node) throws Exception
    {
        Compartment parent = (Compartment)node.getParent();
        Edge[] edges = node.getEdges();
        if( edges.length != 0 )
            node.getAttributes().add( new DynamicProperty( "sbgn:cloneMarker", String.class, node.getCompleteNameInDiagram() ) );
        for( Edge edge : edges )
        {
            String newName = DefaultSemanticController.generateUniqueNodeName(parent, node.getName());
            Node cloneNode = node.clone(parent, newName);
            VariableRole role = node.getRole(VariableRole.class);
            cloneNode.setRole(role);
            role.addAssociatedElement(cloneNode);
            parent.put(cloneNode);
            if( edge.getInput() == node )
                edge.setInput(cloneNode);
            else if( edge.getOutput() == node )
                edge.setOutput(cloneNode);
            else
                throw new Exception("Incorrect edge " + edge.getName());
            cloneNode.addEdge(edge);
            cloneNode.getAttributes().add(new DynamicProperty("sbgn:cloneMarker", String.class, node.getCompleteNameInDiagram()));
        }
    }
}
