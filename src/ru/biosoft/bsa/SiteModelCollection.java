
package ru.biosoft.bsa;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * DataCollection to store site models
 */
@ClassIcon("resources/sitemodelcollection.gif")
@PropertyName("profile")
public interface SiteModelCollection extends DataCollection<SiteModel>
{
}
