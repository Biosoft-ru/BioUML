
package biouml.plugins.biopax.writer;

import java.io.File;

import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.plugins.biopax.BioPAXSupport;

import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * @author anna
 *
 */
public abstract class BioPAXWriter extends BioPAXSupport
{
    protected OWLDataFactory factory;
    protected OWLOntology ontology;
    protected OWLOntologyManager manager;
    public abstract void write(Module module, File file, FunctionJobControl jobControl);
    public abstract void writeDiagram(Diagram diagram, File file);
}
