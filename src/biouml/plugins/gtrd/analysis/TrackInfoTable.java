package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biouml.plugins.gtrd.access.GTRDStatsAccess;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.table.SqlColumnModel;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TrackInfoTable  extends ArrayList<TrackInfoTable.TrackInfoTableRow>
{
	//static String DB_CONNECTION_STRING = "jdbc:mysql://micro:3306/gtrd_v2005_stats?user=gtrd&password=gtrd";
	static String GTRD_STATS_DB = "gtrd_v2005_stats";      
	static Connection con;
	
	TrackInfoTable() throws ClassNotFoundException, SQLException
	{
		//con = getSQLConnection( DB_CONNECTION_STRING );
                con = Connectors.getConnection( GTRD_STATS_DB );
	}
	
	public void getTrackInfoTab() throws ClassNotFoundException, SQLException
	{
		if( GTRDStatsAccess.doesTableExist( "track_info" ) )
		{
			String sql = "SELECT * FROM track_info";
			try (PreparedStatement ps = con.prepareStatement( sql ))
			{
				ResultSet rs = ps.executeQuery();
				while( rs.next() )
				{
					TrackInfoTableRow row = new TrackInfoTableRow();
					row.setId( rs.getString( 1 ) );
					row.setFactorId( rs.getString( 2 ) );
					row.setTrackId( rs.getString( 3 ) );
					row.setParentId( rs.getString( 4 ) );
					row.setTrackType( rs.getString( 5 ) );
					row.setMethod( rs.getString( 6 ) );
					row.setMethodParams( rs.getString( 7 ) );
					row.setSpecies( rs.getString( 8 ) );
					row.setCellId( rs.getString( 9 ) );
					row.setHasControl( rs.getString( 10 ) );
					row.setCompletePath( rs.getString( 11 ) );
					this.add( row );
				}
			}
		}
	}

	static Pattern tinfoPattern = Pattern.compile( "TINFO[0-9]{6}" );
	
	public TrackInfoTableRow getTrackInfoTableRow( String trackInfoId ) throws ClassNotFoundException, SQLException
	{
		Matcher matcher = tinfoPattern.matcher( trackInfoId );
		if( !matcher.find() )
			throw new IllegalArgumentException( trackInfoId + " is not chromatin_info_id");
		TrackInfoTableRow row = new TrackInfoTableRow();
		String sql = "SELECT * FROM chromatin_info WHERE id='" + trackInfoId + "';";
		try (PreparedStatement ps = con.prepareStatement( sql ))
		{
			ResultSet rs = ps.executeQuery();
			while( rs.next() )
			{
				row.setId( rs.getString( 1 ) );
				row.setFactorId( rs.getString( 2 ) );
				row.setTrackId( rs.getString( 3 ) );
				row.setParentId( rs.getString( 4 ) );
				row.setTrackType( rs.getString( 5 ) );
				row.setMethod( rs.getString( 6 ) );
				row.setMethodParams( rs.getString( 7 ) );
				row.setSpecies( rs.getString( 8 ) );
				row.setCellId( rs.getString( 9 ) );
				row.setHasControl( rs.getString( 10 ) );
				row.setCompletePath( rs.getString( 11 ) );
			}
		}
		return row;
	}
	
	public String getTrackInfoId(String pathToFile) throws SQLException, ClassNotFoundException
	{
		String trackInfoId = null;
		String sql = "SELECT id FROM track_info WHERE complete_path=?";
        try (PreparedStatement ps = con.prepareStatement( sql ))
        {
            ps.setString( 1, pathToFile );
            ResultSet rs = ps.executeQuery();
            while( rs.next() )
            	trackInfoId = rs.getString( 1 );
            return trackInfoId;
        }
	}
	
	public void addTrackInfoTableRowIntoDB( TrackInfoTableRow row ) throws SQLException, ClassNotFoundException
	{
		ResultSet rsId = con.createStatement().executeQuery( "SELECT id FROM chromatin_info;");
		while( rsId.next() )
		{
			if( row.getId().equals( rsId.getString( 1 ) ) )
				throw new GTRDStatsAccess.DuplicatedIdException( "Duplicated id: " + row.getId() );
		}
		
		try(PreparedStatement ps = con.prepareStatement( "INSERT INTO track_info "
				+ "(id, factor_id, track_id, parent_id, track_type, method, method_params, species, cell_id, has_control, complete_path) "
				+ "VALUES(?,?,?,?,?,?,?,?,?,?,?)" ))
        {
			ps.setString( 1, row.getId() );
            ps.setString( 2, row.getFactorId() );
            ps.setString( 3, row.getTrackId() );
            ps.setString( 4, row.getParentId() );
            ps.setString( 5, row.getTrackType() );
            ps.setString( 6, row.getMethod() );
            ps.setString( 7, row.getMethodParams() );
            ps.setString( 8, row.getSpecies() );
            ps.setString( 9, row.getCellId() );
            ps.setString( 10, row.getHasControl() );
            ps.setString( 11, row.getCompletePath() );
            ps.executeUpdate();
        }
	}
	
	private static Connection getSQLConnection(String connectionString) throws SQLException, ClassNotFoundException
    {
        Class.forName( "com.mysql.jdbc.Driver" );
        Connection con = DriverManager.getConnection( connectionString );
        return con;
    }
	
	public String getNewTrackInfoId() throws SQLException, ClassNotFoundException
	{	
		Statement st = con.createStatement();
	        ResultSet rs = st.executeQuery( "SELECT MAX(id) FROM track_info");
	        if( rs.next() )
	        {
	            String lastIdString = rs.getString( 1 );
	            int nextId = lastIdString == null ? 0 : Integer.parseInt( lastIdString.substring( "TINFO".length() ) ) + 1;
	            return String.format( "TINFO%06d", nextId );
	        }
	        return "TINFO000001";
	}
	
	public void writeTrackInfoTable( List<TrackInfoTableRow> trackInfoTable, DataElementPath outputPath) throws ClassNotFoundException, SQLException
	{
		final SqlTableDataCollection outputTable = (SqlTableDataCollection)TableDataCollectionUtils.createTableDataCollection( outputPath );
		SqlColumnModel cm = outputTable.getColumnModel();
		cm.addColumn( "factor_id", String.class );
		cm.addColumn( "track_id", String.class ); 
		cm.addColumn( "parent_id", String.class ); 
		cm.addColumn( "track_type", String.class ); 
		cm.addColumn( "method", String.class ); 
		cm.addColumn( "method_params", String.class );
		cm.addColumn( "species", String.class );
		cm.addColumn( "cell_id", String.class );
		cm.addColumn( "has_control", String.class );
		cm.addColumn( "complete_path", String.class );

		for( TrackInfoTableRow row : trackInfoTable )
		{
			TableDataCollectionUtils.addRow( outputTable, row.getId(), row.getParametersArray(), true );
		}
		outputTable.finalizeAddition();
		outputPath.save( outputTable );
	}

	public void writeTrackInfoTableToDB( List<TrackInfoTableRow> trackInfoTable ) throws SQLException, ClassNotFoundException
	{
		//Connection con = getSQLConnection( DB_CONNECTION_STRING );
                Connection con = Connectors.getConnection( GTRD_STATS_DB );
		Statement st = con.createStatement();
		
		String sql = "CREATE TABLE IF NOT EXISTS track_info (id VARCHAR(255) NOT NULL, factor_id VARCHAR(255), "
				+ "track_id VARCHAR(255),parent_id VARCHAR(255),track_type VARCHAR(255),method VARCHAR(255), "
				+ "method_params VARCHAR(255),species VARCHAR(255), cell_id INT,has_control VARCHAR(255), "
				+ "complete_path TEXT,PRIMARY KEY (id));";
		st.execute( sql );
		for( TrackInfoTableRow row : trackInfoTable )
		{
			String[] fields = row.getAllFields();
			for( int i = 0; i < fields.length; i++ )
				fields[i] = quote( fields[i] );
	        st.executeUpdate( "INSERT INTO track_info (id,factor_id,track_id,parent_id,track_type,method,"
	        		+ "method_params,species,cell_id,has_control,complete_path) VALUES("
	        		+ String.join( ",", fields ) + ")" );
		}
	}
	
	public void removeTrackInfoTable() throws SQLException, ClassNotFoundException
	{
		Statement st = con.createStatement();
		if( GTRDStatsAccess.doesTableExist( "track_info" ) )
		{
			String sql = "DROP TABLE track_info;";
			st.execute( sql );
		}
	}
	
	public static String quote(String str)
    {
        if( str == null )
            return "NULL";
        return "'" + str.replace( "'", "''" ).replace( "\\", "\\\\" ) + "'";
    }
	
	public static class TrackInfoTableRow
	{
		private String id, factorId, trackId, parentId, trackType, method, methodParams, cellId, species, completePath, hasControl;
		
		TrackInfoTableRow()
		{}
		
		private TrackInfoTableRow(Builder builder)
		{
			id = builder.id;
			factorId = builder.factorId;
			trackId = builder.trackId;
			parentId = builder.parentId;
			trackType = builder.trackType;
			method = builder.method;
			methodParams = builder.methodParams;
			cellId = builder.cellId;
			species = builder.species;
			completePath = builder.completePath;
			hasControl = builder.hasControl;
		}
		
		public static class Builder
		{
			private String id, factorId, trackId, parentId, trackType, method, methodParams, cellId, species, completePath, hasControl;
			public Builder setId(String id) 
			{
	            this.id = id;
	            return this;
	        }
			public Builder setFactorId(String factorId) 
			{
	            this.factorId = factorId;
	            return this;
	        }
			public Builder setTrackId(String trackId) 
			{
	            this.trackId = trackId;
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
			public Builder setMethod(String method) 
			{
	            this.method = method;
	            return this;
	        }
			public Builder setMethodParams(String methodParams) 
			{
	            this.methodParams = methodParams;
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
			public Builder setHasControl(String hasControl) 
			{
	            this.hasControl = hasControl;
	            return this;
	        }
			public TrackInfoTableRow build() {
	            return new TrackInfoTableRow(this);
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
		public String getFactorId() 
		{
			return factorId;
		}
		public void setFactorId(String factorId) 
		{
			this.factorId = factorId;
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
		public String getMethod() 
		{
			return method;
		}
		public void setMethod(String method) 
		{
			this.method = method;
		}
		public String getMethodParams() 
		{
			return methodParams;
		}
		public void setMethodParams(String methodParams) 
		{
			this.methodParams = methodParams;
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
		public String getHasControl()
		{
			return hasControl;
		}
		public void setHasControl(String hasControl) 
		{
			this.hasControl = hasControl;
		}
		
		public String[] getParametersArray()
		{
			String[] array = new String[10];
			array[0] = this.getFactorId();
			array[1] = this.getTrackId();
			array[2] = this.getParentId();
			array[3] = this.getTrackType();
			array[4] = this.getMethod();
			array[5] = this.getMethodParams();
			array[6] = this.getSpecies();
			array[7] = this.getCellId();
			array[8] = this.getHasControl();
			array[9] = this.getCompletePath();

			return array;
		}
		
		public String[] getAllFields()
		{
			String[] array = new String[11];
			array[0] = this.getId();
			array[1] = this.getFactorId();
			array[2] = this.getTrackId();
			array[3] = this.getParentId();
			array[4] = this.getTrackType();
			array[5] = this.getMethod();
			array[6] = this.getMethodParams();
			array[7] = this.getSpecies();
			array[8] = this.getCellId();
			array[9] = this.getHasControl();
			array[10] = this.getCompletePath();

			return array;
		}
	}
}
