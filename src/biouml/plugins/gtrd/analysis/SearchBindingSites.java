package biouml.plugins.gtrd.analysis;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.standard.type.Gene;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.DefaultReferenceType;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysis.FakeProgress;
import ru.biosoft.analysis.type.GeneTableType;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Position;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WithSite;
import ru.biosoft.bsa.exporter.IntervalTrackExporter;
import ru.biosoft.table.SqlTableDataCollection;

public class SearchBindingSites extends SearchByRegulation<SearchBindingSites.Parameters>
{
    public SearchBindingSites(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        FakeProgress progress = new FakeProgress( jobControl, 10000 );
        progress.start();
        try
        {
            Track resultingTrack = searchBindingSites();
            if(parameters.getOutputType().equals( Parameters.OUTPUT_TYPE_OPEN_IN_GENOME_BROWSER ))
                return resultingTrack;
            return exportTrack(resultingTrack);
        }
        finally
        {
            progress.stop();
        }
    }
    
    private Object exportTrack(Track resultingTrack) throws Exception
    {
        String name = parameters.getDataSet() + "_" + parameters.getOrganism().getCommonName();
        if(!"Any".equals( parameters.getTf() ))
            name += "_" + parameters.getUniprotId();
        name = name + ".interval";
        
        DataElementPath path = parameters.getResultingSites().getSiblingPath( name ).uniq();
        name = path.getName();
        
        DataCollection<? extends DataElement> parent = path.optParentCollection();
        File file = DataCollectionUtils.getChildFile(parent, name);
        FileDataElement result = new FileDataElement(name, parent, file);
        
        IntervalTrackExporter exporter = new IntervalTrackExporter();
        exporter.doExport( resultingTrack, file );
        
        path.save( result );
        path.getParentCollection().release( path.getName() );
        return result;
    }

    private Track searchBindingSites() throws Exception
    {
        initConnection();
        createResultingTrack();
        
        String ensemblGene = getEnsemblGeneID();
        if( !ensemblGene.isEmpty() )
            searchBindingSitesNearGene( ensemblGene );
        else //Search whole genome
        {
            if( parameters.isClusters() )
            {
                String tf = parameters.getTf();
                if( tf.equals( "Any" ) )
                    throw new IllegalArgumentException();//target TF should be set, see validateParameters
                searchClustersByTF( parameters.getUniprotId() );
            }
            else//peaks
            {
                searchPeaks( );
            }
        }
        
        resultingTrack.resetSize();
        if(resultingTrack.getAllSites().isEmpty())
            throw new Exception("No binding sites found");
        
        
        //set default position for genome browser and add additional tracks
        if( ensemblGene.isEmpty() )
            resultingTrack.setDefaultPosition( new Position( findAnySite(resultingTrack) ) );
        else
        {
            Gene gene = TrackUtils.getGenesCollection( parameters.getOrganism(), parameters.getResultingSites() ).get(ensemblGene);
            String chrName = ((WithSite) gene).getSite().getOriginalSequence().getName();
            Interval geneInterval = ((WithSite) gene).getSite().getInterval();
            resultingTrack.setDefaultPosition( new Position( chrName,  geneInterval.zoom( 1.5 )) );
        }
        
        DataElementPath ensemblPath = resultingTrack.getGenomeSelector().getDbSelector().getBasePath();
        DataElementPath genesTrackPath = ensemblPath.getChildPath( "Tracks", "Genes" );
        resultingTrack.getInfo().getProperties().setProperty( Track.OPEN_WITH_TRACKS, genesTrackPath.toString() );
        
        resultingTrackPath.save( resultingTrack );
        resultingTrackPath.getParentCollection().release( resultingTrackPath.getName() );
        return resultingTrackPath.getDataElement( Track.class );
    }

    public Site findAnySite(SqlTrack track)
    {
        for(DataElementPath chr : track.getChromosomesPath().getChildren())
        {
            Sequence seq = chr.getDataElement( AnnotatedSequence.class ).getSequence();
            DataCollection<Site> sites = track.getSites( chr.toString(), 0, seq.getLength() );
            for(Site s : sites)
                return s;
        }
        //not found on common chromosomes
        return track.getAllSites().iterator().next();
    }
    
    private Connection con;
    private DataElementPath resultingTrackPath;
    private SqlTrack resultingTrack;
    private String resultingTrackId;
    
    private void initConnection()
    {
        con = parameters.getPreparedTrack().getConnection();
    }
    
    private void createResultingTrack() throws SQLException
    {
        resultingTrackPath = parameters.getResultingSites().uniq();
        resultingTrack = SqlTrack.createTrack( resultingTrackPath, parameters.getPreparedTrack() );
        resultingTrackId = getTableId( resultingTrack );
    }
    
    //tf,cellLine,treatment can be "Any", but not all at once
    private void searchPeaks() throws Exception
    {
        /*
        if((cellLine.equals( "Any" )) && treatment.equals( "Any" ))
        {
            searchPeaksByTF(tf);
            return;
        }
        */
            
        String query = "SELECT c.id FROM chip_experiments c";
        List<String> restrictions = new ArrayList<>();
        if(!parameters.getCellLine().equals( "Any" ))
        {
            query += " JOIN cells ON(cells.id=cell_id)";
            restrictions.add( "cells.title=" + SqlUtil.quoteString(parameters.getCellLine()) );
        }
        
        if(!parameters.getTf().equals( "Any" ))
            restrictions.add("c.tf_uniprot_id=" + SqlUtil.quoteString( parameters.getUniprotId() ));
        if(!parameters.getTreatment().equals( "Any" ))
            restrictions.add( "c.treatment=" + SqlUtil.quoteString( parameters.getTreatment() ) );
        
        query += " WHERE " + String.join( " AND ", restrictions );
        
        List<String> expIds = SqlUtil.queryStrings( con, query );
        
        
        SqlTrack preparedTrack = parameters.getPreparedTrack();

        SqlUtil.execute( con, "DROP TABLE IF EXISTS " + resultingTrackId );
        SqlUtil.execute( con, "CREATE TABLE " + resultingTrackId + " like " + getTableId( preparedTrack ) );
        SqlUtil.execute( con, "ALTER TABLE " + resultingTrackId + " CONVERT TO CHARACTER SET utf8" );
        
        String mainColumns = "chrom,start,end,strand";
        String constantColumns = "type,prop_experiment,prop_uniprotId,prop_tfTitle,prop_tfClassId,prop_antibody,prop_cellLine,prop_treatment";
        SqlUtil.execute( con, "ALTER TABLE "+ resultingTrackId + " MODIFY id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT" );
        SqlUtil.execute( con, "ALTER TABLE "+ resultingTrackId + " DISABLE KEYS" );
        
        
        DataCollection<ChIPseqExperiment> experiments = DataElementPath.create( "databases/GTRD/Data/experiments" ).getDataCollection( ChIPseqExperiment.class );
        
        for(String id : expIds)
        {
            ChIPseqExperiment exp = experiments.get( id );
            String caller = parameters.getDataSet().split( " " )[0];
            DataElementPath path = DataElementPath.create( "databases/GTRD/Data/peaks/" + caller + "/" + exp.getPeakId() );
            SqlTrack track = path.optDataElement( SqlTrack.class );
            if(track == null)
                continue;
            Set<String> siteProperties = track.getAllProperties().stream()
                    .filter( x->!x.equals("name") )//remove useless property of PICS and MACS2 peaks
                    .map(x->SqlUtil.quoteIdentifier( "prop_"+x ))
                    .collect( Collectors.toSet() );
            String columns = mainColumns;
            if(!siteProperties.isEmpty())
                columns += "," + String.join( ",", siteProperties );
            String constants = Stream.of(
                    exp.getTfTitle(),//type=tfTitle to be shown in genome browser
                    exp.getName(),
                    exp.getTfUniprotId(),
                    exp.getTfTitle(),
                    exp.getTfClassId(),
                    exp.getAntibody(),
                    exp.getCell().getTitle(),
                    exp.getTreatment() )
            .map( SqlUtil::quoteString )
            .collect( Collectors.joining( "," ) );
            
            query = "INSERT INTO " + resultingTrackId + "(" +columns + "," + constantColumns+ ") SELECT " + columns + "," + constants + " FROM " + getTableId( track ) ;
            SqlUtil.execute( con, query );
        }

        SqlUtil.execute( con, "ALTER TABLE "+ resultingTrackId + " MODIFY id INTEGER UNSIGNED NOT NULL" );
        SqlUtil.execute( con, "ALTER TABLE "+ resultingTrackId + " ENABLE KEYS" );
        
    }

    private void searchPeaksByTF(String tf) throws SQLException
    {
        //TODO: make same columns as in preparedTrack (except tf columns)
        String dataSet = parameters.getDataSet();
        String caller = dataSet.split( " " )[0];
        caller = caller.toUpperCase();
        SqlTrack track = parameters.getClustersFolder()
                .getChildPath( parameters.getOrganism().getLatinName(), "By TF", tf, caller + " peaks" )
                .optDataElement( SqlTrack.class );
        if(track == null)
            return;
        
        //just copy track to resultingTrack, use direct SQL queries for better performance
        String sqlTable = getTableId( track );
        
        SqlUtil.execute( con, "DROP TABLE IF EXISTS " + resultingTrackId );
        SqlUtil.execute( con, "CREATE TABLE " + resultingTrackId + " like " + sqlTable );
        
        String query = "INSERT INTO " + resultingTrackId + " SELECT * FROM " + sqlTable;
        SqlUtil.execute( con, query );
    }

    //tf - can not be "Any"
    private void searchClustersByTF(String tfUniprotId) throws Exception
    {
        String dataSet = parameters.getDataSet();
        String caller = dataSet.split( " " )[0];
        caller = caller.equals( "meta" ) ? caller : caller.toUpperCase();
        SqlTrack track = parameters.getClustersFolder()
                .getChildPath( parameters.getOrganism().getLatinName(), "By TF", tfUniprotId, caller + " clusters" )
                .optDataElement( SqlTrack.class );
        if(track == null)
            return;
        
        //just copy track to resultingTrack, use direct SQL queries for better performance
        String sqlTable = getTableId( track );
        
        SqlUtil.execute( con, "DROP TABLE IF EXISTS " + resultingTrackId );
        SqlUtil.execute( con, "CREATE TABLE " + resultingTrackId + " like " + sqlTable );
        
        String query = "INSERT INTO " + resultingTrackId + " SELECT * FROM " + sqlTable;
        SqlUtil.execute( con, query );
        
        String tfName = parameters.getTf().split( " " )[0];
        query = "UPDATE " + resultingTrackId + " SET type=" + SqlUtil.quoteString( tfName );
        SqlUtil.execute( con, query );
    }

    private void searchBindingSitesNearGene(String ensemblGene) throws Exception
    {
        SqlTrack preparedTrack = parameters.getPreparedTrack();
        String preparedTrackId = getTableId( preparedTrack );
        
        SqlUtil.execute( con, "DROP TABLE IF EXISTS " + resultingTrackId );
        SqlUtil.execute( con, "CREATE TABLE " + resultingTrackId + " like " + preparedTrackId );
        
        
        SqlTableDataCollection preparedTable = parameters.getPreparedTable();
        String preparedTableId = getTableId( preparedTable );

        String query = "INSERT INTO " + resultingTrackId + " SELECT distinct track.* FROM " + preparedTrackId + " track";
        List<String> joins = getJoins();
        if( !ensemblGene.isEmpty() )
            joins.add( preparedTableId + " use index (GeneID) ON(SiteID=track.id)" );
        if(!joins.isEmpty())
            query += StreamEx.of( joins ).map( s->" JOIN " + s ).joining();
        List<String> restrictions = getRestrictions();
        if(!ensemblGene.isEmpty())
        {
            restrictions.add( "GeneID=" + SqlUtil.quoteString( ensemblGene ) );
            restrictions.add( "Distance <= " + parameters.getMaxGeneDistance() );
        }
        if(!restrictions.isEmpty())
            query += " WHERE " + String.join( " AND ", restrictions );


        SqlUtil.execute( con, query );
    }

    public String getEnsemblGeneID()
    {
        String gene = parameters.getGene();
        if(gene.isEmpty() || gene.equals( "Any" ))
            return "";
        
        DataCollection<Gene> genesCollection = TrackUtils.getGenesCollection( parameters.getOrganism(), parameters.getResultingSites() );
        if(genesCollection.contains( gene ))
            return gene;
                
        ReferenceType inputType = ReferenceTypeRegistry.detectReferenceType( new String[] {gene} , GeneTableType.class );
        if(inputType instanceof DefaultReferenceType)
            throw new IllegalArgumentException( "Can not match provided gene '" + gene + "'" );
        log.info( "Provided gene detected as " + inputType.toString() );
        
        Properties inputProperties = BioHubSupport.createProperties( parameters.getOrganism(), inputType );
        Properties outputProperties = BioHubSupport.createProperties( parameters.getOrganism(),
                ReferenceTypeRegistry.getReferenceType( EnsemblGeneTableType.class ) );
        
        Set<String> genes = BioHubRegistry.getReferencesFlat( new String[] { gene }, inputProperties, outputProperties, null );
        if(genes == null || genes.isEmpty())
          throw new IllegalArgumentException("Gene " + gene + " not found");
        return genes.iterator().next();
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        boolean geneSet = !parameters.getGene().isEmpty() && !parameters.getGene().equals( "Any" );
        if(geneSet)
            checkLesser( "maxGeneDistance", 50001 );
        if(!geneSet && parameters.getTf().equals( "Any" ) && parameters.getCellLine().equals( "Any" ) && parameters.getTreatment().equals( "Any" ))
            throw new IllegalArgumentException("Please restrict your search by selecting at least one of 'Gene symbol or ID', 'Transcription factor', 'Cell line' or 'Treatment'");
    }
    
    public static class Parameters extends SearchByRegulation.Parameters
    {
        private String gene = "Any";
        private DataElementPath resultingSites = DataElementPath.create( "data/Collaboration/Demo/tmp/TF binding sites" );
        
        @PropertyName( "Gene symbol or ID" )
        @PropertyDescription( "Restrict search to sites near this gene, enter 'Any' to search in all genes" )
        public String getGene() {
            return gene;
        }

        public void setGene(String gene)
        {
            Object oldValue = this.gene;
            this.gene = gene;
            firePropertyChange( "gene", oldValue, gene );
        }
        
        public static final String OUTPUT_TYPE_OPEN_IN_GENOME_BROWSER = "Open in genome browser";
        public static final String OUTPUT_TYPE_DOWNLOAD_FILE = "Download file";
        public String outputType = OUTPUT_TYPE_OPEN_IN_GENOME_BROWSER;
        @PropertyName( "Output type" )
        @PropertyDescription( "Type of result" )
        public String getOutputType()
        {
            return outputType;
        }

        public void setOutputType(String outputType)
        {
            String oldValue = this.outputType;
            this.outputType = outputType;
            firePropertyChange( "outputType", oldValue, outputType );
        }

        @PropertyName( "Binding sites" )
        @PropertyDescription( "Found transcription factor binding sites" )
        public DataElementPath getResultingSites()
        {
            return resultingSites;
        }

        public void setResultingSites(DataElementPath resultingSites)
        {
            Object oldValue = this.resultingSites;
            this.resultingSites = resultingSites;
            firePropertyChange( "resultingSites", oldValue, resultingSites );
        }
    }
    
    public static class ParametersBeanInfo extends SearchByRegulation.ParametersBeanInfo
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            property("outputType").tags( Parameters.OUTPUT_TYPE_OPEN_IN_GENOME_BROWSER, Parameters.OUTPUT_TYPE_DOWNLOAD_FILE ).add();
            property("resultingSites").outputElement( Track.class ).expert().add();
        }
        
        @Override
        protected void initAfterOrganism() throws Exception
        {
            add( "gene" );
        }
    }

}
