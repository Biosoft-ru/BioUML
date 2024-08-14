package biouml.plugins.keynodes.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.Properties;
import java.util.function.Function;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.Cache;

/**
 * SQL-based KeyNodes hub, manage SQL connections, checks for hub availability etc.
 *
 */
public abstract class KeyNodesSqlHub<N> extends KeyNodesHub<N>
{
    protected Logger log = Logger.getLogger( KeyNodesSqlHub.class.getName() );
    protected String sqlTest = null;
    protected String sqlGetTitle = null;

    protected boolean isAvailable = false;
    protected boolean isChecked = false;

    protected ThreadLocal<Connection> connection = new ThreadLocal<>();

    public KeyNodesSqlHub(Properties properties)
    {
        super( properties );
    }

    protected int getDefaultPriority()
    {
        return 10;
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        DataElementPathSet dbList = dbOptions.getUsedCollectionPaths();
        if( dbList.size() > 0 )
        {
            for( DataElementPath cr : dbList )
            {
                if( !cr.isDescendantOf( KEY_NODES_HUB ) && !cr.isDescendantOf( getModulePath() ) )
                {
                    return 0;
                }
            }
            return isHubAvailable() ? getDefaultPriority() : 0;
        }
        return 0;
    }

    protected boolean isHubAvailable()
    {
        if( isChecked )
            return isAvailable;

        if( sqlTest == null )
        {
            isChecked = true;
            return isAvailable;
        }

        try
        {
            Connection conn = getConnection();
            if( conn != null )
            {
                if( SqlUtil.hasResult( conn, sqlTest ) )
                {
                    isAvailable = true;
                }
            }
        }
        catch( BiosoftSQLException e )
        {
            e.log();
        }
        if( !isAvailable )
        {
            log.log(Level.SEVERE,  "Hub " + getClass().getSimpleName() + " cannot be initialized: unable to establish SQL connection" );
        }
        isChecked = true;
        return isAvailable;
    }

    protected Connection getConnection()
    {
        Connection result = connection.get();
        if( result == null )
        {
            DataElementPath modulePath = getModulePath();
            if( modulePath != null )
            {
                result = SqlConnectionPool.getConnection( modulePath.getDataCollection() );
            }
            if( result == null )
            {
                result = SqlConnectionPool.getPersistentConnection( properties );
            }
            connection.set( result );
        }
        SqlUtil.checkConnection( result );
        return result;
    }

    private final Function<String, String> titleCache = Cache.soft( acc -> {
        try (PreparedStatement ps = getConnection().prepareStatement( sqlGetTitle ))
        {
            ps.setString( 1, acc );
            try (ResultSet rs = ps.executeQuery())
            {
                if( rs.next() )
                {
                    return rs.getString( 1 );
                }
            }
        }
        catch( SQLException e )
        {
            new BiosoftSQLException( getConnection(), sqlGetTitle, e ).log();
        }
        return null;
    } );

    @Override
    public String getElementTitle(Element element)
    {
        if( sqlGetTitle == null )
            return element.getAccession();

        String title = titleCache.apply( element.getAccession() );
        if( title != null )
            return title;

        return element.getAccession();
    }
}
