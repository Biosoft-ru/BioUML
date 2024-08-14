package biouml.plugins.server;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import biouml.model.Module;
import biouml.plugins.server.access.AccessClient;
import biouml.plugins.server.access.ClientDataCollection;
import biouml.plugins.server.access.ClientModule;
import biouml.plugins.server.access.ServerRegistry;
import biouml.workbench.module.AbstractLoadPane;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SingleSignOnSupport;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;

/**
 * Load database panel
 * 
 * @author tolstyh
 */
public class LoadDatabasesPane extends AbstractLoadPane
{
    public static final String SELECT_SERVER_URL = "(select server)";
    public static final String CONNECTION_CLASS_NAME = "ru.biosoft.server.tomcat.TomcatConnection";
    public static final String PLUGINS = "ru.biosoft.server.tomcat";

    public LoadDatabasesPane(boolean showHelpButton)
    {
        super(showHelpButton ? "load_database_dialog" : null, true, resources.getResourceString("LOAD_DATABASE_DIALOG_INFO_TEXT"));
    }

    protected static String generateHTMLList(String[] list)
    {
        StringBuffer htmlList = new StringBuffer();
        htmlList.append("<ul>");
        for( String element : list )
        {
            htmlList.append("<li>");
            htmlList.append(element);
            htmlList.append("</li>");
        }
        htmlList.append("</ul>");
        return htmlList.toString();
    }

    @Override
    protected List<String> getServers()
    {
        return Arrays.asList(ServerRegistry.getServerHosts(username.getText(), new String(password.getPassword())));
    }

    @Override
    protected void registerServer(String url)
    {
        ServerRegistry.addServer(url);
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
    protected int loadModule(String sModuleName, String cModuleName, FunctionJobControl jc)
    {
        int installedCount = 0;
        if( !databases.contains(cModuleName) )
        {
            Module newModule = null;

            String serverName = getSelectedUrl();

            Properties primary = new Properties();
            primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, cModuleName);
            primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, ClientModule.class.getName());

            primary.setProperty(ClientConnection.URL_PROPERTY, serverName);
            primary.setProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME, "databases/" + ru.biosoft.access.core.DataElementPath.escapeName(sModuleName));
//                    + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR);

            primary.setProperty(QuerySystem.QUERY_SYSTEM_CLASS, "biouml.plugins.lucene.LuceneQuerySystemClient");
            primary.setProperty(ClientConnection.CONNECTION_TYPE, "ru.biosoft.server.tomcat.TomcatConnection");
            primary.setProperty(DataCollectionConfigConstants.PRIMARY_COLLECTION, databases.getCompletePath().toString());
            primary.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, "ru.biosoft.server.tomcat");

            try
            {
                newModule = (Module)CollectionFactoryUtils.createSubDirCollection(databases, cModuleName, primary);
                if(!username.getText().isEmpty())
                {
                    newModule.getInfo().setTransientValue(ClientDataCollection.AUTH_PROPERTY,
                            new SingleSignOnSupport.ModuleProperties(cModuleName, username.getText(), new String(password.getPassword())));
                }
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not find module for server='" + serverName + "' , module='" + sModuleName + "'");
            }

            if( newModule != null )
            {
                installedCount++;
                String[] externalModules = newModule.getExternalModuleNames();
                externalModules = filterNewModuleList(externalModules);
                if( externalModules.length > 0 )
                {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "<html>"
                                    + MessageFormat.format(
                                            resources.getResourceString("LOAD_DATABASE_DIALOG_EXTERNAL_DATABASE_CONFIRM_TEXT"),
                                            new Object[] {generateHTMLList(externalModules)}),
                            resources.getResourceString("LOAD_DATABASE_DIALOG_EXTERNAL_DATABASE_CONFIRM_TITLE"),
                            JOptionPane.OK_CANCEL_OPTION);
                    if( result == JOptionPane.OK_OPTION )
                    {
                        for( String moduleName : externalModules )
                        {
                            installedCount += loadModule(moduleName, moduleName, null);
                        }
                    }
                }
            }
        }
        else
        {
            log.warning("Database with the same name already exists ('" + cModuleName + "')");
        }
        if( jc != null )
            jc.setPreparedness(100);
        return installedCount;
    }

    protected String getDatabaseAvailability(Properties properties)
    {
        String protectionStatus = properties.getProperty("protection");
        if( protectionStatus != null )
        {
            try
            {
                int availability = Integer.parseInt(protectionStatus);
                if( availability == 0 )
                {
                    return "public";
                }
                else if( availability == 1 )
                {
                    return "public, read only";
                }
                else if( availability == 2 )
                {
                    return "public read, protected write";
                }
                else if( availability == 3 )
                {
                    return "protected, read only";
                }
                else if( availability == 4 )
                {
                    return "protected, read/write";
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not get module availability", e);
            }
        }
        return "unknown";
    }

    @Override
    protected List<DatabaseLink> getDatabaseLinks(String url, String username, String password) throws Exception
    {
        if(url == null || url.equals(SELECT_SERVER_URL)) return Collections.emptyList();
        Map<String, Properties> modules = null;
        String host = url;
        AccessClient connection = getConnection(host);
        String usernameStr = username;
        String passwordStr = password;
        Permission permission = connection.login(null, usernameStr, passwordStr);
        ServerRegistry.setServerSession(host, permission.getSessionId());
        modules = connection.getFlaggedNameList(ROOT_DC_NAME);
        ServerRegistry.saveServers();

        List<DatabaseLink> result = new ArrayList<>();
        for( Entry<String, Properties> entry : modules.entrySet() )
        {
            Properties props = entry.getValue();
            String databaseAccessType = getDatabaseAccessType(props);
            if(databaseAccessType == null)
                continue;
            DatabaseLink databaseLink = new DatabaseLink(entry.getKey());
            databaseLink.setAvailability(getDatabaseAvailability(props));
            databaseLink.setAccessType(databaseAccessType);
            result.add(databaseLink);
        }
        return result;
    }

    protected String[] filterNewModuleList(String[] input)
    {
        return Stream
                .of( input )
                .filter(
                        moduleName -> !databases.contains( moduleName )
                                && databaseLinks.stream().noneMatch(
                                        dLink -> dLink.isShouldBeInstalled() && dLink.getServerName().equals( moduleName ) ) )
                .toArray( String[]::new );
    }



    protected String getDatabaseAccessType(Properties properties)
    {
        return properties.getProperty("importType");
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
    protected String getSelectedUrl()
    {
        Object obj = serverURL.getSelectedItem();
        if( obj == null )
            return null;
        return ServerRegistry.getServerURL(obj.toString());
    }
}
