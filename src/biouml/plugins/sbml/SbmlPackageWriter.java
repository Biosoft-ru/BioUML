package biouml.plugins.sbml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Variable;


public abstract class SbmlPackageWriter extends SbmlSupport
{
    public Document document;
    protected boolean writeBioUMLAnnotation;
    protected boolean modelDefinition = false;
    protected SbmlModelWriter parentWriter;
    
    public void setModelDefinition(boolean val)
    {
        this.modelDefinition = val;
    }
    
    public void setWriteBioUMLAnnotation(boolean value)
    {
        this.writeBioUMLAnnotation = value;
    }
    
    protected void init(Document document, Diagram diagram)
    {
        this.document = document;
        this.diagram = diagram;
        this.emodel = (SbmlEModel)diagram.getRole();
    }
    
    public void setParent(SbmlModelWriter writer)
    {
        this.parentWriter = writer;
    }
    
    protected void preprocess(Diagram diagram)
    {
        
    }

    protected void processSpecie(Element speciesElement, Node species)
    {

    }
    
    protected void processParameter(Element parameterElement, Variable parameter)
    {

    }
    
    protected void processCompartment(Element parameterElement, Compartment compartment)
    {

    }
    
    protected void processReaction(Element parameterElement, Node reaction)
    {

    }
    
    protected void processModel(Element modelElement, Diagram diagram)
    {

    }
    
    protected void processSBML(Element sbmlElement, Diagram diagram)
    {

    }
    
    public abstract String getNameSpace();
    
    public abstract String getPackageName();
}
