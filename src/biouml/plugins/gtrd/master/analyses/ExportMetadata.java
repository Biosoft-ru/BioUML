package biouml.plugins.gtrd.master.analyses;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

import biouml.plugins.gtrd.ATACExperiment;
import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.ChIPExperiment;
import biouml.plugins.gtrd.ChIPexoExperiment;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.DNaseExperiment;
import biouml.plugins.gtrd.ExperimentType;
import biouml.plugins.gtrd.FAIREExperiment;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.MNaseExperiment;
import biouml.plugins.gtrd.analysis.OpenPerTFView.SpeciesSelector;
import biouml.plugins.gtrd.master.meta.Metadata;
import biouml.plugins.gtrd.master.meta.TF;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class ExportMetadata extends AnalysisMethodSupport<ExportMetadata.Parameters>
{
    public ExportMetadata(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    public Object justAnalyzeAndPut() throws Exception
    {
        loadHocomoco();
        if(parameters.isAllFactors())
        {
            TFSelector tfSelector = new TFSelector();
            tfSelector.setBean( parameters );
            String[] tfList = tfSelector.getAvailableValues();
            for(int i = 0; i < tfList.length; i++)
            {
                String tf = tfList[i];
                log.info( "Exporting " + tf );
                jobControl.pushProgress( i*100/tfList.length, (i+1)*100/tfList.length );
                parameters.setTf( tf );
                exportFactor( parameters.getUniprotId() );
                jobControl.popProgress();
            }
        }
        else
            exportFactor(parameters.getUniprotId());
        
        return new Object[] {};
    }

    private Map<String, List<SiteModel>> siteModels = new HashMap<>();
    private void loadHocomoco()
    {
        String hocomocoPath = "databases/HOCOMOCO v11/Data";
        String[] profiles = {"PWM_HUMAN_mono_pval=0.0001", "PWM_MOUSE_mono_pval=0.0001"};
        for( String profile : profiles )
        {
            SiteModelCollection dc = DataElementPath.create( hocomocoPath, profile ).getDataElement( SiteModelCollection.class );
            for( SiteModel sm : dc )
            {
                for( TranscriptionFactor tf : sm.getBindingElement().getFactors() )
                    siteModels.computeIfAbsent( tf.getName(), k -> new ArrayList<>() ).add( sm );
            }
        }
    }

    private void addSiteModels(Metadata meta)
    {
        List<SiteModel> modelList = siteModels.get( meta.tf.uniprotId );
        if( modelList != null )
            for( SiteModel sm : modelList )
                meta.siteModels.add( sm.getCompletePath() );
    }

    
    private void exportFactor(String uniprotId) throws Exception
    {
        log.info( "Loading metadata" );
        jobControl.pushProgress( 0, 20 );
        long startTime = System.currentTimeMillis();
        Metadata metadata = loadMetadata(uniprotId, log);
        long time = System.currentTimeMillis();
        jobControl.popProgress();
        log.info( "Done loading metadata in " + (time - startTime) + "ms" );
        
        addSiteModels(metadata);
        
        metadata.setVersion( parameters.getVersion() );
        metadata.setName( "mt." + metadata.tf.uniprotName + ".v" + metadata.getVersion() + (parameters.isAddExtension() ? ".json" : "") );
        
        log.info( "Writing metadata" );
        DataElementPath folder = parameters.getResultPath();
        DataElementPath resPath = folder.getChildPath( metadata.getName() );
        DataCollectionUtils.createFoldersForPath( resPath );
        resPath.save( metadata );
        log.info( "Done writing metadata" );
    }
    
    public static Metadata loadMetadata(String uniprotId, Logger log) throws Exception
    {
        Metadata meta = new Metadata();
        
        loadTF(uniprotId, meta, log);
        
        DataCollection<ChIPseqExperiment> chipseq = DataElementPath.create( "databases/GTRD/Data/experiments" ).getDataCollection( ChIPseqExperiment.class );
        DataCollection<ChIPexoExperiment> chipexo = DataElementPath.create( "databases/GTRD/Data/ChIP-exo experiments" ).getDataCollection( ChIPexoExperiment.class );
        DataCollection<HistonesExperiment> histones = DataElementPath.create( "databases/GTRD/Data/ChIP-seq HM experiments" ).getDataCollection( HistonesExperiment.class );
        
        for(ChIPseqExperiment exp : chipseq)
        {
            if(!uniprotId.equals(exp.getTfUniprotId()))
                continue;
            meta.chipSeqExperiments.put( exp.getName(), exp );
            CellLine cell = exp.getCell();
            meta.cells.put( cell.getName(), cell );
        }
        
        for(ChIPseqExperiment exp : new ArrayList<>(meta.chipSeqExperiments.values()))
        {
            if(exp.getControlId() != null)
            {
                ChIPseqExperiment ctrl = chipseq.get( exp.getControlId() );
                if(ctrl == null)
                {
                    ChIPExperiment anotherTypeCtrl = histones.get( exp.getControlId() );
                    if(anotherTypeCtrl == null)
                        anotherTypeCtrl = chipexo.get( exp.getControlId() );
                    if(anotherTypeCtrl == null)
                        throw new RuntimeException("Control not found: " + exp.getControlId() );
                    ctrl = new ChIPseqExperiment( anotherTypeCtrl.getOrigin(), anotherTypeCtrl.getName() );
                    copyControl( anotherTypeCtrl, ctrl );
                }
                meta.chipSeqExperiments.put(ctrl.getName(), ctrl);
                CellLine cell = ctrl.getCell();
                meta.cells.put( cell.getName(), cell );
            }
        }
        
        
        for(ChIPexoExperiment exp : chipexo)
        {
            if(!uniprotId.equals(exp.getTfUniprotId()))
                continue;
            meta.chipExoExperiments.put( exp.getName(), exp );
            CellLine cell = exp.getCell();
            meta.cells.put( cell.getName(), cell );
        }
        
        for(ChIPexoExperiment exp : new ArrayList<>(meta.chipExoExperiments.values()))
        {
            if(exp.getControlId() != null)
            {
                ChIPexoExperiment ctrl = chipexo.get( exp.getControlId() );
                if(ctrl == null)
                {
                    ChIPExperiment anotherTypeCtrl = histones.get( exp.getControlId() );
                    if(anotherTypeCtrl == null)
                        anotherTypeCtrl = chipseq.get( exp.getControlId() );
                    if(anotherTypeCtrl == null)
                        throw new RuntimeException("Control not found: " + exp.getControlId() );
                    ctrl = new ChIPexoExperiment( anotherTypeCtrl.getOrigin(), anotherTypeCtrl.getName() );
                    copyControl( anotherTypeCtrl, ctrl );
                }
                meta.chipExoExperiments.put(ctrl.getName(), ctrl);
                CellLine cell = ctrl.getCell();
                meta.cells.put( cell.getName(), cell );
            }
        }
        
        
        for(HistonesExperiment exp : histones)
        {
            if(meta.cells.containsKey( exp.getCell().getName()))
                meta.histoneExperiments.put( exp.getName(), exp );
        }
        
        for(HistonesExperiment exp : new ArrayList<>( meta.histoneExperiments.values() ))
        {
            if(exp.getControlId() != null)
            {
                HistonesExperiment ctrl = histones.get( exp.getControlId() );
                if(ctrl == null)
                {
                    ChIPExperiment anotherTypeCtrl = chipseq.get( exp.getControlId() );
                    if(anotherTypeCtrl == null)
                        anotherTypeCtrl = chipexo.get( exp.getControlId() );
                    if(anotherTypeCtrl == null)
                        throw new RuntimeException("Control not found: " + exp.getControlId() );
                    ctrl = new HistonesExperiment( anotherTypeCtrl.getOrigin(), anotherTypeCtrl.getName() );
                    copyControl( anotherTypeCtrl, ctrl );
                }
                meta.histoneExperiments.put(ctrl.getName(), ctrl);
                CellLine cell = ctrl.getCell();
                meta.cells.put( cell.getName(), cell );
            }
        }
        
        DataCollection<DNaseExperiment> dnase = DataElementPath.create( "databases/GTRD/Data/DNase experiments" ).getDataCollection( DNaseExperiment.class );
        for(DNaseExperiment exp : dnase)
        {
            if(meta.cells.containsKey( exp.getCell().getName()))
                meta.dnaseExperiments.put( exp.getName(), exp );
        }
        
        DataCollection<MNaseExperiment> mnase = DataElementPath.create( "databases/GTRD/Data/MNase-seq experiments" ).getDataCollection( MNaseExperiment.class );
        for(MNaseExperiment exp : mnase)
        {
            if(meta.cells.containsKey( exp.getCell().getName()))
                meta.mnaseExperiments.put( exp.getName(), exp );
        }
        
        DataCollection<ATACExperiment> atac = DataElementPath.create( "databases/GTRD/Data/ATAC-seq experiments" ).getDataCollection( ATACExperiment.class );
        for(ATACExperiment exp : atac)
        {
            if(meta.cells.containsKey( exp.getCell().getName()))
                meta.atacExperiments.put( exp.getName(), exp );
        }
        
        DataCollection<FAIREExperiment> faire = DataElementPath.create( "databases/GTRD/Data/FAIRE-seq experiments" ).getDataCollection( FAIREExperiment.class );
        for(FAIREExperiment exp : faire)
        {
            if(meta.cells.containsKey( exp.getCell().getName()))
                meta.faireExperiments.put( exp.getName(), exp );
        }
        
        return meta;
    }

    public static void copyControl(ChIPExperiment src, ChIPExperiment dst)
    {
        dst.setAlignment( src.getAlignment() );
        dst.setAlignmentId( src.getAlignmentId() );
        dst.setAntibody( src.getAntibody() );
        dst.setArticles( src.getArticles() );
        dst.setCell( src.getCell() );
        dst.setControlId( null );
        dst.getProperties().putAll( src.getProperties() );
        dst.setExpType( ExperimentType.CHIP_CONTROL );
        dst.setPeak( src.getPeak() );
        dst.setPeakId( src.getPeakId() );
        dst.setReads( src.getReads() );
        dst.setReadsIds( src.getReadsIds() );
        dst.setSpecie( src.getSpecie() );
        dst.setTreatment( src.getTreatment() );
    }

    private static void loadTF(String uniprotId, Metadata meta, Logger log) throws SQLException
    {
        Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD/Data/experiments" ).getDataElement() );
        try(PreparedStatement ps = con.prepareStatement( "SELECT * FROM uniprot WHERE id=?" ))
        {
            ps.setString( 1, uniprotId );
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
                throw new RuntimeException("Can not find " + uniprotId + " in uniprot");
            TF tf = new TF();
            tf.uniprotId = uniprotId;
            tf.uniprotName = rs.getString( "name" );
            tf.uniprotStatus = rs.getString( "status" );
            tf.organism = rs.getString( "species" );
            tf.tfClassId = rs.getString( "cached_tf_class" );
            tf.uniprotGeneNames = Arrays.asList( rs.getString( "gene_names" ).split( " " ) );
            String str = rs.getString( "protein_names" );
            tf.uniprotProteinNames = parseProteinNames(str, log);
            meta.tf = tf; 
        }
    }

    private static List<String> parseProteinNames(String str, Logger log)
    {
        int idx = str.indexOf( " (" );
        if(idx == -1)
            return Collections.singletonList( str );
        List<String> result = new ArrayList<>();
        result.add( str.substring( 0, idx ) );
        while(true)
        {
            int start = idx + 2;
            int end = str.indexOf( ')', start );
            if(end == -1)
                throw new RuntimeException("Unmatched ( in: " + str);
            result.add( str.substring( start, end ) );
            idx = end + 1;
            if(idx >= str.length())
                break;
            if(str.charAt( idx ) != ' ' || idx+1 >= str.length() || str.charAt( idx+1 ) != '(')
            {
                log.warning( "Expecting ' (' at " + idx + " in: " + str );
                break;
            }
        }
        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private Species organism = Species.getSpecies( "Homo sapiens" );
        @PropertyName("Organism")
        public Species getOrganism()
        {
            return organism;
        }
        public void setOrganism(Species organism)
        {
            Species oldValue = this.organism;
            this.organism = organism;
            firePropertyChange( "organism", oldValue, organism );
            setTf( null );
        }
        
        private boolean allFactors = false;
        public boolean isAllFactors()
        {
            return allFactors;
        }
        public void setAllFactors(boolean allFactors)
        {
            boolean oldValue = this.allFactors;
            this.allFactors = allFactors;
            firePropertyChange( "allFactors", oldValue, allFactors );
        }
        
        private String tf = null;
        @PropertyName("Transcription factor")
        public String getTf()
        {
            return tf;
        }
        public void setTf(String tf)
        {
            String oldValue = this.tf;
            this.tf = tf;
            firePropertyChange( "tf", oldValue, tf );
        }
        public String getUniprotId()
        {
            return TextUtil.split( getTf(), ' ' )[1];
        }
        
        private int version = 1;
        public int getVersion()
        {
            return version;
        }
        public void setVersion(int version)
        {
            int oldValue = this.version;
            this.version = version;
            firePropertyChange( "version", oldValue, version );
        }
        
        private boolean addExtension = true;
        public boolean isAddExtension()
        {
            return addExtension;
        }
        public void setAddExtension(boolean addExtension)
        {
            boolean oldValue = this.addExtension;
            this.addExtension = addExtension;
            firePropertyChange( "addExtension", oldValue, addExtension );
        }

        private DataElementPath resultPath;

        @PropertyName ( "Result path" )
        public DataElementPath getResultPath()
        {
            return resultPath;
        }
        public void setResultPath(DataElementPath resultPath)
        {
            Object oldValue = this.resultPath;
            this.resultPath = resultPath;
            firePropertyChange( "resultPath", oldValue, resultPath );
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
            property( "organism" ).editor( SpeciesSelector.class ).hideChildren().add();
            property( "allFactors" ).add();
            property( "tf" ).editor( TFSelector.class ).hidden( "isAllFactors" ).add();
            property( "version" ).add();
            property( "addExtension" ).add();
            property( "resultPath" ).outputElement( FolderCollection.class ).add();
        }
    }
    
    public static class TFSelector extends GenericComboBoxEditor
    {
        @Override
        public String[] getAvailableValues()
        {
            Connection con = DataCollectionUtils.getSqlConnection( DataElementPath.create( "databases/GTRD" ).getDataElement() );
            Object parameters = getBean();
            ComponentModel model = ComponentFactory.getModel( parameters );
            Species organism = (Species)model.findProperty( "organism" ).getValue();

            String query = "SELECT DISTINCT uniprot.id, uniprot.gene_name FROM uniprot "
                    + "JOIN chip_experiments ce on(uniprot.id=ce.tf_uniprot_id)";
            if(organism != null)
                    query += " WHERE ce.specie=?";
            query += " ORDER BY 2";
            

            List<String> result = new ArrayList<>();
            try(PreparedStatement ps = con.prepareStatement( query ))
            {
                if(organism != null)
                    ps.setString( 1, organism.getLatinName() );
                try( ResultSet rs = ps.executeQuery() )
                {
                    while( rs.next() )
                    {
                        String uniprotId = rs.getString( 1 );
                        String geneName = rs.getString( 2 );
                        result.add( geneName + " " + uniprotId );
                    }
                }
            }
            catch( SQLException e )
            {
                throw new RuntimeException(e);
            }

            return result.toArray(new String[0]);
        }
    }
}
