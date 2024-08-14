package ru.biosoft.bsa.server;

import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.CacheableBeanProvider;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.table.TableDataCollection;

/**
 * Provide list of colors, that are used for track visualization in table (for example, in CMA result).
 * Colors can be changed to adjust nice view.
 * @author anna
 *
 */
public class ColorSchemesBeanProvider implements CacheableBeanProvider
{
    @Override
    public Object getBean(String completeName)
    {
        DataCollection<?> table = CollectionFactory.getDataCollection( completeName );
        if( table instanceof TableDataCollection )
            return BSAService.getTableColorSchemes( WebServicesServlet.getSessionCache(), (TableDataCollection)table );
        return new DynamicPropertySetAsMap();
    }

}
