package biouml.plugins.gtrd.analysis.nosql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.access.BigBedFiles;
import biouml.plugins.gtrd.master.meta.json.Experiments;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.BigBedFile;
import ru.biosoft.bigbed.BigBedWriter;
import ru.biosoft.bigbed.BigBedWriterOptions;
import ru.biosoft.bigbed.ChromInfo;

//replacement for SearchBindingSites analysis that works directly with bigBed files and metadata in json
public class SBSMain
{
    public File metadataFolder;
    public String bigBedFolder;
    public File ensemblGTF;
    public String chrMappingFile;
    
    public String organism;
    public Optional<String> ensemblGeneId = Optional.empty();
    public Optional<String> tfUniprotId = Optional.empty();
    public String dataSet = "meta clusters";
    public int maxGeneDistance = 5000;
    public Optional<Integer> cellId = Optional.empty();
    public Optional<String> treatment = Optional.empty();
    
    public File outputFile; 
    
    
    public void parseCommandLineArgs(String[] args) throws ParseException
    {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        
        Option opt = new Option("M", "metadata-folder", true, "folder with metadata json files");
        opt.setRequired(true);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        
        opt = new Option("B", "bigbed-folder", true, "folder with bigBed files");
        opt.setRequired(true);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("G", "ensembl-gtf", true, "ensembl gtf file");
        opt.setRequired(false);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("C", "chr-mapping-file", true, "used to translate chr names in GTF to UCSC");
        opt.setRequired(false);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        
        opt = new Option("g", "ensembl-gene", true, "ensembl gene id");
        opt.setRequired(false);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("o", "organism", true, "organism latin name");
        opt.setRequired(true);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("T", "tf", true, "TF uniprot id");
        opt.setRequired(false);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("d", "dataset", true, "Dataset: meta clusters, macs2 peaks, gem peaks, sissrs peaks, pics peaks");
        opt.setRequired(true);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("l", "max-gene-distance", true, "Max distance to gene");
        opt.setRequired(false);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("c", "cell-id", true, "Cell id");
        opt.setRequired(false);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        opt = new Option("t", "treatment", true, "Treatment");
        opt.setRequired(false);
        opt.setOptionalArg(false);
        options.addOption(opt);

        
        opt = new Option("O", "output", true, "output bigBed file");
        opt.setRequired(true);
        opt.setOptionalArg(false);
        options.addOption(opt);
        
        
        CommandLine parsed = parser.parse(options, args);

        String[] cmd = parsed.getArgs();
        
        String metadataFolderArg = parsed.getOptionValue( 'M' );
        metadataFolder = new File(metadataFolderArg);
        
        bigBedFolder = parsed.getOptionValue( 'B' );
        
        String ensemblGTFArg = parsed.getOptionValue( 'G' );
        if(ensemblGTFArg != null)
            ensemblGTF = new File(ensemblGTFArg);
        
        chrMappingFile = parsed.getOptionValue( 'C' );
        
        String ensemblGeneIdArg = parsed.getOptionValue( 'g' );
        if(ensemblGeneIdArg != null)
            ensemblGeneId = Optional.of( ensemblGeneIdArg );

        organism = parsed.getOptionValue( 'o' );
        
        String tfUniprotIdArg = parsed.getOptionValue( 'T' );
        if(tfUniprotIdArg != null)
            tfUniprotId = Optional.of( tfUniprotIdArg );
        
        dataSet = parsed.getOptionValue( 'd' );
        
        String maxGeneDistanceArg = parsed.getOptionValue( 'l' );
        if(maxGeneDistanceArg != null)
            maxGeneDistance = Integer.parseInt( maxGeneDistanceArg );
        
        
        String cellIdArg = parsed.getOptionValue( 'c' );
        if(cellIdArg != null)
            cellId = Optional.of( Integer.parseInt( cellIdArg ) );
        
        String treatmentArg = parsed.getOptionValue( 't' );
        if(treatmentArg != null)
            treatment = Optional.of( treatmentArg );
        
        outputFile = new File(parsed.getOptionValue( 'O' ));
    }
    
    public void run() throws Exception
    {
        System.out.println("Loading metadata from json files");
        long time = System.currentTimeMillis();
        Experiments exps = Experiments.loadFromJson( metadataFolder.toPath() );
        System.out.println("Done in " +(System.currentTimeMillis() - time) + "ms");
        
        Location loc = null;
        if(ensemblGeneId.isPresent())
        {
            
            System.out.println("Loading gene annotation from " + ensemblGTF);
            time = System.currentTimeMillis();
            loc = getGeneLocation();
            System.out.println("Done in " +(System.currentTimeMillis() - time) + "ms");
            
            if(chrMappingFile != null)
                loc.chr = translateChrNameToUCSC( loc.chr );
        }
        
        
        if(dataSet.endsWith( " peaks" ))
        {
            System.out.println("Querying bigBed files");
            time = System.currentTimeMillis();
            
            List<BedEntry> result = new ArrayList<>();
            List<ChromInfo> chroms = null;
            for(ChIPseqExperiment e : exps.chipSeqExps.values())
            {
                if(e.isControlExperiment())
                    continue;
                if(!e.getSpecie().getLatinName().equals( organism ))
                    continue;
                if(tfUniprotId.isPresent() && !e.getTfUniprotId().equals( tfUniprotId.get() ))
                    continue;
                if(cellId.isPresent() && !e.getCell().getName().equals( cellId.get().toString() ))
                    continue;
                if(treatment.isPresent() && !treatment.get().equals( e.getTreatment() ))
                    continue;
                String peakCaller = dataSet.substring( 0, dataSet.length() -" peaks".length() );
                String bbPath = BigBedFiles.getBigBedPathForPeaks( e, peakCaller );
                bbPath = bbPath.replaceAll( "^/bigBeds", "" );
                File bbFile = new File(bigBedFolder + bbPath);
                if(!bbFile.exists())
                    continue;
                System.err.println("Processing file " + bbFile.getAbsolutePath());
                BigBedFile bb = BigBedFile.read(bbFile.getAbsolutePath());
                if(chroms == null) {
                	chroms = new ArrayList<>();
                	bb.traverseChroms(chroms::add);
                }
                if(ensemblGeneId.isPresent())
                {
                    List<BedEntry> entries = bb.queryIntervals( loc.chr, (loc.from-1)-maxGeneDistance, loc.to + maxGeneDistance, 0 );
                    result.addAll( entries );
                }
                else
                {
                    for(ChromInfo chr : chroms)
                    {
                        List<BedEntry> entries = bb.queryIntervals( chr.id, 0, chr.length, 0 );
                        result.addAll( entries );
                    }
                }
                System.err.println("Current size of result: " + result.size());
            }
            if(chroms == null)
            {
                Files.write( outputFile.toPath(), new byte[0] );
                return;
            }
            chroms.sort(Comparator.comparingInt(chr->chr.id));
            System.out.println("Done in " +(System.currentTimeMillis() - time) + "ms");
            System.out.println("Found " + result.size() + " sites");
            
            System.out.println("Writing results");
            time = System.currentTimeMillis();
            BigBedWriterOptions options = new BigBedWriterOptions();
			BigBedWriter.write(result, chroms, outputFile, options);
            System.out.println("Done in " +(System.currentTimeMillis() - time) + "ms");
        }else if(dataSet.equals( "meta clusters" ))
        {
            
        }else
            throw new Exception("Invalid dataSet: " + dataSet);
        
    }
    
    static class Location {
        String chr;
        int from, to;//one based both inclusive
    }
    
    private Location getGeneLocation() throws IOException
    {
        Location res = new Location();
        InputStream is = new FileInputStream(ensemblGTF);
        if(ensemblGTF.getName().endsWith( ".gz" ))
            is = new GZIPInputStream( is );
        try (BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                if( line.startsWith( "#" ) )
                    continue;
                String[] parts = line.split( "\t" );
                if( !parts[2].equals( "gene" ) )
                    continue;
                for( String prop : parts[8].split( ";" ) )
                {
                    prop.trim();
                    if( prop.isEmpty() )
                        continue;
                    String[] kv = prop.split( " ", 2 );
                    kv[1] = kv[1].replace( "\"", "" );
                    if( kv[0].equals( "gene_id" ) && kv[1].equals( ensemblGeneId.get() ) )
                    {
                        res.chr = parts[0];
                        res.from = Integer.parseInt(parts[3]);
                        res.to = Integer.parseInt(parts[4]);
                        return res;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Gene not found: " + ensemblGeneId.get());
    }
    
    private String translateChrNameToUCSC(String ensemblChr) throws IOException
    {
        if(chrMappingFile == null)
            return ensemblChr;
        BufferedReader reader = new BufferedReader( new FileReader( chrMappingFile ) );
        String line;
        while( ( line = reader.readLine() ) != null )
        {
            String[] parts = line.split( "\t" );
            String ucsc = parts[0];
            String ensembl = parts[1];
            if(ensemblChr.equals( ensembl ))
                return ucsc;
        }
        throw new IllegalArgumentException("Unknown chromosome: " + ensemblChr);
    }

    public void validateParameters()
    {
        if(!metadataFolder.exists())
            throw new IllegalArgumentException("No metadata folder");
        if(!Files.exists( Paths.get(bigBedFolder) ))
            throw new IllegalArgumentException("No BigBed folder");
        if(ensemblGeneId.isPresent())
        {
            if(ensemblGTF == null || !ensemblGTF.exists())
                throw new IllegalArgumentException("No GTF file");
        }
           
    }
    
    public static void main(String[] args) throws Exception
    {
        SBSMain main = new SBSMain();
        main.parseCommandLineArgs( args );
        main.run();
    }
}
