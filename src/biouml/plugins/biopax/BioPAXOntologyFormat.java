// $ Id: $
package biouml.plugins.biopax;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owl.model.OWLAnnotation;
import org.semanticweb.owl.vocab.NamespaceOWLOntologyFormat;

public class BioPAXOntologyFormat extends NamespaceOWLOntologyFormat
{
    private final Map<URI, Set<OWLAnnotation>> annotationURI2Annotation;

    private final Set<URI> annotationURIs;

    public BioPAXOntologyFormat()
    {
        annotationURI2Annotation = new HashMap<>();
        annotationURIs = new HashSet<>();
    }

    @Override
    public String toString()
    {
        return "BioPAX";
    }


    public Set<URI> getAnnotationURIs()
    {
        return Collections.unmodifiableSet(annotationURIs);
    }

    /**
     * This method and the functionality that it provides are merely a stopgap
     * until the OWL 1.1 specification is fixed.  Use at your own risk! It will
     * be removed when the spec is fixed!
     */
    public void addAnnotationURI(URI uri)
    {
        annotationURIs.add(uri);
        if( annotationURI2Annotation.get(uri) == null )
        {
            annotationURI2Annotation.put(uri, new HashSet<OWLAnnotation>());
        }
    }


    /**
     * This method and the functionality that it provides are merely a stopgap
     * until the OWL 1.1 specification is fixed.  Use at your own risk! It will
     * be removed when the spec is fixed!
     */
    public void addAnnotationURIAnnotation(URI uri, OWLAnnotation anno)
    {
        addAnnotationURI(uri);
        Set<OWLAnnotation> annos = annotationURI2Annotation.get(uri);
        if( annos == null )
        {
            annos = new HashSet<>();
            annotationURI2Annotation.put(uri, annos);
        }
        annos.add(anno);
    }


    /**
     * This method and the functionality that it provides are merely a stopgap
     * until the OWL 1.1 specification is fixed.  Use at your own risk! It will
     * be removed when the spec is fixed!
     */
    public void removeAnnotationURIAnnotation(URI uri, OWLAnnotation anno)
    {
        Set<OWLAnnotation> annos = annotationURI2Annotation.get(uri);
        if( annos != null )
        {
            annos.remove(anno);
        }
    }


    /**
     * This method and the functionality that it provides are merely a stopgap
     * until the OWL 1.1 specification is fixed.  Use at your own risk! It will
     * be removed when the spec is fixed!
     */
    public void clearAnnotationURIAnnotations()
    {
        annotationURI2Annotation.clear();
    }


    /**
     *
     * This method and the functionality that it provides are merely a stopgap
     * until the OWL 1.1 specification is fixed.  Use at your own risk! It will
     * be removed when the spec is fixed!
     */
    public Map<URI, Set<OWLAnnotation>> getAnnotationURIAnnotations()
    {
        return Collections.unmodifiableMap(annotationURI2Annotation);
    }

}
