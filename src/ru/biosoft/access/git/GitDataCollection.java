package ru.biosoft.access.git;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.file.GenericFileDataCollection;

public class GitDataCollection extends GenericFileDataCollection
{

    public GitDataCollection(DataCollection<?> parent, Properties properties) throws IOException
    {
        super( parent, properties );
    }

    public void initGitFilter() throws Exception
    {
        List<String> filter = infoProvider.getFileFilter();
        filter.add( 0, ".git" );
        infoProvider.setFileFilter( filter );
    }

}
