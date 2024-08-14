package biouml.plugins.antimony;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.astparser_v2.AntimonyNotationParser;
import biouml.plugins.antimony.astparser_v2.AstAfter;
import biouml.plugins.antimony.astparser_v2.AstAssert;
import biouml.plugins.antimony.astparser_v2.AstAt;
import biouml.plugins.antimony.astparser_v2.AstColon;
import biouml.plugins.antimony.astparser_v2.AstColonEqual;
import biouml.plugins.antimony.astparser_v2.AstComma;
import biouml.plugins.antimony.astparser_v2.AstComment;
import biouml.plugins.antimony.astparser_v2.AstConversionFactor;
import biouml.plugins.antimony.astparser_v2.AstDelete;
import biouml.plugins.antimony.astparser_v2.AstEOL;
import biouml.plugins.antimony.astparser_v2.AstElse;
import biouml.plugins.antimony.astparser_v2.AstEqual;
import biouml.plugins.antimony.astparser_v2.AstEqualZero;
import biouml.plugins.antimony.astparser_v2.AstEquation;
import biouml.plugins.antimony.astparser_v2.AstFunction;
import biouml.plugins.antimony.astparser_v2.AstHas;
import biouml.plugins.antimony.astparser_v2.AstIs;
import biouml.plugins.antimony.astparser_v2.AstList;
import biouml.plugins.antimony.astparser_v2.AstMap;
import biouml.plugins.antimony.astparser_v2.AstModel;
import biouml.plugins.antimony.astparser_v2.AstPersistent;
import biouml.plugins.antimony.astparser_v2.AstPlus;
import biouml.plugins.antimony.astparser_v2.AstPriority;
import biouml.plugins.antimony.astparser_v2.AstProduct;
import biouml.plugins.antimony.astparser_v2.AstProperty;
import biouml.plugins.antimony.astparser_v2.AstRateEqual;
import biouml.plugins.antimony.astparser_v2.AstReactant;
import biouml.plugins.antimony.astparser_v2.AstReactionTitle;
import biouml.plugins.antimony.astparser_v2.AstReactionType;
import biouml.plugins.antimony.astparser_v2.AstRegularFormulaElement;
import biouml.plugins.antimony.astparser_v2.AstRelationshipType;
import biouml.plugins.antimony.astparser_v2.AstSemicolon;
import biouml.plugins.antimony.astparser_v2.AstSet;
import biouml.plugins.antimony.astparser_v2.AstSingleProperty;
import biouml.plugins.antimony.astparser_v2.AstStart;
import biouml.plugins.antimony.astparser_v2.AstStoichiometry;
import biouml.plugins.antimony.astparser_v2.AstSubSymbol;
import biouml.plugins.antimony.astparser_v2.AstSubstanceOnly;
import biouml.plugins.antimony.astparser_v2.AstSymbol;
import biouml.plugins.antimony.astparser_v2.AstSymbolType;
import biouml.plugins.antimony.astparser_v2.AstText;
import biouml.plugins.antimony.astparser_v2.AstTriggerInitialValue;
import biouml.plugins.antimony.astparser_v2.AstUnit;
import biouml.plugins.antimony.astparser_v2.AstUnitFormula;
import biouml.plugins.antimony.astparser_v2.AstUseValuesFromTriggerTime;
import biouml.plugins.antimony.astparser_v2.AstVarOrConst;
import biouml.plugins.antimony.astparser_v2.SimpleNode;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnComplexStructureManager;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbgn.Type;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Gene;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

public class AntimonyAstUpdater extends AntimonyAstCreator
{
    public AntimonyAstUpdater(Diagram diagram)
    {
        super(diagram);
    }

    /**
    * add element in astTree
    * @param astStart
    * @param de
    * @throws Exception
    */
    public void addElement(AstStart astStart, DiagramElement de) throws Exception
    {
        DynamicPropertySet dps = de.getAttributes();
        dps.remove(AntimonyConstants.ANTIMONY_LINK);

        AstModel astModel = getModel(de, astStart);

        if( astModel == null )
            return;
        Role role = de.getRole();
        if( de instanceof Node )
        {
            if( de instanceof ModelDefinition )
            {
                addModelDefinition(astStart.getStart(), (ModelDefinition)de, true);
                return;
            }
            else if( de instanceof SubDiagram )
            {
                addSubdiagram(astModel, (SubDiagram)de);
                return;
            }

            Base kernel = de.getKernel();
            if( kernel instanceof Specie )
            {
                if( AntimonyUtility.isClone((Node)de) )
                {
                    addCloneProperty((Node)de, astStart);
                    linkCloneToAstNodes((Node)de);
                    return;
                }

                if( SbgnUtil.isComplex((Node)de.getParent()) )
                {
                    changeSbgnViewTitle((Node)de, astStart);
                    if( !SbgnUtil.isComplex((Node)de) )
                        addProperties(de, getPropertiesAsMap(de), astModel, true);
                    return;
                }

                String entityType = ( (Specie)kernel ).getType();
                if( SbgnUtil.isComplex((Node)de) )
                {
                    if( de.getParent() instanceof Node && !SbgnUtil.isComplex((Node)de.getParent()) )
                    {
                        choosePlaceForDeclarationAstNode(de, astModel, AstSymbolType.SPECIES);
                        addProperties(de, getPropertiesAsMap(de), astModel, true);
                        addComplexElementsProperty((Node)de, astModel, true);
                    }
                    else
                        changeSbgnViewTitle((Node)de, astStart);
                }
                else
                {
                    if( AntimonyConstants.SBGN_GENE.equals(entityType) )
                        choosePlaceForDeclarationAstNode(de, astModel, AstSymbolType.GENE);
                    else
                        choosePlaceForDeclarationAstNode(de, astModel, AstSymbolType.SPECIES);
                    addProperties(de, getPropertiesAsMap(de), astModel, true);
                    addComplexElementsProperty((Node)de, astModel, true);
                }
            }
            else if( kernel instanceof biouml.standard.type.Compartment )
            {
                choosePlaceForDeclarationAstNode(de, astModel, AstSymbolType.COMPARTMENT);
            }
            else if( kernel instanceof Reaction )
            {
                addReaction((Node)de, astModel, false, true);
            }
            else if( kernel instanceof Stub.ConnectionPort )
                addPort(de, astModel);
            else if( kernel instanceof Stub.Note )
                addNoteProperty((Node)de, astModel);
            else if( kernel instanceof Stub )
            {
                if( role instanceof Equation )
                {
                    addEquation((Equation)role, astModel, true);
                }
                else if( role instanceof Constraint )
                {
                    addConstraint((Constraint)role, astModel, true);
                }
                else if( role instanceof Event )
                {
                    addEvent((Event)role, astModel, true);
                }
                else if( role instanceof Function )
                {
                    addFunction(astStart.getStart(), (Function)role, true);
                }
                else if( role instanceof Bus )
                {
                    addBusProperties((Node)de, astModel, true);
                }
                else if( role instanceof SimpleTableElement )
                {
                    addTableProperty((Node)de, astModel);
                }
                else
                    changeSbgnViewTitle((Node)de, astStart);
            }
        }
        else if( de instanceof Edge )
        {
            Edge edge = (Edge)de;
            if( role instanceof Connection )
            {
                if( Util.isBus(edge.getInput()) || Util.isBus(edge.getOutput()) )
                    addPortToBus(edge);
                else
                    addConnection(edge, astModel, true);
            }
            else if( SbgnUtil.isLogical(edge.getInput()) )
            {
                addModifier(edge.getOutput(), astModel, edge, true);
                addProperties(edge, getPropertiesAsMap(edge), astModel, true);
            }
            else if( SbgnUtil.isEquivalenceNode(edge.getInput()) )
            {
                addSubtypes(edge.getInput(), edge.getOutput(), astModel, true);
            }
            else if( SbgnUtil.isPhenotype(edge.getOutput()) )
            {
                addPhenotypeReaction(edge.getOutput(), astModel, true);
                addModifier(edge.getOutput(), astModel, edge, true);
                addProperties(edge.getOutput(), getPropertiesAsMap(edge.getOutput()), astModel, true);
            }
            else if( SbgnUtil.isNoteEdge(edge) )
            {
                addEdgeToNote(edge);
            }
            else
                addSpecieReferenceEdge(edge);
        }
    }


    private void addEdgeToNote(Edge edge)
    {
        if( SbgnUtil.isNote(edge.getInput()) && SbgnUtil.isNote(edge.getOutput()) )
            return;

        Node note;
        Node node;

        if( SbgnUtil.isNote(edge.getInput()) )
        {
            note = edge.getInput();
            node = edge.getOutput();
        }
        else
        {
            note = edge.getOutput();
            node = edge.getInput();
        }

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(note);

        if( astNodes != null )
        {
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstProperty )
                {
                    AstSingleProperty sprop = ( (AstProperty)astNode ).getSinglePropety("nodes");
                    if( sprop == null )
                    {
                        List<String> nodesList = new ArrayList<String>();
                        nodesList.add(node.getName());
                        ( (AstProperty)astNode ).addSingleProperty("nodes", nodesList);

                        sprop = ( (AstProperty)astNode ).getSinglePropety("nodes");
                        AstList list = (AstList)sprop.getValueNode();

                        for( biouml.plugins.antimony.astparser_v2.Node symbol : list.getChildren() )
                        {
                            if( symbol instanceof AstSymbol && ( (AstSymbol)symbol ).getChainNames()[0].equals(node.getName()) )
                            {
                                link(node, symbol);
                                return;
                            }
                        }
                    }
                    else
                    {
                        AstList list = (AstList)sprop.getValueNode();

                        for( String name : list.getValue() )
                        {
                            if( name.equals(node.getName()) )
                                return;
                        }

                        int index = list.jjtGetNumChildren() - 1;
                        //first element and last element are AstRegularFormulaElement "[" and "]"
                        SimpleNode lastElement = (SimpleNode)list.jjtGetChild(index);

                        list.removeChild(index);

                        if( list.getValue().size() != 0 )
                            list.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
                        AstSymbol symbol = addSymbol(list, node);

                        list.addAsLast(lastElement);
                        link(edge, symbol);
                        return;
                    }
                }
            }
        }


    }

    private void addPortToBus(Edge edge)
    {
        Node portNode;
        Node busNode;
        if( Util.isPort(edge.getInput()) && Util.isBus(edge.getOutput()) )
        {
            busNode = edge.getOutput();
            portNode = edge.getInput();
        }
        else if( Util.isPort(edge.getOutput()) && Util.isBus(edge.getInput()) )
        {
            busNode = edge.getInput();
            portNode = edge.getOutput();
        }
        else
            return;

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(busNode);
        AstMap map = null;
        if( astNodes != null )
        {
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstProperty )
                    for( biouml.plugins.antimony.astparser_v2.Node child : ( (AstProperty)astNode ).getChildren() )
                    {
                        if( child instanceof AstSingleProperty )
                        {
                            AstSingleProperty sp = (AstSingleProperty)child;
                            if( sp.getPropertyName().equals("bus") && sp.getValueNode() instanceof AstMap )
                            {
                                map = (AstMap)sp.getValueNode();
                                break;
                            }
                        }
                    }
            }

            if( map == null )
                return;

            for( biouml.plugins.antimony.astparser_v2.Node astNode : map.getChildren() )
            {
                if( astNode instanceof AstSingleProperty )
                {
                    AstSingleProperty sprop = (AstSingleProperty)astNode;
                    if( sprop.getPropertyName().equals("ports") && sprop.getValueNode() instanceof AstList )
                    {
                        AstList list = (AstList)sprop.getValueNode();


                        int index = list.jjtGetNumChildren() - 1;
                        //first element and last element are AstRegularFormulaElement "[" and "]"
                        SimpleNode lastElement = (SimpleNode)list.jjtGetChild(index);

                        boolean isBrace = lastElement instanceof AstRegularFormulaElement
                                && ( (AstRegularFormulaElement)lastElement ).toString().equals("]");

                        if( isBrace )
                            list.removeChild(index);

                        if( list.getValue().size() != 0 )
                            list.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));

                        AstSymbol subSymbol = addSubSymbol(list, portNode);
                        link(edge, subSymbol);

                        if( isBrace )
                            list.addAsLast(lastElement);

                        return;
                    }
                }
            }
        }

    }

    private AstSymbol addSubSymbol(biouml.plugins.antimony.astparser_v2.Node parent, Node node)
    {
        AstSymbol symbol = createSubSymbol(node.getCompleteNameInDiagram());
        ( (SimpleNode)parent ).addAsLast(symbol);
        return symbol;
    }

    private void linkCloneToAstNodes(Node de)
    {
        Node primaryNode = (Node) ( (VariableRole)de.getRole() ).getDiagramElement();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(primaryNode);
        if( astNodes == null )
            return;

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( astNode instanceof AstProperty )
            {
                for( biouml.plugins.antimony.astparser_v2.Node child : ( (AstProperty)astNode ).getChildren() )
                    if( child instanceof AstSingleProperty
                            && ! ( (AstSingleProperty)child ).getPropertyName().equals(AntimonyConstants.SBGN_CLONE) )
                    {
                        link(de, astNode);
                        break;
                    }
            }
            else if( astNode instanceof AstSymbol
                    && ( astNode.jjtGetParent() instanceof AstReactant || astNode.jjtGetParent() instanceof AstProduct ) )
                continue;
            else
                link(de, astNode);
        }
    }

    public void addElement(AstStart astStart, Variable var) throws Exception
    {
        DynamicPropertySet dps = var.getAttributes();
        dps.remove(AntimonyConstants.ANTIMONY_LINK);

        String name = var.getName();
        if( "time".equals(name) || name.startsWith("$$") || var instanceof VariableRole )
            return;
        choosePlaceForDeclarationAstNode(var, getModel(var, astStart), AstSymbolType.PARAMETERS);
    }

    // return model
    private AstModel getModel(DiagramElement de, AstStart astStart)
    {
        DiagramElement parent = Diagram.getDiagram(de);

        DynamicProperty dp = parent.getAttributes().getProperty(ModelDefinition.REF_MODEL_DEFINITION);
        if( dp != null && dp.getValue() instanceof ModelDefinition )
        {
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode((ModelDefinition)dp.getValue());
            if( astNodes == null )
                return null;

            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                if( AntimonyUtility.isAstModel(astNode) )
                    return (AstModel)astNode;
            return null;
        }
        else
            return astStart.getMainModel();
    }

    private AstModel getModel(Variable var, AstStart astStart)
    {
        if( var instanceof VariableRole )
            return getModel( ( (VariableRole)var ).getDiagramElement(), astStart);
        else if( var.getParent() instanceof EModel )
            return getModel( ( (EModel)var.getParent() ).getDiagramElement(), astStart);
        return null;
    }

    protected void addSpecieReferenceEdge(Edge edge) throws Exception
    {
        String typeInReaction = "";
        Node reactionNode = null;
        Node node = null;

        if( edge.getInput().getKernel() instanceof Reaction )
        {
            reactionNode = edge.getInput();
            node = edge.getOutput();
            typeInReaction = "product";
        }
        else if( edge.getOutput().getKernel() instanceof Reaction )
        {
            reactionNode = edge.getOutput();
            node = edge.getInput();
            typeInReaction = "reactant";
        }
        if( reactionNode == null || node == null )
            return;

        if( AntimonyUtility.isClone(node) && AntimonyAnnotationImporter.isPropertyImported(SBGNPropertyConstants.SBGN_CLONE_MARKER) )
            addReactionToClone(node, reactionNode);

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(reactionNode);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isReactionAstSymbol(astNode) )
            {
                if( ! ( edge.getKernel() instanceof SpecieReference ) )
                    return;
                SpecieReference sr = (SpecieReference)edge.getKernel();

                // is modifier
                if( !sr.isReactantOrProduct() )
                {
                    addModifier(reactionNode, astNode.jjtGetParent(), edge, true);

                    HashMap<String, Object> value = new HashMap<String, Object>();
                    value.put("edgeType", sr.getModifierAction());
                    addProperty(edge, "sbgn", value, (AstModel)astNode.jjtGetParent());
                    return;
                }
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstReactionTitle )
                    {
                        AstReactionTitle reactionTitle = (AstReactionTitle)astNode.jjtGetChild(i);

                        //check duplicating specieReference
                        if( typeInReaction.equals("reactant") )
                            for( String name : reactionTitle.getReactantNames() )
                            {
                                if( node.getName().equals(name) )
                                    return;
                            }
                        else if( typeInReaction.equals("product") )
                            for( String name : reactionTitle.getProductNames() )
                            {
                                if( name.equals(node.getName()) )
                                    return;
                            }

                        // add specieReference in reaction title
                        for( int j = 0; j < reactionTitle.jjtGetNumChildren(); j++ )
                        {
                            if( reactionTitle.jjtGetChild(j) instanceof AstReactionType )
                            {
                                if( typeInReaction.equals("reactant") )
                                {
                                    AstReactant astReactant = new AstReactant(AntimonyNotationParser.JJTREACTANT);

                                    AstStoichiometry astStoichiometry = new AstStoichiometry(AntimonyNotationParser.JJTSTOICHIOMETRY);
                                    AstSymbol astSymbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                                    astStoichiometry.setStoichiometry(sr.getStoichiometry());
                                    createSpace(astStoichiometry);
                                    astSymbol.setName(validAndNotReservedName(node.getRole().getDiagramElement().getName()));

                                    astReactant.addAsLast(astStoichiometry);
                                    astReactant.addAsLast(astSymbol);

                                    if( reactionTitle.jjtGetChild(j - 1) instanceof AstReactant )
                                    {
                                        AstPlus plus = new AstPlus(AntimonyNotationParser.JJTPLUS);
                                        createSpace(plus);
                                        reactionTitle.addWithDisplacement(plus, j);
                                        j++;
                                    }

                                    reactionTitle.addWithDisplacement(astReactant, j);
                                    return;

                                }
                                else if( typeInReaction.equals("product") )
                                {
                                    AstProduct astProduct = new AstProduct(AntimonyNotationParser.JJTPRODUCT);

                                    AstStoichiometry astStoichiometry = new AstStoichiometry(AntimonyNotationParser.JJTSTOICHIOMETRY);
                                    AstSymbol astSymbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                                    astStoichiometry.setStoichiometry(sr.getStoichiometry());
                                    createSpace(astStoichiometry);
                                    astSymbol.setName(validAndNotReservedName(node.getRole().getDiagramElement().getName()));

                                    astProduct.addAsLast(astStoichiometry);
                                    astProduct.addAsLast(astSymbol);

                                    if( reactionTitle.jjtGetNumChildren() > j + 1
                                            && reactionTitle.jjtGetChild(j + 1) instanceof AstProduct )
                                    {
                                        AstPlus plus = new AstPlus(AntimonyNotationParser.JJTPLUS);
                                        createSpace(plus);
                                        reactionTitle.addWithDisplacement(plus, j + 1);
                                    }

                                    reactionTitle.addWithDisplacement(astProduct, j + 1);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    void addReactionToClone(Node node, Node reactionNode)
    {
        if( !AntimonyUtility.isClone(node) )
            return;

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);
        AstMap map = null;
        if( astNodes != null )
        {
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstProperty )
                    for( biouml.plugins.antimony.astparser_v2.Node child : ( (AstProperty)astNode ).getChildren() )
                    {
                        if( child instanceof AstSingleProperty )
                        {
                            AstSingleProperty sp = (AstSingleProperty)child;
                            if( sp.getPropertyName().equals(AntimonyConstants.SBGN_CLONE) && sp.getValueNode() instanceof AstMap )
                            {
                                map = (AstMap)sp.getValueNode();
                                break;
                            }
                        }
                    }
            }

            if( map == null )
                return;

            for( biouml.plugins.antimony.astparser_v2.Node astNode : map.getChildren() )
            {
                if( astNode instanceof AstSingleProperty )
                {
                    AstSingleProperty sprop = (AstSingleProperty)astNode;
                    if( sprop.getPropertyName().equals("reactions") && sprop.getValueNode() instanceof AstList )
                    {
                        AstList list = (AstList)sprop.getValueNode();

                        for( String name : list.getValue() )
                        {
                            if( name.equals(reactionNode.getName()) )
                                return;
                        }

                        int index = list.jjtGetNumChildren() - 1;
                        //first element and last element are AstRegularFormulaElement "[" and "]"
                        SimpleNode lastElement = (SimpleNode)list.jjtGetChild(index);

                        boolean isBrace = lastElement instanceof AstRegularFormulaElement
                                && ( (AstRegularFormulaElement)lastElement ).toString().equals("]");

                        if( isBrace )
                            list.removeChild(index);

                        if( list.getValue().size() != 0 )
                            list.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
                        addSymbol(list, reactionNode);

                        if( isBrace )
                            list.addAsLast(lastElement);

                        return;
                    }
                }
            }
        }
    }

    protected void choosePlaceForDeclarationAstNode(DataElement de, AstModel astModel, String type) throws Exception
    {
        placeTypeDeclaration(de, astModel, type);
        if( de instanceof Node )
        {
            Node node = (Node)de;
            Role role = node.getRole();
            if( role instanceof VariableRole )
            {
                if( ( (VariableRole)role ).getInitialValue() != 0 )
                    changeInitialValue((Variable)role, astModel);
                if( !validAndNotReservedName(node.getName()).equals(node.getTitle()) )
                    addDisplayName(node, astModel, true);
            }
        }
    }

    private void placeTypeDeclaration(DataElement de, AstModel astModel, String type)
    {
        boolean isSubstanceOnly = de instanceof Node && ( (Node)de ).getKernel() instanceof Specie
                && ( (Node)de ).getRole() instanceof VariableRole
                && VariableRole.AMOUNT_TYPE == ( (Node)de ).getRole(VariableRole.class).getQuantityType();
        for( int i = 0; i <= astModel.jjtGetNumChildren(); i++ )
        {
            if( i == astModel.jjtGetNumChildren() )
            {
                int indexToPlace = 1; // all declarations should be in the beginning
                if( ! ( astModel.jjtGetChild(indexToPlace) instanceof AstEOL ) )
                    astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), indexToPlace);
                indexToPlace++;

                AstSymbolType symbolType = new AstSymbolType(AntimonyNotationParser.JJTSYMBOLTYPE);
                symbolType.setType(type);
                addSymbol(symbolType, de);
                symbolType.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

                if( isSubstanceOnly )
                {
                    AstSubstanceOnly substanceOnly = new AstSubstanceOnly(AntimonyNotationParser.JJTSUBSTANCEONLY);
                    createSpace(symbolType);
                    substanceOnly.addAsLast(symbolType);
                    createIndent(substanceOnly);

                    astModel.addWithDisplacement(substanceOnly, indexToPlace++);
                }
                else
                {
                    createIndent(symbolType);
                    astModel.addWithDisplacement(symbolType, indexToPlace++);
                }

                astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), indexToPlace);
                break;
            }
            else if( !isSubstanceOnly && AntimonyUtility.isAstSymbolType(astModel.jjtGetChild(i))
                    && ( (AstSymbolType)astModel.jjtGetChild(i) ).getType().equals(type) )

            {
                AstSymbolType symbolType = (AstSymbolType)astModel.jjtGetChild(i);

                int index = symbolType.jjtGetNumChildren() - 1;
                //lastElement is EOL, EOF or Semicolon
                SimpleNode lastElement = (SimpleNode)symbolType.jjtGetChild(index);
                if( lastElement instanceof AstEOL || lastElement instanceof AstSemicolon )
                    symbolType.removeChild(index);

                symbolType.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
                addSymbol(symbolType, de);
                if( lastElement instanceof AstEOL || lastElement instanceof AstSemicolon )
                    symbolType.addAsLast(lastElement);
                break;
            }
            else if( astModel.jjtGetChild(i) instanceof AstSubstanceOnly && isSubstanceOnly )
            {
                AstSubstanceOnly substanceOnly = (AstSubstanceOnly)astModel.jjtGetChild(i);

                if( AntimonyUtility.isAstSymbolType(substanceOnly.jjtGetChild(0))
                        && ( (AstSymbolType)substanceOnly.jjtGetChild(0) ).getType().equals(type) )
                {
                    AstSymbolType symbolType = (AstSymbolType)substanceOnly.jjtGetChild(i);
                    int index = symbolType.jjtGetNumChildren() - 1;
                    //lastElement is EOL, EOF or Semicolon
                    SimpleNode lastElement = (SimpleNode)symbolType.jjtGetChild(index);
                    if( lastElement instanceof AstEOL || lastElement instanceof AstSemicolon )
                        symbolType.removeChild(index);

                    symbolType.addAsLast(new AstComma(AntimonyNotationParser.JJTCOMMA));
                    addSymbol(symbolType, de);
                    if( lastElement instanceof AstEOL || lastElement instanceof AstSemicolon )
                        symbolType.addAsLast(lastElement);
                    break;
                }

            }
        }
    }

    protected void removeElement(DiagramElement de, AstStart astStart) throws Exception
    {
        if( de instanceof Node && de.getCompartment().getKernel() instanceof Specie )
            changeSbgnViewTitle((Node)de, astStart);

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(de);
        if( astNodes == null )
            return;

        Base kernel = de.getKernel();
        if( kernel instanceof Gene || kernel instanceof Specie || kernel instanceof biouml.standard.type.Compartment
                || kernel instanceof Stub || kernel instanceof Stub.ContactConnectionPort || de.getRole() instanceof Connection )
        {

            SubDiagram subDiagram = SubDiagram.getParentSubDiagram(Diagram.getDiagram(de)); // if de is contained in SubDiagram
            if( subDiagram != null )
            {
                addDeleteFromSubDiagram(de, subDiagram);
                return;
            }
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                removeAstNode(astNode);

            if( de instanceof Node && de.getRole() instanceof VariableRole
                    && de.getRole(VariableRole.class).getAssociatedElements().length <= 2 )
                removeCloneAstNodes((Node)de);
        }
        else if( kernel instanceof Reaction )
        {
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                removeAstNode(astNode);
        }
        else if( kernel instanceof SpecieReference )
        {
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                removeAstNode(astNode);
        }

        changedNames.remove(de.getName());
    }

    private void removeCloneAstNodes(Node de)
    {
        for( DiagramElement associatedDe : de.getRole(VariableRole.class).getAssociatedElements() )
        {
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(associatedDe);
            if( astNodes == null )
                return;

            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstProperty )
                {
                    for( biouml.plugins.antimony.astparser_v2.Node node : ( (AstProperty)astNode ).getChildren() )
                    {
                        if( node instanceof AstSingleProperty
                                && ( (AstSingleProperty)node ).getPropertyName().equals(AntimonyConstants.SBGN_CLONE) )

                        {
                            removeAstNode(astNode);
                            break;
                        }

                    }
                }
            }


        }

    }

    protected void removeElement(Variable var)
    {
        DynamicProperty dp = var.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(dp);
        if( astNodes == null )
            return;

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            removeAstNode(astNode);
        }
    }

    private void addDeleteFromSubDiagram(DiagramElement de, SubDiagram subDiagram)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(subDiagram);
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isSubdiagramAstSymbol(astNode) )
            {
                String name = validAndNotReservedName(subDiagram.getName());
                name += "." + validAndNotReservedName(de.getName());
                SimpleNode parent = (SimpleNode)astNode.jjtGetParent();
                if( containDelete(parent, name) )
                    continue;
                AstDelete delete = new AstDelete(AntimonyNotationParser.JJTDELETE);
                AstSubSymbol symbol = new AstSubSymbol(AntimonyNotationParser.JJTSUBSYMBOL);
                createSpace(symbol);
                symbol.setName(name);
                delete.addAsLast(symbol);
                delete.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
                parent.addWithDisplacement(delete, parent.findIndex(astNode) + 1);
                parent.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), parent.findIndex(astNode) + 1);
                link(subDiagram, delete);
            }
        }
    }

    private boolean containDelete(SimpleNode parent, String name)
    {
        for( biouml.plugins.antimony.astparser_v2.Node child : parent.getChildren() )
            if( child instanceof AstDelete && ( (AstDelete)child ).getAstSymbol().getName().equals(name) )
                return true;
        return false;
    }

    protected void removeAstNode(biouml.plugins.antimony.astparser_v2.Node astNode)
    {
        biouml.plugins.antimony.astparser_v2.Node parent = astNode.jjtGetParent();
        int index = astNode.remove();
        // if parent can't exist without astNode
        if( parent instanceof AstIs )
            parent.jjtGetParent().remove();

        //remove comma
        if( parent.jjtGetChild(index - 1) instanceof AstComma )
            parent.removeChild(index - 1);
        else if( parent.jjtGetChild(index) instanceof AstComma )
            parent.removeChild(index);

        if( AntimonyUtility.isAstSymbolType(parent) || parent instanceof AstVarOrConst || AntimonyUtility.isReactionOrProduct(parent)
                || AntimonyUtility.isDatabaseReferenceAstSymbol(parent) )
        {
            if( AntimonyUtility.isEmpty(parent) )
            {
                if( parent.jjtGetParent() instanceof AstVarOrConst )
                {
                    index = parent.jjtGetParent().remove();
                    removeEOL(parent.jjtGetParent().jjtGetParent(), index);
                    return;
                }
                else if( parent.jjtGetParent() instanceof AstSubstanceOnly )
                {
                    index = parent.jjtGetParent().remove();
                    removeEOL(parent.jjtGetParent().jjtGetParent(), index);

                    if( parent.jjtGetParent().jjtGetParent() instanceof AstVarOrConst )
                    {
                        index = parent.jjtGetParent().jjtGetParent().remove();
                        removeEOL(parent.jjtGetParent().jjtGetParent().jjtGetParent(), index);
                    }
                    return;
                }
                index = parent.remove();
                removeEOL(parent.jjtGetParent(), index);
            }
        }
        else if( parent instanceof AstProperty && ( (AstProperty)parent ).isEmpty() )
        {
            index = parent.remove();
            removeEOL(parent.jjtGetParent(), index);
        }
        else if( parent instanceof AstMap && ( (AstMap)parent ).isEmpty() )
        {
            index = parent.remove();
            //biouml.plugins.antimony.astparser_v2.Node parent = astNode.jjtGetParent();
        }
        else if( AntimonyUtility.isAstModel(parent) || parent instanceof AstStart )
        {
            removeEOL(parent, index);
        }

        updateBlockLocation(astNode);
    }

    private void updateBlockLocation(biouml.plugins.antimony.astparser_v2.Node astNode)
    {
        biouml.plugins.antimony.astparser_v2.Node parent = astNode.jjtGetParent();
        if( parent instanceof AstModel || parent instanceof AstStart )
        {
            String type = AntimonyUtility.getBlockType(astNode);
            if( type == null )
                return;

            biouml.plugins.antimony.astparser_v2.Node newLocationNode = null;
            if( astNode.equals(blockLocations.get(type)) )
            {
                int childrenNum = parent.jjtGetNumChildren();
                for( int i = 0; i < childrenNum; i++ )
                {
                    biouml.plugins.antimony.astparser_v2.Node node = parent.jjtGetChild(i);

                    // if node is the last in the block (taking into account EOL)
                    if( type.equals(AntimonyUtility.getBlockType(node))
                            && ! ( i + 2 < childrenNum && type.equals(AntimonyUtility.getBlockType(parent.jjtGetChild(i + 2))) ) )
                    {
                        blockLocations.put(type, node);
                        newLocationNode = node;
                        break;
                    }
                }
            }
            else
                return;

            if( newLocationNode instanceof AstComment )//if only left block node is comment - remove it
            {
                removeAstNode(newLocationNode);
                newLocationNode = null;
            }
            if( newLocationNode == null )
            {
                blockLocations.remove(type);
            }
        }

    }

    private void removeEOL(biouml.plugins.antimony.astparser_v2.Node parent, int index)
    {
        if( parent.jjtGetChild(index) instanceof AstEOL )
            parent.removeChild(index);
    }

    protected void changeInitialValue(Variable var, AstStart astStart) throws Exception
    {
        AstModel astModel = getModel(var, astStart);
        changeInitialValue(var, astModel);
    }

    protected void changeInitialValue(Variable var, AstModel astModel) throws Exception
    {
        boolean initIsExist = false;
        String compartmentName = null;
        DynamicProperty dp;
        if( var instanceof VariableRole )
        {
            DiagramElement de = ( (VariableRole)var ).getDiagramElement();
            dp = de.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        }
        else
        {
            dp = var.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        }
        if( dp != null )
        {
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(dp);
            if( astNodes == null )
                return;
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( AntimonyUtility.isInitAstSymbol(astNode) )
                {
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    {
                        if( astNode.jjtGetChild(i) instanceof AstEquation )
                        {
                            ( (AstRegularFormulaElement)astNode.jjtGetChild(i).jjtGetChild(0) )
                                    .setElement(String.valueOf(var.getInitialValue()));
                        }
                    }
                    initIsExist = true;
                }
                if( AntimonyUtility.isHasUnitAstSymbol(astNode) )
                    removeAstNode(astNode);
            }
        }

        if( !initIsExist )
        {
            AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
            symbol.setTypeDeclaration(AstSymbol.INIT);
            symbol.setName(validAndNotReservedName(var.getName()));

            AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);

            if( var instanceof VariableRole && ( (VariableRole)var ).getInitialQuantityType() == VariableRole.AMOUNT_TYPE )
            {
                DiagramElement de = ( (VariableRole)var ).getDiagramElement();
                if( de.getParent() instanceof Compartment
                        && ( (Compartment)de.getParent() ).getKernel() instanceof biouml.standard.type.Compartment )
                    compartmentName = ( (Compartment)de.getParent() ).getName();
            }
            AstEquation equation = createInitializingEquation(var.getInitialValue(), var.getUnits(), compartmentName);
            createSpace(equal);
            symbol.addAsLast(equal);
            symbol.addAsLast(equation);

            createIndent(symbol);
            symbol.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));

            int index;
            if( blockLocations.containsKey(AntimonyConstants.BLOCK_INITIALIZATIONS) )
                index = astModel.findIndex(blockLocations.get(AntimonyConstants.BLOCK_INITIALIZATIONS)) + 2;
            else
                index = astModel.jjtGetNumChildren();

            astModel.addWithDisplacement(symbol, index);
            astModel.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index + 1);

            blockLocations.put(AntimonyConstants.BLOCK_INITIALIZATIONS, astModel.jjtGetChild(index));

            if( var instanceof VariableRole )
            {
                Node varNode = (Node) ( (VariableRole)var ).getDiagramElement();
                symbol.setName(validAndNotReservedName(varNode.getName()));
                link(varNode, symbol);
            }
            else
            {
                link(var.getAttributes(), symbol);
            }
        }
    }

    protected void changeKineticLaw(Node reactionNode)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(reactionNode);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isReactionAstSymbol(astNode) )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstEquation )
                    {
                        AstEquation newAstEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
                        setFormulaToAstEquation( ( (Reaction)reactionNode.getKernel() ).getFormula(), newAstEquation);
                        astNode.jjtAddChild(newAstEquation, i);
                        if( newAstEquation.jjtGetChild(0) != null )
                            createSpace((SimpleNode)newAstEquation.jjtGetChild(0));
                    }
                }
            }
        }
    }

    protected void changeFormula(DiagramElement de)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(de);
        if( astNodes == null )
            return;

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isAstSymbol(astNode) )
            {
                if( AntimonyUtility.isConstraintAstSymbol(astNode) )
                {
                    for( biouml.plugins.antimony.astparser_v2.Node elem : ( (AstSymbol)astNode ).getChildren() )
                        if( elem instanceof AstAssert )
                        {
                            astNode = elem;
                            break;
                        }
                }

                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstEquation )
                    {
                        AstEquation newAstEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
                        if( AntimonyUtility.isReactionAstSymbol(astNode) )
                            setFormulaToAstEquation( ( (Reaction)de.getKernel() ).getFormula(), newAstEquation);
                        else if( AntimonyUtility.isEquationAstSymbol(astNode) )
                            setFormulaToAstEquation(de.getRole(Equation.class).getFormula(), newAstEquation);
                        else if( astNode instanceof AstAssert ) // for constraint
                            setFormulaToAstEquation(de.getRole(Constraint.class).getFormula(), newAstEquation);
                        else
                            break;
                        astNode.jjtAddChild(newAstEquation, i);
                        if( newAstEquation.jjtGetChild(0) != null )
                            createSpace((SimpleNode)newAstEquation.jjtGetChild(0));
                        break;
                    }
                }
            }
            else if( astNode instanceof AstFunction )
                setFormulaToAstFunction(de.getRole(Function.class).getFormula(), (AstFunction)astNode);
        }
    }

    protected void changeStoichiometry(SpecieReference sr)
    {
        //TODO: find better way to get edge by specie reference
        Node reactionNode = (Node)sr.getParent().getParent();
        String typeInReaction = sr.isProduct() ? "product" : "reactant";
        Node node = reactionNode.edges().filter(edge -> edge.getKernel() == sr).map(edge -> edge.getOtherEnd(reactionNode)).findAny()
                .orElse(null);
        DynamicProperty dp = reactionNode.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        if( dp == null || node == null )
            return;
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(dp);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isReactionAstSymbol(astNode) || AntimonyUtility.isModifierAstSymbol(astNode) )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstReactionTitle )
                    {
                        AstReactionTitle astReactionTitle = (AstReactionTitle)astNode.jjtGetChild(i);
                        if( typeInReaction.equals("reactant") )
                            for( AstReactant astReactant : astReactionTitle.getReactants() )
                            {
                                if( astReactant.getName().equals(node.getName()) )
                                    astReactant.setStoichiometry(sr.getStoichiometry());
                            }
                        else if( typeInReaction.equals("product") )
                            for( AstProduct astProduct : astReactionTitle.getProducts() )
                            {
                                if( astProduct.getName().equals(node.getName()) )
                                    astProduct.setStoichiometry(sr.getStoichiometry());
                            }
                    }
                }
            }
        }
    }

    protected void changeConstantProperty(Variable var, AstStart astStart) throws Exception
    {
        AstModel astModel = getModel(var, astStart);
        changeConstantProperty(var, astModel);
    }

    protected void changeConstantProperty(Variable var, AstModel astModel) throws Exception
    {
        boolean declarationIsExist = false;

        DynamicProperty dp;
        boolean removeConstPrefix = false;
        if( var instanceof VariableRole )
        {
            dp = ( (VariableRole)var ).getDiagramElement().getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
            if( !var.isConstant() )
                removeConstPrefix = true;
        }
        else
            dp = var.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);

        if( dp != null )
        {
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(dp);
            if( astNodes == null )
                return;
            Set<biouml.plugins.antimony.astparser_v2.Node> setWhichRemoved = new HashSet<>();

            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( removeConstPrefix && astNode instanceof AstSymbol )
                    ( (AstSymbol)astNode ).setConstantPrefix(false);
                biouml.plugins.antimony.astparser_v2.Node parent = astNode.jjtGetParent();
                if( parent instanceof AstVarOrConst )
                {
                    if( ( (AstVarOrConst)parent ).isConst() == var.isConstant() )
                        declarationIsExist = true;
                    else
                    {
                        removeAstNode(astNode);
                        setWhichRemoved.add(astNode);
                    }
                }
                else if( parent.jjtGetParent() instanceof AstVarOrConst )
                {
                    if( ( (AstVarOrConst)parent.jjtGetParent() ).isConst() == var.isConstant() )
                        declarationIsExist = true;
                    else
                    {
                        removeAstNode(astNode);
                        setWhichRemoved.add(astNode);
                    }
                }
            }
            astNodes.removeAll(setWhichRemoved);
        }
        if( !declarationIsExist )
        {
            choosePlaceForConstantAstNode(var, astModel);
        }
    }

    protected void changeTypeSpecie(DiagramElement de, String type, AstStart astStart) throws Exception
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(de);
        boolean placeForDeclarationFound = false;
        boolean notStub = ! ( de.getRole() != null );
        if( astNodes == null )
        {
            if( notStub )
                log.log(Level.SEVERE, "Can't find compartment " + de.getName() + " in antimony text");
        }
        else
        {
            Set<biouml.plugins.antimony.astparser_v2.Node> setWhichRemoved = new HashSet<>();
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                if( AntimonyUtility.isAstSymbolType(astNode.jjtGetParent()) )
                {
                    AstSymbolType parent = (AstSymbolType)astNode.jjtGetParent();
                    if( type.equals(parent.getType()) )
                    {
                        placeForDeclarationFound = true;
                        continue;
                    }
                    else if( parent.getSymbols().size() == 1 && parent.getSymbols().contains(astNode) )
                    {
                        parent.setType(type);
                        placeForDeclarationFound = true;
                    }
                    else
                    {
                        removeAstNode(astNode);
                        setWhichRemoved.add(astNode);
                    }
                }
            astNodes.removeAll(setWhichRemoved);
        }
        if( !placeForDeclarationFound && notStub )
        {
            AstModel astModel = getModel(de, astStart);
            if( type.equals(AstSymbolType.GENE) )
                choosePlaceForDeclarationAstNode(de, astModel, AstSymbolType.GENE);
            else
                choosePlaceForDeclarationAstNode(de, astModel, AstSymbolType.SPECIES);
        }
    }

    protected void changeReversibleType(DiagramElement de)
    {
        Reaction reaction = (Reaction)de.getKernel();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(de);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isReactionAstSymbol(astNode) )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    if( astNode.jjtGetChild(i) instanceof AstReactionTitle )
                    {
                        AstReactionTitle title = (AstReactionTitle)astNode.jjtGetChild(i);
                        AstReactionType type = title.getReactionType();
                        if( reaction.isReversible() )
                            type.setType(type.getType().replace("=", "-"));
                        else
                            type.setType(type.getType().replace("-", "="));
                    }
            }
        }
    }

    protected void changeDisplayName(Node node, AstStart astStart) throws Exception
    {
        boolean declarationExist = false;
        boolean shouldDelete = node.getTitle().equals(node.getName());
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);

        if( astNodes != null )
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( AntimonyUtility.isDisplayNameAstSymbol(astNode) )
                {
                    if( shouldDelete )
                    {
                        removeAstNode(astNode);
                        removeLink(node, astNode);
                        return;
                    }

                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                        if( astNode.jjtGetChild(i) instanceof AstIs )
                        {
                            ( (AstIs)astNode.jjtGetChild(i) ).setDisplayName(node.getTitle());
                            declarationExist = true;
                            break;
                        }
                }
            }

        if( !declarationExist && !shouldDelete )
            addDisplayName(node, getModel(node, astStart), true);
    }

    protected void changeEquationVariable(Equation eq)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(eq.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isAstSymbol(astNode) )
                ( (AstSymbol)astNode ).setName(validAndNotReservedName(eq.getVariable()));
        }

    }

    protected void changeEventAssignment(Assignment assignment, PropertyChangeEvent e)
    {

        Event event = (Event)assignment.getParent();
        int indexAssignment = -1;
        for( int i = 0; i < event.getEventAssignment().length; i++ )
        {
            if( event.getEventAssignment(i) == assignment )
            {
                indexAssignment = i;
                break;
            }
        }
        if( indexAssignment == -1 )
            return;

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(event.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEventAstSymbol(astNode) )
            {
                int indexAstNode = 0;
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( AntimonyUtility.isInitAstSymbol(astNode.jjtGetChild(i)) )
                    {
                        if( indexAssignment == indexAstNode )
                        {
                            if( e.getPropertyName().equals("variable") )
                                ( (AstSymbol)astNode.jjtGetChild(i) ).setName(validAndNotReservedName(assignment.getVariable()));
                            else if( e.getPropertyName().equals("math") )
                            {
                                AstEquation newAstEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
                                setFormulaToAstEquation(assignment.getMath(), newAstEquation);
                                if( newAstEquation.jjtGetChild(0) != null )
                                    createSpace((SimpleNode)newAstEquation.jjtGetChild(0));
                                astNode.jjtGetChild(i).jjtAddChild(newAstEquation, 1);
                            }
                            return;
                        }
                        indexAstNode++;
                    }
                }
            }
        }
    }

    protected void updateEventAssignment(PropertyChangeEvent e)
    {
        Assignment[] oldAssignments = (Assignment[])e.getOldValue();
        Assignment[] newAssignments = (Assignment[])e.getNewValue();
        Event event = (Event)e.getSource();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(event.getDiagramElement());

        if( astNodes == null )
            return;
        if( oldAssignments.length > newAssignments.length )
            for( int i = 0; i < oldAssignments.length; i++ )
            {
                if( i >= newAssignments.length || oldAssignments[i] != newAssignments[i] )
                {
                    for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                        if( AntimonyUtility.isAstSymbol(astNode) )
                            removeAssignmentFromEvent((AstSymbol)astNode, i);
                    return;
                }
            }
        else
            for( int i = 0; i < newAssignments.length; i++ )
            {
                if( i >= oldAssignments.length || oldAssignments[i] != newAssignments[i] )
                {
                    AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                    createSpace(symbol);
                    symbol.setName(validAndNotReservedName(newAssignments[i].getVariable()));
                    symbol.setTypeDeclaration(AstSymbol.INIT);
                    AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
                    createSpace(equal);
                    AstEquation newAstEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
                    setFormulaToAstEquation(newAssignments[i].getMath(), newAstEquation);
                    if( newAstEquation.jjtGetChild(0) != null )
                        createSpace((SimpleNode)newAstEquation.jjtGetChild(0));
                    symbol.addAsLast(equal);
                    symbol.addAsLast(newAstEquation);
                    for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                        if( AntimonyUtility.isAstSymbol(astNode) )
                            addAssignmentInEvent((AstSymbol)astNode, symbol, i);
                    return;
                }
            }
    }

    private void addAssignmentInEvent(AstSymbol event, AstSymbol symbol, int assignmentNumber)
    {
        int index = 0;
        for( int i = 0; i < event.jjtGetNumChildren(); i++ )
        {
            if( AntimonyUtility.isAstSymbol(event.jjtGetChild(i)) )
            {
                if( index == assignmentNumber )
                {
                    event.addWithDisplacement(new AstColon(AntimonyNotationParser.JJTCOLON), i);
                    event.addWithDisplacement(symbol, i);
                    return;
                }
                else
                    index++;
            }
        }
        index = event.jjtGetNumChildren() - 1;
        event.addWithDisplacement(symbol, index);
        event.addWithDisplacement(new AstColon(AntimonyNotationParser.JJTCOLON), index);
    }

    private void removeAssignmentFromEvent(AstSymbol event, int assignmentNumber)
    {
        int index = 0;
        for( int i = 0; i < event.jjtGetNumChildren(); i++ )
        {
            if( AntimonyUtility.isAstSymbol(event.jjtGetChild(i)) )
            {
                if( index == assignmentNumber )
                {
                    event.removeChild(i);
                    // remove colon
                    if( event.jjtGetChild(i) instanceof AstColon )
                        event.removeChild(i);
                    else if( event.jjtGetChild(i - 1) instanceof AstColon )
                        event.removeChild(i - 1);
                    return;
                }
                else
                    index++;
            }
        }
    }

    protected void changeEquationType(Equation eq)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(eq.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEquationAstSymbol(astNode) )
            {
                if( eq.getType().equals(Equation.TYPE_ALGEBRAIC) )
                {
                    ( (AstSymbol)astNode ).setTypeDeclaration(AstSymbol.ALGEBRAIC);
                    ( (AstSymbol)astNode ).setName(validAndNotReservedName("unknown"));
                    AstEqualZero equal = new AstEqualZero(AntimonyNotationParser.JJTEQUALZERO);
                    createIndent(equal);
                    astNode.jjtAddChild(equal, 0);
                }
                else
                {
                    if( ( (AstSymbol)astNode ).getDeclarationType().equals(AstSymbol.ALGEBRAIC) )
                        eq.setVariable(validAndNotReservedName("unknown"));
                    ( (AstSymbol)astNode ).setName(validAndNotReservedName(eq.getVariable()));
                    if( eq.getType().equals(Equation.TYPE_INITIAL_ASSIGNMENT) )
                    {
                        ( (AstSymbol)astNode ).setTypeDeclaration(AstSymbol.INIT);
                        astNode.jjtAddChild(new AstEqual(AntimonyNotationParser.JJTEQUAL), 0);
                    }
                    else if( eq.getType().equals(Equation.TYPE_SCALAR) )
                    {
                        ( (AstSymbol)astNode ).setTypeDeclaration(AstSymbol.RULE);
                        astNode.jjtAddChild(new AstColonEqual(AntimonyNotationParser.JJTCOLONEQUAL), 0);
                    }
                    else if( eq.getType().equals(Equation.TYPE_RATE) )
                    {
                        ( (AstSymbol)astNode ).setTypeDeclaration(AstSymbol.RATE);
                        astNode.jjtAddChild(new AstRateEqual(AntimonyNotationParser.JJTRATEEQUAL), 0);
                    }
                    createSpace((SimpleNode)astNode.jjtGetChild(0));
                    createIndent((AstSymbol)astNode);
                }

            }
        }
    }

    protected void changeTriggerEvent(Event ev)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(ev.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEventAstSymbol(astNode) )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstAt )
                    {
                        AstEquation newAstEquation = ( (AstAt)astNode.jjtGetChild(i) ).getTrigger();
                        setFormulaToAstEquation(ev.getTrigger(), newAstEquation);
                        if( newAstEquation.jjtGetChild(0) != null )
                            createSpace((SimpleNode)newAstEquation.jjtGetChild(0));
                        break;
                    }
                }
            }
        }
    }

    protected void changeDelayEvent(Event ev)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(ev.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEventAstSymbol(astNode) )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstAt )
                    {
                        AstAt at = (AstAt)astNode.jjtGetChild(i);
                        AstEquation newAstEquation = at.getDelay();
                        if( ev.getDelay() != null && ! ( ev.getDelay().equals("0") || ev.getDelay().equals("") ) )
                        {
                            if( newAstEquation == null )
                            {
                                newAstEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
                                setFormulaToAstEquation(ev.getDelay(), newAstEquation);
                                AstAfter after = new AstAfter(AntimonyNotationParser.JJTAFTER);
                                createSpace(after);
                                at.addWithDisplacement(after, 0);
                                at.addWithDisplacement(newAstEquation, 0);
                            }
                            else
                                setFormulaToAstEquation(ev.getDelay(), newAstEquation);

                            if( newAstEquation.jjtGetChild(0) != null )
                                createSpace((SimpleNode)newAstEquation.jjtGetChild(0));
                        }
                        else
                        {
                            if( newAstEquation != null )
                                newAstEquation.remove();
                            for( int j = 0; j < at.jjtGetNumChildren(); j++ )
                                if( at.jjtGetChild(j) instanceof AstAfter )
                                    at.jjtGetChild(j).remove();
                        }
                        break;
                    }
                }
            }
        }
    }

    protected void choosePlaceForConstantAstNode(Variable var, AstModel astModel) throws Exception
    {
        for( int i = 0; i <= astModel.jjtGetNumChildren(); i++ )
        {
            if( i == astModel.jjtGetNumChildren() )
            {
                AstVarOrConst astConst = new AstVarOrConst(AntimonyNotationParser.JJTVARORCONST);
                createIndent(astConst);
                astConst.setType(var.isConstant());

                AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                createSpace(symbol);
                if( var instanceof VariableRole )
                {
                    Node varNode = (Node) ( (VariableRole)var ).getDiagramElement();
                    symbol.setName(validAndNotReservedName(varNode.getName()));
                    link(varNode, symbol);
                }
                else
                {
                    symbol.setName(validAndNotReservedName(var.getName()));
                    link(var.getAttributes(), symbol);
                }

                astConst.addAsLast(symbol);
                astConst.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
                astModel.addAsLast(astConst);
                astModel.addAsLast(new AstEOL(AntimonyNotationParser.JJTEOL));
                break;
            }
            if( astModel.jjtGetChild(i) instanceof AstVarOrConst
                    && ( (AstVarOrConst)astModel.jjtGetChild(i) ).isConst() == var.isConstant() )
            {
                AstVarOrConst astConst = (AstVarOrConst)astModel.jjtGetChild(i);


                AstSymbol symbol = new AstSymbol(AntimonyNotationParser.JJTSYMBOL);
                createSpace(symbol);
                if( var instanceof VariableRole )
                {
                    Node varNode = (Node) ( (VariableRole)var ).getDiagramElement();
                    symbol.setName(validAndNotReservedName(varNode.getName()));
                    link(varNode, symbol);
                }
                else
                {
                    symbol.setName(validAndNotReservedName(var.getName()));
                    link(var.getAttributes(), symbol);
                }
                SimpleNode parent;
                if( AntimonyUtility.isAstSymbolType(astConst.jjtGetChild(0)) )
                    parent = (SimpleNode)astConst.jjtGetChild(0);
                else
                    parent = astConst;
                int index = parent.jjtGetNumChildren() - 1;
                parent.addWithDisplacement(symbol, index);

                parent.addWithDisplacement(new AstComma(AntimonyNotationParser.JJTCOMMA), index);
                break;
            }
        }
    }

    Set<biouml.plugins.antimony.astparser_v2.Node> isHighlight = new HashSet<>();
    protected void highlight(DiagramElement de)
    {
        for( biouml.plugins.antimony.astparser_v2.Node astNode : isHighlight )
            ( (SimpleNode)astNode ).setHighlight(false);
        isHighlight = new HashSet<>();

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(de);
        if( astNodes == null )
            return;

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            ( (SimpleNode)astNode ).setHighlight(true);
            isHighlight.add(astNode);
        }
    }

    protected void changePriorityEvent(Event ev)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(ev.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEventAstSymbol(astNode) )
            {
                boolean declarationExist = false;
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstPriority )
                    {
                        setFormulaToAstEquation(ev.getPriority(), ( (AstPriority)astNode.jjtGetChild(i) ).getEquation());
                        declarationExist = true;
                        break;
                    }
                }
                if( !declarationExist )
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    {
                        if( astNode.jjtGetChild(i) instanceof AstAt )
                        {
                            AstPriority priority = new AstPriority(AntimonyNotationParser.JJTPRIORITY);
                            AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
                            createSpace(equal);
                            AstEquation priorityEquation = new AstEquation(AntimonyNotationParser.JJTEQUATION);
                            setFormulaToAstEquation(ev.getPriority(), priorityEquation);
                            if( priorityEquation.jjtGetChild(0) != null )
                                createSpace((SimpleNode)priorityEquation.jjtGetChild(0));
                            AstComma comma = new AstComma(AntimonyNotationParser.JJTCOMMA);
                            ( (SimpleNode)astNode ).addWithDisplacement(priority, i + 1);
                            ( (SimpleNode)astNode ).addWithDisplacement(comma, i + 1);
                            priority.addAsLast(equal);
                            priority.addAsLast(priorityEquation);
                        }
                    }
            }
        }
    }

    protected void changeUseValuesFromTriggerTimeEvent(Event ev)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(ev.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEventAstSymbol(astNode) )
            {
                boolean declarationExist = false;
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstUseValuesFromTriggerTime )
                    {
                        ( (AstUseValuesFromTriggerTime)astNode.jjtGetChild(i) ).setValue(ev.isUseValuesFromTriggerTime());
                        declarationExist = true;
                        break;
                    }
                }
                if( !declarationExist )
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    {
                        if( astNode.jjtGetChild(i) instanceof AstAt )
                        {
                            AstUseValuesFromTriggerTime useValuesFromTriggerTime = new AstUseValuesFromTriggerTime(
                                    AntimonyNotationParser.JJTUSEVALUESFROMTRIGGERTIME);
                            createEventAttribute(astNode, i, useValuesFromTriggerTime, ev.isUseValuesFromTriggerTime());
                        }
                    }
            }
        }
    }

    protected void changeTriggerPersistentEvent(Event ev)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(ev.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEventAstSymbol(astNode) )
            {
                boolean declarationExist = false;
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstPersistent )
                    {
                        ( (AstPersistent)astNode.jjtGetChild(i) ).setValue(ev.isTriggerPersistent());
                        declarationExist = true;
                        break;
                    }
                }
                if( !declarationExist )
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    {
                        if( astNode.jjtGetChild(i) instanceof AstAt )
                        {
                            AstPersistent triggerPersistent = new AstPersistent(AntimonyNotationParser.JJTPERSISTENT);
                            createEventAttribute(astNode, i, triggerPersistent, ev.isTriggerPersistent());
                        }
                    }
            }
        }
    }

    protected void changeTriggerInitialValueEvent(Event ev)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(ev.getDiagramElement());
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isEventAstSymbol(astNode) )
            {
                boolean declarationExist = false;
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstTriggerInitialValue )
                    {
                        ( (AstTriggerInitialValue)astNode.jjtGetChild(i) ).setValue(ev.isTriggerInitialValue());
                        declarationExist = true;
                        break;
                    }
                }
                if( !declarationExist )
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    {
                        if( astNode.jjtGetChild(i) instanceof AstAt )
                        {
                            AstTriggerInitialValue triggerInitialValue = new AstTriggerInitialValue(
                                    AntimonyNotationParser.JJTTRIGGERINITIALVALUE);
                            createEventAttribute(astNode, i, triggerInitialValue, ev.isTriggerInitialValue());
                        }
                    }
            }
        }
    }

    private void createEventAttribute(biouml.plugins.antimony.astparser_v2.Node astNode, int i, SimpleNode eventAttribute,
            boolean attributeValue)
    {
        createSpace(eventAttribute);
        AstEqual equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
        createSpace(equal);
        AstRegularFormulaElement value = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
        value.setElement(attributeValue);
        createSpace(value);
        AstComma comma = new AstComma(AntimonyNotationParser.JJTCOMMA);
        ( (SimpleNode)astNode ).addWithDisplacement(eventAttribute, i + 1);
        ( (SimpleNode)astNode ).addWithDisplacement(comma, i + 1);
        eventAttribute.addAsLast(equal);
        eventAttribute.addAsLast(value);
    }

    protected void changeMainVariable(Edge edge)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(edge);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isSynchronizationsSymbol(astNode) )
            {
                AstSymbol firstSymbol = (AstSymbol)astNode;
                for( int i = 0; i < firstSymbol.jjtGetNumChildren(); i++ )
                    if( firstSymbol.jjtGetChild(i) instanceof AstIs )
                    {
                        SimpleNode is = (SimpleNode)firstSymbol.jjtGetChild(i);
                        AstSymbol secondSymbol = ( (AstIs)is ).getSynchronizedElement();
                        String name = secondSymbol.getName();
                        secondSymbol.setName(firstSymbol.getName());
                        firstSymbol.setName(name);
                        break;
                    }
            }
        }
    }

    protected void changeConversionFactor(SubDiagram source, String factorName, String antimonyProperty)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(source);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isSubdiagramAstSymbol(astNode) )
            {
                DynamicProperty factorProperty = source.getAttributes().getProperty(factorName);
                if( factorProperty == null )
                    return;
                String value = factorProperty.getValue().toString();
                for( int j = 0; j < astNode.jjtGetNumChildren(); j++ )
                {
                    if( astNode.jjtGetChild(j) instanceof AstConversionFactor )
                    {
                        AstConversionFactor astFactor = (AstConversionFactor)astNode.jjtGetChild(j);
                        String factor = astFactor.getFactor();
                        if( antimonyProperty.equals(factor) )
                        {
                            if( value == null || value.isEmpty() )
                            {
                                astNode.removeChild(j);
                                //remove comma
                                if( astNode.jjtGetChild(j - 1) instanceof AstComma )
                                    astNode.removeChild(j - 1);
                            }
                            else
                                astFactor.setValue(value);
                            return;
                        }
                    }
                }
                if( value == null || value.isEmpty() )
                    return;
                AstConversionFactor astFactor = createConversionFactor(factorProperty, antimonyProperty);
                ( (SimpleNode)astNode ).addWithDisplacement(new AstComma(AntimonyNotationParser.JJTCOMMA), astNode.jjtGetNumChildren() - 1);
                ( (SimpleNode)astNode ).addWithDisplacement(astFactor, astNode.jjtGetNumChildren() - 1);
            }
        }
    }

    private List<AstProperty> findProperties(DiagramElement de, String notation, String name)
    {
        List<AstProperty> result = new ArrayList<>();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(de);
        if( astNodes != null )
        {
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstProperty )
                {
                    AstProperty property = ( (AstProperty)astNode );
                    if( property.getNotationType().equals(notation) && ( property.getChainNames().isEmpty()
                            || property.getChainNames().get(property.getChainNames().size() - 1).equals(name) ) )
                        result.add(property);
                }
            }
        }
        return result;
    }

    private List<AstSingleProperty> findSingleProperties(AstProperty property, String propName)
    {
        List<AstSingleProperty> result = new ArrayList<>();
        for( biouml.plugins.antimony.astparser_v2.Node child : property.getChildren() )
        {
            if( child instanceof AstSingleProperty && propName.equals( ( (AstSingleProperty)child ).getPropertyName()) )
            {
                result.add((AstSingleProperty)child);
            }
        }
        return result;
    }

    /**
     * Change modifier action of modifier specie reference. There are 5 types of modifiers in SBGN and 3 types in Antimony.
     * 1. Inhibitor (inhibition in SBGN). In Antimony is denoted as: "R_mod : A -| R;" <br>
     * 2. Unknown (modulation in SBGN, may inhibit or activate). In Antimony: "R_mod: A -( R;" <br>
     * 3. Activator: "R_mod : A -o R;" <br>
     * In the last case we have three options in SBGN: "catalysis", "stimulation" and "neccessary stimulation".<br>
     * By default we will consider "catalysis" modifier type.<br> 
     * To distinguish between "stimulation" and "neccessary stimulation" we add "edgeType" property e.g.
     *  "@sbgn R_mod.edgeType = "stimulation"";
     */
    protected void changeModifierType(SpecieReference reference, String modifierAction, AstStart astStart)
    {
        Edge edge = (Edge)diagram.findDiagramElement(reference.getName());
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(edge);
        if( astNodes == null )
            return;

        boolean propertyExists = false;
        boolean addProperty = false;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isModifierAstSymbol(astNode) )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    if( astNode.jjtGetChild(i) instanceof AstReactionTitle )
                    {
                        String antimonyNotation = AstReactionType.UNKNOWN_MODIFIER;
                        AstReactionTitle reactionTitle = (AstReactionTitle)astNode.jjtGetChild(i);
                        if( SpecieReference.ACTION_INHIBITION.equals(modifierAction) )
                            antimonyNotation = AstReactionType.INHIBITOR;
                        else if( SpecieReference.ACTION_CATALYSIS.equals(modifierAction) )
                            antimonyNotation = AstReactionType.ACTIVATOR;
                        else if( SpecieReference.ACTION_NECCESSARY_STIMULATION.equals(modifierAction)
                                || SpecieReference.ACTION_STIMULATION.equals(modifierAction) )
                        {
                            antimonyNotation = AstReactionType.ACTIVATOR;
                            addProperty = true; //to resolve ambiguity we add additional property later
                        }
                        reactionTitle.getReactionType().setType(antimonyNotation);
                    }
            }
        }
        //        boolean toAddSingleProperty = false;

        List<AstProperty> toDeleteProps = new ArrayList<>();
        List<AstProperty> props = findProperties(edge, "sbgn", reference.getName());
        for( AstProperty prop : props )
        {
            List<AstSingleProperty> toDelete = new ArrayList<>();
            boolean toAddSingleProperty = true;
            for( AstSingleProperty sprop : findSingleProperties(prop, "edgeType") )
            {
                propertyExists = true;
                toAddSingleProperty = false;
                if( addProperty )
                    sprop.setPropertyValue(reference.getModifierAction());
                else
                    toDelete.add(sprop);
            }
            if( toAddSingleProperty && addProperty )
            {
                propertyExists = true;
                prop.addSingleProperty("edgeType", reference.getModifierAction());
            }
            for( AstSingleProperty toDel : toDelete )
                toDel.remove();
            if( prop.isEmpty() )
            {
                toDeleteProps.add(prop);
            }

        }
        for( AstProperty prop : toDeleteProps )
        {
            this.getLinkedAstNode(edge).remove(prop);
            removeAstNode(prop);
            prop.remove();
        }
        if( !propertyExists && addProperty )
        {
            AstModel model = getModel(edge, astStart);
            HashMap<String, Object> value = new HashMap<String, Object>();
            value.put("edgeType", reference.getModifierAction());
            addProperty(edge, "sbgn", value, model);
        }
        else
        {
            //delete comment if needed
            AstModel model = getModel(edge, astStart);
            System.out.println("");
            //            int index = model.findIndex( blockLocations.get( AntimonyConstants.BLOCK_PROPERTIES ) );
            //            if( index > -1 )
            //            {
            //                model.removeChild( index );
            //
            //            }
        }
    }


    protected void changeProperty(DiagramElement source, String attributeName, String newValue, AstStart astStart) throws Exception
    {
        if( !AntimonyAnnotationImporter.isPropertyImported(attributeName) )
            return;
        boolean propertyExist = false;

        boolean checkType = AntimonyConstants.SBGN_TYPE.equals(attributeName);

        String notationType;
        if( attributeName.startsWith("sbgn") )
            notationType = "sbgn";
        else if( attributeName.startsWith("glycan") )
            notationType = "glycan";
        else if( attributeName.startsWith("smiles") )
            notationType = "smiles";
        else
            notationType = "biouml";

        if( attributeName.endsWith("EdgeType") )
            attributeName = "edgeType";
        else if( attributeName.endsWith("reactionType") )
            attributeName = "reactionType";
        else if( attributeName.endsWith("Type") )
            attributeName = "type";
        else if( attributeName.endsWith("Structure") )
            attributeName = "structure";
        else if( attributeName.endsWith("multimer") )
            attributeName = "multimer";
        else if( attributeName.endsWith("title") )
            attributeName = "title";


        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(source);
        if( astNodes != null )
        {
            //create array to avoid ConcurrentModificationException
            biouml.plugins.antimony.astparser_v2.Node[] astNodesArr = astNodes.stream()
                    .toArray(biouml.plugins.antimony.astparser_v2.Node[]::new);
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodesArr )
            {
                if( astNode instanceof AstProperty )
                {
                    AstProperty property = (AstProperty)astNode;

                    if( !property.getNotationType().equals(notationType) || ( !property.getChainNames().isEmpty()
                            && !property.getChainNames().get(property.getChainNames().size() - 1).equals(source.getName()) ) )
                        continue;


                    AstSingleProperty sprop = property.getSinglePropety(attributeName);

                    // for clone, we are searching for an available property 
                    // we can't just set the title because it relates to several clones
                    if( sprop != null )
                    {
                        if( sprop.getPropertyName().equals(AntimonyConstants.SBGN_CLONE) )
                        {
                            propertyExist = false | propertyExist;
                        }
                        else
                        {
                            propertyExist = true;
                            sprop.setPropertyValue(newValue);
                        }
                    }
                    else
                    {
                        propertyExist = true;
                        property.addSingleProperty(attributeName, newValue);
                    }
                }
                else if( checkType && AntimonyUtility.isAstSymbolType(astNode.jjtGetParent()) )
                {
                    AstSymbolType curParent = (AstSymbolType)astNode.jjtGetParent();
                    biouml.plugins.antimony.astparser_v2.Node model = curParent;
                    while( model != null && !AntimonyUtility.isAstModel(model) )
                        model = model.jjtGetParent();
                    if( model == null )
                        continue;
                    String type = null;
                    if( AstSymbolType.GENE.equals(curParent.getType()) && !Type.TYPE_NUCLEIC_ACID_FEATURE.equals(newValue) )
                        type = AstSymbolType.SPECIES;
                    else if( !AstSymbolType.GENE.equals(curParent.getType()) && Type.TYPE_NUCLEIC_ACID_FEATURE.equals(newValue) )
                        type = AstSymbolType.GENE;
                    if( type != null )
                    {
                        removeAstNode(astNode);
                        removeLink(source, astNode);
                        placeTypeDeclaration(source, (AstModel)model, type);
                    }
                }
            }
        }

        if( !propertyExist )
        {
            HashMap<String, Object> value = new HashMap<String, Object>();
            value.put(attributeName, newValue);
            addProperty(source, notationType, value, getModel(source, astStart));
        }
    }

    public void changeSbgnViewTitle(Node node, AstStart astStart) throws Exception
    {
        if( ! ( node.getKernel() instanceof Specie ) && !SbgnUtil.isVariableNode(node) && !SbgnUtil.isUnitOfInformation(node) )
            return;

        if( node.getParent() instanceof Node )
        {
            Node currentNode = node;
            Node parent = (Node)currentNode.getParent();
            while( ! ( currentNode.getKernel() instanceof Specie && !SbgnUtil.isComplex(parent) || SbgnUtil.isPhenotype(currentNode) ) )
            {
                if( parent.getParent() instanceof Node )
                {
                    currentNode = parent;
                    parent = (Node)currentNode.getParent();
                }
                else
                    return;
            }
            node = currentNode;
        }

        String value = SbgnComplexStructureManager.constructSBGNViewTitle(node);
        //node.getAttributes().add(DPSUtils.createTransient(Util.COMPLEX_STRUCTURE, String.class, value));
        if( ! ( value.isEmpty()
                || ( SbgnUtil.isComplex(node) && node.getParent() instanceof Node && SbgnUtil.isComplex((Node)node.getParent()) ) ) )
            changeProperty(node, AntimonyConstants.SBGN_STRUCTURE, value, astStart);

    }

    public void addUnits(Set<Unit> newSet, AstStart astStart)
    {
        List<biouml.plugins.antimony.astparser_v2.Node> units = new ArrayList<biouml.plugins.antimony.astparser_v2.Node>();
        for( Unit unit : newSet )
        {
            AstUnit astUnit = new AstUnit(AntimonyNotationParser.JJTUNIT);
            AstSymbol symbol = createSymbol(validAndNotReservedName(unit.getName()));
            String unitFormula = unit.getUnitFormula();
            if( !unitFormula.isEmpty() )
            {
                SimpleNode equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);
                AstUnitFormula formula = new AstUnitFormula(AntimonyNotationParser.JJTUNITFORMULA);
                setFormulaToUnitFormula(unit.getUnitFormula(), formula);
                createSpace(symbol);
                createSpace(equal);
                astUnit.addAsLast(symbol);
                astUnit.addAsLast(equal);
                astUnit.addAsLast(formula);
                if( formula.jjtGetChild(0) != null )
                    createSpace((SimpleNode)formula.jjtGetChild(0));
            }
            else
            {
                createSpace(symbol);
                astUnit.addAsLast(symbol);
            }

            astUnit.addAsLast(new AstSemicolon(AntimonyNotationParser.JJTSEMICOLON));
            link( ( (Unit)unit ).getAttributes(), astUnit);

            units.add(astUnit);
        }

        int index;
        if( blockLocations.containsKey(AntimonyConstants.BLOCK_UNITS) )
            index = astStart.findIndex(blockLocations.get(AntimonyConstants.BLOCK_UNITS)) + 1;
        else
            index = astStart.jjtGetNumChildren();

        for( biouml.plugins.antimony.astparser_v2.Node unit : units )
        {
            astStart.addWithDisplacement(new AstEOL(AntimonyNotationParser.JJTEOL), index);
            astStart.addWithDisplacement(unit, index + 1);
            index += 2;
        }

        blockLocations.put(AntimonyConstants.BLOCK_UNITS, astStart.jjtGetChild(index - 1));
    }

    public void changeUnitFormula(Unit unit)
    {
        DynamicProperty dp = unit.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(dp);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( astNode instanceof AstUnit )
            {

                String unitFormula = unit.getUnitFormula();

                if( unitFormula.isEmpty() )
                {
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                        if( astNode.jjtGetChild(i) instanceof AstUnitFormula || astNode.jjtGetChild(i) instanceof AstEqual )
                        {
                            astNode.removeChild(i);
                        }

                }
                else
                {
                    boolean hasFormula = false;
                    AstUnitFormula formula = new AstUnitFormula(AntimonyNotationParser.JJTUNITFORMULA);
                    setFormulaToUnitFormula(unitFormula, formula);
                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                        if( astNode.jjtGetChild(i) instanceof AstUnitFormula )
                        {
                            astNode.jjtAddChild(formula, i);
                            hasFormula = true;
                            if( formula.jjtGetChild(0) != null )
                                createSpace((SimpleNode)formula.jjtGetChild(0));
                        }

                    if( !hasFormula )
                    {
                        for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                            if( astNode.jjtGetChild(i) instanceof AstSymbol )
                            {
                                SimpleNode equal = new AstEqual(AntimonyNotationParser.JJTEQUAL);

                                createSpace(equal);
                                setFormulaToUnitFormula(unitFormula, formula);
                                astNode.addWithDisplacement(equal, i + 1);
                                astNode.addWithDisplacement(formula, i + 2);
                                hasFormula = true;
                                if( formula.jjtGetChild(0) != null )
                                    createSpace((SimpleNode)formula.jjtGetChild(0));
                                return;
                            }
                    }
                }
            }
        }
    }

    public void removeUnits(Set<Unit> oldSet, AstStart astStart)
    {
        for( Unit unit : oldSet )
        {
            DynamicProperty dp = unit.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(dp);
            if( astNodes == null )
                return;
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( AntimonyUtility.isInitAstSymbol(astNode) )
                    StreamEx.of( ( (AstSymbol)astNode ).getAllChildren()).select(AstEquation.class).forEach(childSymbol -> {
                        childSymbol.removeChild(1);
                    });

                else
                    removeAstNode(astNode);
            }
        }

    }


    @SuppressWarnings ( "unchecked" )
    public void changeUnits(PropertyChangeEvent e, AstStart astStart)
    {
        Set<Unit> newSet = new HashSet<Unit>( ( (Map)e.getNewValue() ).values());
        Set<Unit> oldSet = new HashSet<Unit>( ( (Map)e.getOldValue() ).values());
        Set<Unit> intersect = newSet.stream().filter(oldSet::contains).collect(Collectors.toSet());

        newSet.removeAll(intersect);
        if( !newSet.isEmpty() )
            addUnits(newSet, astStart);

        oldSet.removeAll(intersect);
        if( !oldSet.isEmpty() )
            removeUnits(oldSet, astStart);

    }

    public void setUnit(Variable var, AstStart astStart)
    {
        AstModel astModel = getModel(var, astStart);
        setUnit(var, astModel);

    }

    private void setUnit(Variable var, AstModel astModel)
    {
        DynamicProperty dp;
        boolean unitIsSet = false;
        if( var instanceof VariableRole )
        {
            DiagramElement de = ( (VariableRole)var ).getDiagramElement();
            dp = de.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        }
        else
        {
            dp = var.getAttributes().getProperty(AntimonyConstants.ANTIMONY_LINK);
        }

        if( dp != null )
        {
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(dp);
            if( astNodes == null ) // q
                return;

            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( AntimonyUtility.isInitAstSymbol(astNode) )
                {
                    StreamEx.of( ( (AstSymbol)astNode ).getAllChildren()).select(AstEquation.class).forEach(childSymbol -> {
                        childSymbol.setUnit(var.getUnits());
                    });
                    unitIsSet = true;
                }
                else if( AntimonyUtility.isHasUnitAstSymbol(astNode) )
                {
                    if( var.getUnits().equals(Unit.UNDEFINED) || unitIsSet )
                    {
                        removeAstNode(astNode);
                        if( var instanceof VariableRole )
                        {
                            Node varNode = (Node) ( (VariableRole)var ).getDiagramElement();
                            removeLink(varNode, astNode);
                        }
                        else
                        {
                            removeLink(var.getAttributes(), astNode);
                        }
                        return;
                    }
                    ( (AstHas) ( (AstSymbol)astNode ).jjtGetChild(0) ).setUnitName(var.getUnits());
                    return;
                }
            }
        }

        // if no has unit construction found
        if( !unitIsSet )
        {
            addUnitAssignment(var, astModel, true);
        }

    }

    public void updateConstraintMessage(PropertyChangeEvent e)
    {
        Constraint constr = (Constraint)e.getSource();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(constr.getDiagramElement());

        if( astNodes == null )
            return;

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )

            if( AntimonyUtility.isConstraintAstSymbol(astNode) )
            {
                String message = constr.getMessage();
                if( message.isEmpty() )
                {
                    for( int i = astNode.jjtGetNumChildren() - 1; i >= 0; i-- )
                        if( astNode.jjtGetChild(i) instanceof AstElse || astNode.jjtGetChild(i) instanceof AstText )
                            astNode.removeChild(i);

                }
                else
                {
                    boolean hasText = false;
                    for( biouml.plugins.antimony.astparser_v2.Node node : ( (AstSymbol)astNode ).getAllChildren() )
                        if( node instanceof AstText )
                        {
                            ( (AstText)node ).setText("\"" + message + "\"");
                            hasText = true;
                            break;
                        }

                    if( !hasText )

                        for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                            if( astNode.jjtGetChild(i) instanceof AstAssert )
                            {
                                AstElse els = new AstElse(AntimonyNotationParser.JJTELSE);
                                createSpace(els);

                                AstText text = new AstText(AntimonyNotationParser.JJTTEXT);
                                text.setText("\"" + message + "\"");
                                createSpace(text);

                                astNode.addWithDisplacement(els, i + 1);
                                astNode.addWithDisplacement(text, i + 2);
                            }
                }
            }
    }

    public void changeLogicalType(Node node, AstStart astStart)
    {
        AstModel astModel = getModel(node, astStart);
        changeLogicalType(node, astModel);
    }

    private void changeLogicalType(Node node, AstModel astModel)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);

        if( astNodes == null )
            return;

        String type = node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR);
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            if( AntimonyUtility.isModifierAstSymbol(astNode) )
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    if( astNode.jjtGetChild(i) instanceof AstReactionTitle )
                    {
                        ( (AstReactionTitle)astNode.jjtGetChild(i) ).setType(type);
                        return;
                    }
    }

    /**
     * Changes URI in database reference
     * @param e
     * @param astStart
     */
    public void changeURI(PropertyChangeEvent e, AstStart astStart)
    {
        DatabaseReference dr = (DatabaseReference)e.getSource();
        Node node = (Node)dr.getParent().getParent();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);

        if( astNodes == null )
            return;

        String oldURI = "http://identifiers.org/";
        if( e.getPropertyName().equals("databaseName") )
            oldURI += e.getOldValue() + "/" + dr.getId();
        else if( e.getPropertyName().equals("id") )
            oldURI += dr.getDatabaseName() + "/" + e.getOldValue();

        String URI = "http://identifiers.org/" + dr.getDatabaseName() + "/" + dr.getId();
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            if( AntimonyUtility.isDatabaseReferenceAstSymbol(astNode) )
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    biouml.plugins.antimony.astparser_v2.Node child = astNode.jjtGetChild(i);
                    if( child instanceof AstRelationshipType )
                    {
                        if( ! ( (AstRelationshipType)child ).getName().equals(dr.getRelationshipType()) )
                            break;
                    }
                    else if( child instanceof AstText )
                    {
                        if( ( (AstText)child ).getText().equals("\"" + oldURI + "\"") )
                        {
                            ( (AstText)child ).setText("\"" + URI + "\"");
                            return;
                        }
                        else if( ( (AstText)child ).getText().equals("\"" + URI + "\"") )
                            return;
                    }
                }
        // if not returned, corresponding db ref does not exist
        addRelTypeCVterms(dr.getRelationshipType(), Arrays.asList(URI), node, astStart.getMainModel(), false);
    }

    /**
     * Changes relationship type in database reference
     */
    public void changeRelationshipType(PropertyChangeEvent e, AstStart astStart)
    {
        DatabaseReference dr = (DatabaseReference)e.getSource();
        Node node = (Node)dr.getParent().getParent();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);

        if( astNodes == null )
            return;

        String URI = "http://identifiers.org/" + dr.getDatabaseName() + "/" + dr.getId();
        String oldVal = e.getOldValue() == null ? "" : (String)e.getOldValue();

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            if( AntimonyUtility.isDatabaseReferenceAstSymbol(astNode) )
            {
                AstRelationshipType possibleTypeNodeToChange = null;
                int textNodeCounter = 0;
                AstText textNode = null;
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {

                    biouml.plugins.antimony.astparser_v2.Node child = astNode.jjtGetChild(i);
                    if( child instanceof AstRelationshipType )
                    {
                        if( ( (AstRelationshipType)child ).getName().equals(oldVal)
                                || ( (AstRelationshipType)child ).getName().equals(e.getNewValue()) )
                            possibleTypeNodeToChange = (AstRelationshipType)child;
                    }
                    else if( child instanceof AstText )
                    {
                        textNodeCounter++;
                        if( ( (AstText)child ).getText().equals("\"" + URI + "\"") )
                            textNode = (AstText)child;
                        else if( oldVal.isEmpty() )
                        {
                            astNode.addWithDisplacement(new AstComma(AntimonyNotationParser.JJTCOMMA), i + 1);
                            AstText message = new AstText(AntimonyNotationParser.JJTTEXT);
                            message.setText("\"" + URI + "\"");
                            createSpace(message);
                            astNode.addWithDisplacement(message, i + 2);
                            return;
                        }
                    }

                }

                if( possibleTypeNodeToChange != null )
                {
                    if( textNode != null )
                    {
                        if( textNodeCounter == 1 )
                        {
                            possibleTypeNodeToChange.setName((String)e.getNewValue());
                            return;
                        }
                        else
                        {
                            removeAstNode(textNode);
                            break;
                        }
                    }
                }


            }


        addRelTypeCVterms(dr.getRelationshipType(), Arrays.asList(URI), node, astStart.getMainModel(), true);
    }


    public void removeDatabaseReference(PropertyChangeEvent e, AstStart astStart)
    {
        Arrays.asList(e.getNewValue());

        Set<DatabaseReference> newSet = new HashSet<DatabaseReference>(Arrays.asList((DatabaseReference[])e.getNewValue()));
        Set<DatabaseReference> oldSet = new HashSet<DatabaseReference>(Arrays.asList((DatabaseReference[])e.getOldValue()));
        oldSet.removeAll(newSet);

        for( DatabaseReference dr : oldSet )
        {

            Node node = (Node)e.getPropagationId();
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);

            String URI = "http://identifiers.org/" + dr.getDatabaseName() + "/" + dr.getId();
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                if( AntimonyUtility.isDatabaseReferenceAstSymbol(astNode) )
                {

                    for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                    {
                        biouml.plugins.antimony.astparser_v2.Node child = astNode.jjtGetChild(i);

                        if( child instanceof AstRelationshipType )
                            if( ! ( (AstRelationshipType)child ).getName().equals(dr.getRelationshipType()) )
                                break;


                        if( child instanceof AstText && ( (AstText)child ).getText().equals("\"" + URI + "\"") )
                        {
                            removeAstNode(child);
                            removeLink(node, astNode);
                            return;
                        }
                    }
                }
        }

    }

    void changeTitleInProperty(Node node, String propertyName)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);
        AstMap map = null;
        if( astNodes != null )
        {
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstProperty )
                    for( biouml.plugins.antimony.astparser_v2.Node child : ( (AstProperty)astNode ).getChildren() )
                    {
                        if( child instanceof AstSingleProperty )
                        {
                            AstSingleProperty sp = (AstSingleProperty)child;
                            if( sp.getPropertyName().equals(propertyName) && sp.getValueNode() instanceof AstMap )
                            {
                                map = (AstMap)sp.getValueNode();
                                break;
                            }
                        }
                    }
            }

            if( map == null )
                return;

            for( biouml.plugins.antimony.astparser_v2.Node astNode : map.getChildren() )
            {
                if( astNode instanceof AstSingleProperty && ( (AstSingleProperty)astNode ).getPropertyName().equals("title") )
                {
                    ( (AstSingleProperty)astNode ).setPropertyValue(node.getTitle());
                    return;
                }
            }
        }

        return;

    }

    public void addCloneProperty(Node node, AstStart astStart)
    {
        AstModel astModel = getModel(node, astStart);
        addCloneProperty(node, astModel, true);
    }

    /**
     * Removes the reaction from reaction list in clone property
     * @param node
     * @param reactionNode
     */
    public void removeReactionFromClone(Node node, Node reactionNode)
    {
        if( !AntimonyUtility.isClone(node) )
            return;

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(reactionNode);

        if( astNodes != null )
        {
            String realName = ( (VariableRole)node.getRole() ).getShortName();
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )

                if( astNode instanceof AstSymbol && astNode.jjtGetParent() instanceof AstList )
                {
                    SimpleNode current = (SimpleNode)astNode.jjtGetParent();
                    while( ! ( current instanceof AstStart ) )
                    {
                        if( current instanceof AstProperty && ( (AstProperty)current ).getChainNames().get(0).equals(realName) )
                        {
                            removeAstNode(astNode);
                            removeLink(reactionNode, astNode);
                            return;
                        }
                        current = (SimpleNode)current.jjtGetParent();
                    }
                }
        }
    }

    /**
     * Moves the reaction to the list of reactions in the property astnode of new clone 
     * @param oldNode
     * @param newNode
     * @param reactionNode
     */
    void updateCloneReaction(Node oldNode, Node newNode, Node reactionNode)
    {
        removeReactionFromClone(oldNode, reactionNode);
        addReactionToClone(newNode, reactionNode);

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(oldNode);

        if( astNodes != null )
        {
            AstSymbol nodeToChangeLink = null;
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
            {
                if( astNode instanceof AstSymbol )
                {
                    if( astNode.jjtGetParent() instanceof AstReactant || astNode.jjtGetParent() instanceof AstReactant )
                    {
                        SimpleNode currentNode = (SimpleNode)astNode.jjtGetParent();
                        AstSymbol potentialNode = (AstSymbol)astNode;
                        while( ! ( currentNode instanceof AstStart ) )
                        {
                            if( currentNode instanceof AstSymbol
                                    && ( (AstSymbol)currentNode ).getDeclarationType().equals(AstSymbol.REACTION_DEFINITION)
                                    && ( (AstSymbol)currentNode ).getName().equals(reactionNode.getName()) )
                            {
                                nodeToChangeLink = potentialNode;
                                break;
                            }
                            currentNode = (SimpleNode)currentNode.jjtGetParent();
                        }
                    }
                    //                    else
                    //                        nodesToChangeLink.add((AstSymbol)astNode); // isn't it
                }
            }

            if( nodeToChangeLink != null )
            {
                removeLink(oldNode, nodeToChangeLink);
                link(newNode, nodeToChangeLink);
                return;
            }

        }
    }

    /**
     * Updates blocks' locations after application of antimony text
     * @param ast - AstStart or AstModel
     */
    void updateBlockLocations(biouml.plugins.antimony.astparser_v2.Node ast)
    {
        if( ast instanceof AstStart )
            blockLocations.clear();
        else if( ! ( ast instanceof AstModel ) )
            return;

        for( int i = ast.jjtGetNumChildren() - 1; i >= 0; i-- )
        {
            biouml.plugins.antimony.astparser_v2.Node node = ast.jjtGetChild(i);

            if( node instanceof AstModel )
                updateBlockLocations(node);
            else if( ( node instanceof AstEOL ) || ( node instanceof AstSymbolType ) || ( node instanceof AstVarOrConst ) )
                continue;
            else
            {
                String type = AntimonyUtility.getBlockType(node);
                if( type == null )
                    continue;

                if( blockLocations.get(type) == null )
                    blockLocations.put(type, node);
            }

        }


    }

    public void changeQuantityType(VariableRole variableRole, AstStart astStart)
    {
        AstModel astModel = getModel(variableRole, astStart);
        changeQuantityType(variableRole, astModel);
    }

    private void changeQuantityType(VariableRole variableRole, AstModel astModel)
    {
        Node node = (Node)variableRole.getDiagramElement();
        boolean isSubstanceOnly = variableRole.getQuantityType() == VariableRole.AMOUNT_TYPE;
        if( node.getKernel() instanceof Specie )
        {
            String entityType = ( (Specie)node.getKernel() ).getType();
            Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);
            for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
                if( AntimonyUtility.isAstSymbolType(astNode.jjtGetParent())
                        && AntimonyUtility.isAstSubstanceOnly(astNode.jjtGetParent().jjtGetParent()) != isSubstanceOnly )
                {
                    removeAstNode(astNode);
                    removeLink(node, astNode);
                }

            if( SbgnUtil.isComplex(node) )
            {
                if( node.getParent() instanceof Node && !SbgnUtil.isComplex((Node)node.getParent()) )
                    placeTypeDeclaration(node, astModel, AstSymbolType.SPECIES);
            }
            else
            {
                if( AntimonyConstants.SBGN_GENE.equals(entityType) )
                    placeTypeDeclaration(node, astModel, AstSymbolType.GENE);
                else
                    placeTypeDeclaration(node, astModel, AstSymbolType.SPECIES);
            }
        }
    }

    void changeInitialQuantityType(VariableRole var, AstStart astStart)
    {
        String compartmentName = null;
        DiagramElement de = ( (VariableRole)var ).getDiagramElement();

        if( ( (VariableRole)var ).getInitialQuantityType() == VariableRole.AMOUNT_TYPE )
            if( de.getParent() instanceof Compartment
                    && ( (Compartment)de.getParent() ).getKernel() instanceof biouml.standard.type.Compartment )
                compartmentName = ( (Compartment)de.getParent() ).getName();

        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(de);
        if( astNodes == null )
            return;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isInitAstSymbol(astNode) )
            {
                for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
                {
                    if( astNode.jjtGetChild(i) instanceof AstEquation )
                    {
                        AstEquation eq = (AstEquation)astNode.jjtGetChild(i);
                        if( compartmentName != null )
                        {
                            AstRegularFormulaElement div = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
                            div.setElement("/");
                            createSpace(div);
                            eq.addAsLast(div);

                            AstRegularFormulaElement comp = new AstRegularFormulaElement(AntimonyNotationParser.JJTREGULARFORMULAELEMENT);
                            comp.setString(true);
                            comp.setElement(compartmentName);
                            createSpace(comp);
                            eq.addAsLast(comp);
                        }
                        else if( eq.hasAmountQuantityType() )
                        {
                            eq.removeChild(eq.jjtGetNumChildren() - 1); // compartment name removed
                            eq.removeChild(eq.jjtGetNumChildren() - 1); // division operator removed
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the text in note property
     * @param node
     * @param newText
     */
    public void changeNoteText(Node node, String newText)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);

        if( astNodes == null )
            return;

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )

            if( AntimonyUtility.isAstProperty(astNode) )
            {
                AstProperty property = (AstProperty)astNode;
                property.getSinglePropety("text").setPropertyValue(newText);
                return;
            }
    }

    /**
     * Update list of nodes connected to the note node in note property
     * @param node
     * @param newText
     */
    public void updateNoteNodes(Node node, String newText)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(node);

        if( astNodes == null )
            return;

        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )

            if( AntimonyUtility.isAstProperty(astNode) )
            {
                AstProperty property = (AstProperty)astNode;
                property.getSinglePropety("text").setPropertyValue(newText);
                return;
            }
    }


    /**
     * Changes a variable or name of a column in columns/argColumn of table property 
     * @param subPropertyName
     * @param e
     * @param model
     */
    @SuppressWarnings ( "unchecked" )
    void changeTableProperty(String subPropertyName, PropertyChangeEvent e, AstModel model)
    {
        VarColumn col = (VarColumn)e.getSource();
        Node tableNode = (Node)col.getParent();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(tableNode);

        if( astNodes == null )
            return;

        String propertyName;
        // check references to identify whether it is argcolumn or not
        if( ( (SimpleTableElement)tableNode.getRole() ).getArgColumn() == col )
            propertyName = "argColumn";
        else
            propertyName = "columns";


        String newValue = "";
        if( subPropertyName.equals("variable") )
            newValue = col.getVariable();
        else if( subPropertyName.equals("name") )
            newValue = col.getColumn();
        else
            return;

        boolean toDelete = newValue.isEmpty();

        AstSingleProperty sprop = null;
        AstProperty property = null;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )

            if( AntimonyUtility.isAstProperty(astNode) )
            {
                property = (AstProperty)astNode;
                sprop = property.getSinglePropety(propertyName);
            }

        if( property == null )
            return;

        if( sprop == null )
        {
            if( !toDelete )
            {
                Object colProps;
                if( propertyName.equals("argColumn") )
                {
                    colProps = new HashMap<String, String>();
                    ( (HashMap<String, String>)colProps ).put(subPropertyName, newValue);
                }
                else
                {
                    colProps = new HashSet<Map<String, String>>();
                    Map<String, String> singleColProps = new HashMap<String, String>();
                    singleColProps.put(subPropertyName, newValue);
                    ( (HashSet<Map<String, String>>)colProps ).add(singleColProps);
                }
                property.addSingleProperty(propertyName, colProps);
            }
        }
        else
        {
            // have to add AstSingleProperty with propertyName if it's not an empty string
            if( propertyName.equals("argColumn") )
            {
                AstMap map = (AstMap)sprop.getValueNode();
                AstSingleProperty subProp = map.getSingleProperty(subPropertyName);
                if( subProp == null )
                {
                    if( !toDelete )
                        map.addSingleProperty(subPropertyName, newValue);
                }
                else
                {
                    if( !toDelete )
                        subProp.setPropertyValue(newValue);
                    else
                        removeAstNode(subProp);
                }
            }
            else //if( propertyName.equals("columns") )
            {
                AstSet set = (AstSet)sprop.getValueNode();

                Map<String, String> oldColProps = new HashMap<String, String>();
                boolean isVariableProperty = subPropertyName.equals("variable");

                //put properties of a column to reset or create a new value node
                if( ! ( (String)e.getOldValue() ).isEmpty() )
                    oldColProps.put(subPropertyName, (String)e.getOldValue());

                if( isVariableProperty )
                {
                    if( !col.getColumn().isEmpty() )
                        oldColProps.put("name", col.getColumn());
                }
                else if( !col.getVariable().isEmpty() )
                    oldColProps.put("variable", col.getVariable());

                // searching for astnode containing old properties
                SimpleNode elem = set.get(oldColProps);
                if( elem == null && !toDelete )
                {
                    Map<String, String> newColProps = new HashMap<String, String>();
                    newColProps.put(subPropertyName, newValue);
                    set.add(newColProps);
                }
                else if( elem instanceof AstMap )
                {
                    AstMap map = (AstMap)elem;
                    AstSingleProperty subProp = map.getSingleProperty(subPropertyName);
                    if( subProp == null )
                    {
                        if( !toDelete )
                            map.addSingleProperty(subPropertyName, newValue);
                    }
                    else
                    {
                        if( !toDelete )
                            subProp.setPropertyValue(newValue);
                        else
                            removeAstNode(subProp);
                    }
                }
            }

        }
    }

    /**
     * Removes a column from columns for table property
     * @param e 
     * @param model
     */
    public void removeColumn(PropertyChangeEvent e, AstModel model)
    {
        Node tableNode = (Node)e.getPropagationId();
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(tableNode);

        if( astNodes == null )
            return;

        AstSingleProperty sprop = null;
        AstProperty property = null;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )

            if( AntimonyUtility.isAstProperty(astNode) )
            {
                property = (AstProperty)astNode;
                sprop = property.getSinglePropety("columns");
            }

        if( sprop == null || property == null )
            return;

        // find removed VarColumn
        Set<VarColumn> oldCols = new HashSet<VarColumn>(Arrays.asList((VarColumn[])e.getOldValue()));
        for( VarColumn col : (VarColumn[])e.getNewValue() )
            oldCols.remove(col);

        if( oldCols.isEmpty() )
            return;

        VarColumn colToRemove = oldCols.toArray(new VarColumn[0])[0];
        Map<String, String> oldColProps = new HashMap<String, String>();
        AstSet set = (AstSet)sprop.getValueNode();

        if( !colToRemove.getColumn().isEmpty() )
            oldColProps.put("name", colToRemove.getColumn());

        if( !colToRemove.getVariable().isEmpty() )
            oldColProps.put("variable", colToRemove.getVariable());

        SimpleNode elem = set.get(oldColProps);
        if( elem != null )
            removeAstNode(elem);
    }

    /**
     * Change table path in table property
     * @param oldPath
     * @param newPath
     * @param tableNode
     * @param model
     */
    public void changePath(DataElementPath newPath, Node tableNode, AstModel model)
    {
        Set<biouml.plugins.antimony.astparser_v2.Node> astNodes = getLinkedAstNode(tableNode);

        if( astNodes == null )
            return;

        AstSingleProperty sprop = null;
        for( biouml.plugins.antimony.astparser_v2.Node astNode : astNodes )
        {
            if( AntimonyUtility.isAstProperty(astNode) )
            {
                AstProperty property = (AstProperty)astNode;
                sprop = property.getSinglePropety("path");
            }
        }

        if( sprop != null )
            sprop.setPropertyValue(newPath.toString());


    }

}
