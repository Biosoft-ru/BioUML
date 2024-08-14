package biouml.model.util;

import biouml.model.Diagram;

/**
 * Property that needs to know about diagram
 * e.g. VariableName needs to know possible variable names
 * @author Ilya
 *
 */
public interface DiagramDependentProperty
{
    public void setDiagram(Diagram diagram);
}
