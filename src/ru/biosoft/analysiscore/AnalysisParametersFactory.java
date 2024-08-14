package ru.biosoft.analysiscore;

import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;

/**
 * Utility class for saving and restoring {@link AnalysisParameters} for {@link ru.biosoft.access.core.DataElement}
 */
public class AnalysisParametersFactory
{
    public static final String ANALYSIS_PREFIX = "analysis.";
    public static final String ANALYSIS_NAME_PROPERTY = "analysisName";
    public static final String PARAMETERS_DESCRIPTION_PROPERTY = "analysisParametersDescription";
    /**
     * Write {@link AnalysisParameters} to info object of {@link ru.biosoft.access.core.DataCollection}
     */
    public static void write(DataElement de, @Nonnull AnalysisMethod analysis)
    {
        write( de, analysis, getPrefix( de ) );
    }
    
    public static void write(DataElement de, @Nonnull AnalysisMethod analysis, String prefix)
    {
        DataCollection<?> targetDC = null;
        if( de instanceof DataCollection )
        {
            targetDC = (DataCollection<?>)de;
        }
        else
        {
            targetDC = de.getOrigin();
        }

        if( targetDC != null )
        {
            Properties properties = targetDC.getInfo().getProperties();
            properties.put( prefix.startsWith( DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX ) ? prefix + ANALYSIS_NAME_PROPERTY
                            : ANALYSIS_NAME_PROPERTY, analysis.getName() );
            AnalysisParameters parameters = analysis.getParameters();
            parameters.write( properties, prefix );
            if( !prefix.startsWith( DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX ) && !analysis.getName().isEmpty())
            {
                properties.put( PARAMETERS_DESCRIPTION_PROPERTY, AnalysesPropertiesWriter.getParametersHTMLDescription( parameters ) );
            }
            properties.put( prefix + "class", parameters.getClass().getName() );
        }
    }
    
    public static void writePersistent(DataElement de, @Nonnull AnalysisMethod analysis)
    {
        write( de, analysis, DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX + getPrefix( de ) );
    }

    public static Class<? extends AnalysisParameters> getAnalysisClass(DataElement de)
    {
        return getAnalysisClass(de, getPrefix(de));
    }

    public static Class<? extends AnalysisParameters> getPersistentAnalysisClass(DataElement de)
    {
        return getAnalysisClass(de, DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX + getPrefix(de));
    }

    /**
     * @param de
     * @param prefix
     * @return
     */
    private static Class<? extends AnalysisParameters> getAnalysisClass(DataElement de, String prefix)
    {
        DataCollection<?> targetDC = null;
        if( de instanceof DataCollection )
        {
            targetDC = (DataCollection<?>)de;
        }
        else
        {
            targetDC = de.getOrigin();
        }

        if( targetDC != null )
        {
            String className = targetDC.getInfo().getProperty(prefix + "class");
            if( className != null )
            {
                String pluginName = targetDC.getInfo().getProperty(prefix + "plugin");
                try
                {
                    return ClassLoading.loadSubClass( className, pluginName == null ? ClassLoading.getPluginForClass( className )
                    : pluginName, AnalysisParameters.class );
                }
                catch( Exception e )
                {
                }
            }
        }
        return null;
    }
    
    /**
     * Read {@link AnalysisParameters} from info object of {@link ru.biosoft.access.core.DataCollection}
     */
    public static AnalysisParameters read(DataElement de)
    {
        return read(de, getPrefix(de));
    }
    
    public static AnalysisParameters read(DataElement de, String prefix)
    {
        DataCollection<?> targetDC = null;
        if( de instanceof DataCollection )
        {
            targetDC = (DataCollection<?>)de;
        }
        else
        {
            targetDC = de.getOrigin();
        }

        if( targetDC != null )
        {
            String analysisName = prefix.startsWith(DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX) ? null : targetDC.getInfo()
                    .getProperty("analysisName");
            if( analysisName != null )
            {
                AnalysisMethodInfo methodInfo = AnalysisMethodRegistry.getMethodInfo( analysisName );
                if( methodInfo != null )
                {
                    AnalysisParameters params = methodInfo.createAnalysisMethod().getParameters();
                    params.read( targetDC.getInfo().getProperties(), prefix );
                    return params;
                }
            } else
            {
                String className = targetDC.getInfo().getProperty(prefix + "class");
                if( className != null )
                {
                    String pluginName = targetDC.getInfo().getProperty(prefix + "plugin");
                    AnalysisParameters params = getEmptyParametersObject(className, pluginName);
                    if( params != null )
                    {
                        Properties properties = targetDC.getInfo().getProperties();
                        params.read( properties, prefix );
                        return params;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Read {@link AnalysisParameters} from persistent info of {@link ru.biosoft.access.core.DataCollection}
     */
    public static AnalysisParameters readPersistent(DataElement de)
    {
        return read(de, DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX + getPrefix(de));
    }
    
    /**
     * Create new instance of {@link AnalysisParameters} object
     */
    public static @CheckForNull AnalysisParameters getEmptyParametersObject(@Nonnull String className)
    {
        return getEmptyParametersObject(className, null);
    }

    public static @CheckForNull AnalysisParameters getEmptyParametersObject(@Nonnull String className, @CheckForNull String pluginName)
    {
        try
        {
            Class<? extends AnalysisParameters> cl = pluginName == null ? ClassLoading.loadSubClass( className, AnalysisParameters.class ) : ClassLoading.loadSubClass( className, pluginName, AnalysisParameters.class );
            try
            {
                return cl.newInstance();
            }
            catch( InstantiationException e )
            {
                return (AnalysisParameters)cl.getSuperclass().newInstance();
            }
        }
        catch( Exception e )
        {
        }
        return null;
    }

    /**
     * Create prefix string
     */
    public static String getPrefix(DataElement de)
    {
        String prefix = ANALYSIS_PREFIX;
        if( ! ( de instanceof DataCollection ) )
        {
            prefix += de.getName() + ".";
        }
        return prefix;
    }

    public static AnalysisMethod readAnalysis(DataElement de)
    {
        return readAnalysis( de, getPrefix( de ) );
    }
    public static AnalysisMethod readAnalysisPersistent(DataElement de)
    {
        return readAnalysis( de, DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX + getPrefix( de ) );
    }

    public static AnalysisMethod readAnalysis(DataElement de, String prefix)
    {
        DataCollection<?> targetDC = de instanceof DataCollection ? (DataCollection<?>)de : de.getOrigin();

        if( targetDC != null )
        {
            String analysisName = prefix.startsWith( DataCollectionUtils.PERSISTENT_PROPERTY_PREFIX ) ? targetDC.getInfo().getProperty(
                    prefix + "analysisName" ) : targetDC.getInfo().getProperty( "analysisName" );
            if( analysisName != null )
            {
                AnalysisMethodInfo methodInfo = AnalysisMethodRegistry.getMethodInfo( analysisName );
                if( methodInfo != null )
                {
                    AnalysisMethod analysis = methodInfo.createAnalysisMethod();
                    AnalysisParameters params = analysis.getParameters();
                    params.read( targetDC.getInfo().getProperties(), prefix );
                    analysis.setParameters( params );
                    return analysis;
                }
            }
        }
        return null;
    }
}
