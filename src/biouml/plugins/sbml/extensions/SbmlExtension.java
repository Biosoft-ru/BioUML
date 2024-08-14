package biouml.plugins.sbml.extensions;

import javax.annotation.Nonnull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.sbml.SbmlModelReader;
import biouml.plugins.sbml.SbmlModelWriter;

public interface SbmlExtension
{
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram);
    public Element[] writeElement(DiagramElement specie, Document document);
    public void setSbmlModelReader(SbmlModelReader reader);
    public void setSbmlModelWriter(SbmlModelWriter writer);
}
