package ru.biosoft.access;

import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataElement;

/**
 * This interface will be used to define collection with user-specific hubs
 * to work with UserCollectionBioHub correctly.
 * Need to extend ru.biosoft.access.core.DataElement to work correctly as input element type for BeanInfo
 * @author manikitos
 */
@ClassIcon ( "resources/collection.gif" )
public interface UserHubCollection extends DataElement
{
}
