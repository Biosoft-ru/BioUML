package ru.biosoft.table;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.access.repository.JSONSerializable;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.exception.TableAddColumnException;
import ru.biosoft.table.exception.TableColumnExistsException;
import ru.biosoft.table.exception.TableModifyColumnException;
import ru.biosoft.table.exception.TableRemoveColumnException;

public class SqlColumnModel extends ColumnModel implements JSONSerializable
{
    public SqlColumnModel(SqlTableDataCollection origin)
    {
        super(origin);
    }
    
    protected SqlTableDataCollection getOrigin()
    {
        return (SqlTableDataCollection)origin;
    }

    /**
     * Add new column.
     * @param dp column info object.
     * @return
     */
    @Override
    public TableColumn addColumn(TableColumn dp)
    {
        try
        {
            if( hasColumn(dp.getName()) )
            {
                throw new TableColumnExistsException(getOrigin(), dp.getName());
            }
            TableColumn[] newColumnInfo = new TableColumn[columnsInfo.length + 1];
            System.arraycopy(columnsInfo, 0, newColumnInfo, 0, columnsInfo.length);
            newColumnInfo[columnsInfo.length] = dp;
            columnsInfo = newColumnInfo;

            getOrigin().addColumnToDb(dp);

            rebuildColumnName2Index();
            return dp;
        }
        catch( Exception e )
        {
            throw new TableAddColumnException(e, getOrigin(), dp.getName());
        }
    }

    @Override
    public void removeColumn(int columnPos)
    {
        try
        {
            getOrigin().removeColumnFromDb(columnPos);
            TableColumn[] newColumnInfo = new TableColumn[columnsInfo.length - 1];
            System.arraycopy(columnsInfo, 0, newColumnInfo, 0, columnPos);
            System.arraycopy(columnsInfo, columnPos + 1, newColumnInfo, columnPos, columnsInfo.length - columnPos - 1);
            columnsInfo = newColumnInfo;

            rebuildColumnName2Index();
        }
        catch( Exception e )
        {
            throw new TableRemoveColumnException(e, getOrigin(), "#"+columnPos);
        }
    }

    @Override
    public void renameColumn(int columnPos, String newName)
    {
        try
        {
            TableColumn col = getColumn(columnPos);
            String oldName = col.getName();
            String oldType = col.getType().name();
            super.renameColumn(columnPos, newName);
            col = getColumn( columnPos );
            getOrigin().changeDbColumn( oldName, oldType, col );
        }
        catch(Exception e)
        {
            throw new TableModifyColumnException(e, getOrigin(), "#"+columnPos);
        }
    }

    @Override
    public void fromJSON(JSONObject input) throws JSONException
    {
        JSONArray jsonColumns = input.optJSONArray("columns");
        columnsInfo = new TableColumn[jsonColumns.length()];
        for(int i=0; i<jsonColumns.length(); i++)
        {
            JSONObject jsonColumn = jsonColumns.getJSONObject(i);
            columnsInfo[i] = new TableColumnProxy(
                    jsonColumn.getString("name"),
                    jsonColumn.has("displayName")?jsonColumn.getString("displayName"):jsonColumn.getString("name"),
                    jsonColumn.has("description")?jsonColumn.getString("description"):jsonColumn.getString("name"),
                    DataType.fromString(jsonColumn.getString("type")),
                    jsonColumn.optString("expression")
            );
            if(jsonColumn.optBoolean("hide"))
                columnsInfo[i].setHidden(true);
            JSONObject properties = jsonColumn.optJSONObject("properties");
            if(properties != null)
            {
                Iterator<String> iterator = properties.keys();
                while(iterator.hasNext())
                {
                    String key = iterator.next();
                    columnsInfo[i].setValue(key, properties.optString(key));
                }
            }
        }
        rebuildColumnName2Index();
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject jsonResult = new JSONObject();
        JSONArray jsonColumns = new JSONArray();
        for(TableColumn column: columnsInfo)
        {
            JSONObject jsonColumn = new JSONObject();
            jsonColumn.put("name", column.getName());
            if(!column.getName().equals(column.getDisplayName()))
                jsonColumn.put("displayName", column.getDisplayName());
            if(!column.getName().equals(column.getShortDescription()))
                jsonColumn.put("description", column.getShortDescription());
            jsonColumn.put("type", column.getType().name());
            if(column.getExpression()!=null && !column.getExpression().isEmpty())
                jsonColumn.put("expression", column.getExpression());
            if(column.isHidden())
                jsonColumn.put("hide", true);
            JSONObject properties = new JSONObject();
            for(String key: column.getKeys())
            {
                properties.put(key, column.getValue(key));
            }
            if(properties.length() > 0)
            {
                jsonColumn.put("properties", properties);
            }
            jsonColumns.put(jsonColumn);
        }
        jsonResult.put("columns", jsonColumns);
        return jsonResult;
    }
}
