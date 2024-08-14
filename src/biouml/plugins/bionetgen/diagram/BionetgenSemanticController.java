package biouml.plugins.bionetgen.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.PropertiesDialog;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.CreateEdgeDialog;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PathwaySimulationSemanticController;
import biouml.standard.diagram.ReactionInitialProperties;
import biouml.standard.diagram.ReactionPane.CreateReactionException;
import biouml.standard.type.Base;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 *
 */
public class BionetgenSemanticController extends PathwaySimulationSemanticController
{
    @Override
    public DiagramElementGroup createInstance(@Nonnull Compartment parent, Object type, Point point, ViewEditorPane viewEditor)
    {
        try
        {
            if( type instanceof String )
            {
                Object properties = getPropertiesByType( parent, type, point );

                if( BionetgenConstants.TYPE_EDGE.equals( type ) && properties instanceof DynamicPropertySet )
                {
                    CreateEdgeDialog dialog = CreateEdgeDialog.getSimpleEdgeDialog( Module.optModule( parent ), point, viewEditor,
                            getNameByType( (String)type ), BionetgenConstants.TYPE_EDGE, (DynamicPropertySet)properties );
                    dialog.setVisible( true );
                    return null;
                }
                else
                {
                    PropertiesDialog dialog = new PropertiesDialog( Application.getApplicationFrame(), "New element", properties );
                    if( dialog.doModal() )
                    {
                        if( properties instanceof InitialElementProperties )
                            return ( (InitialElementProperties)properties ).createElements( parent, point, viewEditor );
                        return null;
                    }
                }
            }
            else if( type instanceof Class )
                return super.createInstance( parent, type, point, viewEditor );
        }
        catch( Throwable t )
        {
            throw ExceptionRegistry.translateException( t );
        }
        return null;
    }

    @Override
    public boolean canAccept(Compartment parent, DiagramElement de)
    {
        if( parent == null || de == null )
            return false;

        if( BionetgenUtils.isSpecies( parent ) && parent.equals( de.getParent() ) )
        {
            if( BionetgenUtils.isMolecule( de ) )
                return true;
            else if( BionetgenUtils.isBngEdge( de ) )
                return ( (Edge)de ).nodes().allMatch( BionetgenUtils::isMoleculeComponent );
            return false;
        }
        else if( BionetgenUtils.isMolecule( parent ) )
            return BionetgenUtils.isMoleculeComponent( de );
        else if( BionetgenUtils.isObservable( parent ) || BionetgenUtils.isMoleculeType( parent ) )
            return false;
        else if( BionetgenUtils.isMoleculeComponent( de ) || BionetgenUtils.isBngEdge( de ) || BionetgenUtils.isMolecule( de ) )
            return false;

        return true;
    }

    @Override
    public boolean isResizable(DiagramElement de)
    {
        return BionetgenUtils.isSpecies( de );
    }

    @Override
    public Object getPropertiesByType(Compartment compartment, Object type, Point point)
    {
        if( type instanceof Class && Reaction.class.isAssignableFrom( (Class<?>)type ) )
            return new BionetgenReactionProperties();
        else if( ! ( type instanceof String ) )
            return super.getPropertiesByType( compartment, type, point );

        if( BionetgenConstants.TYPE_EDGE.equals( type ) )
            return getDPSByType( (String)type );
        return new BionetgenElementProperties( (String)type, generateName( compartment, "", (String)type ) );
    }

    private String generateName(Compartment compartment, String name, String type)
    {
        if( name == null || name.isEmpty() )
            name = getNameByType( type );
        if( BionetgenConstants.TYPE_MOLECULE.equals( type ) || BionetgenConstants.TYPE_MOLECULE_COMPONENT.equals( type )
                || BionetgenConstants.TYPE_EDGE.equals( type ) )
            return BionetgenUtils.generateUniqueName( compartment, name );

        return generateUniqueNodeName( Diagram.getDiagram( compartment ), name );
    }

    private static String getNameByType(String type)
    {
        return typeToName.containsKey( type ) ? typeToName.get( type ) : type;
    }
    @SuppressWarnings ( "serial" )
    private static final Map<String, String> typeToName = new HashMap<String, String>()
    {
        {
            put( BionetgenConstants.TYPE_SPECIES, "Species" );
            put( BionetgenConstants.TYPE_MOLECULE, "Molecule" );
            put( BionetgenConstants.TYPE_MOLECULE_COMPONENT, "component" );
            put( BionetgenConstants.TYPE_MOLECULETYPE, BionetgenConstants.MOLECULE_TYPE_NAME );
            put( BionetgenConstants.TYPE_OBSERVABLE, "observable" );
            put( BionetgenConstants.TYPE_REACTION, "reaction" );
            put( BionetgenConstants.TYPE_EQUATION, BionetgenConstants.EQUATION_NAME );
            put( BionetgenConstants.TYPE_EDGE, BionetgenConstants.EDGE_NAME );
        }
    };

    private void generateMissedAttributes(DynamicPropertySet target, String stubType)
    {
        DynamicPropertySet attributes = getDPSByType( stubType );
        for( DynamicProperty dp : attributes )
        {
            if( target.getProperty( dp.getName() ) != null )
                continue;
            target.add( dp );
        }
    }
    private void setAttributes(Node node, DynamicPropertySet dps)
    {
        for( DynamicProperty dp : dps )
            node.getAttributes().add( dp );
    }

    /**
     * Creates new node (except reaction node)
     * @return created node
     */
    public Node createNodeInstance(@Nonnull Compartment parent, String type, String name, Point location, DynamicPropertySet dps)
    {
        try
        {
            if( dps == null )
                dps = new DynamicPropertySetAsMap();
            generateMissedAttributes( dps, type );
            Base kernel = BionetgenUtils.createKernelByType( type, generateName( parent, name, type ) );
            switch( type )
            {
                case BionetgenConstants.TYPE_SPECIES:
                    return createSpecies( parent, kernel, location, dps );
                case BionetgenConstants.TYPE_MOLECULE:
                    return createMolecule( parent, kernel, location, dps );
                case BionetgenConstants.TYPE_MOLECULE_COMPONENT:
                    return createMoleculeComponent( parent, kernel, name, location, dps );
                case BionetgenConstants.TYPE_MOLECULETYPE:
                    return createMoleculeType( parent, kernel, location, dps );
                case BionetgenConstants.TYPE_OBSERVABLE:
                    return createObservable( parent, kernel, location, dps );
                case BionetgenConstants.TYPE_EQUATION:
                    return createEquation( parent, kernel, location, dps );
                default:
                    throw new IllegalArgumentException( "Cannot create element of unsopported type: '" + type + "'." );
            }
        }
        catch( Exception t )
        {
            throw ExceptionRegistry.translateException( t );
        }
    }

    private Compartment createSpecies(Compartment parent, Base kernel, Point location, DynamicPropertySet dps) throws Exception
    {
        Compartment species = new Compartment( parent, kernel );
        setAttributes( species, dps );
        species.setShapeSize( new Dimension( 130, 80 ) );
        species.setRole( new VariableRole( species ) );
        species.setLocation( location );

        String graphStr = species.getAttributes().getValueAsString( BionetgenConstants.GRAPH_ATTR );
        if( graphStr == null || graphStr.isEmpty() )
            return species;

        boolean isEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( false );
        BionetgenUtils.generateGraphComplexContent( species, new BionetgenSpeciesGraph( graphStr ) );
        parent.setNotificationEnabled( isEnabled );
        return species;
    }
    private Compartment createMolecule(Compartment parent, Base kernel, Point location, DynamicPropertySet dps) throws Exception
    {
        Compartment molecule = new Compartment( parent, kernel );
        setAttributes( molecule, dps );
        molecule.setLocation( location );
        molecule.setShapeSize( new Dimension( 80, 50 ) );

        String moleculeStr = molecule.getAttributes().getValueAsString( BionetgenConstants.MOLECULE_ATTR );
        if( moleculeStr != null && !moleculeStr.isEmpty() )
            BionetgenUtils.generateMoleculeContent( molecule, new BionetgenMolecule( new BionetgenSpeciesGraph( "" ), moleculeStr ) );
        return molecule;
    }
    private Node createMoleculeComponent(Compartment parent, Base kernel, String title, Point location, DynamicPropertySet dps)
    {
        Node molComp = new Node( parent, kernel );
        molComp.setLocation( location );
        molComp.setTitle( title );
        setAttributes( molComp, dps );
        return molComp;
    }
    private Node createMoleculeType(Compartment parent, Base kernel, Point location, DynamicPropertySet dps)
    {
        Node molType = new Node( parent, kernel );
        setAttributes( molType, dps );
        molType.setLocation( location );
        return molType;
    }
    private Compartment createObservable(Compartment parent, Base kernel, Point location, DynamicPropertySet dps)
    {
        Compartment observable = new Compartment( parent, kernel );
        observable.setLocation( location );
        observable.setShapeSize( new Dimension( 70, 70 ) );
        setAttributes( observable, dps );
        return observable;
    }
    private Node createEquation(Compartment parent, Base kernel, Point location, DynamicPropertySet dps) throws Exception
    {
        Node eqNode = new Node( parent, kernel );
        eqNode.setLocation( location );
        setAttributes( eqNode, dps );

        boolean notificationEnabled = parent.isNotificationEnabled();
        parent.setNotificationEnabled( false );
        Equation eq = new Equation( eqNode, Equation.TYPE_INITIAL_ASSIGNMENT, "unknown", "0" );
        eqNode.setRole( eq );
        parent.setNotificationEnabled( notificationEnabled );
        return eqNode;
    }

    /**
     * Creates reaction and all necessary edges.
     *
     * @return list which contains reaction node and all its edges
     * @throws Exception
     */
    public List<DiagramElement> createReactionElements(@Nonnull Compartment parent, String name, String formula,
            List<SpecieReference> components, Point location) throws Exception
    {
        List<DiagramElement> result = new ArrayList<>();

        Diagram diagram = Diagram.getDiagram( parent );
        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled( false );

        Reaction reaction = new Reaction( null, generateName( parent, name, BionetgenConstants.TYPE_REACTION ) );
        reaction.setTitle( DiagramUtility.generateReactionTitle( components ) );
        reaction.setKineticLaw( new KineticLaw() );

        Node reactionNode = new Node( parent, reaction );
        if( !canAccept( parent, reactionNode ) )
            throw new CreateReactionException( "Unacceptable compartment for reaction: " + parent.getName() + "." );
        reaction.setParent( reactionNode );
        result.add( reactionNode );
        reactionNode.setLocation( location );

        List<Edge> edges = new ArrayList<>();
        for( SpecieReference prototype : components )
            edges.addAll( createReactionEdges( reactionNode, reaction, diagram, prototype ) );

        setAttributes( reactionNode, getDPSByType( BionetgenConstants.TYPE_REACTION ) );
        DiagramUtility.generateRoles( diagram, reactionNode );

        DynamicPropertySet dps = reactionNode.getAttributes();
        if( BionetgenUtils.isCorrectRateFormula( formula ) )
        {
            dps.setValue( BionetgenConstants.FORWARD_RATE_ATTR, formula );
            if( formula.startsWith( "MM" ) )
            {
                dps.setValue( BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.MM );
                dps.setValue( BionetgenConstants.REVERSIBLE_ATTR, false );
            }
            else if( formula.startsWith( "Sat" ) )
            {
                dps.setValue( BionetgenConstants.RATE_LAW_TYPE_ATTR, BionetgenConstants.SATURATION );
                dps.setValue( BionetgenConstants.REVERSIBLE_ATTR, false );
            }
            else
            {
                boolean isReversible = reaction.isReversible();
                dps.setValue( BionetgenConstants.REVERSIBLE_ATTR, isReversible );
                String backwardRate = dps.getValueAsString( BionetgenConstants.BACKWARD_RATE_ATTR );
                if( isReversible && ( backwardRate == null || backwardRate.isEmpty() ) )
                    dps.add( new DynamicProperty( BionetgenConstants.BACKWARD_RATE_ATTR, String.class, formula ) );
            }
        }
        else
        {
            dps.setValue( BionetgenConstants.FORWARD_RATE_ATTR, "0.0" );
            dps.setValue( BionetgenConstants.REVERSIBLE_ATTR, false );
            if( formula == null || !formula.isEmpty() )
                log.warning( "Invalid formula: '" + formula + "' in '" + name + "' will be replaced by zero." );
        }

        if( notificationEnabled )
            diagram.setNotificationEnabled( true );

        result.addAll( edges );
        return result;
    }

    private List<Edge> createReactionEdges(@Nonnull Node reactionNode, Reaction reaction, Diagram diagram, SpecieReference prototype)
            throws Exception
    {
        List<Edge> result = new ArrayList<>();

        Node node = diagram.findNode( DiagramUtility.toDiagramPath( prototype.getName() ) );
        if( node == null )
            node = diagram.findNode( DiagramUtility.toDiagramPath( prototype.getSpecie() ) );

        String id = reaction.getName() + ": " + node.getKernel().getName() + " as " + prototype.getRole();
        SpecieReference real = prototype.clone( reaction, id );
        real.setTitle( node.getKernel().getName() + " as " + prototype.getRole() );
        real.setSpecie( node.getCompleteNameInDiagram() );
        real.getAttributes().add( new DynamicProperty( BionetgenConstants.REACTANT_NUMBER_ATTR, Integer.class, reaction.getSize() ) );
        reaction.put( real );

        Edge edge = real.isProduct() ? new Edge( real, reactionNode, node ) : new Edge( real, node, reactionNode );
        reactionNode.addEdge( edge );
        result.add( edge );

        int stoichiometry = Integer.parseInt( real.getStoichiometry() );
        if( stoichiometry >= 2 )
        {
            for( int i = 1; i < stoichiometry; i++ )
            {
                SpecieReference ref = BionetgenUtils.addReference( reaction, real.getSpecie(), real.getRole(), false, " #" + i );
                ref.getAttributes().add( new DynamicProperty( BionetgenConstants.REACTANT_NUMBER_ATTR, Integer.class, reaction.getSize() ) );
                reaction.put( ref );

                Edge newEdge = ref.isProduct() ? new Edge( ref, reactionNode, node ) : new Edge( ref, node, reactionNode );
                reactionNode.addEdge( newEdge );
                result.add( newEdge );
            }
            real.setStoichiometry( "1" );
        }
        return result;
    }

    public static DynamicPropertySet getDPSByType(String type)
    {
        DynamicPropertySet dps = new DynamicPropertySetAsMap();

        switch( type )
        {
            case BionetgenConstants.TYPE_SPECIES:
                dps.add( new DynamicProperty( BionetgenConstants.GRAPH_ATTR, BionetgenConstants.GRAPH_ATTR, "bionetgen species graph",
                        String.class, "" ) );
                dps.add( new DynamicProperty( BionetgenConstants.IS_SEED_SPECIES_PD, Boolean.class, false ) );
                dps.add( new DynamicProperty( BionetgenConstants.LABEL_ATTR, String.class, "" ) );
                break;

            case BionetgenConstants.TYPE_MOLECULE:
                dps.add( new DynamicProperty( BionetgenConstants.MOLECULE_ATTR, BionetgenConstants.MOLECULE_ATTR, "bionetgen molecule",
                        String.class, "" ) );
                break;

            case BionetgenConstants.TYPE_MOLECULETYPE:
                dps.add( new DynamicProperty( BionetgenConstants.MOLECULE_TYPE_ATTR, BionetgenConstants.MOLECULE_TYPE_ATTR,
                        "bionetgen molecule type", String.class, "" ) );
                dps.add( new DynamicProperty( BionetgenConstants.LABEL_ATTR, String.class, "" ) );
                break;

            case BionetgenConstants.TYPE_OBSERVABLE:
                dps.add( new DynamicProperty( BionetgenConstants.CONTENT_ATTR, BionetgenConstants.CONTENT_ATTR, "Observable content",
                        String[].class, new String[] {} ) );
                dps.add( new DynamicProperty( BionetgenConstants.MATCH_ONCE_ATTR, Boolean.class, false ) );
                dps.add( new DynamicProperty( BionetgenConstants.LABEL_ATTR, String.class, "" ) );
                break;

            case BionetgenConstants.TYPE_REACTION:
                dps.add( new DynamicProperty( BionetgenConstants.FORWARD_RATE_ATTR, BionetgenConstants.FORWARD_RATE_ATTR,
                        "forward rate of reaction", String.class, "0" ) );
                dps.add( new DynamicProperty( BionetgenConstants.BACKWARD_RATE_ATTR, BionetgenConstants.BACKWARD_RATE_ATTR,
                        "backward rate of reaction (can be empty)", String.class, "" ) );
                dps.add( new DynamicProperty( BionetgenConstants.RATE_LAW_TYPE_PD, String.class, BionetgenConstants.DEFAULT ) );
                dps.add( new DynamicProperty( BionetgenConstants.REVERSIBLE_ATTR, BionetgenConstants.REVERSIBLE_ATTR,
                        "reversibility of reaction flag", Boolean.class, false ) );
                dps.add( new DynamicProperty( BionetgenConstants.ADDITION_ATTR, String[].class, new String[] {} ) );
                dps.add( new DynamicProperty( BionetgenConstants.LABEL_ATTR, String.class, "" ) );
                break;

            case BionetgenConstants.TYPE_EQUATION:
                break;

            case BionetgenConstants.TYPE_MOLECULE_COMPONENT:
                break;

            default:
                break;
        }

        return dps;
    }

    @PropertyName ( "BioNetGen element" )
    public class BionetgenElementProperties implements InitialElementProperties
    {
        private final String type;
        private String name = "";
        private DynamicPropertySet properties;

        public BionetgenElementProperties(String type, String name)
        {
            this.type = type;
            this.name = name;
            this.properties = getDPSByType( type );
        }

        @Override
        public DiagramElementGroup createElements(Compartment compartment, Point location, ViewEditorPane viewPane) throws Exception
        {
            Node node = createNodeInstance( compartment, type, name, location, properties );
            node.setShapeSize( new Dimension( 0, 0 ) );
            return new DiagramElementGroup( node );
        }

        @PropertyName ( "name" )
        @PropertyDescription ( "name" )
        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            this.name = name;
        }
        @PropertyName ( "properties" )
        @PropertyDescription ( "properties" )
        public DynamicPropertySet getProperties()
        {
            return properties;
        }
        public void setProperties(DynamicPropertySet dps)
        {
            this.properties = dps;
        }
    }

    public static class BionetgenElementPropertiesBeanInfo extends BeanInfoEx2<BionetgenElementProperties>
    {
        public BionetgenElementPropertiesBeanInfo()
        {
            super( BionetgenElementProperties.class );
        }

        @Override
        public void initProperties() throws IntrospectionException
        {
            add( "name" );
            add( "properties" );
        }
    }

    public class BionetgenReactionProperties extends ReactionInitialProperties
    {
        private String formula = "0";
        private List<SpecieReference> components = new ArrayList<>();
        @Override
        public void setSpecieReferences(List<SpecieReference> references)
        {
            components = references;
        }
        @Override
        public void setKineticlaw(KineticLaw kineticLaw)
        {
            if( kineticLaw.getFormula() != null && !kineticLaw.getFormula().isEmpty() )
                formula = kineticLaw.getFormula();
        }
        @Override
        public DiagramElementGroup createElements(Compartment parent, Point location, ViewEditorPane viewPane) throws Exception
        {
            return new DiagramElementGroup( createReactionElements( parent, "", formula, components, location ) );
        }
    }

}
