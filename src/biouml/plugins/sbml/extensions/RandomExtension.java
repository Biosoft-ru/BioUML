package biouml.plugins.sbml.extensions;

import java.util.Map;
import java.util.HashMap;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.DiagramElement;

public class RandomExtension extends SbmlExtensionSupport
{
//    public void readElement(Element element, DiagramElement specie, Diagram diagram)
//    {
////        D/iag/ramXmlReader reader = new DiagramXmlReader(diagram.getName());
////        reader.setDiagram(diagram);
////        diagram.addState(StateXmlSerializer.readXmlElement(element, diagram, reader));
//    }

    private static Map<String, String> definitions = new HashMap<String, String>()
    {
        {
            put("binomial", "http://en.wikipedia.org/wiki/Binomial_distribution");
            put("normal", "http://en.wikipedia.org/wiki/Normal_distribution");
            put("uniform", "http://en.wikipedia.org/wiki/Uniform_distribution_(continous)");
        }
    };

    @Override
    public Element[] writeElement(DiagramElement de, Document document)
    {
        if( de.getKernel() != null && de.getKernel().getType().equals("math-function") )
        {
            Element randomElement = document.createElement("distribution");
            String name = de.getName();
            if( definitions.containsKey("binomial") )
            {
                randomElement.setAttribute("xmlns", "http://sbml.org/annotations/distribution");
                randomElement.setAttribute("defnition", definitions.get(name));
                return new Element[]{randomElement};
            }
        }
        return null;
    }

    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        // TODO Auto-generated method stub
        
    }
}
