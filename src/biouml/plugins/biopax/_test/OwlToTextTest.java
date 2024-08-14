package biouml.plugins.biopax._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataPropertyExpression;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyURIMapper;
import org.semanticweb.owl.model.OWLTypedConstant;
import org.semanticweb.owl.util.SimpleURIMapper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OwlToTextTest extends TestCase
{
    /** Standart JUnit constructor */
    public OwlToTextTest(String name)
    {
        super(name);

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
    public static Test suite()
    {
        TestSuite suite = new TestSuite(OwlToTextTest.class.getName());
        suite.addTest(new OwlToTextTest("exportTest"));
        return suite;
    }

    public static void exportTest() throws Exception
    {
        convertOneOwl("source.owl", "first.txt");
        convertOneOwl("new.owl", "second.txt");

        System.err.println("-- ok --");
    }

    private static void convertOneOwl(String input, String output)
    {
        try
        {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            String home = System.getProperty("user.dir").replace('\\', '/').replaceAll(" ", "%20");
            URI physicalURI = URI.create("file:/" + home + "/data/BioPAX/" + input);

            OWLOntologyURIMapper oom1 = new SimpleURIMapper(URI.create("http://www.biopax.org/release/biopax-level1.owl"), URI
                    .create("file:/" + home + "/data/BioPAX/biopax-level1.owl"));
            manager.addURIMapper(oom1);

            OWLOntologyURIMapper oom2 = new SimpleURIMapper(URI.create("http://www.biopax.org/release/biopax-level2.owl"), URI
                    .create("file:/" + home + "/data/BioPAX/biopax-level2.owl"));
            manager.addURIMapper(oom2);

            // Now ask the manager to load the ontology
            OWLOntology ontology = manager.loadOntologyFromPhysicalURI(physicalURI);

            //Print out all of the classes which are referenced in the ontology
            List<String> records = new LinkedList<>();
            for( OWLIndividual individual : ontology.getReferencedIndividuals() )
            {
                for( OWLDataPropertyExpression dpe : individual.getDataPropertyValues(ontology).keySet() )
                {
                    Set<OWLConstant> param = individual.getDataPropertyValues( ontology ).get( dpe );
                    for( int i = 0; i < param.toArray().length; i++ )
                    {
                        String record = individual.getTypes(ontology).toArray()[0] + "\t";
                        record += dpe + "\t";
                        String value = "";
                        Object obj = param.toArray()[i];
                        if( obj instanceof OWLTypedConstant )
                        {
                            value = ( (OWLTypedConstant)obj ).getLiteral();
                        }
                        else
                        {
                            value = obj.toString();
                        }
                        record += value + "\t";
                        //record += individual;
                        records.add(record);
                    }
                }
                for( OWLObjectPropertyExpression ope : individual.getObjectPropertyValues(ontology).keySet() )
                {
                    Set<OWLIndividual> param = individual.getObjectPropertyValues( ontology ).get( ope );
                    for( int i = 0; i < param.toArray().length; i++ )
                    {
                        String record = individual.getTypes(ontology).toArray()[0] + "\t";
                        record += ope + "\t";
                        //record += individual + "\t";
                        record += param.toArray()[i] + "\t";
                        records.add(record);
                    }
                }
            }
            Collections.sort(records);
            try (PrintWriter pw = new PrintWriter( new FileOutputStream( new File( home + "/data/BioPAX/" + output ) ) ))
            {
                Iterator<String> iter = records.iterator();
                while( iter.hasNext() )
                {
                    pw.println( iter.next() );
                }
            }
        }
        catch( Exception e )
        {
            System.out.println("The ontology could not be converted: " + e.getMessage());
        }
    }
}
