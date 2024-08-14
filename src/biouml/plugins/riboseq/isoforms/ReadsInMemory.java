package biouml.plugins.riboseq.isoforms;

public class ReadsInMemory<T> implements ReadReader<T>
{
    private T[] data;
    private int cur;
    
    public ReadsInMemory(T[] data)
    {
        this.data = data;
    }
    
    @Override
    public void reset()
    {
        cur = 0;
    }

    @Override
    public T read()
    {
        if( cur < data.length )
            return data[cur++];
        return null;
    }

    @Override
    public void close() {}
}
