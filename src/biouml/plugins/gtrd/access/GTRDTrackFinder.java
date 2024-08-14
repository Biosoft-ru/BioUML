package biouml.plugins.gtrd.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPexoExperimentSQLTransformer;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.analysis.CellsFiltering.NodeInfo;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.finder.TrackFinder;
import ru.biosoft.bsa.finder.TrackSummary;

public class GTRDTrackFinder extends TrackFinder
{
    private static final Logger log = Logger.getLogger( GTRDTrackFinder.class.getName() );

    public GTRDTrackFinder(DataElementPath databasePath)
    {
        super( databasePath );
    }

    private List<ru.biosoft.access.core.DataElementPath> supportedGenomes;
    @Override
    public synchronized List<ru.biosoft.access.core.DataElementPath> getSupportedGenomes()
    {
        if(supportedGenomes == null)
        {
            supportedGenomes = new ArrayList<>();
            Connection con = SqlConnectionPool.getConnection( databasePath.getDataCollection() );
            List<String> names = SqlUtil.queryStrings( con, "SELECT DISTINCT specie FROM chip_experiments" );
            for(String latinName : names)
            {
                Species s = Species.getSpecies( latinName );
                if(s == null)
                {
                    log.log( Level.WARNING, "Can not find species: " + latinName );
                    continue;
                }
                String path = s.getAttributes().getValueAsString(TrackUtils.ENSEMBL_PATH_PROPERTY);
                if(path == null)
                {
                    log.log( Level.WARNING, "Can not find ensembl for: " + latinName );
                    continue;
                }
                DataElementPath ensemblPath = DataElementPath.create( path );
                DataElementPath genomePath;
                try
                {
                    genomePath = TrackUtils.getPrimarySequencesPath( ensemblPath );
                }
                catch( Exception e )
                {
                    log.log( Level.WARNING, "Can not find genome for: " + ensemblPath, e );
                    continue;
                }
                supportedGenomes.add( genomePath );
            }
            Collections.sort( supportedGenomes );
        }
        return supportedGenomes;
    }
    
    static final String ALL_STRING_OPTION = "all";
    private static final String DATA_TYPE_CHIP_SEQ_ALIGNMENTS = "chip-seq alignments";
    private static final String DATA_TYPE_CHIP_SEQ_PEAKS = "chip-seq peaks";
    private static final String DATA_TYPE_CHIP_SEQ_META_CLUSTERS = "chip-seq meta-clusters";
    private static final String DATA_TYPE_CHIP_SEQ_NEW_META_CLUSTERS = "new meta-clusters (rank aggregation)";
    public static final String DATA_TYPE_UNMAPPABLE_REGION = "unmappable regions";
    static final String DATA_TYPE_DNASE_OPEN_CHROMATIN_MACS2 = "Open chromatin (DNase-seq; macs2)";
    static final String DATA_TYPE_DNASE_OPEN_CHROMATIN_HOTSPOT2 = "Open chromatin (DNase-seq; hotspot2)";
    static final String DATA_TYPE_ATAC_OPEN_CHROMATIN_MACS2 = "Open chromatin (ATAC-seq; macs2)";
    static final String DATA_TYPE_FAIRE_OPEN_CHROMATIN_MACS2 = "Open chromatin (FAIRE-seq; macs2)";
    static final String DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 = "DNase footprints (wellington_macs2)";
    static final String DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 = "DNase footprints (wellington_hotspot2)";
    static final String DATA_TYPE_HOCOMOCO_STRONG_SITES = "HOCOMOCO (strong sites)";
    static final String DATA_TYPE_HOCOMOCO_WEAK_SITES = "HOCOMOCO (strong & weak sites)";
    static final String DATA_TYPE_HISTONE_MODIFICATIONS = "Histone Modifications (macs2)";
    static final String DATA_TYPE_CHIPEXO_PEAKS_GEM = "ChIP-exo peaks (gem)";
    static final String DATA_TYPE_CHIPEXO_PEAKS_PEAKZILLA = "ChIP-exo peaks (peakzilla)";
    static final String DATA_TYPE_MNASE_NUCLEOSOMES = "MNase-seq nucleosomes (danpos2)";
    public static final String[] DATA_TYPES = {
            DATA_TYPE_CHIP_SEQ_ALIGNMENTS,
            DATA_TYPE_CHIP_SEQ_PEAKS,
            DATA_TYPE_CHIP_SEQ_META_CLUSTERS,
            DATA_TYPE_CHIP_SEQ_NEW_META_CLUSTERS,
            DATA_TYPE_CHIPEXO_PEAKS_GEM,
            DATA_TYPE_CHIPEXO_PEAKS_PEAKZILLA,
            DATA_TYPE_HISTONE_MODIFICATIONS,
            DATA_TYPE_DNASE_OPEN_CHROMATIN_MACS2,
            DATA_TYPE_DNASE_OPEN_CHROMATIN_HOTSPOT2,
            DATA_TYPE_ATAC_OPEN_CHROMATIN_MACS2,
            DATA_TYPE_FAIRE_OPEN_CHROMATIN_MACS2,
            DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2,
            DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2,
            DATA_TYPE_MNASE_NUCLEOSOMES,
            DATA_TYPE_HOCOMOCO_STRONG_SITES,
            DATA_TYPE_HOCOMOCO_WEAK_SITES,
            DATA_TYPE_UNMAPPABLE_REGION };
    
    private String dataType = DATA_TYPE_CHIP_SEQ_PEAKS;
    
    private DataElementPath pathToSource = DataElementPath.create( "databases/GTRD/Dictionaries/source" );    
    public DataElementPath getPathToSource() {
		return pathToSource;
	}
	
	@PropertyName("Data type")
    public String getDataType()
    {
        return dataType;
    }
    public void setDataType(String dataType)
    {
        this.dataType = dataType;
        
        getCellSource().setName( ALL_STRING_OPTION );
        getCellName().setName( ALL_STRING_OPTION );
        setTranscriptionFactor( ALL_STRING_OPTION );
        setDnaseCellName( ALL_STRING_OPTION );
    }
    
    public boolean isEmptyLevel(DataElementPath choosedPath) 
    {

		int choosed = 0;
		if (choosedPath != null)
		{
			ru.biosoft.access.core.DataElementPath[] childrens = choosedPath.getChildrenArray();
			for (DataElementPath el : childrens)
			{
				String elId = el.getName();
				if (!elId.startsWith("GTRD"))
					choosed++;
			}

			if (choosed == 0)
				return true; 

			else 
				return false;
		}
		else
			return true;
	}
    
    private NodeInfo cellClusters =  new NodeInfo(ALL_STRING_OPTION, null, null);
    @PropertyName("Clusters")
    
	public NodeInfo getCellClusters() {
		return cellClusters;
	}

	public void setCellClusters(NodeInfo cellClusters) 
	{
		if ( this.cellClusters.getName() != cellClusters.getName() )
		{
			cellSource = new NodeInfo(ALL_STRING_OPTION, null, null);
			cellSourceLvl2 = new NodeInfo(ALL_STRING_OPTION, null, null);
			cellName = new NodeInfo(ALL_STRING_OPTION, null, null);
		}
				
		this.cellClusters = cellClusters;
		
	}
	public boolean isCellClusterHidden() 
	{
		if ( isCellHidden() )
			return true;
		
		else
			return false;
		
	}
	
	private NodeInfo cellSource =  new NodeInfo(ALL_STRING_OPTION, null, null);
	@PropertyName("Cell source")
    public NodeInfo getCellSource()
    {
        return cellSource;
    }
    public void setCellSource(NodeInfo cellSource)
    {
    	if ( this.cellSource.getName() != cellSource.getName() )
		{
			cellSourceLvl2 = new NodeInfo(ALL_STRING_OPTION, null, null);
			cellName = new NodeInfo(ALL_STRING_OPTION, null, null);
		}
    	this.cellSource = cellSource;
    }

    public boolean isCellSourceHidden()
    {
    	if ( isCellClusterHidden() )
    		return true;
    	
    	else
    	{
    		ru.biosoft.access.core.DataElementPath path = getCellClusters().getPath();
    		if ( path != null )
    		{
    			if ( isEmptyLevel(path) )
    				return true;
    			else
    				return false;
    			
    		}
    		else
    			return true;
    	}
    	
    }
    
    private NodeInfo cellSourceLvl2 =  new NodeInfo(ALL_STRING_OPTION, null, null);
    @PropertyName("Cell source level 2")
    
    public NodeInfo getCellSourceLvl2() {
		return cellSourceLvl2;
	}

	public void setCellSourceLvl2(NodeInfo cellSourceLvl2) 
	{
		
		if ( this.cellSourceLvl2.getName() != cellSourceLvl2.getName() )
		{
			cellName = new NodeInfo(ALL_STRING_OPTION, null, null);
		}
		this.cellSourceLvl2 = cellSourceLvl2;
	}
	
	 public boolean isCellSourceLvl2Hidden()
	    {
	    	if ( isCellSourceHidden() )
	    		return true;
	    	
	    	else
	    	{
	    		ru.biosoft.access.core.DataElementPath path = getCellSource().getPath();
	    		if ( path != null )
	    		{
	    			if ( isEmptyLevel(path) )
	    				return true;
	    			else
	    				return false;
	    			
	    		}
	    		else
	    			return true;
	    	}
	    	
	    }

    
    private NodeInfo cellName =  new NodeInfo(ALL_STRING_OPTION, null, null);
    
	@PropertyName("Cell type")
    public NodeInfo getCellName()
    {
        return cellName;
    }
    public void setCellName(NodeInfo cellName)
    {
        this.cellName = cellName;
    }
    
    public boolean isCellHidden()
    {
        if(dataType.equals( DATA_TYPE_CHIP_SEQ_ALIGNMENTS )) return false;
        if(dataType.equals( DATA_TYPE_CHIP_SEQ_PEAKS )) return false;
        return true;
    }
    
    private String dnaseCellName = ALL_STRING_OPTION;
    @PropertyName ( "Cell name" )
    public String getDnaseCellName()
    {
        return dnaseCellName;
    }
    public void setDnaseCellName(String dnaseCellName)
    {
        this.dnaseCellName = dnaseCellName;
    }
    
    public boolean isDnaseCellHidden()
    {
        if(dataType.equals( DATA_TYPE_DNASE_OPEN_CHROMATIN_MACS2 )) return false;
        if(dataType.equals( DATA_TYPE_DNASE_OPEN_CHROMATIN_HOTSPOT2 )) return false;
        if(dataType.equals( DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2 )) return false;
        if(dataType.equals( DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2 )) return false;
        return true;
    }

    private String chipexoCellName = ALL_STRING_OPTION;
    @PropertyName ( "Cell name" )
    public String getChipexoCellName()
    {
        return chipexoCellName;
    }
    public void setChipexoCellName(String chipexoCellName)
    {
        this.chipexoCellName = chipexoCellName;
    }
    
    public boolean isChipexoCellHidden()
    {
        if(dataType.equals( DATA_TYPE_CHIPEXO_PEAKS_GEM )) return false;
        if(dataType.equals( DATA_TYPE_CHIPEXO_PEAKS_PEAKZILLA )) return false;
        return true;
    }
    
    private String histCellName = ALL_STRING_OPTION;
    @PropertyName ( "Cell name" )
    public String getHistCellName()
    {
        return histCellName;
    }
    public void setHistCellName(String histCellName)
    {
        this.histCellName = histCellName;
    }
    
    public boolean isHistCellHidden()
    {
        if(dataType.equals( DATA_TYPE_HISTONE_MODIFICATIONS )) return false;
        return true;
    }
    
    private String mnaseCellName = ALL_STRING_OPTION;
    @PropertyName ( "Cell name" )
    public String getMnaseCellName()
    {
        return mnaseCellName;
    }
    public void setMnaseCellName(String mnaseCellName)
    {
        this.mnaseCellName = mnaseCellName;
    }
    
    public boolean isMnaseCellHidden()
    {
        if(dataType.equals( DATA_TYPE_MNASE_NUCLEOSOMES )) return false;
        return true;
    }
    
    private String atacCellName = ALL_STRING_OPTION;
    @PropertyName ( "Cell name" )
    public String getATACseqCellName()
    {
        return atacCellName;
    }
    public void setATACseqCellName(String atacCellName)
    {
        this.atacCellName = atacCellName;
    }
    
    public boolean isATACseqCellHidden()
    {
        if(dataType.equals( DATA_TYPE_ATAC_OPEN_CHROMATIN_MACS2 )) return false;
        return true;
    }
    
    private String faireCellName = ALL_STRING_OPTION;
    @PropertyName ( "Cell name" )
    public String getFAIREseqCellName()
    {
        return faireCellName;
    }
    public void setFAIREseqCellName(String faireCellName)
    {
        this.faireCellName = faireCellName;
    }
    
    public boolean isFAIREseqCellHidden()
    {
        if(dataType.equals( DATA_TYPE_FAIRE_OPEN_CHROMATIN_MACS2 )) return false;
        return true;
    }
    
    private String transcriptionFactor = ALL_STRING_OPTION;
    @PropertyName("Transcription factor/cofactor")
    public String getTranscriptionFactor()
    {
        return transcriptionFactor;
    }
    public String getTFUniprotId()
    {
        return ( transcriptionFactor == null || ALL_STRING_OPTION.equals( transcriptionFactor ) ) ? null
                : transcriptionFactor.split( " " )[1];
    }
    public void setTranscriptionFactor(String transcriptionFactor)
    {
        this.transcriptionFactor = transcriptionFactor;
    }
    public boolean isTranscriptionFactorHidden()
    {
        if(dataType.equals( DATA_TYPE_CHIP_SEQ_ALIGNMENTS )) return false;
        if(dataType.equals( DATA_TYPE_CHIP_SEQ_PEAKS )) return false;
        if(dataType.equals( DATA_TYPE_CHIP_SEQ_META_CLUSTERS )) return false;
        if(dataType.equals( DATA_TYPE_CHIP_SEQ_NEW_META_CLUSTERS )) return false;
        return true;
    }
    public boolean isTranscriptionFactorChIPexoHidden()
    {
        if(dataType.equals( DATA_TYPE_CHIPEXO_PEAKS_GEM )) return false;
        if(dataType.equals( DATA_TYPE_CHIPEXO_PEAKS_PEAKZILLA )) return false;
        return true;
    }
    @Override
    public DataCollection<? extends TrackSummary> findTracks()
    {
        DataCollection<GTRDTrackSummary> result = new VectorDataCollection<>( "" );
        switch(dataType)
        {
            case DATA_TYPE_CHIP_SEQ_ALIGNMENTS:
                findChipSeqAligns(result);
                break;
            case DATA_TYPE_CHIP_SEQ_PEAKS:
                findChipSeqPeaks(result);
                break;
            case DATA_TYPE_CHIP_SEQ_META_CLUSTERS:
                findChipSeqMetaclusters(result);
                break;
            case DATA_TYPE_CHIP_SEQ_NEW_META_CLUSTERS:
                findChipSeqNewMetaclusters(result);
                break;
            case DATA_TYPE_UNMAPPABLE_REGION:
                findUnmappableTracks( result );
                break;
            case DATA_TYPE_CHIPEXO_PEAKS_GEM:
            case DATA_TYPE_CHIPEXO_PEAKS_PEAKZILLA:
            	findChipexoPeaks( result, getPeakCallerByDataType( dataType ) );
            	break;
            case DATA_TYPE_DNASE_OPEN_CHROMATIN_MACS2:
            case DATA_TYPE_DNASE_OPEN_CHROMATIN_HOTSPOT2:
            case DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2:
            case DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2:
                findDNaseseq( result, getPeakCallerByDataType( dataType ) );
                break;
            case DATA_TYPE_ATAC_OPEN_CHROMATIN_MACS2:
            	findATACseqPeaks( result, "macs2" );
                break;
            case DATA_TYPE_FAIRE_OPEN_CHROMATIN_MACS2:
            	findFAIREseqPeaks( result, "macs2" );
                break;
            case DATA_TYPE_HISTONE_MODIFICATIONS:
            	findHistPeaks( result, "macs2" );
                break;
            case DATA_TYPE_MNASE_NUCLEOSOMES:
            	findMNasePeaks( result, "danpos2" );
                break;
            case DATA_TYPE_HOCOMOCO_STRONG_SITES:
                findHocomoco( result, true );
                break;
            case DATA_TYPE_HOCOMOCO_WEAK_SITES:
                findHocomoco( result, false );
                break;
            default:
                log.warning( "Unhandled dataType: " + dataType );
        }
        return result;
    }
    
    private void findChipSeqAligns(DataCollection<GTRDTrackSummary> result)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;
        
        boolean hasCellSourse = cellClusters.getName() != null && !ALL_STRING_OPTION.equals( cellClusters.getName() );
        boolean hasCellName = cellName.getName() != null && !ALL_STRING_OPTION.equals( cellName.getName() );
        boolean hasTF = transcriptionFactor != null && !ALL_STRING_OPTION.equals( transcriptionFactor );

        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        //TODO: rework
        StringBuilder sb = new StringBuilder( "SELECT DISTINCT hub.input FROM chip_experiments ce" )
                .append( " join hub on (output=ce.id and hub.output_type='ExperimentGTRDType' and hub.input_type='AlignmentsGTRDType')" )
                .append( " join cells on (ce.cell_id=cells.id)" )
                .append( " where " );
        
        if( hasCellName )
            sb.append( "cells.id=? and " );
        else if( hasCellSourse )
            sb.append( "cells.source_id like ? and " );
        if( hasTF )
            sb.append( "ce.tf_uniprot_id=? and " );
        sb.append( "ce.specie=?" );
        String query = sb.toString();
        
        List<String> alignIdList = new ArrayList<>();
        try(PreparedStatement ps = con.prepareStatement( query ))
        {
            int i = 1;
            if( hasCellName )
                ps.setString( i++, cellName.getId() );
            else if( hasCellSourse )
            {
                NodeInfo lvl = checkSourceLevel();
            	ps.setString( i++, "%" + lvl.getId() + "%" );
            }
            
            if( hasTF )
                ps.setString( i++, getTFUniprotId() );
            ps.setString( i, organism );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                {
                    String id = rs.getString( 1 );
                    alignIdList.add( id );
                }
            }
        }
        catch( SQLException e )
        {
            throw new RuntimeException(e);
        }

        for( String id : alignIdList )
        {
            DataElementPath trackPath = databasePath.getChildPath( "Data", "alignments", id + ".bam" );
            if( trackPath.exists() )
            {
                Track t = trackPath.getDataElement( Track.class );
                GTRDTrackSummary ts = new GTRDTrackSummary( t );
                ts.setSize( t.getAllSites().getSize() );
                result.put( ts );
            }
        }
    }
    
    private void findChipSeqNewMetaclusters(DataCollection<GTRDTrackSummary> result)
    {
        String tf = getTFUniprotId();
        String organism = getSpeciesLatinName();
        if(tf == null || organism == null)
            return;
        DataElementPath trackPath = databasePath.getChildPath( "Data", "new_clusters", organism, "By TF", tf);
        if(trackPath.exists())
        {
            Track track = trackPath.getDataElement( Track.class );
            GTRDTrackSummary ts = new GTRDTrackSummary( track );
            ts.setSize( track.getAllSites().getSize() );
            result.put( ts );
        }
    }
    
    private void findChipSeqMetaclusters(DataCollection<GTRDTrackSummary> result)
    {
        String tf = getTFUniprotId();
        String organism = getSpeciesLatinName();
        if(tf == null || organism == null)
            return;
        DataElementPath trackPath = databasePath.getChildPath( "Data", "clusters", organism, "By TF", tf, "meta clusters" );
        if(trackPath.exists())
        {
            Track track = trackPath.getDataElement( Track.class );
            GTRDTrackSummary ts = new GTRDTrackSummary( track );
            ts.setSize( track.getAllSites().getSize() );
            result.put( ts );
        }
    }

    private void findChipSeqPeaks(DataCollection<GTRDTrackSummary> result)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;
        DataElementPath expDC = DataElementPath.create( getDatabasePath() + "/Data/experiments" );
        
        boolean hasCellCluster = cellClusters.getName() != null && !ALL_STRING_OPTION.equals( cellClusters.getName() );
        boolean hasCellName = cellName.getName() != null && !ALL_STRING_OPTION.equals( cellName.getName() );
        boolean hasTF = transcriptionFactor != null && !ALL_STRING_OPTION.equals( transcriptionFactor );

        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        //TODO: rework
        StringBuilder sb = new StringBuilder( "SELECT DISTINCT hub.output FROM chip_experiments ce" )
                .append( " join hub on (output=ce.id and hub.output_type='ExperimentGTRDType' and hub.input_type='PeaksGTRDType')" )
                .append( " join cells on (ce.cell_id=cells.id)" )
                .append( " join peaks_finished pf on(pf.exp_id=ce.id)" )
                .append( " where " );
        
        if( hasCellName )
            sb.append( "cells.id=? and " );
        else if( hasCellCluster )
            sb.append( "cells.source like ? and " );
        if( hasTF )
            sb.append( "ce.tf_uniprot_id=? and " );
        sb.append( "ce.specie=?" );
        String query = sb.toString();
        
        List<ChIPseqExperiment> expList = new ArrayList<>();
        try(PreparedStatement ps = con.prepareStatement( query ))
        {
            int i = 1;
            
            if( hasCellName )
                ps.setString( i++, cellName.getId() );
            else if( hasCellCluster )
			{
            	NodeInfo lvl = checkSourceLevel(); 
                ps.setString( i++, "%" + lvl.getId() + "%" );
			}
            if( hasTF )
                ps.setString( i++, getTFUniprotId() );
            ps.setString( i, organism );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                {
                    String expId = rs.getString( 1 );
                    ChIPseqExperiment exp = expDC.getChildPath( expId ).getDataElement( ChIPseqExperiment.class );
                    expList.add( exp );
                }
            }
        }
        catch( SQLException e )
        {
            throw new RuntimeException(e);
        }

        for(String peakCaller : new String[] {"gem", "macs2", "sissrs", "pics"})
        {
            for(ChIPseqExperiment exp : expList)
            {
            	String id = exp.getPeakId();
                DataElementPath trackPath = databasePath.getChildPath( "Data", "peaks", peakCaller, id );
                if(trackPath.exists())
                {
                    Track t = trackPath.getDataElement( Track.class );
                    GTRDTrackSummary ts = new GTRDTrackSummary( t, peakCaller + "/" +id, !hasTF, true, !hasCellName );
                    if( hasCellName )
                    	ts.setCellName( exp.getCell().getTitle() );
                    if( hasTF )
                    	ts.setTarget( exp.getTfUniprotId() + ": " + exp.getTfTitle() );
                    ts.setTreatment( exp.getTreatment() );
                    ts.setSize( t.getAllSites().getSize() );
                    result.put(ts);
                }
            }
        }
    }
    
    private void findChipexoPeaks(DataCollection<GTRDTrackSummary> result, String peakCaller)
    {
        String organism = getSpeciesLatinName();
        String tf = getTFUniprotId();
        DataElementPath expDC = DataElementPath.create( getDatabasePath() + "/Data/ChIP-exo experiments" );
        
        boolean hasTF = tf != null && !ALL_STRING_OPTION.equals( tf );
        boolean hasCellName = chipexoCellName != null && !ALL_STRING_OPTION.equals( chipexoCellName );
        
        if( organism == null )
            return;
        
        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        //TODO: rework
        StringBuilder sb = new StringBuilder( "SELECT DISTINCT hub.output FROM chipexo_experiments ce" )
                .append( " join hub on (output=ce.id and hub.output_type='ChIPexoExperiment' and hub.input_type='ChIPexoPeaks')" )
                .append( " join cells on (ce.cell_id=cells.id)" )
                .append( " join chipexo_peaks_finished pf on(pf.exp_id=ce.id)" )
                .append( " where " );
        
        if( hasCellName )
            sb.append( "cells.id=? and " );
        if( hasTF )
            sb.append( "ce.tf_uniprot_id=? and " );
        sb.append( "ce.specie=?" );
        String query = sb.toString();
        
        List<ChIPexoExperiment> expList = new ArrayList<>();
        try(PreparedStatement ps = con.prepareStatement( query ))
        {
            int i = 1;
            
            if( hasCellName )
                ps.setString( i++, chipexoCellName );
            if( hasTF )
                ps.setString( i++, getTFUniprotId() );
            ps.setString( i, organism );
            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                {
                    String expId = rs.getString( 1 );
                    ChIPexoExperiment exp = expDC.getChildPath( expId ).getDataElement( ChIPexoExperiment.class );
                    expList.add( exp );
                }
            }
        }
        catch( SQLException e )
        {
        	throw new RuntimeException(e);
        }

        for(ChIPexoExperiment exp : expList)
        {
        	String id = exp.getPeakId();
        	ru.biosoft.access.core.DataElementPath trackPath = DataElementPath.create(ChIPexoExperimentSQLTransformer.DEFAULT_GTRD_PEAKS, peakCaller, id);
        	if(trackPath.exists())
        	{
        		Track t = trackPath.getDataElement( Track.class );
        		GTRDTrackSummary ts = new GTRDTrackSummary( t, peakCaller + "/" + id, !hasTF, true, !hasCellName );
        		if( hasCellName )
        			ts.setCellName( exp.getCell().getTitle() );
        		ts.setTarget( exp.getTfUniprotId() + ": " + exp.getTfTitle() );
        		ts.setTreatment( exp.getTreatment() );
        		ts.setSize( t.getAllSites().getSize() );
        		result.put(ts);
        	}
        }
    }
    
    public NodeInfo checkSourceLevel()
    {
    	if ( cellSourceLvl2.getPath() != null )
    		return cellSourceLvl2;
    	else if ( cellSource.getPath() != null)
    		return cellSource;
    	else
    		return cellClusters;
    }
    
    private void findDNaseseq(DataCollection<GTRDTrackSummary> result, String peakType)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;

        boolean hasCellName = dnaseCellName != null && !ALL_STRING_OPTION.equals( dnaseCellName );

        DataElementPath expDC = DataElementPath.create( getDatabasePath() + "/Data/DNase experiments" );
        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        StringBuilder sb = new StringBuilder( "SELECT e.id FROM dnase_experiments e join cells on(e.cell_id=cells.id)" )
                .append( " JOIN dnase_peaks_finished ON(e.id=exp_id AND peak_type=?)" )
                .append( " WHERE organism=?" );

        if( hasCellName )
            sb.append( " AND cells.title=?" );
        sb.append( " ORDER BY 1" );

        String query = sb.toString();
        List<String> experimentIdList = new ArrayList<>();
        try( PreparedStatement ps = con.prepareStatement( query ) )
        {
            int i = 1;
            ps.setString( i++, peakType );
            ps.setString( i++, organism );
            if( hasCellName )
                ps.setString( i, dnaseCellName );

            try( ResultSet rs = ps.executeQuery() )
            {
                while( rs.next() )
                {
                    String id = rs.getString( 1 );
                    experimentIdList.add( id );
                }
            }
        }
        catch( SQLException e )
        {
            throw new RuntimeException( e );
        }

        for( String expId : experimentIdList )
        {
            DNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( DNaseExperiment.class );
            for( DataElementPath trackPath : exp.getPeaksByPeakCaller( peakType ) )
            {
                if( trackPath.exists() )
                {
                    Track t = trackPath.getDataElement( Track.class );
                    GTRDTrackSummary ts = new GTRDTrackSummary( t, false, true, hasCellName );
            		if( hasCellName )
            			ts.setCellName( exp.getCell().getTitle() );
            		ts.setTreatment( exp.getTreatment() );
                    ts.setSize( t.getAllSites().getSize() );
                    result.put( ts );
                }
            }
        }
    }

    private void findMNasePeaks(DataCollection<GTRDTrackSummary> result, String peakType)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;

        boolean hasCellName = mnaseCellName != null && !ALL_STRING_OPTION.equals( mnaseCellName );

        DataElementPath expDC = DataElementPath.create( getDatabasePath() + "/Data/MNase-seq experiments" );
        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        StringBuilder sb = new StringBuilder( "SELECT e.id FROM mnase_experiments e join cells on(e.cell_id=cells.id)" )
                .append( " JOIN mnase_peaks_finished ON(e.id=exp_id AND peak_type=?)" )
                .append( " WHERE organism=?" );

        if( hasCellName )
            sb.append( " AND cells.title=?" );
        sb.append( " ORDER BY 1" );

        String query = sb.toString();
        List<String> experimentIdList = new ArrayList<>();
        try( PreparedStatement ps = con.prepareStatement( query ) )
        {
            int i = 1;
            ps.setString( i++, peakType );
            ps.setString( i++, organism );
            if( hasCellName )
                ps.setString( i, mnaseCellName );

            try( ResultSet rs = ps.executeQuery() )
            {
            	while( rs.next() )
            	{
            		String id = rs.getString( 1 );
            		experimentIdList.add( id );
            	}
            }
        }
        catch( SQLException e )
        {
        	throw new RuntimeException( e );
        }

        for( String expId : experimentIdList )
        {
        	MNaseExperiment exp = expDC.getChildPath( expId ).getDataElement( MNaseExperiment.class );
        	ru.biosoft.access.core.DataElementPath trackPath = exp.getPeaksByPeakCaller( peakType ); 

        	if( trackPath.exists() )
        	{
        		Track t = trackPath.getDataElement( Track.class );
        		GTRDTrackSummary ts = new GTRDTrackSummary( t, false, true, !hasCellName );
        		ts.setTreatment( exp.getTreatment() );
        		if( hasCellName )
        			ts.setCellName( exp.getCell().getTitle() );
        		ts.setSize( t.getAllSites().getSize() );
        		result.put( ts );
        	}

        }
    }
    
    private void findATACseqPeaks(DataCollection<GTRDTrackSummary> result, String peakType)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;

        boolean hasCellName = atacCellName != null && !ALL_STRING_OPTION.equals( atacCellName );

        DataElementPath expDC = DataElementPath.create( getDatabasePath() + "/Data/ATAC-seq experiments" );
        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        StringBuilder sb = new StringBuilder( "SELECT e.id FROM atac_experiments e join cells on(e.cell_id=cells.id)" )
                .append( " JOIN atac_peaks_finished ON(e.id=exp_id AND peak_type=?)" )
                .append( " WHERE organism=?" );

        if( hasCellName )
            sb.append( " AND cells.title=?" );
        sb.append( " ORDER BY 1" );

        String query = sb.toString();
        List<String> experimentIdList = new ArrayList<>();
        try( PreparedStatement ps = con.prepareStatement( query ) )
        {
            int i = 1;
            ps.setString( i++, peakType );
            ps.setString( i++, organism );
            if( hasCellName )
                ps.setString( i, atacCellName );

            try( ResultSet rs = ps.executeQuery() )
            {
            	while( rs.next() )
            	{
            		String id = rs.getString( 1 );
            		experimentIdList.add( id );
            	}
            }
        }
        catch( SQLException e )
        {
        	throw new RuntimeException( e );
        }

        for( String expId : experimentIdList )
        {
        	ATACExperiment exp = expDC.getChildPath( expId ).getDataElement( ATACExperiment.class );
        	ru.biosoft.access.core.DataElementPath trackPath = exp.getPeaksByPeakCaller( peakType ); 

        	if( trackPath.exists() )
        	{
        		Track t = trackPath.getDataElement( Track.class );
        		GTRDTrackSummary ts = new GTRDTrackSummary( t, false, true, !hasCellName );
        		ts.setTreatment( exp.getTreatment() );
        		if( hasCellName )
        			ts.setCellName( exp.getCell().getTitle() );
        		ts.setSize( t.getAllSites().getSize() );
        		result.put( ts );
        	}

        }
    }
    
    private void findFAIREseqPeaks(DataCollection<GTRDTrackSummary> result, String peakType)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;

        boolean hasCellName = faireCellName != null && !ALL_STRING_OPTION.equals( faireCellName );

        DataElementPath expDC = DataElementPath.create( getDatabasePath() + "/Data/FAIRE-seq experiments" );
        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        StringBuilder sb = new StringBuilder( "SELECT e.id FROM faire_experiments e join cells on(e.cell_id=cells.id)" )
                .append( " JOIN faire_peaks_finished ON(e.id=exp_id AND peak_type=?)" )
                .append( " WHERE organism=?" );

        if( hasCellName )
            sb.append( " AND cells.title=?" );
        sb.append( " ORDER BY 1" );

        String query = sb.toString();
        List<String> experimentIdList = new ArrayList<>();
        try( PreparedStatement ps = con.prepareStatement( query ) )
        {
            int i = 1;
            ps.setString( i++, peakType );
            ps.setString( i++, organism );
            if( hasCellName )
                ps.setString( i, faireCellName );

            try( ResultSet rs = ps.executeQuery() )
            {
            	while( rs.next() )
            	{
            		String id = rs.getString( 1 );
            		experimentIdList.add( id );
            	}
            }
        }
        catch( SQLException e )
        {
        	throw new RuntimeException( e );
        }

        for( String expId : experimentIdList )
        {
        	FAIREExperiment exp = expDC.getChildPath( expId ).getDataElement( FAIREExperiment.class );
        	ru.biosoft.access.core.DataElementPath trackPath = exp.getPeaksByPeakCaller( peakType ); 

        	if( trackPath.exists() )
        	{
        		Track t = trackPath.getDataElement( Track.class );
        		GTRDTrackSummary ts = new GTRDTrackSummary( t, false, true, !hasCellName );
        		ts.setTreatment( exp.getTreatment() );
        		if( hasCellName )
        			ts.setCellName( exp.getCell().getTitle() );
        		ts.setSize( t.getAllSites().getSize() );
        		result.put( ts );
        	}

        }
    }
    
    private void findHistPeaks(DataCollection<GTRDTrackSummary> result, String peakType)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;

        boolean hasCellName = histCellName != null && !ALL_STRING_OPTION.equals( histCellName );

        DataElementPath expDC = DataElementPath.create( getDatabasePath() + "/Data/ChIP-seq HM experiments" );
        Connection con = DataCollectionUtils.getSqlConnection( getDatabasePath().getDataElement() );

        StringBuilder sb = new StringBuilder( "SELECT e.id FROM hist_experiments e join cells on(e.cell_id=cells.id)" )
                .append( " JOIN hist_peaks_finished ON(e.id=exp_id AND peak_type=?)" )
                .append( " WHERE specie=?" );

        if( hasCellName )
            sb.append( " AND cells.title=?" );
        sb.append( " ORDER BY 1" );

        String query = sb.toString();
        List<String> experimentIdList = new ArrayList<>();
        try( PreparedStatement ps = con.prepareStatement( query ) )
        {
        	int i = 1;
        	ps.setString( i++, peakType );
        	ps.setString( i++, organism );
        	if( hasCellName )
        		ps.setString( i, histCellName );

        	try( ResultSet rs = ps.executeQuery() )
        	{
        		while( rs.next() )
        		{
        			String id = rs.getString( 1 );
        			experimentIdList.add( id );
        		}
        	}
        }
        catch( SQLException e )
        {
        	throw new RuntimeException( e );
        }

        for( String expId : experimentIdList )
        {
        	HistonesExperiment exp = expDC.getChildPath( expId ).getDataElement( HistonesExperiment.class );
        	if( exp.isControlExperiment() )
        		continue;
        	ru.biosoft.access.core.DataElementPath trackPath = exp.getPeakByPeakCaller( peakType );

        	if( trackPath.exists() )
        	{
        		Track t = trackPath.getDataElement( Track.class );
        		GTRDTrackSummary ts = new GTRDTrackSummary( t, true, true, !hasCellName );
        		if( hasCellName )
        			ts.setCellName( exp.getCell().getTitle() );
        		ts.setTarget( exp.getTarget() );
        		ts.setTreatment( exp.getTreatment() );
        		ts.setSize( t.getAllSites().getSize() );
        		result.put( ts );
        	}
        }
    }

    static String getPeakCallerByDataType(String type)
    {
    	if( type == null )
            return "";
        switch(type)
        {
            case DATA_TYPE_DNASE_OPEN_CHROMATIN_MACS2:
                return "macs2";
            case DATA_TYPE_DNASE_OPEN_CHROMATIN_HOTSPOT2:
                return "hotspot2";
            case DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_MACS2:
                return "wellington_macs2";
            case DATA_TYPE_DNASE_FOOTPRINTS_WELLINGTON_HOTSPOT2:
                return "wellington_hotspot2";
            case DATA_TYPE_CHIPEXO_PEAKS_GEM:
                return "gem";
            case DATA_TYPE_CHIPEXO_PEAKS_PEAKZILLA:
                return "peakzilla";
            default:
                return "";
        }
    }
    
    private void findHocomoco(DataCollection<GTRDTrackSummary> result, boolean strongSites)
    {
        String organism = getSpeciesLatinName();
        if( organism == null )
            return;

        Species species = Species.getSpecies( organism );
        String name = species.getCommonName().toLowerCase() + "_hocomoco_v11_pval=" + ( strongSites ? "0.0001" : "0.001" );
        DataElementPath path = DataElementPath.create( getDatabasePath() + "/Data/generic/predicted sites/" + name );
        if( !path.exists() )
            return;

        Track t = path.getDataElement( Track.class );
        GTRDTrackSummary ts = new GTRDTrackSummary( t );
        ts.setSize( t.getAllSites().getSize() );
        result.put( ts );
    }

    private void findUnmappableTracks(DataCollection<GTRDTrackSummary> result)
    {
        String latinName = getSpeciesLatinName();
        if(latinName == null)
        {
            log.warning( "Can not find species latin name for " + genome );
            return;
        }
        DataElementPath trackPath = DataElementPath.create( databasePath + "/Data/generic/mappability", latinName + " unmappable 50"  );
        if(!trackPath.exists())
            return;
        Track track = trackPath.getDataElement( Track.class );
        GTRDTrackSummary ts = new GTRDTrackSummary( track );
        ts.setSize( track.getAllSites().getSize() );
        result.put( ts );
    }
    
    public String getSpeciesLatinName()
    {
        String[] parts = getGenome().getPathComponents();
        if(parts.length < 2)
            return null;
        DataElementPath ensemblDBPath = DataElementPath.create( parts[0], parts[1] );
        return ensemblDBPath.getDataCollection().getInfo().getProperty(DataCollectionUtils.SPECIES_PROPERTY);
    }

}
