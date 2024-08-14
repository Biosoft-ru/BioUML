package ru.biosoft.util.archive;

import java.io.IOException;

/**
 * ArchiveFile which unpacks automatically all supported nested archives
 * @author lan
 */
public class ComplexArchiveFile implements ArchiveFile
{
    private ArchiveFile source;
    private ArchiveFile subFile = null;

    public ComplexArchiveFile(ArchiveFile source)
    {
        this.source = source;
    }

    @Override
    public boolean isValid()
    {
        return source.isValid();
    }

    @Override
    public ArchiveEntry getNextEntry() throws IOException
    {
        ArchiveEntry result = null;
        if(subFile != null)
        {
            result = subFile.getNextEntry();
            if(result != null) return result;
            subFile.close();
        }
        result = source.getNextEntry();
        if(result == null) return null;
        subFile = ArchiveFactory.getArchiveFile(result);
        if(subFile != null)
        {
            subFile = new ComplexArchiveFile(subFile);
            return getNextEntry();
        }
        return result;
    }

    @Override
    public void close()
    {
        if(subFile != null)
            subFile.close();
        if(source != null)
            source.close();
    }

    @Override
    public long offset()
    {
        return source.offset();
    }

}
