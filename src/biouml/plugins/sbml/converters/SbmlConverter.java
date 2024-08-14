package biouml.plugins.sbml.converters;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverterSupport;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.Function;
import biouml.model.dynamics.Variable;
import biouml.plugins.sbml.SbmlDiagramType;
import biouml.plugins.sbml.SbmlDiagramType_L3v1;
import biouml.plugins.sbml.SbmlEModel;
import biouml.plugins.sbml.extensions.RdfExtensionReader;
import biouml.standard.StandardModuleType;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Cell;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Gene;
import biouml.standard.type.GenericEntity;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;
import biouml.standard.type.Unit;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.graph.Path;

public class SbmlConverter extends DiagramTypeConverterSupport
{
    protected String[] nodeKernelTypes = new String[] {Type.TYPE_CELL, Type.TYPE_CONCEPT, Type.TYPE_GENE, Type.TYPE_RNA, Type.TYPE_PROTEIN,
            Type.TYPE_SUBSTANCE};
    protected String[] edgeKernelTypes = new String[] {Type.TYPE_SEMANTIC_RELATION, Type.TYPE_CHEMICAL_ROLE};

    public static final String COMPLETE_NAME_ATTR = "completeName";
    public static final String CAS_REGISTRY_NUMBER_ATTR = "casRegistryNumber";
    public static final String FORMULA_ATTR = "formula";
    public static final String STRUCTURE_REFERENCES_ATTR = "structureReferences";
    public static final String SYNONYMS_ATTR = "synonyms";
    public static final String FUNCTIONAL_STATE_ATTR = "functionalState";
    public static final String GENE_ATTR = "gene";
    public static final String MODIFICATION_ATTR = "modification";
    public static final String REGULATION_ATTR = "regulation";
    public static final String SPECIES_ATTR = "species";
    public static final String SOURCE_ATTR = "source";
    public static final String RNA_TYPE_ATTR = "rnaType";
    public static final String CHROMOSOME_ATTR = "chromosome";
    public static final String DEFAULT_COLOR_ATTR = "defaultColor";
    public static final String RELATION_TYPE_ATTR = "relationType";
    public static final String INPUT_ELEMENT_NAME_ATTR = "inputElementName";
    public static final String OUTPUT_ELEMENT_NAME_ATTR = "outputElementName";
    public static final String MODIFIER_ACTION_ATTR = "modifierAction";
    public static final String PARTICIPATION_ATTR = "participation";
    public static final String ROLE_ATTR = "role";
    public static final String SPECIE_ATTR = "specie";
    public static final String STOICHIOMETRY_ATTR = "stoichiometry";

    public static final String MIRIAM_RESOURCE = "databases/Utils/MIRIAM";

    @Override
    public Diagram convert(Diagram diagram, Object type) throws Exception
    {
        if( type instanceof Class && SbmlDiagramType.class.isAssignableFrom((Class<?>)type)
                && ! ( (Class<?>)type ).equals(SbmlDiagramType.class) )
            return super.convert(diagram, type);
        else
            return super.convert(diagram, SbmlDiagramType_L3v1.class);
    }

    @Override
    protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
    {
        Diagram oldDiagram = diagram.clone(diagram.getOrigin(), diagram.getName());

        diagram.setType(diagramType);
        diagram.clear();

        DiagramInfo oldInfo = (DiagramInfo)diagram.getKernel();
        DiagramInfo newInfo = oldInfo.clone(diagram.getName());
        fillMainProperties(oldInfo, newInfo);
        diagram.setKernel(newInfo);

        if( isEModel(diagram) )
        {
            boolean notification = diagram.getRole(EModel.class).isNotificationEnabled();
            EModel emodel = new SbmlEModel(diagram);
            diagram.setRole(emodel);
            emodel.setNotificationEnabled(notification);
        }
        else if( diagram.getRole() == null )
        {
            diagram.setRole(new SbmlEModel(diagram));
        }

        try
        {
            transformNodesAndEdges(diagram, oldDiagram);
            if( isEModel(oldDiagram) )
            {
                copyParameters( diagram, oldDiagram.getRole( EModel.class ) );
                transformUnits(diagram);
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't convert diagram: ", t);
        }
        return diagram;
    }

    private boolean isEModel(Diagram diagram)
    {
        return ( diagram.getRole() != null ) && ( diagram.getRole() instanceof EModel );
    }

    protected void copyParameters(Diagram diagram, EModel oldModel)
    {
        EModel model = diagram.getRole(EModel.class);
        for(Variable var : oldModel.getParameters())
        {
            Variable newVar = (Variable)var.clone();
            var.setOrigin(model.getVariables());
            var.setParent(model);
            model.put(newVar);
        }
    }

    protected void transformUnits(Diagram diagram) throws Exception
    {
        EModel emodel = diagram.getRole( EModel.class );
        Set<String> unitsSet = StreamEx.ofValues( emodel.getUnits() ).map( Unit::getTitle ).toSet();

        DataCollection<Unit> unitsDC = null;
        Module module = Module.optModule(diagram);
        if( module != null && module.get(Module.METADATA) != null )
        {
            DataCollection<?> dictionaries = (DataCollection<?>)module.get(Module.METADATA);
            unitsDC = (DataCollection<Unit>)dictionaries.get(StandardModuleType.UNIT);
        }

        DataCollection<Variable> variables = emodel.getVariables();
        if( variables != null )
        {
            for( Variable var : variables )
            {
                String varUnits = var.getUnits();
                if( varUnits != null && !varUnits.isEmpty() && !Unit.getBaseUnitsList().contains(varUnits) && !unitsSet.contains(varUnits) )
                {
                    if( unitsDC != null )
                    {
                        Unit unit = null;
                        for( Unit de : unitsDC )
                        {
                            if( de.getTitle().equals(varUnits) )
                            {
                                unit = de;
                                break;
                            }
                        }
                        if( unit != null )
                        {
                            Unit newUnit = unit.clone(null, unit.getTitle());
                            emodel.addUnit(newUnit);
//                            emodelUnits.add(newUnit);
                            unitsSet.add(varUnits);
                        }
                        else
                        {
                            var.setUnits(Unit.UNDEFINED);
                            log.info("All units except the table units must be declared in the units data collection. Unknown units "
                                    + varUnits + " was remove.");
                        }
                    }
                    else
                    {
                        var.setUnits(Unit.UNDEFINED);
                        log.info("SbmlConverter: can not convert units " + varUnits + ". Units data collection is null.");
                    }
                }
            }
        }
    }

    protected void transformNodesAndEdges(Compartment newCompartment, Compartment oldCompartment) throws Exception
    {
        List<DiagramElement> elements = new ArrayList<>( DataCollectionUtils.asCollection( oldCompartment,
                DiagramElement.class ) );
        for( DiagramElement de : elements )
        {
            if( de instanceof Node )
                transformNode((Node)de, newCompartment);
        }
        for( DiagramElement de : elements )
        {
            if( de instanceof Edge )
                transformEdge((Edge)de, newCompartment, oldCompartment);
        }
    }

    protected void transformNode(Node node, Compartment newCompartment) throws Exception
    {
        Base kernel = node.getKernel();

        //exit if this node shouldn't be transformed
        if( kernel instanceof Specie )
        {
            if( node instanceof Compartment )
            {
                Specie newKernel = new Specie(null, kernel.getName());
                copyAttributes((Specie)kernel, newKernel);
                Node newNode = new Node(newCompartment, newKernel);
                fillNodeFields(newNode, node);
                newCompartment.put(newNode);
            }
            else
                newCompartment.put( node.clone( newCompartment, node.getName() ) );
            return;
        }

        if( kernel instanceof Reaction )
        {
            Reaction newKernel = new Reaction(null, kernel.getName());
            copyAttributes((Reaction)kernel, newKernel);
            fillMainProperties((Reaction)kernel, newKernel);
            newKernel.setFast( ( (Reaction)kernel ).isFast());
            KineticLaw kineticLaw = ( (Reaction)kernel ).getKineticLaw();
            newKernel.getKineticLaw().setComment(kineticLaw.getComment());
            newKernel.getKineticLaw().setFormula(kineticLaw.getFormula());
            newKernel.getKineticLaw().setSubstanceUnits(kineticLaw.getSubstanceUnits());
            newKernel.getKineticLaw().setTimeUnits(kineticLaw.getTimeUnits());

            Node newNode = new Node(newCompartment, newKernel);
            fillNodeFields(newNode, node);
            newCompartment.put(newNode);

            Edge[] edges = node.getEdges();
            for( Edge edge : edges )
            {
                if( ( edge.getKernel() != null ) && ( edge.getKernel() instanceof SpecieReference )
                        && ( (Reaction)kernel ).contains(edge.getKernel()) )
                {
                    SpecieReference spRef = (SpecieReference)edge.getKernel();
                    SpecieReference newSpRef = spRef.clone(spRef.getOrigin(), spRef.getName());
                    newKernel.put(newSpRef);

                    Node element = SpecieReference.PRODUCT.equals(spRef.getRole())? edge.getOutput():  edge.getInput();

                    String newSpecie = element.getCompleteNameInDiagram();
                    newSpRef.setSpecie( newSpecie );
                }
            }
            return;
        }
        else if( kernel instanceof Stub && kernel.getType().equals(Type.TYPE_REACTION) )
        {
            Object completeNameObj = node.getAttributes().getValue(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY);
            if( completeNameObj != null )
            {
                DataElement de = CollectionFactory.getDataElement(completeNameObj.toString());
                if( de instanceof Reaction )
                    kernel = (Reaction)de;
            }

            Reaction newKernel = new Reaction(null, kernel.getName());
            copyAttributes((Reaction)kernel, newKernel);
            fillMainProperties((Reaction)kernel, newKernel);
            newKernel.setFast( ( (Reaction)kernel ).isFast());
            KineticLaw kineticLaw = ( (Reaction)kernel ).getKineticLaw();
            newKernel.getKineticLaw().setComment(kineticLaw.getComment());
            newKernel.getKineticLaw().setFormula(kineticLaw.getFormula());
            newKernel.getKineticLaw().setSubstanceUnits(kineticLaw.getSubstanceUnits());
            newKernel.getKineticLaw().setTimeUnits(kineticLaw.getTimeUnits());

            Node newNode = new Node(newCompartment, newKernel);
            fillNodeFields(newNode, node);
            newCompartment.put(newNode);

            Edge[] edges = node.getEdges();
            for( Edge edge : edges )
            {
                if( ( edge.getKernel() != null ) && ( edge.getKernel() instanceof SpecieReference ) )
                //&& ( (Reaction)kernel ).contains(edge.getKernel()) )
                {
                    SpecieReference spRef = (SpecieReference)edge.getKernel();
                    SpecieReference newSpRef = spRef.clone(spRef.getOrigin(), spRef.getName());
                    newKernel.put(newSpRef);

                    Node element = SpecieReference.PRODUCT.equals(spRef.getRole())? edge.getOutput(): edge.getInput();

                    String newSpecie = element.getCompleteNameInDiagram();
                    newSpRef.setSpecie( newSpecie );
                }
            }
            return;
        }

        Role role = node.getRole();
        if( role != null && ( ( role instanceof Event ) || ( role instanceof Equation ) || ( role instanceof Function ) ) )
        {
            String newName = node.getName().replace("-", "_");
            Node newNode = node.clone(newCompartment, newName);
            newNode = (Node)Diagram.getDiagram( newNode ).getType().getSemanticController().validate( newCompartment, newNode );
            newCompartment.put(newNode);
            return;
        }

        //process compartments
        if( kernel.getType().equals(Type.TYPE_COMPARTMENT) )
        {
            biouml.standard.type.Compartment newKernel = new biouml.standard.type.Compartment(null, kernel.getName());
            copyAttributes((BaseSupport)kernel, newKernel);
            fillMainProperties((BaseSupport)kernel, newKernel);

            Compartment newNode = new Compartment(newCompartment, newKernel);
            newNode = (Compartment)Diagram.getDiagram( newNode ).getType().getSemanticController().validate( newCompartment, newNode );
            newNode.setShapeSize(node.getShapeSize());
            newNode.setShapeType( ( (Compartment)node ).getShapeType());
            fillNodeFields(newNode, node);
            newCompartment.put(newNode);
            transformNodesAndEdges(newNode, (Compartment)node);
            return;
        }
        else if (kernel.getType().equals( Type.TYPE_BLOCK ))
        {
            Compartment c = (Compartment)node;
            for (Node n: c.getNodes())
                this.transformNode( n, newCompartment );
        }

        if( shouldNodeTransformed(kernel) )
        {
            Specie newSpecie = new Specie(null, kernel.getName());
            fillNodeKernelFields(newSpecie, kernel);
            Node newNode = new Node(newCompartment, newSpecie);
            newNode = (Node)Diagram.getDiagram( newNode ).getType().getSemanticController().validate( newCompartment, newNode );
            fillNodeFields(newNode, node);
            
            newCompartment.put(newNode);
        }
    }

    protected void transformEdge(Edge edge, Compartment newCompartment, Compartment oldCompartment) throws Exception
    {
        Diagram diagram = Diagram.getDiagram(newCompartment);
        Node inputSpecieNode = diagram.findNode(edge.getInput().getCompleteNameInDiagram());
        Node outputSpecieNode = diagram.findNode(edge.getOutput().getCompleteNameInDiagram());

        if( inputSpecieNode == null || outputSpecieNode == null )
            return;

        Reaction reactionKernel = null;
        Node specieReferenceNode = null;
        if( inputSpecieNode.getKernel() instanceof Reaction )
        {
            reactionKernel = (Reaction)inputSpecieNode.getKernel();
            specieReferenceNode = outputSpecieNode;
        }
        else if( outputSpecieNode.getKernel() instanceof Reaction )
        {
            reactionKernel = (Reaction)outputSpecieNode.getKernel();
            specieReferenceNode = inputSpecieNode;
        }
        else
            return; // TODO: throw an exception?
        if( reactionKernel != null )
        {
            SpecieReference kernel = null;
            for( SpecieReference spRef : reactionKernel )
            {
                String specie = spRef.getSpecie();
                if( specieReferenceNode.getCompleteNameInDiagram().equals(specie) )
                {
                    kernel = spRef;
                    break;
                }
            }
            if(kernel == null)
            {
                // SpecieReference not found in reaction
                // This can occur in some strange cases like Transpath diagrams
                // Try to construct SpecieReference manually
                kernel = new SpecieReference( reactionKernel, ((SpecieReference)edge.getKernel()).getName(),
                        ((SpecieReference)edge.getKernel()).getRole());
                kernel.setSpecie( specieReferenceNode.getCompleteNameInDiagram() );
            }

            Edge newEdge = new Edge(newCompartment, edge.getName(), kernel, inputSpecieNode, outputSpecieNode);
            Role role = edge.getRole();
            if( role != null )
                newEdge.setRole(role.clone(newEdge));
            copyAttributes(edge, newEdge);
            newEdge.setComment(edge.getComment());
            newEdge.setTitle(edge.getTitle());
            newEdge.setPath(edge.getPath() != null ? edge.getPath() : new Path());

            inputSpecieNode.addEdge(newEdge);
            outputSpecieNode.addEdge(newEdge);
            newCompartment.put(newEdge);
            return;
        }

        Base kernel = edge.getKernel();
        if( shouldEdgeTransformed(kernel) )
        {
            Reaction newKernel = new Reaction(null, kernel.getName());

            SpecieReference input = new SpecieReference(null, kernel.getName() + "-input");
            input.setSpecie(inputSpecieNode.getName());
            input.setRole(SpecieReference.REACTANT);

            SpecieReference output = new SpecieReference(null, kernel.getName() + "-output");
            output.setSpecie(outputSpecieNode.getName());
            output.setRole(SpecieReference.PRODUCT);

            newKernel.setSpecieReferences(new SpecieReference[] {input, output});

            fillEdgeKernelFields(newKernel, kernel);

            Node newReaction = new Node(newCompartment, newKernel);
            Point inputLocation = inputSpecieNode.getLocation();
            Point outputLocation = outputSpecieNode.getLocation();
            newReaction.setLocation( ( inputLocation.x + outputLocation.x ) / 2, ( inputLocation.y + outputLocation.y ) / 2);
            newCompartment.put(newReaction);

            Edge inputEdge = new Edge(newCompartment, input, inputSpecieNode, newReaction);
            newCompartment.put(inputEdge);

            Edge outputEdge = new Edge(newCompartment, output, newReaction, outputSpecieNode);
            newCompartment.put(outputEdge);
        }
    }

    protected boolean shouldNodeTransformed(Base kernel)
    {
        for( String type : nodeKernelTypes )
        {
            if( kernel.getType().equals(type) )
                return true;
        }
        return false;
    }

    protected boolean shouldEdgeTransformed(Base kernel)
    {
        for( String type : edgeKernelTypes )
        {
            if( kernel.getType().equals(type) )
                return true;
        }
        return false;
    }

    protected DatabaseReference[] transformDatabaseReferences(Referrer kernel)
    {
        DatabaseReference[] references = kernel.getDatabaseReferences();
        ArrayList<DatabaseReference> newReferences = null;
        if( references != null )
        {
            try
            {
                newReferences = new ArrayList<>();

                for( DatabaseReference ref : references )
                {
                    Module module = Module.optModule(kernel);
                    if( module != null && module.get(Module.METADATA) != null )
                    {
                        DataCollection<?> metadata = (DataCollection<?>)module.get(Module.METADATA);
                        DataCollection<DatabaseInfo> databaseInfos = (DataCollection<DatabaseInfo>)metadata.get(StandardModuleType.DATABASE_INFO);
                        if( databaseInfos != null )
                        {
                            DatabaseInfo info = null;
                            String databaseName = ref.getDatabaseName();
                            if( databaseInfos.contains(databaseName) )
                            {
                                info = databaseInfos.get(databaseName);
                            }
                            else
                            {
                                for( DatabaseInfo intermediateInfo : databaseInfos )
                                {
                                    if( intermediateInfo.getTitle().equals(databaseName) )
                                    {
                                        info = intermediateInfo;
                                        break;
                                    }
                                }
                            }

                            if( info == null )
                            {
                                log.info("Database reference " + ref.getId() + " of element " + kernel.getName()
                                        + " was removed. Reason: database info " + databaseName + " is null.");
                                continue;
                            }
                            else if( info.getDatabaseReferences() == null )
                            {
                                log.info("Database reference " + ref.getId() + " of element " + kernel.getName()
                                        + " was removed. Reason: there is no match for " + info.getTitle() + " in Miriam database.");
                                continue;
                            }

                            DataCollection<DatabaseInfo> miriamCollection = (DataCollection<DatabaseInfo>)CollectionFactory.getDataElement(MIRIAM_RESOURCE);
                            if( miriamCollection != null )
                            {
                                for( DatabaseReference reference : info.getDatabaseReferences() )
                                {
                                    if( miriamCollection.contains(reference.getId()) )
                                    {
                                        info = miriamCollection.get(reference.getId());
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                log.log(Level.SEVERE, "Can not convert database references. Miriam database is null.");
                                return null;
                            }

                            if( info != null )
                            {
                                DatabaseReference newRef = new DatabaseReference(info.getName(), ref.getId());
                                newRef.setComment(ref.getComment());
                                String type = ref.getRelationshipType();

                                if( RdfExtensionReader.checkRelationshipType(type) )
                                {
                                    newRef.setRelationshipType(type);
                                    newReferences.add(newRef);
                                }
                                else
                                {
                                    log.info("Database reference " + ref.getId() + " of element " + kernel.getName()
                                            + " was removed. Reason: unknown relationship type.");
                                }
                            }
                        }
                        else
                        {
                            log.log(Level.SEVERE, "Can not transform database references. Reason: database infos collection is null.");
                            return null;
                        }
                    }
                }
            }
            catch( Throwable t )
            {
                t.printStackTrace();
            }
        }
        if( newReferences != null )
            return newReferences.toArray(new DatabaseReference[newReferences.size()]);

        return null;
    }

    protected String[] transformLiteratureReferences(Referrer kernel)
    {
        String[] references = kernel.getLiteratureReferences();
        ArrayList<String> newReferences = new ArrayList<>();
        if( references != null )
        {
            try
            {
                for( String ref : references )
                {
                    Module module = Module.optModule(kernel);
                    if( module != null && module.get(Module.DATA) != null )
                    {
                        DataCollection<?> data = (DataCollection<?>)module.get(Module.DATA);
                        DataCollection<Publication> literature = (DataCollection<Publication>)data.get( "literature" );
                        if( literature != null )
                        {
                            if( literature.contains(ref) )
                            {
                                Publication publication = literature.get(ref);
                                if( publication.getPubMedId() != null && !publication.getPubMedId().equals("")
                                        && !publication.getPubMedId().equals("0") )
                                {
                                    newReferences.add(publication.getPubMedId());
                                }
                                else
                                {
                                    log.info("Literature reference " + publication.getReference() + " of element " + kernel.getName()
                                            + " was removed. Reason: PubMedId of the publication is incorrect.");
                                }
                            }
                            else
                            {
                                log.log(Level.SEVERE, "The element " + ref + " does not exist in the literature database.");
                            }
                        }
                        else
                        {
                            log.log(Level.SEVERE, "Can not transform literature references. Reason: literature collection is null.");
                            return null;
                        }
                    }
                }
            }
            catch( Throwable t )
            {
                t.printStackTrace();
            }
        }
        return newReferences.toArray(new String[newReferences.size()]);
    }

    protected void fillNodeFields(Node newNode, Node node)
    {
        newNode.setLocation(node.getLocation());
        newNode.setTitle(node.getTitle());
        newNode.setComment(node.getComment());
        Role nodeRole = node.getRole();
        if( nodeRole != null )
        {
            newNode.setRole(nodeRole.clone(newNode));
        }
        copyAttributes(node, newNode);
    }
   
    public void fillNodeKernelFields(Specie newSpecie, Base oldSpecie)
    {
        fillNodeKernelFields(newSpecie, oldSpecie, true);
    }

    public void fillNodeKernelFields(Specie newSpecie, Base oldSpecie, boolean transformReferences)
    {
        if( oldSpecie instanceof BaseSupport )
        {
            copyAttributes((BaseSupport)oldSpecie, newSpecie);

            if( oldSpecie instanceof Referrer )
            {
                fillMainProperties((Referrer)oldSpecie, newSpecie, transformReferences);

                if( oldSpecie instanceof Substance )
                {
                    fillKernelFromSubstance(newSpecie, (Substance)oldSpecie);
                }
                else if( oldSpecie instanceof Protein )
                {
                    fillKernelFromProtein(newSpecie, (Protein)oldSpecie);
                }
                else if( oldSpecie instanceof RNA )
                {
                    fillKernelFromRNA(newSpecie, (RNA)oldSpecie);
                }
                else if( oldSpecie instanceof Gene )
                {
                    fillKernelFromGene(newSpecie, (Gene)oldSpecie);
                }
                else if( oldSpecie instanceof Concept )
                {
                    fillKernelFromConcept(newSpecie, (Concept)oldSpecie);
                }
                else if( oldSpecie instanceof Cell )
                {
                    fillKernelFromCell(newSpecie, (Cell)oldSpecie);
                }
            }
        }
    }

    protected void fillEdgeKernelFields(Reaction newReaction, Base oldSpecie)
    {
        if( oldSpecie instanceof SemanticRelation )
        {
            fillKernelFromSemanticRelation(newReaction, (SemanticRelation)oldSpecie);
        }
        else if( oldSpecie instanceof SpecieReference )
        {
            fillKernelFromSpecieReference(newReaction, (SpecieReference)oldSpecie);
        }
    }

    //
    // nodes transformations
    //
    protected void fillKernelFromSubstance(Specie newSpecie, Substance oldSpecie)
    {
        try
        {
            String completeName = oldSpecie.getCompleteName();
            if( completeName != null )
                newSpecie.getAttributes().add(new DynamicProperty(COMPLETE_NAME_ATTR, String.class, completeName));
            String casRegistryNumber = oldSpecie.getCasRegistryNumber();
            if( casRegistryNumber != null )
                newSpecie.getAttributes().add(new DynamicProperty(CAS_REGISTRY_NUMBER_ATTR, String.class, casRegistryNumber));
            String formula = oldSpecie.getFormula();
            if( formula != null )
                newSpecie.getAttributes().add(new DynamicProperty(FORMULA_ATTR, String.class, formula));
            String[] structureReferences = oldSpecie.getStructureReferences();
            if( structureReferences != null )
                newSpecie.getAttributes().add(new DynamicProperty(STRUCTURE_REFERENCES_ATTR, String[].class, structureReferences));
            String synonyms = oldSpecie.getSynonyms();
            if( synonyms != null )
                newSpecie.getAttributes().add(new DynamicProperty(SYNONYMS_ATTR, String[].class, synonyms));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    protected void fillKernelFromProtein(Specie newSpecie, Protein oldSpecie)
    {
        try
        {
            String completeName = oldSpecie.getCompleteName();
            if( completeName != null )
                newSpecie.getAttributes().add(new DynamicProperty(COMPLETE_NAME_ATTR, String.class, completeName));
            String[] structureReferences = oldSpecie.getStructureReferences();
            if( structureReferences != null )
                newSpecie.getAttributes().add(new DynamicProperty(STRUCTURE_REFERENCES_ATTR, String[].class, structureReferences));
            String synonyms = oldSpecie.getSynonyms();
            if( synonyms != null )
                newSpecie.getAttributes().add(new DynamicProperty(SYNONYMS_ATTR, String[].class, synonyms));
            String functionalState = oldSpecie.getFunctionalState();
            if( functionalState != null )
                newSpecie.getAttributes().add(new DynamicProperty(FUNCTIONAL_STATE_ATTR, String.class, functionalState));
            String gene = oldSpecie.getGene();
            if( gene != null )
                newSpecie.getAttributes().add(new DynamicProperty(GENE_ATTR, String.class, gene));
            String modification = oldSpecie.getModification();
            if( modification != null )
                newSpecie.getAttributes().add(new DynamicProperty(MODIFICATION_ATTR, String.class, modification));
            String regulation = oldSpecie.getRegulation();
            if( regulation != null )
                newSpecie.getAttributes().add(new DynamicProperty(REGULATION_ATTR, String.class, regulation));
            String species = oldSpecie.getSpecies();
            if( species != null )
                newSpecie.getAttributes().add(new DynamicProperty(SPECIES_ATTR, String.class, species));
            String source = oldSpecie.getSource();
            if( source != null )
                newSpecie.getAttributes().add(new DynamicProperty(SOURCE_ATTR, String.class, source));
            newSpecie.setType(Type.TYPE_PROTEIN);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    protected void fillKernelFromRNA(Specie newSpecie, RNA oldSpecie)
    {
        try
        {
            String completeName = oldSpecie.getCompleteName();
            if( completeName != null )
                newSpecie.getAttributes().add(new DynamicProperty(COMPLETE_NAME_ATTR, String.class, completeName));
            String[] structureReferences = oldSpecie.getStructureReferences();
            if( structureReferences != null )
                newSpecie.getAttributes().add(new DynamicProperty(STRUCTURE_REFERENCES_ATTR, String[].class, structureReferences));
            String synonyms = oldSpecie.getSynonyms();
            if( synonyms != null )
                newSpecie.getAttributes().add(new DynamicProperty(SYNONYMS_ATTR, String[].class, synonyms));
            String gene = oldSpecie.getGene();
            if( gene != null )
                newSpecie.getAttributes().add(new DynamicProperty(GENE_ATTR, String.class, gene));
            String regulation = oldSpecie.getRegulation();
            if( regulation != null )
                newSpecie.getAttributes().add(new DynamicProperty(REGULATION_ATTR, String.class, regulation));
            String species = oldSpecie.getSpecies();
            if( species != null )
                newSpecie.getAttributes().add(new DynamicProperty(SPECIES_ATTR, String.class, species));
            String source = oldSpecie.getSource();
            if( source != null )
                newSpecie.getAttributes().add(new DynamicProperty(SOURCE_ATTR, String.class, source));
            String rnaType = oldSpecie.getRnaType();
            if( rnaType != null )
                newSpecie.getAttributes().add(new DynamicProperty(RNA_TYPE_ATTR, String.class, rnaType));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    protected void fillKernelFromGene(Specie newSpecie, Gene oldSpecie)
    {
        try
        {
            String completeName = oldSpecie.getCompleteName();
            if( completeName != null )
                newSpecie.getAttributes().add(new DynamicProperty(COMPLETE_NAME_ATTR, String.class, completeName));
            String[] structureReferences = oldSpecie.getStructureReferences();
            if( structureReferences != null )
                newSpecie.getAttributes().add(new DynamicProperty(STRUCTURE_REFERENCES_ATTR, String[].class, structureReferences));
            String synonyms = oldSpecie.getSynonyms();
            if( synonyms != null )
                newSpecie.getAttributes().add(new DynamicProperty(SYNONYMS_ATTR, String[].class, synonyms));
            String regulation = oldSpecie.getRegulation();
            if( regulation != null )
                newSpecie.getAttributes().add(new DynamicProperty(REGULATION_ATTR, String.class, regulation));
            String species = oldSpecie.getSpecies();
            if( species != null )
                newSpecie.getAttributes().add(new DynamicProperty(SPECIES_ATTR, String.class, species));
            String source = oldSpecie.getSource();
            if( source != null )
                newSpecie.getAttributes().add(new DynamicProperty(SOURCE_ATTR, String.class, source));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    protected void fillKernelFromConcept(Specie newSpecie, Concept oldSpecie)
    {
        try
        {
            String completeName = oldSpecie.getCompleteName();
            if( completeName != null )
                newSpecie.getAttributes().add(new DynamicProperty(COMPLETE_NAME_ATTR, String.class, completeName));
            String synonyms = oldSpecie.getSynonyms();
            if( synonyms != null )
                newSpecie.getAttributes().add(new DynamicProperty(SYNONYMS_ATTR, String[].class, synonyms));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    protected void fillKernelFromCell(Specie newSpecie, Cell oldSpecie)
    {
        try
        {
            String completeName = oldSpecie.getCompleteName();
            if( completeName != null )
                newSpecie.getAttributes().add(new DynamicProperty(COMPLETE_NAME_ATTR, String.class, completeName));
            String synonyms = oldSpecie.getSynonyms();
            if( synonyms != null )
                newSpecie.getAttributes().add(new DynamicProperty(SYNONYMS_ATTR, String[].class, synonyms));
            String species = oldSpecie.getSpecies();
            if( species != null )
                newSpecie.getAttributes().add(new DynamicProperty(SPECIES_ATTR, String.class, species));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    //
    // edges transformations
    //
    protected void fillKernelFromSemanticRelation(Reaction newReaction, SemanticRelation oldSpecie)
    {
        fillMainProperties(oldSpecie, newReaction);
        copyAttributes(oldSpecie, newReaction);

        try
        {
            String relationType = oldSpecie.getRelationType();
            if( relationType != null )
                newReaction.getAttributes().add(new DynamicProperty(RELATION_TYPE_ATTR, String.class, relationType));
            String inputElementName = oldSpecie.getInputElementName();
            if( inputElementName != null )
                newReaction.getAttributes().add(new DynamicProperty(INPUT_ELEMENT_NAME_ATTR, String.class, inputElementName));
            String outputElementName = oldSpecie.getOutputElementName();
            if( outputElementName != null )
                newReaction.getAttributes().add(new DynamicProperty(OUTPUT_ELEMENT_NAME_ATTR, String.class, outputElementName));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    protected void fillKernelFromSpecieReference(Reaction newReaction, SpecieReference oldSpecie)
    {
        newReaction.setComment(oldSpecie.getComment());
        newReaction.setTitle(oldSpecie.getTitle());
        newReaction.setDate(oldSpecie.getDate());
        copyAttributes(oldSpecie, newReaction);

        try
        {
            String formula = oldSpecie.getFormula();
            if( formula != null )
                newReaction.getAttributes().add(new DynamicProperty(FORMULA_ATTR, String.class, formula));
            String modifierAction = oldSpecie.getModifierAction();
            if( modifierAction != null )
                newReaction.getAttributes().add(new DynamicProperty(MODIFIER_ACTION_ATTR, String.class, modifierAction));
            String participation = oldSpecie.getParticipation();
            if( participation != null )
                newReaction.getAttributes().add(new DynamicProperty(PARTICIPATION_ATTR, String.class, participation));
            String role = oldSpecie.getRole();
            if( role != null )
                newReaction.getAttributes().add(new DynamicProperty(ROLE_ATTR, String.class, role));
            String specie = oldSpecie.getSpecie();
            if( specie != null )
                newReaction.getAttributes().add(new DynamicProperty(SPECIE_ATTR, String.class, specie));
            String stoichiometry = oldSpecie.getStoichiometry();
            if( stoichiometry != null )
                newReaction.getAttributes().add(new DynamicProperty(STOICHIOMETRY_ATTR, String.class, stoichiometry));
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "can't create attribute for specie", t);
        }
    }

    //
    // other methods
    //

    protected void copyAttributes(BaseSupport input, BaseSupport output)
    {
        for(DynamicProperty dp : input.getAttributes())
        {
            output.getAttributes().add(dp);
        }
    }

    protected void copyAttributes(DiagramElement input, DiagramElement output)
    {
        for(DynamicProperty dp : input.getAttributes())
            output.getAttributes().add(dp);
    }

    protected void fillMainProperties(BaseSupport input, Referrer output)
    {
        fillMainProperties(input, output, true);
    }
    
    protected void fillMainProperties(BaseSupport input, Referrer output, boolean transformReferences)
    {
        output.setTitle(input.getTitle());
        if( input instanceof GenericEntity )
        {
            output.setComment( ( (GenericEntity)input ).getComment());
            output.setDate( ( (GenericEntity)input ).getDate());
        }
        if( input instanceof Referrer )
        {
            output.setDescription( ( (Referrer)input ).getDescription());
            output.setDatabaseReferences(transformReferences? transformDatabaseReferences((Referrer)input): ((Referrer)input).getDatabaseReferences());//TODO: or should we clone references?
            output.setLiteratureReferences(transformReferences? transformLiteratureReferences((Referrer)input): ((Referrer)input).getLiteratureReferences());
        }
    }
}
