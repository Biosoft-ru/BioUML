package biouml.workbench.graphsearch;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.TargetOptions;

import biouml.standard.type.Base;

import ru.biosoft.jobcontrol.JobControl;

public class QueryEngineSupport implements QueryEngine
{
    @Override
    public String getName( TargetOptions dbOptions )
    {
        return "Unspesified collection search";
    }
    
    @Override
    public int canSearchLinked(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public int canSearchPath(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public int canSearchSet(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public SearchElement[] searchLinked(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions,
            JobControl jobControl) throws Exception
    {
        return null;
    }

    @Override
    public SearchElement[] searchPath(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl)
            throws Exception
    {
        return null;
    }

    @Override
    public SearchElement[] searchSet(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl)
            throws Exception
    {
        return null;
    }
    
    protected String getPath(Base base)
    {
        return DataElementPath.create(base).toString();
    }
}
