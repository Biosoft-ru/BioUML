package ru.biosoft.server.servlets.webservices.providers;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.table.export.TableDPSWrapper;

/**
 * Provides data element for the export depending on type
 * @author anna
 *
 */
public class CommonExportedDeProvider implements ExportedDeProvider
{
    @Override
    public @Nonnull ru.biosoft.access.core.DataElement getExportedDataElement(String type, BiosoftWebRequest params) throws WebException
    {
        DataElement de;
        DataElementPath path = params.getDataElementPath();
        if( type.equals( "Diagram" ) )
            de = WebDiagramsProvider.getDiagramChecked( path );
        else if( type.equals( "Table" ) )
        {
            Object deObj = WebBeanProvider.getBean( path.toString() );
            if( deObj instanceof DataElement )
            {
                de = (DataElement)deObj;
            }
            else if( deObj instanceof DynamicPropertySet[] )
            {
                de = new TableDPSWrapper( (DynamicPropertySet[])deObj );
            }
            else
                throw new WebException( "EX_QUERY_NO_EXPORT_ELEMENT", path );
        }
        else //type.equals("Element")
        {
            Object deObj = WebBeanProvider.getBean( path.toString() );
            if( deObj instanceof DataElement )
            {
                de = (DataElement)deObj;
            }
            else
                throw new WebException( "EX_QUERY_NO_EXPORT_ELEMENT", path );
        }
        return de;
    }
}
