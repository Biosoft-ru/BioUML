package biouml.workbench.perspective;

import java.util.Collections;
import java.util.Comparator;
import one.util.streamex.StreamEx;

import org.eclipse.core.runtime.IConfigurationElement;

import ru.biosoft.util.ExtensionRegistrySupport;

/**
 * @author lan
 */
public class PerspectiveRegistry extends ExtensionRegistrySupport<Perspective>
{
    private static final PerspectiveRegistry instance = new PerspectiveRegistry();

    private PerspectiveRegistry()
    {
        super("biouml.workbench.perspective", "name");
    }

    @Override
    protected Perspective loadElement(IConfigurationElement element, String elementName) throws Exception
    {
        return new BioUMLPerspective(element);
    }

    @Override
    protected void postInit()
    {
        Collections.sort( extensions, Comparator.comparing( Perspective::getPriority ).reversed().thenComparing( Perspective::getTitle ) );
    }

    public static Perspective getDefaultPerspective()
    {
        return instance.stream().findFirst().orElse( null );
    }
    
    public static StreamEx<Perspective> perspectives()
    {
        return instance.stream();
    }
    
    public static Perspective getPerspective(String name)
    {
        return instance.getExtension(name);
    }
}
