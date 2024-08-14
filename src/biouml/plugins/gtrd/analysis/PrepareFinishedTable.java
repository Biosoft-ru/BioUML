package biouml.plugins.gtrd.analysis;

import java.sql.Connection;

import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PrepareFinishedTable extends AnalysisMethodSupport<PrepareFinishedTable.Parameters>
{

    public PrepareFinishedTable(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Connection con = DataCollectionUtils.getSqlConnection( parameters.getGtrdPath().getDataElement() );
        
        addTfUnirpotIndex(con);

        if(parameters.isChipSeqPeaks())
            preparePeaksFinishedTable( con );
        
        if(parameters.isChipSeqClusters())
            prepareClustersFinishedTable( con );
        
        if(parameters.isDnasePeaks())
            prepareDNAseFinishedTable( con );
        
        if(parameters.isATACseqPeaks())
            prepareATACseqFinishedTable( con );
        
        if(parameters.isFAIREseqPeaks())
            prepareFAIREseqFinishedTable( con );
        
        if(parameters.isMnasePeaks())
            prepareMNaseFinishedTable( con );
        
        if(parameters.isChipexoPeaks())
            prepareChIPexoFinishedTable( con );
        
        if(parameters.isHistonesPeaks())
            prepareHistonesFinishedTable( con );;
        
        return null;
    }

    private void addTfUnirpotIndex(Connection con)
    {
        if(!SqlUtil.isIndexExists( con, "chip_experiments", "by_tf_uniprot_id" ))
        {
            //it will speed up biouml.plugins.gtrd.analysis.OpenPerTFView.TFSelector query
            SqlUtil.execute( con, "create index by_tf_uniprot_id on chip_experiments(tf_uniprot_id)" );
        }
    }

    private void prepareClustersFinishedTable(Connection con) throws BiosoftSQLException, RepositoryException
    {
        final String CLUSTERS_FINISHED_TABLE = "clusters_finished";
        SqlUtil.dropTable( con, CLUSTERS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + CLUSTERS_FINISHED_TABLE + " ("
            + "uniprot_id VARCHAR(20) NOT NULL,"
            + "specie VARCHAR(100) NOT NULL,"
            + "cluster_type VARCHAR(20) NOT NULL,"
            + "PRIMARY KEY(uniprot_id,specie,cluster_type)"
            +") ENGINE=MyISAM" );
        
        
        for(String specie : Species.allSpecies().map( Species::getLatinName ).toList())
        {
            DataElementPath byTF = parameters.getGtrdPath().getChildPath( "Data", "clusters", specie, "By TF" );
            if(!byTF.exists())
                continue;
            for(DataElementPath tf : byTF.getChildren())
            {
                for(String clusterType : new String[] {"GEM", "MACS", "PICS", "SISSRS", "meta"})
                {
                    if(tf.getChildPath( clusterType + " clusters" ).exists())
                    {
                        Query query = new Query("INSERT INTO $t$ (uniprot_id,specie,cluster_type) VALUES($tf$,$specie$,$cluster_type$)")
                                .name( "t", CLUSTERS_FINISHED_TABLE )
                                .str( "tf", tf.getName() )
                                .str( "specie", specie )
                                .str( "cluster_type", clusterType.toLowerCase() );
                        SqlUtil.execute( con, query );
                    }
                }
            }
        }
    }

    private void preparePeaksFinishedTable(Connection con) throws BiosoftSQLException, RepositoryException
    {
        final String PEAKS_FINISHED_TABLE = "peaks_finished" ;
        SqlUtil.dropTable( con, PEAKS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + PEAKS_FINISHED_TABLE + " ("
                + "exp_id VARCHAR(20) NOT NULL,"
                + "peak_type VARCHAR(10) NOT NULL,"
                + "PRIMARY KEY (exp_id, peak_type)"
                + ") ENGINE=MyISAM" );
        DataCollection<ChIPseqExperiment> experiments = parameters.getGtrdPath().getChildPath( "Data", "experiments" ).getDataCollection( ChIPseqExperiment.class );
        for(ChIPseqExperiment e : experiments)
        {
            if(e.isControlExperiment())
                continue;
            String expId = e.getName();
            for(String peakCaller : ChIPseqExperiment.PEAK_CALLERS)
            {
                DataElementPath peakPath = e.getPeaksByPeakCaller( peakCaller );
                if(peakPath.exists())
                {
                    Query query = new Query( "INSERT INTO $t$ (exp_id, peak_type) VALUES($exp_id$, $peak_type$)" )
                        .name( "t", PEAKS_FINISHED_TABLE )
                        .str( "exp_id", expId )
                        .str( "peak_type", peakCaller );
                    SqlUtil.execute( con, query );
                }
            }
        }
    }
    
    private void prepareDNAseFinishedTable(Connection con)
    {
        final String DNASE_PEAKS_FINISHED_TABLE = "dnase_peaks_finished" ;
        SqlUtil.dropTable( con, DNASE_PEAKS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + DNASE_PEAKS_FINISHED_TABLE + " ("
                + "exp_id VARCHAR(20) NOT NULL,"
                + "peak_type VARCHAR(32) NOT NULL,"
                + "PRIMARY KEY (exp_id, peak_type)"
                + ") ENGINE=MyISAM" );
        DataCollection<DNaseExperiment> experiments = parameters.getGtrdPath().getChildPath( "Data", "DNase experiments" ).getDataCollection( DNaseExperiment.class );
        for(DNaseExperiment e : experiments)
        {
            String expId = e.getName();           
            for(String peakType : DNaseExperiment.PEAK_CALLERS)
            {
                boolean anyExists = false;
                for(DataElementPath peakPath : e.getPeaksByPeakCaller( peakType ))
                {
                    if(peakPath.exists())
                        anyExists = true;
                }                
                if(anyExists)
                {
                    Query query = new Query( "INSERT INTO $t$ (exp_id, peak_type) VALUES($exp_id$, $peak_type$)" )
                        .name( "t", DNASE_PEAKS_FINISHED_TABLE )
                        .str( "exp_id", expId )
                        .str( "peak_type", peakType );
                    SqlUtil.execute( con, query );
                }
            }
        }
    }

    private void prepareChIPexoFinishedTable(Connection con)
    {
        final String CHIPEXO_PEAKS_FINISHED_TABLE = "chipexo_peaks_finished" ;
        SqlUtil.dropTable( con, CHIPEXO_PEAKS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + CHIPEXO_PEAKS_FINISHED_TABLE + " ("
                + "exp_id VARCHAR(20) NOT NULL,"
                + "peak_type VARCHAR(32) NOT NULL,"
                + "PRIMARY KEY (exp_id, peak_type)"
                + ") ENGINE=MyISAM" );
        
        DataCollection<ChIPexoExperiment> experiments = parameters.getGtrdPath().getChildPath( "Data", "ChIP-exo experiments" ).getDataCollection( ChIPexoExperiment.class );
        for(ChIPexoExperiment e : experiments)
        {
            if(e.isControlExperiment())
                continue;
            for(String peakType : ChIPexoExperiment.PEAK_CALLERS)
            {
                if(e.getPeaksByPeakCaller( peakType ).exists())
                {
                    String expId = e.getName();
                    Query query = new Query( "INSERT INTO $t$ (exp_id, peak_type) VALUES($exp_id$, $peak_type$)" )
                        .name( "t", CHIPEXO_PEAKS_FINISHED_TABLE )
                        .str( "exp_id", expId )
                        .str( "peak_type", peakType );
                    SqlUtil.execute( con, query );
                }
            }
        }
    }
    
    private void prepareHistonesFinishedTable(Connection con)
    {
        final String HISTONES_PEAKS_FINISHED_TABLE = "hist_peaks_finished" ;
        SqlUtil.dropTable( con, HISTONES_PEAKS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + HISTONES_PEAKS_FINISHED_TABLE + " ("
                + "exp_id VARCHAR(20) NOT NULL,"
                + "peak_type VARCHAR(32) NOT NULL,"
                + "PRIMARY KEY (exp_id, peak_type)"
                + ") ENGINE=MyISAM" );
        DataCollection<HistonesExperiment> experiments = parameters.getGtrdPath().getChildPath( "Data", "ChIP-seq HM experiments" ).getDataCollection( HistonesExperiment.class );
        for(HistonesExperiment e : experiments)
        {
            if(e.isControlExperiment())
                continue;
            for(String peakType : HistonesExperiment.PEAK_CALLERS)
            {
                if(e.getPeakByPeakCaller( peakType ).exists())
                {
                    String expId = e.getName();
                    Query query = new Query( "INSERT INTO $t$ (exp_id, peak_type) VALUES($exp_id$, $peak_type$)" )
                        .name( "t", HISTONES_PEAKS_FINISHED_TABLE )
                        .str( "exp_id", expId )
                        .str( "peak_type", peakType );
                    SqlUtil.execute( con, query );
                }
            }
        }
    }
    
    private void prepareATACseqFinishedTable(Connection con)
    {
        final String ATAC_PEAKS_FINISHED_TABLE = "atac_peaks_finished" ;
        SqlUtil.dropTable( con, ATAC_PEAKS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + ATAC_PEAKS_FINISHED_TABLE + " ("
                + "exp_id VARCHAR(20) NOT NULL,"
                + "peak_type VARCHAR(32) NOT NULL,"
                + "PRIMARY KEY (exp_id, peak_type)"
                + ") ENGINE=MyISAM" );
        DataCollection<ATACExperiment> experiments = parameters.getGtrdPath().getChildPath( "Data", "ATAC-seq experiments" ).getDataCollection( ATACExperiment.class );
        for(ATACExperiment e : experiments)
        {
            String expId = e.getName();           
            for(String peakType : ATACExperiment.PEAK_CALLERS)
            {
                if(e.getPeaksByPeakCaller( peakType ).exists())
                {
                    Query query = new Query( "INSERT INTO $t$ (exp_id, peak_type) VALUES($exp_id$, $peak_type$)" )
                        .name( "t", ATAC_PEAKS_FINISHED_TABLE )
                        .str( "exp_id", expId )
                        .str( "peak_type", peakType );
                    SqlUtil.execute( con, query );
                }
            }
        }
    }
    
    private void prepareFAIREseqFinishedTable(Connection con)
    {
        final String FAIRE_PEAKS_FINISHED_TABLE = "faire_peaks_finished" ;
        SqlUtil.dropTable( con, FAIRE_PEAKS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + FAIRE_PEAKS_FINISHED_TABLE + " ("
                + "exp_id VARCHAR(20) NOT NULL,"
                + "peak_type VARCHAR(32) NOT NULL,"
                + "PRIMARY KEY (exp_id, peak_type)"
                + ") ENGINE=MyISAM" );
        DataCollection<FAIREExperiment> experiments = parameters.getGtrdPath().getChildPath( "Data", "FAIRE-seq experiments" ).getDataCollection( FAIREExperiment.class );
        for(FAIREExperiment e : experiments)
        {
            String expId = e.getName();           
            for(String peakType : MNaseExperiment.PEAK_CALLERS)
            {
                if(e.getPeaksByPeakCaller( peakType ).exists())
                {
                    Query query = new Query( "INSERT INTO $t$ (exp_id, peak_type) VALUES($exp_id$, $peak_type$)" )
                        .name( "t", FAIRE_PEAKS_FINISHED_TABLE )
                        .str( "exp_id", expId )
                        .str( "peak_type", peakType );
                    SqlUtil.execute( con, query );
                }
            }
        }
    }
    
    private void prepareMNaseFinishedTable(Connection con)
    {
        final String MNASE_PEAKS_FINISHED_TABLE = "mnase_peaks_finished" ;
        SqlUtil.dropTable( con, MNASE_PEAKS_FINISHED_TABLE );
        SqlUtil.execute( con, "CREATE TABLE " + MNASE_PEAKS_FINISHED_TABLE + " ("
                + "exp_id VARCHAR(20) NOT NULL,"
                + "peak_type VARCHAR(32) NOT NULL,"
                + "PRIMARY KEY (exp_id, peak_type)"
                + ") ENGINE=MyISAM" );
        DataCollection<MNaseExperiment> experiments = parameters.getGtrdPath().getChildPath( "Data", "MNase-seq experiments" ).getDataCollection( MNaseExperiment.class );
        for(MNaseExperiment e : experiments)
        {
            String expId = e.getName();           
            for(String peakType : MNaseExperiment.PEAK_CALLERS)
            {
                if(e.getPeaksByPeakCaller( peakType ).exists())
                {
                    Query query = new Query( "INSERT INTO $t$ (exp_id, peak_type) VALUES($exp_id$, $peak_type$)" )
                        .name( "t", MNASE_PEAKS_FINISHED_TABLE )
                        .str( "exp_id", expId )
                        .str( "peak_type", peakType );
                    SqlUtil.execute( con, query );
                }
            }
        }
    }
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath gtrdPath = DataElementPath.create( "databases/GTRD" );

        public DataElementPath getGtrdPath()
        {
            return gtrdPath;
        }
        public void setGtrdPath(DataElementPath gtrdPath)
        {
            Object oldValue = this.gtrdPath;
            this.gtrdPath = gtrdPath;
            firePropertyChange( "gtrdPath", oldValue, gtrdPath );
        }
        
        private boolean chipSeqPeaks = true;
        public boolean isChipSeqPeaks()
        {
            return chipSeqPeaks;
        }
        public void setChipSeqPeaks(boolean chipSeqPeaks)
        {
            boolean oldValue = this.chipSeqPeaks;
            this.chipSeqPeaks = chipSeqPeaks;
            firePropertyChange( "chipSeqPeaks", oldValue, chipSeqPeaks );
        }
        
        private boolean chipSeqClusters = true;
        public boolean isChipSeqClusters()
        {
            return chipSeqClusters;
        }
        public void setChipSeqClusters(boolean chipSeqClusters)
        {
            boolean oldValue = this.chipSeqClusters;
            this.chipSeqClusters = chipSeqClusters;
            firePropertyChange( "chipSeqClusters", oldValue, chipSeqClusters );
        }

        private boolean dnasePeaks = true;
        public boolean isDnasePeaks()
        {
            return dnasePeaks;
        }
        public void setDnasePeaks(boolean dnasePeaks)
        {
            boolean oldValue = this.dnasePeaks;
            this.dnasePeaks = dnasePeaks;
            firePropertyChange( "dnasePeaks", oldValue, dnasePeaks );
        }
        
        private boolean atacPeaks = true;
        public boolean isATACseqPeaks()
        {
            return atacPeaks;
        }
        public void setATACseqPeaks(boolean atacPeaks)
        {
            boolean oldValue = this.atacPeaks;
            this.atacPeaks = atacPeaks;
            firePropertyChange( "atacPeaks", oldValue, atacPeaks );
        }
        
        private boolean fairePeaks = true;
        public boolean isFAIREseqPeaks()
        {
            return fairePeaks;
        }
        public void setFAIREseqPeaks(boolean fairePeaks)
        {
            boolean oldValue = this.fairePeaks;
            this.fairePeaks = fairePeaks;
            firePropertyChange( "fairePeaks", oldValue, fairePeaks );
        }
        
        private boolean mnasePeaks = true;
        public boolean isMnasePeaks()
        {
            return mnasePeaks;
        }
        public void setMnasePeaks(boolean mnasePeaks)
        {
            boolean oldValue = this.mnasePeaks;
            this.mnasePeaks = mnasePeaks;
            firePropertyChange( "mnasePeaks", oldValue, mnasePeaks );
        }
        
        private boolean chipexoPeaks = true;
        public boolean isChipexoPeaks()
        {
            return chipexoPeaks;
        }
        public void setChipexoPeaks(boolean chipexoPeaks)
        {
            boolean oldValue = this.chipexoPeaks;
            this.chipexoPeaks = chipexoPeaks;
            firePropertyChange( "chipexoPeaks", oldValue, chipexoPeaks );
        }
        
        private boolean histonesPeaks = true;
        public boolean isHistonesPeaks()
        {
            return histonesPeaks;
        }
        public void setHistonesPeaks(boolean histonesPeaks)
        {
            boolean oldValue = this.histonesPeaks;
            this.histonesPeaks = histonesPeaks;
            firePropertyChange( "histonesPeaks", oldValue, histonesPeaks );
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
            property("gtrdPath").inputElement( ru.biosoft.access.core.DataCollection.class ).add();
            property("chipSeqPeaks").add();
            property("chipSeqClusters").add();
            property("dnasePeaks").add();
            property("atacPeaks").add();
            property("fairePeaks").add();
            property("mnasePeaks").add();
            property("chipexoPeaks").add();
            property("histonesPeaks").add();
        }
    }

}
