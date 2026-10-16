package ru.biosoft.server.servlets.webservices.providers;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import one.util.streamex.StreamEx;

import com.developmentontheedge.application.Application;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import biouml.workbench.perspective.Perspective;
import biouml.workbench.perspective.PerspectiveRegistry;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.util.JsonUtils;

import ru.biosoft.tasks.TasksSqlTransformer;
import ru.biosoft.tasks.TaskInfo;

/**
 * @author lan
 *
 */
public class PerspectivesProvider extends WebJSONProviderSupport
{
    private static final Logger log = Logger.getLogger( PerspectivesProvider.class.getName() );

    /**
     * Value of the "scope" parameter which selects the perspective for the current session only,
     * leaving the user's perspective preference unchanged
     */
    public static final String SCOPE_SESSION = "session";

    /**
     * @return titles of the perspectives the current user may open, in registry order
     */
    public static Set<String> getAvailablePerspectives()
    {
        StreamEx<Perspective> perspectives = PerspectiveRegistry.perspectives();
        if(SecurityManager.isProductAvailable( "Perspectives" ))
        {
            perspectives = perspectives.filter( perspective -> {
                Permission permissions = SecurityManager.getPermissions( DataElementPath.create( "perspectives/" + perspective.getTitle() ) );
                return permissions.isReadAllowed();
            });
        }
        return perspectives.map( Perspective::getTitle ).toCollection( LinkedHashSet::new );
    }

    /**
     * @param availablePerspectives non-empty result of {@link #getAvailablePerspectives()}
     * @return title of the perspective in effect for the current session if it is available, otherwise of the first available one
     */
    public static String getCurrentPerspective(Set<String> availablePerspectives)
    {
        String defaultPerspective = availablePerspectives.iterator().next();
        String perspectiveName = PerspectiveRegistry.getCurrentPerspectiveName( defaultPerspective );
        return availablePerspectives.contains( perspectiveName ) ? perspectiveName : defaultPerspective;
    }

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        Set<String> availablePerspectives = getAvailablePerspectives();

        if(availablePerspectives.isEmpty())
        {
            response.error( "No perspectives available. This can be caused by IP limitations from Security Provider" );
            return;
        }

        boolean ignoreIfSame = arguments.getBoolean( "ignoreIfSame" );
        boolean ignore = false;
        boolean sessionOnly = SCOPE_SESSION.equals( arguments.get( "scope" ) );
        String perspectiveName = arguments.get("name");
        boolean store = perspectiveName != null;
        if(perspectiveName == null)
        {
            perspectiveName = PerspectiveRegistry.getCurrentPerspectiveName( availablePerspectives.iterator().next() );
            sessionOnly = PerspectiveRegistry.getSessionPerspective() != null;
        }
        else
        {
            ignore = ignoreIfSame;
        }

        if(!availablePerspectives.contains( perspectiveName ))
        {
            //rather than falling with error, allow user to login with default perspective
            perspectiveName = availablePerspectives.iterator().next();
            store = true;
        }

        if(store)
        {
            if(sessionOnly)
            {
                if(!PerspectiveRegistry.setSessionPerspective( perspectiveName ))
                {
                    response.error( "Cannot select perspective for the current session: session is not initialized" );
                    return;
                }
            }
            else
            {
                PerspectiveRegistry.setSessionPerspective( null );
                Application.getPreferences().addValue( PerspectiveRegistry.PERSPECTIVE_PREFERENCE, perspectiveName );
            }
        }

        String prevPerspective = ( String )WebSession.getCurrentSession().getValue( "currentPerspective" );

        if( prevPerspective == null || !prevPerspective.equals( perspectiveName ) )
        {
            try
            {
                TasksSqlTransformer.logTaskRecord( SecurityManager.getSessionUser(), TaskInfo.generateName( null ),
                new java.sql.Timestamp( System.currentTimeMillis() ), new java.sql.Timestamp( System.currentTimeMillis() ),
                    TaskInfo.PERSPECTIVE, perspectiveName, (String)null,
                    "SESSION=" + WebSession.getCurrentSession().getSessionId(),
                    true );
            }
            catch( Exception e )
            {
                log.log( Level.WARNING, "Cannot log the change of perspective to " + perspectiveName, e );
            }

            WebSession.getCurrentSession().putValue( "currentPerspective", perspectiveName );
        }

        Perspective perspective = PerspectiveRegistry.getPerspective(perspectiveName);
        response.sendJSON(new JsonObject()
            .add( "perspective", perspective.toJSON() )
            .add( "ignore", ignore )
            .add( "names", StreamEx.of( availablePerspectives ).map( JsonValue::valueOf ).collect(JsonUtils.toArray())));
    }
}
