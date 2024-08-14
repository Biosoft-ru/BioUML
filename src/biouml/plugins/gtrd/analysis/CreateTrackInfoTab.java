package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.analysis.TrackInfoTable.TrackInfoTableRow;
import biouml.plugins.gtrd.analysis.TrackInfoTable;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CreateTrackInfoTab extends AnalysisMethodSupport<CreateTrackInfoTab.CreateTrackInfoTabParamteres>
{
	private static List<TrackInfoTableRow> trackInfoTable = new ArrayList<>();
	private static String lastTrackInfoId = "TINFO000000";
	private static final DataElementPath CHIPSEQ_EXPS_PATH = DataElementPath.create( "databases/GTRD/Data/experiments" );
	private static final DataElementPath CHIPSEQ_CLUSTERS_PATH = DataElementPath.create( "databases/GTRD/Data/clusters" );

	public CreateTrackInfoTab(DataCollection<?> origin, String name)
	{
		super(origin, name, new CreateTrackInfoTabParamteres());
	}

	@Override
	public DataCollection<?> justAnalyzeAndPut() throws LoggedException, Exception 
	{
		DataCollection<ChIPseqExperiment> chipseqExperiments = CHIPSEQ_EXPS_PATH.optDataCollection( ChIPseqExperiment.class );
		for( ChIPseqExperiment exp : chipseqExperiments )
		{
			if( !exp.isControlExperiment() )
				for( String peakCaller : ChIPseqExperiment.PEAK_CALLERS )
				{
					ru.biosoft.access.core.DataElementPath peakPath = exp.getPeaksByPeakCaller(peakCaller);
					if(peakPath.exists())
					{
						TrackInfoTableRow row = new TrackInfoTableRow();
						row.setId( getNewTrackInfoId() );
						row.setFactorId( exp.getTfUniprotId() );
						row.setTrackId( peakPath.getName() );
						row.setTrackType( "chip-seq" );
						row.setMethod( peakCaller );
						row.setSpecies( exp.getSpecie().getLatinName() );
						row.setCellId( exp.getCell().getName() );
						row.setCompletePath( peakPath.toString() );
						if( exp.getControlId() != null )
							row.setHasControl( "true" );
						else
							row.setHasControl( "false" );
						trackInfoTable.add( row );
					}
				}
		}

		DataCollection<DataElement> chipseqClusters = CHIPSEQ_CLUSTERS_PATH.optDataCollection( ru.biosoft.access.core.DataElement.class );
		for( ru.biosoft.access.core.DataElement speciesDir : chipseqClusters )
		{
			for( String peakCaller : new String[] {"MACS", "GEM", "PICS", "SISSRS", "meta"} )
			{
				TrackInfoTableRow row = new TrackInfoTableRow();
				row.setId( getNewTrackInfoId() );
				if( peakCaller.equals( "meta" ))
					row.setTrackType( "meta-clusters" );
				else
				{
					row.setTrackType( "clusters" );
					row.setMethod( peakCaller.toLowerCase() );
				}
				row.setSpecies( speciesDir.getName() );
				String trackId = "all " + peakCaller + " clusters";
				row.setTrackId( trackId );
				row.setCompletePath( speciesDir.getCompletePath() + "/" + trackId );
				trackInfoTable.add( row );
			}
			ru.biosoft.access.core.DataElementPath byTf = DataElementPath.create( speciesDir.getCompletePath().getDataCollection(), "By TF" );
			for( ru.biosoft.access.core.DataElement factorDir : byTf.getDataCollection())
			{
				for( String peakCaller : new String[] {"MACS", "GEM", "PICS", "SISSRS", "meta"} )
				{
					TrackInfoTableRow row = new TrackInfoTableRow();
					row.setId( getNewTrackInfoId() );
					if( peakCaller.equals( "meta" ))
						row.setTrackType( "meta-clusters" );
					else
					{
						row.setTrackType( "clusters" );
						row.setMethod( peakCaller.toLowerCase() );
					}
					row.setSpecies( speciesDir.getName() );
					row.setFactorId( factorDir.getName() );
					String trackId = peakCaller + " clusters";
					row.setTrackId( trackId );
					row.setCompletePath( factorDir.getCompletePath() + "/" + trackId);
					trackInfoTable.add( row );
				}
			}
		}
		
		TrackInfoTable trackInfo = new TrackInfoTable();
		
		trackInfo.removeTrackInfoTable();
		if( !parameters.isWriteToDB() )
			trackInfo.writeTrackInfoTable( trackInfoTable, parameters.getOutputPath() );
		else
			trackInfo.writeTrackInfoTableToDB( trackInfoTable );
		
		return null;
	}

	public static Connection getSQLConnection(String connectionString) throws SQLException, ClassNotFoundException
    {
        Class.forName( "com.mysql.jdbc.Driver" );
        Connection con = DriverManager.getConnection( connectionString );
        return con;
    }
    
	private static String getNewTrackInfoId()
	{
		int newId = lastTrackInfoId == null ? 0 : Integer.parseInt( lastTrackInfoId.substring( "TINFO".length() ) ) + 1;
		String newTrackInfoId = String.format( "TINFO" + "%06d", newId );
		lastTrackInfoId = newTrackInfoId;
		return newTrackInfoId;
	}

	public static class CreateTrackInfoTabParamteres extends AbstractAnalysisParameters
	{
		private DataElementPath outputPath;
		private boolean writeToDB = false;
		
		CreateTrackInfoTabParamteres()
		{}

		public DataElementPath getOutputPath() 
		{
			return outputPath;
		}
		public void setOutputPath(DataElementPath outputPath) 
		{
			this.outputPath = outputPath;
		}

		public boolean isWriteToDB() {
			return writeToDB;
		}

		public void setWriteToDB(boolean writeToDB) {
			this.writeToDB = writeToDB;
		}

	}

	public static class CreateTrackInfoTabParamteresBeanInfo extends BeanInfoEx2<CreateTrackInfoTabParamteres>
	{
		public CreateTrackInfoTabParamteresBeanInfo()
		{
			super(CreateTrackInfoTabParamteres.class);
		}

		@Override
		protected void initProperties() throws Exception 
		{
			property("writeToDB").add();
			property("outputPath").hidden("isWriteToDB").outputElement( TableDataCollection.class ).add();
		}
	}



}
