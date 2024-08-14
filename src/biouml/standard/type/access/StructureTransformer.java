package biouml.standard.type.access;

import java.io.BufferedReader;
import java.io.Reader;

import biouml.standard.type.Structure;
import ru.biosoft.access.Entry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;

public class StructureTransformer extends BeanInfoEntryTransformer<Structure>
{
    @Override
    public Class<Structure> getOutputType()
    {
        return Structure.class;
    }

    @Override
    public Structure transformInput(Entry input) throws Exception
    {
        Structure structure = super.transformInput(input);
        Reader entryReader = input.getReader();

        BufferedReader reader = null;
        if( entryReader instanceof BufferedReader )
            reader = (BufferedReader)entryReader;
        else
            reader = new BufferedReader(entryReader);

        String data = readData(reader);
        structure.setData(data);

        return structure;
    }

    protected String readData(BufferedReader reader) throws Exception
    {
        StringBuilder buf = new StringBuilder();

        String line;
        while( ( line = reader.readLine() ) != null )
        {
            if( line.startsWith("DA") && line.length() >= 3 )
            {
                buf.append(line.substring(4));
                buf.append(endl);
            }
        }

        return buf.toString();
    }
}
