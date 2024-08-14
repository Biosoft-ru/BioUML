package ru.biosoft.plugins.docker;

import ru.biosoft.access.CacheableBeanProvider;

public class CWLDockeredAnalysisParametersBeanProvider implements CacheableBeanProvider
{
    @Override
    public Object getBean(String path)
    {
        System.out.println( "CWLDockeredAnalysisParametersBeanProvider: " + path );
        if( !path.startsWith( "local/" ) )
        {
            return null;
        }

        CWLDockeredAnalysisParameters pars = new CWLDockeredAnalysisParameters();
        int ind1 = path.indexOf( '/' );
        int ind2 = path.lastIndexOf( '/' );
        pars.setDockerImage( path.substring( ind1 + 1, ind2 ) );
        pars.setCwlFile( path.substring( ind2 + 1 ) );
        pars.extractParametersAndOutputs();
        return pars;
    }
}
