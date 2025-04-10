package biouml.plugins.bionetgen.diagram;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.StringUtils;

import ru.biosoft.util.TextUtil2;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.bionetgen.bnglparser.BNGAction;
import biouml.plugins.bionetgen.bnglparser.BNGDescription;
import biouml.plugins.bionetgen.bnglparser.BNGEOL;
import biouml.plugins.bionetgen.bnglparser.BNGExpression;
import biouml.plugins.bionetgen.bnglparser.BNGLabel;
import biouml.plugins.bionetgen.bnglparser.BNGList;
import biouml.plugins.bionetgen.bnglparser.BNGModel;
import biouml.plugins.bionetgen.bnglparser.BNGMoleculeType;
import biouml.plugins.bionetgen.bnglparser.BNGObservable;
import biouml.plugins.bionetgen.bnglparser.BNGParameter;
import biouml.plugins.bionetgen.bnglparser.BNGRateLaw;
import biouml.plugins.bionetgen.bnglparser.BNGReaction;
import biouml.plugins.bionetgen.bnglparser.BNGSeedSpecie;
import biouml.plugins.bionetgen.bnglparser.BNGSpecies;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.plugins.bionetgen.bnglparser.BionetgenParser;
import biouml.plugins.bionetgen.bnglparser.SimpleNode;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;

public class BionetgenAstUpdater extends BionetgenAstCreator
{
    private Diagram diagram;
    private int callCounter = 0;

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }

    public void addElement(BNGStart bngStart, Variable var) throws Exception
    {
        BNGModel model = bngStart.getModel();
        if( model == null )
        {
            writeModel(diagram, bngStart);
            return;
        }
        String varName = var.getName();
        if( var instanceof VariableRole || "time".equals(varName) || "unknown".equals(varName) || varName.startsWith("$$") )
            return;
        BNGList list = model.getList(BNGList.PARAMETER);
        if( list == null )
        {
            writeParameters(diagram, model);
            list = model.getList(BNGList.PARAMETER);
            if( list == null )
                return;
            model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            return;
        }
        for( int i = 0; i < list.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node child = list.jjtGetChild(i);
            if( child instanceof BNGParameter && varName.equals(child.getName()) )
                return;
        }
        var.getAttributes().remove(Bionetgen.BIONETGEN_LINK);
        writeParameter(diagram, list, var);
    }

    public void addElement(BNGStart bngStart, DiagramElement de) throws Exception
    {
        BNGModel model = bngStart.getModel();
        if( model == null )
        {
            writeModel(diagram, bngStart);
            return;
        }

        de.getAttributes().remove(Bionetgen.BIONETGEN_LINK);
        if( de instanceof Node )
        {
            if( BionetgenUtils.isSpecies(de) )
                addSpecies(model, (Node)de);
            else if( BionetgenUtils.isMolecule(de) )
                addMolecule(model, (Node)de);
            else if( BionetgenUtils.isObservable(de) )
                addObservable(model, (Node)de);
            else if( BionetgenUtils.isMoleculeComponent(de) )
                addMoleculeComponent(model, (Node)de);
            else if( BionetgenUtils.isReaction(de) )
                addReaction(model, (Node)de);
            else if( BionetgenUtils.isEquation(de) )
                addInitialAssignment(model, (Node)de);
            else if( BionetgenUtils.isMoleculeType(de) )
                addMoleculeType(model, (Node)de);
        }
        else if( de instanceof Edge )
        {
            addEdge( (Edge)de );
        }
    }

    private void addMoleculeType(BNGModel model, Node moleculeType) throws Exception
    {
        BNGList list = model.getList(BNGList.MOLECULETYPE);
        if( list == null )
        {
            writeMoleculeTypes(diagram, model);
            if( model.getList(BNGList.MOLECULETYPE) != null )
                model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            model.reinitMoleculeTypes();
            return;
        }
        writeMoleculeType(list, moleculeType);
        model.reinitMoleculeTypes();
    }

    private void addInitialAssignment(BNGModel model, Node eqNode) throws Exception
    {
        Equation nodeRole = eqNode.getRole(Equation.class);
        String varName = nodeRole.getVariable();
        if( "unknown".equals(varName) )
            return;
        EModel diagramRole = diagram.getRole(EModel.class);
        Variable var = diagramRole.getVariable(varName);
        BNGList list;
        if( var instanceof VariableRole )
        {
            list = model.getList(BNGList.SPECIES);
            if( list == null )
            {
                writeSpeciesList(diagram, model);
                list = model.getList(BNGList.SPECIES);
                if( list == null )
                    return;
                model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
                return;
            }
            DiagramElement diagramElement = ( (VariableRole)var ).getDiagramElement();
            Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(diagramElement);
            if( astNodes == null )
                return;
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
            {
                if( astNode instanceof BNGSeedSpecie )
                {
                    for( int j = 0; j < astNode.jjtGetNumChildren(); j++ )
                    {
                        biouml.plugins.bionetgen.bnglparser.Node currentChild = astNode.jjtGetChild(j);
                        if( currentChild instanceof BNGExpression )
                        {
                            BNGExpression expr = rewriteExpression(nodeRole.getFormula(), (BNGExpression)currentChild);
                            astNode.jjtAddChild(expr, j);
                            expr.jjtSetParent(astNode);
                            relink(diagramElement, currentChild, expr);
                            link(eqNode, expr);
                            link(eqNode, astNode);
                            return;
                        }
                    }
                }
            }
        }
        else
        {
            list = model.getList(BNGList.PARAMETER);
            if( list == null )
            {
                writeParameters(diagram, model);
                list = model.getList(BNGList.PARAMETER);
                if( list == null )
                    return;
                model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            }
            for( int i = 0; i < list.jjtGetNumChildren(); i++ )
            {
                biouml.plugins.bionetgen.bnglparser.Node child = list.jjtGetChild(i);
                if( child instanceof BNGParameter && child.getName().equals(varName) )
                {
                    for( int j = 0; j < child.jjtGetNumChildren(); j++ )
                    {
                        if( child.jjtGetChild(j) instanceof BNGExpression )
                        {
                            BNGExpression expr = rewriteExpression(nodeRole.getFormula(), (BNGExpression)child.jjtGetChild(j));
                            child.jjtAddChild(expr, j);
                            expr.jjtSetParent(child);
                            if( var == null )
                            {
                                var = new Variable(varName, diagramRole, diagramRole.getVariables());
                                diagramRole.put(var);
                            }
                            relink(var.getAttributes(), child.jjtGetChild(j), expr);
                            link(eqNode, expr);
                            link(eqNode, child);
                            return;
                        }
                    }
                }
            }
            if( var == null )
                diagramRole.put(new Variable(varName, diagramRole, diagramRole.getVariables()));
            else
                writeParameter(diagram, list, var);
        }
    }

    private void addSpecies(BNGModel model, Node species) throws Exception
    {
        BionetgenSpeciesGraph bsg = new BionetgenSpeciesGraph(species.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR));
        if( BionetgenUtils.isOtherType(species) )
        {
            if( !model.checkMoleculesTypeOf(bsg, true) )
            {
                diagram.remove(species.getName());
                throw new BionetgenUtils.BionetgenException( "Can't add new species. Reason: invalid molecule type in \"" + bsg.toString()
                        + "\"." );
            }
            return;
        }
        else if( BionetgenUtils.isStartType(species) )
        {
            if( !model.checkMoleculesTypeOf(bsg, false) )
            {
                diagram.remove(species.getName());
                throw new BionetgenUtils.BionetgenException( "Can't add new species. Reason: invalid molecule type in \"" + bsg.toString()
                        + "\"." );
            }
            BNGList seedSpeciesList = model.getList(BNGList.SPECIES);
            if( seedSpeciesList == null )
            {
                writeSpeciesList(diagram, model);
                if( model.getList(BNGList.SPECIES) != null )
                    model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
                return;
            }
            writeSpecies(seedSpeciesList, species);
        }
    }

    private void addMolecule(BNGModel model, Node molecule) throws Exception
    {
        if( callCounter != 0 )
            return;
        Compartment parent = molecule.getCompartment();
        DynamicProperty dp = parent.getAttributes().getProperty(BionetgenConstants.GRAPH_ATTR);
        BionetgenSpeciesGraph oldGraph = new BionetgenSpeciesGraph(dp.getValue().toString());
        BionetgenSpeciesGraph newGraph = recreateGraph(parent);
        if( newGraph == null || newGraph.isomorphicTo(oldGraph) )
            return;
        if( !model.checkMoleculesTypeOf(newGraph, BionetgenUtils.isOtherType(parent)) )
        {
            ++callCounter;
            parent.remove(molecule.getName());
            --callCounter;
            throw new BionetgenUtils.BionetgenException( "Can't add new molecule. Reason: invalid molecule type in \"" + newGraph + "\"." );
        }
        ++callCounter;
        dp.setValue(newGraph.toString());
        --callCounter;
    }

    private BionetgenSpeciesGraph recreateGraph(Compartment parent)
    {
        String molecules = parent.stream( Node.class ).filter( BionetgenUtils::isMolecule )
                .map( node -> node.getAttributes().getValueAsString( BionetgenConstants.MOLECULE_ATTR ) ).joining( "." );
        if( molecules.isEmpty() )
            return null;
        return new BionetgenSpeciesGraph( molecules );
    }

    private void addMoleculeComponent(BNGModel model, Node moleculeComponent) throws Exception
    {
        if( callCounter != 0 )
            return;
        Compartment parent = moleculeComponent.getCompartment();
        DynamicProperty dp = parent.getAttributes().getProperty(BionetgenConstants.MOLECULE_ATTR);
        BionetgenMolecule oldMol = new BionetgenMolecule(new BionetgenSpeciesGraph(""), dp.getValue().toString());
        BionetgenMolecule newMol = recreateMolecule(parent);
        if( newMol == null || newMol.compareTo(oldMol) == 0 )
            return;
        if( !model.checkMoleculeTypeOf(newMol, BionetgenUtils.isOtherType((DiagramElement)parent.getParent())) )
        {
            ++callCounter;
            parent.remove(moleculeComponent.getName());
            --callCounter;
            throw new BionetgenUtils.BionetgenException( "Can't add new molecule component. Reason: invalid molecule type \"" + newMol
                    + "\"." );
        }
        ++callCounter;
        dp.setValue(newMol.toString());
        --callCounter;
    }

    private BionetgenMolecule recreateMolecule(Compartment parent)
    {
        String molComps = parent.stream( Node.class ).filter( BionetgenUtils::isMoleculeComponent ).map( Node::getTitle ).joining( "," );
        String moleculeName = TextUtil2.split(parent.getAttributes().getValueAsString(BionetgenConstants.MOLECULE_ATTR), '(')[0];
        if( molComps.isEmpty() )
            return new BionetgenMolecule(new BionetgenSpeciesGraph(""), moleculeName);
        return new BionetgenMolecule( new BionetgenSpeciesGraph( "" ), moleculeName + "(" + molComps + ")" );
    }

    private void addObservable(BNGModel model, Node observable) throws Exception
    {
        BNGList observableList = model.getList(BNGList.OBSERVABLES);
        if( observableList == null )
        {
            writeObservables(diagram, model);
            if( model.getList(BNGList.OBSERVABLES) != null )
                model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            return;
        }
        writeObservable(observableList, observable);
    }

    private void addReaction(BNGModel model, Node reactionNode) throws Exception
    {
        BNGList reactionList = model.getList(BNGList.REACTIONS);
        if( reactionList == null )
        {
            writeReactions(diagram, model);
            if( model.getList(BNGList.REACTIONS) != null )
                model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            return;
        }
        writeReaction(reactionList, reactionNode);
    }

    private void addEdge(Edge edge)
    {
        if( BionetgenUtils.isBngEdge( edge ) )
        {
            if( callCounter != 0 )
                return;
            Node parentNode = (Node)edge.getParent();
            if( BionetgenUtils.isSpecies(parentNode) )
            {
                int number = new BionetgenSpeciesGraph(parentNode.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR))
                        .getEdgesNumber() / 2 + 1;
                String newBind = "!" + number;
                edge.nodes().forEach( node -> node.setTitle( node.getTitle()+newBind ) );
            }
        }
    }

    public void removeElement(Variable var)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(var.getAttributes()
                .getProperty(Bionetgen.BIONETGEN_LINK));
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            astNode.remove();
        }
    }

    public void removeElement(BNGModel model, DiagramElement de) throws Exception
    {
        DynamicProperty dp;
        if( callCounter == 0 )
        {
            Compartment parentComp;
            if( BionetgenUtils.isMolecule(de) )
            {
                parentComp = de.getCompartment();
                BionetgenSpeciesGraph newGraph = recreateGraph(parentComp);
                dp = parentComp.getAttributes().getProperty(BionetgenConstants.GRAPH_ATTR);
                if( dp == null )
                {
                    dp = new DynamicProperty(BionetgenConstants.GRAPH_ATTR, String.class, "");
                    parentComp.getAttributes().add(dp);
                }
                if( newGraph != null )
                {
                    ++callCounter;
                    dp.setValue(newGraph.toString());
                    --callCounter;
                }
            }
            else if( BionetgenUtils.isMoleculeComponent(de) )
            {
                parentComp = de.getCompartment();
                BionetgenMolecule newMolecule = recreateMolecule(parentComp);
                dp = parentComp.getAttributes().getProperty(BionetgenConstants.MOLECULE_ATTR);
                if( dp == null )
                {
                    dp = new DynamicProperty(BionetgenConstants.MOLECULE_ATTR, String.class, "");
                    parentComp.getAttributes().add(dp);
                }
                if( newMolecule != null )
                {
                    ++callCounter;
                    dp.setValue(newMolecule.toString());
                    --callCounter;
                }
                for( Edge edge : ( (Node)de ).getEdges() )
                {
                    ( (Compartment)parentComp.getParent() ).remove(edge.getName());
                }
            }
        }
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(de);
        if( astNodes == null )
            return;
        List<biouml.plugins.bionetgen.bnglparser.Node> nodesToChange = new ArrayList<>();
        if( de instanceof Node )
        {
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
            {
                if( ( astNode instanceof BNGParameter || astNode instanceof BNGSeedSpecie ) && BionetgenUtils.isEquation(de) )
                    continue;
                nodesToChange.add(astNode);
            }
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : nodesToChange )
            {
                biouml.plugins.bionetgen.bnglparser.Node parent = astNode.jjtGetParent();
                int index = astNode.remove();
                if( BionetgenUtils.isEquation(de) && astNode instanceof BNGExpression )
                {
                    EModel diagramRole = diagram.getRole(EModel.class);
                    Variable parameter = diagramRole.getVariable(de.getRole(Equation.class).getVariable());
                    BNGExpression expr;
                    if( parameter instanceof VariableRole )
                    {
                        expr = rewriteExpression(String.valueOf(parameter.getInitialValue()), (BNGExpression)astNode);
                        parent.addChild(expr, index);
                        relink( ( (VariableRole)parameter ).getDiagramElement(), astNode, expr);
                    }
                    else
                    {
                        if( parameter != null )
                            expr = rewriteExpression(String.valueOf(parameter.getInitialValue()), (BNGExpression)astNode);
                        else
                        {
                            expr = rewriteExpression("0.0", (BNGExpression)astNode);
                            parameter = new Variable(de.getRole(Equation.class).getVariable(), diagramRole, diagramRole.getVariables());
                            diagramRole.put(parameter);
                        }
                        parent.addChild(expr, index);
                        relink(parameter.getAttributes(), astNode, expr);
                    }
                }
            }
            if( BionetgenUtils.isMoleculeType(de) )
                model.reinitMoleculeTypes();
        }
        else if( de instanceof Edge )
        {
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
            {
                nodesToChange.add(astNode);
            }
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : nodesToChange )
            {
                biouml.plugins.bionetgen.bnglparser.Node parent = astNode.jjtGetParent();
                int index = astNode.remove();
                if( index > 0 && parent instanceof BNGList && ( (BNGList)parent ).getType() == BNGList.REACTIONCOMPONENT )
                    parent.jjtGetChild(index - 1).remove();
            }
        }
    }

    public void changeInitialValue(Variable var, BNGModel model) throws Exception
    {
        DynamicProperty dp;
        boolean isVariableRole = var instanceof VariableRole;
        if( isVariableRole )
            dp = ( (VariableRole)var ).getDiagramElement().getAttributes().getProperty(Bionetgen.BIONETGEN_LINK);
        else
            dp = var.getAttributes().getProperty(Bionetgen.BIONETGEN_LINK);

        if( dp != null )
        {
            for( Equation eq : diagram.getRole(EModel.class).getEquations() )
            {
                if( eq.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT) && eq.getVariable().equals(var.getName()) )
                    return;
            }
            Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(dp);
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
            {
                if( astNode instanceof BNGExpression )
                {
                    biouml.plugins.bionetgen.bnglparser.Node parent = astNode.jjtGetParent();
                    int index = astNode.remove();
                    BNGExpression expr = rewriteExpression(String.valueOf(var.getInitialValue()), (BNGExpression)astNode);
                    parent.addChild(expr, index);
                    if( isVariableRole )
                        relink( ( (VariableRole)var ).getDiagramElement(), astNode, expr);
                    else
                        relink(var.getAttributes(), astNode, expr);
                    return;
                }
            }
        }
        if( isVariableRole )
            return;
        writeParameter(diagram, model.getList(BNGList.PARAMETER), var);
    }

    public void changeMatchOnce(DiagramElement de, boolean matchOnce)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(de);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGObservable )
            {
                ( (BNGObservable)astNode ).setMatchOnce(matchOnce);
                break;
            }
        }
    }

    public void changeComment(Option option, String newComment, BNGStart start)
    {
        if( option instanceof Diagram )
        {
            BNGDescription description = start.getDescription();
            if( description == null )
            {
                writeDescription((Diagram)option, start);
            }
            else
            {
                description.remove();
                writeDescription((Diagram)option, start);
            }
        }
        else if( option instanceof Variable )
        {
            Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes( ( (Variable)option ).getAttributes().getProperty(
                    Bionetgen.BIONETGEN_LINK));
            if( astNodes == null )
                return;
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
            {
                if( astNode instanceof BNGParameter )
                {
                    ( (BNGParameter)astNode ).changeComment(newComment);
                    break;
                }
            }
        }
        else if( option instanceof Node )
        {
            Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes((Node)option);
            if( astNodes == null )
                return;
            if( BionetgenUtils.isReaction((Node)option) )
            {
                for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
                {
                    if( astNode instanceof BNGReaction )
                    {
                        ( (BNGReaction)astNode ).changeComment(newComment);
                        break;
                    }
                }
            }
            else if( BionetgenUtils.isObservable((Node)option) )
            {
                for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
                {
                    if( astNode instanceof BNGObservable )
                    {
                        ( (BNGObservable)astNode ).changeComment(newComment);
                        break;
                    }
                }
            }
            else if( BionetgenUtils.isSpecies((Node)option) )
            {
                for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
                {
                    if( astNode instanceof BNGSeedSpecie )
                    {
                        ( (BNGSeedSpecie)astNode ).changeComment(newComment);
                        break;
                    }
                }
            }
            else if( BionetgenUtils.isMoleculeType((Node)option) )
            {
                for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
                {
                    if( astNode instanceof BNGMoleculeType )
                    {
                        ( (BNGMoleculeType)astNode ).changeComment(newComment);
                        break;
                    }
                }
            }
        }
    }

    public void changeSeedSpeciesConstancy(Node node, boolean isConstant)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(node);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGSeedSpecie )
            {
                ( (BNGSeedSpecie)astNode ).setConstant(isConstant);
                break;
            }
        }
    }

    public void changeTypeAttribute(Node node, boolean oldValue, boolean newValue, BNGModel model) throws Exception
    {
        if( oldValue == newValue )
            return;
        if( newValue )
        {
            BNGList seedSpeciesList = model.getList(BNGList.SPECIES);
            if( seedSpeciesList == null )
            {
                writeSpeciesList( Diagram.getDiagram( node ), model );
                return;
            }
            String graphAttrString = node.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR);
            for( int i = 0; i < seedSpeciesList.jjtGetNumChildren(); i++ )
            {
                biouml.plugins.bionetgen.bnglparser.Node child = seedSpeciesList.jjtGetChild(i);
                if( child instanceof BNGSeedSpecie )
                {
                    for( int j = 0; j < child.jjtGetNumChildren(); j++ )
                    {
                        biouml.plugins.bionetgen.bnglparser.Node species = child.jjtGetChild(j);
                        if( species instanceof BNGSpecies && species.getName().equals(graphAttrString) )
                            return;
                    }
                }
            }
            writeSpecies(seedSpeciesList, node);
        }
        else
        {
            Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(node);
            if( astNodes == null )
                return;
            for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
            {
                if( astNode instanceof BNGSeedSpecie )
                {
                    astNode.remove();
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                        astNodes.remove(astNode.jjtGetChild(i));
                    astNodes.remove(astNode);
                    break;
                }
            }
        }
    }

    public void changeGraph(DiagramElement de, String oldGraph, String newGraph, BNGModel model) throws Exception
    {
        boolean shouldRebuild = true;
        if( newGraph == null || newGraph.isEmpty() )
        {
            de.getAttributes().getProperty(BionetgenConstants.GRAPH_ATTR).setValue(oldGraph);
            throw new BionetgenUtils.BionetgenException( "Graph attribute mustn't be null or empty. Previous value will be restored." );
        }
        BionetgenSpeciesGraph newBSG;
        BionetgenSpeciesGraph oldBSG;
        try
        {
            newBSG = new BionetgenSpeciesGraph(newGraph);
        }
        catch( IllegalArgumentException e )
        {
            de.getAttributes().getProperty(BionetgenConstants.GRAPH_ATTR).setValue(oldGraph);
            throw new BionetgenUtils.BionetgenException( "New graph attribute is invalid: " + e.getMessage()
                    + ". Previous value will be restored." );
        }
        try
        {
            oldBSG = new BionetgenSpeciesGraph(oldGraph);
            if( !model.checkMoleculesTypeOf(newBSG, BionetgenUtils.isOtherType(de)) )
            {
                de.getAttributes().getProperty(BionetgenConstants.GRAPH_ATTR).setValue(oldGraph);
                throw new BionetgenUtils.BionetgenException( "Can't change graph attribute. Reason: invalid molecule type in \"" + newGraph
                        + "\"." );
            }
            if( oldBSG.isomorphicTo(newBSG) )
                shouldRebuild = false;
        }
        catch( IllegalArgumentException e )
        {
        }

        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(de);
        if( astNodes == null )
            return;
        List<biouml.plugins.bionetgen.bnglparser.Node> nodesToChange = new ArrayList<>();
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGSpecies )
            {
                nodesToChange.add(astNode);
            }
        }
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : nodesToChange )
        {
            BNGSpecies species = BionetgenUtils.generateSpecies(newGraph);
            if( species == null )
            {
                de.getAttributes().getProperty(BionetgenConstants.GRAPH_ATTR).setValue(oldGraph);
                throw new BionetgenUtils.BionetgenException( "Invalid species graph: '" + newGraph + "'.  Previous value will be restored." );
            }
            species.transferSpecialTokens((BNGSpecies)astNode);
            astNode.jjtGetParent().addChild(species, astNode.remove());
            relink(de, astNode, species);
        }
        if( callCounter != 0 || !shouldRebuild )
            return;
        ++callCounter;
        try
        {
            BionetgenUtils.rebuildSpecies((Compartment)de);
        }
        finally
        {
            --callCounter;
        }
    }

    public void changeMolecule(DiagramElement de, String oldValue, String newValue, BNGModel model) throws Exception
    {
        Compartment parent = de.getCompartment();
        if( newValue == null || newValue.isEmpty() )
        {
            de.getAttributes().getProperty(BionetgenConstants.MOLECULE_ATTR).setValue(oldValue);
            throw new BionetgenUtils.BionetgenException( "Molecule attribute mustn't be null or empty. Previous value will be restored." );
        }
        BionetgenMolecule newMol;
        try
        {
            newMol = new BionetgenMolecule(new BionetgenSpeciesGraph(""), newValue);
        }
        catch( IllegalArgumentException e )
        {
            de.getAttributes().getProperty(BionetgenConstants.MOLECULE_ATTR).setValue(oldValue);
            throw new BionetgenUtils.BionetgenException( "New molecule attribute is invalid: " + e.getMessage()
                    + ". Previous value will be restored." );
        }
        if( !model.checkMoleculeTypeOf(newMol, BionetgenUtils.isOtherType(parent)) )
        {
            de.getAttributes().getProperty(BionetgenConstants.MOLECULE_ATTR).setValue(oldValue);
            BionetgenUtils.rebuildMolecule((Compartment)de);
            throw new BionetgenUtils.BionetgenException( "Can't change molecule attribute. Reason: invalid molecule type \"" + newValue
                    + "\"." );
        }

        if( !BionetgenUtils.isMolecule(de) || !BionetgenUtils.isSpecies(parent) )
            return;
        DynamicPropertySet parentAttributes = parent.getAttributes();
        DynamicProperty dp = parentAttributes.getProperty(BionetgenConstants.GRAPH_ATTR);
        if( dp == null )
        {
            dp = new DynamicProperty(BionetgenConstants.GRAPH_ATTR, String.class, "");
            parentAttributes.add(dp);
        }
        ++callCounter;
        try
        {
            if( callCounter == 1 )
                BionetgenUtils.rebuildMolecule((Compartment)de);
            dp.setValue(recreateGraph(parent).toString());
        }
        finally
        {
            --callCounter;
        }
    }

    public void changeMoleculeComponent(DiagramElement de, String oldName, BNGModel model) throws Exception
    {
        if( callCounter != 0 || !BionetgenUtils.isMoleculeComponent(de) )
            return;
        Compartment parent = de.getCompartment();
        if( !BionetgenUtils.isMolecule(parent) )
            return;
        DynamicPropertySet parentAttributes = parent.getAttributes();
        DynamicProperty dp = parentAttributes.getProperty(BionetgenConstants.MOLECULE_ATTR);
        if( dp == null )
        {
            dp = new DynamicProperty(BionetgenConstants.MOLECULE_ATTR, String.class, "");
            parentAttributes.add(dp);
        }
        BionetgenMolecule newMolecule = recreateMolecule(parent);
        if( !model.checkMoleculeTypeOf(newMolecule, BionetgenUtils.isOtherType((Node)parent.getParent())) )
        {
            String newName = de.getTitle();
            de.setTitle(oldName);
            throw new BionetgenUtils.BionetgenException( "Can't change molecule component's name. Reason: invalid molecule type \""
                    + newName
                    + "\"." );
        }
        ++callCounter;
        dp.setValue(newMolecule.toString());
        --callCounter;
    }

    public void changeMoleculeType(Node node, String oldValue, String newValue, BNGModel model) throws Exception
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(node);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGMoleculeType )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    biouml.plugins.bionetgen.bnglparser.Node child = astNode.jjtGetChild(i);
                    if( child instanceof BNGSpecies )
                    {
                        BNGSpecies species = BionetgenUtils.generateSpecies(newValue);
                        if( species == null || species.getName().contains(".") )
                        {
                            node.getAttributes().getProperty(BionetgenConstants.MOLECULE_TYPE_ATTR).setValue(oldValue);
                            throw new BionetgenUtils.BionetgenException( "Invalid molecule type: '" + newValue + "'." );
                        }
                        species.transferSpecialTokens((BNGSpecies)child);
                        astNode.jjtAddChild(species, i);
                        break;
                    }
                }
            }
        }
        model.reinitMoleculeTypes();
    }

    public void changeFormula(DiagramElement de) throws Exception
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(de);
        if( astNodes == null )
            return;
        List<BNGExpression> nodesToChange = StreamEx.of( astNodes ).select( BNGExpression.class ).toList();
        for( BNGExpression astNode : nodesToChange )
        {
            biouml.plugins.bionetgen.bnglparser.Node parent = astNode.jjtGetParent();
            int index = astNode.remove();
            BNGExpression expr = rewriteExpression(de.getRole(Equation.class).getFormula(), astNode);
            parent.addChild(expr, index);
            relink(de, astNode, expr);
        }
    }

    public void changeVariableName(PropertyChangeEvent e, BNGModel model) throws Exception
    {
        Equation eq = (Equation)e.getSource();
        Node eqNode = (Node)eq.getDiagramElement();
        DynamicProperty dp = eqNode.getAttributes().getProperty(Bionetgen.BIONETGEN_LINK);
        if( dp != null )
        {
            removeElement(model, eqNode);
            eqNode.getAttributes().remove(Bionetgen.BIONETGEN_LINK);
        }
        addInitialAssignment(model, eqNode);
    }

    public void changeObservableName(DiagramElement observable, String newName)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(observable);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGObservable )
            {
                ( (BNGObservable)astNode ).changeName(newName);
                break;
            }
        }
    }

    public void changeContentAttribute(Node observable, String[] newValue) throws Exception
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(observable);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGObservable )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    biouml.plugins.bionetgen.bnglparser.Node child = astNode.jjtGetChild(i);
                    if( child instanceof BNGList && ( (BNGList)child ).getType() == BNGList.OBSERVABLECONTENT )
                    {
                        BNGList newContent = writeObservableContent(newValue);
                        newContent.transferListSpecialTokens((BNGList)child);
                        astNode.jjtAddChild(newContent, i);
                        newContent.jjtSetParent(astNode);
                        break;
                    }
                }
                break;
            }
        }
    }

    public void changeLabel(Node node, String newValue)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(node);
        if( astNodes == null )
            return;
        biouml.plugins.bionetgen.bnglparser.Node addLabelTo = null;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGLabel )
            {
                ( (BNGLabel)astNode ).setName(newValue);
                return;
            }
            else if( BionetgenUtils.isReaction(node) && astNode instanceof BNGReaction )
            {
                addLabelTo = astNode;
            }
            else if( BionetgenUtils.isSpecies(node) && BionetgenUtils.isStartType(node) && astNode instanceof BNGSeedSpecie )
            {
                addLabelTo = astNode;
            }
            else if( BionetgenUtils.isObservable(node) && astNode instanceof BNGObservable )
            {
                addLabelTo = astNode;
            }
            else if( BionetgenUtils.isMoleculeType(node) && astNode instanceof BNGMoleculeType )
            {
                addLabelTo = astNode;
            }
        }
        if( addLabelTo != null )
        {
            BNGLabel label = new BNGLabel(BionetgenParser.JJTLABEL);
            label.setName(newValue);
            link(node, label);
            addLabelTo.addChild(label, 0);
            label.jjtSetParent(addLabelTo);
        }
    }

    public void changeActionAttribute(Diagram diagram, DynamicProperty newValue)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(diagram);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGList && ( (BNGList)astNode ).getType() == BNGList.ACTION )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    biouml.plugins.bionetgen.bnglparser.Node child = astNode.jjtGetChild(i);
                    if( child instanceof BNGAction && newValue.getName().equals(child.getName()) )
                    {
                        BNGAction newAction = writeAction(newValue);
                        newAction.transferSpecialTokens((BNGAction)child);
                        astNode.jjtAddChild(newAction, i);
                        newAction.jjtSetParent(astNode);
                    }
                }
            }
        }
    }

    public void changeAdditionAttribute(Node reactionNode, String[] newValue)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(reactionNode);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGReaction )
            {
                boolean containsList = false;
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    biouml.plugins.bionetgen.bnglparser.Node child = astNode.jjtGetChild(i);
                    if( child instanceof BNGList && ( (BNGList)child ).getType() == BNGList.ADDITIONCOMPONENT )
                    {
                        BNGList newAddition = writeAddition(newValue);
                        newAddition.transferListSpecialTokens((BNGList)child);
                        astNode.jjtAddChild(newAddition, i);
                        newAddition.jjtSetParent(astNode);
                        containsList = true;
                        break;
                    }
                }
                if( !containsList )
                {
                    BNGList newAddition = writeAddition(newValue);
                    astNode.addChild(newAddition, astNode.jjtGetNumChildren() - 1);
                }
                break;
            }
        }
    }

    public void changeReversible(Node reactionNode, boolean isReversible) throws Exception
    {
        DynamicPropertySet dps = reactionNode.getAttributes();
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(dps.getProperty(Bionetgen.BIONETGEN_LINK));
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGReaction )
            {
                BNGReaction bngReaction = (BNGReaction)astNode;
                if( bngReaction.isReversible() == isReversible )
                    break;
                boolean canChangeReversible = BionetgenConstants.DEFAULT.equals( bngReaction.getRateLaw().getType() )
                        || BionetgenConstants.DEFAULT.equals(dps.getValueAsString(BionetgenConstants.RATE_LAW_TYPE_ATTR));
                if( !canChangeReversible )
                {
                    dps.setValue(BionetgenConstants.REVERSIBLE_ATTR, false);
                    break;
                }
                bngReaction.setReversible(isReversible);
                ++callCounter;
                if( isReversible )
                {
                    DynamicProperty dp = dps.getProperty(BionetgenConstants.BACKWARD_RATE_ATTR);
                    if( dp == null )
                    {
                        dp = new DynamicProperty(BionetgenConstants.BACKWARD_RATE_ATTR, String.class, "");
                        dps.add(dp);
                    }
                    if( dp.getValue().toString().isEmpty() )
                        dp.setValue(dps.getValueAsString(BionetgenConstants.FORWARD_RATE_ATTR));
                }
                --callCounter;
                if( callCounter == 0 )
                {
                    BNGRateLaw oldLaw = bngReaction.getRateLaw();
                    BNGRateLaw law = rewriteRateLaw(reactionNode, oldLaw);
                    astNode.addChild(law, oldLaw.remove());
                }
                break;
            }
        }
    }

    public void changeRateLaw(Node reactionNode, String attrName, PropertyChangeEvent evt) throws Exception
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(reactionNode);
        if( astNodes == null )
            return;
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            if( astNode instanceof BNGReaction )
            {
                ++callCounter;
                try
                {
                    String newValue = StringUtils.defaultString((String)evt.getNewValue());
                    String oldValue = StringUtils.defaultString((String)evt.getOldValue());
                    if( attrName.equals(BionetgenConstants.FORWARD_RATE_ATTR) )
                        changeForwardRate(reactionNode, oldValue, newValue);
                    else if( attrName.equals(BionetgenConstants.RATE_LAW_TYPE_ATTR) )
                        changeRateLawType(reactionNode, oldValue, newValue);
                    else if( attrName.equals(BionetgenConstants.BACKWARD_RATE_ATTR) )
                        changeBackwardRate(reactionNode, oldValue, newValue);
                }
                finally
                {
                    --callCounter;
                }
                if( callCounter == 0 )
                {
                    BNGRateLaw oldRateLaw = ( (BNGReaction)astNode ).getRateLaw();
                    BNGRateLaw law = rewriteRateLaw(reactionNode, oldRateLaw);
                    astNode.addChild(law, oldRateLaw.remove());
                }
                break;
            }
        }
    }

    private void changeBackwardRate(Node reactionNode, String oldValue, String newValue) throws Exception
    {
        DynamicPropertySet dps = reactionNode.getAttributes();
        if( newValue.isEmpty() )
        {
            if( !Boolean.parseBoolean(dps.getValueAsString(BionetgenConstants.REVERSIBLE_ATTR)) || oldValue.isEmpty() )
                return;
            dps.setValue(BionetgenConstants.BACKWARD_RATE_ATTR, oldValue);
            throw new BionetgenUtils.BionetgenException( "Backward rate attribute must not be empty! Previous value will be restored." );
        }
        else if( !BionetgenUtils.isCorrectExpression(newValue) )
        {
            dps.setValue(BionetgenConstants.BACKWARD_RATE_ATTR, oldValue);
            throw new BionetgenUtils.BionetgenException( "Invalid backward rate expression: '" + newValue
                    + "'. Previous value will be restored." );
        }
    }

    private void changeForwardRate(Node reactionNode, String oldValue, String newValue) throws Exception
    {
        DynamicPropertySet dps = reactionNode.getAttributes();
        if( newValue.isEmpty() )
        {
            dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, oldValue);
            throw new BionetgenUtils.BionetgenException( "Forward rate attribute must not be empty! Previous value will be restored" );
        }
        if( !BionetgenUtils.isCorrectRateFormula(newValue) )
        {
            dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, oldValue);
            throw new BionetgenUtils.BionetgenException( "Invalid forward rate expression: '" + newValue
                    + "'. Previous value will be restored." );
        }

        String type = dps.getValueAsString(BionetgenConstants.RATE_LAW_TYPE_ATTR);
        if( BionetgenConstants.MM.equals(type) )
        {
            if( !newValue.startsWith("MM") )
            {
                if( newValue.startsWith("Sat") )
                    dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.SATURATION);
                else
                    dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.DEFAULT);
            }
        }
        else if( BionetgenConstants.SATURATION.equals(type) )
        {
            if( !newValue.startsWith("Sat") )
            {
                if( newValue.startsWith("MM") )
                    dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.MM);
                else
                    dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.DEFAULT);
            }
        }
        else if( BionetgenConstants.DEFAULT.equals(type) )
        {
            if( newValue.startsWith("MM") )
                dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.MM);
            else if( newValue.startsWith("Sat") )
                dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.SATURATION);
        }
    }

    private void changeRateLawType(Node reactionNode, String oldValue, String newValue) throws Exception
    {
        if( oldValue.equals(newValue) )
            return;

        DynamicPropertySet dps = reactionNode.getAttributes();
        if( !BionetgenUtils.isValidRateLawType(newValue) )
        {
            dps.setValue(BionetgenConstants.RATE_LAW_TYPE_ATTR, oldValue);
            throw new BionetgenUtils.BionetgenException( "Invalid rate law type: '" + newValue + "'. Previous value will be restored." );
        }

        DynamicProperty dp = dps.getProperty(BionetgenConstants.FORWARD_RATE_ATTR);
        String oldLaw = dp.getValue().toString();
        if( BionetgenConstants.DEFAULT.equals(newValue) )
        {
            if( oldLaw.startsWith("MM") || oldLaw.startsWith("Sat") )
            {
                String[] rateConstants = TextUtil2.split(oldLaw.substring(oldLaw.indexOf('(') + 1, oldLaw.lastIndexOf(')')), ',');
                dp.setValue(rateConstants[0]);
                dp = dps.getProperty(BionetgenConstants.BACKWARD_RATE_ATTR);
                if( dp == null )
                {
                    dp = new DynamicProperty(BionetgenConstants.BACKWARD_RATE_ATTR, String.class, "");
                    dps.add(dp);
                }
                dp.setValue(rateConstants[1]);
                dps.setValue(BionetgenConstants.REVERSIBLE_ATTR, true);
            }
            else if( dps.getProperty(BionetgenConstants.BACKWARD_RATE_ATTR) != null )
                dps.setValue(BionetgenConstants.REVERSIBLE_ATTR, true);
        }
        else if( BionetgenConstants.DEFAULT.equals(oldValue) )
        {
            dps.setValue(BionetgenConstants.REVERSIBLE_ATTR, false);
            dp = dps.getProperty(BionetgenConstants.FORWARD_RATE_ATTR);
            if( ! ( BionetgenConstants.MM.equals(newValue) && oldLaw.startsWith("MM") )
                    && ! ( BionetgenConstants.SATURATION.equals(newValue) && oldLaw.startsWith("Sat") ) )
            {
                StringBuilder sb = new StringBuilder(newValue);
                sb.append("(");
                String s = dp.getValue().toString();
                sb.append(s);
                dp = dps.getProperty(BionetgenConstants.BACKWARD_RATE_ATTR);
                if( dp == null )
                    sb.append(",").append(s).append(")");
                else
                    sb.append(",").append(dp.getValue().toString()).append(")");
                dps.setValue(BionetgenConstants.FORWARD_RATE_ATTR, sb.toString());
            }
        }
        else
        {
            if( BionetgenConstants.MM.equals(newValue) && oldLaw.startsWith("Sat") )
                dp.setValue(oldLaw.replaceFirst("Sat", "MM"));
            else if( BionetgenConstants.SATURATION.equals(newValue) && oldLaw.startsWith("MM") )
                dp.setValue(oldLaw.replaceFirst("MM", "Sat"));
        }
    }

    protected BNGRateLaw rewriteRateLaw(Node reactionNode, BNGRateLaw oldLaw) throws Exception
    {
        BNGRateLaw law = writeRateLaw(reactionNode);
        if( oldLaw != null )
            law.transferRateLawSpecialTokens(oldLaw);
        return law;
    }

    protected BNGExpression rewriteExpression(String newValue, BNGExpression oldExpression) throws Exception
    {
        BNGExpression expr = BionetgenUtils.generateExpression(newValue);
        if( oldExpression != null )
            expr.transferSpecialTokens(oldExpression);
        return expr;
    }

    private Set<biouml.plugins.bionetgen.bnglparser.Node> isHighlight = new HashSet<>();
    public void highlight(DiagramElement de)
    {
        for( biouml.plugins.bionetgen.bnglparser.Node astNode : isHighlight )
            ( (SimpleNode)astNode ).setHighlight(false);
        isHighlight = new HashSet<>();

        DiagramElement deToHighlight = de;
        if( deToHighlight instanceof Diagram )
            return;
        if( BionetgenUtils.isMolecule(deToHighlight) || BionetgenUtils.isMoleculeComponent(deToHighlight) )
        {
            if( !BionetgenUtils.isSpecies(deToHighlight = (DiagramElement)de.getParent()) )
                if( !BionetgenUtils.isSpecies(deToHighlight = (DiagramElement)deToHighlight.getParent()) )
                    return;
        }
        Set<biouml.plugins.bionetgen.bnglparser.Node> astNodes = getLinkedAstNodes(deToHighlight);
        if( astNodes == null )
            return;

        for( biouml.plugins.bionetgen.bnglparser.Node astNode : astNodes )
        {
            ( (SimpleNode)astNode ).setHighlight(true);
            isHighlight.add(astNode);
        }
    }

    public static void relink(DiagramElement de, biouml.plugins.bionetgen.bnglparser.Node oldAstNode,
            biouml.plugins.bionetgen.bnglparser.Node newAstNode)
    {
        relink(de.getAttributes(), oldAstNode, newAstNode);
    }
    public static void relink(DynamicPropertySet dps, biouml.plugins.bionetgen.bnglparser.Node oldAstNode,
            biouml.plugins.bionetgen.bnglparser.Node newAstNode)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> set = getLinkedAstNodes(dps.getProperty(Bionetgen.BIONETGEN_LINK));
        if( set == null )
        {
            link(dps, newAstNode);
        }
        else
        {
            set.remove(oldAstNode);
            set.add(newAstNode);
        }
    }

    private static Set<biouml.plugins.bionetgen.bnglparser.Node> getLinkedAstNodes(DiagramElement de)
    {
        return getLinkedAstNodes(de.getAttributes().getProperty(Bionetgen.BIONETGEN_LINK));
    }
    private static Set<biouml.plugins.bionetgen.bnglparser.Node> getLinkedAstNodes(DynamicProperty dp)
    {
        return dp == null ? null : (Set<biouml.plugins.bionetgen.bnglparser.Node>)dp.getValue();
    }
}
