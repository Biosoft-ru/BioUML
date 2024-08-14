package biouml.plugins.modelreduction._test;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.state.State;
import biouml.standard.type.Reaction;
import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

public class SteadyStateApply extends AbstractBioUMLTest
{
    public SteadyStateApply(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SteadyStateApply.class.getName());
        suite.addTest(new SteadyStateApply("test"));
        return suite;
    }
    
    public static void test() throws Exception
    {
        Diagram originalDiagram = getDiagram(DATA_RESOURCES_REPOSITORY, EXAMPLE_DIAGRAMS_COLLECTION, "Mitchel 2013");
        Diagram modifiedDiagram = getDiagram(DATA_RESOURCES_REPOSITORY, EXAMPLE_DIAGRAMS_COLLECTION, "Mitchell 2013 exp");

        Diagram result =  originalDiagram.clone(originalDiagram.getOrigin(), "Mitchell 2013");
        
        result.addState(new State(result, "state 1"));
        result.setCurrentStateName("state 1");

        for (Node node :result.recursiveStream().select(Node.class))
        {
            if (modifiedDiagram.findNode(node.getName()) == null)
                result.remove(node.getName());
        }
        
        modifiedDiagram.stream(Node.class).filter(n -> n.getRole() instanceof Function).forEach(n -> {
            DiagramElement de = result.findNode(n.getName());
            if( de != null )
            {
                Function oldFunction = n.getRole(Function.class);
                String oldFormula = oldFunction.getFormula();
                Function f = de.getRole(Function.class);
                f.setFormula(oldFormula);
            }
//            else
//            {
//                result.put((Node)n.clone(result, n.getName()));
//            }
        });
        
        for (Node node :modifiedDiagram.stream(Node.class).filter(n -> n.getRole() instanceof Equation))
        {
            DiagramElement de = result.findNode(node.getName());
            if( de != null )
            {
                Equation oldFunction = node.getRole(Equation.class);
                String oldFormula = oldFunction.getFormula();
                Equation f = de.getRole(Equation.class);
                f.setFormula(oldFormula);
                f.setVariable(oldFunction.getVariable());
                f.setType(oldFunction.getType());
            }
//            else
//            {
//                result.put((Node)node.clone(result, node.getName()));
//            }
        }
        
        EModel modifiedEModel = modifiedDiagram.getRole(EModel.class);
        EModel newEModel = result.getRole(EModel.class);
        

        for (Node node: DiagramUtility.getReactionNodes(modifiedDiagram))
        {
            Node newNode = result.findNode(node.getName());
            Reaction oldReaction = (Reaction)node.getKernel();
            Reaction newReaction = (Reaction)newNode.getKernel();
            newReaction.setFormula(oldReaction.getFormula());
        }
        
        for( Variable var : modifiedEModel.getVariables() )
        {
            Variable newVar = newEModel.getVariable(var.getName());
            if( newVar != null )
            {
                newVar.setInitialValue(var.getInitialValue());
                newVar.setConstant(var.isConstant());
                if( var instanceof VariableRole )
                {
                    ( (VariableRole)newVar ).setBoundaryCondition( ( (VariableRole)var ).isBoundaryCondition());
                }
            }
            else
            {
                
                newEModel.getVariables().put(var.clone(var.getName()));
            }
        }
    
        result.restore();
        result.save();
    }



    public static Diagram getDiagram(String repositoryPath, String collectionName, String name) throws Exception
    {
        CollectionFactory.unregisterAllRoot();
        CollectionFactory.createRepository(repositoryPath);
        DataCollection<?> collection1 = CollectionFactory.getDataCollection(collectionName);
        DataElement de = collection1.get(name);
        return (Diagram)de;
    }

    public static final String DATA_RESOURCES_REPOSITORY = "../data_resources";
    public static final String EXAMPLE_DIAGRAMS_COLLECTION = "data/Collaboration/Iron metabolism/Data/Diagrams";
}
