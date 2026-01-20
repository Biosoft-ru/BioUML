package ru.biosoft.analysis;

import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.analysiscore.AnalysisParameters;

/**
 * @author lan
 *
 */
public class AnalysisRelaunchBeanProvider implements BeanProvider
{
    @Override
    public Object getBean(String path)
    {
        try
        {
            AnalysisParameters parameters = AnalysisParametersFactory.read( CollectionFactory.getDataElement( path ) );
            parameters.setExpertMode( true );
            return parameters;

        }
        catch( Exception e )
        {
            return null;
        }
    }
}
