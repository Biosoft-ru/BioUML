package ru.biosoft.access.html;

import java.util.Arrays;
import java.util.List;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.FileExporter;

public class ZipHtmlExporter extends FileExporter
{
    @Override
    public int accept(DataElement de)
    {
        return de instanceof ZipHtmlDataCollection ? ACCEPT_HIGH_PRIORITY : ACCEPT_UNSUPPORTED;
    }
    
    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Arrays.asList( ZipHtmlDataCollection.class );
    }
}
