package biouml.standard.type;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.access.core.DataElement;

/**
 * General interface to be implemented by all classes to be used as BioUML diagram elements.
 *
 * All properties are divided into 2 groups:
 * <ol>
 *  <li> static properties represented as BeanProperties.
 *  We are trying to present problem domain using static properties as much as possible.
 *  <li> dynamic properties represented as DynamicPropertySet.
 *  When we are mapping information from different databases into Java objects,
 *  then information that does not correspond to static properties is represented using dynamic
 *  properties. By this way we can represent information from a database as Java
 *  object formally and without loss of information.
 */
public interface Base extends DataElement, Type
{
    /**
     * @returns data element type.
     * @see Type
     */
    public String getType();

    /**
     * @returns data element title to be used in the diagram.
     */
    public String getTitle();

    public DynamicPropertySet getAttributes();
}