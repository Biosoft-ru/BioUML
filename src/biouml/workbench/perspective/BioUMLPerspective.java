package biouml.workbench.perspective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;


/**
 * Default implementation of Perspective in BioUML
 * @author lan
 */
public class BioUMLPerspective implements Perspective
{
    private static Logger log = Logger.getLogger( BioUMLPerspective.class.getName() );

    private static final String IMPORTERS_ATTR = "importers";
    private static final String EXPORTERS_ATTR = "exporters";
    private static final String VIEWPARTS_ATTR = "viewparts";
    private static final String TAB_ATTR = "tab";
    private static final String REPOSITORY_ATTR = "repository";
    private static final String MESSAGEBUNDLE_ATTR = "messageBundle";
    private static final String PRIORITY_ATTR = "priority";
    private static final String NAME_ATTR = "name";
    private static final String INTRO_ATTR = "intro";
    private static final String ACTIONS_ATTR = "actions";
    private static final String PROJECTSELECTOR_ATTR = "projectSelector";
    private static final String TEMPLATE_ATTR = "template";
    private static final String HIDEDIAGRAMPANEL_ATTR = "hideDiagramPanel";
    private static final String CLOSEONLYONSESSIONEXPIRE_ATTR = "closeOnlyOnSessionExpire";

    private final String title;
    private final List<RepositoryTabInfo> tabInfo = new ArrayList<>();
    private final List<Rule> viewPartRules = new ArrayList<>();
    private final List<Rule> actionRules = new ArrayList<>();
    private final List<Rule> importerRules = new ArrayList<>();
    private final List<Rule> exporterRules = new ArrayList<>();

    private final Map<String,String> messageBundle = new HashMap<>();

    private int priority;
    private String introPage;
    private boolean showProjectSelector = true;
    private String defaultTemplate = null;
    private boolean hideDiagramPanel = false;
    private boolean closeOnlyOnSessionExpire = false;

    protected BioUMLPerspective(IConfigurationElement element)
    {
        title = element.getAttribute(NAME_ATTR);
        try
        {
            priority = Integer.parseInt(element.getAttribute(PRIORITY_ATTR));
        }
        catch( Exception e )
        {
            priority = 0;
        }

        if( element.getAttribute( PROJECTSELECTOR_ATTR ) != null )
            showProjectSelector = Boolean.parseBoolean( element.getAttribute( PROJECTSELECTOR_ATTR ) );

        IConfigurationElement[] repository = element.getChildren(REPOSITORY_ATTR);
        if( repository != null && repository.length > 0 )
        {
            IConfigurationElement[] tabs = repository[0].getChildren(TAB_ATTR);
            if( tabs != null )
            {
                for( IConfigurationElement tab : tabs )
                {
                    tabInfo.add(new RepositoryTabInfo(tab));
                }
            }
        }

        IConfigurationElement[] messagesArray = element.getChildren(MESSAGEBUNDLE_ATTR);
        if( messagesArray != null && messagesArray.length > 0)
        {
            IConfigurationElement[] messages = messagesArray[0].getChildren();
            if( messages != null )
            {
                for( IConfigurationElement message : messages )
                {
                    messageBundle.put( message.getAttribute( "key" ), message.getAttribute( "value" ) );
                }
            }
        }

        IConfigurationElement[] viewParts = element.getChildren(VIEWPARTS_ATTR);
        if( viewParts != null && viewParts.length > 0)
        {
            IConfigurationElement[] rules = viewParts[0].getChildren();
            if( rules != null )
            {
                for( IConfigurationElement rule : rules )
                {
                    viewPartRules.add(new Rule(rule));
                }
            }
        }

        IConfigurationElement[] actions = element.getChildren( ACTIONS_ATTR );
        if( actions != null && actions.length > 0 )
        {
            IConfigurationElement[] rules = actions[0].getChildren();
            if( rules != null )
            {
                for( IConfigurationElement rule : rules )
                {
                    actionRules.add( new Rule( rule ) );
                }
            }
        }

        IConfigurationElement[] importers = element.getChildren( IMPORTERS_ATTR );
        if( importers != null && importers.length > 0 )
        {
            IConfigurationElement[] rules = importers[0].getChildren();
            if( rules != null )
            {
                for( IConfigurationElement rule : rules )
                {
                    importerRules.add( new Rule( rule ) );
                }
            }
        }
        IConfigurationElement[] exporters = element.getChildren( EXPORTERS_ATTR );
        if( exporters != null && exporters.length > 0 )
        {
            IConfigurationElement[] rules = exporters[0].getChildren();
            if( rules != null )
            {
                for( IConfigurationElement rule : rules )
                {
                    exporterRules.add( new Rule( rule ) );
                }
            }
        }
        try
        {
            introPage = element.getAttribute(INTRO_ATTR);
            defaultTemplate = element.getAttribute( TEMPLATE_ATTR );
            hideDiagramPanel = Boolean.parseBoolean( element.getAttribute( HIDEDIAGRAMPANEL_ATTR ) );
            closeOnlyOnSessionExpire = Boolean.parseBoolean( element.getAttribute( CLOSEONLYONSESSIONEXPIRE_ATTR ) );
        }
        catch( Exception e )
        {
            log.fine( "Can not load all perspective attributes for " + title );
        }
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public List<RepositoryTabInfo> getRepositoryTabs()
    {
        return Collections.unmodifiableList(tabInfo);
    }

    @Override
    public boolean isViewPartAvailable(String viewPartId)
    {
        return matchRules( viewPartRules, viewPartId );
    }

    @Override
    public String getIntroPage()
    {
        return introPage;
    }

    @Override
    public int getPriority()
    {
        return priority;
    }

    @Override
    public JsonObject toJSON()
    {
        JsonObject result = new JsonObject();
        result.add(NAME_ATTR, title);
        result.add(PRIORITY_ATTR, priority);

        if( introPage != null )
            result.add(INTRO_ATTR, introPage);
        if( showProjectSelector )
            result.add( PROJECTSELECTOR_ATTR, showProjectSelector );
        if( defaultTemplate != null )
            result.add( TEMPLATE_ATTR, defaultTemplate );
        if( hideDiagramPanel )
            result.add( HIDEDIAGRAMPANEL_ATTR, hideDiagramPanel );
        if( closeOnlyOnSessionExpire )
            result.add( CLOSEONLYONSESSIONEXPIRE_ATTR, closeOnlyOnSessionExpire );

        if( messageBundle.size() > 0 )
        {
            JsonObject bundle = new JsonObject();
            messageBundle.forEach( ( k, v ) -> bundle.add( k, v ) );
            result.add( MESSAGEBUNDLE_ATTR, bundle );
        } 

        JsonArray repository = new JsonArray();
        for( RepositoryTabInfo tab : tabInfo )
        {
            repository.add(tab.toJSON());
        }
        result.add(REPOSITORY_ATTR, repository);
        JsonArray viewParts = new JsonArray();
        for( Rule rule : viewPartRules )
        {
            viewParts.add(rule.toJSON());
        }
        result.add(VIEWPARTS_ATTR, viewParts);
        JsonArray actions = new JsonArray();
        for( Rule rule : actionRules )
        {
            actions.add( rule.toJSON() );
        }
        result.add( ACTIONS_ATTR, actions );
        return result;
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    @Override
    public boolean isActionAvailable(String actionId)
    {
        return matchRules( actionRules, actionId );
    }

    @Override
    public boolean isImporterAvailable(String importerId)
    {
        return matchRules( importerRules, importerId );
    }

    @Override
    public boolean isExporterAvailable(String exporterId)
    {
        return matchRules( exporterRules, exporterId );
    }

    private static boolean matchRules(List<Rule> rules, String id)
    {
        boolean result = true;
        for( Rule rule : rules )
        {
            if( rule.isMatched( id ) )
                result = rule.isAllow();
        }
        return result;
    }

    @Override
    public String getDefaultTemplate()
    {
        return defaultTemplate;
    }
}
