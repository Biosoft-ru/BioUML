package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ru.biosoft.access.AbstractFileTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.ChrNameMapping;

public class ChrNameMappingTransformer extends AbstractFileTransformer<ChrNameMapping>
{
    @Override
    public Class<? extends ChrNameMapping> getOutputType()
    {
        return ChrNameMapping.class;
    }

    @Override
    public ChrNameMapping load(File file, String name, DataCollection<ChrNameMapping> origin) throws Exception
    {
        ChrNameMapping result = new ChrNameMapping( name, origin );
        try (BufferedReader reader = new BufferedReader( new FileReader( file ) ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                String[] parts = line.split( "\t", 2 );
                result.srcToDst.put( parts[0], parts[1] );
            }
        }
        result.srcToDst.forEach( (k, v) -> result.dstToSrc.put( v, k ) );
        return result;
    }

    @Override
    public void save(File output, ChrNameMapping element) throws Exception
    {
        throw new UnsupportedOperationException();
    }
}