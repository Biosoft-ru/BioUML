package biouml.plugins.gtrd.access;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import biouml.plugins.machinelearning.utils.DataMatrixString;

import ru.biosoft.access.sql.Connectors;

public class GTRDStatsAccess {

	//static String DB_CONNECTION_STRING = "jdbc:mysql://micro:3306/gtrd_v2005_stats?user=gtrd&password=gtrd";
	static String GTRD_STATS_DB = "gtrd_v2005_stats";      
	static Connection con;
	
	public GTRDStatsAccess() 
	{}
	
	private static Connection getSQLConnection(String connectionString) throws SQLException, ClassNotFoundException
	{
		Class.forName( "com.mysql.jdbc.Driver" );
		Connection con = DriverManager.getConnection( connectionString );
		return con;
	}
	
	/** 
	 * @param tableName - name of the table
	 * @param table - table
	 * @param columnsDescription contains description of tables' columns (new String[] { columnName, columnType, "NOT NULL" or "" }) 
	 * @param primaryKeyCol - name of the column containing primaryKey for the table
	 * */
	public static void writeNewTable( String tableName, List<String[]> table, List<String[]> columnsDescription, String primaryKeyCol ) throws ClassNotFoundException, SQLException
	{
		//Connection con = getSQLConnection( DB_CONNECTION_STRING );
                Connection con = Connectors.getConnection( GTRD_STATS_DB );
		Statement st = con.createStatement();

		if( doesTableExist( tableName ) )
			return;
		
		String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (";
		for( int i = 0; i < columnsDescription.size(); i++ )
		{
			sql += String.join( " ", columnsDescription.get( i ) );
			sql += ", ";
		}
		sql += " PRIMARY KEY (" + primaryKeyCol + "));";
		st.execute( sql );

		for(String[] row : table)
		{
			LinkedList<String> columnNames = new LinkedList<>();
			for( String[] columnDescription : columnsDescription )
				columnNames.add( columnDescription[0] );
			LinkedList<String> values = new LinkedList<>();
			for(String value : row)
				values.add( "'" + value + "'" );
			sql = "INSERT INTO " + tableName + " (" + String.join(", ", columnNames) + ") VALUES( " + String.join(",", values) + ");";
			try( PreparedStatement ps = con.prepareStatement( sql ) )
			{
				ps.executeUpdate();
			}
			catch(Exception e)
			{
				con.close();
				//con = getSQLConnection( DB_CONNECTION_STRING );
                                con = Connectors.getConnection( GTRD_STATS_DB );
				con.prepareStatement( sql ).executeUpdate();
			}
		}
	}

	public static void addToExistedTable( String tableName, List<String[]> table ) throws ClassNotFoundException, SQLException
	{
		if( !doesTableExist( tableName ) )
			throw new NullPointerException( "NO " + tableName + " TABLE!" );
		//Connection con = getSQLConnection( DB_CONNECTION_STRING );
                Connection con = Connectors.getConnection( GTRD_STATS_DB );
		LinkedList<String[]> columnsDescription = getTableColumnsDescription( tableName );
		String primaryKey;
		List<String> primaryKeyValues = new ArrayList<>();
		ResultSet rsPrimaryKeys = con.getMetaData().getPrimaryKeys( null, null, tableName );
		if( rsPrimaryKeys.next() )
			primaryKey = rsPrimaryKeys.getString( "COLUMN_NAME" );
		else
			throw new NullPointerException( "PRIMARY KEY DOESN'T EXIST" );
		ResultSet rsPrimaryKeyValues = con.createStatement().executeQuery( "SELECT " + primaryKey + " FROM " + tableName + ";" );
		while( rsPrimaryKeyValues.next() )
			primaryKeyValues.add( rsPrimaryKeys.getString( 1 ) );
		int primaryKeyIndex = 0;
		String[] columnNames = new String[columnsDescription.size()];
		for( int i = 0; i < columnsDescription.size(); i++ )
		{
			columnNames[i] = columnsDescription.get( i )[0];
			if( columnsDescription.get( i )[0].equals( primaryKey ) )
				primaryKeyIndex = i;
		}
		List<String> sqls = new ArrayList<>();
		for(String[] row : table)
		{
			if( primaryKeyValues.contains( row[primaryKeyIndex] ) )
				throw new DuplicatedIdException();
			LinkedList<String> values = new LinkedList<>();
			for(String value : row)
				values.add( "'" + value + "'" );
			String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columnNames) + ") VALUES( " + String.join(",", values) + ");";
			sqls.add( sql );
		}
		for(String query : sqls)
			try( PreparedStatement ps = con.prepareStatement( query ) )
			{
				ps.executeUpdate();
			}
	}
	public static void writeTableFromDataMatrix(DataMatrixString table, String tableName,String first_column,String[] columnType, String key, boolean writeNewMatrix, boolean useNewKey) throws ClassNotFoundException, SQLException
	{
		String[] rowNames = table.getRowNames();
		String[] columnNames = table.getColumnNames();
		List<String[]> newTable = new ArrayList<>();
		List<String[]> columnDwscrp = new ArrayList<>();
		for(int i = 0; i < rowNames.length;i++)
		{
			
			if(useNewKey)
				newTable.add(concatenate(concatenate(new String[] {i + 1 + ""}, new String[] {rowNames[i]}), table.getRow(rowNames[i])));
			else
				newTable.add(table.getRow(rowNames[i]));
		}
		if(writeNewMatrix)
		{
			columnDwscrp.add(new String[] {key, "INTEGER",""});
			columnDwscrp.add(new String[] {first_column, "VARCHAR(255)",""});
			for(int j =0; j < columnNames.length;j++)
				columnDwscrp.add(new String[] {columnNames[j], columnType[j],""});
			writeNewTable(tableName, newTable, columnDwscrp, key);
		}
		else
			addToExistedTable(tableName, newTable);
		
	}
	
	public static String[] concatenate(String[] a, String[] b) 
	{
	    int length = a.length + b.length;
	    String[] result = new String[length];
	    System.arraycopy(a, 0, result, 0, a.length);
	    System.arraycopy(b, 0, result, a.length, b.length);

	    return result;
	}

	public static class DuplicatedIdException extends RuntimeException 
	{
		public DuplicatedIdException()
		{
			super();
		}
		
		public DuplicatedIdException( String s)
		{
			super(s);
		}
	}
	public static boolean doesTableExist( String tableName ) throws SQLException, ClassNotFoundException
	{
		//con = getSQLConnection( DB_CONNECTION_STRING );
                con = Connectors.getConnection( GTRD_STATS_DB );
		DatabaseMetaData dbmd = con.getMetaData();
		ResultSet rs = dbmd.getTables(null, null, tableName, new String[] {"TABLE"});
		return rs.next();
	}
	
	public static String[] getAllTablesNames() throws ClassNotFoundException, SQLException
	{
		List<String> tableNames = new ArrayList<>();
		//con = getSQLConnection( DB_CONNECTION_STRING );
                con = Connectors.getConnection( GTRD_STATS_DB );
		DatabaseMetaData dbmd = con.getMetaData();
		ResultSet rs = dbmd.getTables(null, null, "%", new String[] {"TABLE"});
		while( rs.next() )
			tableNames.add( rs.getString( "TABLE_NAME" ) );
		return tableNames.toArray( new String[tableNames.size()] );
	}

	public static LinkedList<String[]> getTableColumnsDescription( String tableName ) throws ClassNotFoundException, SQLException
	{
		if( !doesTableExist( tableName ) )
			return null;
		LinkedList<String[]> columnsDescription = new LinkedList<>();
		//con = getSQLConnection( DB_CONNECTION_STRING );
                con = Connectors.getConnection( GTRD_STATS_DB );
		DatabaseMetaData dbmd = con.getMetaData();
		ResultSet columnInfo = dbmd.getColumns(null, null, tableName, null);
		while( columnInfo.next() )
		{
			String[] description = new String[3];
			description[0] = columnInfo.getString( "COLUMN_NAME" );
			description[1] = columnInfo.getString( "DATA_TYPE" );
			description[2] = columnInfo.getString( "IS_NULLABLE" ).equals( "YES" ) ? "" : "NOT NULL";
			columnsDescription.add( description );
		}
		return columnsDescription;
	}

	public static void removeTable( String tableName ) throws SQLException, ClassNotFoundException
	{
		//String connectionString = "jdbc:mysql://localhost:3306/gtrd_v1903_stats?user=gtrd&password=gtrd";
		//Connection con = getSQLConnection( connectionString );
                Connection con = Connectors.getConnection( "gtrd_v1903_stats" );
		Statement st = con.createStatement();
		if( doesTableExist( tableName ) )
		{
			String sql = "DROP TABLE " + tableName + " ;";
			st.execute( sql );
		}
	}

}
