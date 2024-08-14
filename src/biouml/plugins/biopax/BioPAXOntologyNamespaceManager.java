// $ Id: $
package biouml.plugins.biopax;

import java.util.HashSet;
import java.util.Set;

import org.coode.xml.OWLOntologyNamespaceManager;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;

public class BioPAXOntologyNamespaceManager extends OWLOntologyNamespaceManager
{
    String format;
    BioPAXOntologyNamespaceManager(OWLOntologyManager manager, OWLOntology ontology, String format)
    {
        super(manager, ontology);
        this.format = format;
    }
    
    @Override
    public Set<String> getPrefixes()
    {
        Set<String> prefixes = new HashSet<>();
        prefixes.add("owl");
        prefixes.add("owl11");
        prefixes.add("xsd");
        prefixes.add("owl11xml");
        prefixes.add("rdfs");
        prefixes.add("rdf");
        prefixes.add(format);
        return prefixes;
    }
    
    @Override
    public Set<String> getNamespaces()
    {
        Set<String> namespaces = new HashSet<>();
        namespaces.add(super.getNamespaceForPrefix("owl"));
        namespaces.add(super.getNamespaceForPrefix("owl11"));
        namespaces.add(super.getNamespaceForPrefix("xsd"));
        namespaces.add(super.getNamespaceForPrefix("owl11xml"));
        namespaces.add(super.getNamespaceForPrefix("rdfs"));
        namespaces.add(super.getNamespaceForPrefix("rdf"));
        namespaces.add(super.getNamespaceForPrefix(format));
        return namespaces;
    }
}
