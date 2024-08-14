package biouml.model;

import biouml.model.xml.XmlDiagramType;

/**
 * General interface to convert diagram type.
 */
public interface DiagramTypeConverter
{
    /**
     * Converts diagram to another type
     * @param diagram
     * @param type {@link DiagramType}(Class) or name of {@link XmlDiagramType} (String)
     * @return
     * @throws Exception
     */
    public Diagram convert(Diagram diagram, Object type) throws Exception;
    public DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram) throws Exception;

    /**
     * @return whether specified de can be converted to DiagramElement suitable for diagram
     */
    public boolean canConvert(DiagramElement de);

    public static class YesConverter extends DiagramTypeConverterSupport
    {
        @Override
        protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception
        {
            diagram.setType(diagramType);
            return diagram;
        }
    }
}
