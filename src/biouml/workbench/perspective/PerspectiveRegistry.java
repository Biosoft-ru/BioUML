package biouml.workbench.perspective;

import java.util.Collections;
import java.util.Comparator;
import one.util.streamex.StreamEx;

import org.eclipse.core.runtime.IConfigurationElement;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.util.ExtensionRegistrySupport;

/**
 * @author lan
 */
public class PerspectiveRegistry extends ExtensionRegistrySupport<Perspective>
{
    /**
     * Name of the preference which stores the user's perspective
     */
    public static final String PERSPECTIVE_PREFERENCE = "Perspective";

    /**
     * Session cache key of the perspective selected for the current session only
     */
    private static final String SESSION_PERSPECTIVE_KEY = "perspective/session";

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

    /**
     * Selects the perspective for the current session only: the user's perspective preference is not changed
     * @param name perspective name, or null to return to the user's preference
     * @return false if the current session has no session cache to keep the selection in
     */
    public static boolean setSessionPerspective(String name)
    {
        SessionCache cache = SessionCacheManager.getSessionCache( SecurityManager.getSession() );
        if( cache == null )
            return false;
        if( name == null )
            cache.removeObject( SESSION_PERSPECTIVE_KEY );
        else
            cache.addObject( SESSION_PERSPECTIVE_KEY, name, true );
        return true;
    }

    /**
     * @return name of the perspective selected for the current session only, or null
     */
    public static String getSessionPerspective()
    {
        SessionCache cache = SessionCacheManager.getSessionCache( SecurityManager.getSession() );
        Object name = cache == null ? null : cache.getObject( SESSION_PERSPECTIVE_KEY );
        return name instanceof String ? (String)name : null;
    }

    /**
     * @return name of the perspective in effect: the one selected for the current session,
     * otherwise the user's perspective preference, otherwise defaultName
     */
    public static String getCurrentPerspectiveName(String defaultName)
    {
        String name = getSessionPerspective();
        if( name != null )
            return name;
        Preferences preferences = Application.getPreferences();
        return preferences != null ? preferences.getStringValue( PERSPECTIVE_PREFERENCE, defaultName ) : defaultName;
    }
}
