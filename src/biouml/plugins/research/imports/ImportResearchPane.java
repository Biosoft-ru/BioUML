package biouml.plugins.research.imports;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.Application;

import biouml.model.util.ModulePackager;
import biouml.plugins.research.MessageBundle;
import biouml.plugins.server.access.AccessClient;
import biouml.plugins.server.access.ServerRegistry;
import biouml.workbench.BioUMLApplication;
import biouml.workbench.module.AbstractLoadPane;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.Permission;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;

public class ImportResearchPane extends AbstractLoadPane
{
    public static final @Nonnull String CONNECTION_CLASS_NAME = "ru.biosoft.server.tomcat.TomcatConnection";
    public static final String PLUGINS = "ru.biosoft.server.tomcat";
    public static final String ROOT_DC_NAME = "data/";
    private static final String[] PROJECT_DC_NAMES = new String[] {"Collaboration", "Examples", "Public", "Projects",
            "Collaboration (git)"};
    protected static final MessageBundle researchResources = new MessageBundle();

    public ImportResearchPane(boolean showHelpButton)
    {
        super(showHelpButton ? "load_research_dialog" : null, true, researchResources.getResourceString("LOAD_RESEARCH_DIALOG_INFO_TEXT"));
        PROJECT_DC_NAMES[0] = ru.biosoft.access.core.DataElementPath.escapeName(CollectionFactoryUtils.getUserProjectsPath().getName());
    }

    protected AccessClient connection = null;
    protected String currentHost = null;
    protected AccessClient getConnection(String url) throws Exception
    {
        if( currentHost == null || !url.equals(currentHost) )
        {
            if( connection != null )
            {
                connection.close();
            }
            Class<? extends ClientConnection> connectionClass = ClassLoading.loadSubClass( CONNECTION_CLASS_NAME, PLUGINS, ClientConnection.class );
            ClientConnection conn = ConnectionPool.getConnection(connectionClass, url);
            connection = new AccessClient(new Request(conn, log), log);

            currentHost = url;
        }
        return connection;
    }

    @Override
    protected List<DatabaseLink> getDatabaseLinks(String url, String username, String password) throws Exception
    {
        List<DatabaseLink> result = new ArrayList<>();
        String host = url;
        String usernameStr = username;
        String passwordStr = password;
        AccessClient connection = getConnection(host);
        Permission permission = null;

        try
        {
            permission = connection.login(null, usernameStr, passwordStr);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, ExceptionRegistry.log(e));
            return null;
        }
        ServerRegistry.setServerSession(host, permission.getSessionId());
        for( String projectCollectionName : PROJECT_DC_NAMES )
        {
            Map<String, Properties> researches;
            DataElementPath fullName = DataElementPath.create("data").getChildPath(projectCollectionName);
            ServerRegistry.saveServers();
            try
            {
                researches = connection.getFlaggedNameList(fullName.toString());
            }
            catch( Exception e )
            {
                log.warning(ExceptionRegistry.translateException(e).getMessage());
                continue;
            }

            for( Entry<String, Properties> entry : researches.entrySet() )
            {
                DatabaseLink databaseLink = new DatabaseLink(DataElementPath.create(projectCollectionName).getChildPath(entry.getKey()).toString());
                Properties props = entry.getValue();
                databaseLink.setClientName(entry.getKey());
                databaseLink.setAvailability("public");
                databaseLink.setAccessType(props.getProperty("importType"));
                result.add(databaseLink);
            }
        }
        loadButton.setEnabled(true);
        return result;

    }

    @Override
    protected List<String> getServers()
    {
        return Arrays.asList(ServerRegistry.getServerHosts(username.getText(), new String(password.getPassword())));
    }

    @Override
    protected int loadModule(String sModuleName, String cModuleName, FunctionJobControl jc)
    {
        String researchCollectionName = DataElementPath.create(sModuleName).getPathComponents()[0];
        int installedCount = 0;
        try
        {
            DataCollection<?> userProjectsParent = DataElementPath.create( "data" ).getChildPath( researchCollectionName )
                    .getDataCollection();
            if( !userProjectsParent.contains(cModuleName) )
            {
                AccessClient connection = getConnection(getSelectedUrl());
                File importFile = connection.importDataCollection(ROOT_DC_NAME + sModuleName, cModuleName);

                try( JarFile moduleFile = new JarFile( importFile ) )
                {
                    ModulePackager.importModule( null, moduleFile, (Repository)userProjectsParent, null, jc );
                }
                importFile.delete();
                if( userProjectsParent.get(cModuleName) != null )
                    installedCount++;
            }
            else
            {
                log.warning("Research with the same name already exists ('" + cModuleName + "')");
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not import research: " + e.getMessage());
        }

        ( (BioUMLApplication)Application.getApplicationFrame() ).updateJournalBox(JournalRegistry.getJournalNames());
        return installedCount;
    }


    @Override
    protected String getDBInfo(DatabaseLink link)
    {
        if( link != null )
        {
            try
            {
                String dbInfo = getConnection(getSelectedUrl()).getDescription(DataElementPath.create(ROOT_DC_NAME + link.getServerName()));
                return dbInfo;
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not load description for database: " + link.getServerName(), e);
            }
        }
        return null;
    }

    @Override
    protected void registerServer(String url)
    {
        ServerRegistry.addServer(url);
    }

    @Override
    protected String getSelectedUrl()
    {
        Object obj = serverURL.getSelectedItem();
        if( obj == null )
            return null;
        return ServerRegistry.getServerURL(obj.toString());
    }
}
