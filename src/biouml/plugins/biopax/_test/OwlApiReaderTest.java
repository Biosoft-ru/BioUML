package biouml.plugins.biopax._test;


import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Date;
import java.util.logging.LogManager;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyCreationException;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyURIMapper;
import org.semanticweb.owl.util.SimpleURIMapper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OwlApiReaderTest extends TestCase
{
    /** Standart JUnit constructor */
    public OwlApiReaderTest ( String name )
    {
        super ( name );

        // Setup log
        File configFile = new File( "./biouml/plugins/biopax/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    /** Make suite if tests. */
    public static Test suite ()
    {
        TestSuite suite = new TestSuite ( OwlApiReaderTest.class.getName () );
        suite.addTest ( new OwlApiReaderTest ( "owlApiRead" ) );
        return suite;
    }

    public static void owlApiRead () throws Exception
    {
        System.err.println("-- OwlApiFirst test --");

        try
        {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            String home = System.getProperty("user.dir").replace('\\', '/').replaceAll(" ", "%20");
            URI physicalURI = URI.create("file:/"+home+"/data/BioPAX/new.owl");

            OWLOntologyURIMapper oom1 = new SimpleURIMapper(URI.create("http://www.biopax.org/release/biopax-level1.owl"), URI.create("file:/"+home+"/data/BioPAX/biopax-level1.owl"));
            manager.addURIMapper(oom1);

            OWLOntologyURIMapper oom2 = new SimpleURIMapper(URI.create("http://www.biopax.org/release/biopax-level2.owl"), URI.create("file:/"+home+"/data/BioPAX/biopax-level2.owl"));
            manager.addURIMapper(oom2);

            long start=(new Date()).getTime();
            // Now ask the manager to load the ontology
            System.out.println( "HOME DIR " + physicalURI );
            OWLOntology ontology = manager.loadOntologyFromPhysicalURI(physicalURI);

            System.err.println("----------------"+((new Date()).getTime()-start)+"----------------");
            //Print out all of the classes which are referenced in the ontology
            int controlCount = 0;
            for(OWLIndividual individual : ontology.getReferencedIndividuals())
            {
                if(individual.getTypes(ontology).toArray()[0].toString().equals("control") ||
                        individual.getTypes(ontology).toArray()[0].toString().equals("modulation") ||
                        individual.getTypes(ontology).toArray()[0].toString().equals("catalysis"))
                {
                    controlCount++;
                }
                /*System.out.println("("+individual.getTypes(ontology).toArray()[0]+")\t\tid = "+individual);
                System.out.println("\t-- data properties --");
                for(OWLDataPropertyExpression dpe: individual.getDataPropertyValues(ontology).keySet())
                {
                    System.out.println("\t"+dpe+" = "+individual.getDataPropertyValues(ontology).get(dpe));
                }
                System.out.println("\t-- object properties --");
                for(OWLObjectPropertyExpression ope: individual.getObjectPropertyValues(ontology).keySet())
                {
                    System.out.println("\t"+ope+" = "+individual.getObjectPropertyValues(ontology).get(ope));
                }*/
            }
            System.out.println("Control count = "+controlCount);
        }
        catch (OWLOntologyCreationException e)
        {
            System.out.println("The ontology could not be created: " + e.getMessage());
        }

        System.err.println("-- ok --");
    }
}
