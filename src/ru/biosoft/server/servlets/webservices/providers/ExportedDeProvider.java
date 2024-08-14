package ru.biosoft.server.servlets.webservices.providers;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;

public interface ExportedDeProvider
{
    public @Nonnull ru.biosoft.access.core.DataElement getExportedDataElement(String type, BiosoftWebRequest params) throws WebException;
}