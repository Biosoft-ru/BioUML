package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;
import java.text.DecimalFormat;

import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.DataElementFactory;
import ru.biosoft.access.support.IdGenerator;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.util.BeanUtil;

public class NewElementProvider extends WebJSONProviderSupport
{
    private static final String NEW_ELEMENT_BEAN_PREFIX = WebBeanProvider.BEANS_PREFIX + "newElement/";

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        switch( arguments.getAction() )
        {
            case "generateName":
                generateName( arguments, response );
                break;
            case "createElement":
                createElement( arguments, response );
                break;
            case "save":
                save( arguments, response );
                break;
            case "rename":
                rename( arguments, response );
                break;
            default:
                throw new WebException( "EX_QUERY_PARAM_INVALID_VALUE", BiosoftWebRequest.ACTION );
        }
    }

    private void generateName(BiosoftWebRequest arguments, JSONResponse response) throws WebException, IOException
    {
        String name = "";
        DataCollection<?> parent = arguments.getDataCollection();
        String idFormat = parent.getInfo().getProperty( DataCollectionConfigConstants.ID_FORMAT );
        if( idFormat != null )
            name = IdGenerator.generateUniqueName( parent, new DecimalFormat( idFormat ) );
        response.sendString( name );
    }

    private void createElement(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElementPath dePath = arguments.getDataElementPath();
        DataElement newDE = DataElementFactory.getInstance().create( dePath.getParentCollection(), dePath.getName() );
        WebServicesServlet.getSessionCache().addObject( NEW_ELEMENT_BEAN_PREFIX + dePath, newDE, true );
        response.sendString( "ok" );
    }

    private void rename(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElementPath dePath = arguments.getDataElementPath();
        String newName = arguments.getElementName( "newName" );
        Object obj = WebServicesServlet.getSessionCache().getObject( NEW_ELEMENT_BEAN_PREFIX + dePath.toString() );
        if( ! ( obj instanceof DataElement ) )
        {
            response.error( "Element not found " + dePath.toString() );
            return;
        }
        DataElement oldDE = (DataElement)obj;
        DataElement newDE = renameDataElement( oldDE, newName );
        WebServicesServlet.getSessionCache().removeObject( NEW_ELEMENT_BEAN_PREFIX + dePath );
        WebServicesServlet.getSessionCache().addObject( NEW_ELEMENT_BEAN_PREFIX + dePath.getSiblingPath( newName ), newDE, true );
        response.sendString( "ok" );
    }

    private DataElement renameDataElement(DataElement oldDE, String newName) throws Exception
    {
        if( oldDE instanceof CloneableDataElement )
            try
            {
                return ( (CloneableDataElement)oldDE ).clone( oldDE.getOrigin(), newName );
            }
            catch( CloneNotSupportedException e )
            {
            }
        DataElement newDE = DataElementFactory.getInstance().create( oldDE.getOrigin(), newName );
        BeanUtil.copyBean( oldDE, newDE );
        return newDE;
    }

    private void save(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElementPath dePath = arguments.getDataElementPath();
        Object obj = WebServicesServlet.getSessionCache().getObject( NEW_ELEMENT_BEAN_PREFIX + dePath );
        if( ! ( obj instanceof DataElement ) )
        {
            response.error( "Element not found " + dePath.toString() );
            return;
        }
        DataElement de = (DataElement)obj;
        DataCollection<DataElement> parent = dePath.getParentCollection();
        if( putIfAbsent( parent, de ) )
        {
            WebServicesServlet.getSessionCache().removeObject( NEW_ELEMENT_BEAN_PREFIX + dePath );
            response.sendString( "ok" );
        }
        else
        {
            response.sendString( "exists" );
        }
    }

    private boolean putIfAbsent(DataCollection<DataElement> parent, DataElement de) throws Exception
    {
        synchronized( parent )
        {
            if( parent.contains( de ) )
                return false;
            parent.put( de );
            return true;
        }
    }



}
