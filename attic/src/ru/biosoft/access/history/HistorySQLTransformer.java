package ru.biosoft.access.history;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;

/**
 * Transformer for {@link SQLHistoryDataCollection}
 * 
 CREATE TABLE `history` (
  `id` varchar(255) NOT NULL,
  `path` varchar(255) default NULL,
  `timestamp` datetime default NULL,
  `version` integer default NULL,
  `author` varchar(255) default NULL,
  `comment` text default NULL,
  `type` enum('changes', 'object'),
  `data` text default NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
 */
public class HistorySQLTransformer extends SqlTransformerSupport<HistoryElement>
{
    public static final String JDBC_TABLE_PROPERTY = "jdbcTable";

    public static final String JDBC_ID_COLUMN = "id";
    public static final String JDBC_PATH_COLUMN = "path";
    public static final String JDBC_TIMESTAMP_COLUMN = "timestamp";
    public static final String JDBC_VERSION_COLUMN = "version";
    public static final String JDBC_AUTHOR_COLUMN = "author";
    public static final String JDBC_COMMENT_COLUMN = "comment";
    public static final String JDBC_TYPE_COLUMN = "type";
    public static final String JDBC_DATA_COLUMN = "data";

    @Override
    public boolean init(SqlDataCollection<HistoryElement> owner)
    {
        table = owner.getInfo().getProperty(JDBC_TABLE_PROPERTY);
        return true;
    }

    @Override
    public String getTable()
    {
        return table;
    }

    @Override
    public Class getTemplateClass()
    {
        return HistoryElement.class;
    }

    @Override
    public HistoryElement create(ResultSet resultSet, Connection connection) throws Exception
    {
        HistoryElement element = new HistoryElement(owner, resultSet.getString(1));
        element.setDePath(DataElementPath.create(resultSet.getString(2)));
        element.setType(HistoryElement.Type.fromString(resultSet.getString(7)));
        element.setTimestamp(new Date(resultSet.getTimestamp(3).getTime()));
        element.setVersion(resultSet.getInt(4));
        element.setAuthor(resultSet.getString(5));
        element.setComment(resultSet.getString(6));
        element.setData(resultSet.getString(8));

        return element;
    }

    @Override
    public void addInsertCommands(Statement statement, HistoryElement element) throws Exception
    {
        String date = validateValue(new Timestamp(element.getTimestamp().getTime()).toString());
        String author = processNull(validateValue(element.getAuthor()));
        String type = validateValue(element.getType().toString());
        String comment = processNull(validateValue(element.getComment()));
        statement.addBatch("INSERT INTO " + table + " (" + JDBC_ID_COLUMN + ", " + JDBC_PATH_COLUMN + ", " + JDBC_TIMESTAMP_COLUMN + ", "
                + JDBC_VERSION_COLUMN + ", " + JDBC_AUTHOR_COLUMN + ", " + JDBC_COMMENT_COLUMN + ", " + JDBC_TYPE_COLUMN + ", "
                + JDBC_DATA_COLUMN + ") VALUES(" + validateValue(element.getName()) + ", " + validateValue(element.getDePath().toString())
                + ", " + date + ", " + element.getVersion() + ", " + author + ", " + comment + ", " + type + ", "
                + processNull(validateValue(element.getData())) + ")");
    }

    protected String processNull(Object value)
    {
        if( value == null )
            return "NULL";
        return value.toString();
    }
}
