package biouml.model;

import one.util.streamex.StreamEx;

import biouml.model.xml.XmlDiagramType;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Repository;
import ru.biosoft.util.Clazz;

public interface ModuleType extends DataElement
{
    /**
     * @returns array of diagram types that can be used in this module.
     */
    public Class<? extends DiagramType>[] getDiagramTypes();
    
    /**
     * @returns array of xml diagram type names that can be used in this module.
     */
    public String[] getXmlDiagramTypes();
    
    default public StreamEx<DiagramType> getDiagramTypeObjects() {
        String[] xmlDiagramTypes = getXmlDiagramTypes();
        if(xmlDiagramTypes == null)
            xmlDiagramTypes = new String[0];
        return StreamEx.of( getDiagramTypes() ).map( Clazz.of( DiagramType.class )::createOrLog )
                .append( StreamEx.of( xmlDiagramTypes ).map( XmlDiagramType::getTypeObject ) ).nonNull();
    }

    /**
     * @returns true if the module supports the category concept.
     */
    public boolean isCategorySupported();

    /**
     * The category is used to group diagram elements by folders in BioUML <code>Module</code>.
     *
     * @returns the category.
     * @pending the other variant has Categorizer (singleton with static methods)
     * where we can register all classes.
     */
    public String getCategory(Class<? extends DataElement> aClass);

    /**
     * Indicates whether an empty module with initialised internal structure
     * can be created
     */
    public boolean canCreateEmptyModule();

    /** Creates empty module with initialised internal structure. */
    public Module createModule(Repository parent, String name) throws Exception;

    /** @returns version of the module software */
    public String getVersion();
}
