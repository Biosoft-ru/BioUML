package ru.biosoft.bsa.analysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SortSqlTrack extends AnalysisMethodSupport<SortSqlTrack.Parameters>
{

    public SortSqlTrack(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        SqlTrack inputTrack = parameters.getInputTrack().getDataElement( SqlTrack.class );
        sortSqlTrack( inputTrack, parameters.isRegenerateIds() );
        return new Object[0];
    }

    public static void sortSqlTrack(SqlTrack inputTrack, boolean regenerateIds) throws SQLException
    {
        Connection con = inputTrack.getConnection();

        Query query;
        String tmpTableName = createNewTableLikeOldOne( con, "sorting_sql_track_", inputTrack.getTableId() );;
        
        query = new Query("DROP INDEX chrom ON $table$").name( tmpTableName );
        SqlUtil.execute( con, query );

        if(regenerateIds)
        {
            modifyColumn( con, tmpTableName, "id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT");
            List<String> columns = getColumnNames( con, tmpTableName );
            String columnListSql = columns.stream().skip( 1 /*skip id*/ ).map( SqlUtil::quoteIdentifier ).collect( Collectors.joining( "," ) );
            query = new Query( "INSERT INTO $new$(" + columnListSql+ ") SELECT " + columnListSql + " FROM $old$ ORDER BY chrom,start,id" )
                    .name( "new", tmpTableName )
                    .name( "old", inputTrack.getTableId() );
            SqlUtil.execute( con, query );
            modifyColumn( con, tmpTableName, "id INTEGER UNSIGNED NOT NULL");
        }
        else
        {
            query = new Query( "INSERT INTO $new$ SELECT * FROM $old$ ORDER BY chrom,start,id" )
                    .name( "new", tmpTableName )
                    .name( "old", inputTrack.getTableId() );
            SqlUtil.execute( con, query );
        }
        
        query = new Query("CREATE UNIQUE INDEX chrom on $table$ (chrom,start,id)").name( tmpTableName );
        SqlUtil.execute( con, query );
        
        query = new Query( "drop table $old$" )
                .name( "old", inputTrack.getTableId() );
        SqlUtil.execute( con, query );
        
        query = new Query( "rename table $new$ to $old$" )
                .name( "new", tmpTableName )
                .name( "old", inputTrack.getTableId() );
        SqlUtil.execute( con, query );
        
        DataCollection<?> origin = inputTrack.getOrigin();
        if(origin != null)
            origin.release( inputTrack.getName() );
    }
    
    private static List<String> getColumnNames(Connection con, String table) throws SQLException
    {
        List<String> result = new ArrayList<>();
        ResultSet columns = con.getMetaData().getColumns( null, null, table, null );
        while(columns.next())
        {
            String col = columns.getString( "COLUMN_NAME" );
            result.add( col );
        }
        return result;
    }
    
    private static void modifyColumn(Connection con, String table, String colDef) throws SQLException
    {
        Query query = new Query( "ALTER TABLE $table$ MODIFY " + colDef )
                .name( "table", table );
        SqlUtil.execute( con, query );
    }
    
    private static String createNewTableLikeOldOne(Connection con, String prefix, String oldTable)
    {
        while(true) {
            try
            {
                String tmpTableName = chooseTableName( con, prefix);
                Query query = new Query( "CREATE TABLE $new$ like $old$").name( "new", tmpTableName ).name( "old", oldTable );
                SqlUtil.execute( con, query );
                return tmpTableName;
            }catch (BiosoftSQLException e){
                //table already exists exception                
            }
        }
    }
    
    private static String chooseTableName(Connection con, String prefix)
    {
        int i = 1;
        String res;
        do
        {
            res = prefix + i++;
        }while(SqlUtil.hasTable( con,  res));
        return res;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTrack;

        @PropertyName("Input SQL track")
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }

        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }
        
        private boolean regenerateIds = false;
        @PropertyName("Regenerate ids")
        public boolean isRegenerateIds()
        {
            return regenerateIds;
        }
        public void setRegenerateIds(boolean regenerateIds)
        {
            boolean oldValue = this.regenerateIds;
            this.regenerateIds = regenerateIds;
            firePropertyChange( "regenerateIds", oldValue, regenerateIds );
        }
        
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("inputTrack");
            add("regenerateIds");
        }
    }
}
