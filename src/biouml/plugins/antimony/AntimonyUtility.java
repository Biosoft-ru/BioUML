package biouml.plugins.antimony;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.antimony.astparser_v2.AstComment;
import biouml.plugins.antimony.astparser_v2.AstEOL;
import biouml.plugins.antimony.astparser_v2.AstEquation;
import biouml.plugins.antimony.astparser_v2.AstFunction;
import biouml.plugins.antimony.astparser_v2.AstModel;
import biouml.plugins.antimony.astparser_v2.AstProduct;
import biouml.plugins.antimony.astparser_v2.AstProperty;
import biouml.plugins.antimony.astparser_v2.AstReactant;
import biouml.plugins.antimony.astparser_v2.AstReactionTitle;
import biouml.plugins.antimony.astparser_v2.AstRelationshipType;
import biouml.plugins.antimony.astparser_v2.AstSemicolon;
import biouml.plugins.antimony.astparser_v2.AstSpecialFormula;
import biouml.plugins.antimony.astparser_v2.AstStart;
import biouml.plugins.antimony.astparser_v2.AstSubstanceOnly;
import biouml.plugins.antimony.astparser_v2.AstSymbol;
import biouml.plugins.antimony.astparser_v2.AstSymbolType;
import biouml.plugins.antimony.astparser_v2.AstUnit;
import biouml.plugins.antimony.astparser_v2.Node;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbml.SbmlDiagramType;
import biouml.standard.diagram.CompositeDiagramType;
import biouml.standard.diagram.MathDiagramType;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.Util;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Unit;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil;

public class AntimonyUtility
{
    public static boolean isAstSymbol(Node astNode)
    {
        return ( astNode instanceof AstSymbol );
    }

    public static boolean isAstModel(Node astNode)
    {
        return ( astNode instanceof AstModel );
    }

    public static boolean isAstSymbolType(Node astNode)
    {
        return ( astNode instanceof AstSymbolType );
    }

    public static boolean isInitAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.INIT.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isReactionAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.REACTION_DEFINITION.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }
    public static boolean isModifierAstSymbol(Node astNode)
    {
        if( !isAstSymbol(astNode) )
            return false;
        String type = ( (AstSymbol)astNode ).getDeclarationType();
        return AstSymbol.REACTION_ACTIVATOR.equals(type) || AstSymbol.REACTION_INHIBITOR.equals(type)
                || AstSymbol.REACTION_UNKNOWN_MODIFIER.equals(type);
    }

    public static boolean isSubdiagramAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.SUBDIAGRAM.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isSynchronizationsSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.SYNCHRONIZATIONS.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isEventAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.EVENT.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isDisplayNameAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.DISPLAY_NAME.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isEquationAstSymbol(Node astNode)
    {
        if( !isAstSymbol(astNode) )
            return false;
        String type = ( (AstSymbol)astNode ).getDeclarationType();
        return ( AstSymbol.RULE.equals(type) || AstSymbol.RATE.equals(type) || AstSymbol.INIT.equals(type)
                || AstSymbol.ALGEBRAIC.equals(type) );
    }

    public static boolean isReactionOrProduct(Node astNode)
    {
        return astNode instanceof AstReactant || astNode instanceof AstProduct;
    }

    public static boolean isEmpty(Node astNode)
    {
        for( int i = 0; i < astNode.jjtGetNumChildren(); i++ )
        {
            if( ! ( astNode.jjtGetChild(i) instanceof AstEOL ) && ! ( astNode.jjtGetChild(i) instanceof AstSemicolon )
                    && ! ( astNode.jjtGetChild(i) instanceof AstRelationshipType ) )
                return false;
        }
        return true;
    }

    public static String getAntimonyValidName(String name)
    {
        if( name == null )
            return null;
        String[] validName = TextUtil.split(name, '.');
        String antimonyName = validName[validName.length - 1];
        antimonyName = antimonyName.replaceAll("[-+()]", "_").replace("$", "");
        return antimonyName.split("[0-9]")[0].isEmpty() ? "_" + antimonyName : antimonyName;
    }

    public static String getAntimonyValidDiagramName(String name)
    {
        return name == null ? null : name.replaceAll("[-+()$.]", "_");
    }

    private static Collection<String> reservedNameCollection = Stream.of("after", "function", "assert", "else", "delete", "end")
            .collect(Collectors.toSet());

    public static Collection<String> getReservedNameCollection()
    {
        return reservedNameCollection;
    }

    public static boolean checkDiagramType(Diagram diagram)
    {
        DiagramType type = diagram.getType();
        return type instanceof SbgnDiagramType || type instanceof SbmlDiagramType || type instanceof MathDiagramType
                || type instanceof CompositeDiagramType;
    }

    public static List<biouml.model.Node> getCompartmentNodes(Diagram diagram)
    {
        return diagram.getRole(EModel.class).getVariableRoles().stream().map(VariableRole::getDiagramElement)
                .filter(biouml.model.Node.class::isInstance).map(n -> (biouml.model.Node)n)
                .filter(node -> node.getKernel() instanceof biouml.standard.type.Compartment).collect(Collectors.toList());
    }

    public static List<Variable> getParameters(Diagram diagram)
    {
        return diagram
                .getRole(EModel.class).getParameters().stream().filter(var -> ! ( var.getName().equals("time")
                        || var.getName().startsWith("unit") || Unit.getBaseUnitsList().contains(var.getName()) ))
                .collect(Collectors.toList());
    }

    public static List<ModelDefinition> getModelDefinitions(Compartment diagram)
    {
        return diagram.recursiveStream().select(ModelDefinition.class).toList();
    }

    public static List<DiagramElement> getPortConnectionEdges(Compartment diagram)
    {
        return diagram.recursiveStream().filter(de -> isPortConnectionEdge(de)).toList();
    }

    public static boolean isPortConnectionEdge(DiagramElement de)
    {
        return de instanceof Edge && de.getRole() instanceof Connection
                && ! ( Util.isBus( ( (Edge)de ).getInput()) || Util.isBus( ( (Edge)de ).getOutput()) );
    }

    public static List<SubDiagram> getSubdiagrams(Compartment diagram)
    {
        return diagram.recursiveStream().select(SubDiagram.class).toList();
    }

    public static List<biouml.model.Node> getSpecieNodes(Diagram diagram, boolean xml)
    {
        return diagram.getRole(EModel.class).getVariableRoles().stream().map(VariableRole::getDiagramElement)
                .filter(biouml.model.Node.class::isInstance).map(n -> (biouml.model.Node)n)
                .filter(node -> node.getKernel() instanceof Specie && !isGene(node, xml)).collect(Collectors.toList());
    }

    public static List<biouml.model.Node> getPhenotypes(Diagram diagram)
    {
        return diagram.stream().select(biouml.model.Node.class).filter(node -> SbgnUtil.isPhenotype(node)).toList();
    }

    public static List<biouml.model.Node> getPortNodes(Compartment diagram, boolean withSubdiagram)
    {
        List<biouml.model.Node> portNodes = new ArrayList<>();
        for( biouml.model.Node node : diagram.getNodes() )
        {
            if( node instanceof Compartment && ( withSubdiagram || ! ( node instanceof SubDiagram ) ) )
                portNodes.addAll(getPortNodes((Compartment)node, withSubdiagram));

            if( node.getKernel() instanceof Stub.ConnectionPort )
                portNodes.add(node);
        }
        return portNodes;
    }

    public static List<biouml.model.Node> getGeneNode(Compartment diagram, boolean xml)
    {
        return diagram.stream().select(biouml.model.Node.class).filter(node -> node.getKernel() instanceof Specie && isGene(node, xml))
                .toList(); //recursiveStream()
    }

    public static boolean isGene(biouml.model.Node node, boolean xml)
    {
        if( xml )
            return AntimonyConstants.SBGN_GENE.equals(node.getKernel().getType());
        else
            return AntimonyConstants.STANDARD_GENE.equals(node.getKernel().getType());
    }

    public static String generateName(Diagram diagram, DecimalFormat format)
    {
        // put all compartments, species and reaction names
        HashMap<String, DiagramElement> map = new HashMap<>();
        fillNameMap(diagram, map);

        int n = 1;
        String name;
        DecimalFormat formatter = format;
        while( true )
        {
            name = formatter.format(n++);
            if( !map.containsKey(name) )
            {
                break;
            }
        }

        return name;
    }

    private static void fillNameMap(Compartment compartment, HashMap<String, DiagramElement> map)
    {
        map.put(compartment.getName(), compartment);

        for( DiagramElement de : compartment )
        {
            if( de instanceof Compartment )
                fillNameMap((Compartment)de, map);
            else
                map.put(de.getName(), de);
        }
    }

    public static boolean isIgnoreInText(Node currentNode)
    {
        return currentNode instanceof AstEquation || currentNode instanceof AstReactant || currentNode instanceof AstProduct
                || currentNode instanceof AstReactionTitle || currentNode instanceof AstStart || currentNode instanceof AstSpecialFormula;
    }

    public static String getAntimonyAttribute(Diagram diagram, String attributeName)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty(attributeName);
        return dp == null ? null : dp.getValue().toString();
    }

    public static void setAntimonyAttribute(Diagram diagram, String value, String attributeName)
    {
        if( value == null || value.equals(getAntimonyAttribute(diagram, attributeName)) )
            return;
        DynamicProperty dp = new DynamicProperty(attributeName, String.class, value);
        diagram.getAttributes().add(dp);
        DPSUtils.makeTransient(dp);
        dp.setReadOnly(true);
    }

    public static boolean isVariableRole(String type)
    {
        return type.equals(AstSymbolType.GENE) || type.equals(AstSymbolType.SPECIES) || type.equals(AstSymbolType.COMPARTMENT);
    }

    public static boolean haveSameAccessType(PortProperties properties, biouml.model.Node port)
    {
        return ( properties.isPrivatePort() == Util.isPrivatePort(port) ) && ( properties.isPublicPort() == Util.isPublicPort(port) )
                && ( properties.isPropagatedPort() == Util.isPropagatedPort(port) );
    }

    public static Variable findVariable(String nameVariable, EModel role)
    {
        DataCollection<Variable> variables = role.getVariables();
        for( Variable var : variables )
        {
            String name = AntimonyUtility.getAntimonyValidName(var.getName());
            if( name.equals(nameVariable) )
                return var;
        }
        return null;
    }

    public static String getOppositePortType(biouml.model.Node port)
    {
        if( Util.isInputPort(port) )
            return "output";
        if( Util.isOutputPort(port) )
            return "input";
        return "contact";
    }

    public static String getOppositePortType(String type)
    {
        if( type.equals("output") )
            return "input";
        if( type.equals("input") )
            return "output";
        if( type.equals("contact") )
            return "contact";
        return null;
    }

    public static boolean isHasUnitAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.SET_UNIT.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isCompartment(biouml.model.Node node)
    {
        return ( node.getKernel() instanceof biouml.standard.type.Compartment );
    }

    public static boolean isConstraintAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.CONSTRAINT.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isEmptyCompartment(biouml.model.Node node)
    {
        if( node instanceof Compartment && ( (Compartment)node ).getNodes().length == 0 )
            return true;
        return false;
    }

    public static List<biouml.model.Node> getPhenotypeNodes(Diagram diagram)
    {
        return diagram.recursiveStream().select(biouml.model.Node.class).filter(node -> SbgnUtil.isPhenotype(node)).toList();
    }

    public static List<biouml.model.Node> getLogicalNodes(Diagram diagram)
    {
        return diagram.stream().select(biouml.model.Node.class).filter(node -> SbgnUtil.isLogical(node)).toList();
    }


    public static List<Edge> getModifierEdges(Compartment diagram)
    {
        return diagram.recursiveStream().select(Edge.class).filter(
                edge -> ( isModifierEdge(edge) && ! ( SbgnUtil.isLogical(edge.getInput()) || SbgnUtil.isLogical(edge.getOutput()) ) ))
                .toList();
    }

    private static boolean isModifierEdge(Edge edge)
    {
        return edge.getKernel() instanceof SpecieReference
                && SpecieReference.MODIFIER.equals( ( (SpecieReference)edge.getKernel() ).getRole());
    }

    public static boolean isModifierAstProperty(Node astNode)
    {
        return isAstProperty(astNode);// && AntimonyConstants.SBGN_EDGE_TYPE.equals( ( (AstProperty)astNode ).getPropertyName());
    }

    public static boolean isAstProperty(Node astNode)
    {
        return ( astNode instanceof AstProperty );
    }

    /**
     * Return edges connecting equivalence operator and supertype entity.
     */

    public static List<Edge> getEquivalenceEdges(Compartment diagram)
    {
        return diagram.recursiveStream().select(Edge.class).filter(edge -> SbgnUtil.isEquivalenceNode(edge.getInput())).toList();
    }

    public static boolean isSubtypeAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.SUBTYPE.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isDatabaseReferenceAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.DATABASE_REFERENCE.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isClone(biouml.model.Node de)
    {
        return de.getAttributes().getProperty(SBGNPropertyConstants.SBGN_CLONE_MARKER) != null;
    }

    static String getBlockType(Node astNode)
    {
        if( astNode instanceof AstUnit )
            return AntimonyConstants.BLOCK_UNITS;
        else if( astNode instanceof AstFunction )
            return AntimonyConstants.BLOCK_FUNCTIONS;
        else if( astNode instanceof AstProperty )
            return AntimonyConstants.BLOCK_PROPERTIES;
        else if( isSubtypeAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_SUBTYPES;
        else if( isInitAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_INITIALIZATIONS;
        else if( isHasUnitAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_UNIT_ASSIGNMENTS;
        else if( isReactionAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_REACTIONS;
        else if( isEquationAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_EQUATIONS;
        else if( isEventAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_EVENTS;
        else if( isConstraintAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_CONSTRAINTS;
        else if( isDisplayNameAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_TITLES;
        else if( isDatabaseReferenceAstSymbol(astNode) )
            return AntimonyConstants.BLOCK_CV_TERMS;
        else if( isSynchronizationsSymbol(astNode) )
            return AntimonyConstants.BLOCK_CONNECTIONS;
        else if (astNode instanceof AstComment)
            return ( (AstComment)astNode ).getText();
        return null;
    }

    static boolean isFunctionReturnAssignmentAstSymbol(Node astNode)
    {
        return isAstSymbol(astNode) && AstSymbol.FUNCTION_RETURN_ASSIGNMENT.equals( ( (AstSymbol)astNode ).getDeclarationType());
    }

    public static boolean isAstSubstanceOnly(Node currentNode)
    {
        return currentNode instanceof AstSubstanceOnly;
    }


}
