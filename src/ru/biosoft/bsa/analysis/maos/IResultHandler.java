package ru.biosoft.bsa.analysis.maos;

public interface IResultHandler
{
    void init();
    
    void siteMutationEvent(SiteMutation siteMutation);

    void finish() throws Exception;

    Object[] getResults();
}