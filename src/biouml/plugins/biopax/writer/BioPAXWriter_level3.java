
package biouml.plugins.biopax.writer;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import biouml.plugins.biopax.model.EntityFeature;
import biouml.plugins.biopax.model.Evidence;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
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

public class BioPAXWriter_level3 extends BioPAXWriter
{
    private static final Logger log = Logger.getLogger(BioPAXWriter_level3.class.getName());

    private String biopax = "http://www.biopax.org/release/biopax-level3.owl";
    private String levelTag = "biopax-level3";
    private DataCollection primaryCollection;
    private DataElementPath primaryCollectionPath;
    protected File file;
    private String fileURI;

    protected Set<String> exportedIDs;

    public BioPAXWriter_level3()
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
            DataCollection data = (DataCollection)primaryCollection.get(Module.DATA);
            DataCollection controls = (DataCollection)data.get(BioPAXSupport.CONTROL);
            DataCollection conversion = (DataCollection)data.get(BioPAXSupport.CONVERSION);
            DataCollection physicalEntities = (DataCollection)data.get(BioPAXSupport.PHYSICAL_ENTITY);
            DataCollection complexes = (DataCollection)data.get(BioPAXSupport.COMPLEX);
            DataCollection proteins = (DataCollection)data.get(BioPAXSupport.PROTEIN);
            DataCollection rnas = (DataCollection)data.get(BioPAXSupport.RNA);
            DataCollection dnas = (DataCollection)data.get(BioPAXSupport.DNA);
            DataCollection smallMolecules = (DataCollection)data.get(BioPAXSupport.SMALL_MOLECULE);
            DataCollection participants = (DataCollection)data.get(BioPAXSupport.PARTICIPANT);
            DataCollection diagrams = (DataCollection)primaryCollection.get(Module.DIAGRAM);
            
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
                Iterator<?> iter = diagrams.iterator();
                while( iter.hasNext() )
                {
                    Diagram di = (Diagram)iter.next();
                    exportPathway((DiagramInfo)di.getKernel());
                    currentCount += 10;
                    if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                    {
                        terminated = true;
                        break;
                    }
                    jobControl.setPreparedness(( currentCount * 100 ) / totalCount);
                }
                if( !terminated )
                {
                    iter = conversion.iterator();
                    while( iter.hasNext() )
                    {
                        Reaction de = (Reaction)iter.next();
                        exportConversion(de);
                        currentCount++;
                        if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                        {
                            terminated = true;
                            break;
                        }
                        jobControl.setPreparedness(( currentCount * 100 ) / totalCount);
                    }
                }
                if( !terminated )
                {
                    iter = controls.iterator();
                    while( iter.hasNext() )
                    {
                        SemanticRelation de = (SemanticRelation)iter.next();
                        exportControl(de);
                        currentCount++;
                        if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                        {
                            terminated = true;
                            break;
                        }
                        jobControl.setPreparedness(( currentCount * 100 ) / totalCount);
                    }
                }
                if( !terminated )
                {
                    iter = physicalEntities.iterator();
                    while( iter.hasNext() )
                    {
                        Concept de = (Concept)iter.next();
                        String entityType = (String)de.getAttributes().getValue("Type");
                        if( "DNARegionReference".equals(entityType) )
                        {
                            exportNARegion(de, entityType);
                        }
                        else if( "RNARegionReference".equals(entityType) )
                        {
                            exportNARegion(de, entityType);
                        }
                        else
                            exportConcept(de);
                        currentCount++;
                        if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                        {
                            terminated = true;
                            break;
                        }
                        jobControl.setPreparedness(( currentCount * 100 ) / totalCount);
                    }
                }
                if( !terminated )
                {
                    iter = complexes.iterator();
                    while( iter.hasNext() )
                    {
                        Complex de = (Complex)iter.next();
                        exportBase(de);
                        currentCount++;
                        if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                        {
                            terminated = true;
                            break;
                        }
                        jobControl.setPreparedness(( currentCount * 100 ) / totalCount);
                    }
                }
                if( !terminated )
                {
                    iter = proteins.iterator();
                    while( iter.hasNext() )
                    {
                        Protein de = (Protein)iter.next();
                        exportProtein(de);
                        currentCount++;
                        if( jobControl.getStatus() == FunctionJobControl.TERMINATED_BY_REQUEST )
                        {
                            terminated = true;
                            break;
                        }
                        jobControl.setPreparedness(( currentCount * 100 ) / totalCount);
                    }
                }
                if( !terminated )
                {
                    iter = rnas.iterator();
                    while( iter.hasNext() )
                    {
                        RNA de = (RNA)iter.next();
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
                    iter = dnas.iterator();
                    while( iter.hasNext() )
                    {
                        DNA de = (DNA)iter.next();
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
                    iter = smallMolecules.iterator();
                    while( iter.hasNext() )
                    {
                        Substance de = (Substance)iter.next();
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
                    iter = participants.iterator();
                    while( iter.hasNext() )
                    {
                        SpecieReference de = (SpecieReference)iter.next();
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



    public OWLIndividual exportPathway(DiagramInfo di) throws Exception
    {
        String name = di.getName();

        OWLClass pathway = factory.getOWLClass(URI.create(biopax + "#Pathway"));
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( exportedIDs.contains(name) )
            return ind;
        OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, pathway);
        manager.applyChange(new AddAxiom(ontology, axiom));

        String shortName = di.getTitle();
        if( shortName == null )
            shortName = name;

        writeName(name, ind);
        writeDisplayName(shortName, ind);
        writeComments(di.getComment(), ind);
        writeDatabaseReferences(di.getDatabaseReferences(), ind, "xref");
        writePublications(di.getLiteratureReferences(), ind, "xref");

        DynamicPropertySet dps = di.getAttributes();
        writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
        //writeSynonyms(dps.getValueAsString("Synonyms"), ind);
        writeDataSource((String[])dps.getValue("DataSource"), ind);
        writeOrganism(dps.getValueAsString("Organism"), ind);

        String[] components = (String[])dps.getValue("Components");
        if( components != null )
        {
            for( String componentName : components )
            {
                if( componentName.startsWith(Module.DIAGRAM) )
                {
                    Diagram diagram = (Diagram)getDataElementByName(componentName);
                    DiagramInfo diagramInfo = (DiagramInfo)diagram.getKernel();
                    OWLIndividual result = exportPathway(diagramInfo);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#pathwayComponent"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
                else if( componentName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.CONVERSION) )
                {
                    Reaction conversion = (Reaction)getDataElementByName(componentName);
                    OWLIndividual result = exportConversion(conversion);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#pathwayComponent"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
                else if( componentName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.CONTROL) )
                {
                    SemanticRelation control = (SemanticRelation)getDataElementByName(componentName);
                    OWLIndividual result = exportControl(control);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#pathwayComponent"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
            }
        }
        exportedIDs.add(name);
        return ind;
    }

    public OWLIndividual exportControl(SemanticRelation kernel) throws Exception
    {
        DynamicPropertySet dps = kernel.getAttributes();
        String controlType = dps.getValueAsString("Type");
        if( controlType != null )
        {
            controlType = TextUtil.ucFirst( controlType );
            if( controlType.equals("TemplateReaction") )
                return exportTemplateReaction(kernel);
            if( !isControlType(controlType) )
                controlType = "Control";
            String name = kernel.getName();
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + controlType + "_" + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                OWLClass control = factory.getOWLClass(URI.create(biopax + "#" + controlType));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, control);
                manager.applyChange(new AddAxiom(ontology, axiom));

                writeName(name, ind);
                writeDisplayName(kernel.getTitle(), ind);
                writeComments(kernel.getComment(), ind);
                writeStringProperty("controlType", kernel.getRelationType(), ind);
                writeDatabaseReferences(kernel.getDatabaseReferences(), ind, "xref");
                writePublications(kernel.getLiteratureReferences(), ind, "xref");

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
                else if( input instanceof Diagram )
                {
                    result = exportPathway((DiagramInfo) ( (Diagram)input ).getKernel());
                }
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#controller"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));

                DataElement output = getDataElementByName(kernel.getOutputElementName());
                result = null;
                if( output instanceof Diagram )
                {
                    result = exportPathway((DiagramInfo) ( (Diagram)output ).getKernel());
                }
                else if( output instanceof SemanticRelation )
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
                OWLObjectProperty component2 = factory.getOWLObjectProperty(URI.create(biopax + "#controlled"));
                assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component2, result);
                manager.applyChange(new AddAxiom(ontology, assertion));

                writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
                //writeSynonyms(dps.getValueAsString("Synonyms"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);
                writeOCV(dps.getValueAsString("InteractionType"), ind, "interactionType");
                if( controlType.equals("Catalysis") )
                {
                    writeStringProperty("catalysisDirection", dps.getValueAsString("Direction"), ind);
                    if( dps.getValue("Cofactor") != null )
                    {
                        for( String coFactor : (String[])dps.getValue("Cofactor") )
                        {
                            if( coFactor.startsWith(Module.DATA) )
                            {
                                SpecieReference spr = (SpecieReference)getDataElementByName(coFactor);
                                result = exportPhysicalEntity(spr);
                                component = factory.getOWLObjectProperty(URI.create(biopax + "#coFactor"));
                                assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                                manager.applyChange(new AddAxiom(ontology, assertion));
                            }
                        }
                    }
                }
                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }

    public OWLIndividual exportTemplateReaction(SemanticRelation kernel) throws Exception
    {
        DynamicPropertySet dps = kernel.getAttributes();
        String controlType = dps.getValueAsString("Type");
        if( controlType != null && controlType.equals("TemplateReaction") )
        {
            String name = kernel.getName();
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + controlType + "_" + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                OWLClass control = factory.getOWLClass(URI.create(biopax + "#" + controlType));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, control);
                manager.applyChange(new AddAxiom(ontology, axiom));

                writeName(name, ind);
                writeDisplayName(kernel.getTitle(), ind);
                writeComments(kernel.getComment(), ind);
                writeStringProperty("controlType", kernel.getRelationType(), ind);
                writeDatabaseReferences(kernel.getDatabaseReferences(), ind, "xref");
                writePublications(kernel.getLiteratureReferences(), ind, "xref");

                DataElement input = getDataElementByName(kernel.getInputElementName());
                OWLIndividual result = null;
                if( input instanceof SpecieReference )
                {
                    result = exportPhysicalEntity((SpecieReference)input);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#template"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }


                DataElement output = getDataElementByName(kernel.getOutputElementName());
                result = null;
                if( output instanceof SpecieReference )
                {
                    result = exportPhysicalEntity((SpecieReference)output);
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#product"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }


                writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
                //writeSynonyms(dps.getValueAsString("Synonyms"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);
                writeOCV(dps.getValueAsString("InteractionType"), ind, "interactionType");
                writeStringProperty("templateDirection", dps.getValueAsString("TemplateDirection"), ind);

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
            conversionType = TextUtil.ucFirst( conversionType );
            if( conversionType.equals("GeneticInteraction") )
                return exportGeneticInteraction(kernel);
            if( !isConversionType(conversionType) )
                conversionType = "Conversion";
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
                writeDisplayName(shortName, ind);
                writeComments(kernel.getComment(), ind);
                writeDatabaseReferences(kernel.getDatabaseReferences(), ind, "xref");
                writePublications(kernel.getLiteratureReferences(), ind, "xref");

                for( SpecieReference sr : kernel.getSpecieReferences() )
                {
                    SpecieReference spr = (SpecieReference)getDataElementByName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + "Participants" + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(sr.getName()));
                    OWLIndividual result = null;
                    if( sr.getRole().equals(SpecieReference.REACTANT) )//add to LEFT
                    {
                        result = exportPhysicalEntity(spr);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#left"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                    else if( sr.getRole().equals(SpecieReference.PRODUCT) )//add to RIGHT
                    {
                        result = exportPhysicalEntity(spr);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#right"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                    else if( sr.getRole().equals(SpecieReference.OTHER) )//add to PARTICIPANTS
                    {
                        result = exportPhysicalEntity(spr);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#participant"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                    if( result != null && spr.getStoichiometry() != null )
                    {
                        OWLIndividual stoich = exportStoichiometry(spr.getStoichiometry(), result);
                        OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#participantStoichiometry"));
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, stoich);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                }

                writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
                //writeSynonyms(dps.getValueAsString("Synonyms"), ind);
                writeStringProperty("spontaneous", dps.getValueAsString("Spontaneous"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);
                writeOCV(dps.getValueAsString("InteractionType"), ind, "interactionType");

                if( conversionType.equals("BiochemicalReaction") || conversionType.equals("TransportWithBiochemicalReaction") )
                {
                    writeStringProperty("deltaG", dps.getValueAsString("DeltaG"), ind);
                    writeStringProperty("deltaH", dps.getValueAsString("DeltaH"), ind);
                    writeStringProperty("deltaS", dps.getValueAsString("DeltaS"), ind);
                    writeStringListProperty("eCNumber", (String[])dps.getValue("EcNumber"), ind);
                    writeStringProperty("kEQ", dps.getValueAsString("Keq"), ind);
                }
                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }

    public OWLIndividual exportGeneticInteraction(Reaction kernel) throws Exception
    {
        DynamicPropertySet dps = kernel.getAttributes();
        String conversionType = dps.getValueAsString("Type");
        if( conversionType != null && conversionType.equals("GeneticInteraction") )
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
                writeDisplayName(shortName, ind);
                writeComments(kernel.getComment(), ind);
                writeDatabaseReferences(kernel.getDatabaseReferences(), ind, "xref");
                writePublications(kernel.getLiteratureReferences(), ind, "xref");

                SpecieReference sr[] = kernel.getSpecieReferences();
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#participant"));
                for( SpecieReference element : sr )
                {
                    SpecieReference spr = (SpecieReference)getDataElementByName(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                            + "Participants" + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + ru.biosoft.access.core.DataElementPath.escapeName(element.getName()));
                    if( element.getRole().equals(SpecieReference.OTHER) )//add to PARTICIPANTS
                    {
                        OWLIndividual result = exportGene(spr);
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                }

                writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
                //writeSynonyms(dps.getValueAsString("Synonyms"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);
                writeOCV(dps.getValueAsString("InteractionType"), ind, "interactionType");
                writeOCV(dps.getValueAsString("Phenotype"), ind, "phenotype");

                if( dps.getValue("InteractionScore") != null )
                {
                    component = factory.getOWLObjectProperty(URI.create(biopax + "#interactionScore"));
                    for( Confidence conf : (Confidence[])dps.getValue("InteracitonScore") )
                    {
                        OWLIndividual result = exportConfidence(conf);
                        OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                        manager.applyChange(new AddAxiom(ontology, assertion));
                    }
                }
                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }

    public OWLIndividual exportGene(SpecieReference sr) throws Exception
    {
        DynamicPropertySet dps = sr.getAttributes();
        String partType = dps.getValueAsString("Type");
        if( partType != null && partType.equals("Gene") )
        {
            String name = sr.getName();
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + partType + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + partType));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
                manager.applyChange(new AddAxiom(ontology, axiom));

                writeStandardName(dps.getValueAsString("StandardName"), ind);
                String shortName = sr.getTitle();
                if( shortName != null )
                    writeDisplayName(shortName, ind);
                writeComments(sr.getComment(), ind);
                writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);
                writeOrganism(dps.getValueAsString("Organism"), ind);

                writeOCV(dps.getValueAsString("CellularLocation"), ind, "cellularLocation");

                writeFeatureList((EntityFeature[])dps.getValue("FeatureList"), ind, "feature");
                writeFeatureList((EntityFeature[])dps.getValue("NotFeatureList"), ind, "notFeature");

                writeDatabaseReferences((DatabaseReference[])dps.getValue("DatabaseReference"), ind, "xref");
                writePublications((String[])dps.getValue("PublicationReference"), ind, "xref");

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
            partType = TextUtil.ucFirst( partType );
            if( !isPhysicalEntityType(partType) )
                partType = "PhysicalEntity";
            String name = sr.getName();
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + partType + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                DataElement de = getDataElementByName(sr.getSpecie());
                OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + partType));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
                manager.applyChange(new AddAxiom(ontology, axiom));

                if(de != null)
                {
                    OWLIndividual result = exportBase(de.cast( Concept.class ));
                    OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#entityReference"));
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }

                writeStandardName(dps.getValueAsString("StandardName"), ind);
                writeDisplayName(sr.getTitle(), ind);
                writeComments(sr.getComment(), ind);
                writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
                writeDataSource((String[])dps.getValue("DataSource"), ind);
                writeEvidence((Evidence)dps.getValue("Evidence"), ind);

                writeOCV(dps.getValueAsString("CellularLocation"), ind, "cellularLocation");

                writeFeatureList((EntityFeature[])dps.getValue("FeatureList"), ind, "feature");
                writeFeatureList((EntityFeature[])dps.getValue("NotFeatureList"), ind, "notFeature");

                writeDatabaseReferences((DatabaseReference[])dps.getValue("DatabaseReference"), ind, "xref");

                if( dps.getValue("PublicationReference") != null )
                {
                    writePublications((String[])dps.getValue("PublicationReference"), ind, "xref");
                }

                if( partType.equals("Complex") )
                {
                    if( dps.getValue("Components") != null )
                    {
                        String[] components = (String[])dps.getValue("Components");
                        List<String> stoich = new ArrayList<>();
                        for( String componentName : components )
                        {

                            if( componentName.startsWith(Module.DATA) )
                            {
                                SpecieReference spr = (SpecieReference)getDataElementByName(componentName);
                                if( spr.getStoichiometry() != null )
                                {
                                    stoich.add(spr.getStoichiometry());
                                }
                                OWLIndividual result = exportPhysicalEntity(spr);

                                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#component"));
                                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                                manager.applyChange(new AddAxiom(ontology, assertion));
                            }
                        }
                        writeStringListProperty("componentStoichiometry", stoich.toArray(new String[stoich.size()]), ind);
                    }
                }
                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }

    public OWLIndividual exportBase(Concept de) throws Exception
    {
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
        else
        {
            String entityType = (String) de.getAttributes().getValue("Type");
            if( "DNARegionReference".equals(entityType) )
            {
                result = exportNARegion(de, entityType);
            }
            else if( "RNARegionReference".equals(entityType) )
            {
                result = exportNARegion(de, entityType);
            }
            else
                result = exportConcept(de);
        }
        return result;
    }

    private void writeEntityReferenceCommons(OWLIndividual ind, Concept de) throws Exception
    {
        String shortName = de.getTitle();
        if( shortName == null )
            shortName = de.getName();

        writeName(de.getCompleteName(), ind);
        writeDisplayName(shortName, ind);
        
        writeDatabaseReferences(de.getDatabaseReferences(), ind, "xref");
        writePublications(de.getLiteratureReferences(), ind, "xref");

        DynamicPropertySet dps = de.getAttributes();
        //writeStringProperty("availability", dps.getValueAsString("Availability"), ind);
        writeEvidence((Evidence)dps.getValue("Evidence"), ind);
        writeStandardName(dps.getValueAsString("StandardName"), ind);
        writeOCV(dps.getValueAsString("EntityReferenceType"), ind, "entityReferenceType");
        writeFeatureList((EntityFeature[])dps.getValue("FeatureList"), ind, "entityFeature");
    }

    public OWLIndividual exportConcept(Concept de) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#EntityReference"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));
            writeEntityReferenceCommons(ind, de);
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#ProteinReference"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeEntityReferenceCommons(ind, de);

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("sequence", dps.getValueAsString("Sequence"), ind);
            writeOrganism(dps.getValueAsString("Organism"), ind);
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#RNAReference"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeEntityReferenceCommons(ind, de);

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("sequence", dps.getValueAsString("Sequence"), ind);
            writeOrganism(dps.getValueAsString("Organism"), ind);
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#DNAReference"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeEntityReferenceCommons(ind, de);

            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("sequence", dps.getValueAsString("Sequence"), ind);
            writeOrganism(dps.getValueAsString("Organism"), ind);
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#SmallMoleculeReference"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeEntityReferenceCommons(ind, de);

            DynamicPropertySet dps = de.getAttributes();
            writeDataSource((String[])dps.getValue("DataSource"), ind);

            writeStringProperty("chemicalFormula", dps.getValueAsString("ChemicalFormula"), ind);
            writeStringProperty("molecularWeight", dps.getValueAsString("MolecularWeight"), ind);

            Structure structure = (Structure)dps.getValue("ChemicalStructure");
            if( structure != null )
            {
                OWLIndividual result = exportChemicalStructure(structure);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#structure"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
            exportedIDs.add(name);
        }
        return ind;
    }

    //Nucleic acid - RNA or DNA region
    public OWLIndividual exportNARegion(Concept de, String type) throws Exception
    {
        String name = de.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + type));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeEntityReferenceCommons(ind, de);
            DynamicPropertySet dps = de.getAttributes();
            writeStringProperty("sequence", dps.getValueAsString("Sequence"), ind);
            writeFeatureList((EntityFeature[])dps.getValue("AbsoluteRegion"), ind, "absoluteRegion");
            String[] subRegions = (String[])dps.getValue("SubRegion");
            if( subRegions != null )
            {
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#subRegion"));
                for( String subRegion : subRegions )
                {
                    Concept deSR = DataElementPath.create(subRegion).getDataElement(Concept.class);
                    OWLIndividual sr = exportBase(deSR);
                    OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, sr);
                    manager.applyChange(new AddAxiom(ontology, assertion));
                }
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#ChemicalStructure"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeStringProperty("structureData", de.getData(), ind);
            writeStringProperty("structureFormat", de.getFormat(), ind);
            writeComments(de.getComment(), ind);
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

        String type = TextUtil.ucFirst( dr.getRelationshipType() );
        OWLIndividual ind = null;
        if( type != null )
        {
            ind = factory.getOWLIndividual(URI.create(fileURI + "#" + type + "_" + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                if( !type.equals("UnificationXref") )
                {
                    type = "RelationshipXref";
                }
                OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + type));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
                manager.applyChange(new AddAxiom(ontology, axiom));

                writeComments(dr.getComment(), ind);
                writeStringProperty("dbVersion", dr.getDatabaseVersion(), ind);
                writeStringProperty("db", db, ind);
                writeStringProperty("idVersion", dr.getIdVersion(), ind);
                writeStringProperty("id", id, ind);

                if( !type.equals("UnificationXref") )
                {
                    writeStringProperty("relationshipType", dr.getRelationshipType(), ind);
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#PublicationXref"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(pb.getComment(), ind);
            writeStringProperty("title", pb.getTitle(), ind);
            writeStringProperty("year", pb.getYear(), ind);
            writeAuthors(pb.getAuthors(), ind);
            writeStringProperty("url", pb.getFullTextURL(), ind);

            if(pb.getDb() == null)
            {
                writeStringProperty("db", "PubMed", ind);
                writeStringProperty("id", pb.getPubMedId(), ind);
            }
            else
            {
                writeStringProperty("db", pb.getDb(), ind);
                writeStringProperty("dbVersion", pb.getDbVersion(), ind);
                writeStringProperty("id", pb.getIdName(), ind);
                writeStringProperty("idVersion", pb.getIdVersion(), ind);
            }
            writeStringProperty("source", pb.getSimpleSource(), ind);
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#BioSource"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeName(organism.getCompleteName(), ind);
            writeComments(organism.getComment(), ind);
            writeStringProperty("tissue", organism.getTissue(), ind);
            writeDatabaseReferences(organism.getDatabaseReferences(), ind, "xref");
            writePublications(organism.getLiteratureReferences(), ind, "xref");
            writeStringProperty("cellType", organism.getCelltype(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportProvenance(DatabaseInfo databaseInfo) throws Exception
    {
        String name = databaseInfo.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#Provenance"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeName(databaseInfo.getName(), ind);
            writeComments(databaseInfo.getComment(), ind);
            writeDatabaseReferences(databaseInfo.getDatabaseReferences(), ind, "xref");
            writePublications(databaseInfo.getLiteratureReferences(), ind, "xref");
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
            String type = ocv.getVocabularyType();
            if(!isVocabularyType(type))
                type = "ControlledVocabulary";
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + type));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(ocv.getComment(), ind);
            writeStringProperty("term", ocv.getTerm(), ind);
            writeDatabaseReferences(ocv.getDatabaseReferences(), ind, "xref");
            writePublications(ocv.getLiteratureReferences(), ind, "xref");
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#Evidence"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(ev.getComment(), ind);
            writeConfidence(ev.getConfidence(), ind);
            writeOCV(ev.getEvidenceCode(), ind, "evidenceCode");
            writeDatabaseReferences(ev.getDatabaseReferences(), ind, "xref");
            writePublications(ev.getLiteratureReferences(), ind, "xref");
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#Score"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(cd.getComment(), ind);
            writeStringProperty("value", cd.getConfidenceValue(), ind);
            writeDatabaseReferences(cd.getDatabaseReferences(), ind, "xref");
            writePublications(cd.getLiteratureReferences(), ind, "xref");
            exportedIDs.add(name);
        }
        return ind;

    }

    public OWLIndividual exportSequenceFeature(EntityFeature sf) throws Exception
    {
        String name = sf.getName();
        OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
        if( !exportedIDs.contains(name) )
        {
            DynamicPropertySet dps = sf.getAttributes();
            String type = dps.getValueAsString("Type");
            if( type == null )
                type = "EntityFeature";

            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#" + type));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(sf.getComment(), ind);
            writeName(sf.getName(), ind);
            writeDisplayName(sf.getTitle(), ind);
            writeOCV(sf.getFeatureType(), ind, "featureLocationType");
            writeDatabaseReferences(sf.getDatabaseReferences(), ind, "xref");
            writePublications(sf.getLiteratureReferences(), ind, "xref");
            writeEvidence((Evidence)dps.getValue("Evidence"), ind);
            writeFeatureLocations(sf.getFeatureLocation(), ind, "featureLocation");

            if( type.equals("BindingFeature") || type.equals("CovalentBindingFeature") )
            {
                writeStringProperty("intramolecular", dps.getValueAsString("Intramolecular"), ind);
                writeFeatureLocations((EntityFeature[])dps.getValue("bindsTo"), ind, "bindsTo");
            }
            else if( type.equals("ModificationFeature") )
            {
                writeOCV(dps.getValueAsString("ModificationType"), ind, "modificationType");
            }
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#SequenceInterval"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(sf.getComment(), ind);
            writeSequenceSite(sf.getBegin(), ind, "sequenceIntervalBegin");
            writeSequenceSite(sf.getEnd(), ind, "sequenceIntervalEnd");
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
            OWLClass entity = factory.getOWLClass(URI.create(biopax + "#SequenceSite"));
            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
            manager.applyChange(new AddAxiom(ontology, axiom));

            writeComments(ss.getComment(), ind);
            writeStringProperty("positionStatus", ss.getPositionStatus(), ind);
            writeStringProperty("sequencePosition", ss.getSequencePosition(), ind);
            exportedIDs.add(name);
        }
        return ind;
    }

    public OWLIndividual exportStoichiometry(String coeff, OWLIndividual stoichEntity) throws Exception
    {
        if( coeff != null )
        {
            String name = stoichEntity.toString() + "_stoichiometry_" + coeff;
            OWLIndividual ind = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
            if( !exportedIDs.contains(name) )
            {
                OWLClass entity = factory.getOWLClass(URI.create(biopax + "#Stoichiometry"));
                OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind, entity);
                manager.applyChange(new AddAxiom(ontology, axiom));

                writeStringProperty("stoichiometricCoefficient", coeff, ind);

                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#physicalEntity"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, stoichEntity);
                manager.applyChange(new AddAxiom(ontology, assertion));

                exportedIDs.add(name);
            }
            return ind;
        }
        return null;
    }


    /*
     * simple operations
     */
    private void writeName(String name, OWLIndividual ind) throws Exception
    {
        if( name != null && !name.equals("") )
        {
            OWLConstant owlName = factory.getOWLUntypedConstant(name);
            OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#name"), owlName);
            OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
            manager.applyChange(new AddAxiom(ontology, ea));
        }
    }

    private void writeDisplayName(String shortName, OWLIndividual ind) throws Exception
    {
        if( shortName != null && !shortName.equals("") )
        {
            OWLConstant owlName = factory.getOWLUntypedConstant(shortName);
            OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#displayName"), owlName);
            OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
            manager.applyChange(new AddAxiom(ontology, ea));
        }
    }

    private void writeStandardName(String standardName, OWLIndividual ind) throws Exception
    {
        if( standardName != null && !standardName.equals("") )
        {
            OWLConstant owlName = factory.getOWLUntypedConstant(standardName);
            OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#standardName"), owlName);
            OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
            manager.applyChange(new AddAxiom(ontology, ea));
        }
    }

    private void writeStringProperty(String propertyName, String value, OWLIndividual ind) throws Exception
    {
        if( value != null )
        {
            OWLConstant owlValue = factory.getOWLUntypedConstant(value);
            OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#" + propertyName), owlValue);
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
                OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#" + propertyName), owlValue);
                OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                manager.applyChange(new AddAxiom(ontology, ea));
            }
        }
    }

    private void writeSynonyms(String synonyms, OWLIndividual ind) throws Exception
    {
        if( synonyms != null )
        {
            for( String synonym : StreamEx.split(synonyms, ';').map( String::trim ).remove( String::isEmpty ) )
            {
                OWLConstant owlSynonym = factory.getOWLUntypedConstant(synonym);
                OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#synonyms"), owlSynonym);
                OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                manager.applyChange(new AddAxiom(ontology, ea));
            }
        }
    }

    private void writeComments(String comments, OWLIndividual ind) throws Exception
    {
        if( comments != null )
        {
            for( String comment : StreamEx.split(comments, "\\([0-9]+\\)").map( String::trim ).remove(String::isEmpty) )
            {
                OWLConstant owlComment = factory.getOWLUntypedConstant(comment);
                OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#comment"), owlComment);
                OWLAxiom ea = factory.getOWLEntityAnnotationAxiom(ind, myannotation);
                manager.applyChange(new AddAxiom(ontology, ea));
            }
        }
    }

    private void writeAuthors(String authors, OWLIndividual ind) throws Exception
    {
        if( authors != null )
        {
            for( String author : StreamEx.split(authors, "\\([0-9]+\\)").map( String::trim ).remove(String::isEmpty) )
            {
                OWLConstant owlAuthor = factory.getOWLUntypedConstant(author);
                OWLAnnotation myannotation = factory.getOWLConstantAnnotation(URI.create(biopax + "#AUTHORS"), owlAuthor);
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
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#organism"));
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
                OWLIndividual result = exportProvenance(di);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#dataSource"));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
        }
    }

    private void writeFeatureList(EntityFeature[] ef, OWLIndividual ind, String tag) throws Exception
    {
        if( ef != null )
        {
            for( EntityFeature di : ef )
            {
                OWLIndividual result = exportSequenceFeature(di);
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#" + tag));
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
            OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#confidence"));
            OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
            manager.applyChange(new AddAxiom(ontology, assertion));
        }
    }

    private void writeFeatureLocations(Concept[] ci, OWLIndividual ind, String tag) throws Exception
    {
        if( ci != null )
        {
            for( Concept concept : ci )
            {
                OWLIndividual result = null;
                if( concept instanceof SequenceInterval )
                    result = exportSequenceInterval((SequenceInterval)concept);
                else if( concept instanceof SequenceSite )
                    result = exportSequenceSite((SequenceSite)concept);
                else
                {
                    String name = concept.getName();
                    OWLIndividual ind2 = factory.getOWLIndividual(URI.create(fileURI + "#" + toSimpleString(name)));
                    if( !exportedIDs.contains(name) )
                    {
                        OWLClass entity = factory.getOWLClass(URI.create(biopax + "#SequenceLocation"));
                        OWLAxiom axiom = factory.getOWLClassAssertionAxiom(ind2, entity);
                        manager.applyChange(new AddAxiom(ontology, axiom));
                        writeComments(concept.getComment(), ind2);
                        exportedIDs.add(name);
                    }
                }
                OWLObjectProperty component = factory.getOWLObjectProperty(URI.create(biopax + "#" + tag));
                OWLAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(ind, component, result);
                manager.applyChange(new AddAxiom(ontology, assertion));
            }
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

    private boolean isControlType(String type)
    {
        if(type == null)
            return false;
        return type.equals("Control") || type.equals("Catalysis") || type.equals("Modulation") || type.equals("TemplateReactionRegulation");
    }

    private boolean isConversionType(String type)
    {
        if(type == null)
            return false;
        return type.equals("Conversion") || type.equals("BiochemicalReaction") || type.equals("ComplexAssembly")
                || type.equals("Degradation") || type.equals("Transport") || type.equals("TransportWithBiochemicalReaction");
    }

    private boolean isPhysicalEntityType(String type)
    {
        if(type == null)
            return false;
        return type.equals("PhysicalEntity") || type.equals("Complex") || type.equals("DNA") || type.equals("DNARegion")
                || type.equals("Protein") || type.equals("RNA") || type.equals("RNARegion") || type.equals("SmallMolecule");
    }

    private boolean isEntityReferenceType(String type)
    {
        if(type == null)
            return false;
        return type.equals("EntityReference") || type.equals("DnaReference") || type.equals("ProteinReference")
                || type.equals("RnaReference") || type.equals("SmallMoleculeReference");
    }

    private boolean isVocabularyType(String type)
    {
        if(type == null)
            return false;
        return type.equals("ControlledVocabulary") || type.equals("CellularLocationVocabulary") || type.equals("CellVocabulary")
                || type.equals("EntityReferenceTypeVocabulary") || type.equals("EvidenceCodeVocabulary")
                || type.equals("ExperimentalFormVocabulary") || type.equals("InteractionVocabulary") || type.equals("PhenotypeVocabulary")
                || type.equals("RelationshipTypeVocabulary") || type.equals("SequenceModificationVocabulary")
                || type.equals("SequenceRegionVocabulary") || type.equals("TissueVocabulary");
    }
}
