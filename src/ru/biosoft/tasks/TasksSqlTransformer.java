package ru.biosoft.tasks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.GlobalDatabaseManager;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.LazyDynamicPropertySet;
import ru.biosoft.util.TextUtil2;

import ru.biosoft.jobcontrol.AbstractJobControl;
import ru.biosoft.jobcontrol.JobControl;

import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * @author lan
 *
 */
public class TasksSqlTransformer extends SqlTransformerSupport<TaskInfo>
{
    /**
     * Property for SqlDataCollection
     * If true, then only tasks for current user are shown
     */
    public static final String USER_MODE_PROPERTY = "userMode";
    private static final Logger log = Logger.getLogger(TasksSqlTransformer.class.getName());
    private boolean userMode = false;

    private static boolean hasIsHidden = false;

    @Override
    public Class<TaskInfo> getTemplateClass()
    {
        return TaskInfo.class;
    }

    @Override
    public TaskInfo create(ResultSet resultSet, Connection connection) throws Exception
    {
        String name = resultSet.getString( 1 );
        long startTime = resultSet.getTimestamp( 2 ).getTime();
        long endTime = resultSet.getTimestamp( 3 ) == null ? 0 : resultSet.getTimestamp( 3 ).getTime();
        String type = resultSet.getString( 4 );
        String source = resultSet.getString( 5 );
        int status = resultSet.getInt( 6 );
        String user = resultSet.getString( 7 );
        String message = resultSet.getString( 8 );
        String properties = resultSet.getString( 9 );

        boolean interrupted = status == JobControl.RUNNING || status == JobControl.CREATED || status == JobControl.PAUSED;
        boolean paused = status == JobControl.PAUSED;
        if( status == JobControl.CREATED || status == JobControl.RUNNING || status == JobControl.PAUSED )
        {
            status = JobControl.TERMINATED_BY_ERROR;
            message+="\nERROR : Terminated unexpectedly due to server shutdown\n";
        }

        TaskInfo taskInfo = new TaskInfo(owner, name, type, DataElementPath.create(source), new StubJobControl(status, startTime, endTime
                - startTime, endTime, 0, startTime, AbstractJobControl.getTextStatus(status), 100), null);
        taskInfo.setStartTime( startTime );
        taskInfo.setEndTime( endTime );
        taskInfo.setUser( user );
        taskInfo.setLogInfo( message );
        taskInfo.setAttributes( new LazyDynamicPropertySet( properties ) );
        taskInfo.setTransient( "interrupted", interrupted );
        taskInfo.setTransient( "paused", paused );

        return taskInfo;
    }

    @Override
    public void addInsertCommands(Statement statement, TaskInfo ti) throws Exception
    {
        if(userMode) throw new UnsupportedOperationException();
        statement.addBatch("INSERT INTO tasks(name, start, type, source, status, user, properties) values("
                + validateValue(ti.getName()) + ","
                + validateValue(new Timestamp(ti.getStartTime()).toString()) + ","
                + validateValue(ti.getType()) + ","
                + validateValue(ti.getSource() == null ? ti.getData() : ti.getSource().toString()) + ","
                + ( ti.getJobControl() == null ? -1 : ti.getJobControl().getStatus() ) + ","
                + validateValue(ti.getUser()) + ","
                + validateValue(TextUtil2.writeDPSToJSON(ti.getAttributes())) + ")");
    }

    @Override
    public void addUpdateCommands(Statement statement, TaskInfo ti) throws Exception
    {
        if(userMode) throw new UnsupportedOperationException();
        String message = ti.getLogInfo();
        String user = ti.getUser();
        long endTime = ti.getEndTime();
        statement.addBatch("UPDATE tasks SET status=" + ( ti.getJobControl() == null ? -1 : ti.getJobControl().getStatus() )
                + ( endTime > 0 ? ",end=" + validateValue(new Timestamp(endTime).toString()) : "" )
                + ( user != null ? ",user=" + validateValue(user) : "" )
                + ( message != null ? ",message=" + validateValue( message ) : "" ) + ",properties="
                + validateValue( TextUtil2.writeDPSToJSON( ti.getAttributes() ) ) + " WHERE name=" + validateValue( ti.getName() ) );
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        if(userMode) throw new UnsupportedOperationException();

        if( hasIsHidden )
        {
            String sql = "UPDATE tasks SET isHidden = 'true' WHERE " + idField + "=" + validateValue(name);  
            statement.addBatch( sql );
            log.info( "Hiding task via: " + sql );
            return;
        }   

        super.addDeleteCommands(statement, name);
    }

    @Override
    public boolean init(SqlDataCollection<TaskInfo> owner)
    {
        super.init(owner);
        table = "tasks";
        idField = "name";
        userMode = Boolean.valueOf(owner.getInfo().getProperty(USER_MODE_PROPERTY));

        String csql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS ";
        csql += "WHERE TABLE_SCHEMA = 'bioumlsupport2'  AND TABLE_NAME = 'tasks'  AND COLUMN_NAME = 'isHidden'";

        Connection rootConnection = GlobalDatabaseManager.getDatabaseConnection();
        try
        {
            hasIsHidden = SqlUtil.queryString( rootConnection, csql ) != null;
            if( !hasIsHidden )
            {
                SqlUtil.execute( rootConnection,
                        "ALTER TABLE bioumlsupport2.tasks ADD COLUMN isHidden ENUM( 'false', 'true' ) NOT NULL default 'false'" );
                hasIsHidden = true;
                log.info( "'isHidden' column for the table 'tasks' successfully created" );

            }
        }
        catch( Exception e )
        {
            if( e.getMessage() != null && e.getMessage().indexOf( "Duplicate column name" ) >= 0 )
            {
            }
            else
            {
                log.log( Level.SEVERE, "Unable to create 'isHidden' column for the table 'tasks'", e );
            }
        }

        return true;
    }

    @Override
    public String getSelectQuery()
    {
        String sql = "SELECT name, start, end, type, source, status, user, message, properties FROM tasks";
        return addCondition( sql );
    }

    protected String addCondition(String sql)
    {
        String retSql = userMode ? sql + " " + ( sql.contains("WHERE") ? "AND" : "WHERE" ) + " (user="
                + SqlUtil.quoteString(SecurityManager.getSessionUser()) + " OR user='*')" : sql;

        if( hasIsHidden )
        {
            retSql += " " + ( retSql.contains("WHERE") ? "AND" : "WHERE" ) + " isHidden = 'false'";
        }

        //log.info( "Querying tasks table via: " + retSql );
   
        return retSql;  
    }

    @Override
    public String getCountQuery()
    {
        return addCondition(super.getCountQuery());
    }

    @Override
    public String getNameListQuery()
    {
        return addCondition("SELECT " + idField + " FROM " + table);
    }

    @Override
    public String getElementExistsQuery(String name)
    {
        return addCondition(super.getElementExistsQuery(name));
    }

    @Override
    public String getElementQuery(String name)
    {
        return addCondition(super.getElementQuery(name));
    }

    @Override
    public boolean isSortingSupported()
    {
        return true;
    }

    @Override
    public String[] getSortableFields()
    {
        return SORT_MAP.keySet().toArray(new String[SORT_MAP.size()]);
    }

    private static final Map<String, String[]> SORT_MAP = new HashMap<>();

    static
    {
        SORT_MAP.put("name", new String[] {"name"});
        SORT_MAP.put("startTime", new String[] {"start"});
        SORT_MAP.put("endTime", new String[] {"end"});
        SORT_MAP.put("type", new String[] {"type", "start"});
        SORT_MAP.put("status", new String[] {"status", "start"});
        SORT_MAP.put("source", new String[] {"source", "start"});
        SORT_MAP.put("logInfo", new String[] {"message", "start"});
    }

    @Override
    public String getSortedNameListQuery(String fieldName, boolean direction)
    {
        String[] fields = SORT_MAP.get(fieldName);
        if(fields == null) return getNameListQuery();
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for(String field: fields)
            sb.append(field).append(direction?" ASC":" DESC").append(",");
        return getNameListQuery()+(sb.toString().substring(0, sb.length()-1));
    }

    /* 
        type @see ru.biosoft.tasks.TaskInfo
    */ 

    public static void logTaskRecord( String user, String name, Timestamp start, Timestamp end,          
            String type, String source, String properties, String message, boolean isHidden
            ) throws Exception
    {
        String sql = "INSERT INTO tasks(name, start, end, type, source, status, user, properties, message"
                + ( isHidden && hasIsHidden ? ",isHidden" : "" ) + ") values("
                + SqlUtil.quoteString( name ) + ","
                + SqlUtil.quoteString( start.toString() ) + ","
                + ( end != null ? SqlUtil.quoteString( end.toString() ) : "NULL" ) + ","
                + SqlUtil.quoteString( type ) + ","
                + SqlUtil.quoteString( source ) + ","
                + "3," /*COMPLETED*/
                + SqlUtil.quoteString( user ) + ","
                + SqlUtil.quoteString( properties ) + ","
                + SqlUtil.quoteString( message )
                + ( isHidden && hasIsHidden ? ", 'true'" : "" )
                + ")";

        Connection rootConnection = GlobalDatabaseManager.getDatabaseConnection();
        SqlUtil.execute( rootConnection, sql );
    }

    public static void logTaskRecord(TaskInfo ti, boolean isHidden) throws Exception
    {
        logTaskRecord( ti.getUser(), ti.getName(), new Timestamp( ti.getStartTime() ), new Timestamp( ti.getEndTime() ), ti.getType(),
                ti.getSource() == null ? ti.getData() : ti.getSource().toString(), TextUtil2.writeDPSToJSON( ti.getAttributes() ),
                ti.getLogInfo(), isHidden );
    }
}
