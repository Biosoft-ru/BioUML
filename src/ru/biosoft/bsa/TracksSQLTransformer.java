package ru.biosoft.bsa;

import java.sql.Statement;

import ru.biosoft.access.DataElementsSqlTransformer;

public class TracksSQLTransformer extends DataElementsSqlTransformer<SqlTrack>
{

    @Override
    public void addDeleteCommands(Statement statement, String name) throws Exception
    {
        SqlTrack track = owner.get(name);
        if( track == null )
            return;
        int id = track.getId();
        statement.addBatch( "DELETE FROM data_element WHERE id = " + id );
        statement.addBatch("DROP TABLE IF EXISTS " + track.getTableId());
    }

    @Override
    public Class<SqlTrack> getTemplateClass()
    {
        return SqlTrack.class;
    }

    @Override
    public String[] getUsedTables()
    {
        return new String[] {"data_element", "de_info"};
    }
}
