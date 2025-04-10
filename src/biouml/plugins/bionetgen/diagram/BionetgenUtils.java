package biouml.plugins.bionetgen.diagram;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.bionetgen.bnglparser.BNGExpression;
import biouml.plugins.bionetgen.bnglparser.BNGSpecies;
import biouml.plugins.bionetgen.bnglparser.BionetgenParser;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import one.util.streamex.StreamEx;
import ru.biosoft.util.TextUtil2;

public class BionetgenUtils
{
    public static boolean checkDiagramType(Diagram diagram)
    {
        DiagramType type = diagram.getType();
        if( type == null )
            return false;
        return type instanceof BionetgenDiagramType || getBionetgenAttr( diagram ) != null;
    }

    public static boolean isEquation(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_EQUATION.equals( de.getKernel().getType() );
    }

    public static boolean isMolecule(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_MOLECULE.equals( de.getKernel().getType() );
    }

    public static boolean isMoleculeComponent(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_MOLECULE_COMPONENT.equals( de.getKernel().getType() );
    }

    public static boolean isMoleculeType(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_MOLECULETYPE.equals( de.getKernel().getType() );
    }

    public static boolean isOtherType(DiagramElement de)
    {
        DynamicProperty dp = de.getAttributes().getProperty( BionetgenConstants.IS_SEED_SPECIES_ATTR );
        return dp == null || !Boolean.valueOf( dp.getValue().toString() );
    }

    public static boolean isStartType(DiagramElement de)
    {
        return Boolean.valueOf( de.getAttributes().getValueAsString( BionetgenConstants.IS_SEED_SPECIES_ATTR ) );
    }

    public static boolean isSpecies(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_SPECIES.equals( de.getKernel().getType() );
    }

    public static boolean isObservable(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_OBSERVABLE.equals( de.getKernel().getType() );
    }

    public static boolean isBngEdge(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_EDGE.equals( de.getKernel().getType() );
    }

    public static boolean isReaction(DiagramElement de)
    {
        return de.getKernel() != null && BionetgenConstants.TYPE_REACTION.equals( de.getKernel().getType() );
    }

    public static boolean isValidRateLawType(String type)
    {
        return BionetgenConstants.MM.equals(type) || BionetgenConstants.SATURATION.equals(type) || BionetgenConstants.DEFAULT.equals(type);
    }

    private static BionetgenParser parser = new BionetgenParser();
    public static boolean isCorrectRateFormula(String formula)
    {
        return formula != null && ( isCorrectExpression( formula ) || isSatOrMM( formula ) );
    }
    public static boolean isCorrectExpression(String expression)
    {
        parser.parseFormula(expression);
        return parser.getStatus() == BionetgenParser.STATUS_OK;
    }
    private static boolean isSatOrMM(String rate)
    {
        String[] parts = TextUtil2.split(rate, '(');
        if( parts.length != 2 || ( !"MM".equals(parts[0]) && !"Sat".equals(parts[0]) ) )
            return false;
        parts = TextUtil2.split(parts[1], ')');
        if( parts.length != 2 || !parts[1].isEmpty() )
            return false;
        parts = TextUtil2.split(parts[0], ',');
        if( parts.length != 2 )
            return false;
        return isCorrectExpression(parts[0]) && isCorrectExpression(parts[1]);
    }

    public static @CheckForNull BNGSpecies generateSpecies(String graph)
    {
        BNGSpecies species = parser.parseSpecies(graph);
        if( parser.getStatus() != BionetgenParser.STATUS_OK )
            return null;
        return species;
    }

    /**
     * Creates math expression from given string
     * @param formula
     * @return
     * @throws Exception
     */
    public static BNGExpression generateExpression(String formula) throws Exception
    {
        BNGExpression expr = parser.parseFormula(formula);
        if( parser.getStatus() != BionetgenParser.STATUS_OK )
            throw new BionetgenException( "Improper math expression's syntax: '" + formula + "'." );
        return expr;
    }

    public static String generateNodeName(String name)
    {
        return name.replace('.', '_');
    }

    /**
     * Generates unique name. Unlike in <code>DefaultSemanticController.generateUniqueNodeName</code> returned
     * name can be not unique in diagram, but it will be unique in origin species
     * @param compartment
     * @param baseName
     * @return
     */
    public static String generateUniqueName(Compartment compartment, String baseName)
    {
        while( compartment.getOrigin() instanceof Compartment && !isSpecies(compartment) )
            compartment = (Compartment)compartment.getOrigin();

        if( DefaultSemanticController.isNodeNameUnique(compartment, baseName) )
            return baseName;

        String id = baseName + "_";
        int n = 1;

        while( !DefaultSemanticController.isNodeNameUnique(compartment, id + n) )
            n++;

        return id + n;
    }

    public static void rebuildSpecies(Compartment species) throws Exception
    {
        if( !isSpecies(species) )
            return;

        List<String> names = new ArrayList<>();
        for( DiagramElement de : species )
        {
            names.add(de.getName());
        }
        for( String name : names )
        {
            species.remove(name);
        }
        generateGraphComplexContent(species,
                new BionetgenSpeciesGraph(species.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR)));
    }

    public static void generateGraphComplexContent(Compartment complex, BionetgenSpeciesGraph graph) throws Exception
    {
        HashMap<MoleculeComponent, Node> molToNode = new HashMap<>();
        for( BionetgenMolecule bionetgenMolecule : graph.getMoleculesList() )
        {
            Compartment molecule = new Compartment( complex, createKernelByType( BionetgenConstants.TYPE_MOLECULE,
                    generateUniqueName( complex, bionetgenMolecule.getName() ) ) );
            molecule.setTitle(bionetgenMolecule.getName());
            molecule.setShapeSize(new Dimension(80, 50));
            molecule.setLocation((int)complex.getLocation().getX() + 25, (int)complex.getLocation().getY() + 15);
            molecule.getAttributes().add(new DynamicProperty(BionetgenConstants.MOLECULE_ATTR, String.class, bionetgenMolecule.toString()));

            complex.put(molecule);

            generateMoleculeContent(molecule, bionetgenMolecule, molToNode);
        }

        for( MoleculeComponent molComp : graph.getAdjacency().keySet() )
        {
            Set<MoleculeComponent> set = graph.getAdjacency().get(molComp);
            for( MoleculeComponent otherMolComp : set )
            {
                Node in = molToNode.get(molComp);
                Node out = molToNode.get(otherMolComp);
                boolean alreadyConnected = in.edges().anyMatch( e -> e.nodes().has( in ) )
                        || out.edges().anyMatch( e -> e.nodes().has( out ) );

                if( alreadyConnected )
                    continue;

                Edge newEdge = new Edge( complex, createKernelByType( BionetgenConstants.TYPE_EDGE,
                        generateUniqueName( complex, BionetgenConstants.EDGE_NAME ) ), in, out );
                in.addEdge(newEdge);
                out.addEdge(newEdge);
                complex.put(newEdge);
            }
        }
    }

    public static void rebuildMolecule(Compartment molecule) throws Exception
    {
        if( !isMolecule(molecule) )
            return;

        List<String> names = new ArrayList<>();
        for( Node node : molecule.getNodes() )
        {
            names.add(node.getName());
            for( Edge edge : node.getEdges() )
            {
                ( (Compartment)molecule.getParent() ).remove(edge.getName());
            }
        }
        for( String name : names )
        {
            molecule.remove(name);
        }
        BionetgenMolecule mol = new BionetgenMolecule(new BionetgenSpeciesGraph(""), molecule.getAttributes().getValueAsString(
                BionetgenConstants.MOLECULE_ATTR));
        generateMoleculeContent(molecule, mol);
    }

    public static void generateMoleculeContent(Compartment molecule, BionetgenMolecule bionetgenMolecule) throws Exception
    {
        generateMoleculeContent(molecule, bionetgenMolecule, null);
    }

    private static void generateMoleculeContent(Compartment molecule, BionetgenMolecule bionetgenMolecule,
            HashMap<MoleculeComponent, Node> molToNode) throws Exception
    {
        for( MoleculeComponent molComp : bionetgenMolecule.getMoleculeComponents() )
        {
            Node molCompNode = new Node( molecule, createKernelByType( BionetgenConstants.TYPE_MOLECULE_COMPONENT,
                    generateUniqueName( molecule, molComp.getName() ) ) );
            molCompNode.setLocation( (int)molecule.getLocation().getX() - 10, (int)molecule.getLocation().getY() - 10 );
            if( molToNode != null )
                molToNode.put(molComp, molCompNode);
            molCompNode.setTitle(molComp.toString());
            molecule.put(molCompNode);
        }
    }

    public static SpecieReference addReference(Reaction reaction, String specie, String role, boolean forTemplate) throws Exception
    {
        return addReference( reaction, specie, role, forTemplate, "" );
    }
    protected static SpecieReference addReference(Reaction reaction, String specie, String role, boolean forTemplate, @Nonnull String suffix)
            throws Exception
    {
        String id;
        if( forTemplate )
            id = reaction.getName() + "#" + reaction.getSize() + ": " + specie + " as " + role;
        else
            id = SpecieReference.generateSpecieReferenceName(reaction.getName(), specie, role);
        SpecieReference ref = new SpecieReference( reaction, id, role );
        ref.setTitle( "" );
        ref.setSpecie( specie );
        ref.setStoichiometry( "1" );
        Integer number = null;
        SpecieReference oldRef = reaction.put( ref );
        if( oldRef != null )
        {
            ref.setStoichiometry( String.valueOf( Integer.parseInt( oldRef.getStoichiometry() ) + 1 ) );
            number = Integer.valueOf( oldRef.getAttributes().getValueAsString( BionetgenConstants.REACTANT_NUMBER_ATTR ) );
        }
        number = number == null ? (Integer)reaction.getSize() : number;
        ref.getAttributes().add( new DynamicProperty( BionetgenConstants.REACTANT_NUMBER_ATTR, Integer.class, number ) );
        return ref;
    }

    /**
     * Creates reaction suitable to be used by ReactionTemplate using given reaction node.
     * If given reverse is true, creates reversed reaction.
     * @param reactionNode
     * @param reverse
     * @return
     * @throws Exception
     */
    public static Reaction prepareReaction(Node reactionNode, boolean reverse) throws Exception
    {
        Reaction reaction;
        if( reverse )
            reaction = new Reaction(null, "rev" + reactionNode.getName());
        else
            reaction = new Reaction(null, reactionNode.getName());

        DynamicPropertySet dps = reaction.getAttributes();
        for( DynamicProperty attr : reactionNode.getAttributes() )
        {
            dps.add(new DynamicProperty(attr.getName(), attr.getType(), attr.getValue()));
        }
        if( reverse )
        {
            dps.add(new DynamicProperty(BionetgenConstants.FORWARD_RATE_ATTR, String.class, dps
                    .getValueAsString(BionetgenConstants.BACKWARD_RATE_ATTR)));
        }

        List<Node> reactants = new ArrayList<>();
        List<Node> products = new ArrayList<>();
        List<Edge> edges = StreamEx.of( reactionNode.getEdges() ).sorted( Comparator.comparingInt(e ->
            (Integer)e.getKernel().getAttributes().getValue( BionetgenConstants.REACTANT_NUMBER_ATTR )
        )).toList();
        for( Edge edge : edges )
        {
            if( edge.getOutput().equals(reactionNode) )
                reactants.add(edge.getInput());
            else if( edge.getInput().equals(reactionNode) )
                products.add(edge.getOutput());
        }
        if( reverse )
            addReferences(reaction, products, reactants);
        else
            addReferences(reaction, reactants, products);
        return reaction;
    }

    private static void addReferences(Reaction reaction, List<Node> reactants, List<Node> products) throws Exception
    {
        for( Node reactant : reactants )
        {
            addReference(reaction, reactant.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR), SpecieReference.REACTANT, true);
        }
        for( Node product : products )
        {
            addReference(reaction, product.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR), SpecieReference.PRODUCT, true);
        }
    }

    /**
     * Adds given <b>text</b> as BioNetGen attribute to given <b>diagram</b>
     * @param diagram
     * @param text
     */
    public static void setBionetgenAttr(Diagram diagram, String text)
    {
        DynamicProperty dp = new DynamicProperty(Bionetgen.BIONETGEN_ATTR, String.class, text);
        dp.setReadOnly(true);
        diagram.getAttributes().add(dp);
    }

    /**
     * 
     * @param diagram
     * @return value of BioNetGen attribute of given <b>diagram</b> if it exists and <code>null</code> otherwise
     */
    public static String getBionetgenAttr(Diagram diagram)
    {
        return diagram.getAttributes().getValueAsString(Bionetgen.BIONETGEN_ATTR);
    }

    public static Stub createKernelByType(String type, String name)
    {
        return new Stub( null, name, type );
    }

    @SuppressWarnings ( "serial" )
    public static class BionetgenException extends Exception
    {
        public BionetgenException(String string)
        {
            super( string );
        }
    }
}
