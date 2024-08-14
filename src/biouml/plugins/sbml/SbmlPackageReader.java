package biouml.plugins.sbml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.Variable;
import ru.biosoft.access.core.DataCollection;

public abstract class SbmlPackageReader extends SbmlSupport
{

    protected Document document;
    
    public void processSpecie(Element element, Node node) throws Exception
    {
        //do nothing by default
    }

    public void processReaction(Element element, Node node) throws Exception
    {
        //do nothing by default
    }
  
    public void processSpecieReference(Element element, Edge edge) throws Exception
    {
        //do nothing by default
    }
    
    public void processParameter(Element element, Variable parameter) throws Exception
    {
        //do nothing by default
    }
    
    public void processRule(Element element, Node rule)
    {
        //do nothing by default
    }
    
    public void processCompartment(Element element, Compartment compartment) throws Exception
    {
        //do nothing by default
    }
    
    
    public void preprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        //do nothing by default
    }
    
    public void postprocessDiagram(Element element, Diagram diagram) throws Exception
    {
        //do nothing by default
    }
    
    public void preprocess(Document doc, DataCollection<?> origin) throws Exception
    {
        //do nothing by default
    }
    
    public abstract String getPackageName();
    
    public DiagramType getDiagramType()
    {
        return null;
    }
    
    public void setModelDefintion(boolean val)
    {
        this.modelDefinition = val;
    }
    protected boolean modelDefinition = false;
}
