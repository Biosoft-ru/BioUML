package ru.biosoft.server.servlets.webservices.providers;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        Set<String> availablePerspectives = new LinkedHashSet<>();

        if(SecurityManager.isProductAvailable( "Perspectives" ))
        {
            availablePerspectives = PerspectiveRegistry.perspectives().filter( perspective -> {
                Permission permissions = SecurityManager.getPermissions( DataElementPath.create( "perspectives/" + perspective.getTitle() ) );
                return permissions.isReadAllowed();
            }).map( Perspective::getTitle ).collect( Collectors.toCollection( LinkedHashSet::new ) );
        }
        else
        {
            availablePerspectives = PerspectiveRegistry.perspectives()
                    .map( Perspective::getTitle )
                    .collect( Collectors.toCollection( LinkedHashSet::new ) );
        }

        if(availablePerspectives.isEmpty())
        {
            response.error( "No perspectives available. This can be caused by IP limitations from Security Provider" );
            return;
        }

        boolean ignoreIfSame = arguments.getBoolean( "ignoreIfSame" );
        boolean ignore = false;
        String perspectiveName = arguments.get("name");
        if(perspectiveName == null)
        {
            perspectiveName = Application.getPreferences().getStringValue("Perspective", availablePerspectives.iterator().next());
        }
        else
        {
            Application.getPreferences().addValue( "Perspective", perspectiveName );
            ignore = ignoreIfSame;
        }

        if(!availablePerspectives.contains( perspectiveName ))
        {
            //rather than falling with error, allow user to login with default perspective
            perspectiveName = availablePerspectives.iterator().next();
            Application.getPreferences().addValue("Perspective", perspectiveName);
        }

        String prevPerspective = ( String )WebSession.getCurrentSession().getValue( "currentPerspective" );

        if( prevPerspective == null || !prevPerspective.equals( perspectiveName ) )
        {             
            TasksSqlTransformer.logTaskRecord( SecurityManager.getSessionUser(), TaskInfo.generateName( null ),
            new java.sql.Timestamp( System.currentTimeMillis() ), new java.sql.Timestamp( System.currentTimeMillis() ),
                TaskInfo.PERSPECTIVE, perspectiveName, (String)null, 
                "SESSION=" + WebSession.getCurrentSession().getSessionId(), 
                true );

            WebSession.getCurrentSession().putValue( "currentPerspective", perspectiveName );
        }

        Perspective perspective = PerspectiveRegistry.getPerspective(perspectiveName);
        response.sendJSON(new JsonObject()
            .add( "perspective", perspective.toJSON() )
            .add( "ignore", ignore )
            .add( "names", PerspectiveRegistry.perspectives().map( Perspective::getTitle )
                    .filter( availablePerspectives::contains )
                    .map( JsonValue::valueOf ).collect(JsonUtils.toArray())));
    }
}
