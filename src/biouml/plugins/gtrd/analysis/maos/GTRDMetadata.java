package biouml.plugins.gtrd.analysis.maos;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.analysis.SiteModelsToProteins;
import ru.biosoft.bsa.analysis.SiteModelsToProteinsSupport.Link;

public class GTRDMetadata
{
    public Map<String, String> uniprot2tfClass = new HashMap<>();
    public Map<String, String[]> siteModel2Uniprot;
    public Map<String, List<ExperimentGroup>> tfClass2ExperimentGroups = new HashMap<>();
    public Map<String, Set<String>> siteModel2TfClass = new HashMap<>();
    private AnalysisJobControl progress;
    
    public GTRDMetadata(AdvancedParameters parameters, AnalysisJobControl progress, Logger analysisLog) throws Exception
    {
        this.progress = progress;
        
        analysisLog.info( "Loading transcription factors from uniprot." );
        progress.pushProgress( 0, 30 );
        loadUniprot2TfClass();
        progress.popProgress();
        analysisLog.info( "Loaded " + uniprot2tfClass.size() + " transcription factors." );
        
        analysisLog.info( "Mapping site models into uniprot." );
        progress.pushProgress( 30, 60 );
        loadSiteModel2Uniprot(parameters.getSiteModelCollection(), progress, analysisLog);
        progress.popProgress();
        analysisLog.info( "Mapping finished." );
        
        analysisLog.info( "Mapping site models into tfClasses." );
        progress.pushProgress( 60, 100 );
        for(String siteModelName : parameters.getSiteModelCollection().getNameList())
        {
            Set<String> tfClasses = convertSiteModelToTFClass( siteModelName, parameters.getTfClassDepthNumber() );
            siteModel2TfClass.put( siteModelName, tfClasses );
        }
        progress.popProgress();
        analysisLog.info( "Mapping finished." );
        
        /*
        analysisLog.info( "Loading GTRD experiments." );
        progress.pushProgress( 60, 100 );
        loadExperiments(parameters.getTfClassDepthNumber());
        progress.popProgress();
        analysisLog.info("GTRD loaded.");*/
    }
    
    private Set<String> convertSiteModelToTFClass(String siteModelName, int level)
    {
        Set<String> result = new HashSet<>();
        String[] uniprots = siteModel2Uniprot.get( siteModelName );
        if(uniprots == null)
            return result;
        for(String uniprot : uniprots)
        {
            String tfClass = uniprot2tfClass.get( uniprot );
            if(tfClass == null)
                continue;
            tfClass = toLowerDepth( tfClass, level );
            result.add( tfClass );
        }
        return result;
    }
    
    
    private void loadUniprot2TfClass() throws SQLException
    {
        Connection con = SqlConnectionPool.getConnection( getExperimentsCollection() );

        try(Statement st = con.createStatement();
            ResultSet rs = st.executeQuery( "SELECT id,cached_tf_class FROM uniprot WHERE species='Homo sapiens' AND NOT(isNULL(cached_tf_class))" ))
        {
                while( rs.next() )
                {
                    String uniprotId = rs.getString( 1 );
                    String tfClass = rs.getString( 2 );
                    uniprot2tfClass.put( uniprotId, tfClass );
                }
        }
    }

    
    private void loadSiteModel2Uniprot(SiteModelCollection smc, AnalysisJobControl progress, Logger analysisLog)
    {
        String[] siteNames = smc.getNameList().toArray( new String[0] );
        Species human = Species.getSpecies( "Homo sapiens" );
        ReferenceType uniprot = ReferenceTypeRegistry.getReferenceType( UniprotProteinTableType.class );
        
        progress.pushProgress( 0, 50 );
        Map<String, Set<Link>> factors = SiteModelsToProteins.getFactors( smc, siteNames, human, progress, analysisLog );
        progress.popProgress();
        
        progress.pushProgress( 50, 100 );
        siteModel2Uniprot = SiteModelsToProteins.getMolecules( factors, uniprot, human, progress );
        progress.popProgress();
    }

    
    
    /* private void loadExperiments(int depth)
    {
        Map<TfCellTreatment, ExperimentGroup> result = new HashMap<>();
        for(Experiment e : getExperimentsCollection())
        {
            if(e.getExpType() == ExperimentType.CHIP_CONTROL || e.getExpType() == ExperimentType.BIO_CONTROL)
                continue;
            if(e.getTfClassId() == null)
                continue;
    
            String tf = e.getTfClassId();
            tf = toLowerDepth( tf, depth );
            if(tf == null)
                continue;
            
            String cell = e.getCell().getTitle();
            if(cell == null)
                throw new AssertionError();
    
            String treatment = e.getTreatment();
            if(treatment == null)
                treatment = "";
            
            TfCellTreatment key = new TfCellTreatment( tf, cell, treatment );
            result.computeIfAbsent( key, k->new ExperimentGroup( k.tf ) ).addExperiment( e );
        }
        
        result.forEach( (key, value)->{
            tfClass2ExperimentGroups.computeIfAbsent( key.tf, k->new ArrayList<>() ).add( value );
        } );
    }*/
    
    private @Nonnull DataCollection<ChIPseqExperiment> getExperimentsCollection()
    {
        return DataElementPath.create( "databases/GTRD/Data/experiments" ).getDataCollection(ChIPseqExperiment.class);
    }
    
    public static String toLowerDepth(String tfClass, int depth)
    {
        String[] tfParts = tfClass.split( "[.]" );
        if(tfParts.length < depth)
            return null;
        String[] tfPartsToDepth = new String[depth];
        for(int i = 0; i < depth; i++)
            tfPartsToDepth[i] = tfParts[i];
        return String.join( ".", tfPartsToDepth );
    }
}
