package biouml.plugins.bionetgen.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.FastGridLayouter;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.PathwayLayouter;
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
import biouml.plugins.bionetgen.bnglparser.BNGActionParameter;
import biouml.plugins.bionetgen.bnglparser.BNGAddition;
import biouml.plugins.bionetgen.bnglparser.BNGComment;
import biouml.plugins.bionetgen.bnglparser.BNGConstant;
import biouml.plugins.bionetgen.bnglparser.BNGDescription;
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
import biouml.plugins.bionetgen.bnglparser.BNGSpecies;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.workbench.graph.DiagramToGraphTransformer;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class BionetgenDiagramGenerator
{
    private @Nonnull Diagram diagram;
    private EModel emodel;
    private final BionetgenDiagramType type = new BionetgenDiagramType();
    private BionetgenSemanticController controller;
    protected BNGStart start;

    public static Diagram generateDiagram(BNGStart start, Diagram prototypeDiagram, boolean needLayout) throws Exception
    {
        DataCollection<?> origin = null;
        String name = null;
        if( prototypeDiagram != null )
        {
            origin = prototypeDiagram.getOrigin();
            name=  prototypeDiagram.getName();
        }
        return generateDiagram( start, origin, name, needLayout );
    }

    public static Diagram generateDiagram(BNGStart start, DataCollection<?> origin, String diagramName, boolean needLayout) throws Exception
    {
        String name = ( diagramName == null || diagramName.isEmpty() ) ? start.getName() : diagramName;
        BionetgenDiagramGenerator generator = new BionetgenDiagramGenerator( start, origin, name );
        return generator.generateDiagram( needLayout );
    }

    private BionetgenDiagramGenerator(BNGStart start, DataCollection<?> origin, String name) throws Exception
    {
        this.start = start;
        diagram = type.createDiagram( origin, name, new DiagramInfo( null, name ) );
    }

    private Diagram generateDiagram(boolean needLayout) throws Exception
    {
        controller = (BionetgenSemanticController)type.getSemanticController();
        String bngText = ( new BionetgenTextGenerator(start) ).generateText();

        emodel = diagram.getRole( EModel.class );

        BionetgenUtils.setBionetgenAttr(diagram, bngText);

        fillDiagram();

        if( needLayout )
        {
            Graph graph = DiagramToGraphTransformer.generateGraph( diagram, null );
            if( diagram.getSize() < 500 )
            {
                FastGridLayouter layouter = new FastGridLayouter();
                layouter.setStrongRepulsion( -1 );
                layouter.setIterations( 1 );
                layouter.setThreadCount( 1 );
                layouter.setCool( 0.7 );
                layouter.doLayout( graph, null );
            }
            else
            {
                PathwayLayouter layouter = new PathwayLayouter( new ForceDirectedLayouter() );
                layouter.doLayout( graph, null );
            }
            DiagramToGraphTransformer.applyLayout(graph, diagram);
        }
        diagram.stream( Compartment.class ).forEach( this::layoutContent );

        return diagram;
    }

    private void fillDiagram() throws Exception
    {
        for( int i = 0; i < start.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = start.jjtGetChild(i);
            if( currentChild instanceof BNGDescription )
                addDescription(currentChild.getName());
            else if( currentChild instanceof BNGModel )
                addModel((BNGModel)currentChild);
            else if( currentChild instanceof BNGList && ( (BNGList)currentChild ).getType() == BNGList.ACTION )
                addActions((BNGList)currentChild);
        }
    }

    private void layoutContent(Compartment complex)
    {
        String s;
        if( ( s = complex.getAttributes().getValueAsString(BionetgenConstants.GRAPH_ATTR) ) == null )
            return;
        int size = new BionetgenSpeciesGraph(s).getMoleculesList().size();
        int width = calculateWidth(size);
        complex.setShapeSize(new Dimension(130 * width, 80 * width));
        Point point = complex.getLocation();

        int i = 0;
        for( DiagramElement de : complex )
        {
            if( de instanceof Compartment )
            {
                ( (Compartment)de ).setLocation((int)point.getX() + 25 + ( i / width ) * 130, (int)point.getY() + 15 + ( i % width ) * 80);
                locateNodes((Compartment)de);
                i++;
            }
            else if( de instanceof Edge )
            {
                Edge edge = (Edge)de;
                edge.setInPort(edge.getInput().getLocation());
                edge.setOutPort(edge.getOutput().getLocation());
                de = edge;
            }
        }
    }

    private int calculateWidth(int n)
    {
        int root = (int)Math.sqrt(n);
        return root * root == n ? root : root + 1;
    }

    private void locateNodes(Compartment molecule)
    {
        Point point = molecule.getLocation();
        Node[] nodes = molecule.getNodes();
        for( int i = 0; i < nodes.length; i++ )
        {
            Node node = nodes[i];
            node.setLocation((int)point.getX() - 20 + 80 * ( ( i / 2 ) % 2 ), (int)point.getY() - 10 + 50 * ( i % 2 ));
        }
    }

    private void addDescription(String description)
    {
        diagram.setComment(description);
    }

    private void addModel(BNGModel model) throws Exception
    {
        /*
         * Order of parameters, seed species, etc. is important in diagram generation:
         * the first is parameters block, seed species, observables, reactions.
         * But in the bngl-file they may be in any order
         */
        int[] order = new int[5];
        for( int i = 0; i < 5; i++ )
            order[i] = -1;
        for( int i = 0; i < model.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = model.jjtGetChild(i);
            if( currentChild instanceof BNGList )
            {
                int type = ( (BNGList)currentChild ).getType();
                switch( type )
                {
                    case BNGList.PARAMETER:
                        order[0] = i;
                        break;
                    case BNGList.SPECIES:
                        order[1] = i;
                        break;
                    case BNGList.OBSERVABLES:
                        order[2] = i;
                        break;
                    case BNGList.REACTIONS:
                        order[3] = i;
                        break;
                    case BNGList.MOLECULETYPE:
                        order[4] = i;
                        break;
                    case BNGList.UNDEFINED:
                        throw new Exception( "Error during parsing: one of the block has no type. Try to parse again." );
                    default:
                        break;
                }
            }
        }
        if( order[0] != -1 )
            addParameters((BNGList)model.jjtGetChild(order[0]));
        if( order[1] != -1 )
            addSpecies((BNGList)model.jjtGetChild(order[1]));
        if( order[2] != -1 )
            addObservables((BNGList)model.jjtGetChild(order[2]));
        if( order[3] != -1 )
            addReactions((BNGList)model.jjtGetChild(order[3]));
        if( order[4] != -1 )
            addMoleculeTypes((BNGList)model.jjtGetChild(order[4]));

        if( !model.checkMoleculesTypes() )
            throw new BionetgenUtils.BionetgenException( "Molecule type mismatch in " + model.getErrorMessage() );
    }

    private void addParameters(BNGList parameters) throws Exception
    {
        for( int i = 0; i < parameters.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = parameters.jjtGetChild(i);
            if( currentChild instanceof BNGParameter )
                addParameter((BNGParameter)currentChild);
        }
    }

    private void addParameter(BNGParameter parameter) throws Exception
    {
        Variable param = new Variable(parameter.getName(), emodel, emodel.getVariables());
        DynamicPropertySet attributes = param.getAttributes();
        BionetgenAstCreator.link(attributes, parameter);
        for( int i = 0; i < parameter.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = parameter.jjtGetChild(i);
            if( currentChild instanceof BNGExpression )
            {
                String initialValue = ( (BNGExpression)currentChild ).generateFormula();
                BionetgenAstCreator.link(attributes, currentChild);
                try
                {
                    double value = Double.parseDouble(initialValue);
                    param.setInitialValue(value);
                    param.setConstant(true);
                }
                catch( NumberFormatException e )
                {
                    Node eqNode = putInitialAssignment(parameter.getName(), initialValue);
                    BionetgenAstCreator.link(eqNode, currentChild);
                    BionetgenAstCreator.link(eqNode, parameter);
                }
            }
            else if( currentChild instanceof BNGComment )
            {
                param.setComment(currentChild.getName());
            }
            else if( currentChild instanceof BNGLabel )
            {
                attributes.add(new DynamicProperty(BionetgenConstants.LABEL_ATTR, String.class, currentChild.getName()));
                BionetgenAstCreator.link(attributes, currentChild);
            }
        }
        emodel.put(param);
    }

    private void addSpecies(BNGList species) throws Exception
    {
        for( int i = 0; i < species.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = species.jjtGetChild(i);
            if( currentChild instanceof BNGSeedSpecie )
                addStartSpecie((BNGSeedSpecie)currentChild);
        }
    }

    private void addStartSpecie(BNGSeedSpecie seedSpecie) throws Exception
    {
        Compartment complex = null;
        BNGLabel label = null;
        String labelString = "";
        for( int i = 0; i < seedSpecie.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = seedSpecie.jjtGetChild(i);
            if( currentChild instanceof BNGSpecies )
            {
                complex = generateSpecies( diagram, currentChild.getName(), true, seedSpecie.isConstant() );
                BionetgenAstCreator.link(complex, currentChild);
            }
            else if( currentChild instanceof BNGExpression && complex != null )
            {
                BionetgenAstCreator.link(complex, currentChild);
                String initialValue = ( (BNGExpression)currentChild ).generateFormula();
                VariableRole variableRole = complex.getRole(VariableRole.class);
                variableRole.setConstant(seedSpecie.isConstant());
                try
                {
                    double value = Double.parseDouble(initialValue);
                    variableRole.setInitialValue(value);
                }
                catch( NumberFormatException e )
                {
                    Node eqNode = putInitialAssignment(variableRole.getName(), initialValue);
                    BionetgenAstCreator.link(eqNode, currentChild);
                    BionetgenAstCreator.link(eqNode, seedSpecie);
                }
            }
            else if( currentChild instanceof BNGComment && complex != null )
            {
                complex.setComment(currentChild.getName());
            }
            else if( currentChild instanceof BNGLabel )
            {
                label = (BNGLabel)currentChild;
                labelString = label.getName();
            }
        }
        if( complex == null )
            return;
        complex.getAttributes().add(new DynamicProperty(BionetgenConstants.LABEL_ATTR, String.class, labelString));
        if( label != null )
        {
            BionetgenAstCreator.link(complex, label);
        }
        BionetgenAstCreator.link(complex, seedSpecie);
    }

    private void addObservables(BNGList observables) throws Exception
    {
        for( int i = 0; i < observables.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = observables.jjtGetChild(i);
            if( currentChild instanceof BNGObservable )
                addObservable((BNGObservable)currentChild);
        }
    }

    private void addObservable(BNGObservable observable) throws Exception
    {
        Compartment observableNode = (Compartment)controller.createNodeInstance( diagram, BionetgenConstants.TYPE_OBSERVABLE,
                BionetgenConstants.OBSERVABLE, new Point( 0, 0 ), null );
        observableNode.setTitle(observable.getName());
        observableNode.setShapeSize(new Dimension(70, 70));

        DynamicPropertySet attributes = observableNode.getAttributes();
        attributes.add(new DynamicProperty(BionetgenConstants.MATCH_ONCE_ATTR, Boolean.class, observable.isMatchOnce()));
        BionetgenAstCreator.link(observableNode, observable);
        List<String> contents = new ArrayList<>();
        for( int i = 0; i < observable.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = observable.jjtGetChild(i);
            if( currentChild instanceof BNGList && ( (BNGList)currentChild ).getType() == BNGList.OBSERVABLECONTENT )
            {
                addContent((BNGList)currentChild, contents);
            }
            else if( currentChild instanceof BNGComment )
            {
                observableNode.setComment(currentChild.getName());
            }
            else if( currentChild instanceof BNGLabel )
            {
                attributes.add(new DynamicProperty(BionetgenConstants.LABEL_ATTR, String.class, currentChild.getName()));
                BionetgenAstCreator.link(observableNode, currentChild);
            }
        }
        attributes.add( new DynamicProperty( BionetgenConstants.CONTENT_ATTR, String[].class,
                contents.toArray( new String[contents.size()] ) ) );
        diagram.put(observableNode);
    }

    private void addContent(BNGList list, List<String> contentsList)
    {
        for( int i = 0; i < list.jjtGetNumChildren(); i++ )
        {
            if( list.jjtGetChild( i ) instanceof BNGSpecies )
                contentsList.add( ( list.jjtGetChild( i ) ).getName() );
        }
    }

    private void addReactions(BNGList reactions) throws Exception
    {
        for( int i = 0; i < reactions.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node currentChild = reactions.jjtGetChild(i);
            if( currentChild instanceof BNGReaction )
                addReaction((BNGReaction)currentChild);
        }
    }

    private void addReaction(BNGReaction bngReaction) throws Exception
    {
        Reaction reaction = new Reaction( null, bngReaction.getName() );
        BNGLabel label = null;
        String comment = "";

        DynamicPropertySet attributes = new DynamicPropertySetAsMap();
        attributes.add( new DynamicProperty( BionetgenConstants.REVERSIBLE_ATTR, Boolean.class, bngReaction.isReversible() ) );

        for( int i = 0; i < bngReaction.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node child = bngReaction.jjtGetChild(i);
            if( child instanceof BNGList )
            {
                if( ( (BNGList)child ).getType() == BNGList.REACTIONCOMPONENT )
                    addReactionComponents( (BNGList)child, reaction );
                else if( ( (BNGList)child ).getType() == BNGList.ADDITIONCOMPONENT )
                    setAddition( (BNGList)child, attributes );
            }
            else if( child instanceof BNGRateLaw )
            {
                setRateLaw( (BNGRateLaw)child, attributes );
            }
            else if( child instanceof BNGComment )
            {
                comment = child.getName();
            }
            else if( child instanceof BNGLabel )
            {
                label = (BNGLabel)child;
            }
        }
        List<SpecieReference> components = StreamEx.of( reaction.getSpecieReferences() )
                .sorted( Comparator.comparingInt( sr -> (Integer)sr.getAttributes().getValue( BionetgenConstants.REACTANT_NUMBER_ATTR ) ) )
                .toList();
        List<DiagramElement> elements = controller
                .createReactionElements( diagram, bngReaction.getName(), "0", components, new Point( 0, 0 ) );
        Node reactionNode = StreamEx.of( elements ).select( Node.class ).peek( diagram::put ).findFirst().get();
        StreamEx.of( elements ).select( Edge.class ).forEach( diagram::put );

        for( DynamicProperty dp : attributes )
            reactionNode.getAttributes().add( dp );
        if( label != null )
        {
            reactionNode.getAttributes().add( new DynamicProperty( BionetgenConstants.LABEL_ATTR, String.class, label.getName() ) );
            BionetgenAstCreator.link( reactionNode, label );
        }
        if( !comment.isEmpty() )
            reactionNode.setComment( comment );
        BionetgenAstCreator.link( reactionNode, bngReaction );
    }

    private void addReactionComponents(BNGList list, Reaction reaction) throws Exception
    {
        String role = list.getName();
        for( int i = 0; i < list.jjtGetNumChildren(); i++ )
        {
            if( list.jjtGetChild(i) instanceof BNGSpecies )
                addReactionComponent( (BNGSpecies)list.jjtGetChild( i ), reaction, role );
        }
    }

    private void addReactionComponent(BNGSpecies species, Reaction reaction, String role) throws Exception
    {
        String specie = species.getName();
        String validName = BionetgenUtils.generateNodeName( specie );

        Compartment specieNode = (Compartment)diagram.get( validName );
        if( specieNode == null )
        {
            specieNode = generateSpecies( diagram, specie, false, false );
        }
        BionetgenAstCreator.link( specieNode, species );

        BionetgenUtils.addReference( reaction, validName, role, false );
    }

    private void setAddition(BNGList list, DynamicPropertySet dps)
    {
        String[] additions = IntStreamEx.range( list.jjtGetNumChildren() ).mapToObj( list::jjtGetChild ).select( BNGAddition.class )
                .map( BNGAddition::getFullName ).toArray( String[]::new );
        dps.add( new DynamicProperty( BionetgenConstants.ADDITION_ATTR, String[].class, additions ) );
    }

    private void setRateLaw(BNGRateLaw law, DynamicPropertySet dps)
    {
        dps.add( new DynamicProperty( BionetgenConstants.RATE_LAW_TYPE_PD, String.class, law.getType() ) );
        if( !law.getType().equals( BionetgenConstants.DEFAULT ) )
        {
            dps.add( new DynamicProperty( BionetgenConstants.FORWARD_RATE_ATTR, String.class, law.getFullName() ) );
        }
        else
        {
            dps.add( new DynamicProperty( BionetgenConstants.FORWARD_RATE_ATTR, String.class, ( (BNGExpression)law.jjtGetChild( 0 ) )
                    .generateFormula() ) );

            DynamicProperty dp = dps.getProperty( BionetgenConstants.REVERSIBLE_ATTR );
            if( dp != null && (Boolean)dp.getValue() )
                dps.add( new DynamicProperty( BionetgenConstants.BACKWARD_RATE_ATTR, String.class, ( (BNGExpression)law.jjtGetChild( 2 ) )
                        .generateFormula() ) );
        }
    }

    private void addMoleculeTypes(BNGList molTypes) throws Exception
    {
        for( int i = 0; i < molTypes.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node child = molTypes.jjtGetChild(i);
            if( child instanceof BNGMoleculeType )
            {
                addMoleculeType((BNGMoleculeType)child);
            }
        }
    }

    private void addMoleculeType(BNGMoleculeType moleculeType) throws Exception
    {
        Node node = controller.createNodeInstance( diagram, BionetgenConstants.TYPE_MOLECULETYPE,
                BionetgenConstants.MOLECULE_TYPE_NAME, new Point( 0, 0 ), null );

        DynamicPropertySet attributes = node.getAttributes();
        for( int i = 0; i < moleculeType.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node child = moleculeType.jjtGetChild(i);
            if( child instanceof BNGSpecies )
            {
                attributes.setValue( BionetgenConstants.MOLECULE_TYPE_ATTR, child.getName() );
            }
            else if( child instanceof BNGComment )
            {
                node.setComment(child.getName());
            }
            else if( child instanceof BNGLabel )
            {
                attributes.setValue( BionetgenConstants.LABEL_ATTR, child.getName() );
                BionetgenAstCreator.link(node, child);
            }
        }
        BionetgenAstCreator.link(node, moleculeType);
        diagram.put(node);
    }

    private void addActions(BNGList actionList) throws Exception
    {
        for( int i = 0; i < actionList.jjtGetNumChildren(); i++ )
        {
            if( actionList.jjtGetChild(i) instanceof BNGAction )
            {
                addAction((BNGAction)actionList.jjtGetChild(i));
                BionetgenAstCreator.link(diagram, actionList);
            }
        }
    }

    private void addAction(BNGAction action) throws Exception
    {
        String name = action.getName();
        if( !name.equals(BionetgenConstants.GENERATE_NETWORK_ATTR) && !name.equals(BionetgenConstants.SIMULATE_ODE_ATTR)
                && !name.equals(BionetgenConstants.SIMULATE_SSA_ATTR) && !name.equals(BionetgenConstants.SIMULATE_ATTR) )
            return;

        DynamicPropertySetAsMap dpsam = new DynamicPropertySetAsMap();

        for( int i = 0; i < action.jjtGetNumChildren(); i++ )
        {
            if( action.jjtGetChild(i) instanceof BNGActionParameter )
                addActionParameter((BNGActionParameter)action.jjtGetChild(i), dpsam);
        }

        DynamicProperty dp = new DynamicProperty(name, DynamicPropertySet.class, dpsam);
        DynamicPropertySet attributes = diagram.getAttributes();
        attributes.add(dp);
        dp.setParent(attributes);
    }

    private void addActionParameter(BNGActionParameter parameter, DynamicPropertySetAsMap dpsam) throws Exception
    {
        String name = parameter.getName();
        DynamicProperty dp;
        if( BionetgenConstants.SAMPLE_TIMES_PARAM.equals(name) )
        {
            List<Double> sampleTimes = new ArrayList<>();
            for( int i = 0; i < parameter.jjtGetNumChildren(); i++ )
            {
                if( parameter.jjtGetChild(i) instanceof BNGConstant )
                    sampleTimes.add(Double.parseDouble(parameter.jjtGetChild(i).getName()));
            }
            Collections.sort(sampleTimes);
            double[] sampleTimesArray = new double[sampleTimes.size()];
            for( int i = 0; i < sampleTimes.size(); i++ )
            {
                sampleTimesArray[i] = sampleTimes.get(i);
            }
            dp = new DynamicProperty(name, double[].class, sampleTimesArray);
            dpsam.add(dp);
            dp.setParent(dpsam);
            return;
        }
        for( int i = 0; i < parameter.jjtGetNumChildren(); i++ )
        {
            biouml.plugins.bionetgen.bnglparser.Node child = parameter.jjtGetChild(i);
            if( child instanceof BNGConstant )
            {
                dp = new DynamicProperty(name, String.class, child.getName());
                dpsam.add(dp);
                dp.setParent(dpsam);
                break;
            }
            else if( child instanceof BNGHash )
            {
                DynamicPropertySetAsMap map = new DynamicPropertySetAsMap();
                for( int j = 0; j < child.jjtGetNumChildren(); j++ )
                {
                    biouml.plugins.bionetgen.bnglparser.Node hashChild = child.jjtGetChild(j);
                    if( hashChild instanceof BNGActionParameter )
                    {
                        String hashParamName = hashChild.getName();
                        for( int k = 0; k < hashChild.jjtGetNumChildren(); k++ )
                        {
                            if( hashChild.jjtGetChild(k) instanceof BNGConstant )
                            {
                                dp = new DynamicProperty(hashParamName, String.class, hashChild.jjtGetChild(k).getName());
                                map.add(dp);
                                dp.setParent(map);
                            }
                        }
                    }
                }
                dp = new DynamicProperty(name, DynamicPropertySet.class, map);
                dpsam.add(dp);
                dp.setParent(dpsam);
                break;
            }
        }
    }

    private Compartment generateSpecies(@Nonnull Diagram diagram, String graphStr, boolean isSeedSpecies, boolean isConstant)
            throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        dps.add( new DynamicProperty( BionetgenConstants.IS_SEED_SPECIES_PD, Boolean.class, isSeedSpecies ) );
        dps.add( new DynamicProperty( BionetgenConstants.GRAPH_ATTR, String.class, graphStr ) );
        Compartment species = (Compartment)controller.createNodeInstance( diagram, BionetgenConstants.TYPE_SPECIES,
                BionetgenUtils.generateNodeName( graphStr ), new Point( 0, 0 ), dps );
        species.getRole( VariableRole.class ).setConstant( isConstant );
        diagram.put( species );

        BionetgenSpeciesGraph graph = new BionetgenSpeciesGraph( graphStr );
        int width = calculateWidth(graph.getMoleculesList().size());
        species.setShapeSize( new Dimension( 130 * width, 80 * width ) );

        return species;
    }

    private Node putInitialAssignment(String variableName, String initialValue) throws Exception
    {
        Node eqNode = controller.createNodeInstance( diagram, BionetgenConstants.TYPE_EQUATION, BionetgenConstants.EQUATION_NAME,
                new Point( 0, 0 ), null );
        Equation role = eqNode.getRole( Equation.class );
        role.setVariable( variableName );
        role.setFormula( initialValue );
        diagram.put( eqNode );
        return eqNode;
    }
}
