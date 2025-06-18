
package biouml.plugins.biopax.reader;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataPropertyExpression;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLOntology;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.TextUtil2;
import uk.ac.manchester.cs.owl.OWLDataPropertyImpl;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramViewOptions;
import biouml.plugins.biopax.access.BioPaxOwlDataCollection.BioPAXCollectionJobControl;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.Confidence;
import biouml.plugins.biopax.model.Evidence;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.biopax.model.SequenceFeature;
import biouml.plugins.biopax.model.SequenceInterval;
import biouml.plugins.biopax.model.SequenceSite;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DNA;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Structure;
import biouml.standard.type.Substance;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * @author anna
 *
 */
public class BioPAXReader_level2 extends BioPAXReader
{
    protected String namePrefix = "";
    public BioPAXReader_level2(OWLOntology ontology)
    {
        super(ontology);
    }

    public BioPAXReader_level2(OWLOntology ontology, DataCollection<DataCollection> data, DataCollection<Diagram> diagrams, DataCollection<DataCollection> dictionaries)
    {
        super(ontology);
        this.data = data;
        this.diagrams = diagrams;
        this.dictionaries = dictionaries;
        setInternalCollections();
    }

    public boolean read() throws Exception
    {
        return read("", null);
    }

    @Override
    public boolean read(String prefix, BioPAXCollectionJobControl jobControl) throws Exception
    {
        namePrefix = prefix;
        if( jobControl != null )
            jobControl.functionStarted();

        Set<OWLIndividual> individuals = ontology.getReferencedIndividuals();
        int totalCount = individuals.size();
        int currentCount = 0;
        int currentPercent = 0;

        for(OWLIndividual individual: individuals)
        {
            String type = getType(individual);
            if( type != null )
            {
                if( type.equals("pathway") )
                {
                    Diagram diagram = readDiagram(null, diagrams, individual);
                    diagrams.put(diagram);
                }
                else if( type.equals("complex") )
                {
                    parseComplex(individual);
                }
                else if( type.equals("protein") )
                {
                    parseProtein(individual);
                }
                else if( type.equals("rna") )
                {
                    parseRna(individual);
                }
                else if( type.equals("dna") )
                {
                    parseDna(individual);
                }
                else if( type.equals("smallMolecule") )
                {
                    parseSmallMolecule(individual);
                }
                else if( type.equals("physicalEntity") )
                {
                    parsePhysicalEntity(individual);
                }
                else if( isType(type, CONTROL_TYPES) )
                {
                    parseControl(individual, type);
                }
                else if( isType(type, CONVERSION_TYPES) )
                {
                    parseConversion(individual, type);
                }
                else if( isType(type, PHYSICAL_ENTITY_TYPES) )
                {
                    parsePhysicalEntityParticipant(individual, null, null);
                }
            }
            currentCount++;
            if( ( currentCount * 100 ) / totalCount > currentPercent )
            {
                currentPercent = currentCount * 100 / totalCount;
                if( jobControl != null )
                {
                    jobControl.setPreparedness(currentPercent);
                    if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                    {
                        return false;
                    }
                }
            }
        }
        if( jobControl != null )
            jobControl.functionFinished("complete");
        return true;
    }

    public Diagram readDiagram(String name, DataCollection<Diagram> origin, OWLIndividual pathwayIndividual)
    {
        String diagramName = name == null ? namePrefix + pathwayIndividual.toString() : name;
        Diagram diagram;
        if( origin != null && origin.contains(diagramName) )
        {
            return get(origin, diagramName);
        }
        DiagramInfo diagramInfo = new DiagramInfo(origin, diagramName);

        diagramInfo.setTitle(getTitleName(pathwayIndividual));
        diagramInfo.setComment(getComments(pathwayIndividual));
        diagramInfo.setDatabaseReferences(getDatabaseReference(pathwayIndividual, "XREF"));
        diagramInfo.setLiteratureReferences(getPublications(pathwayIndividual, "XREF"));

        DynamicPropertySet dps = diagramInfo.getAttributes();
        writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", pathwayIndividual));
        writeToDPS(dps, "Organism", String.class, getOrganism(pathwayIndividual));
        writeToDPS(dps, "Synonyms", String.class, getSynonyms(pathwayIndividual));
        writeToDPS(dps, "DataSource", String[].class, getDataSource(pathwayIndividual));

        DiagramType diagramType = getDiagramType();
        diagram = new Diagram(origin, diagramInfo, diagramType);
        if( diagram.getViewOptions() instanceof XmlDiagramViewOptions )
        {
            DynamicPropertySet options = ( (XmlDiagramViewOptions)diagram.getViewOptions() ).getOptions();
            options.setValue( "customTitleFont", new ColorFont( "Arial", 0, 12 ) );
            options.setValue( "nodeTitleFont", new ColorFont( "Arial", 0, 12 ) );
            options.setValue( "nodeTitleLimit", 100.0 );
        }
        diagram.setNotificationEnabled(false);

        List<String> components = parsePathway(pathwayIndividual);
        drawPathway(components, diagram);
        arrangeDiagram(diagram);
        writeToDPS(dps, "Components", String[].class, components.toArray(new String[components.size()]));
        diagram.setNotificationEnabled(true);

        return diagram;
    }

    private List<String> parsePathway(OWLIndividual individual)
    {
        List<String> components = new ArrayList<>();
        for(OWLIndividual ind: getProperties(individual, "PATHWAY-COMPONENTS"))
        {
            String type = getType(ind);
            if( type.equals("pathwayStep") )
            {
                String[] results = parsePathwayStep(ind);
                for( String result : results )
                {
                    components.add(result);
                }
            }
            else if( isType(type, CONTROL_TYPES) )
            {
                components.add(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(controls.getName())
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(parseControl(ind, type)));
            }
            else if( isType(type, CONVERSION_TYPES) )
            {
                components.add(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(conversions.getName())
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(parseConversion(ind, type)));
            }
        }

        return components;
    }

    private String[] parsePathwayStep(OWLIndividual individual)
    {
        List<String> result = new ArrayList<>();
        for(OWLIndividual ind: getProperties(individual, "STEP-INTERACTIONS"))
        {
            String type = getType(ind);
            if( isType(type, CONTROL_TYPES) )
            {
                result.add(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(controls.getName())
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(parseControl(ind, type)));
            }
            else if( isType(type, CONVERSION_TYPES) )
            {
                result.add(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(conversions.getName())
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(parseConversion(ind, type)));
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private String parseControl(OWLIndividual individual, String controlType)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        SemanticRelation control = get(controls, name);
        if( control == null )
        {
            control = new SemanticRelation(controls, name);
            control.setTitle(getShortName(individual));
            control.setRelationType(getStringProperty("CONTROL-TYPE", individual, controlType.toUpperCase()));
            control.setComment(getComments(individual));

            OWLIndividual ind = getProperty(individual, "CONTROLLER");
            if( ind != null )
            {
                String type = getType(ind);
                if( isType(type, PHYSICAL_ENTITY_TYPES) )
                {
                    SpecieReference specie = parsePhysicalEntityParticipant(ind, control, null);
                    control.setInputElementName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(participants.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(specie.getName()));
                }
                else if( isType(type, CONTROL_TYPES) )
                {
                    String inputName = parseControl(ind, type);
                    control.setInputElementName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(controls.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(inputName));
                }
                else if( isType(type, CONVERSION_TYPES) )
                {
                    String inputName = parseConversion(ind, type);
                    control.setInputElementName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(conversions.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(inputName));
                }
            }

            ind = getProperty(individual, "CONTROLLED");
            if( ind != null )
            {
                String type = getType(ind);
                if( isType(type, PHYSICAL_ENTITY_TYPES) )
                {
                    SpecieReference specie = parsePhysicalEntityParticipant(ind, control, null);
                    control.setOutputElementName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(participants.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(specie.getName()));
                }
                else if( isType(type, CONTROL_TYPES) )
                {
                    String outputName = parseControl(ind, type);
                    control.setOutputElementName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(controls.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(outputName));
                }
                else if( isType(type, CONVERSION_TYPES) )
                {
                    String outputName = parseConversion(ind, type);
                    control.setOutputElementName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(conversions.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + ru.biosoft.access.core.DataElementPath.escapeName(outputName));
                }
            }

            control.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
            control.setLiteratureReferences(getPublications(individual, "XREF"));

            DynamicPropertySet dps = control.getAttributes();
            writeToDPS(dps, "Type", String.class, controlType);
            writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
            writeToDPS(dps, "Synonyms", String.class, getSynonyms(individual));
            writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));
            writeToDPS(dps, "Evidence", Evidence.class, parseEvidence(getObjectProperty(individual, "EVIDENCE", "evidence")));
            OpenControlledVocabulary ocv = parseOpenControlledVocabulary(getObjectProperty(individual, "INTERACTION-TYPE", "openControlledVocabulary"));
            if( ocv != null )
            {
                writeToDPS(dps, "InteractionType", String.class, Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                        + ru.biosoft.access.core.DataElementPath.escapeName(vocabulary.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                        + ru.biosoft.access.core.DataElementPath.escapeName(ocv.getName()));
            }

            if( controlType.equals("catalysis") )
            {
                writeToDPS(dps, "Direction", String.class, getStringProperty("DIRECTION", individual));
            }

            controls.put(control);
        }

        return control.getName();
    }


    private String parseConversion(OWLIndividual individual, String conversionType)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        if(conversions.contains( name ))
            return name;
        Reaction conversion = new Reaction(conversions, name);

        conversion.setTitle(getShortName(individual));
        conversion.setComment(getComments(individual));

        EntryStream<String, String> stream = EntryStream.of( "PARTICIPANTS", SpecieReference.OTHER );
        if(!conversionType.equals("physicalInteraction"))
            stream = stream.prepend( "LEFT", SpecieReference.REACTANT, "RIGHT", SpecieReference.PRODUCT );

        SpecieReference[] specieReferences = stream
                .mapKeyValue( (String propertyName, String role) -> getPropertiesByTypes( individual, propertyName, PHYSICAL_ENTITY_TYPES )
                        .map( ind -> parsePhysicalEntityParticipant( ind, conversion, role ) ) )
                .flatMap( Function.identity() ).distinct().toArray( SpecieReference[]::new );

        conversion.setSpecieReferences(specieReferences);
        conversion.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
        conversion.setLiteratureReferences(getPublications(individual, "XREF"));

        DynamicPropertySet dps = conversion.getAttributes();
        writeToDPS(dps, "Type", String.class, conversionType);
        writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
        writeToDPS(dps, "Synonyms", String.class, getSynonyms(individual));
        writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));
        writeToDPS(dps, "Evidence", Evidence.class, parseEvidence(getObjectProperty(individual, "EVIDENCE", "evidence")));
        if( !conversionType.equals("physicalInteraction") )
        {
            writeToDPS(dps, "Spontaneous", String.class, getStringProperty("SPONTANEOUS", individual));
        }
        OpenControlledVocabulary ocv = parseOpenControlledVocabulary(getObjectProperty(individual, "INTERACTION-TYPE", "openControlledVocabulary"));
        if( ocv != null )
        {
            writeToDPS(dps, "InteractionType", String.class, Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                    + ru.biosoft.access.core.DataElementPath.escapeName(vocabulary.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                    + ru.biosoft.access.core.DataElementPath.escapeName(ocv.getName()));
        }

        if( conversionType.equals("biochemicalReaction") || conversionType.equals("transportWithBiochemicalReaction") )
        {
            writeToDPS(dps, "DeltaG", String.class, getStringProperty("DELTA-G", individual));
            writeToDPS(dps, "DeltaH", String.class, getStringProperty("DELTA-H", individual));
            writeToDPS(dps, "DeltaS", String.class, getStringProperty("DELTA-S", individual));
            writeToDPS(dps, "EcNumber", String[].class, getStringListProperty("EC-NUMBER", individual));
            writeToDPS(dps, "Keq", String.class, getStringProperty("KEQ", individual));
        }

        conversions.put(conversion);

        return conversion.getName();
    }

    private SpecieReference parsePhysicalEntityParticipant(OWLIndividual individual, BaseSupport parent, String role)
    {
        String pepType = getType(individual);
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        if( role != null && !role.equals(SpecieReference.OTHER) )
        {
            name += "_as_" + role;
        }
        SpecieReference sr = get(participants, name);
        if( sr == null )
        {
            Base specie = null;
            OWLIndividual entity = getProperty(individual, "PHYSICAL-ENTITY");
            if( entity != null )
            {
                String type = getType(entity);
                if( type.equals("complex") )
                {
                    specie = parseComplex(entity);
                }
                else if( type.equals("protein") )
                {
                    specie = parseProtein(entity);
                }
                else if( type.equals("rna") )
                {
                    specie = parseRna(entity);
                }
                else if( type.equals("dna") )
                {
                    specie = parseDna(entity);
                }
                else if( type.equals("smallMolecule") )
                {
                    specie = parseSmallMolecule(entity);
                }
                else if( type.equals("physicalEntity") )
                {
                    specie = parsePhysicalEntity(entity);
                }
            }
            sr = new SpecieReference(participants, name);
            sr.setParent(parent);
            sr.setComment(getComments(individual));
            sr.setStoichiometry(getStringProperty("STOICHIOMETRIC-COEFFICIENT", individual, "1"));
            sr.setRole(role == null ? SpecieReference.OTHER : role);

            DynamicPropertySet dps = sr.getAttributes();
            OpenControlledVocabulary ocv = parseOpenControlledVocabulary(getObjectProperty(individual, "CELLULAR-LOCATION", "openControlledVocabulary"));
            if( ocv != null )
            {
                writeToDPS(dps, "CellularLocation", String.class, Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                        + ru.biosoft.access.core.DataElementPath.escapeName(vocabulary.getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                        + ru.biosoft.access.core.DataElementPath.escapeName(ocv.getName()));
            }
            writeToDPS(dps, "Type", String.class, pepType);

            if( pepType.equals("sequenceParticipant") )
            {
                writeToDPS(dps, "FeatureList", SequenceFeature[].class, getSequenceFeatures(individual));
            }

            if(specie != null)
            {
                sr.setSpecie(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(specie.getOrigin().getName())
                        + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(specie.getName()));
                sr.setTitle(specie.getTitle());
                if( specie instanceof Referrer )
                {
                    addMatchingLink(name, ( (Referrer)specie ).getDatabaseReferences(), false);
                    addMatchingLink(specie.getName(), ( (Referrer)specie ).getDatabaseReferences(), true);
                }
            }

            participants.put(sr);
        }
        return sr;
    }

    private Base parsePhysicalEntity(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Concept entity = get(physicalEntities, name);
        if( entity == null )
        {
            entity = new Concept(physicalEntities, name);
            entity.setCompleteName(getName(individual));
            entity.setTitle(getShortName(individual));
            entity.setSynonyms(getSynonyms(individual));
            entity.setComment(getComments(individual));
            entity.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
            entity.setLiteratureReferences(getPublications(individual, "XREF"));

            DynamicPropertySet dps = entity.getAttributes();
            writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
            writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));

            physicalEntities.put(entity);
        }

        return entity;
    }

    private Base parseComplex(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        if(complexes.contains( name ))
            return get(complexes, name);
        Complex complex = new Complex(complexes, name);
        complex.setCompleteName(getName(individual));
        complex.setTitle(getShortName(individual));
        complex.setSynonyms(getSynonyms(individual));
        complex.setComment(getComments(individual));
        complex.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
        complex.setLiteratureReferences(getPublications(individual, "XREF"));

        DynamicPropertySet dps = complex.getAttributes();
        writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
        writeToDPS(dps, "Organism", String.class, getOrganism(individual));
        writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));

        String[] components = getPropertiesByTypes(individual, "COMPONENTS", PHYSICAL_ENTITY_TYPES)
            .map( ind -> parsePhysicalEntityParticipant(ind, complex, null) )
            .map( specie -> Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                    + ru.biosoft.access.core.DataElementPath.escapeName(specie.getOrigin().getName()) + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                    + ru.biosoft.access.core.DataElementPath.escapeName(specie.getName()) )
            .distinct().toArray( String[]::new );
        if(components.length > 0)
            complex.setComponents(components);
        complexes.put(complex);
        return complex;
    }

    private Base parseProtein(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Protein protein = get(proteins, name);
        if( protein == null )
        {
            protein = new Protein(proteins, name);
            protein.setCompleteName(getName(individual));
            protein.setTitle(getShortName(individual));
            protein.setSynonyms(getSynonyms(individual));
            protein.setComment(getComments(individual));
            protein.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
            protein.setLiteratureReferences(getPublications(individual, "XREF"));

            DynamicPropertySet dps = protein.getAttributes();
            writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
            writeToDPS(dps, "Organism", String.class, getOrganism(individual));
            writeToDPS(dps, "Sequence", String.class, getStringProperty("SEQUENCE", individual));
            writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));

            proteins.put(protein);
        }
        return protein;
    }

    private Base parseRna(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        RNA rna = get(rnas, name);
        if( rna == null )
        {
            rna = new RNA(rnas, name);
            rna.setCompleteName(getName(individual));
            rna.setTitle(getShortName(individual));
            rna.setSynonyms(getSynonyms(individual));
            rna.setComment(getComments(individual));
            rna.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
            rna.setLiteratureReferences(getPublications(individual, "XREF"));

            DynamicPropertySet dps = rna.getAttributes();
            writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
            writeToDPS(dps, "Organism", String.class, getOrganism(individual));
            writeToDPS(dps, "Sequence", String.class, getStringProperty("SEQUENCE", individual));
            writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));

            rnas.put(rna);
        }

        return rna;
    }

    private Base parseDna(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        DNA dna = get(dnas, name);
        if( dna == null )
        {
            dna = new DNA(dnas, name);
            dna.setCompleteName(getName(individual));
            dna.setTitle(getShortName(individual));
            dna.setSynonyms(getSynonyms(individual));
            dna.setComment(getComments(individual));
            dna.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
            dna.setLiteratureReferences(getPublications(individual, "XREF"));

            DynamicPropertySet dps = dna.getAttributes();
            writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
            writeToDPS(dps, "Organism", String.class, getOrganism(individual));
            writeToDPS(dps, "Sequence", String.class, getStringProperty("SEQUENCE", individual));
            writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));

            dnas.put(dna);
        }

        return dna;
    }

    private Base parseSmallMolecule(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        if(smallMolecules.contains( name ))
            return get(smallMolecules, name);
        Substance smallMolecule = new Substance(smallMolecules, name);
        smallMolecule.setCompleteName(getName(individual));
        smallMolecule.setTitle(getShortName(individual));
        smallMolecule.setSynonyms(getSynonyms(individual));
        smallMolecule.setComment(getComments(individual));
        smallMolecule.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
        smallMolecule.setLiteratureReferences(getPublications(individual, "XREF"));

        DynamicPropertySet dps = smallMolecule.getAttributes();
        writeToDPS(dps, "Availability", String.class, getStringProperty("AVAILABILITY", individual));
        writeToDPS(dps, "DataSource", String[].class, getDataSource(individual));
        writeToDPS(dps, "ChemicalFormula", String.class, getStringProperty("CHEMICAL-FORMULA", individual));
        writeToDPS(dps, "MolecularWeight", String.class, getStringProperty("MOLECULAR-WEIGHT", individual));

        getPropertiesByTypes(individual, "STRUCTURE", "chemicalStructure")
            .map( this::parseChemicalStructure )
            .forEach( structure -> writeToDPS(dps, "ChemicalStructure", Structure.class, structure) );

        smallMolecules.put(smallMolecule);

        return smallMolecule;
    }

    private Structure parseChemicalStructure(OWLIndividual individual)
    {
        String name = DiagramUtility.validateName( namePrefix + individual.toString() );
        Structure cs = new Structure(null, name);

        cs.setData(getStringProperty("STRUCTURE-DATA", individual));
        cs.setFormat(getStringProperty("STRUCTURE-FORMAT", individual));
        cs.setComment(getComments(individual));

        return cs;
    }

    private DatabaseReference parseDatabaseReference(OWLIndividual individual)
    {
        String type = getType(individual);
        DatabaseReference dr = new DatabaseReference();

        dr.setComment(getComments(individual));
        dr.setDatabaseVersion(getStringProperty("DB-VERSION", individual));
        dr.setDatabaseName(getStringProperty("DB", individual));
        dr.setIdVersion(getStringProperty("ID-VERSION", individual));
        dr.setId(getStringProperty("ID", individual));
        if( type.equals("unificationXref") )
        {
            dr.setRelationshipType(type);
        }
        else
        {
            String rt = getStringProperty("RELATIONSHIP-TYPE", individual);
            if( rt != null )
            {
                dr.setRelationshipType(getStringProperty("RELATIONSHIP-TYPE", individual));
            }
            else
            {
                dr.setRelationshipType(type);
            }
        }

        return dr;
    }

    private Publication parsePublication(OWLIndividual individual)
    {
        String name = namePrefix + individual.toString();
        if( publications.contains(name) )
        {
            return get(publications, name);
        }

        Publication pb = new Publication(publications, name);

        pb.setAuthors(getAuthors(individual));

        pb.setComment(getComments(individual));
        String year = getStringProperty("YEAR", individual);
        if( year != null )
        {
            pb.setYear(year);
        }
        pb.setTitle(getStringProperty("TITLE", individual));
        pb.setFullTextURL(getStringProperty("URL", individual));
        pb.setDb(getStringProperty("DB", individual));
        pb.setDbVersion(getStringProperty("DB-VERSION", individual));
        pb.setIdName(getStringProperty("ID", individual));
        pb.setIdVersion(getStringProperty("ID-VERSION", individual));
        pb.setSimpleSource(getStringProperty("SOURCE", individual));
        if(pb.getDb() != null && pb.getDb().equals("PubMed"))
            pb.setPubMedId(pb.getIdName());

        publications.put(pb);

        return pb;
    }
    private BioSource parseBioSource(OWLIndividual individual)
    {
        String name = namePrefix + individual.toString();
        BioSource bs = get(organisms, name);
        if( bs == null )
        {
            bs = new BioSource(null, name);
            bs.setCompleteName(getName(individual));
            bs.setComment(getComments(individual));
            bs.setTissue(getStringProperty("TISSUE", individual));
            bs.setDatabaseReferences(getDatabaseReference(individual, "TAXON-XREF"));
            bs.setLiteratureReferences(getPublications(individual, "TAXON-XREF"));
            bs.setCelltype(getStringProperty("CELLTYPE", individual));

            organisms.put(bs);
        }
        return bs;
    }

    private DatabaseInfo parseDataSource(OWLIndividual individual)
    {
        String name = namePrefix + getName(individual);
        DatabaseInfo di = get(dataSources, name);
        if( di == null )
        {
            di = new DatabaseInfo(null, name);
            di.setComment(getComments(individual));

            dataSources.put(di);
        }
        return di;
    }

    private OpenControlledVocabulary parseOpenControlledVocabulary(OWLIndividual individual)
    {
        if(individual == null) return null;
        String name = namePrefix + individual.toString();
        OpenControlledVocabulary sp = get(vocabulary, name);
        if( sp == null )
        {
            sp = new OpenControlledVocabulary(null, name);
            sp.setComment(getComments(individual));
            sp.setTerm(getStringProperty("TERM", individual));
            sp.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
            sp.setLiteratureReferences(getPublications(individual, "XREF"));

            vocabulary.put(sp);
        }
        return sp;
    }

    private Evidence parseEvidence(OWLIndividual individual)
    {
        if(individual == null) return null;
        String name = namePrefix + individual.toString();
        Evidence ev = new Evidence(null, name);
        ev.setComment(getComments(individual));
        ev.setConfidence(parseConfidence(getObjectProperty(individual, "CONFIDENCE", "confidence")));
        OpenControlledVocabulary ocv = parseOpenControlledVocabulary(getObjectProperty(individual, "EVIDENCE-CODE", "openControlledVocabulary"));
        if( ocv != null )
        {
            ev.setEvidenceCode(Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(vocabulary.getName())
                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(ocv.getName()));
        }

        ev.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
        ev.setLiteratureReferences(getPublications(individual, "XREF"));

        return ev;
    }

    private Confidence parseConfidence(OWLIndividual individual)
    {
        if(individual == null) return null;
        String name = namePrefix + individual.toString();
        Confidence ev = new Confidence(null, name);
        ev.setComment(getComments(individual));
        ev.setConfidenceValue(getStringProperty("CONFIDENCE-VALUE", individual));
        ev.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
        ev.setLiteratureReferences(getPublications(individual, "XREF"));

        return ev;
    }

    private SequenceFeature parseSequenceFeature(OWLIndividual individual)
    {
        SequenceFeature sf = new SequenceFeature(null, namePrefix + individual.toString());
        sf.setComment(getComments(individual));
        sf.setTitle(getShortName(individual));
        OpenControlledVocabulary ocv = parseOpenControlledVocabulary(getObjectProperty(individual, "FEATURE-TYPE", "openControlledVocabulary"));
        if( ocv != null )
        {
            sf.setFeatureType(Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(vocabulary.getName())
                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(ocv.getName()));
        }
        sf.setDatabaseReferences(getDatabaseReference(individual, "XREF"));
        sf.setLiteratureReferences(getPublications(individual, "XREF"));
        sf.setSynonyms(getSynonyms(individual));
        sf.setFeatureLocation(parseSequenceInterval(getObjectProperty(individual, "FEATURE-LOCATION", "sequenceInterval")));

        return sf;
    }

    private SequenceInterval parseSequenceInterval(OWLIndividual individual)
    {
        if(individual == null) return null;
        SequenceInterval si = new SequenceInterval(null, namePrefix + getName(individual));
        si.setComment(getComments(individual));
        si.setBegin(parseSequenceSite(getObjectProperty(individual, "SEQUENCE-INTERVAL-BEGIN", "sequenceSite")));
        si.setEnd(parseSequenceSite(getObjectProperty(individual, "SEQUENCE-INTERVAL-END", "sequenceSite")));

        return si;
    }

    private SequenceSite parseSequenceSite(OWLIndividual individual)
    {
        if(individual == null) return null;
        SequenceSite ss = new SequenceSite(null, namePrefix + getName(individual));
        ss.setComment(getComments(individual));
        ss.setPositionStatus(getStringProperty("POSITION-STATUS", individual));
        ss.setSequencePosition(getStringProperty("SEQUENCE-POSITION", individual));

        return ss;
    }

    ////////////////////////////////////////////////////////////////////////////
    // get properties from individuals
    //

    private String getName(OWLIndividual individual)
    {
        return getStringProperty("NAME", individual, individual.toString()).replaceAll("[/:]", "_");
    }

    private String getShortName(OWLIndividual individual)
    {
        return getStringProperty("SHORT-NAME", individual, getName(individual));
    }

    protected String getTitleName(OWLIndividual individual)
    {
        return getShortName(individual);
    }

    private String getOrganism(OWLIndividual individual)
    {
        return getPropertiesByTypes(individual, "ORGANISM", "bioSource").map( this::parseBioSource )
            .map( bs -> Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(organisms.getName())
                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(bs.getName()) )
                    .findAny().orElse( null );
    }

    private String getStringProperty(String propertyName, OWLIndividual individual)
    {
        return getStringProperty(propertyName, individual, null);
    }

    private String getStringProperty(String propertyName, OWLIndividual individual, String defaultValue)
    {
        OWLDataPropertyExpression ode = new OWLDataPropertyImpl(factory, createURI( propertyName ));
        Set<OWLConstant> param = individual.getDataPropertyValues(ontology).get(ode);
        if( param != null )
        {
            return toString(param.iterator().next());
        }
        return defaultValue;
    }

    private String[] getStringListProperty(String propertyName, OWLIndividual individual)
    {
        OWLDataPropertyExpression ode = new OWLDataPropertyImpl(factory, createURI(propertyName));
        Set<OWLConstant> param = individual.getDataPropertyValues(ontology).get(ode);
        if( param != null )
        {
            return param.stream().map(BioPAXReader::toString).sorted().distinct().toArray( String[]::new );
        }
        return new String[0];
    }

    private String getComments(OWLIndividual individual)
    {
        OWLDataPropertyExpression ope = new OWLDataPropertyImpl(factory, createURI("COMMENT"));
        Set<OWLConstant> param = individual.getDataPropertyValues(ontology).get(ope);
        if( param != null )
        {
            return join(param.stream().map( BioPAXReader::toString )
                    .map( str -> str.replaceAll( "[^a-zA-Z0-9!@#$%^&*()\\?;=+',-:_<>.\"/\\\\]", " " ) )
                    .filter( TextUtil2::nonEmpty )
                    .sorted().toArray( String[]::new )).trim();
        }
        return "";
    }

    private String getAuthors(OWLIndividual individual)
    {
        OWLDataPropertyExpression ope = new OWLDataPropertyImpl(factory, createURI("AUTHORS"));
        Set<OWLConstant> param = individual.getDataPropertyValues(ontology).get(ope);
        if( param != null )
        {
            return join(param.stream().map( BioPAXReader::toString )
                    .sorted().toArray( String[]::new )).trim();
        }
        return "";
    }

    private String getSynonyms(OWLIndividual individual)
    {
        OWLDataPropertyExpression ope = new OWLDataPropertyImpl(factory, createURI("SYNONYMS"));
        Set<OWLConstant> param = individual.getDataPropertyValues(ontology).get(ope);
        if( param != null )
        {
            return StreamEx.of(param).map( BioPAXReader::toString ).joining( "; " );
        }
        return "";
    }

    private DatabaseReference[] getDatabaseReference(OWLIndividual individual, String tag)
    {
        DatabaseReference[] xrefs = getPropertiesByTypes( individual, tag, "xref", "unificationXref", "relationshipXref" ).map(
                this::parseDatabaseReference ).toArray( DatabaseReference[]::new );
        return xrefs.length == 0 ? null : xrefs;
    }

    private String[] getPublications(OWLIndividual individual, String tag)
    {
        String[] xrefs = getPropertiesByTypes(individual, tag, "publicationXref").map( this::parsePublication )
            .map( pb -> Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(pb.getOrigin().getName())
                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(pb.getName()) ).toArray( String[]::new );
        return xrefs.length == 0 ? null : xrefs;
    }

    private String[] getDataSource(OWLIndividual individual)
    {
        return getPropertiesByTypes(individual, "DATA-SOURCE", "dataSource")
        .map( this::parseDataSource )
        .map( ds -> Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(dataSources.getName())
                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(ds.getName()) )
                    .sorted().distinct().toArray( String[]::new );
    }

    private SequenceFeature[] getSequenceFeatures(OWLIndividual individual)
    {
        SequenceFeature[] dis = getPropertiesByTypes(individual, "SEQUENCE-FEATURE-LIST", "sequenceFeature")
            .map( this::parseSequenceFeature ).toArray( SequenceFeature[]::new );
        return dis.length == 0 ? null : dis;
    }

    private OWLIndividual getObjectProperty(OWLIndividual individual, String name, String wantedType)
    {
        return getPropertiesByTypes(individual, name, wantedType).findAny().orElse( null );
    }

    ////////////////////////////////////////////////////////////////////////////
    // draw diargam
    //
    protected void drawPathway(List<String> components, Diagram diagram)
    {
        for(String component: components)
        {
            if( component.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(conversions.getName())) )
            {
                drawConversion(component, diagram);
            }
            else if( component.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(controls.getName())) )
            {
                drawControl(component, diagram);
            }
        }
    }

    protected Node drawConversion(String fullName, Diagram diagram)
    {
        Node node = (Node)diagram.get(DataElementPath.create(fullName).getName());
        if( node != null )
            return node;
        Reaction conversion = (Reaction)getDataElementByName(fullName);
        String title = conversion.getTitle();
        if( title == null )
            title = conversion.getName();
        node = new Node(diagram, conversion);
        
        String type = (String)conversion.getAttributes().getValue("Type");
        String sbgnType = type.equals("ComplexAssembly")? "association": type.equals("Degradation")? "dissociation": "process";
        node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_REACTION_TYPE_PD, String.class, sbgnType));
        
        node.setTitle(title);
        diagram.put(node);

        for( SpecieReference sr : conversion.getSpecieReferences() )
        {
            Edge edge = null;
            Node targetNode = drawNode(sr.getSpecie(), diagram);
            if( sr.getRole().equals(SpecieReference.PRODUCT) )
            {
                edge = new Edge(diagram, sr, node, targetNode);
            }
            else
            {
                edge = new Edge(diagram, sr, targetNode, node);
            }
            diagram.put(edge);
        }
        return node;
    }

    protected DiagramElement drawControl(String fullName, Diagram diagram)
    {
        DiagramElement de = diagram.get(DataElementPath.create(fullName).getName());
        if( de != null )
            return de;
        SemanticRelation control = (SemanticRelation)getDataElementByName(fullName);
        String inputName = control.getInputElementName();
        Node inputNode = null;
        if( inputName != null )
        {
            if( inputName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(participants.getName())) )
            {
                inputName = ( (SpecieReference)getDataElementByName(inputName) ).getSpecie();
            }
            inputNode = drawNode(inputName, diagram);
        }
        String outputName = control.getOutputElementName();
        Node outputNode = null;
        if( outputName != null )
        {
            if( outputName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(participants.getName())) )
            {
                outputName = ( (SpecieReference)getDataElementByName(outputName) ).getSpecie();
            }
            if( outputName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(conversions.getName())) )
            {
                outputNode = drawConversion(outputName, diagram);
            }
            else
            {
                outputNode = drawNode(outputName, diagram);
            }
        }
        if(inputNode != null && outputNode != null)
        {
            Edge edge = new Edge(diagram, control, inputNode, outputNode);
            diagram.put(edge);
            return edge;
        }
        return null;
    }

    protected @Nonnull Node drawNode(String fullName, Compartment diagram)
    {
        Node node = (Node)diagram.get(DataElementPath.create(fullName).getName());
        if( node != null )
            return node;

        Base base = (Base)getDataElementByName(fullName);

        String entityType = base.getOrigin().getName().equals(smallMolecules.getName())? "simple chemical": "macromolecule";      
        Specie newKernel = new Specie(null, base.getName(), entityType);
        
        node = new Compartment(diagram, newKernel);
        
        String title = base.getTitle();
        if( title == null )
            title = base.getName();
        node.setTitle(title);
//        addXmlNodeProperties(node);
        node.setShapeSize(new Dimension(0, 0));
        diagram.put(node);

        if( base instanceof Complex )
        {
            Complex complex = (Complex)base;
            String[] components = complex.getComponents();
            if( components != null )
            {
                for( String component : components )
                {
                    SpecieReference specie = (SpecieReference)getDataElementByName(component);
                    Node n1 = drawNode(specie.getSpecie(), (Compartment)node);
                    if(!specie.getStoichiometry().isEmpty() && !specie.getStoichiometry().equals("1"))
                    {
                        n1.getAttributes().add(
                                new DynamicProperty( SBGNPropertyConstants.SBGN_MULTIMER_PD, Integer.class, Integer.parseInt( specie
                                        .getStoichiometry() ) ) );
                    }
                }
            }
        }
        return node;
    }

    private static String[] PHYSICAL_ENTITY_TYPES = {"physicalEntityParticipant", "sequenceParticipant"};
    private static String[] CONVERSION_TYPES = {"conversion", "transport", "biochemicalReaction", "complexAssembly",
            "transportWithBiochemicalReaction", "physicalInteraction"};
    private static String[] CONTROL_TYPES = {"control", "catalysis", "modulation"};
}
