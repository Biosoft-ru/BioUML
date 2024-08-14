package ru.biosoft.analysis;

import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.analysiscore.AnalysisParametersFactory;

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
            return AnalysisParametersFactory.read(CollectionFactory.getDataElement(path));
        }
        catch( Exception e )
        {
            return null;
        }
    }
}
