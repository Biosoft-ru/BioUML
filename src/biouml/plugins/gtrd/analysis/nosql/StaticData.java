package biouml.plugins.gtrd.analysis.nosql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.access.BigBedFiles;
import biouml.plugins.gtrd.master.meta.json.Experiments;

public class StaticData
{
    public Experiments exps;

    //Various data organized by organism name
    public Map<String, GenomeInfo> genomeInfo;
    public Map<String, GeneAnnotation> geneAnnotation;
    public Map<String, Set<QueryParams>> possibleQueries = new HashMap<>();
    public Map<String, UniprotEntry> uniprot = new HashMap<>();
    
    private static StaticData instance;

    public static final String MAIN_FOLDER = System.getProperty( "gtrd-nosql-folder", java.lang.System.getProperty("biouml.server.path") + "/gtrd-nosql" );
    
    private StaticData() throws IOException
    {
        exps = Experiments.loadFromJson( Paths.get( MAIN_FOLDER, "experiments") );
        
        genomeInfo = GenomeInfo.loadAll( Paths.get( MAIN_FOLDER, "genome_info.json" ) );
        for(GenomeInfo gi : genomeInfo.values())
            BigBedFiles.setUCSCGenomeBuild( gi.getOrganism(), gi.getGenomeBuildUCSC() );
        
        geneAnnotation = new HashMap<>();
        for(GenomeInfo gi : genomeInfo.values())
        {
            GeneAnnotation ga = new GeneAnnotation();
            ga.loadFromGTF( Paths.get(MAIN_FOLDER, gi.getEnsemblGTF()) );
            geneAnnotation.put( gi.getOrganism(), ga );
        }
        
        
        for(ChIPseqExperiment exp : exps.chipSeqExps.values())
        {
            if(exp.isControlExperiment())
                continue;

            for(String peakCaller : ChIPseqExperiment.PEAK_CALLERS)
            {
                String bbPath = BigBedFiles.getBigBedPathForPeaks( exp, peakCaller );
                File bbFile = Paths.get( MAIN_FOLDER, bbPath ).toFile();
                if(!bbFile.exists())
                    continue;
                QueryParams params = new QueryParams();
                params.dataset = peakCaller + " peaks";
                params.cell = exp.getCell().getTitle();
                params.tf = exp.getTfUniprotId();
                params.treatment = exp.getTreatment();
                possibleQueries.computeIfAbsent( exp.getSpecie().getLatinName(), k->new HashSet<>() ).add( params );
            }
        }
        
        for( ChIPseqExperiment exp : exps.chipSeqExps.values() )
        {
            if( exp.isControlExperiment() )
                continue;
            String organism = exp.getSpecie().getLatinName();
            String uniprot = exp.getTfUniprotId();
            String bbPath = BigBedFiles.getBigBedPathForMetaclusters( uniprot, organism );
            File bbFile = Paths.get( StaticData.MAIN_FOLDER, bbPath ).toFile();
            if( !bbFile.exists() )
                continue;
            QueryParams params = new QueryParams();
            params.dataset = "meta clusters";
            params.tf = uniprot;
            params.cell = exp.getCell().getTitle();
            params.treatment = exp.getTreatment();
            possibleQueries.computeIfAbsent( organism, k->new HashSet<>() ).add( params );
        }
        
        loadUniprot();
    }
    
    private void loadUniprot() throws IOException
    {
        Path path = Paths.get( MAIN_FOLDER, "uniprot.txt.gz" );
        InputStream is = new FileInputStream(path.toFile());
        is = new GZIPInputStream( is );
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
        {
            String line = reader.readLine();//header
            while((line = reader.readLine()) != null)
            {
                String[] parts = line.split( "\t" );
                UniprotEntry e = new UniprotEntry();
                e.id = parts[0];
                e.name = parts[1];
                e.geneName = parts[5];
                e.organism = parts[6];
                uniprot.put( e.id, e );
            }
        }
    }

    public synchronized static StaticData getInstance() throws IOException
    {
        if(instance == null)
            instance = new StaticData();
        return instance;
    }
    
}