package ru.biosoft.table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.table.datatype.DataType;

/**
 * ColumnModel for SqlQueryTableDataCollection
 * @author lan
 */
public class SqlQueryTableColumnModel extends ColumnModel
{
    private Map<Integer, Integer> queryColumnMap = new HashMap<>();
    
    /**
     * @param origin table
     * @param nameColumn
     * @throws SQLException
     */
    public SqlQueryTableColumnModel(SqlQueryTableDataCollection origin, int nameColumn) throws SQLException
    {
        super(origin);
        initColumns(nameColumn);
    }

    private void initColumns(int nameColumn) throws SQLException
    {
        SqlQueryTableDataCollection tdc = (SqlQueryTableDataCollection)origin;
        String query = tdc.getQuery();
        Connection connection = tdc.getConnection();
        try(PreparedStatement ps = connection.prepareStatement(query))
        {
            ResultSetMetaData metaData = ps.getMetaData();
            int count = metaData.getColumnCount();
            columnsInfo = new TableColumn[nameColumn<1 || nameColumn>count?count:count-1];
            int colNum = 0;
            for(int i=1; i<=count; i++)
            {
                if(i == nameColumn) continue;
                String name = metaData.getColumnName(i);
                if( name == null || "".equals( name ) )
                    name = "Col#" + i;
                String colLabel = metaData.getColumnLabel(i);
                if( colLabel != null && !"".equals( colLabel ) )
                {
                    name = colLabel;
                }
                name = generateUniqueColumnName(name);
                int typeInt = metaData.getColumnType(i);
                Class<?> type;
                switch( typeInt )
                {
                    case Types.BIT:
                        type = DataType.BooleanType.class;
                        break;
                    case Types.TINYINT:
                    case Types.SMALLINT:
                    case Types.INTEGER:
                        type = Integer.class;
                        break;
                    case Types.BIGINT:
                    case Types.FLOAT:
                    case Types.DOUBLE:
                    case Types.REAL:
                    case Types.NUMERIC:
                    case Types.DECIMAL:
                        type = Double.class;
                        break;
                    default:
                        type = String.class;
                }
                TableColumnProxy column = new TableColumnProxy(name, type);
                columnsInfo[colNum] = column;
                queryColumnMap.put(colNum, i);
                colNum++;
            }
            rebuildColumnName2Index();
        }
    }
    
    public int getColumnQueryIndex(int index)
    {
        return queryColumnMap.containsKey(index)?queryColumnMap.get(index):-1;
    }

    @Override
    public synchronized TableColumn addColumn(TableColumn dp)
    {
        TableColumn[] newColumnInfo = new TableColumn[columnsInfo.length + 1];
        System.arraycopy(columnsInfo, 0, newColumnInfo, 0, columnsInfo.length);
        newColumnInfo[columnsInfo.length] = dp;
        columnsInfo = newColumnInfo;
        ((SqlQueryTableDataCollection)origin).clearCache();
        rebuildColumnName2Index();
        return dp;
    }

    @Override
    public synchronized void removeColumn(int columnPos)
    {
        TableColumn[] newColumnInfo = new TableColumn[columnsInfo.length - 1];
        System.arraycopy(columnsInfo, 0, newColumnInfo, 0, columnPos);
        System.arraycopy(columnsInfo, columnPos + 1, newColumnInfo, columnPos, columnsInfo.length - columnPos - 1);
        columnsInfo = newColumnInfo;
        queryColumnMap.remove(columnPos);
        for(int i=columnPos + 1; i<columnsInfo.length; i++)
        {
            if(queryColumnMap.containsKey(i))
            {
                queryColumnMap.put(i-1, queryColumnMap.get(i));
                queryColumnMap.remove(i);
            }
        }
        rebuildColumnName2Index();
        ((SqlQueryTableDataCollection)origin).clearCache();
    }
}
