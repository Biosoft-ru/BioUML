package biouml.plugins.sabiork;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eml.sdbv.sabioclient.GetAllPathways;
import org.eml.sdbv.sabioclient.GetReactionIDs;
import org.eml.sdbv.sabioclient.GetSBML;
import org.eml.sdbv.sabioclient.GetSBMLResponse;

import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class PathwayProvider extends ServiceProvider
{
    @Override
    public Diagram getDataElement(DataCollection<?> parent, String name) throws Exception
    {
        DiagramType diagramType = new PathwaySimulationDiagramType();
        DiagramInfo diagramInfo = new DiagramInfo(null, name);

        //String sbmlString = getSBMLString(name);

        Diagram diagram = new Diagram(parent, diagramInfo, diagramType);

        diagram.setNotificationEnabled(false);

        diagram.setTitle(name);
        diagram.setRole(new EModel(diagram));

        importReactions(diagram, null);

        diagram.setNotificationEnabled(true);
        return diagram;
    }

    @Override
    public List<String> getNameList() throws RemoteException
    {
        GetAllPathways gp = new GetAllPathways();
        String[] pathwayNames = getSabiokrPort().getAllPathways(gp);

        List<String> result = new ArrayList<>();
        for( String name : pathwayNames )
        {
            result.add(getSafeString(name));
        }
        return result;
    }

    protected String getSBMLString(String name) throws Exception
    {
        String result = null;
        int[] results = getSabiokrPort().getReactionIDs(new GetReactionIDs(getOriginalString(name)));
        if( results != null )
        {
            Integer[] reactions = new Integer[results.length];
            for( int i = 0; i < results.length; i++ )
            {
                reactions[i] = results[i];
            }
            Integer[] kinlaws = {5046};
            GetSBMLResponse sbmlResponse = getSabiokrPort().getSBML(new GetSBML(reactions, kinlaws, 2, 3, "aaa", false));
            result = sbmlResponse.get_return();
        }
        return result;
    }

    protected void importReactions(Diagram diagram, FunctionJobControl jobControl) throws Exception
    {
        int[] results = getReactionsIDs(diagram.getName());
        if( results != null )
        {
            EModel model = diagram.getRole( EModel.class );

            for( int i = 0; i < results.length; i++ )
            {
                if( jobControl != null )
                {
                    jobControl.setPreparedness((int) ( ( (double)i / (double)results.length ) * 100.0 ));
                }

                if( results[i] == 0 )
                    continue;

                Reaction reaction = (Reaction)SabiorkUtility.getDataCollection(diagram, SabiorkUtility.REACTION_DC).get(
                        String.valueOf(results[i]));

                if( reaction == null )
                {
                    log.log(Level.SEVERE, "Can not find reaction: " + results[i]);
                    continue;
                }

                //process parameters
                Variable[] parameters = (Variable[])reaction.getAttributes().getValue("parameters");
                if( parameters != null )
                {
                    for( Variable param : parameters )
                    {
                        if( model.getVariable(param.getName()) == null )
                        {
                            param.setParent(model);
                            model.put(param);
                        }
                    }
                }

                Node reactionNode = new Node(diagram, reaction);
                Equation rule = new Equation(reactionNode, Equation.TYPE_SCALAR, "$$rate_" + reactionNode.getName());
                reactionNode.setRole(rule);
                diagram.put(reactionNode);

                Node enzymeNode = null;

                if( reaction.getSpecieReferences() != null )
                {
                    for( SpecieReference sr : reaction.getSpecieReferences() )
                    {
                        //get or create node
                        Base kernel = CollectionFactory.getDataElement(sr.getSpecie(), Module.getModule(diagram), Base.class);
                        Node node = null;
                        if( diagram.contains(kernel.getName()) )
                        {
                            node = (Node)diagram.get(kernel.getName());
                        }
                        else
                        {
                            node = new Node(diagram, kernel);
                            VariableRole variable = new VariableRole(node, 0.1/*parameters for testing simulation*/);
                            node.setRole(variable);
                            model.put(variable);
                            diagram.put(node);
                        }

                        if( node.getKernel() instanceof Protein )
                        {
                            enzymeNode = node;
                        }

                        //create edge
                        Edge edge = null;
                        if( sr.getRole().equals(SpecieReference.PRODUCT) )
                        {
                            edge = new Edge(diagram, sr, reactionNode, node);
                        }
                        else
                        {
                            edge = new Edge(diagram, sr, node, reactionNode);
                        }
                        VariableRole var = node.getRole( VariableRole.class );
                        Equation equation = new Equation(edge, Equation.TYPE_RATE, var.getName());
                        edge.setRole(equation);
                        reactionNode.addEdge(edge);
                        node.addEdge(edge);
                        diagram.put(edge);
                    }
                }
                fixKineticLaw(diagram, reaction, enzymeNode);
            }
        }
    }

    public int[] getReactionsIDs(String diagramName) throws Exception
    {
        return getSabiokrPort().getReactionIDs(new GetReactionIDs(getOriginalString(diagramName)));
    }

    protected static final String ENZYME = "Enzyme";
    protected void fixKineticLaw(Diagram diagram, Reaction reaction, Node enzymeNode)
    {
        if( reaction == null || reaction.getKineticLaw() == null )
            return;

        String formula = reaction.getKineticLaw().getFormula();
        if( formula == null )
            return;

        Map<String, Variable> variableMap = new HashMap<>();
        Variable[] variables = (Variable[])reaction.getAttributes().getValue("variables");
        if( variables != null )
        {
            for( Variable var : variables )
            {
                variableMap.put(var.getName(), var);
            }
        }

        KineticLawProcessor klp = new KineticLawProcessor(formula);
        for( String element : klp.elements() )
        {
            if( !klp.isSign(element) )
            {
                try
                {
                    Node node = null;
                    if( element.equals(ENZYME) && enzymeNode != null )
                    {
                        node = enzymeNode;
                    }
                    else
                    {
                        node = diagram.findNode(element);
                    }
                    if( node != null && node.getRole() != null )
                    {
                        VariableRole role = node.getRole( VariableRole.class );
                        klp.replaceFormulaElement( element, role.getName() );
                        if( variableMap.get(element) != null )
                        {
                            role.setInitialValue( variableMap.get( element ).getInitialValue() );
                        }
                    }
                }
                catch( Exception e )
                {
                    //just go to next element
                }
            }
        }
        reaction.getKineticLaw().setFormula(klp.getKineticLaw());
    }

    private String getSafeString(String originalString)
    {
        return originalString.replaceAll("/", "\\\\");
    }

    private String getOriginalString(String safeString)
    {
        return safeString.replaceAll("\\\\", "/");
    }
}
