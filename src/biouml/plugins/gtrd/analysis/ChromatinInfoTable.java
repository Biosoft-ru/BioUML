package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biouml.plugins.gtrd.access.GTRDStatsAccess;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.table.SqlColumnModel;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class ChromatinInfoTable extends ArrayList<ChromatinInfoTable.ChromatinInfoTableRow> 
{
	//static String connectionString = "jdbc:mysql://localhost:3306/gtrd_v1903_stats?user=gtrd&password=gtrd";
	static Connection con;

	ChromatinInfoTable() throws ClassNotFoundException, SQLException
	{
		//con = getSQLConnection( connectionString );
                con = Connectors.getConnection( "gtrd_v1903_stats" );
	}

	public void getChromatinInfoTab() throws ClassNotFoundException, SQLException
	{
		if( GTRDStatsAccess.doesTableExist( "chromatin_info" ) )
		{
			String sql = "SELECT * FROM chromatin_info;";
			try (PreparedStatement ps = con.prepareStatement( sql ))
			{
				ResultSet rs = ps.executeQuery();
				while( rs.next() )
				{
					ChromatinInfoTableRow row = new ChromatinInfoTableRow();
					row.setId( rs.getString( 1 ) );
					row.setTrackId( rs.getString( 2 ) );
					row.setRepNum( rs.getString( 3 ) );
					row.setParentId( rs.getString( 4 ) );
					row.setTrackType( rs.getString( 5 ) );
					row.setAggregation( rs.getString( 6 ) );
					row.setMethod( rs.getString( 7 ) );
					row.setFpMethod( rs.getString( 8 ) );
					row.setFpMethodParams( rs.getString( 9 ) );
					row.setSpecies( rs.getString( 10 ) );
					row.setCellId( rs.getString( 11 ) );
					row.setCompletePath( rs.getString( 12 ) );
					this.add( row );
				}
			}
		}
	}
	
	static Pattern cinfoPattern = Pattern.compile( "CINFO[0-9]{6}" );
	
	public ChromatinInfoTableRow getChromatinInfoTableRow( String chromatinInfoId ) throws ClassNotFoundException, SQLException
	{
		Matcher matcher = cinfoPattern.matcher( chromatinInfoId );
		if( !matcher.find() )
			throw new IllegalArgumentException( chromatinInfoId + " is not chromatin_info_id");
		ChromatinInfoTableRow row = new ChromatinInfoTableRow();
		String sql = "SELECT * FROM chromatin_info WHERE id='" + chromatinInfoId + "';";
		try (PreparedStatement ps = con.prepareStatement( sql ))
		{
			ResultSet rs = ps.executeQuery();
			while( rs.next() )
			{
				row.setId( rs.getString( 1 ) );
				row.setTrackId( rs.getString( 2 ) );
				row.setRepNum( rs.getString( 3 ) );
				row.setParentId( rs.getString( 4 ) );
				row.setTrackType( rs.getString( 5 ) );
				row.setAggregation( rs.getString( 6 ) );
				row.setMethod( rs.getString( 7 ) );
				row.setFpMethod( rs.getString( 8 ) );
				row.setFpMethodParams( rs.getString( 9 ) );
				row.setSpecies( rs.getString( 10 ) );
				row.setCellId( rs.getString( 11 ) );
				row.setCompletePath( rs.getString( 12 ) );
			}
		}
		return row;
	}
	

	public String getChromatinInfoId(String completePath) throws SQLException, ClassNotFoundException
	{
		String chromatinInfoId = null;
		String sql = "SELECT id FROM chromatin_info WHERE complete_path=?";
		try (PreparedStatement ps = con.prepareStatement( sql ))
		{
			ps.setString( 1, completePath );
			ResultSet rs = ps.executeQuery();
			while( rs.next() )
				chromatinInfoId = rs.getString( 1 );
			return chromatinInfoId;
		}
	}

	public void addChromatinInfoTableRowIntoDB( ChromatinInfoTableRow row ) throws SQLException, ClassNotFoundException
	{
		ResultSet rsId = con.createStatement().executeQuery( "SELECT id FROM chromatin_info;");
		while( rsId.next() )
		{
			if( row.getId().equals( rsId.getString( 1 ) ) )
				throw new GTRDStatsAccess.DuplicatedIdException( "Duplicated id: " + row.getId() );
		}
		
		try(PreparedStatement ps = con.prepareStatement( "INSERT INTO chromatin_info "
				+ "(id, track_id, rep_num, parent_id, track_type, aggregation, method, fp_method, "
				+ "method_params, species, cell_id, complete_path) "
				+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)" ))
		{
			ps.setString( 1, row.getId() );
			ps.setString( 2, row.getTrackId() );
			ps.setString( 3, row.getRepNum() );
			ps.setString( 4, row.getParentId() );
			ps.setString( 5, row.getTrackType() );
			ps.setString( 6, row.getAggregation() );
			ps.setString( 7, row.getMethod() );
			ps.setString( 8, row.getFpMethod() );
			ps.setString( 9, row.getFpMethodParams() );
			ps.setString( 10, row.getSpecies() );
			ps.setString( 11, row.getCellId() );
			ps.setString( 12, row.getCompletePath() );
			ps.executeUpdate();
		}
	}

	private static Connection getSQLConnection(String connectionString) throws SQLException, ClassNotFoundException
	{
		Class.forName( "com.mysql.jdbc.Driver" );
		Connection con = DriverManager.getConnection( connectionString );
		return con;
	}

	public String getNewChromatinInfoId() throws SQLException, ClassNotFoundException
	{	
		Statement st = con.createStatement();
		ResultSet rs = st.executeQuery( "SELECT MAX(id) FROM chromatin_info");
		if( rs.next() )
		{
			String lastIdString = rs.getString( 1 );
			int nextId = lastIdString == null ? 0 : Integer.parseInt( lastIdString.substring( "CINFO".length() ) ) + 1;
			return String.format( "CINFO%06d", nextId );
		}
		return "CINFO000001";
	}

	public void writeChromatinInfoTable( List<ChromatinInfoTableRow> chromatinInfoTable, DataElementPath outputPath)
	{
		final SqlTableDataCollection outputTable = (SqlTableDataCollection)TableDataCollectionUtils.createTableDataCollection( outputPath );
		SqlColumnModel cm = outputTable.getColumnModel();
		cm.addColumn( "track_id", String.class );
		cm.addColumn( "rep_num", String.class );
		cm.addColumn( "parent_id", String.class ); 
		cm.addColumn( "track_type", String.class ); 
		cm.addColumn( "aggregation", String.class ); 
		cm.addColumn( "method", String.class ); 
		cm.addColumn( "method_params", String.class );
		cm.addColumn( "fp_method", String.class );
		cm.addColumn( "species", String.class );
		cm.addColumn( "cell_id", String.class );
		cm.addColumn( "complete_path", String.class );

		for( ChromatinInfoTableRow row : chromatinInfoTable )
		{
			TableDataCollectionUtils.addRow( outputTable, row.getId(), row.getParametersArray(), true );
		}
		outputTable.finalizeAddition();
		outputPath.save( outputTable );
	}

	public void writeChromatinInfoTableToDB( List<ChromatinInfoTableRow> chromatinInfoTable ) throws SQLException, ClassNotFoundException
	{
		Statement st = con.createStatement();
		String sql = "CREATE TABLE IF NOT EXISTS chromatin_info (id VARCHAR(255) NOT NULL, track_id VARCHAR(255), rep_num INT, "
				+ "parent_id VARCHAR(255), track_type VARCHAR(255), aggregation VARCHAR(255), method VARCHAR(255), "
				+ "fp_method VARCHAR(255), fp_method_params VARCHAR(255), species VARCHAR(255),"
				+ "cell_id INT, complete_path TEXT, PRIMARY KEY (id));";
		st.execute( sql );
		for( ChromatinInfoTableRow row : chromatinInfoTable )
		{
			String[] fields = row.getAllFields();
			for( int i = 0; i < fields.length; i++ )
				fields[i] = quote( fields[i] );
			st.executeUpdate( "INSERT INTO chromatin_info (id, track_id, rep_num, parent_id, track_type, aggregation, method, "
					+ "fp_method, fp_method_params, species, cell_id, complete_path) VALUES("
					+ String.join( ",", fields ) + ");" );
		}
	}

	public void removeChromatinInfoTable() throws SQLException, ClassNotFoundException
	{
		Statement st = con.createStatement();
		if( GTRDStatsAccess.doesTableExist( "chromatin_info" ) )
		{
			String sql = "DROP TABLE chromatin_info;";
			st.execute( sql );
		}
	}

	/*
	public static void fixChromatinInfoIds( HashMap<String, String> idsToFix ) throws ClassNotFoundException, SQLException
	{
		List<String> sqls = new ArrayList<String>();
		String connectionString = "jdbc:mysql://localhost:3306/gtrd_v1903_stats?user=gtrd&password=gtrd";
		Connection con = getSQLConnection( connectionString );
		Statement st = con.createStatement();
		HashMap<String, String> pathToIds = getChromatinInfoIdsPaths();
		
		List<String> tablesWithIds = new ArrayList<String>();
		String sql = "SHOW TABLES;";
		ResultSet rs = st.executeQuery( sql );
		while( rs.next() )
			tablesWithIds.add(rs.getString( 1 ));
		tablesWithIds.remove("track_info");
		tablesWithIds.remove("chromatin_info");
		
		for(String table : tablesWithIds)
		{
			sql = "select * from " + table;
			rs = st.executeQuery( sql );
			ResultSetMetaData rsmd = rs.getMetaData();
			boolean doFix = false;
			while( rs.next() )
			{
				for( int i = 1; 1 <= rsmd.getColumnCount(); i++ )
				{
					String value = rs.getString( i );
					if(value != null && idsToFix.containsKey( value ))
					{
						
					}
				}
			}
		}
	}*/
	
	public HashMap<String, String> getChromatinInfoIdsPaths() throws ClassNotFoundException, SQLException
	{
		HashMap<String, String> backup = new HashMap<>();
		//String connectionString = "jdbc:mysql://localhost:3306/gtrd_v1903_stats?user=gtrd&password=gtrd";
		//Connection con = getSQLConnection( connectionString );
                Connection con = Connectors.getConnection( "gtrd_v1903_stats" );
		Statement st = con.createStatement();
		
		String sql = "select id, complete_path from chromatin_info";
		ResultSet rs = st.executeQuery( sql );
		if( rs.next() )
			backup.put(rs.getString( 2 ), rs.getString( 1 ));
		return backup;
	}
	
	public static String quote(String str)
	{
		if( str == null )
			return "NULL";
		return "'" + str.replace( "'", "''" ).replace( "\\", "\\\\" ) + "'";
	}

	public static class ChromatinInfoTableRow
	{
		private String id, trackId, repNum, parentId, trackType, aggregation, method, fpMethodParams, fpMethod, cellId, species, completePath;

		ChromatinInfoTableRow()
		{}

		private ChromatinInfoTableRow(Builder builder)
		{
			id = builder.id;
			trackId = builder.trackId;
			repNum = builder.repNum;
			parentId = builder.parentId;
			trackType = builder.trackType;
			aggregation = builder.aggregation;
			method = builder.method;
			fpMethod = builder.fpMethod;
			fpMethodParams = builder.fpMethodParams;
			cellId = builder.cellId;
			species = builder.species;
			completePath = builder.completePath;
		}

		public static class Builder
		{
			private String id, trackId, repNum, parentId, trackType, aggregation, method, fpMethodParams, fpMethod, cellId, species, completePath;
			public Builder setId(String id) 
			{
				this.id = id;
				return this;
			}
			public Builder setFpMethod(String fpMethod) 
			{
				this.fpMethod = fpMethod;
				return this;
			}
			public Builder setTrackId(String trackId) 
			{
				this.trackId = trackId;
				return this;
			}
			public Builder setRepNum(String repNum) 
			{
				this.repNum = repNum;
				return this;
			}
			public Builder setParentId(String parentId) 
			{
				this.parentId = parentId;
				return this;
			}
			public Builder setTrackType(String trackType) 
			{
				this.trackType = trackType;
				return this;
			}
			public Builder setAggregation(String aggregation) 
			{
				this.aggregation = aggregation;
				return this;
			}
			public Builder setMethod(String method) 
			{
				this.method = method;
				return this;
			}
			public Builder setFpMethodParams(String fpMethodParams) 
			{
				this.fpMethodParams = fpMethodParams;
				return this;
			}
			public Builder setCellId(String cellId) 
			{
				this.cellId = cellId;
				return this;
			}
			public Builder setSpecies(String species) 
			{
				this.species = species;
				return this;
			}
			public Builder setCompletePath(String completePath) 
			{
				this.completePath = completePath;
				return this;
			}
			public ChromatinInfoTableRow build() {
				return new ChromatinInfoTableRow(this);
			}
		}

		public String getId() 
		{
			return id;
		}
		public void setId(String id) 
		{
			this.id = id;
		}
		public String getTrackId() 
		{
			return trackId;
		}
		public void setTrackId(String trackId) 
		{
			this.trackId = trackId;
		}
		public String getParentId() 
		{
			return parentId;
		}
		public void setParentId(String parentId) 
		{
			this.parentId = parentId;
		}
		public String getTrackType() 
		{
			return trackType;
		}
		public void setTrackType(String trackType) 
		{
			this.trackType = trackType;
		}
		public String getFpMethod() {
			return fpMethod;
		}

		public void setFpMethod(String fpMethod) {
			this.fpMethod = fpMethod;
		}

		public String getAggregation() {
			return aggregation;
		}

		public void setAggregation(String aggregation) {
			this.aggregation = aggregation;
		}

		public String getMethod() 
		{
			return method;
		}
		public void setMethod(String method) 
		{
			this.method = method;
		}
		public String getFpMethodParams() 
		{
			return fpMethodParams;
		}
		public void setFpMethodParams(String fpMethodParams) 
		{
			this.fpMethodParams = fpMethodParams;
		}
		public String getCellId() 
		{
			return cellId;
		}
		public void setCellId(String cellId) 
		{
			this.cellId = cellId;
		}
		public String getSpecies() 
		{
			return species;
		}
		public void setSpecies(String species) 
		{
			this.species = species;
		}
		public String getCompletePath() 
		{
			return completePath;
		}
		public void setCompletePath(String completePath) 
		{
			this.completePath = completePath;
		}
		public String getRepNum() 
		{
			return repNum;
		}
		public void setRepNum(String repNum) 
		{
			this.repNum = repNum;
		}

		public String[] getParametersArray()
		{
			String[] array = new String[11];
			array[0] = this.getTrackId();
			array[1] = this.getRepNum();
			array[2] = this.getParentId();
			array[3] = this.getTrackType();
			array[4] = this.getAggregation();
			array[5] = this.getMethod();
			array[6] = this.getFpMethod();
			array[7] = this.getFpMethodParams();
			array[8] = this.getSpecies();
			array[9] = this.getCellId();
			array[10] = this.getCompletePath();

			return array;
		}

		public String[] getAllFields()
		{
			String[] array = new String[12];
			array[0] = this.getId();
			array[1] = this.getTrackId();
			array[2] = this.getRepNum();
			array[3] = this.getParentId();
			array[4] = this.getTrackType();
			array[5] = this.getAggregation();
			array[6] = this.getMethod();
			array[7] = this.getFpMethod();
			array[8] = this.getFpMethodParams();
			array[9] = this.getSpecies();
			array[10] = this.getCellId();
			array[11] = this.getCompletePath();

			return array;
		}
	}
}
