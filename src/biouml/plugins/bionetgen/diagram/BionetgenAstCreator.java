package biouml.plugins.bionetgen.diagram;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.bionetgen.bnglparser.BNGAction;
import biouml.plugins.bionetgen.bnglparser.BNGActionParameter;
import biouml.plugins.bionetgen.bnglparser.BNGAddition;
import biouml.plugins.bionetgen.bnglparser.BNGComment;
import biouml.plugins.bionetgen.bnglparser.BNGConstant;
import biouml.plugins.bionetgen.bnglparser.BNGDescription;
import biouml.plugins.bionetgen.bnglparser.BNGEOL;
import biouml.plugins.bionetgen.bnglparser.BNGExpression;
import biouml.plugins.bionetgen.bnglparser.BNGHash;
import biouml.plugins.bionetgen.bnglparser.BNGLabel;
import biouml.plugins.bionetgen.bnglparser.BNGList;
import biouml.plugins.bionetgen.bnglparser.BNGModel;
import biouml.plugins.bionetgen.bnglparser.BNGMoleculeType;
import biouml.plugins.bionetgen.bnglparser.BNGObservable;
import biouml.plugins.bionetgen.bnglparser.BNGParameter;
import biouml.plugins.bionetgen.bnglparser.BNGRateLaw;
import biouml.plugins.bionetgen.bnglparser.BNGReaction;
import biouml.plugins.bionetgen.bnglparser.BNGSeedSpecie;
import biouml.plugins.bionetgen.bnglparser.BNGSimpleElement;
import biouml.plugins.bionetgen.bnglparser.BNGSpecies;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.plugins.bionetgen.bnglparser.BionetgenParser;
import biouml.plugins.bionetgen.bnglparser.SimpleNode;
import biouml.plugins.bionetgen.bnglparser.Token;
import biouml.standard.type.SpecieReference;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

public class BionetgenAstCreator
{
    protected Logger log = Logger.getLogger(BionetgenAstCreator.class.getName());

    /**
     * If diagram have attribute "Bionetgen", then method returns astTree
     * which is generated from attribute.
     * If it haven't, then generates astTree from diagram
     * @param diagram
     * @return astTree
     */
    public BNGStart getAST(Diagram diagram)
    {
        BNGStart start = new BNGStart(BionetgenParser.JJTSTART);
        try
        {
            start.setName(diagram.getName());
            String bngText = BionetgenUtils.getBionetgenAttr(diagram);
            if( bngText != null )
            {
                start = generateAstFromText(bngText);
            }
            else
            {
                cleanLink(diagram);

                writeDescription(diagram, start);
                writeModel(diagram, start);
                writeActions(diagram, start);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can't create ASTTree from diagram: " + e.getMessage());
        }
        return start;
    }

    public static BNGStart generateAstFromText(String text) throws Exception
    {
        if( text.isEmpty() )
            return new BNGStart(BionetgenParser.JJTSTART);
        BionetgenParser parser = new BionetgenParser();
        BNGStart bngStart = parser.parse(null, new StringReader(text));
        if( parser.getStatus() != BionetgenParser.STATUS_OK )
            throw new BionetgenUtils.BionetgenException( parser.getMessages().toString() );
        return bngStart;
    }

    /**
     * Creates description from diagram's comment and adds it in astTree
     * @param diagram diagram to write description from
     * @param start astTree to write description in
     */
    protected void writeDescription(Diagram diagram, BNGStart start)
    {
        String comment = diagram.getComment();
        if( comment == null || comment.isEmpty() )
            return;
        BNGDescription description = new BNGDescription(BionetgenParser.JJTDESCRIPTION);
        String[] lines;
        if( comment.contains("\r") )
            lines = TextUtil.split(comment.replaceAll("\r\n", "\n").replaceAll("\r", "\n"), '\n');
        else
            lines = TextUtil.split(comment, '\n');

        for( String line : lines )
        {
            BNGComment bngComment = new BNGComment(BionetgenParser.JJTCOMMENT);
            bngComment.setName(line);
            description.addAsLast(bngComment);
            writeEOL(description);
        }
        writeEOL(description);
        start.addChild(description, 0);
    }

    /**
     * Adds model block in astTree using diagram
     * @param diagram diagram to write from
     * @param start astTree to write in
     * @throws Exception
     */
    protected void writeModel(Diagram diagram, BNGStart start) throws Exception
    {
        BNGModel model = new BNGModel(BionetgenParser.JJTMODEL);
        BNGSimpleElement element;

        writeBegin(model);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.MODEL);
        createSpace(element);
        model.addAsLast(element);
        writeEOL(model);

        writeParameters(diagram, model);
        writeMoleculeTypes(diagram, model);
        writeSpeciesList(diagram, model);
        writeObservables(diagram, model);
        writeReactions(diagram, model);

        if( model.containsList() )
            writeEOL(model);
        writeEnd(model);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.MODEL);
        createSpace(element);
        model.addAsLast(element);
        writeEOL(model);

        start.addAsLast(model);
    }

    /**
     * Creates molecule types block using diagram
     * @param diagram
     * @return created molecule types block
     * @throws Exception
     */
    protected void writeMoleculeTypes(Diagram diagram, BNGModel model) throws Exception
    {
        boolean exists = false;
        BNGList molTypes = new BNGList(BionetgenParser.JJTLIST);
        molTypes.setType(BNGList.MOLECULETYPE);
        BNGSimpleElement element;

        writeBegin(molTypes);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.MOLECULE_TYPES);
        createSpace(element);
        molTypes.addAsLast(element);
        writeEOL(molTypes);

        for( Node node : diagram.stream( Node.class ).filter( BionetgenUtils::isMoleculeType ) )
        {
            writeMoleculeType(molTypes, node);
            exists = true;
        }

        writeEnd(molTypes);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.MOLECULE_TYPES);
        createSpace(element);
        molTypes.addAsLast(element);
        writeEOL(molTypes);

        if( exists )
        {
            model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            model.addInBlock(molTypes);
        }
    }

    protected void writeMoleculeType(BNGList molTypes, Node node) throws Exception
    {
        BNGMoleculeType moleculeType = new BNGMoleculeType(BionetgenParser.JJTMOLECULETYPE);

        DynamicPropertySet attributes = node.getAttributes();
        String labelString = attributes.getValueAsString(BionetgenConstants.LABEL_ATTR);
        if( labelString != null && !labelString.isEmpty() )
        {
            BNGLabel label = new BNGLabel(BionetgenParser.JJTLABEL);
            label.setName(labelString);
            createIndent(label);
            moleculeType.addAsLast(label);
            link(node, label);
        }

        String molTypeGraph = attributes.getValueAsString(BionetgenConstants.MOLECULE_TYPE_ATTR);
        BNGSpecies species = BionetgenUtils.generateSpecies(molTypeGraph);
        if( species == null || molTypeGraph.contains(".") )
            throw new BionetgenUtils.BionetgenException( "Invalid molecule type: '" + molTypeGraph + "'." );
        createIndent(species);
        moleculeType.addAsLast(species);
        String commentStr = node.getComment();
        if( commentStr != null && !commentStr.isEmpty() )
        {
            BNGComment comment = new BNGComment(BionetgenParser.JJTCOMMENT);
            comment.setName(commentStr);
            createIndent(comment);
            writeEOL(comment);

            moleculeType.addAsLast(comment);
        }
        else
        {
            moleculeType.addAsLast(new BNGEOL(BionetgenParser.JJTEOL));
        }
        molTypes.addInBlock(moleculeType);
        link(node, moleculeType);
    }

    /**
     * Adds parameters block to the model block of astTree
     * @param diagram
     * @param model
     * @throws Exception
     */
    protected void writeParameters(Diagram diagram, BNGModel model) throws Exception
    {
        boolean exists = false;
        BNGList parameters = new BNGList(BionetgenParser.JJTLIST);
        parameters.setType(BNGList.PARAMETER);
        BNGSimpleElement element;

        writeBegin(parameters);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.PARAMETERS);
        createSpace(element);
        parameters.addAsLast(element);
        writeEOL(parameters);

        EModel emodel = (EModel)diagram.getRole();
        if( emodel == null )
            return;
        
        List<Variable> vars = emodel.getVariables().stream()
                .filter( var -> ! ( var.getName().equals( "time" ) || var.getName().equals( "unknown" ) || var.getName().contains( "$" ) ) )
                .sorted( Comparator.comparingDouble( Variable::getInitialValue ).reversed() ).collect( Collectors.toList() );

        for( Variable var : vars )
        {
            writeParameter(diagram, parameters, var);
            exists = true;
        }

        writeEnd(parameters);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.PARAMETERS);
        createSpace(element);
        parameters.addAsLast(element);
        writeEOL(parameters);

        if( exists )
        {
            model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            model.addInBlock(parameters);
        }
    }

    /**
     * Adds parameter corresponding given variable to the parameters block
     * @param diagram
     * @param parameters parameters block to add new parameter in
     * @param var variable parameter corresponds to
     * @throws Exception
     */
    protected void writeParameter(Diagram diagram, BNGList parameters, Variable var) throws Exception
    {
        boolean hasExpression = false;
        BNGParameter parameter = new BNGParameter(BionetgenParser.JJTPARAMETER);
        parameter.setName(var.getName());

        DynamicPropertySet variableAttrs = var.getAttributes();
        String labelString = variableAttrs.getValueAsString(BionetgenConstants.LABEL_ATTR);
        if( labelString != null && !labelString.isEmpty() )
        {
            BNGLabel label = new BNGLabel(BionetgenParser.JJTLABEL);
            label.setName(labelString);
            createIndent(label);
            parameter.addAsLast(label);
            link(variableAttrs, label);
        }

        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(var.getName());
        createIndent(element);
        parameter.addAsLast(element);

        BNGExpression expr = new BNGExpression(BionetgenParser.JJTEXPRESSION);
        for( Equation eq : diagram.getRole(EModel.class).getEquations() )
        {
            if( eq.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT) && eq.getVariable().equals(var.getName()) )
            {
                expr = BionetgenUtils.generateExpression(eq.getFormula());
                createIndent(expr);
                hasExpression = true;
                link(eq.getParent(), expr);
                link(eq.getParent(), parameter);
                break;
            }
        }
        if( !hasExpression )
        {
            expr = BionetgenUtils.generateExpression(String.valueOf(var.getInitialValue()));
            createIndent(expr);
            link(variableAttrs, expr);
        }
        createIndent(expr);
        parameter.addAsLast(expr);

        if( var.getComment() != null && !var.getComment().isEmpty() )
        {
            BNGComment comment = new BNGComment(BionetgenParser.JJTCOMMENT);
            comment.setName(var.getComment());
            createIndent(comment);
            writeEOL(comment);

            parameter.addAsLast(comment);
        }
        else
        {
            writeEOL(parameter);
        }
        link(variableAttrs, parameter);
        parameters.addInBlock(parameter);
    }

    /**
     * Adds seed species block to the model
     * @param diagram
     * @param model
     * @throws Exception
     */
    protected void writeSpeciesList(Diagram diagram, BNGModel model) throws Exception
    {
        boolean exists = false;
        BNGList speciesList = new BNGList(BionetgenParser.JJTLIST);
        speciesList.setType(BNGList.SPECIES);
        BNGSimpleElement element;

        writeBegin(speciesList);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.SEED_SPECIES);
        createSpace(element);
        speciesList.addAsLast(element);
        writeEOL(speciesList);

        for( Node node : diagram.getNodes() )
        {
            if( BionetgenUtils.isSpecies(node) && BionetgenUtils.isStartType(node) )
            {
                writeSpecies(speciesList, node);
                exists = true;
            }
        }

        writeEnd(speciesList);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.SEED_SPECIES);
        createSpace(element);
        speciesList.addAsLast(element);
        writeEOL(speciesList);

        if( exists )
        {
            model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            model.addInBlock(speciesList);
        }
    }

    /**
     * Adds seed species to the seed species block using given node
     * @param speciesList
     * @param node
     * @throws Exception
     */
    protected void writeSpecies(BNGList speciesList, Node node) throws Exception
    {
        BNGSeedSpecie species = new BNGSeedSpecie(BionetgenParser.JJTSEEDSPECIE);

        DynamicPropertySet attributes = node.getAttributes();

        String labelString = attributes.getValueAsString(BionetgenConstants.LABEL_ATTR);
        if( labelString != null && !labelString.isEmpty() )
        {
            BNGLabel label = new BNGLabel(BionetgenParser.JJTLABEL);
            label.setName(labelString);
            createIndent(label);
            species.addAsLast(label);
            link(node, label);
        }

        VariableRole role = node.getRole(VariableRole.class);
        boolean isConstant = role.isConstant();
        if( isConstant )
        {
            BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
            element.setName(BNGSeedSpecie.CONSTANCY_TAG);
            createIndent(element);
            species.addAsLast(element);
        }

        String graphString = attributes.getValueAsString(BionetgenConstants.GRAPH_ATTR);
        BNGSpecies specie = BionetgenUtils.generateSpecies(graphString);
        if( specie == null )
            throw new Exception("Invalid species graph: '" + graphString + "'.");
        if( !isConstant )
            createIndent(specie);
        species.addAsLast(specie);
        link(node, specie);

        BNGExpression expr = new BNGExpression(BionetgenParser.JJTEXPRESSION);
        boolean hasExpression = false;
        for( Equation eq : Diagram.getDiagram(node).getRole(EModel.class).getEquations() )
        {
            if( eq.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT) && eq.getVariable().equals(role.getName()) )
            {
                expr = BionetgenUtils.generateExpression(eq.getFormula());
                createIndent(expr);
                link(node, expr);
                link(eq.getParent(), expr);
                link(eq.getParent(), species);
                hasExpression = true;
                break;
            }
        }
        if( !hasExpression )
        {
            expr = BionetgenUtils.generateExpression(String.valueOf(role.getInitialValue()));
            createIndent(expr);
            link(node, expr);
        }
        species.addAsLast(expr);

        String commentString = node.getComment();
        if( commentString != null && !commentString.isEmpty() )
        {
            BNGComment comment = new BNGComment(BionetgenParser.JJTCOMMENT);
            comment.setName(commentString);
            createIndent(comment);
            writeEOL(comment);
            species.addAsLast(comment);
        }
        else
        {
            writeEOL(species);
        }
        link(node, species);
        speciesList.addInBlock(species);
    }

    /**
     * Adds observable block to the model block
     * @param diagram
     * @param model
     * @throws Exception
     */
    protected void writeObservables(Diagram diagram, BNGModel model) throws Exception
    {
        boolean exists = false;
        BNGList observablesList = new BNGList(BionetgenParser.JJTLIST);
        observablesList.setType(BNGList.OBSERVABLES);
        BNGSimpleElement element;

        writeBegin(observablesList);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.OBSERVABLES);
        createSpace(element);
        observablesList.addAsLast(element);
        writeEOL(observablesList);

        for( Node node : diagram.stream( Node.class ).filter( BionetgenUtils::isObservable ) )
        {
            writeObservable(observablesList, node);
            exists = true;
        }

        writeEnd(observablesList);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.OBSERVABLES);
        createSpace(element);
        observablesList.addAsLast(element);
        writeEOL(observablesList);

        if( exists )
        {
            model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            model.addInBlock(observablesList);
        }
    }

    /**
     * Adds observable to the observable block
     * @param observablesList
     * @param node
     * @throws Exception
     */
    protected void writeObservable(BNGList observablesList, Node node) throws Exception
    {
        BNGObservable observable = new BNGObservable(BionetgenParser.JJTOBSERVABLE);
        DynamicPropertySet attributes = node.getAttributes();
        observable.setMatchOnce(Boolean.valueOf(attributes.getValueAsString(BionetgenConstants.MATCH_ONCE_ATTR)));

        String labelString = attributes.getValueAsString(BionetgenConstants.LABEL_ATTR);
        if( labelString != null && !labelString.isEmpty() )
        {
            BNGLabel label = new BNGLabel(BionetgenParser.JJTLABEL);
            label.setName(labelString);
            createIndent(label);
            observable.addAsLast(label);
            link(node, label);
        }

        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        if( observable.isMatchOnce() )
            element.setName(BNGObservable.IS_MATCH_ONCE);
        else
            element.setName(BNGObservable.IS_NOT_MATCH_ONCE);
        createIndent(element);
        observable.addAsLast(element);

        observable.setName(node.getTitle());
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(node.getTitle());
        createIndent(element);
        observable.addAsLast(element);

        String[] contents = (String[])attributes.getValue(BionetgenConstants.CONTENT_ATTR);
        BNGList content = writeObservableContent(contents);
        observable.addAsLast(content);

        if( node.getComment() != null && !node.getComment().isEmpty() )
        {
            BNGComment comment = new BNGComment(BionetgenParser.JJTCOMMENT);
            comment.setName(node.getComment());
            createIndent(comment);
            writeEOL(comment);

            observable.addAsLast(comment);
        }
        else
        {
            writeEOL(observable);
        }
        observablesList.addInBlock(observable);
        link(node, observable);
    }

    /**
     * Adds observable content from node attribute to the observable
     * from the observables block
     * @param contents
     * @return
     * @throws Exception
     */
    protected BNGList writeObservableContent(@CheckForNull String[] contents) throws Exception
    {
        BNGList content = new BNGList(BionetgenParser.JJTLIST);
        content.setType(BNGList.OBSERVABLECONTENT);
        BNGSimpleElement element;
        BNGSpecies species;

        if( contents == null )
            return content;

        for( String contentItem : contents )
        {
            if( contentItem.isEmpty() )
                continue;
            try
            {
                new BionetgenSpeciesGraph(contentItem);
            }
            catch( Exception e )
            {
                continue;
            }
            if( content.jjtGetNumChildren() != 0 )
            {
                element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
                element.setName(",");
                content.addAsLast(element);

                species = BionetgenUtils.generateSpecies(contentItem);
                if( species == null )
                    throw new BionetgenUtils.BionetgenException( "Invalid species graph in observable content: '" + contentItem + "'." );
                createSpace(species);
                content.addAsLast(species);
            }
            else
            {
                species = BionetgenUtils.generateSpecies(contentItem);
                if( species == null )
                    throw new BionetgenUtils.BionetgenException( "Invalid species graph in observable content: '" + contentItem + "'." );
                createIndent(species);
                content.addAsLast(species);
            }
        }

        return content;
    }

    /**
     * Adds reaction rules block to the model block of astTree
     * @param diagram
     * @param model
     * @throws Exception
     */
    protected void writeReactions(Diagram diagram, BNGModel model) throws Exception
    {
        boolean exists = false;
        BNGList reactionsList = new BNGList(BionetgenParser.JJTLIST);
        reactionsList.setType(BNGList.REACTIONS);
        BNGSimpleElement element;

        writeBegin(reactionsList);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.REACTION_RULES);
        createSpace(element);
        reactionsList.addAsLast(element);
        writeEOL(reactionsList);

        for( Node node : diagram.stream( Node.class ).filter( BionetgenUtils::isReaction ) )
        {
            writeReaction(reactionsList, node);
            exists = true;
        }

        writeEnd(reactionsList);
        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.REACTION_RULES);
        createSpace(element);
        reactionsList.addAsLast(element);
        writeEOL(reactionsList);

        if( exists )
        {
            model.addInBlock(new BNGEOL(BionetgenParser.JJTEOL));
            model.addInBlock(reactionsList);
        }
    }

    /**
     * Adds reaction rule to the reaction rules block
     * @param reactionList
     * @param reactionNode
     * @throws Exception
     */
    protected void writeReaction(BNGList reactionList, Node reactionNode) throws Exception
    {
        BNGReaction bngReaction = new BNGReaction(BionetgenParser.JJTREACTION);

        DynamicPropertySet attributes = reactionNode.getAttributes();
        String labelString = attributes.getValueAsString(BionetgenConstants.LABEL_ATTR);
        if( labelString != null && !labelString.isEmpty() )
        {
            BNGLabel label = new BNGLabel(BionetgenParser.JJTLABEL);
            label.setName(labelString);
            createIndent(label);
            bngReaction.addAsLast(label);
            link(reactionNode, label);
        }

        writeReactionComponents(bngReaction, reactionNode, SpecieReference.REACTANT);

        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        Object isReversible = attributes.getValue(BionetgenConstants.REVERSIBLE_ATTR);
        if( isReversible != null && (Boolean)isReversible )
            element.setName("<->");
        else
            element.setName("->");
        createIndent(element);
        bngReaction.addAsLast(element);

        writeReactionComponents(bngReaction, reactionNode, SpecieReference.PRODUCT);
        bngReaction.addAsLast(writeRateLaw(reactionNode));
        writeAdditions(bngReaction, reactionNode);

        if( reactionNode.getComment() != null && !reactionNode.getComment().isEmpty() )
        {
            BNGComment comment = new BNGComment(BionetgenParser.JJTCOMMENT);
            comment.setName(reactionNode.getComment());
            createIndent(comment);
            writeEOL(comment);

            bngReaction.addAsLast(comment);
        }
        else
        {
            writeEOL(bngReaction);
        }
        reactionList.addInBlock(bngReaction);
        link(reactionNode, bngReaction);
    }

    /**
     * Writes additions (exclude/include reactants/products)
     * (if any) in the reaction rule
     * @param bngReaction
     * @param node
     */
    protected void writeAdditions(BNGReaction bngReaction, Node node)
    {
        String[] array;
        if( ( array = (String[])node.getAttributes().getValue(BionetgenConstants.ADDITION_ATTR) ) != null )
        {
            BNGList additions = writeAddition(array);
            bngReaction.addAsLast(additions);
        }
    }

    /**
     * Creates addition of the reaction rule from given array of String
     * @param additionsArray
     * @return
     */
    protected BNGList writeAddition(String[] additionsArray)
    {
        BNGList additionList = new BNGList(BionetgenParser.JJTLIST);
        additionList.setType(BNGList.ADDITIONCOMPONENT);
        for( int i = 0; i < additionsArray.length; i++ )
        {
            BNGAddition bngAddition = new BNGAddition(BionetgenParser.JJTADDITION);
            bngAddition.setName(additionsArray[i]);

            if( i == 0 )
                createIndent(bngAddition);
            else
                createSpace(bngAddition);

            additionList.addAsLast(bngAddition);
        }
        return additionList;
    }

    /**
     * Creates rate law of the reaction rule
     * @param reactionNode
     * @return
     * @throws Exception
     */
    protected BNGRateLaw writeRateLaw(Node reactionNode) throws Exception
    {
        BNGRateLaw law = new BNGRateLaw(BionetgenParser.JJTRATELAW);
        BNGExpression expr;
        BNGSimpleElement element;
        DynamicPropertySet attributes = reactionNode.getAttributes();
        String type = attributes.getValueAsString(BionetgenConstants.RATE_LAW_TYPE_ATTR);
        law.setType(type);
        String forwardRate = attributes.getValueAsString(BionetgenConstants.FORWARD_RATE_ATTR);
        if( BionetgenConstants.MM.equals( type ) || BionetgenConstants.SATURATION.equals( type ) )
        {
            int openBraceIndex = forwardRate.indexOf('(');
            int closeBraceIndex = forwardRate.lastIndexOf(')');
            int delimiterIndex = forwardRate.indexOf(',');

            element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
            element.setName(forwardRate.substring(0, openBraceIndex));
            createIndent(element);
            law.addAsLast(element);

            element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
            element.setName("(");
            law.addAsLast(element);

            expr = BionetgenUtils.generateExpression(forwardRate.substring(openBraceIndex + 1, delimiterIndex));
            law.addAsLast(expr);

            element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
            element.setName(",");
            law.addAsLast(element);

            expr = BionetgenUtils.generateExpression(forwardRate.substring(delimiterIndex + 1, closeBraceIndex));
            law.addAsLast(expr);

            element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
            element.setName(")");
            law.addAsLast(element);
        }
        else
        {
            expr = BionetgenUtils.generateExpression(forwardRate);
            createIndent(expr);
            law.addAsLast(expr);
            boolean isReversible = Boolean.parseBoolean(attributes.getValueAsString(BionetgenConstants.REVERSIBLE_ATTR));
            if( isReversible )
            {
                element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
                element.setName(",");
                law.addAsLast(element);

                expr = BionetgenUtils.generateExpression(attributes.getValueAsString(BionetgenConstants.BACKWARD_RATE_ATTR));
                createSpace(expr);
                law.addAsLast(expr);
            }
        }
        return law;
    }

    /**
     * Adds reaction components to reaction rule
     * @param bngReaction
     * @param reactionNode
     * @param role
     * @throws Exception
     */
    protected void writeReactionComponents(BNGReaction bngReaction, Node reactionNode, String role) throws Exception
    {
        BNGList listComponents = new BNGList(BionetgenParser.JJTLIST);
        listComponents.setType(BNGList.REACTIONCOMPONENT);
        listComponents.setName(role);
        List<Edge> boundedEdges = new ArrayList<>();
        BNGSpecies species;
        if( SpecieReference.REACTANT.equals(role) )
        {
            for( Edge edge : reactionNode.getEdges() )
            {
                if( edge.getOutput().equals(reactionNode) )
                    boundedEdges.add(edge);
            }
        }
        else
        {
            for( Edge edge : reactionNode.getEdges() )
            {
                if( edge.getInput().equals(reactionNode) )
                    boundedEdges.add(edge);
            }
        }
        for( int i = 0; i < boundedEdges.size(); i++ )
        {
            Node boundedNode;
            if( SpecieReference.REACTANT.equals(role) )
                boundedNode = boundedEdges.get(i).getInput();
            else
                boundedNode = boundedEdges.get(i).getOutput();

            species = writeReactionComponent(listComponents, boundedNode);

            link(boundedNode, species);
        }
        bngReaction.addAsLast(listComponents);
    }

    /**
     * Adds reaction component linked with given node to the reaction rule
     * @param listComponents
     * @param boundedNode
     * @return
     * @throws Exception if bounded node has incorrect species graph string
     */
    protected BNGSpecies writeReactionComponent(BNGList listComponents, Node boundedNode) throws Exception
    {
        int compIndex = listComponents.jjtGetNumChildren();
        if( compIndex != 0 )
        {
            BNGSimpleElement element;
            element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
            element.setName("+");
            createSpace(element);
            listComponents.addAsLast(element);
        }

        String graphString = boundedNode.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR);
        BNGSpecies species = BionetgenUtils.generateSpecies(graphString);
        if( species == null )
            throw new BionetgenUtils.BionetgenException( "Invalid species graph in reaction: '" + graphString + "'." );
        if( compIndex == 0 )
            createIndent(species);
        else
            createSpace(species);
        listComponents.addAsLast(species);
        return species;
    }

    protected void writeEOL(SimpleNode node)
    {
        BNGEOL eol = new BNGEOL(BionetgenParser.JJTEOL);
        node.addAsLast(eol);
    }

    protected void writeEnd(SimpleNode block)
    {
        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.END);
        block.addAsLast(element);
        if( block instanceof BNGList )
            ( (BNGList)block ).setEndIndex(block.jjtGetNumChildren());
        else if( block instanceof BNGModel )
            ( (BNGModel)block ).setEndIndex(block.jjtGetNumChildren());
    }

    protected void writeBegin(SimpleNode block)
    {
        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName(BionetgenConstants.BEGIN);
        block.addAsLast(element);
    }

    /**
     * Adds all actions from diagram attributes to the astTree
     * @param diagram
     * @param start
     */
    protected void writeActions(Diagram diagram, BNGStart start)
    {
        boolean exists = false;
        BNGList actions = new BNGList(BionetgenParser.JJTLIST);
        actions.setType(BNGList.ACTION);
        writeEOL(actions);
        DynamicPropertySet dps = diagram.getAttributes();
        for( DynamicProperty dp : dps )
        {
            if( dp.getType().equals(DynamicPropertySet.class) )
            {
                BNGAction action = writeAction(dp);
                actions.addAsLast(action);
                writeEOL(actions);
                exists = true;
            }
        }
        if( exists )
        {
            start.addAsLast(actions);
            link(diagram, actions);
        }
    }

    /**
     * Creates action from the given attribute
     * @param dynamicProperty
     * @return
     */
    protected BNGAction writeAction(DynamicProperty dynamicProperty)
    {
        BNGAction action = new BNGAction(BionetgenParser.JJTACTION);
        action.setName(dynamicProperty.getName());

        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName("({");
        action.addAsLast(element);

        DynamicPropertySet dpsam = (DynamicPropertySet)dynamicProperty.getValue();
        int i = 0;
        for( DynamicProperty dp : dpsam )
        {
            if( i != 0 )
            {
                element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
                element.setName(",");
                action.addAsLast(element);
            }
            if( BionetgenConstants.SAMPLE_TIMES_PARAM.equals(dp.getName()) )
                writeArrayParameter(dp, action);
            else
                writeActionParameter(dp, action);
            i++;
        }

        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName("})");
        action.addAsLast(element);

        return action;
    }

    private void writeActionParameter(DynamicProperty dp, BNGAction action)
    {
        BNGActionParameter param = new BNGActionParameter(BionetgenParser.JJTACTIONPARAMETER);
        param.setName(dp.getName());
        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName("=>");
        param.addAsLast(element);

        if( dp.getType().equals(DynamicPropertySet.class) )
        {
            writeHashValue((DynamicPropertySet)dp.getValue(), param);
        }
        else
        {
            BNGConstant constant = new BNGConstant(BionetgenParser.JJTCONSTANT);
            constant.setName(dp.getValue().toString());
            param.addAsLast(constant);
        }

        action.addAsLast(param);
    }

    private void writeArrayParameter(DynamicProperty dp, BNGAction action)
    {
        double[] sampleTimes = (double[])dp.getValue();
        if( sampleTimes.length < 2 )
        {
            log.warning("Sample times does not contain enougth elements (should be two or more).");
            return;
        }

        BNGActionParameter param = new BNGActionParameter(BionetgenParser.JJTACTIONPARAMETER);
        param.setName(dp.getName());
        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName("[");
        param.addAsLast(element);

        for( int i = 0; i < sampleTimes.length; i++ )
        {
            if( i != 0 )
            {
                element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
                element.setName(",");
                param.addAsLast(element);
            }
            BNGConstant constant = new BNGConstant(BionetgenParser.JJTCONSTANT);
            constant.setName(String.valueOf(sampleTimes[i]));
            param.addAsLast(constant);
        }

        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName("]");
        param.addAsLast(element);

        action.addAsLast(param);
    }

    private void writeHashValue(DynamicPropertySet dpsam, BNGActionParameter param)
    {
        BNGHash hash = new BNGHash(BionetgenParser.JJTHASH);
        BNGSimpleElement element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName("{");
        hash.addAsLast(element);

        int i = 0;
        BNGActionParameter hashParam;
        BNGConstant constant;
        for( DynamicProperty dp : dpsam )
        {
            if( i != 0 )
            {
                element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
                element.setName(",");
                hash.addAsLast(element);
            }
            hashParam = new BNGActionParameter(BionetgenParser.JJTACTIONPARAMETER);
            hashParam.setName(dp.getName());
            element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
            element.setName("=>");
            hashParam.addAsLast(element);
            constant = new BNGConstant(BionetgenParser.JJTCONSTANT);
            constant.setName(dp.getValue().toString());
            hashParam.addAsLast(constant);

            hash.addAsLast(hashParam);

            i++;
        }

        element = new BNGSimpleElement(BionetgenParser.JJTSIMPLEELEMENT);
        element.setName("}");
        hash.addAsLast(element);

        param.addAsLast(hash);
    }

    /**
     * add space before astNode
     */
    public static void createSpace(SimpleNode astNode)
    {
        if( astNode.jjtGetFirstToken() == null )
            astNode.jjtSetFirstToken(new Token());
        Token token = astNode.jjtGetFirstToken();
        if( token.image == null )
            token.image = astNode.getName();
        token.specialToken = new Token();
        token.specialToken.image = " ";
    }

    /**
     * add indent before astNode
     */
    public static void createIndent(SimpleNode astNode)
    {
        if( astNode.jjtGetFirstToken() == null )
            astNode.jjtSetFirstToken(new Token());
        Token token = astNode.jjtGetFirstToken();
        if( token.image == null )
            token.image = astNode.getName();
        token.specialToken = new Token();
        token.specialToken.image = "    ";
    }

    /**
     * Creates link with diagram element and astNode
     * (creates attribute in given diagram element)
     * @param de diagram element to link with
     * @param astNode astNode to link with
     */
    public static void link(DiagramElement de, biouml.plugins.bionetgen.bnglparser.Node astNode)
    {
        link(de.getAttributes(), astNode);
    }

    /**
     * Adds linking with bionetgen attribute to the given DynamicPropertySet
     * @param dps
     * @param astNode
     */
    public static void link(DynamicPropertySet dps, biouml.plugins.bionetgen.bnglparser.Node astNode)
    {
        Set<biouml.plugins.bionetgen.bnglparser.Node> set;
        DynamicProperty dp = dps.getProperty(Bionetgen.BIONETGEN_LINK);
        if( dp == null )
        {
            set = new HashSet<>();
            dp = new DynamicProperty(Bionetgen.BIONETGEN_LINK, Set.class, set);
            dp.setReadOnly(true);
            dp.setHidden(true);
            DPSUtils.makeTransient(dp);
            dps.add(dp);
        }
        else
        {
            set = (Set<biouml.plugins.bionetgen.bnglparser.Node>)dp.getValue();
        }
        set.add(astNode);
    }

    /**
     * To clean all old links in diagram
     * @param diagram
     * @throws Exception
     */
    private static void cleanLink(Diagram diagram) throws Exception
    {
        try
        {
            EModel emodel = (EModel)diagram.getRole();
            if( emodel == null )
                return;
            emodel.getVariables().stream().filter( var -> ! ( var instanceof VariableRole ) )
                    .forEach( var -> var.getAttributes().remove( Bionetgen.BIONETGEN_LINK ) );
            diagram.recursiveStream().forEach( node -> node.getAttributes().remove( Bionetgen.BIONETGEN_LINK ) );
        }
        catch( Throwable e )
        {
            throw new Exception( "Can't clean bionetgen link", e );
        }
    }
}
