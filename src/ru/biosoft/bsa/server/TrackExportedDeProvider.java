package ru.biosoft.bsa.server;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.providers.ExportedDeProvider;

/**
 * Provides TrackRegion data element for the export
 * @author anna
 *
 */

public class TrackExportedDeProvider implements ExportedDeProvider
{
    @Override
    public @Nonnull ru.biosoft.access.core.DataElement getExportedDataElement(String type, BiosoftWebRequest params) throws WebException
    {
        DataElementPath path = params.getDataElementPath();
        return new TrackRegion( path.getDataElement( Track.class ), params.get( BSAServiceProtocol.SEQUENCE_NAME ),
                params.optInt( BSAServiceProtocol.FROM ), params.optInt( BSAServiceProtocol.TO ) );
    }

}
