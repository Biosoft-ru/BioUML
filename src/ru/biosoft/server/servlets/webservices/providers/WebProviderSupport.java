package ru.biosoft.server.servlets.webservices.providers;

import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.WebException;

/**
 * @author lan
 *
 */
public abstract class WebProviderSupport implements WebProvider
{
    protected static final String SHOW_MODE = "showMode";
    protected static final String JSON_ATTR = "json";

    protected Logger log;

    protected static @Nonnull <T extends DataElement> T castDataElement(DataElement dataElement, Class<T> clazz) throws WebException
    {
        if(dataElement == null)
            throw new WebException("EX_QUERY_NO_ELEMENT_TYPE", "(null)", DataCollectionUtils.getClassTitle(clazz));
        if( !clazz.isInstance(dataElement) )
            throw new WebException("EX_QUERY_INVALID_ELEMENT_TYPE", DataElementPath.create(dataElement), DataCollectionUtils.getClassTitle(clazz));
        return (T)dataElement;
    }

    protected static @Nonnull <T extends DataElement> T getDataElement(DataElementPath path, Class<T> clazz) throws WebException
    {
        DataElement dataElement = path.optDataElement();
        if( dataElement == null )
            throw new WebException("EX_QUERY_NO_ELEMENT_TYPE", path, DataCollectionUtils.getClassTitle(clazz));
        return castDataElement(dataElement, clazz);
    }

    public WebProviderSupport()
    {
        log = Logger.getLogger( getClass().getName() );
    }
}
