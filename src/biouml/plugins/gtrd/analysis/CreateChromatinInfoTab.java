package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.analysis.ChromatinInfoTable.ChromatinInfoTableRow;
import biouml.plugins.gtrd.analysis.ChromatinInfoTable;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CreateChromatinInfoTab extends AnalysisMethodSupport<CreateChromatinInfoTab.CreateChromatinInfoTabParamteres>
{
	private static List<ChromatinInfoTableRow> chromatinInfoTable = new ArrayList<>();
	private static String lastChromatinInfoId = "CINFO000000";
	private static final DataElementPath DNASE_EXPS_PATH = DataElementPath.create( "databases/GTRD/Data/DNase experiments" );

	public CreateChromatinInfoTab(DataCollection<?> origin, String name)
	{
		super(origin, name, new CreateChromatinInfoTabParamteres());
	}

	@Override
	public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception
	{
		DataCollection<DNaseExperiment> dnaseExperiments = DNASE_EXPS_PATH.optDataCollection( DNaseExperiment.class );
		for( DNaseExperiment exp : dnaseExperiments )
		{
			for( String peakCaller : DNaseExperiment.PEAK_CALLERS )
			{
				DataElementPathSet peakPaths = exp.getPeaksByPeakCaller(peakCaller);
				if( !peakPaths.isEmpty() )
				{
					for( DataElementPath peakPath : peakPaths.toArray(new ru.biosoft.access.core.DataElementPath[peakPaths.size()]) )
					{
						ChromatinInfoTableRow row = new ChromatinInfoTableRow();
						row.setId( getNewChromatinInfoId() );
						row.setTrackId( peakPath.getName().split("_")[0] );
						row.setRepNum( peakPath.getName().split("_")[1] );
						row.setTrackType( "dnase-seq" );
						if(peakCaller.contains( "wellington" ))
						{
							row.setMethod( peakCaller.split("-")[1] );
							row.setFpMethod( "wellington" );
							row.setFpMethodParams( "FDR:0.01" );
						} 
						else
							row.setMethod( peakCaller );
						row.setSpecies( exp.getSpecie().getLatinName() );
						row.setCellId( exp.getCell().getName() );
						row.setCompletePath( peakPath.toString() );
						chromatinInfoTable.add( row );
					}
				}
			}
		}
		
		for( String peakCaller : new String[] {"macs2", "hotspot2"})
		{
			ru.biosoft.access.core.DataElementPath pathToDir = DataElementPath.create( "data/Collaboration/GTRD_build/Data/DNase_profiles/" 
					+ peakCaller + "_hsa_dnase_merged_by_cells" );
			DataElementPathSet peakFiles = pathToDir.getChildren();
			for( DataElementPath peakPath : peakFiles )
			{
				ChromatinInfoTableRow row = new ChromatinInfoTableRow();
				row.setId( getNewChromatinInfoId() );
				row.setTrackId( peakPath.getName() );
				row.setTrackType( "dnase-seq" );
				row.setAggregation( "merged by cell_id" );
				row.setMethod( peakCaller );
				row.setSpecies( "Homo sapiens" );
				row.setCellId( peakPath.getName().split("_")[1] );
				row.setCompletePath( peakPath.toString() );
				chromatinInfoTable.add( row );
			}
		}
		
		ChromatinInfoTable chromatinInfo = new ChromatinInfoTable();
		chromatinInfo.removeChromatinInfoTable();
		
		if( !parameters.isWriteToDB() )
			chromatinInfo.writeChromatinInfoTable( chromatinInfoTable, parameters.getOutputPath() );
		else
		{
			chromatinInfo.writeChromatinInfoTableToDB( chromatinInfoTable );
			//fix renamed ids by file path
			/*HashMap<String, String> newPathToIds = ChromatinInfoTable.getChromatinInfoIdsPaths();
			HashMap<String, String> idsToFix = new HashMap<String, String>(); // <Old id, New id>
			for( Entry<String, String> oldEntry : backupPathToId.entrySet() )
			{
				String newIdForThePath;
				if( newPathToIds.containsKey( oldEntry.getKey() ) )
				{
					newIdForThePath = newPathToIds.get( oldEntry.getKey() );
					if( !oldEntry.getKey().equalsIgnoreCase( newIdForThePath ) )
						idsToFix.put( oldEntry.getKey(), newIdForThePath );
				}
			}
			if( !idsToFix.isEmpty() )
				ChromatinInfoTable.fixChromatinInfoIds( idsToFix );*/
		}
		
		return null;
	}

	public static Connection getSQLConnection(String connectionString) throws SQLException, ClassNotFoundException
    {
        Class.forName( "com.mysql.jdbc.Driver" );
        Connection con = DriverManager.getConnection( connectionString );
        return con;
    }
    
	private static String getNewChromatinInfoId()
	{
		int newId = lastChromatinInfoId == null ? 0 : Integer.parseInt( lastChromatinInfoId.substring( "CINFO".length() ) ) + 1;
		String newChromatinInfoId = String.format( "CINFO" + "%06d", newId );
		lastChromatinInfoId = newChromatinInfoId;
		return newChromatinInfoId;
	}

	public static class CreateChromatinInfoTabParamteres extends AbstractAnalysisParameters
	{
		private DataElementPath outputPath;
		private boolean writeToDB = false;
		
		CreateChromatinInfoTabParamteres()
		{}
		
		public DataElementPath getOutputPath() 
		{
			return outputPath;
		}
		public void setOutputPath(DataElementPath outputPath) 
		{
			this.outputPath = outputPath;
		}

		public boolean isWriteToDB() 
		{
			return writeToDB;
		}

		public void setWriteToDB(boolean writeToDB) 
		{
			this.writeToDB = writeToDB;
		}
	}

	public static class CreateChromatinInfoTabParamteresBeanInfo extends BeanInfoEx2<CreateChromatinInfoTabParamteres>
	{
		public CreateChromatinInfoTabParamteresBeanInfo()
		{
			super(CreateChromatinInfoTabParamteres.class);
		}

		@Override
		protected void initProperties() throws Exception 
		{
			property("writeToDB").add();
			property("outputPath").hidden("isWriteToDB").outputElement( TableDataCollection.class ).add();
		}
	}
}
