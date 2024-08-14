package biouml.standard.simulation.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.util.TextUtil;
import biouml.model.dynamics.Variable;
import biouml.standard.simulation.SimulationResult;

public class SimulationResultSqlTransformer extends SqlTransformerSupport<SimulationResult>
{
    static Logger log = Logger.getLogger(SimulationResultSqlTransformer.class.getName());

    public SimulationResultSqlTransformer()
    {
        table = "simulations";
    }

    @Override
    public Class<SimulationResult> getTemplateClass()
    {
        return SimulationResult.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT ID, diagramID, title, description, initialTime, completionTime FROM " + table;
    }

    @Override
    public SimulationResult create(ResultSet resultSet, Connection connection) throws Exception
    {
        SimulationResult sr = new SimulationResult(owner, resultSet.getString(1));
        try
        {
            sr.setDiagramName(resultSet.getString(2));
            sr.setTitle(resultSet.getString(3));
            sr.setDescription(resultSet.getString(4));
            sr.setInitialTime(resultSet.getDouble(5));
            sr.setCompletionTime(resultSet.getDouble(6));
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "An error occured when initializing simulation result: " + ex);
            return sr;
        }

        // extract values from "initial_values" table
        Statement statement;
        ResultSet initialValuesRS;
        try
        {
            statement = connection.createStatement();
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "An error occured when creating statement: " + ex);
            return sr;
        }

        try
        {
            initialValuesRS = statement.executeQuery("SELECT name, value, units FROM initial_values WHERE resultID = '" + sr.getName()
                    + "'");

            while( initialValuesRS.next() )
            {
                Variable var = new Variable(initialValuesRS.getString(1), null, null);
                var.setInitialValue(initialValuesRS.getDouble(2));
                var.setUnits(initialValuesRS.getString(3));
                sr.addInitialValue(var);

            }
        }
        catch( SQLException sqlex )
        {
            log.log(Level.SEVERE, "An error occured when exctracting initial values: " + sqlex);
        }

        // retrieve values from result_<ID> table
        // count the number of time slices
        try
        {
            int timeSliceNumber = SqlUtil.getRowsCount(connection, "result_" + sr.getName());

            ResultSet resultValues = statement.executeQuery("SELECT * FROM " + "result_" + sr.getName() + " ORDER BY time");
            ResultSetMetaData metaData = resultValues.getMetaData();
            int varCount = metaData.getColumnCount() - 1;

            double[] times = new double[timeSliceNumber];
            double[][] values = new double[timeSliceNumber][varCount];

            HashMap<String, Integer> variableMap = new HashMap<>();

            int timeValueIndex = 0;

            // fill in variable map    firstly and mark index of time attribute
            int varIndex = 0;
            for( int i = 1; i <= metaData.getColumnCount(); i++ )
            {
                if( metaData.getColumnName(i).equals("time") )
                    timeValueIndex = i;
                variableMap.put(metaData.getColumnName(i), varIndex++);
            }

            int counter = 0;
            while( resultValues.next() )
            {
                varIndex = 0;
                for( int i = 1; i <= metaData.getColumnCount(); i++ )
                {
                    if( i == timeValueIndex )
                        times[counter] = resultValues.getDouble(i);
                    else
                        values[counter][varIndex++] = resultValues.getDouble(i);
                }
                counter++;
            }

            sr.setTimes(times);
            sr.setValues(values);
            sr.setVariableMap(variableMap);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "An error occured when exctracting values from " + "result_" + sr.getName() + ": " + ex);
        }

        return sr;
    }

    @Override
    public void addInsertCommands(Statement statement, SimulationResult sr) throws SQLException
    {
        try
        {
            DataElementPath diagramId = sr.getDiagramPath();
            if( diagramId == null )
                diagramId = DataElementPath.EMPTY_PATH;
            String str = "INSERT INTO " + table + " (ID, diagramID, title, description, initialTime, completionTime) VALUES("
                    + validateValue(sr.getName()) + ", " + validateValue(diagramId.toString()) + ", " + validateValue(sr.getTitle()) + ", "
                    + validateValue(sr.getDescription()) + ", " + sr.getInitialTime() + ", " + sr.getCompletionTime() + ")";

            statement.addBatch(str);
            //System.out.println("str = " + str);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "An error occured when adding insertion into " + table + " query: " + ex);
            return;
        }

        // insert to "initial_values"
        ArrayList<Variable> variables = sr.getInitialValues();

        if( variables != null )
        {
            for(Variable var : variables)
            {
                try
                {
                    String units = TextUtil.nullToEmpty( var.getUnits() );
                    String str = "INSERT INTO initial_values (resultID, name, value, units) VALUES(" + validateValue(sr.getName()) + ", "
                            + validateValue(var.getName()) + ", " + validateValue("" + var.getInitialValue()) + ", " + validateValue(units)
                            + ")";

                    statement.addBatch(str);
                    //System.out.println("str = " + str);
                }
                catch( Exception ex )
                {
                    log.log(Level.SEVERE, "An error occured when adding insertion into \"intial_values\" table query: " + ex);
                }
            }
        }

        // insert result values if exist
        Map<String, Integer> variableMap = sr.getVariableMap();
        double[] times = sr.getTimes();
        double[][] values = sr.getValues();

        if( variableMap != null && times != null && values != null )
        {
            String tableName = "result_" + sr.getName();
            try
            {
                statement.addBatch("DROP TABLE IF EXISTS " + tableName);
                StringBuffer query = new StringBuffer("CREATE TABLE " + tableName + "( \n" + "time DOUBLE NOT NULL, \n");

                int varCount = values[0].length;

                // prepare temporary arrays to speed up
                // further values and names fetching
                int[] indeces = new int[varCount];
                String[] names = new String[varCount];
                int counter = 0;
                for(Entry<String, Integer> entry : variableMap.entrySet())
                {
                    String name = entry.getKey();
                    indeces[counter] = entry.getValue();
                    if( !name.equals("time") )
                    {
                        names[counter] = name;
                        counter++;
                    }
                    /*else
                    {
                        varCount--;
                    }*/
                }

                // fill in attribute names
                for( int i = 0; i < varCount - 1; i++ )
                    query.append("`" + names[i] + "` DOUBLE NOT NULL,\n");
                query.append("`" + names[varCount - 1] + "` DOUBLE NOT NULL)\n");

                statement.addBatch(query.toString());

                // now fill this table with values
                int timeSliceNumber = times.length;
                for( int i = 0; i < timeSliceNumber; i++ )
                {
                    query = new StringBuffer("INSERT INTO " + tableName + " (time");
                    for( int j = 0; j < varCount; j++ )
                        query.append(", `" + names[j] + "`");
                    query.append(") ");

                    query.append("VALUES (" + times[i]);
                    for( int j = 0; j < varCount; j++ )
                        query.append(", " + values[i][indeces[j]]);
                    query.append(")\n");
                    statement.addBatch(query.toString());
                }
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "An error occured when adding insertion into " + tableName + " query: " + ex);
            }
        }
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        System.out.println("name = " + name);
        try
        {
            statement.addBatch("DELETE FROM " + table + " WHERE id = '" + name + "'");
            statement.addBatch("DELETE FROM initial_values WHERE resultID = '" + name + "'");
            statement.addBatch("DROP TABLE IF EXISTS " + "result_" + name);
        }
        catch( SQLException sqlex )
        {
            log.log(Level.SEVERE, "An error occured when adding deletion queries: " + sqlex);
        }
    }

    @Override
    public String[] getUsedTables()
    {
        return new String[] {"initial_values", table};
    }

    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `simulations` (" + "  `ID` varchar(20) NOT NULL default '',"
                    + "  `diagramID` varchar(20) NOT NULL default ''," + "  `title` varchar(100) default NULL," + "  `description` text,"
                    + "  `initialTime` double NOT NULL default '0'," + "  `completionTime` double NOT NULL default '0',"
                    + "  PRIMARY KEY  (`ID`)" + ") ENGINE=MyISAM";
        }
        else if( tableName.equals("initial_values") )
        {
            return "CREATE TABLE `initial_values` (" + "  `resultID` varchar(20) NOT NULL default '',"
                    + "  `name` varchar(20) NOT NULL default ''," + "  `title` varchar(100) default NULL,"
                    + "  `value` double NOT NULL default '0'," + "  `units` varchar(20) NOT NULL default '',"
                    + "  `type` varchar(10) NOT NULL default ''" + ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
