
package biouml.plugins.biopax.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import org.semanticweb.owl.model.OWLImportsDeclaration;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;

/**
 * Utility class to read/write diagrams and other owl files in BioPAX format.
 *
 * Factory design pattern is used to process different levels of BioPAX format.
 */

public class BioPAXReaderFactory
{
    public static BioPAXReader getReader(File file) throws FileNotFoundException
    {
        checkBioPAXFile(file);
        OWLOntology ontology = BioPAXReaderFactory.getOntology(file);
        return getReader(ontology);
    }
    
    public static BioPAXReader getReader(OWLOntology ontology)
    {
        String biopaxVersion = getBioPAXVersion(ontology);
        if(biopaxVersion.contains("biopax-level3.owl"))
        {
            return new BioPAXReader_level3(ontology);
        }
        else if(biopaxVersion.contains("biopax-level2.owl") || biopaxVersion.contains("biopax-level1.owl"))
        {
            return new BioPAXReader_level2(ontology);
        }
        else
        {
            return null;
        }
    }

    public static String getBioPAXVersion(OWLOntology ontology)
    {
        Set<OWLImportsDeclaration> importsDeclarations = ontology.getImportsDeclarations();
        if(importsDeclarations.isEmpty())
            return null;
        else
            return importsDeclarations.iterator().next().getImportedOntologyURI().toString();
    }
    
    public static void checkBioPAXFile(File file) throws FileNotFoundException
    {
        if(file == null)
            throw new IllegalArgumentException("File is not supplied");
        if(!file.exists())
            throw new FileNotFoundException(file.getAbsolutePath());
        OWLOntology ontology = getOntology(file);
        if(ontology == null)
            throw new IllegalArgumentException("File "+file+" is not a valid OWL file");
        String version = getBioPAXVersion(ontology);
        if(version == null)
            throw new IllegalArgumentException("File "+file+" is not a valid BioPAX file");
        if(!version.contains("biopax-level1.owl") && !version.contains("biopax-level2.owl")
                && !version.contains("biopax-level3.owl"))
            throw new IllegalArgumentException("File "+file+" has unsupported BioPAX level: "+version);
    }

    public static OWLOntology getOntology(File file)
    {
        OWLOntologyManager manager = BioPAXReader.getOWLOntologyManager();
        try
        {
            return manager.loadOntologyFromPhysicalURI(file.toURI());
        }
        catch( Throwable e )
        {
            return null;
        }
    }
}
