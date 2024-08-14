package biouml.plugins.gtrd.analysis;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.developmentontheedge.beans.DynamicPropertySet;
import ru.biosoft.jobcontrol.Iteration;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.analysis.merge.MergePeakCallers.Caller;
import gnu.trove.map.hash.TIntObjectHashMap;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PrepareClusterToExp extends AnalysisMethodSupport<PrepareClusterToExp.Parameters>
{

	DataCollection<Site> sites;
	int autoId;
	Map<String, String> peakToExp;

	public PrepareClusterToExp(DataCollection<?> origin, String name)
	{
		super( origin, name, new Parameters() );
	}

	@Override
	public Object justAnalyzeAndPut() throws Exception
	{
		SqlTableDataCollection result = (SqlTableDataCollection)TableDataCollectionUtils.createTableDataCollection( parameters.getResult() );
		result.getColumnModel().addColumn( "Cluster id", Integer.class );
		result.getColumnModel().addColumn( "Exp id", String.class );
		peakToExp = loadPeakToExpTable();
		boolean doFirstAlgorithm = true;
		long startTime, endTime, timeElapsed;

		if( doFirstAlgorithm )
		{
			startTime = System.currentTimeMillis();
			log.info( "Loading all sites" );
			sites = parameters.getClustersPath().getDataElement( Track.class ).getAllSites();
			log.info( "Loading all sites is done" );
			HashMap<String, HashMap<Integer, Integer>> clusterIdToIdByTF = new HashMap<>();
			DynamicPropertySet props;
			autoId = 1;
			log.info( "Filling in result table by sql track" );
			int siteCounter = 1;
			int siteSize = sites.getSize();
			for(Site site : sites)
			{
				if( siteCounter++ % 10000000 == 0 )
				{
					log.info( siteCounter + " / " + siteSize );
					log.info( clusterIdToIdByTF.size() + " uniprotIds to load" );
				}

				props = site.getProperties();
				Integer expCount = (Integer)props.getValue( "exp.count" );
				String expSet = props.getValueAsString( "exp.set" );
				Integer id = Integer.parseInt( site.getName() );
				boolean areManyExps;
				if( parameters.getClusterTypeName().equals( "meta" ) )
					areManyExps = expSet.contains( "..." );
				else
					areManyExps = expCount > 10;
				if( areManyExps ) // max number of ids in the field == 10
				{
					String currentUniprotId = props.getValueAsString( "uniprotId" );
					Integer clusterId = (Integer) props.getValue( "id" );
					if( !clusterIdToIdByTF.containsKey( currentUniprotId ) )
						clusterIdToIdByTF.put(currentUniprotId, new HashMap<Integer, Integer>());
					clusterIdToIdByTF.get( currentUniprotId ).put( clusterId, id);
				}
				else
				{
					String[] expIds = props.getValueAsString( "exp.set" ).split( ";" );
					for( String expId : expIds )
						TableDataCollectionUtils.addRow( result, String.valueOf( autoId++ ), new Object[]{ id, expId }, true );
				}
			}
			log.info( "Filling in result table by sql track is done" );
			endTime = System.currentTimeMillis();
			timeElapsed = (endTime - startTime) / 1000;
			log.info( "Filling in result table by sql track takes " + timeElapsed +" s" );
			log.info( "Filling in result table by reading cluster to peak" );
			startTime = System.currentTimeMillis();

			int size = clusterIdToIdByTF.entrySet().size();
			int counter = 1;
			for( Entry<String, HashMap<Integer, Integer>> entry : clusterIdToIdByTF.entrySet())
			{
				log.info( counter++ + " / " + size );
				String currentUniprotId = entry.getKey();
				HashMap<Integer, Integer> clusterIdToId = entry.getValue();
				Set<Integer> clusterIds = clusterIdToId.keySet();
				if( !parameters.getClusterTypeName().equals( "meta" ) )
				{
					TIntObjectHashMap<List<String>> clusterToPeak = loadClusterToPeak( parameters.getClusterTypeName(), currentUniprotId );
					for(Integer clusterId : clusterIds )
					{
						Set<String> expIds = new HashSet<>();
						Integer id = clusterIdToId.get( clusterId );
						for( String peakId : clusterToPeak.get( clusterId ) )
						{
							String expId = peakToExp.get( peakId );
							expIds.add( expId );
						}
						for( String expId : expIds )
						{
							TableDataCollectionUtils.addRow( result, String.valueOf( autoId++ ), new Object[]{ id, expId }, true );
						}
					}
				}
				else
				{
					TIntObjectHashMap<List<CallerCluster>> metaClusterToCluster;
					Map<Caller, TIntObjectHashMap<List<String>>> callerClusterToPeak;

					metaClusterToCluster = loadMetaClusterToCluster( currentUniprotId );
					callerClusterToPeak = new HashMap<>();
					for(Caller caller : Caller.values())
						callerClusterToPeak.put(caller, loadClusterToPeak( caller.name(), currentUniprotId ));
					log.info(currentUniprotId + " loading finished");

					for(Integer metaClusterId : clusterIds )
					{
						Set<String> expIds = new HashSet<>();
						Integer id = clusterIdToId.get( metaClusterId );
						for(CallerCluster cc : metaClusterToCluster.get( metaClusterId ))
						{
							for(String peakId : callerClusterToPeak.get( cc.caller ).get( cc.clusterId ))
							{
								String expId = peakToExp.get( peakId );
								expIds.add(expId);
							}
						}
						for( String expId : expIds )
						{
							TableDataCollectionUtils.addRow( result, String.valueOf( autoId++ ), new Object[]{ id, expId }, true );
						}
					}
				}
			}
			endTime = System.currentTimeMillis();
			timeElapsed = (endTime - startTime) / 1000;
			log.info( "Filling in result table by reading cluster to peak takes " + timeElapsed +" s" );
		}
		else
		{
			log.info( "Loading all sites" );
			sites = parameters.getClustersPath().getDataElement( Track.class ).getAllSites();
			log.info( "Loading all sites is done" );
			HashMap<String, HashMap<Integer, Integer>> clusterIdToIdByTF = new HashMap<>();
			DynamicPropertySet props;
			autoId = 1;
			startTime = System.currentTimeMillis();
			log.info( "Preparing Map<ClusterID, ID>" );
			for(Site site : sites)
			{
				props = site.getProperties();
				String currentUniprotId = props.getValueAsString( "uniprotId" );
				Integer clusterId = (Integer) props.getValue( "id" );
				Integer id = Integer.parseInt( site.getName() );
				if( !clusterIdToIdByTF.containsKey( currentUniprotId ) )
					clusterIdToIdByTF.put(currentUniprotId, new HashMap<Integer, Integer>());
				clusterIdToIdByTF.get( currentUniprotId ).put( clusterId, id);
			}
			endTime = System.currentTimeMillis();
			timeElapsed = (endTime - startTime) / 1000;
			log.info( "Preparing Map<ClusterID, ID> takes " + timeElapsed +" s" );

			startTime = System.currentTimeMillis();
			log.info( "Filling in result table by reading cluster to peak" );
			int size = clusterIdToIdByTF.entrySet().size();
			int counter = 1;
			if( !parameters.getClusterTypeName().equals( "meta" ))
			{
				for( Entry<String, HashMap<Integer, Integer>> entry : clusterIdToIdByTF.entrySet() )
				{
					log.info( counter++ + " / " + size );
					HashMap<Integer, Integer> clusterIdToId = entry.getValue();
					String currentUniprotId = entry.getKey();
					String clusterType = parameters.getClusterTypeName();

					HashMap<Integer, Set<String>> clusterToExpForTF = loadClusterToExpForTF( clusterType, currentUniprotId );

					for( Entry<Integer, Set<String>> clusterToExpForTFentry : clusterToExpForTF.entrySet() )
					{
						Set<String> expIds = clusterToExpForTFentry.getValue();
						Integer clusterId = clusterToExpForTFentry.getKey();
						Integer id = clusterIdToId.get( clusterId );
						for( String expId : expIds )
						{
							TableDataCollectionUtils.addRow( result, String.valueOf( autoId++ ), new Object[]{ id, expId }, true );
						}
					}
				}
			}
			else
			{
				HashMap<String, HashMap<Integer, Integer>> metaClusterToIdByTF = clusterIdToIdByTF;
				for( Entry<String, HashMap<Integer, Integer>> entry : metaClusterToIdByTF.entrySet() )
				{
					log.info( counter++ + " / " + size );
					HashMap<Integer, Integer> metaClusterToId = entry.getValue();
					String currentUniprotId = entry.getKey();

					ru.biosoft.access.core.DataElementPath path = parameters.getMakeMetaTracksOutput().getChildPath( currentUniprotId, "meta cluster to cluster" );
					if(!path.exists())
					{
						log.warning( "Not found " + path );
						return new HashMap<Integer, Set<String>>();
					}

					HashMap<Integer, HashMap<String, Integer>> metaIdToClusterIdByCaller = new HashMap<>();
					TableDataCollection table = path.getDataElement( TableDataCollection.class );
					for(RowDataElement r : table)
					{
						Object[] values = r.getValues();
						Integer metaId = (Integer)values[0];
						Integer clusterId = (Integer)values[1];
						String peakcaller = (String)values[2];

						HashMap<String, Integer> clusterIdsByCaller = metaIdToClusterIdByCaller.get( metaId );
						if( clusterIdsByCaller == null )
							metaIdToClusterIdByCaller.put( metaId, clusterIdsByCaller = new HashMap<>() );
						clusterIdsByCaller.put( peakcaller, clusterId );
					}

					HashMap<String, HashMap<Integer, Set<String>>> clusterToExpForTFbyCaller = new HashMap<>();
					for(Caller caller : Caller.values())
					{
						HashMap<Integer, Set<String>> clusterToExpForTF = loadClusterToExpForTF( caller.name(), currentUniprotId );
						clusterToExpForTFbyCaller.put( caller.name(), clusterToExpForTF );
					}

					for( Entry<Integer, HashMap<String, Integer>> metaIdToClusterIdByCallerEntry : metaIdToClusterIdByCaller.entrySet())
					{
						Integer metaId = metaIdToClusterIdByCallerEntry.getKey();
						HashMap<String, Integer> clusterIdByCaller = metaIdToClusterIdByCallerEntry.getValue();
						Set<String> peakcallers = clusterIdByCaller.keySet();
						Set<String> expIdsAll = new HashSet<>();
						for( String peakcaller : peakcallers )
						{
							Integer clusterId = clusterIdByCaller.get( peakcaller );
							HashMap<Integer, Set<String>> clusterToExpForTF = clusterToExpForTFbyCaller.get( peakcaller );
							Set<String> expIds = clusterToExpForTF.get( clusterId );
							expIdsAll.addAll( expIds );
						}
						Integer id = metaClusterToId.get( metaId );
						for( String expId : expIdsAll )
						{
							TableDataCollectionUtils.addRow( result, String.valueOf( autoId++ ), new Object[]{ id, expId }, true );
						}
					}
				}
			}
			endTime = System.currentTimeMillis();
			timeElapsed = (endTime - startTime) / 1000;
			log.info( "Filling in result table by reading cluster to peak takes " + timeElapsed +" s" );
		}

		log.info( "Filling in result table by reading cluster to peak is done" );
		result.finalizeAddition();
		parameters.getResult().save( result );

		log.info( "Indexing resulting table" );
		SqlUtil.execute( result.getConnection(), "ALTER TABLE " + SqlUtil.quoteIdentifier( result.getTableId() ) + " ADD UNIQUE INDEX(Cluster_id, Exp_id), ADD INDEX(Exp_id)" );
		SqlUtil.execute( result.getConnection(), "ANALYZE TABLE " + SqlUtil.quoteIdentifier( result.getTableId() ) );

		return result;
	}

	private HashMap<Integer, Set<String>> loadClusterToExpForTF( String clusterType, String currentUniprotId)
	{
		ru.biosoft.access.core.DataElementPath path = parameters.getMakeMetaTracksOutput().getChildPath( currentUniprotId, clusterType + " cluster to peak" );
		if(!path.exists())
		{
			log.warning( "Not found " + path );
			return new HashMap<>();
		}
		log.info( "Filling in result table from " + path );
		TableDataCollection table = path.getDataElement( TableDataCollection.class );
		HashMap<Integer, Set<String>> clusterToExpForTF = new HashMap<>();
		for(RowDataElement r : table)
		{
			Object[] values = r.getValues();
			Integer clusterId = (Integer)values[0];
			String peakId = (String)values[1];

			String expId = peakToExp.get( peakId );
			Set<String> list = clusterToExpForTF.get( clusterId );
			if( list == null )
				clusterToExpForTF.put( clusterId, list = new HashSet<>() );
			list.add( expId );
		}
		return clusterToExpForTF;
	}

	private Map<String, String> loadPeakToExpTable()
	{
		Map<String, String> result = new HashMap<>();
		for(ChIPseqExperiment exp : parameters.getExperiments().getDataCollection( ChIPseqExperiment.class ))
		{
			if(exp.isControlExperiment())
				continue;
			String peakId = exp.getPeak().getName();
			String expId = exp.getName();
			result.put( peakId, expId );
		}
		return result;
	}

	public class UniprotIdIteration implements Iteration<List<String>>
	{
		private String curTF;
		private TIntObjectHashMap<List<String>> clusterToPeak;
		private TIntObjectHashMap<List<CallerCluster>> metaClusterToCluster;
		private Map<Caller, TIntObjectHashMap<List<String>>> callerClusterToPeak;
		private TableDataCollection result;
		private int autoId = 1;
		private Map<String, String> peakToExp = new HashMap<>();
		HashMap<String, TIntObjectHashMap<List<CallerCluster>>> metaclusterToClusterAll;
		HashMap<String, HashMap<Caller, TIntObjectHashMap<List<String>>>> callerClusterToPeakAll;
		HashMap<String, TIntObjectHashMap<List<String>>> clusterToPeakAllCurrent;

		public UniprotIdIteration(TableDataCollection result, Map<String, String> peakToExp)
		{
			this.result = result;
			this.peakToExp = peakToExp;
		}

		@Override
		public boolean run(List<String> uniprotIds)
		{
			callerClusterToPeak = new HashMap<>();
			clusterToPeakAllCurrent = new HashMap<>();
			metaclusterToClusterAll = new HashMap<>();
			callerClusterToPeakAll = new HashMap<>();
			long startTime, endTime, timeElapsed;
			startTime = System.currentTimeMillis();
			if(!parameters.getClusterTypeName().equals("meta"))
			{
				for(String uniprotId : uniprotIds)
				{
					clusterToPeakAllCurrent.put(uniprotId, loadClusterToPeak( parameters.getClusterTypeName(), uniprotId ));
					log.info(uniprotId + " loading finished");
				}
			}
			else
			{
				for(String uniprotId : uniprotIds)
				{
					metaclusterToClusterAll.put(uniprotId, loadMetaClusterToCluster( uniprotId ));
					HashMap<Caller, TIntObjectHashMap<List<String>>> callerClusterToPeak = new HashMap<>();
					for(Caller caller : Caller.values())
						callerClusterToPeak.put(caller, loadClusterToPeak( caller.name(), uniprotId ));
					callerClusterToPeakAll.put(uniprotId, callerClusterToPeak);
					log.info(uniprotId + " loading finished");
				}
			}
			endTime = System.currentTimeMillis();
			timeElapsed = (endTime - startTime) / 1000;
			log.info( "loadClusterToPeak for all TFs takes " + timeElapsed +" s" );

			startTime = System.currentTimeMillis();
			for(Site site : sites)
			{
				DynamicPropertySet props = site.getProperties();
				String currentUniprotId = props.getValueAsString( "uniprotId" );
				if(uniprotIds.contains(currentUniprotId))
				{
					Integer id = (Integer)props.getValue( "id" );
					for(String expId : findExperiments(currentUniprotId, id))
					{
						int clusterId = Integer.parseInt( site.getName() );
						TableDataCollectionUtils.addRow( result, String.valueOf( autoId++ ), new Object[]{ clusterId, expId }, true );
					}
				}
			}
			endTime = System.currentTimeMillis();
			timeElapsed = (endTime - startTime) / 1000;
			log.info( "site iteration takes " + timeElapsed +" s" );
			return true;
		}

		private Set<String> findExperiments(String uniprotId, int id)
		{
			if(parameters.getClusterTypeName().equals( "meta" ))
				return findExperimentsForMetaCluster( uniprotId, id );
			return findExperimentsForCluster( uniprotId, id );
		}

		private Set<String> findExperimentsForCluster(String uniprotId, int clusterId)
		{
			if( !uniprotId.equals( curTF ) )
			{
				clusterToPeak = clusterToPeakAllCurrent.get(uniprotId);
				curTF = uniprotId;
			}
			Set<String> exps = new HashSet<>();
			for(String peakId : clusterToPeak.get( clusterId ))
			{
				String expId = peakToExp.get( peakId );
				exps.add(expId);
			}
			return exps;
		}

		private Set<String> findExperimentsForMetaCluster(String uniprotId, int metaClusterId)
		{
			if( !uniprotId.equals( curTF ) )
			{
				callerClusterToPeak = callerClusterToPeakAll.get(uniprotId);
				metaClusterToCluster = metaclusterToClusterAll.get(uniprotId);
				curTF = uniprotId;
			}
			Set<String> exps = new HashSet<>();
			for(CallerCluster cc : metaClusterToCluster.get( metaClusterId ))
			{
				for(String peakId : callerClusterToPeak.get( cc.caller ).get( cc.clusterId ))
				{
					String expId = peakToExp.get( peakId );
					exps.add(expId);
				}
			}
			return exps;
		}
	}

	private TIntObjectHashMap<List<String>> loadClusterToPeak(String clusterType, String uniprotId)
	{
		ru.biosoft.access.core.DataElementPath path = parameters.getMakeMetaTracksOutput().getChildPath( uniprotId, clusterType + " cluster to peak" );
		if(!path.exists())
		{
			log.warning( "Not found " + path );
			return new TIntObjectHashMap<>();
		}
		log.info( "Loading " + path );
		TableDataCollection table = path.getDataElement( TableDataCollection.class );
		TIntObjectHashMap<List<String>> result = new TIntObjectHashMap<>();
		for(RowDataElement r : table)
		{
			Object[] values = r.getValues();
			Integer clusterId = (Integer)values[0];
			List<String> list = result.get( clusterId );
			if(list == null)
				result.put( clusterId, list = new ArrayList<>() );
			String peakId = (String)values[1];
			list.add( peakId.substring( 0, 11 ) );
		}
		return result;
	}

	private TIntObjectHashMap<List<CallerCluster>> loadMetaClusterToCluster(String uniprotId)
	{
		ru.biosoft.access.core.DataElementPath path = parameters.getMakeMetaTracksOutput().getChildPath( uniprotId, "meta cluster to cluster" );
		log.info( "Loading " + path );
		TIntObjectHashMap<List<CallerCluster>> result = new TIntObjectHashMap<>();
		TableDataCollection relationTable = path.getDataElement( TableDataCollection.class );
		for(RowDataElement r : relationTable)
		{
			Object[] values = r.getValues();
			Integer metaClusterId = (Integer)values[0];
			Integer clusterId = (Integer)values[1];
			String caller = (String)values[2];
			CallerCluster cc = new CallerCluster( Caller.valueOf( caller ), clusterId );
			List<CallerCluster> list = result.get( metaClusterId );
			if(list == null)
				result.put( metaClusterId, list = new ArrayList<>() );
			list.add( cc );
		}
		return result;
	}

	private static class CallerCluster {
		public final Caller caller;
		public final int clusterId;
		public CallerCluster(Caller caller, int clusterId)
		{
			this.caller = caller;
			this.clusterId = clusterId;
		}
	}

	public static class Parameters extends AbstractAnalysisParameters
	{
		private DataElementPath experiments;
		public DataElementPath getExperiments()
		{
			return experiments;
		}
		public void setExperiments(DataElementPath experiments)
		{
			Object oldValue = this.experiments;
			this.experiments = experiments;
			firePropertyChange( "experiments", oldValue, experiments );
		}

		private DataElementPath clustersPath;
		public DataElementPath getClustersPath()
		{
			return clustersPath;
		}
		public void setClustersPath(DataElementPath clustersPath)
		{
			Object oldValue = this.clustersPath;
			this.clustersPath = clustersPath;
			firePropertyChange( "clustersPath", oldValue, clustersPath );
		}

		private String clusterTypeName = "MACS2";
		public String getClusterTypeName()
		{
			return clusterTypeName;
		}
		public void setClusterTypeName(String name)
		{
			Object oldValue = this.clusterTypeName;
			this.clusterTypeName = name;
			firePropertyChange( "clusterTypeName", oldValue, name );
		}

		private DataElementPath makeMetaTracksOutput;
		public DataElementPath getMakeMetaTracksOutput()
		{
			return makeMetaTracksOutput;
		}
		public void setMakeMetaTracksOutput(DataElementPath makeMetaTracksOutput)
		{
			Object oldValue = this.makeMetaTracksOutput;
			this.makeMetaTracksOutput = makeMetaTracksOutput;
			firePropertyChange( "makeMetaTracksOutput", oldValue, makeMetaTracksOutput );
		}

		private DataElementPath result;
		public DataElementPath getResult()
		{
			return result;
		}
		public void setResult(DataElementPath result)
		{
			Object oldValue = this.result;
			this.result = result;
			firePropertyChange( "result", oldValue, result );
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
			add( DataElementPathEditor.registerInputChild( "experiments", beanClass, ChIPseqExperiment.class ) );
			property("clustersPath").inputElement( SqlTrack.class ).add();
			property("clusterTypeName").tags( t->StreamEx.of( Caller.values() ).map( Caller::name ).append( "meta" ) ).add();
			property("makeMetaTracksOutput").inputElement( FolderCollection.class ).add();
			property("result").outputElement( TableDataCollection.class ).add();
		}
	}
}
