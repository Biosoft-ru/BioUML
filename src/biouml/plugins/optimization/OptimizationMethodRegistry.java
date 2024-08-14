package biouml.plugins.optimization;

import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.eclipse.core.runtime.IConfigurationElement;

import one.util.streamex.StreamEx;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;
import ru.biosoft.analysiscore.javascript.JavaScriptAnalysisHost;
import ru.biosoft.util.ExtensionRegistrySupport;

public class OptimizationMethodRegistry extends ExtensionRegistrySupport<AnalysisMethodInfo>
{

    private static final OptimizationMethodRegistry instance = new OptimizationMethodRegistry();
    private static Logger log = Logger.getLogger( OptimizationMethodRegistry.class.getName() );

    private static final String NAME_ATTR = "name";
    private static final String CLASS_ATTR = "class";
    public static final String DESCRIPTION_ATTR = "description";
    public static final String SHORT_DESCRIPTION_ATTR = "shortDescription";
    public static final String JS_ATTR = "js";

    public OptimizationMethodRegistry()
    {
        super( "biouml.plugins.optimization.method", NAME_ATTR );
    }

    @Override
    protected AnalysisMethodInfo loadElement(IConfigurationElement element, String elementName) throws Exception
    {
        String name = getStringAttribute( element, NAME_ATTR );
        String className = getStringAttribute( element, CLASS_ATTR );
        String description = getStringAttribute( element, DESCRIPTION_ATTR );
        String shortDescription = element.getAttribute( SHORT_DESCRIPTION_ATTR );
        String js = element.getAttribute( JS_ATTR );

        Class<? extends AnalysisMethod> methodClass = ClassLoading.loadSubClass( className, OptimizationMethod.class );
        AnalysisMethodInfo info = new AnalysisMethodInfo( name, description, null, methodClass, js );

        if( shortDescription != null )
            info.setShortDescription( shortDescription );
        if( js != null && js.contains( "." ) )
        {
            String[] fields = js.split( "\\." );
            JavaScriptAnalysisHost.addAnalysis( fields[0], fields[1], info );
        }
        return info;
    }

    protected AnalysisMethodInfo getMethod(String methodName)
    {
        AnalysisMethodInfo analysisMethodInfo = instance.getExtension( methodName );
        return analysisMethodInfo;
    }

    public static @Nonnull StreamEx<String> getOptimizationMethodNames()
    {
        return instance.stream().map( e -> e.getName() );
    }

    public static OptimizationMethod<?> getOptimizationMethod(String methodName)
    {
        AnalysisMethodInfo ami = instance.getMethod( methodName );
        return ami == null ? null : (OptimizationMethod<?>)ami.createAnalysisMethod();
    }

    public static AnalysisMethodInfo getMethodInfo(String methodName)
    {
        return instance.getMethod( methodName );
    }

}
