package biouml.plugins.biopax;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyFormat;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyStorageException;
import org.semanticweb.owl.model.OWLOntologyStorer;


public class BioPAXOntologyStorer implements OWLOntologyStorer
{
    String format = "";
    
    public BioPAXOntologyStorer(String format)
    {
        super();
        this.format = format;
    }
    
    @Override
    public boolean canStoreOntology(OWLOntologyFormat ontologyFormat)
    {
        if( ontologyFormat instanceof BioPAXOntologyFormat )
        {
            return true;
        }
        return false;
    }


    @Override
    public void storeOntology(OWLOntologyManager manager, OWLOntology ontology, URI physicalURI, OWLOntologyFormat ontologyFormat)
            throws OWLOntologyStorageException
    {
        File file = new File( physicalURI );
        // Ensure that the files exist
        file.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream( file );
                BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( os, StandardCharsets.UTF_8 ) ))
        {
            BioPAXRenderer renderer = new BioPAXRenderer( manager, ontology, bw, format );
            renderer.render();
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
    }
}
