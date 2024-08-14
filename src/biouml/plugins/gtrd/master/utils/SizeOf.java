package biouml.plugins.gtrd.master.utils;

public interface SizeOf
{
    default long sizeOf()
    {
        final int HEADER_SIZE = 16;
        return SizeOfUtils.align(HEADER_SIZE + _fieldsSize())
                + _childsSize();
    }
    
    default long _childsSize()
    {
        return 0;
    }
    
    default long _fieldsSize()
    {
        return 0;
    }
}
