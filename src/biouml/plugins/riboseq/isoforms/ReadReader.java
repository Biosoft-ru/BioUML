package biouml.plugins.riboseq.isoforms;

public interface ReadReader<ReadType>
{
    void reset();
    ReadType read();
    void close();
    
    ReadReader<Read> EMPTY = new ReadReader<Read>() {
        @Override
        public void reset() { }
        @Override
        public Read read() { return null; }
        @Override
        public void close() {}
    } ;
}
