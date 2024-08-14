package biouml.workbench.graphsearch;

import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.biohub.TargetOptions;

/**
 * Base interface for graph search engine
 */
public interface QueryEngine
{
    /**
     * Returns public name
     */
    public String getName( TargetOptions dbOptions );
    /**
     * Indicates availability of searchLinked.
     * @return rang of availability, 0 - searchLinked not possible
     */
    int canSearchLinked ( TargetOptions dbOptions );

    /**
     * Search linked elements.
     */
    SearchElement[] searchLinked ( SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl ) throws Exception;
    /**
     * Indicates availability of searchPath.
     * @return rang of availability, 0 - searchPath not possible
     */
    int canSearchPath ( TargetOptions dbOptions );

    /**
     * Search path between elements.
     */
    SearchElement[] searchPath ( SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl ) throws Exception;
    /**
     * Indicates availability of searchSet.
     * @return rang of availability, 0 - searchSet not possible
     */
    int canSearchSet ( TargetOptions dbOptions );

    /**
     * Search minimal set with input elements.
     */
    SearchElement[] searchSet ( SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl ) throws Exception;

}
