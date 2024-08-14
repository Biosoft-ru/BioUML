package biouml.plugins.biopax.writer;

import java.io.File;
import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.AddAxiom;
import org.semanticweb.owl.model.OWLAnnotation;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.TextUtil;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.biopax.BioPAXOntologyFormat;
import biouml.plugins.biopax.BioPAXOntologyStorer;
import biouml.plugins.biopax.BioPAXSupport;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.Confidence;
import biouml.plugins.biopax.model.Evidence;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.biopax.model.SequenceFeature;
import biouml.plugins.biopax.model.SequenceInterval;
import biouml.plugins.biopax.model.SequenceSite;
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
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Structure;
import biouml.standard.type.Substance;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class BioPAXWriter_level2 extends BioPAXWriter
{
    private static final Logger log = Logger.getLogger(BioPAXWriter_level2.class.getName());

    private String biopax = "http://www.biopax.org/release/biopax-level2.owl";
    private String levelTag = "biopax-level2";
    private DataCollection<?> primaryCollection;
    private DataElementPath primaryCollectionPath;
    protected File file;
    private String fileURI;

    protected Set<String> exportedIDs;

    public BioPAXWriter_level2()
    {
    }

    public BioPAXWriter_level2(Module module, String filename, FunctionJobControl jobControl)
    {
    }
    public DataElement getDataElementByName(String name) throws Exception
    {
        return primaryCollectionPath.getRelativePath(name).getDataElement();
    }

    @Override
    public void writeDiagram(Diagram diagram, File file)
    {
        try
        {
            manager = OWLManager.createOWLOntologyManager();

            exportedIDs = new TreeSet<>();

            URI ontologyURI = file.toURI();
            fileURI = ontologyURI.toString();

            ontology = manager.createOntology(ontologyURI);
            factory = manager.getOWLDataFactory();

            OWLAxiom imports = factory.getOWLImportsDeclarationAxiom(ontology, URI.create(biopax));
            manager.applyChange(new AddAxiom(ontology, imports));

            primaryCollection = diagram.getOrigin().getOrigin();

            if( primaryCollection != null )
            {
                exportPathway((DiagramInfo)diagram.getKernel());
                manager.addOntologyStorer(new BioPAXOntologyStorer(levelTag));
                manager.saveOntology(ontology, new BioPAXOntologyFormat());
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not write diagram", e);
        }
    }

    @Override
    public void write(Module module, File file, FunctionJobControl jobControl)
    {
        try
        {
            jobControl.functionStarted();
            manager = OWLManager.createOWLOntologyManager();

            URI ontologyURI = file.toURI();
            fileURI = ontologyURI.toString();

            ontology = manager.createOntology(ontologyURI);
            factory = manager.getOWLDataFactory();

            OWLAxiom imports = factory.getOWLImportsDeclarationAxiom(ontology, URI.create(biopax));
            manager.applyChange(new AddAxiom(ontology, imports));

            primaryCollection = module.getPrimaryCollection();
            primaryCollectionPath = DataElementPath.create(primaryCollection);
            DataCollection<?> data = (DataCollection<?>)primaryCollection.get(Module.DATA);
            DataCollection<SemanticRelation> controls = (DataCollection<SemanticRelation>)data.get(BioPAXSupport.CONTROL);
            DataCollection<Reaction> conversion = (DataCollection<Reaction>)data.get(BioPAXSupport.CONVERSION);
            DataCollection<Concept> physicalEntities = (DataCollection<Concept>)data.get(BioPAXSupport.PHYSICAL_ENTITY);
            DataCollection<Complex> complexes = (DataCollection<Complex>)data.get(BioPAXSupport.COMPLEX);
            DataCollection<Protein> proteins = (DataCollection<Protein>)data.get(BioPAXSupport.PROTEIN);
            DataCollection<RNA> rnas = (DataCollection<RNA>)data.get(BioPAXSupport.RNA);
            DataCollection<DNA> dnas = (DataCollection<DNA>)data.get(BioPAXSupport.DNA);
            DataCollection<Substance> smallMolecules = (DataCollection<Substance>)data.get(BioPAXSupport.SMALL_MOLECULE);
            DataCollection<SpecieReference> participants = (DataCollection<SpecieReference>)data.get(BioPAXSupport.PARTICIPANT);
            DataCollection<Diagram> diagrams = (DataCollection<Diagram>)primaryCollection.get(Module.DIAGRAM);

            exportedIDs = new TreeSet<>();

            boolean terminated = false;
            int totalCount = diagrams.getSize() * 10;
            totalCount += controls.getSize();
            totalCount += conversion.getSize();
            totalCount += physicalEntities.getSize();
            totalCount += complexes.getSize();
            totalCount += proteins.getSize();
            totalCount += rnas.getSize();
            totalCount += dnas.getSize();
            totalCount += smallMolecules.getSize();
            totalCount += participants.getSize();
            if( totalCount > 0 )
            {
                int currentCount = 0;
                int currentPercent = 0;
                for(Diagram di : diagrams)
                {
                    exportPathway((DiagramInfo)di.getKernel());
                    currentCount += 10;
                    if( ( currentCount * 100 ) / totalCount > currentPercent )
                    {
                        if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                        {
                            terminated = true;
                            break;
                        }
                        currentPercent = ( currentCount * 100 ) / totalCount;
                        jobControl.setPreparedness(currentPercent);
                    }
                }
                if( !terminated )
                {
                    for(Reaction de : conversion)
                    {
                        exportConversion(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for(SemanticRelation de : controls)
                    {
                        exportControl(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for(Concept de : physicalEntities)
                    {
                        exportConcept(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for(Complex de : complexes)
                    {
                        exportComplex(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for(Protein de : proteins)
                    {
                        exportProtein(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for(RNA de : rnas)
                    {
                        exportRna(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for(DNA de : dnas)
                    {
                        exportDna(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for( Substance de : smallMolecules )
                    {
                        exportSmallMolecule(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
                if( !terminated )
                {
                    for(SpecieReference de : participants)
                    {
                        exportPhysicalEntity(de);
                        currentCount++;
                        if( ( currentCount * 100 ) / totalCount > currentPercent )
                        {
                            if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                            {
                                terminated = true;
                                break;
                            }
                            currentPercent = ( currentCount * 100 ) / totalCount;
                            jobControl.setPreparedness(currentPercent);
                        }
                    }
                }
            }
            if( !terminated )
            {
                manager.addOntologyStorer(new BioPAXOntologyStorer(levelTag));
                manager.saveOntology(ontology, new BioPAXOntologyFormat());
            }
            jobControl.functionFinished("complete");
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not write BioPAX module", e);
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
        }
    }

    public void exportPathway(DiagramInfo di) throws Exception
    {
        String name = di.getName();
        if( exportedIDs.contains(name) )
            return;
        OWLClass pathway = factory.getOWLClass(URI.create(biopax + "#pathway"));
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, pathway);
        manager.applyChange(new AddAxiom(ontology, axiom));

        String shortName = di.getTitle();
        if( shortName == null )
            shortName = name;

        writeName(name, ind);
        writeShortName(shortName, ind);
        writeComments(di.getComment(), ind);
        writeDatabaseReferences(di.getDatabaseReferences(), ind, "XREF");
        writePublications(di.getLiteratureReferences(), ind, "XREF");

        DynamicPropertySet dps = di.getAttributes();
        writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
        writeSynonyms(dps.getValueAsString("Synonyms"), ind);
        writeDataSource((String[])dps.getValue("DataSource"), ind);
        writeOrganism(dps.getValueAsString("Organism"), ind);

        String[] components = (String[])dps.getValue("Components");
        if( components != null )
        {
            for( String componentName : components )
            {
                if( componentName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.CONVERSION) )
                {
                    Reaction conversion = (Reaction)getDataElementByName(componentName);
                    OWLIndividual result = exportConversion(conversion);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#PATHWAY-COMPONENTS"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
                else if( componentName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.CONTROL) )
                {
                    SemanticRelation control = (SemanticRelation)getDataElementByName(componentName);
                    OWLIndividual result = exportControl(control);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#PATHWAY-COMPONENTS"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
            }
        }
        exportedIDs.add(name);
    }

    public OWLIndividual exportControl(SemanticRelation kernel) throws Exception
    {
        DynamicPropertySet dps = kernel.getAttributes();
        String controlType = dps.getValueAsString("Type");
        if( controlType != null )
        {
            String name = kernel.getName();
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + controlType + "_" + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                OWLClass control = factory.getOWLClass(URI.create(biopax + "#" + controlType));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, control);
                manager.applyChange(new AddAxiom(ontology, axiom));

                writeName(name, ind);
                writeShortName(kernel.getTitle(), ind);
                writeComments(kernel.getComment(), ind);
                writeStringProperty("CONTROL-TYPE", kernel.getRelationType(), ind);
                writeDatabaseReferences(kernel.getDatabaseReferences(), ind, "XREF");
                writePublications(kernel.getLiteratureReferences(), ind, "XREF");

                DataElement input = getDataElementByName(kernel.getInputElementName());
                OWLIndividual result = null;
                if( input instanceof SemanticRelation )
                {
                    result = exportControl((SemanticRelation)input);
                }
                else if( input instanceof Reaction )
                {
                    result = exportConversion((Reaction)input);
                }
                else if( input instanceof SpecieReference )
                {
                    result = exportPhysicalEntity((SpecieReference)input);
                }
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#CONTROLLER"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));

                DataElement output = getDataElementByName(kernel.getOutputElementName());
                result = null;
                if( output instanceof SemanticRelation )
                {
                    result = exportControl((SemanticRelation)output);
                }
                else if( output instanceof Reaction )
                {
                    result = exportConversion((Reaction)output);
                }
                else if( output instanceof SpecieReference )
                {
                    result = exportPhysicalEntity((SpecieReference)output);
                }
                OWLObjectProperty component2 = factory.getOWLObjectProperty(URI.create(biopax + "#CONTROLLED"));
                assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component2, result);
                manager.applyChange(new AddAxiom(ontology, assertion));

                writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
                writeSynonyms(dps.getValueAsString("Synonyms"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);
                writeOCV(dps.getValueAsString("InteractionType"), ind, "INTERACTION-TYPE");
                if( controlType.equals("catalysis") )
                {
                    writeStringProperty("DIRECTION", dps.getValueAsString("Direction"), ind);
                }
                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }

    public OWLIndividual exportConversion(Reaction kernel) throws Exception
    {
        DynamicPropertySet dps = kernel.getAttributes();
        String conversionType = dps.getValueAsString("Type");
        if( conversionType != null )
        {
            String name = kernel.getName();
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + conversionType + "_" + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                OWLClass conversion = factory.getOWLClass(URI.create(biopax + "#" + conversionType));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, conversion);
                manager.applyChange(new AddAxiom(ontology, axiom));

                String shortName = kernel.getTitle();
                if( shortName == null )
                    shortName = name;

                writeName(name, ind);
                writeShortName(shortName, ind);
                writeComments(kernel.getComment(), ind);
                writeDatabaseReferences(kernel.getDatabaseReferences(), ind, "XREF");
                writePublications(kernel.getLiteratureReferences(), ind, "XREF");

                for( SpecieReference sr : kernel.getSpecieReferences() )
                {
                    SpecieReference spr = (SpecieReference)getDataElementByName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + "Participants" + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(sr.getName()));
                    if( sr.getRole().equals(SpecieReference.REACTANT) )//add to LEFT
                    {
                        OWLIndividual result = exportPhysicalEntity(spr);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#LEFT"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                    else if( sr.getRole().equals(SpecieReference.PRODUCT) )//add to RIGHT
                    {
                        OWLIndividual result = exportPhysicalEntity(spr);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#RIGHT"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                    else if( sr.getRole().equals(SpecieReference.OTHER) )//add to PARTICIPANTS
                    {
                        OWLIndividual result = exportPhysicalEntity(spr);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#PARTICIPANTS"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                }

                writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
                writeSynonyms(dps.getValueAsString("Synonyms"), ind);
                writeStringProperty("SPONTANEOUS", dps.getValueAsString("Spontaneous"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);
                writeOCV(dps.getValueAsString("InteractionType"), ind, "INTERACTION-TYPE");

                if( conversionType.equals("biochemicalReaction") || conversionType.equals("transportWithBiochemicalReaction") )
                {
                    writeStringProperty("DELTA-G", dps.getValueAsString("DeltaG"), ind);
                    writeStringProperty("DELTA-H", dps.getValueAsString("DeltaH"), ind);
                    writeStringProperty("DELTA-S", dps.getValueAsString("DeltaS"), ind);
                    writeStringListProperty("EC-NUMBER", (String[])dps.getValue("EcNumber"), ind);
                    writeStringProperty("KEQ", dps.getValueAsString("Keq"), ind);
                }
                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }

    public OWLIndividual exportPhysicalEntity(SpecieReference sr) throws Exception
    {
        DynamicPropertySet dps = sr.getAttributes();
        String partType = dps.getValueAsString("Type");
        if( partType != null )
        {
            String name = sr.getName();
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + partType + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + partType));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
                manager.applyChange(new AddAxiom(ontology, axiom));

                if( sr.getSpecie() != null )
                {
                    DataElement de = getDataElementByName(sr.getSpecie());
                    OWLIndividual result = null;
                    if( de instanceof Protein )
                    {
                        result = exportProtein((Protein)de);
                    }
                    else if( de instanceof RNA )
                    {
                        result = exportRna((RNA)de);
                    }
                    else if( de instanceof DNA )
                    {
                        result = exportDna((DNA)de);
                    }
                    else if( de instanceof Substance )
                    {
                        result = exportSmallMolecule((Substance)de);
                    }
                    else if( de instanceof Complex )
                    {
                        result = exportComplex((Complex)de);
                    }
                    else
                    {
                        result = exportConcept((Concept)de);
                    }
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#PHYSICAL-ENTITY"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
                writeComments(sr.getComment(), ind);

                if( sr.getStoichiometry() != null )
                {
                    OWLConstant owlConstant = factory.getOWLUntypedConstant(sr.getStoichiometry());
                    OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#STOICHIOMETRIC-COEFFICIENT"),
                            owlConstant);
                    OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                    manager.applyChange(new AddAxiom(ontology, ea));
                }

                writeOCV(dps.getValueAsString("CellularLocation"), ind, "CELLULAR-LOCATION");

                if( partType.equals("sequenceParticipant") )
                {
                    writeFeatureList((SequenceFeature[])dps.getValue("FeatureList"), ind);
                }
                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }

    public OWLIndividual exportConcept(Concept de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#physicalEntity"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            String shortName = de.getTitle();
            if( shortName == null )
                shortName = name;

            writeName(de.getCompleteName(), ind);
            writeShortName(shortName, ind);
            writeSynonyms(de.getSynonyms(), ind);
            writeComments(de.getComment(), ind);
            writeDatabaseReferences(de.getDatabaseReferences(), ind, "XREF");
            writePublications(de.getLiteratureReferences(), ind, "XREF");

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
            writeDataSource((String[])dps.getValue("DataSource"), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportProtein(Protein de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#protein"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            String shortName = de.getTitle();
            if( shortName == null )
                shortName = name;

            writeName(de.getCompleteName(), ind);
            writeShortName(shortName, ind);
            writeSynonyms(de.getSynonyms(), ind);
            writeComments(de.getComment(), ind);
            writeDatabaseReferences(de.getDatabaseReferences(), ind, "XREF");
            writePublications(de.getLiteratureReferences(), ind, "XREF");

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("SEQUENCE", dps.getValueAsString("Sequence"), ind);
            writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
            writeOrganism(dps.getValueAsString("Organism"), ind);
            writeDataSource((String[])dps.getValue("DataSource"), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportRna(RNA de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#rna"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            String shortName = de.getTitle();
            if( shortName == null )
                shortName = name;

            writeName(de.getCompleteName(), ind);
            writeShortName(shortName, ind);
            writeSynonyms(de.getSynonyms(), ind);
            writeComments(de.getComment(), ind);
            writeDatabaseReferences(de.getDatabaseReferences(), ind, "XREF");
            writePublications(de.getLiteratureReferences(), ind, "XREF");

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("SEQUENCE", dps.getValueAsString("Sequence"), ind);
            writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
            writeOrganism(dps.getValueAsString("Organism"), ind);
            writeDataSource((String[])dps.getValue("DataSource"), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportDna(DNA de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#dna"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            String shortName = de.getTitle();
            if( shortName == null )
                shortName = name;

            writeName(de.getCompleteName(), ind);
            writeShortName(shortName, ind);
            writeSynonyms(de.getSynonyms(), ind);
            writeComments(de.getComment(), ind);
            writeDatabaseReferences(de.getDatabaseReferences(), ind, "XREF");
            writePublications(de.getLiteratureReferences(), ind, "XREF");

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("SEQUENCE", dps.getValueAsString("Sequence"), ind);
            writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
            writeOrganism(dps.getValueAsString("Organism"), ind);
            writeDataSource((String[])dps.getValue("DataSource"), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportSmallMolecule(Substance de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#smallMolecule"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            String shortName = de.getTitle();
            if( shortName == null )
                shortName = name;

            writeName(de.getCompleteName(), ind);
            writeShortName(shortName, ind);
            writeSynonyms(de.getSynonyms(), ind);
            writeComments(de.getComment(), ind);
            writeDatabaseReferences(de.getDatabaseReferences(), ind, "XREF");
            writePublications(de.getLiteratureReferences(), ind, "XREF");

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
            writeDataSource((String[])dps.getValue("DataSource"), ind);

            writeStringProperty("CHEMICAL-FORMULA", dps.getValueAsString("ChemicalFormula"), ind);
            writeStringProperty("MOLECULAR-WEIGHT", dps.getValueAsString("MolecularWeight"), ind);

            Structure structure = (Structure)dps.getValue("ChemicalStructure");
            if( structure != null )
            {
                OWLIndividual result = exportChemicalStructure(structure);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#STRUCTURE"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportChemicalStructure(Structure de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#chemicalStructure"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeStringProperty("STRUCTURE-DATA", de.getData(), ind);
            writeStringProperty("STRUCTURE-FORMAT", de.getFormat(), ind);
            writeComments(de.getComment(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportComplex(Complex de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#complex"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            String shortName = de.getTitle();
            if( shortName == null )
                shortName = name;

            writeName(de.getCompleteName(), ind);
            writeShortName(shortName, ind);
            writeSynonyms(de.getSynonyms(), ind);
            writeComments(de.getComment(), ind);
            writeDatabaseReferences(de.getDatabaseReferences(), ind, "XREF");
            writePublications(de.getLiteratureReferences(), ind, "XREF");

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("AVAILABILITY", dps.getValueAsString("Availability"), ind);
            writeOrganism(dps.getValueAsString("Organism"), ind);
            writeDataSource((String[])dps.getValue("DataSource"), ind);


            String sr[] = de.getComponents();
            if( sr != null )
            {
                for( String spr : sr )
                {
                    DataElement srElement = getDataElementByName(spr);
                    if( srElement instanceof SpecieReference )
                    {
                        OWLIndividual result = exportPhysicalEntity((SpecieReference)srElement);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#COMPONENTS"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                }
            }
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportDatabaseReference(DatabaseReference dr) throws Exception
    {
        String db = dr.getDatabaseName();
        String id = dr.getId();
        String name = "";
        if( db != null )
            name += db;
        if( id != null )
            name += id;

        String type = dr.getRelationshipType();
        OWLIndividual ind = null;
        if( type != null )
        {
            ind = factory.getOWLIndividual(URI.create(fileURI + "#" + type + "_" + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                if( !type.equals("unificationXref") )
                {
                    type = "relationshipXref";
                }
                OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + type));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
                manager.applyChange(new AddAxiom(ontology, axiom));

                writeComments(dr.getComment(), ind);
                writeStringProperty("DB-VERSION", dr.getDatabaseVersion(), ind);
                writeStringProperty("DB", db, ind);
                writeStringProperty("ID-VERSION", dr.getIdVersion(), ind);
                writeStringProperty("ID", id, ind);

                if( !type.equals("unificationXref") )
                {
                    writeStringProperty("RELATIONSHIP-TYPE", dr.getRelationshipType(), ind);
                }
                exportedIDs.add(name);
            }
        }
        else
        {
            return null;
        }

        return ind;
    }

    public OWLIndividual exportPublication(Publication pb) throws Exception
    {
        String name = pb.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + pb.getName()));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#publicationXref"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(pb.getComment(), ind);
            writeStringProperty("TITLE", pb.getTitle(), ind);
            writeStringProperty("YEAR", pb.getYear(), ind);
            writeAuthors(pb.getAuthors(), ind);
            writeStringProperty("URL", pb.getFullTextURL(), ind);

            writeStringProperty("DB", pb.getDb(), ind);
            writeStringProperty("DB-VERSION", pb.getDbVersion(), ind);
            writeStringProperty("ID", pb.getIdName(), ind);
            writeStringProperty("ID-VERSION", pb.getIdVersion(), ind);
            writeStringProperty("SOURCE", pb.getSimpleSource(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportBioSource(BioSource organism) throws Exception
    {
        String name = organism.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#bioSource"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeName(organism.getCompleteName(), ind);
            writeComments(organism.getComment(), ind);
            writeStringProperty("TISSUE", organism.getTissue(), ind);
            writeDatabaseReferences(organism.getDatabaseReferences(), ind, "TAXON-XREF");
            writePublications(organism.getLiteratureReferences(), ind, "TAXON-XREF");
            writeStringProperty("CELLTYPE", organism.getCelltype(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportDatabaseInfo(DatabaseInfo databaseInfo) throws Exception
    {
        String name = databaseInfo.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#dataSource"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeName(databaseInfo.getName(), ind);
            writeComments(databaseInfo.getComment(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportOpenControlledVocabulary(OpenControlledVocabulary ocv) throws Exception
    {
        String name = ocv.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#openControlledVocabulary"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(ocv.getComment(), ind);
            writeStringProperty("TERM", ocv.getTerm(), ind);
            writeDatabaseReferences(ocv.getDatabaseReferences(), ind, "XREF");
            writePublications(ocv.getLiteratureReferences(), ind, "XREF");
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportEvidence(Evidence ev) throws Exception
    {
        String name = ev.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#evidence"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(ev.getComment(), ind);
            writeConfidence(ev.getConfidence(), ind);
            writeOCV(ev.getEvidenceCode(), ind, "EVIDENCE-CODE");
            writeDatabaseReferences(ev.getDatabaseReferences(), ind, "XREF");
            writePublications(ev.getLiteratureReferences(), ind, "XREF");
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportConfidence(Confidence cd) throws Exception
    {
        String name = cd.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#confidence"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(cd.getComment(), ind);
            writeStringProperty("CONFIDENCE-VALUE", cd.getConfidenceValue(), ind);
            writeDatabaseReferences(cd.getDatabaseReferences(), ind, "XREF");
            writePublications(cd.getLiteratureReferences(), ind, "XREF");
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportSequenceFeature(SequenceFeature sf) throws Exception
    {
        String name = sf.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#sequenceFeature"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(sf.getComment(), ind);
            writeName(sf.getName(), ind);
            writeShortName(sf.getTitle(), ind);
            writeOCV(sf.getFeatureType(), ind, "FEATURE-TYPE");
            writeDatabaseReferences(sf.getDatabaseReferences(), ind, "XFER");
            writePublications(sf.getLiteratureReferences(), ind, "XREF");
            writeSynonyms(sf.getSynonyms(), ind);
            writeFeatureLocation(sf.getFeatureLocation(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportSequenceInterval(SequenceInterval sf) throws Exception
    {
        String name = sf.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#sequenceInterval"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(sf.getComment(), ind);
            writeSequenceSite(sf.getBegin(), ind, "SEQUENCE-INTERVAL-BEGIN");
            writeSequenceSite(sf.getEnd(), ind, "SEQUENCE-INTERVAL-END");
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportSequenceSite(SequenceSite ss) throws Exception
    {
        String name = ss.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#sequenceSite"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(ss.getComment(), ind);
            writeStringProperty("POSITION-STATUS", ss.getPositionStatus(), ind);
            writeStringProperty("SEQUENCE-POSITION", ss.getSequencePosition(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }
    /*
     * simple operations
     */

    private void writeName(String name, OWLIndividual ind) throws Exception
    {
        if( name != null && !name.equals("") )
        {
            OWLConstant owlName = factory.getOWLUntypedConstant(name);
            OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#NAME"), owlName);
            OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
            manager.applyChange(new AddAxiom(ontology, ea));
        }
    }

    private void writeShortName(String shortName, OWLIndividual ind) throws Exception
    {
        if( shortName != null && !shortName.equals("") )
        {
            OWLConstant owlName = factory.getOWLUntypedConstant(shortName);
            OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#SHORT-NAME"), owlName);
            OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
            manager.applyChange(new AddAxiom(ontology, ea));
        }
    }

    private void writeStringProperty(String propertyName, String value, OWLIndividual ind) throws Exception
    {
        if( value != null )
        {
            OWLConstant owlValue = factory.getOWLUntypedConstant(value);
            OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#" + propertyName), owlValue);
            OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
            manager.applyChange(new AddAxiom(ontology, ea));
        }
    }

    private void writeStringListProperty(String propertyName, String[] values, OWLIndividual ind) throws Exception
    {
        if( values != null )
        {
            for( String st : values )
            {
                OWLConstant owlValue = factory.getOWLUntypedConstant(st);
                OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#" + propertyName), owlValue);
                OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                manager.applyChange(new AddAxiom(ontology, ea));
            }
        }
    }

    private void writeSynonyms(String synonyms, OWLIndividual ind) throws Exception
    {
        if( synonyms != null )
        {
            String syn[] = TextUtil.split( synonyms, ';' );
            for( String element : syn )
            {
                String synonym = element.trim();
                if( !synonym.equals("") )
                {
                    OWLConstant owlSynonym = factory.getOWLUntypedConstant(synonym);
                    OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#SYNONYMS"), owlSynonym);
                    OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                    manager.applyChange(new AddAxiom(ontology, ea));
                }
            }
        }
    }

    private void writeComments(String comments, OWLIndividual ind) throws Exception
    {
        if( comments != null )
        {
            String com[] = comments.split("\\([0-9]+\\)");
            for( String element : com )
            {
                String comment = element.trim();
                if( !comment.equals("") )
                {
                    OWLConstant owlComment = factory.getOWLUntypedConstant(comment);
                    OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#COMMENT"), owlComment);
                    OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                    manager.applyChange(new AddAxiom(ontology, ea));
                }
            }
        }
    }

    private void writeAuthors(String authors, OWLIndividual ind) throws Exception
    {
        if( authors != null )
        {
            for( String author : StreamEx.split(authors, "\\([0-9]+\\)").map( String::trim ).remove( String::isEmpty ) )
            {
                OWLConstant owlAuthor = factory.getOWLUntypedConstant(author);
                OWLAnnotation<?> myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#AUTHORS"), owlAuthor);
                OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                manager.applyChange(new AddAxiom(ontology, ea));
            }
        }
    }

    private void writeDatabaseReferences(DatabaseReference[] xrefs, OWLIndividual ind, String tag) throws Exception
    {
        if( xrefs != null )
        {
            for( DatabaseReference dr : xrefs )
            {
                OWLIndividual result = exportDatabaseReference(dr);
                if( result != null )
                {
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#" + tag));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
            }
        }
    }

    private void writePublications(String[] xrefs, OWLIndividual ind, String tag) throws Exception
    {
        if( xrefs != null )
        {
            for( String xref : xrefs )
            {
                DataElement de = getDataElementByName(xref);
                if( de instanceof Publication )
                {
                    Publication pb = (Publication)de;
                    OWLIndividual result = exportPublication(pb);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#" + tag));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
            }
        }
    }
    private void writeOrganism(String organismName, OWLIndividual ind) throws Exception
    {
        if( organismName != null )
        {
            BioSource organism = (BioSource)getDataElementByName(organismName);
            if( organism != null )
            {
                OWLIndividual result = exportBioSource(organism);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#ORGANISM"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
        }
    }

    private void writeDataSource(String[] databaseinfoNames, OWLIndividual ind) throws Exception
    {
        if( databaseinfoNames != null )
        {
            for( String databaseinfoName : databaseinfoNames )
            {
                DatabaseInfo di = (DatabaseInfo)getDataElementByName(databaseinfoName);
                OWLIndividual result = exportDatabaseInfo(di);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#DATA-SOURCE"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
        }
    }

    private void writeFeatureList(SequenceFeature[] sf, OWLIndividual ind) throws Exception
    {
        if( sf != null )
        {
            for( SequenceFeature di : sf )
            {
                OWLIndividual result = exportSequenceFeature(di);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#SEQUENCE-FEATURE-LIST"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
        }
    }

    private void writeOCV(String ocvName, OWLIndividual ind, String tag) throws Exception
    {
        if( ocvName != null )
        {
            OpenControlledVocabulary ocv = (OpenControlledVocabulary)getDataElementByName(ocvName);
            if( ocv != null )
            {
                OWLIndividual result = exportOpenControlledVocabulary(ocv);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#" + tag));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
        }
    }

    private void writeEvidence(Evidence ev, OWLIndividual ind) throws Exception
    {
        if( ev != null )
        {
            OWLIndividual result = exportEvidence(ev);
            OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#EVIDENCE"));
            OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
            manager.applyChange(new AddAxiom(ontology, assertion));
        }
    }

    private void writeConfidence(Confidence cf, OWLIndividual ind) throws Exception
    {
        if( cf != null )
        {
            OWLIndividual result = exportConfidence(cf);
            OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#CONFIDENCE"));
            OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
            manager.applyChange(new AddAxiom(ontology, assertion));
        }
    }

    private void writeFeatureLocation(SequenceInterval ci, OWLIndividual ind) throws Exception
    {
        if( ci != null )
        {
            OWLIndividual result = exportSequenceInterval(ci);
            OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#FEATURE-LOCATION"));
            OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
            manager.applyChange(new AddAxiom(ontology, assertion));
        }
    }

    private void writeSequenceSite(SequenceSite ss, OWLIndividual ind, String type) throws Exception
    {
        if( ss != null )
        {
            OWLIndividual result = exportSequenceSite(ss);
            OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#" + type));
            OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
            manager.applyChange(new AddAxiom(ontology, assertion));
        }
    }

    private String toSimpleString(String str)
    {
        return str.replaceAll("[^a-zA-Z0-9]", "_");
    }

}
