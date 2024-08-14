package biouml.standard.simulation.plot.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.List;

import java.util.logging.Logger;

import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.graphics.Pen;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;

public class PlotSqlTransformer extends SqlTransformerSupport<Plot>
{
    static Logger log = Logger.getLogger(PlotSqlTransformer.class.getName());

    public PlotSqlTransformer()
    {
        table = "plots";
    }

    @Override
    public Class<Plot> getTemplateClass()
    {
        return Plot.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT ID, title, description, x_title, x_from, x_to, y_title, y_from, y_to FROM " + table;
    }

    @Override
    public Plot create(ResultSet resultSet, Connection connection) throws Exception
    {
        Plot pl = new Plot(owner, resultSet.getString(1));
        try {
            pl.setTitle(resultSet.getString(2));
            pl.setDescription(resultSet.getString(3));
            pl.setXTitle(resultSet.getString(4));
            pl.setXFrom(resultSet.getDouble(5));
            pl.setXTo(resultSet.getDouble(6));
            pl.setYTitle(resultSet.getString(7));
            pl.setYFrom(resultSet.getDouble(8));
            pl.setYTo(resultSet.getDouble(9));
        }
        catch (Exception ex) {
            log.log(Level.SEVERE, "An error occured when initializing simulation result: " + ex);
            return pl;
        }

        Statement statement;
        ResultSet seriesRS;
        try {
            statement = connection.createStatement();
            seriesRS = statement.executeQuery("SELECT " +
                                              "spec, legend, x_var, y_var, simulationResultId, sourceNature FROM plot_series " +
                                              "WHERE plotId = '" + pl.getName() + "'"
                                              );
        }
        catch (Exception ex) {
            log.log(Level.SEVERE,  "Error has occurred when preparing query for \"plot_series\" table: " + ex );
            return pl;
        }

        try
        {
            while (seriesRS.next())
            {
                try
                {
                    Series s = new Series();
                    s.setPlotName(pl.getName());
                    s.setSpec(Pen.createInstance(seriesRS.getString(1)));
                    s.setLegend(seriesRS.getString(2));
                    s.setXVar(seriesRS.getString(3));
                    s.setYVar(seriesRS.getString(4));
                    s.setSource(seriesRS.getString(5));
                    s.setSourceNature(Series.SourceNature.valueOf(seriesRS.getString(6)));

                    pl.addSeries(s);
                }
                catch (Exception ex) {
                    log.log(Level.SEVERE, "Error has occured when retrieving tuple from \"plot_series\" table: " + ex);

                }
            }
        }
        catch (Exception ex) {
            log.log(Level.SEVERE, "Error has occured when retrieving data from \"plot_series\" table: " + ex);
        }
        return pl;
    }

    @Override
    public void addInsertCommands(Statement statement, Plot pl) throws SQLException
    {
        try {
            statement.addBatch("INSERT INTO " + table +
                               "(id, title, description, x_title, x_from, x_to, y_title, y_from, y_to) VALUES (" +
                               validateValue(pl.getName()) + ", " +
                               validateValue(pl.getTitle()) + ", " +
                               validateValue(pl.getDescription()) + ", " +
                               validateValue(pl.getXTitle()) + ", " +
                               "" + pl.getXFrom() + ", " +
                               "" + pl.getXTo() + ", " +
                               validateValue(pl.getYTitle()) + ", " +
                               "" + pl.getYFrom() + ", " +
                               "" + pl.getYTo() + ")"
                               );
        }
        catch (Exception ex) {
            log.log(Level.SEVERE, "Error occured when adding query for insertion into " + table + " table: " + ex);
            return;
        }

        List<Series> series = pl.getSeries();
        if (series != null) {
            for(Series s: series)
            {
                try
                {
                    statement.addBatch("INSERT INTO plot_series (" + "id, plotId, spec, legend, x_var, y_var, simulationResultId, sourceNature) "
                            + "VALUES (" + validateValue(pl.getName() + "_" + s.getXVar() + "_" + s.getYVar()) + ", "
                            + validateValue(pl.getName()) + ", " + validateValue(s.getSpec().toString()) + ", " + validateValue(s.getLegend()) + ", "
                            + validateValue(s.getXVar()) + ", " + validateValue(s.getYVar()) + ", " + validateValue(s.getSource()) + ", "
                            + validateValue(s.getSourceNature().toString()) + ")" + ")");
                }
                catch( Exception ex )
                {
                    log.log(Level.SEVERE, "Error occured when adding insertion query for \"plot_series\" table: " + ex);
                }
            }
        }
    }

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        statement.addBatch("DELETE FROM " + table + " WHERE id = '" + name + "'");
        statement.addBatch("DELETE FROM plot_series WHERE plotId = '" + name + "'");
    }
    
    @Override
    public String[] getUsedTables()
    {
        return new String[] {"plot_series", table};
    }
    
    @Override
    public String getCreateTableQuery(String tableName)
    {
        if( tableName.equals(table) )
        {
            return "CREATE TABLE `plots` (" +
                    "  `id` varchar(20) NOT NULL default ''," +
                    "  `title` varchar(20) default NULL," +
                    "  `description` text," +
                    "  `x_title` varchar(20) default NULL," +
                    "  `x_from` double default NULL," +
                    "  `x_to` double default NULL," +
                    "  `y_title` varchar(20) default NULL," +
                    "  `y_from` double default NULL," +
                    "  `y_to` double default NULL," +
                    "  PRIMARY KEY  (`id`)" +
                    ") ENGINE=MyISAM";
        }
        else if( tableName.equals("plot_series") )
        {
            return "CREATE TABLE `plot_series` (" +
                    "  `id` bigint(20) NOT NULL auto_increment," +
                    "  `plotId` varchar(20) default NULL," +
                    "  `spec` varchar(20) default NULL," +
                    "  `legend` text," +
                    "  `x_var` varchar(20) default NULL," +
                    "  `y_var` varchar(20) default NULL," +
                    "  `simulationResultId` varchar(200) default NULL," +
                    "  `sourceNature` varchar(20) default NULL, " +
                    "  PRIMARY KEY  (`id`)" +
                    ") ENGINE=MyISAM";
        }
        return super.getCreateTableQuery(tableName);
    }
}
