package biouml.plugins.gtrd.analysis.nosql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import biouml.plugins.ensembl.access.EnsemblDatabaseSelector;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.access.BigBedFiles;
import biouml.plugins.gtrd.master.sites.bedconv.BedEntryToPeak;
import biouml.standard.type.Species;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.BigBedFile;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bsa.ChrNameMapping;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Position;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.importer.TrackImportProperties;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.bsa.track.big.BigBedTrackImporter;
import ru.biosoft.bsa.view.DefaultTrackViewBuilder;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

//replacement for SearchBindingSites analysis that works directly with bigBed files and metadata in json
public class SBSAnalysis extends AnalysisMethodSupport<SBSAnalysis.Parameters>
{
    public SBSAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
            jobControl.pushProgress( 0, 80 );
            File intervalFile = searchBindingSites();
            jobControl.popProgress();
            
            jobControl.pushProgress( 80, 100 );
            if(parameters.getOutputType().equals( Parameters.OUTPUT_TYPE_OPEN_IN_GENOME_BROWSER ))
            {
                File bbFile = convertIntervalToBigBed(intervalFile);
                BigBedTrack<?> track = createBBTrack(bbFile);
                jobControl.popProgress();
                return track;
            }
            FileDataElement fileDE = createFileDE(intervalFile);
            jobControl.popProgress();
            return fileDE;
    }
    
    private File convertIntervalToBigBed(File intervalFile) throws IOException, InterruptedException
    {
        File sortedFile = sortFile(intervalFile);
        File bbFile = TempFiles.file( ".bb" );
        StaticData sd = StaticData.getInstance();
        GenomeInfo genomeInfo = sd.genomeInfo.get( parameters.getOrganism() );
        String[] cmd = {"bedToBigBed", 
                "-type=bed3+" + getExtraColumnCount(),
                "-as=" + getAutosql(),
                sortedFile.getAbsolutePath(),
                Paths.get(StaticData.MAIN_FOLDER, genomeInfo.getChrSizesFileUCSC()).toString(),
                bbFile.getAbsolutePath()};
        Process proc = Runtime.getRuntime().exec( cmd );
        int exitCode = proc.waitFor();
        if(exitCode != 0)
            throw new RuntimeException("bedToBigBed exit with " + exitCode);
        return bbFile;
    }
    
    private int getExtraColumnCount()
    {
        switch(parameters.getDataset())
        {
            case "macs2 peaks": return 8;
            case "sissrs peaks": return 6;
            case "gem peaks": return 12;
            case "pics peaks": return 4;
            case "meta clusters": return 1;
            default:
                throw new AssertionError();
        }
    }
    
    private String getAutosql()
    {
        String fileName;
        switch(parameters.getDataset())
        {
            case "macs2 peaks": fileName="macs2_template.as";break;
            case "sissrs peaks": fileName="sissrs_template.as";break;
            case "gem peaks": fileName="gem_template.as";break;
            case "pics peaks": fileName="pics_template.as";break;
            case "meta clusters": fileName="metaclusters_template.as";break;
            default:
                throw new AssertionError();
        }
        return Paths.get(StaticData.MAIN_FOLDER, "autosql", fileName).toString();

    }

    private File sortFile(File intervalFile) throws IOException, InterruptedException
    {
        File sortedFile = TempFiles.file( ".sorted.interval" );
        String[] cmd = {"sort", "-k1,1", "-k2,2n", intervalFile.getAbsolutePath(), "-o", sortedFile.getAbsolutePath()};
        Process proc = Runtime.getRuntime().exec( cmd );
        int exitCode = proc.waitFor();
        if(exitCode != 0)
            throw new RuntimeException("bedToBigBed exit with " + exitCode);
        return sortedFile;
    }

    private FileDataElement createFileDE(File intervalFile) throws Exception
    {
        String name = parameters.getDataset() + "_" + parameters.getOrganism();
        if(!"Any".equals( parameters.getTf() ))
            name += "_" + parameters.getUniprotId();
        name = name + ".interval";
        
        DataElementPath path = parameters.getResultingSites().getSiblingPath( name ).uniq();
        name = path.getName();
        
        DataCollection<? extends DataElement> parent = path.optParentCollection();

        
        File fileInRepo = DataCollectionUtils.getChildFile( parent, name );
        ApplicationUtils.linkOrCopyFile(fileInRepo, intervalFile, null);
        
        FileDataElement result = new FileDataElement(name, parent, fileInRepo);
        
        path.save( result );
        path.getParentCollection().release( path.getName() );
        return result;
    }
    
    private BigBedTrack<?> createBBTrack(File bbFile) throws Exception
    {
        DataElementPath resPath = parameters.getResultingSites().uniq();
        BigBedTrackImporter importer = new BigBedTrackImporter();
        
        
        TrackImportProperties importerParams = importer.getProperties( resPath.getParentCollection(), bbFile, resPath.getName() );
        Properties properties = importerParams.getTrackProperties();
        
        //Use type specific converter
        properties.setProperty( BigBedTrack.PROP_CONVERTER_CLASS, BedEntryWithStableIdConverter.class.getName() );
        
        GenomeInfo gi = StaticData.getInstance().genomeInfo.get( parameters.getOrganism() );
        
        properties.setProperty( ChrNameMapping.PROP_CHR_MAPPING, gi.getChrNameMappingUCSCToEnsembl() );
        
        Species organism = Species.getSpecies( parameters.getOrganism() );
        try
        {
            EnsemblDatabase ensembl = EnsemblDatabaseSelector.getDefaultEnsembl( organism );
            properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, ensembl.getPrimarySequencesPath().toString() );
        }
        catch(Exception e)
        {
            log.log( Level.WARNING, "Can not find chromosomes", e );
        }
        properties.setProperty( BedEntryToPeak.PROP_EXPERIMENTS_PATH, "databases/GTRD/Data/experiments" );
        
        properties.setProperty( BigBedTrack.PROP_VIEW_BUILDER_CLASS, DefaultTrackViewBuilder.class.getName() );
        
        BigBedTrack<? extends Site> resultingTrack = (BigBedTrack<? extends Site>)importer.doImport( resPath.getParentCollection(), bbFile, resPath.getName(), null, log );
        
        //Add gene track
        DataElementPath ensemblPath = resultingTrack.getGenomeSelector().getDbSelector().getBasePath();
        DataElementPath genesTrackPath = ensemblPath.getChildPath( "Tracks", "Genes" );
        resultingTrack.getInfo().getProperties().setProperty( Track.OPEN_WITH_TRACKS, genesTrackPath.toString() );
        
        //Set genome browser position
        Position position = null;
        if( geneLocation == null)
        {
            Site s = findAnySite(resultingTrack);
            if(s != null)
                position = new Position( s );
        }
        else
        {
            position = new Position(geneLocation.chr, new Interval( geneLocation.from, geneLocation.to ));    
        }
        if(position != null)
            resultingTrack.getInfo().getProperties().setProperty(Track.DEFAULT_POSITION_PROPERTY, position.toString());
        
        return resultingTrack;
    }

    private Site findAnySite(BigBedTrack<? extends Site> resultingTrack) throws IOException
    {
        for(String chr : resultingTrack.getChromosomes())
        {
            int chrLen = resultingTrack.getChromSizes().get( chr );
            int l = 10000;
            do {
                l *= 2;
                for(Site s : resultingTrack.query( chr, 0, l ))
                    return s;
            }while(l < chrLen);
        }
        return null;
    }

    private File searchBindingSites() throws Exception
    {
        File intervalFile = TempFiles.file( ".interval" );
        
        findGene();
        GeneLocation geneLocUCSC = geneLocation;
        if(geneLocation != null)
            geneLocUCSC = translateChrNameToUCSC(geneLocation);
        searchBindingSitesNearGene( geneLocUCSC, intervalFile );

        return intervalFile;
    }

   
    private GeneLocation translateChrNameToUCSC(GeneLocation geneLocation) throws IOException
    {
        GenomeInfo genomeInfo = StaticData.getInstance().genomeInfo.get( parameters.getOrganism() );
        ChrNameMapping mapping = ChrNameMapping.getMapping( genomeInfo.getChrNameMappingUCSCToEnsembl() );
        String ensemblChr = geneLocation.chr;
        String ucscChr = mapping.dstToSrc( ensemblChr );
        GeneLocation res = (GeneLocation)geneLocation.clone();
        res.chr = ucscChr;
        return res;
    }

    private String currentTF;
    private void searchBindingSitesNearGene(GeneLocation gene, File intervalFile) throws Exception
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(intervalFile));
        //log.log( Level.INFO, "Querying bigBed files");
        long time = System.currentTimeMillis();
        int size = 0;
        Collection<ChIPseqExperiment> exps = StaticData.getInstance().exps.chipSeqExps.values();
        if(!parameters.isClusters())
        {//peaks
            int i = 0;
            String peakCaller = parameters.getDataset().substring( 0, parameters.getDataset().length() -" peaks".length() );
            for(ChIPseqExperiment e : exps)
            {
                jobControl.setPreparedness( (i++)*100/exps.size() );
                if(e.isControlExperiment())
                    continue;
                if(!e.getSpecie().getLatinName().equals( parameters.getOrganism() ))
                    continue;
                if(!parameters.getTf().equals( "Any" ) && !e.getTfUniprotId().equals( parameters.getUniprotId() ))
                    continue;
                if(!parameters.getCellLine().equals( "Any" ) && !e.getCell().getTitle().equals( parameters.getCellLine() ))
                    continue;
                if(!parameters.getTreatment().equals("Any") && !parameters.getTreatment().equals( e.getTreatment() ))
                    continue;
                currentTF = e.getTfUniprotId();
                
                String bbPath = BigBedFiles.getBigBedPathForPeaks( e, peakCaller );
                File bbFile = Paths.get( StaticData.MAIN_FOLDER, bbPath ).toFile();
                size += processBigBedFile(bbFile, gene, e, writer);
            }
        }else//metaclusters
        {
            if(parameters.getTf().equals( "Any" ))
            {
                Set<String> tfSet = exps.stream()
                        .filter( e->e.getSpecie().getLatinName().equals( parameters.getOrganism() ) )
                        .map( e->e.getTfUniprotId() )
                        .collect( Collectors.toSet() );
                int i = 0;
                for(String tf : tfSet)
                {
                    jobControl.setPreparedness( (i++)*100/tfSet.size() );
                    currentTF= tf;
                    String bbPath = BigBedFiles.getBigBedPathForMetaclusters( tf, parameters.getOrganism() );
                    File bbFile = Paths.get( StaticData.MAIN_FOLDER, bbPath ).toFile(); 
                    size += processBigBedFile( bbFile, gene, null, writer );
                }
                
            }else
            {
                currentTF = parameters.getUniprotId();
                String bbPath = BigBedFiles.getBigBedPathForMetaclusters( parameters.getUniprotId(), parameters.getOrganism() );
                File bbFile = Paths.get( StaticData.MAIN_FOLDER, bbPath ).toFile(); 
                size += processBigBedFile( bbFile, gene, null, writer );
            }
        }
        writer.close();
        //log.info("Done in " +(System.currentTimeMillis() - time) + "ms");
        //log.info("Found " + size + " sites");

    }

    private int processBigBedFile(File bbFile, GeneLocation gene, ChIPseqExperiment e, BufferedWriter writer) throws IOException
    {
        if(!bbFile.exists())
            return 0;
        //log.info("Processing file " + bbFile.getAbsolutePath());
        BigBedFile bb = BigBedFile.read(bbFile.getAbsolutePath());
        int size = 0;
        if(gene != null)
        {
            for(BedEntry entry : bb.queryIntervals( gene.chr, (gene.from-1)-parameters.getMaxGeneDistance(), gene.to + parameters.getMaxGeneDistance(), 0 ))
            {
                String line = fixBedEntry(entry, e, gene.chr);
                writer.append( line ).append( '\n' );
                size++;
            }
        }
        else
        {
        	List<ChromInfo> chrList = new ArrayList<>();
        	bb.traverseChroms(chrInfo->{
        		chrList.add(chrInfo);
        	});
        	for(ChromInfo chr : chrList)
            {
                for(BedEntry entry : bb.queryIntervals( chr.id, 0, chr.length, 0 ))
                {
                    String line = fixBedEntry(entry, e, chr.name);
                    writer.append( line ).append( '\n' );
                    size++;
                }
            }
        }
        return size;
    }

    private String fixBedEntry(BedEntry entry, ChIPseqExperiment e, String chr) throws IOException
    {
        String[] values = entry.getRest().split( "\t" );
        if(parameters.getDataset().equals( "sissrs peaks" ) && values.length == 4)
        {
            String[] values2 = new String[6];
            values2[0] = values[0];
            values2[1] = values[1];
            values2[2] = values[2];
            values2[3] = "NaN";//fold_enrichment
            values2[4] = "NaN";//p_value
            values2[5] = values[3];
            values = values2;
        }

        if(parameters.getDataset().endsWith( " peaks" ))
        {
            String peakCaller = parameters.getDataset().split( " " )[0];
            //replace id number with full id, since result can be mix of experiments
            int idCol = values.length - 1;
            values[idCol] = "p." + e.getName() + "." + peakCaller + "." + values[idCol];
        }
        
        if(parameters.getDataset().equals( "meta clusters" ))
        {
            String id = values[values.length-1];
            //TODO: set correct version
            //TODO: source bigBed should contain stable id, then this fix will be unnecessary
            UniprotEntry uniprot = StaticData.getInstance().uniprot.get( currentTF );
            String uniprotName = uniprot.name;
            String tfName = uniprot.geneName;
            
            values = Arrays.copyOf( values, values.length+1 );
            values[values.length - 2] = tfName;
            
            values[values.length - 1] = "ms." + uniprotName + "." + id + ".v1";
            
            
        }
        
        return chr + "\t" + entry.start + "\t" + entry.end + "\t" + String.join( "\t", values );
    }

    private GeneLocation geneLocation;
    public GeneLocation findGene() throws IOException
    {
        String gene = parameters.getGene();
        if(gene.isEmpty() || gene.equals( "Any" ))
            return null;
        
        GeneAnnotation geneAnnotation = StaticData.getInstance().geneAnnotation.get( parameters.getOrganism() );
        geneLocation = geneAnnotation.byEnsGeneId.get( gene );
        if(geneLocation != null)
            return geneLocation;
        
        geneLocation = geneAnnotation.byGeneSymbol.get( gene );
        if(geneLocation != null)
            return geneLocation;
        
        throw new IllegalArgumentException("Gene " + gene + " not found");
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
    
    public static class Parameters extends AbstractAnalysisParameters
    {
        private String organism = "Homo sapiens";
        @PropertyName("Organism")
        @PropertyDescription("Organism")
        public String getOrganism()
        {
            return organism;
        }
        public void setOrganism(String organism)
        {
            String oldValue = this.organism;
            this.organism = organism;
            firePropertyChange( "organism", oldValue, organism );
            setCellLine( "Any" );
            setTreatment( "Any" );
            setTf( "Any" );
        }

        private String gene = "Any";
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
        
        private String tf = "Any";
        @PropertyName( "Transcription factor" )
        @PropertyDescription( "Transcription factor" )
        public String getTf()
        {
            return tf;
        }
        public void setTf(String tf)
        {
            Object oldValue = this.tf;
            this.tf = tf;
            firePropertyChange( "tf", oldValue, tf );
        }
        public String getUniprotId()
        {
            return TextUtil2.split( getTf(), ' ' )[1];
        }
        
        private String dataSet = "meta clusters";
        @PropertyName( "Data set" )
        @PropertyDescription( "Data set" )
        public String getDataset()
        {
            return dataSet;
        }
        public void setDataset(String dataSet)
        {
            Object oldValue = this.dataSet;
            this.dataSet = dataSet;
            firePropertyChange( "dataSet", oldValue, dataSet );
        }
        public boolean isClusters()
        {
            return dataSet.contains( "clusters" );
        }
        
        private String cellLine = "Any", treatment = "Any";
        @PropertyName( "Cell line" )
        @PropertyDescription( "Cell line" )
        public String getCellLine()
        {
            return cellLine;
        }

        public void setCellLine(String cellLine)
        {
            Object oldValue = this.cellLine;
            this.cellLine = cellLine;
            firePropertyChange( "cellLine", oldValue, cellLine );
        }

        @PropertyName( "Treatment" )
        @PropertyDescription( "Treatment/condition" )
        public String getTreatment()
        {
            return treatment;
        }

        public void setTreatment(String treatment)
        {
            Object oldValue = this.treatment;
            this.treatment = treatment;
            firePropertyChange( "treatment", oldValue, treatment );
        }
        

        int maxGeneDistance = 5000;
        @PropertyName ( "Max gene distance" )
        @PropertyDescription ( "Maximal distance from site to gene" )
        public int getMaxGeneDistance()
        {
            return maxGeneDistance;
        }

        public void setMaxGeneDistance(int maxGeneDistance)
        {
            Object oldValue = this.maxGeneDistance;
            this.maxGeneDistance = maxGeneDistance;
            firePropertyChange( "maxGeneDistance", oldValue, maxGeneDistance );
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



        private DataElementPath resultingSites = DataElementPath.create( "data/Collaboration/Demo/tmp/TF binding sites" );
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
    
    public static class ParametersBeanInfo extends BeanInfoEx2
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            StaticData sd = StaticData.getInstance();
            property("organism").tags( sd.genomeInfo.keySet().toArray(new String[0]) ).add();
            property("gene").add();
            property("tf").editor( TFSelector.class ).add();
            property("dataset").editor( DatasetSelector.class ).add();
            addHidden( "cellLine", CellLineSelector.class, "isClusters" );
            addHidden( "treatment", TreatmentSelector.class, "isClusters" );
            add( "maxGeneDistance" );
            
            property("outputType").tags( Parameters.OUTPUT_TYPE_OPEN_IN_GENOME_BROWSER, Parameters.OUTPUT_TYPE_DOWNLOAD_FILE ).add();
            property("resultingSites").outputElement( Track.class ).expert().add();
        }
        
    }
    
    public static class TFSelector extends GenericComboBoxEditor
    {
        @Override
        protected synchronized Object[] getAvailableValues()
        {
            try
            {
                Set<String> tfSet = new HashSet<>();
                Parameters parameters = (Parameters)getBean();
                Set<QueryParams> queries = StaticData.getInstance().possibleQueries.get( parameters.getOrganism() );
                if(queries == null)
                    return new Object[0];
                for(QueryParams qp :  queries)
                {
                    if(!parameters.getDataset().equals( qp.dataset ))
                        continue;
                    if( !parameters.getCellLine().equals( "Any" ) && !parameters.getCellLine().equals( qp.cell ) )
                        continue;
                    if( !parameters.getTreatment().equals( "Any" ) && !parameters.getTreatment().equals( qp.treatment ) )
                        continue;
                    tfSet.add( qp.tf );
                }
                String[] result = new String[tfSet.size() + 1];
                result[0] = "Any";
                int i = 1;
                Map<String, UniprotEntry> uniprotTable = StaticData.getInstance().uniprot;
                for(String tf : tfSet)
                {
                    UniprotEntry e = uniprotTable.get( tf );
                    if(e != null && e.geneName != null && !e.geneName.isEmpty())
                        tf = e.geneName + " " + tf;
                    result[i++] = tf;
                }
                Arrays.sort( result, 1, result.length );
                return result;
            }
            catch( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }
    public static class CellLineSelector extends GenericComboBoxEditor
    {
        @Override
        protected synchronized Object[] getAvailableValues()
        {
            try
            {
                Set<String> cellSet = new HashSet<>();
                Parameters parameters = (Parameters)getBean();
                Set<QueryParams> queries = StaticData.getInstance().possibleQueries.get( parameters.getOrganism() );
                if(queries == null)
                    return new Object[0];
                for(QueryParams qp :  queries)

                {
                    if(!parameters.getDataset().equals( qp.dataset ))
                        continue;
                    if(!parameters.getTf().equals( "Any" ) && !parameters.getUniprotId().equals( qp.tf ))
                        continue;
                    if(!parameters.getTreatment().equals( "Any" ) && !parameters.getTreatment().equals( qp.treatment ))
                        continue;
                    cellSet.add( qp.cell );
                }
                String[] result = new String[cellSet.size() + 1];
                result[0] = "Any";
                int i = 1;
                for(String cell : cellSet)
                    result[i++] = cell;
                Arrays.sort(result, 1, result.length);
                return result;
            }
            catch( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }
    public static class TreatmentSelector extends GenericComboBoxEditor
    {
        @Override
        protected synchronized Object[] getAvailableValues()
        {
            try
            {
                Set<String> treatmentSet = new HashSet<>();
                Parameters parameters = (Parameters)getBean();
                Set<QueryParams> queries = StaticData.getInstance().possibleQueries.get( parameters.getOrganism() );
                if(queries == null)
                    return new Object[0];
                for(QueryParams qp :  queries)
                {
                    if(!parameters.getDataset().equals( qp.dataset ))
                        continue;
                    if(!parameters.getTf().equals( "Any" ) && !parameters.getUniprotId().equals( qp.tf ))
                        continue;
                    if(!parameters.getCellLine().equals( "Any" ) && !parameters.getCellLine().equals( qp.cell ))
                        continue;
                    treatmentSet.add( qp.treatment );
                }
                String[] result = new String[treatmentSet.size() + 1];
                result[0] = "Any";
                int i = 1;
                for(String t : treatmentSet)
                    result[i++] = t;
                Arrays.sort( result, 1, result.length );
                return result;
            }
            catch( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }
    
    public static class DatasetSelector extends GenericComboBoxEditor
    {
        @Override
        protected synchronized Object[] getAvailableValues()
        {
            try
            {
                Set<String> datasets = new HashSet<>();
                Parameters parameters = (Parameters)getBean();
                Set<QueryParams> queries = StaticData.getInstance().possibleQueries.get( parameters.getOrganism() );
                if(queries == null)
                    return new Object[0];
                for(QueryParams qp :  queries)

                {
                    if(!parameters.getTreatment().equals( "Any" ) && !parameters.getTreatment().equals( qp.treatment ))
                        continue;
                    if(!parameters.getTf().equals( "Any" ) && !parameters.getUniprotId().equals( qp.tf ))
                        continue;
                    if(!parameters.getCellLine().equals( "Any" ) && !parameters.getCellLine().equals( qp.cell ))
                        continue;
                    datasets.add( qp.dataset );
                }
                return datasets.toArray( new String[0] );
            }
            catch( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }


}
