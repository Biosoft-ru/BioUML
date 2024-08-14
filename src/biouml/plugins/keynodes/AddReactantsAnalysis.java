package biouml.plugins.keynodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewOptions;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.util.AddElementsUtils;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.UserHubEdge;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.UserHubCollection;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

@ClassIcon ( "resources/AddReactants.gif" )
public class AddReactantsAnalysis extends AnalysisMethodSupport<AddReactantsAnalysis.AddReactantsAnalysisParameters>
{
    public AddReactantsAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new AddReactantsAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        BioHubInfo hub = getParameters().getBioHub();
        if( hub == null )
            throw new MissingParameterException( "BioHub" );
        if( ! ( hub.getBioHub() instanceof KeyNodesHub ) )
            throw new ParameterNotAcceptableException("BioHub", hub.getName());
    }

    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        Diagram diagram = getParameters().getInputDiagram().getDataElement(Diagram.class);
        KeyNodesHub<?> bioHub = getParameters().getKeyNodesHub();
        Diagram result = new Diagram(getParameters().getOutputDiagram().getParentCollection(),
                new DiagramInfo(getParameters().getOutputDiagram().getParentCollection(), getParameters().getOutputDiagram().getName()),
                bioHub.getVisualizationDiagramType());
        jobControl.pushProgress(0, 50);
        addReactants(diagram, result, bioHub, getParameters().getSpecies());
        jobControl.popProgress();
        Diagram d = bioHub.convert(result);
        jobControl.pushProgress(70, 95);
        KeyNodeUtils.layoutDiagram(d, jobControl);
        jobControl.popProgress();
        annotate(diagram, d);
        jobControl.setPreparedness(100);
        return d;
    }

    private void annotate(Diagram input, Diagram output)
    {
        if( input.getViewOptions().getClass().isAssignableFrom(output.getViewOptions().getClass()) )
            output.setViewOptions((DiagramViewOptions)input.getViewOptions().clone());
        DynamicProperty dp = input.getAttributes().getProperty(DataCollectionUtils.SPECIES_PROPERTY);
        if( dp != null )
            output.getAttributes().add(dp);

        for( Node node : input.recursiveStream().select(Node.class) )
        {
            DiagramElement de = output.get(node.getCompleteNameInDiagram());
            if( de instanceof Node )
                copyColors(node, (Node)de);
        }
    }

    private void copyColors(Node input, Node output)
    {
        output.setPredefinedStyle(input.getPredefinedStyle());
        output.setCustomStyle(input.getCustomStyle());
    }

    private void addReactants(Diagram source, final Diagram target, final KeyNodesHub<?> bioHub, final Species species) throws Exception
    {
        jobControl.forCollection(source.stream(Node.class).toList(), de -> {
            addReactants(target, bioHub, species, de, false);
            return true;
        });
    }

    static void addReactants(final Diagram target, final KeyNodesHub<?> bioHub, final Species species, Node de, boolean missingEndsOnly)
    {
        Base kernel = de.getKernel();
        if( kernel != null && ( kernel instanceof Reaction || "reaction".equals(kernel.getType()) ) )
        {
            Element[] componentsArray;

            if( UserHubEdge.isUserReaction(de) )
            {
                UserHubEdge hubEdge = new UserHubEdge( de.getKernel().getTitle(),
                        kernel.getAttributes().getValueAsString( Element.USER_REACTANTS_PROPERTY ),
                        kernel.getAttributes().getValueAsString( Element.USER_PRODUCTS_PROPERTY ) );
                Element reactionElement = hubEdge.createElement( bioHub );
                if( missingEndsOnly )
                {
                    componentsArray = de.edges().map( edge -> createEdgeElement( bioHub, edge, reactionElement.getPath() ) )
                            .append( reactionElement ).toArray( Element[]::new );
                }
                else
                {
                    List<Element> userReactionComponents = processUserReactionComponents( de.getKernel(), bioHub,
                            reactionElement.getPath() );
                    componentsArray = StreamEx.of( userReactionComponents ).append( reactionElement ).toArray( Element[]::new );
                }
            }
            else
            {
                Map<String, Set<String>> preferredComponents = de.edges()
                        .mapToEntry(edge -> edge.getKernel().cast(SpecieReference.class).getRole(),
                                edge -> edge.getOtherEnd(de).getKernel().getName())
                        .groupingTo(HashSet::new);
                if( missingEndsOnly && preferredComponents.containsKey(RelationType.PRODUCT)
                        && preferredComponents.containsKey(RelationType.REACTANT) )
                    return;
                componentsArray = bioHub.getReactionComponents(kernel.getName(), species, preferredComponents).stream()
                        .toArray(Element[]::new);
                if( missingEndsOnly )
                {
                    String addRole = preferredComponents.containsKey(RelationType.PRODUCT) ? RelationType.REACTANT : RelationType.PRODUCT;
                    componentsArray = StreamEx.of(componentsArray)
                            .filter(e -> e.getRelationType() != null && e.getRelationType().equals(addRole)).limit(1)
                            .toArray(Element[]::new);
                }
            }

            try
            {
                AddElementsUtils.addNodesToCompartment( componentsArray, target, null, null );
                AddElementsUtils.addEdgesToCompartment( componentsArray, target, true, null );
            }
            catch( Exception e )
            {
            }
        }
    }

    private static List<Element> processUserReactionComponents(Base kernel, final KeyNodesHub<?> bioHub, String path)
    {
        List<Element> components = new ArrayList<>();
        for( String prop : new String[] {Element.USER_REACTANTS_PROPERTY, Element.USER_PRODUCTS_PROPERTY} )
        {
            if( kernel.getAttributes().hasProperty( prop ) )
            {
                String[] attrComponents = kernel.getAttributes().getValueAsString( prop ).split( "," );
                for( String comp : attrComponents )
                {
                    Element elem = new Element( bioHub.getCompleteElementPath( comp ) );
                    if(prop.equals( Element.USER_REACTANTS_PROPERTY ) )
                    {
                        elem.setRelationType( RelationType.REACTANT );
                        elem.setLinkedDirection( BioHub.DIRECTION_UP );
                        //edge.addReactant( comp );
                    }
                    else
                    {
                        elem.setRelationType( RelationType.PRODUCT );
                        elem.setLinkedDirection( BioHub.DIRECTION_DOWN );
                        //edge.addProduct( comp );
                    }
                    elem.setLinkedFromPath( path );
                    components.add( elem );
                }
            }
        }
        return components;
    }

    private static Element createEdgeElement(KeyNodesHub<?> bioHub, Edge edge, String path)
    {
        SpecieReference kernel = edge.getKernel().cast(SpecieReference.class);
        Element e = new Element(bioHub.getCompleteElementPath(kernel));
        e.setRelationType(kernel.getRole());
        e.setLinkedFromPath(path);
        e.setLinkedDirection(kernel.getRole().equals(RelationType.PRODUCT) ? BioHub.DIRECTION_DOWN : BioHub.DIRECTION_UP);
        return e;
    }

    @SuppressWarnings ( "serial" )
    public static class AddReactantsAnalysisParameters extends BasicKeyNodeAnalysisParameters
    {
        private DataElementPath inputDiagram, outputDiagram;

        @PropertyName ( "Input diagram" )
        @PropertyDescription ( "Result of network visualization" )
        public DataElementPath getInputDiagram()
        {
            return inputDiagram;
        }
        public void setInputDiagram(DataElementPath inputDiagram)
        {
            Object oldValue = this.inputDiagram;
            this.inputDiagram = inputDiagram;
            firePropertyChange("inputDiagram", oldValue, inputDiagram);
            try
            {
                Diagram diagram = inputDiagram.getDataElement(Diagram.class);

                String speciesStr = diagram.getAttributes().getValueAsString(DataCollectionUtils.SPECIES_PROPERTY);
                Species species = null;
                if( speciesStr != null )
                    species = Species.getSpecies(speciesStr);
                if( species == null )
                    species = Species.getDefaultSpecies(diagram);
                setSpecies(species);

                BioHubInfo hubInfo = BioHubRegistry
                        .getBioHubInfo(diagram.getAttributes().getValueAsString(KeyNodeConstants.BIOHUB_PROPERTY));
                if( hubInfo != null && hubInfo.getBioHub() instanceof KeyNodesHub )
                    setBioHub( hubInfo );
            }
            catch( RepositoryException e )
            {
            }
        }

        @PropertyName ( "Output diagram" )
        @PropertyDescription ( "Full path to the output" )
        public DataElementPath getOutputDiagram()
        {
            return outputDiagram;
        }
        public void setOutputDiagram(DataElementPath outputDiagram)
        {
            Object oldValue = this.outputDiagram;
            this.outputDiagram = outputDiagram;
            firePropertyChange("outputDiagram", oldValue, outputDiagram);
        }

    }

    public static class AddReactantsAnalysisParametersBeanInfo extends BeanInfoEx2<AddReactantsAnalysisParameters>
    {
        public AddReactantsAnalysisParametersBeanInfo()
        {
            super(AddReactantsAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("inputDiagram").inputElement(Diagram.class).add();
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            property( "bioHub" ).simple().editor( BioHubSelector.class ).add();
            property( "customHubCollection" ).inputElement( UserHubCollection.class ).hidden( "isCustomHubCollectionHidden" ).add();
            property("outputDiagram").outputElement(Diagram.class).auto("$inputDiagram$ full").add();
        }
    }
}
