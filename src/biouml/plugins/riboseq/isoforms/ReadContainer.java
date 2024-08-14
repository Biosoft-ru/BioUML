package biouml.plugins.riboseq.isoforms;

import java.util.EnumMap;

import biouml.plugins.riboseq.isoforms.Read.Type;

public class ReadContainer
{
    private EnumMap<Type, Integer> countsByType = new EnumMap<>(Type.class);
    private EnumMap<Type, ReadReader<Read>> readers = new EnumMap<>(Type.class);
    private int minL, maxL;
    
    public ReadContainer(ReadReader<Read> mapped, ReadReader<Read> unmapped, ReadReader<Read> filtered)
    {
        readers.put( Type.MAPPED, mapped );
        readers.put( Type.UNMAPPED, unmapped );
        readers.put( Type.FILTERED, filtered );
        
        minL = Integer.MAX_VALUE;
        maxL = 0;
        for( Type t : Type.values() )
        {
            ReadReader<Read> reader = readers.get( t );
            reader.reset();
            int c = 0;
            Read r;
            while( (r = reader.read()) != null )
            {
                c++;
                if(r.seq.length < minL)
                    minL = r.seq.length;
                if(r.seq.length > maxL)
                    maxL = r.seq.length;
            }
            countsByType.put( t, c );
            
        }
    }

    public int getMappedCount() { return countsByType.get( Type.MAPPED ); }
    public int getUnmappedCount() { return countsByType.get( Type.UNMAPPED ); }
    public int getFilteredCount() { return countsByType.get( Type.FILTERED ); }
    
    public int getMinReadLen() { return minL; }
    public int getMaxReadLen() { return maxL; }

    public ReadReader<Read> getReader(Type type)
    {
        return readers.get( type );
    }
}
