package biouml.plugins.bionetgen.diagram;

import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.util.DiagramXmlConstants;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.workbench.graph.DiagramToGraphTransformer;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.util.DPSUtils;

public class BionetgenDiagramDeployer
{
    private static final String BIONETGEN_GRAPH = "BioNetGen Graph";
    private static final String SIMPLE_CHEMICAL = "simple chemical";
    private static final String MACROMOLECULE = "macromolecule";
    public static final String SPECIES_NAME_FORMAT = "Species";

    public static @Nonnull Diagram deployBNGDiagram(Diagram src) throws Exception
    {
        return deployBNGDiagram(src, true);
    }

    public static @Nonnull Diagram deployBNGDiagram(Diagram src, boolean needLayout) throws Exception
    {
        int maxIter = 100;
        double maxAgg = 1.0e9;
        String prefix = src.getName();
        String suffix = "";
        Map<String, Integer> maxStoich = null;
        boolean considerStoich = false;

        DynamicProperty attr;
        DynamicPropertySet srcAttributes = src.getAttributes();
        attr = srcAttributes.getProperty(BionetgenConstants.GENERATE_NETWORK_ATTR);
        if( attr != null )
        {
            for( DynamicProperty dp : (DynamicPropertySet)attr.getValue() )
            {
                switch( dp.getName() )
                {
                    case BionetgenConstants.MAX_AGG_PARAM:
                        maxAgg = Double.parseDouble(dp.getValue().toString());
                        break;
                    case BionetgenConstants.MAX_ITER_PARAM:
                        maxIter = (int)Double.parseDouble(dp.getValue().toString());
                        break;
                    case BionetgenConstants.MAX_STOICH_PARAM:
                        maxStoich = new HashMap<>();
                        considerStoich = true;
                        for( DynamicProperty stoichElement : (DynamicPropertySet)dp.getValue() )
                        {
                            maxStoich.put(stoichElement.getName(), (int)Double.parseDouble(stoichElement.getValue().toString()));
                        }
                        break;
                    case BionetgenConstants.PREFIX_PARAM:
                        if( ( prefix = dp.getValue().toString() ).isEmpty() )
                        {
                            prefix = src.getName();
                        }
                        break;
                    case BionetgenConstants.SUFFIX_PARAM:
                        suffix = dp.getValue().toString();
                        break;
                    default:
                        break;
                }
            }
        }

        String dName = suffix.isEmpty() ? prefix : prefix + "_" + suffix;
        Diagram diagram = new SbgnDiagramType().createDiagram( src.getOrigin(), dName, new DiagramInfo( src.getOrigin(), dName ) );
        diagram.getViewOptions().setAutoLayout( false );
        EModel emodel = diagram.getRole( EModel.class );
        SbgnSemanticController semanticController = (SbgnSemanticController)diagram.getType().getSemanticController();

        attr = srcAttributes.getProperty(BionetgenConstants.SIMULATE_ATTR);
        if( attr == null )
            attr = srcAttributes.getProperty( BionetgenConstants.SIMULATE_ODE_ATTR );
        if( attr == null )
            attr = srcAttributes.getProperty( BionetgenConstants.SIMULATE_SSA_ATTR );

        if( attr != null )
            readSimulationAttributes( diagram, attr );
        else
            setSimulationEngine( diagram, constructDefaultEngine() );

        if( src.getComment() != null )
        {
            diagram.setComment(src.getComment());
        }
        EModel srcRole = src.getRole(EModel.class);
        srcRole.getVariables().stream().filter( v -> ! ( v instanceof VariableRole || v.getName().startsWith( "$$" ) ) )
                .forEach( emodel::put );

        List<Equation> initialAssignments = new ArrayList<>();
        for( Equation oldEquation : srcRole.getInitialAssignments() )
        {
            if( srcRole.getVariable(oldEquation.getVariable()) instanceof VariableRole )
            {
                initialAssignments.add(oldEquation);
                continue;
            }
            putEquation( oldEquation.getParent().getName(), oldEquation.getVariable(), oldEquation.getFormula(), oldEquation.getType(),
                    diagram, semanticController );
        }

        List<BionetgenSpeciesGraph> startNodes = addStartNodes( src, diagram, semanticController, initialAssignments, considerStoich );
        List<Reaction> reactions = getReactions(src);
        List<ReactionTemplate> templates = createTemplates(reactions);
        List<Node> observables = getObservables(src);
        List<BionetgenReactionRecord> reactionRecords = new ArrayList<>();

        List<BionetgenSpeciesGraph> allNodes = enrichNodesList(startNodes, templates, maxIter, maxAgg, reactionRecords, maxStoich);

        int startNodesNumber = startNodes.size();
        int size = allNodes.size();
        for( int j = startNodesNumber; j < size; j++ )
        {
            BionetgenSpeciesGraph nodeGraph = allNodes.get(j);
            nodeGraph.updateEdges();
            Node node = createNode( diagram, semanticController, nodeGraph.toString(), j );
            diagram.put(node);
        }

        PlotsInfo plotsInfo = new PlotsInfo(emodel);
        PlotInfo observablePlots = new PlotInfo();
        plotsInfo.setPlots(new PlotInfo[] {observablePlots});
        List<Curve> curves = new ArrayList<>();
        for( Node observable : observables )
        {
            String observableFormula = getObservableContent(allNodes, observable);
            String observableName = BionetgenUtils.generateNodeName(observable.getTitle());
            Variable variable = new Variable(observableName, emodel, emodel.getVariables());
            variable.setInitialValue(0.0);
            emodel.put(variable);
            curves.add(new Curve("", variable.getName(), variable.getTitle(), emodel));
            putEquation( "equation_" + observableName + "_" + BionetgenConstants.OBSERVABLE, observableName, observableFormula,
                    Equation.TYPE_SCALAR, diagram, semanticController );
        }
        observablePlots.setYVariables( curves.stream().toArray( Curve[]::new ) );

        DiagramUtility.setPlotsInfo(diagram, plotsInfo);

        boolean generatedMMLaw = false;
        boolean generatedSatLaw = false;
        diagram.setNotificationEnabled( false );
        emodel.setNotificationEnabled( false );
        for( BionetgenReactionRecord reactionRecord : reactionRecords )
        {
            if( reactionRecord == null )
                continue;

            if( !generatedMMLaw && reactionRecord.isMMType() )
            {
                generatedMMLaw = true;
                putFunction( "term", BionetgenConstants.MM_TERM_FORMULA, diagram, semanticController );
                putFunction( "sFree", BionetgenConstants.MM_SFREE_FORMULA, diagram, semanticController );
                putFunction( "MM", BionetgenConstants.MM_FORMULA, diagram, semanticController );
            }
            if( !generatedSatLaw && reactionRecord.isSaturationType() )
            {
                generatedSatLaw = true;
                putFunction( "Sat_1", BionetgenConstants.SATURATION_FORMULA_1REACTANT, diagram, semanticController );
                putFunction( "Sat_2", BionetgenConstants.SATURATION_FORMULA_2REACTANTS, diagram, semanticController );
            }

            putReaction( diagram, semanticController, reactionRecord );
        }
        emodel.setNotificationEnabled( true );
        diagram.setNotificationEnabled( true );
        emodel.detectVariableTypes();

        if( needLayout )
        {
            layoutDiagram( diagram );
        }
        return diagram;
    }

    private static void putReaction(@Nonnull Diagram diagram, SbgnSemanticController sc, BionetgenReactionRecord reactionRecord)
            throws Exception
    {
        String name = reactionRecord.getName();
        Reaction reaction = new Reaction( null, name );
        reaction.setFormula( reactionRecord.generateFormula() );

        TIntSet ignoreIndexes = new TIntHashSet();
        for( TIntIterator iterator = reactionRecord.getReactantsIndexes().iterator(); iterator.hasNext(); )
        {
            int index = iterator.next();
            String specieName = SPECIES_NAME_FORMAT + index;
            if( !ignoreIndexes.contains( index ) )
            {
                BionetgenUtils.addReference( reaction, specieName, SpecieReference.REACTANT, false );
                ignoreIndexes.add( index );
            }
            else
            {
                for( SpecieReference sr : reaction )
                {
                    if( !sr.isReactant() )
                        continue;
                    if( specieName.equals( sr.getSpecieName() ) )
                    {
                        double stoichiometry = Double.parseDouble( sr.getStoichiometry() ) + 1.0;
                        sr.setStoichiometry( String.valueOf( stoichiometry ) );
                        break;
                    }
                }
            }
        }
        for( TIntIterator iterator = reactionRecord.getProductsIndexes().iterator(); iterator.hasNext(); )
        {
            int index = iterator.next();
            String specieName = SPECIES_NAME_FORMAT + index;
            if( !ignoreIndexes.contains( index ) )
            {
                BionetgenUtils.addReference( reaction, specieName, SpecieReference.PRODUCT, false );
                ignoreIndexes.add( index );
            }
            else
            {
                for( SpecieReference sr : reaction.stream().toList() )
                {
                    if( !specieName.equals( sr.getSpecieName() ) )
                        continue;
                    double stoichiometry = Double.parseDouble( sr.getStoichiometry() );
                    if( sr.isReactant() )
                    {
                        stoichiometry -= 1.0;
                        if( stoichiometry == 0 )
                        {
                            reaction.remove( sr.getName() );
                            BionetgenUtils.addReference( reaction, specieName, SpecieReference.MODIFIER, false );
                        }
                        else
                        {
                            sr.setStoichiometry( String.valueOf( stoichiometry ) );
                        }
                    }
                    else if( sr.isProduct() )
                    {
                        stoichiometry += 1.0;
                        sr.setStoichiometry( String.valueOf( stoichiometry ) );
                    }
                    else
                    {
                        reaction.remove( sr.getName() );
                        BionetgenUtils.addReference( reaction, specieName, SpecieReference.PRODUCT, false );
                    }
                }
            }
        }
        sc.createInstance( diagram, Reaction.class, name, new Point( 0, 0 ), reaction ).putToCompartment( );
    }

    private static void putFunction(String name, String formula, @Nonnull Diagram diagram, SbgnSemanticController sc) throws Exception
    {
        Node node = (Node)sc.createInstance( diagram, Function.class, name, new Point( 0, 0 ), null ).getElement();
        node.getRole( Function.class ).setFormula( formula );
        diagram.put( node );
    }

    private static void putEquation(String name, String variable, String formula, String type, @Nonnull Diagram diagram,
            SbgnSemanticController sc) throws Exception
    {
        String validName = DefaultSemanticController.generateUniqueNodeName( diagram, name );
        Node eqNode = (Node)sc.createInstance( diagram, Equation.class, validName, new Point( 0, 0 ), null ).getElement();
        diagram.setNotificationEnabled( false );
        Equation eq = eqNode.getRole( Equation.class );
        eq.setType( type );
        eq.setVariable( variable );
        eq.setFormula( formula );
        diagram.setNotificationEnabled( true );
        diagram.put( eqNode );
    }

    public static void readSimulationAttributes(Diagram diagram, @Nonnull DynamicProperty attr)
    {
        int steps = -1;
        JavaSimulationEngine jse = constructDefaultEngine();
        jse.setDiagram( diagram );
        for( DynamicProperty dp : (DynamicPropertySet)attr.getValue() )
        {
            switch( dp.getName() )
            {
                case BionetgenConstants.T_START_PARAM:
                    jse.setInitialTime( Double.parseDouble( dp.getValue().toString() ) );
                    break;
                case BionetgenConstants.T_END_PARAM:
                    jse.setCompletionTime( Double.parseDouble( dp.getValue().toString() ) );
                    break;
                case BionetgenConstants.N_STEPS_PARAM:
                case BionetgenConstants.N_OUTPUT_STEPS_PARAM:
                    steps = Integer.parseInt( dp.getValue().toString() );
                    break;
                case BionetgenConstants.ATOL_PARAM:
                    jse.setAbsTolerance( Double.parseDouble( dp.getValue().toString() ) );
                    break;
                case BionetgenConstants.RTOL_PARAM:
                    jse.setRelTolerance( Double.parseDouble( dp.getValue().toString() ) );
                    break;
                case BionetgenConstants.SAMPLE_TIMES_PARAM: //TODO: check parameter
                    diagram.getAttributes()
                            .add( new DynamicProperty( BionetgenConstants.SAMPLE_TIMES_PARAM, double[].class, dp.getValue() ) );
                    break;
                default:
                    break;
            }
        }
        if( steps != -1 )
        {
            jse.setTimeIncrement( jse.getCompletionTime() / steps );
        }
        setSimulationEngine( diagram, jse );
    }
    private static void setSimulationEngine(Diagram diagram, JavaSimulationEngine jse)
    {
        jse.setDiagram( diagram );
        diagram.getAttributes()
                .add( DPSUtils.createHiddenReadOnlyTransient( DiagramXmlConstants.SIMULATION_OPTIONS, SimulationEngine.class, jse ) );
    }
    private static JavaSimulationEngine constructDefaultEngine()
    {
        JavaSimulationEngine jse = new JavaSimulationEngine();
        jse.setAbsTolerance( 1e-8 );
        jse.setRelTolerance( 1e-8 );
        return jse;
    }

    public static List<List<BionetgenSpeciesGraph>> withoutPermutations(List<List<BionetgenSpeciesGraph>> reactantSets,
            TDoubleList multipliers)
    {
        List<List<BionetgenSpeciesGraph>> result = new ArrayList<>();
        TDoubleList newMultipliers = new TDoubleArrayList();
        if( multipliers != null )
        {
            for( TDoubleIterator iterator = multipliers.iterator(); iterator.hasNext(); )
            {
                newMultipliers.add(iterator.next());
            }
            multipliers.removeAll(newMultipliers);
        }
        int setsSize = reactantSets.size();
        for( int i = 0; i < setsSize; i++ )
        {
            List<BionetgenSpeciesGraph> reactantSet = reactantSets.get(i);
            boolean hasAddedPermutation = false;
            int size = result.size();
            for( int j = 0; j < size; j++ )
            {
                List<BionetgenSpeciesGraph> addedSet = result.get(j);
                if( isPermutation(reactantSet, addedSet) )
                {
                    if( multipliers != null )
                    {
                        double multiplier = multipliers.get(j) + newMultipliers.get(i);
                        multipliers.set(j, multiplier);
                    }
                    hasAddedPermutation = true;
                    break;
                }
            }
            if( !hasAddedPermutation )
            {
                if( multipliers != null )
                {
                    multipliers.add(newMultipliers.get(i));
                }
                result.add(reactantSet);
            }
        }
        return result;
    }

    private static boolean isPermutation(List<BionetgenSpeciesGraph> suspectList, List<BionetgenSpeciesGraph> targetList)
    {
        if( suspectList.size() != targetList.size() )
            return false;

        Set<BionetgenSpeciesGraph> usedTargets = new HashSet<>();

        SUSPECTS: for( BionetgenSpeciesGraph suspect : suspectList )
        {
            for( BionetgenSpeciesGraph target : targetList )
            {
                if( usedTargets.contains(target) )
                    continue;

                if( target.isomorphicTo(suspect) )
                {
                    usedTargets.add(target);
                    continue SUSPECTS;
                }
            }
            return false;
        }
        return true;
    }

    private static void layoutDiagram(Diagram diagram)
    {
        Layouter layouter;
        if( diagram.getSize() < 1000 )
        {
            layouter = new HierarchicLayouter();
            ( (HierarchicLayouter)layouter ).setVerticalOrientation(true);
        }
        else
        {
            layouter = new ForceDirectedLayouter();
        }
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        pathwayLayouter.doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
    }

    private static List<ReactionTemplate> createTemplates(List<Reaction> reactions)
    {
        return StreamEx.of( reactions ).map( ReactionTemplate::new ).toList();
    }

    private static final double MAX_NUMBER_OF_SPECIES = 1.2e4;
    private static final double MAX_NUMBER_OF_REACTIONS = 3.5e4;

    private static List<BionetgenSpeciesGraph> enrichNodesList(List<BionetgenSpeciesGraph> startNodes, List<ReactionTemplate> templates,
            int maxNumberOfIterations, final double maxAggregate, final List<BionetgenReactionRecord> reactionRecords, final Map<String, Integer> maxStoich)
            throws Exception
    {
        final List<BionetgenSpeciesGraph> result = new ArrayList<>();
        int counter = 0;
        for( BionetgenSpeciesGraph node : startNodes )
        {
            result.add(node);
        }
        if( maxNumberOfIterations == 0 )
            return result;

        class UsedReactantSet
        {
            List<List<BionetgenSpeciesGraph>> graphs = new ArrayList<>();
            ReactionTemplate template;
            int numberOfReactions = 0;

            public UsedReactantSet(ReactionTemplate template)
            {
                this.template = template;
            }

            public boolean checkUsageOf(List<BionetgenSpeciesGraph> reactantSet)
            {
                LISTS: for( List<BionetgenSpeciesGraph> usedSet : graphs )
                {
                    for( int i = 0; i < usedSet.size(); i++ )
                    {
                        if( !usedSet.get(i).isomorphicTo(reactantSet.get(i)) )
                            continue LISTS;
                    }
                    return true;
                }
                return false;
            }
        }

        List<UsedReactantSet> usedReactantSets = StreamEx.of( templates ).map( UsedReactantSet::new ).toList();
        while( true )
        {
            final AtomicBoolean changed = new AtomicBoolean();
            final List<BionetgenSpeciesGraph> newNodes = new ArrayList<>();
            TaskPool.getInstance().iterate(usedReactantSets, (Iteration<UsedReactantSet>)usedReactantSet -> {
                ReactionTemplate template = usedReactantSet.template;
                List<List<BionetgenSpeciesGraph>> reactantCandidates = template.getReactantSets(result);
                int newNodesSize = newNodes.size();
                for( List<BionetgenSpeciesGraph> reactantSet : withoutPermutations( new PermutationList<>(
                        reactantCandidates), null) )
                {
                    if( usedReactantSet.checkUsageOf(reactantSet) )
                        continue;
                    TIntList reactIndexes = BionetgenReactionRecord.getIndexes(reactantSet, result);
                    List<List<BionetgenSpeciesGraph>> allProducts;
                    try
                    {
                        allProducts = template.executeReaction(reactantSet, maxStoich);
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException(e);
                    }
                    int allProductsSize = allProducts.size();
                    for( int j = 0; j < allProductsSize; j++ )
                    {
                        List<BionetgenSpeciesGraph> products = allProducts.get(j);
                        if( products == null )
                            continue;
                        TIntList prodIndexes = new TIntArrayList();
                        for( BionetgenSpeciesGraph product : products )
                        {
                            if( product.getMoleculesList().size() > (int)maxAggregate )
                            {
                                prodIndexes = null;
                                break;
                            }
                            int index = -1;
                            boolean found = false;
                            int i = 0;
                            BionetgenSpeciesGraph[] newNodesArray;
                            synchronized( newNodes )
                            {
                                newNodesArray = newNodes.toArray(new BionetgenSpeciesGraph[newNodesSize]);
                            }
                            for( i = 0; i < newNodesSize; i++ )
                            {
                                BionetgenSpeciesGraph newNode1 = newNodesArray[i];
                                if( newNode1.isomorphicTo(product) )
                                {
                                    index = i + result.size();
                                    found = true;
                                    break;
                                }
                            }
                            if( !found )
                            {
                                for( i = 0; i < result.size(); i++ )
                                {
                                    BionetgenSpeciesGraph node = result.get(i);
                                    if( node.isomorphicTo(product) )
                                    {
                                        index = i;
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if( !found )
                            {
                                synchronized(newNodes)
                                {
                                    if(newNodes.size() != newNodesSize)
                                    {
                                        for( i = newNodesSize; i < newNodes.size(); i++ )
                                        {
                                            BionetgenSpeciesGraph newNode2 = newNodes.get(i);
                                            if( newNode2.isomorphicTo(product) )
                                            {
                                                index = i + result.size();
                                                found = true;
                                                break;
                                            }
                                        }
                                        newNodesSize = newNodes.size();
                                    }
                                    if( !found )
                                    {
                                        index = newNodesSize + result.size();
                                        newNodes.add(product);
                                        newNodesSize++;
                                    }
                                }
                                changed.set(true);
                            }
                            prodIndexes.add(index);
                        }

                        if( prodIndexes != null )
                        {
                            BionetgenReactionRecord reactionRecord = new BionetgenReactionRecord(template.getName() + "_"
                                    + ( ++usedReactantSet.numberOfReactions ), reactIndexes, prodIndexes, template.getForwardRate(),
                                    template.getRateLawType());
                            if( reactionRecord.needMultipliers() )
                            {
                                reactionRecord.addMultiplier(template.getLastMultiplier(j));
                            }
                            synchronized(reactionRecords)
                            {
                                reactionRecords.add(reactionRecord);
                            }
                        }
                        if( result.size() + newNodesSize >= MAX_NUMBER_OF_SPECIES || reactionRecords.size() >= MAX_NUMBER_OF_REACTIONS )
                        {
                            changed.set(false);
                            return false;
                        }
                    }
                    usedReactantSet.graphs.add(reactantSet);
                }
                return true;
            });
            result.addAll(newNodes);
            counter++;
            if( ( !changed.get() ) || ( counter >= maxNumberOfIterations ) )
                return result;
        }
    }

    private static List<BionetgenSpeciesGraph> addStartNodes(@Nonnull Diagram src, @Nonnull Diagram target, SbgnSemanticController targetSC,
            List<Equation> initialAssignments, boolean considerStoich) throws Exception
    {
        List<BionetgenSpeciesGraph> result = new ArrayList<>();
        int j = 0;
        for( DiagramElement de : src )
        {
            if( de instanceof Node && BionetgenUtils.isSpecies(de) && BionetgenUtils.isStartType(de) )
            {
                DynamicPropertySet attributes = de.getAttributes();
                result.add(new BionetgenSpeciesGraph(attributes.getValueAsString(BionetgenConstants.GRAPH_ATTR), considerStoich));
                putStartNode( target, targetSC, (Node)de, j, initialAssignments );
                ++j;
            }
        }
        return result;
    }

    private static void putStartNode(@Nonnull Diagram diagram, SbgnSemanticController sc, Node sourceNode, int number,
            List<Equation> initialAssignments) throws Exception
    {
        Node node = createNode( diagram, sc, sourceNode.getAttributes().getValueAsString( BionetgenConstants.GRAPH_ATTR ), number );
        VariableRole role = node.getRole(VariableRole.class);
        VariableRole sourceRole = sourceNode.getRole(VariableRole.class);
        role.setConstant(sourceRole.isConstant());
        role.setInitialValue(sourceRole.getInitialValue());
        diagram.put( node );

        for( int i = 0; i < initialAssignments.size(); i++ )
        {
            Equation initialAssignment = initialAssignments.get(i);
            if( initialAssignment.getVariable().equals(sourceRole.getName()) )
            {
                putEquation(BionetgenConstants.EQUATION_NAME, role.getName(), initialAssignment.getFormula(),
                        Equation.TYPE_INITIAL_ASSIGNMENT, diagram, sc );
                initialAssignments.remove(i);
                break;
            }
        }
    }

    private static Node createNode(@Nonnull Diagram diagram, SbgnSemanticController sc, String nodeGraph, int number)
    {
        String name = SPECIES_NAME_FORMAT + number;
        Node node = (Node)sc.createInstance( diagram, Specie.class, name, new Point( 0, 0 ), null ).getElement();
        node.setTitle( name );
        node.getAttributes().add( new DynamicProperty( BIONETGEN_GRAPH, String.class, nodeGraph ) );
        if( nodeGraph.contains( "." ) )
            ( (Specie)node.getKernel() ).setType( MACROMOLECULE );
        else
            ( (Specie)node.getKernel() ).setType( SIMPLE_CHEMICAL );
        return node;
    }

    private static List<Reaction> getReactions(Diagram src) throws Exception
    {
        List<Reaction> result = new ArrayList<>();
        for( DiagramElement de : src )
        {
            if( BionetgenUtils.isReaction(de) )
            {
                result.add(BionetgenUtils.prepareReaction((Node)de, false));
                if( Boolean.parseBoolean(de.getAttributes().getValueAsString(BionetgenConstants.REVERSIBLE_ATTR)) )
                {
                    result.add(BionetgenUtils.prepareReaction((Node)de, true));
                }
            }
        }
        return result;
    }

    private static List<Node> getObservables(Diagram src)
    {
        return src.stream( Node.class ).filter( BionetgenUtils::isObservable ).toList();
    }

    private static List<BionetgenSpeciesGraph> createContentTemplates(Node observeNode)
    {
        List<BionetgenSpeciesGraph> result = new ArrayList<>();
        DynamicProperty contentProperty = observeNode.getAttributes().getProperty(BionetgenConstants.CONTENT_ATTR);
        if( contentProperty == null || contentProperty.getValue() == null )
            return result;
        for( String content : (String[])contentProperty.getValue() )
        {
            result.add(new BionetgenSpeciesGraph(content));
        }
        return result;
    }

    private static String getObservableContent(List<BionetgenSpeciesGraph> nodeNames, Node observeNode)
    {
        StringBuilder resultSB = new StringBuilder();
        boolean matchOnce = Boolean.parseBoolean(observeNode.getAttributes().getValueAsString(BionetgenConstants.MATCH_ONCE_ATTR));
        List<BionetgenSpeciesGraph> contentTemplates = createContentTemplates(observeNode);
        if( contentTemplates.isEmpty() )
            return "0.0";
        int size = nodeNames.size();
        for( int j = 0; j < size; j++ )
        {
            BionetgenSpeciesGraph nodeName = nodeNames.get(j);
            int weightCount = 0;
            for( BionetgenSpeciesGraph contentTemplate : contentTemplates )
            {
                weightCount += contentTemplate.isomorphicToSubgraphOf(nodeName, matchOnce).size();
            }
            if( weightCount != 0 )
            {
                resultSB.append("+");
                if( weightCount > 1 )
                {
                    resultSB.append(weightCount).append("*");
                }
                resultSB.append("$").append(SPECIES_NAME_FORMAT).append(j);
            }
        }
        return resultSB.length() != 0 ? resultSB.toString().substring(1) : "0.0";
    }

}
