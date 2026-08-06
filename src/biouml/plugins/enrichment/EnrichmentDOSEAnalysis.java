package biouml.plugins.enrichment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import one.util.streamex.StreamEx;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftFileNotFoundException;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.SubFunctionJobControl;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.NullImportProperties;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.export.TableElementExporter;
import ru.biosoft.table.export.TableElementExporter.TableExporterProperties;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

@ClassIcon("resources/enrichment-analysis.gif")
public class EnrichmentDOSEAnalysis extends FunctionalAnalysisSupport<EnrichmentAnalysisParameters>
{
    private static final Map<String, String> SPECIES_TO_PACKAGE = new HashMap<>();

    static
    {
        // Mammals
        SPECIES_TO_PACKAGE.put( "homo sapiens", "org.Hs.eg.db" );
        SPECIES_TO_PACKAGE.put( "mus musculus", "org.Mm.eg.db" );
        SPECIES_TO_PACKAGE.put( "rattus norvegicus", "org.Rn.eg.db" );
        SPECIES_TO_PACKAGE.put( "bos taurus", "org.Bt.eg.db" );
        SPECIES_TO_PACKAGE.put( "canis lupus familiaris", "org.Cf.eg.db" );
        SPECIES_TO_PACKAGE.put( "sus scrofa", "org.Ss.eg.db" );
        SPECIES_TO_PACKAGE.put( "felis catus", "org.Fc.eg.db" );
        SPECIES_TO_PACKAGE.put( "equus caballus", "org.Ec.eg.db" );
        SPECIES_TO_PACKAGE.put( "ovis aries", "org.Oa.eg.db" );
        SPECIES_TO_PACKAGE.put( "macaca mulatta", "org.Mmu.eg.db" );
        SPECIES_TO_PACKAGE.put( "pan troglodytes", "org.Pt.eg.db" );

        // Birds, Fish, Amphibians, Insects, Nematodes
        SPECIES_TO_PACKAGE.put( "gallus gallus", "org.Gg.eg.db" );
        SPECIES_TO_PACKAGE.put( "danio rerio", "org.Dr.eg.db" );
        SPECIES_TO_PACKAGE.put( "xenopus laevis", "org.Xl.eg.db" );
        SPECIES_TO_PACKAGE.put( "drosophila melanogaster", "org.Dm.eg.db" );
        SPECIES_TO_PACKAGE.put( "caenorhabditis elegans", "org.Ce.eg.db" );

        // Fungi and Plants
        SPECIES_TO_PACKAGE.put( "saccharomyces cerevisiae", "org.Sc.sgd.db" );
        SPECIES_TO_PACKAGE.put( "arabidopsis thaliana", "org.At.tair.db" );
        SPECIES_TO_PACKAGE.put( "oryza sativa", "org.Os.eg.db" );
    }

    /**
     * Returns the Bioconductor R annotation package name for a given scientific name.
     *
     * @param latinName The scientific latin name of the species (e.g., "Homo sapiens").
     * @return The R package name (e.g., "org.Hs.eg.db"), or null if not found.
     */
    public static String getRPackage(String latinName)
    {
        if( latinName == null )
        {
            return null;
        }
        String normalizedKey = latinName.trim().toLowerCase();
        normalizedKey = normalizedKey.replaceAll( "\\s+", " " );

        return SPECIES_TO_PACKAGE.get( normalizedKey );
    }

    public EnrichmentDOSEAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new EnrichmentAnalysisParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
        TableDataCollection source = getParameters().getSource();
        if(source == null)
            throw new IllegalArgumentException("Please specify source table");
        String columnName = getParameters().getColumnName();
        if(columnName == null || columnName.equals(""))
            throw new IllegalArgumentException("Please specify column name");
        int columnIndex = source.getColumnModel().optColumnIndex(columnName);
        if(columnIndex == -1)
            throw new IllegalArgumentException("Specified column not found");
        //heckRange("permutationsCount", 10, 10000);
    }
    
    private File exportTable(DataElementPath tablePath, String[] columnNames, FunctionJobControl jc) throws IOException
    {
        TableElementExporter exporter = new TableElementExporter();
        Properties properties = new Properties();
        properties.setProperty( DataElementExporterRegistry.SUFFIX, "txt" );
        exporter.init( properties );

        TempFile file = TempFiles.file( "dose_input.txt" );
        try
        {
            TableDataCollection table = tablePath.getDataElement( TableDataCollection.class );
            TableExporterProperties exportParameters = (TableExporterProperties) exporter.getProperties( table, file );
            Column[] columns = StreamEx.of( columnNames ).map( name -> new Column( null, name ) ).toArray( Column[]::new );
            exportParameters.setColumns( columns );
            exporter.doExport( table, file, jc );
        }
        catch (Exception e)
        {
            file.delete();
            throw ExceptionRegistry.translateException( e );
        }
        return file;
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        TableDataCollection source = getParameters().getSource();
        String columnName = getParameters().getColumnName();

        File inputFile = exportTable( source.getCompletePath(), new String[] { columnName }, new SubFunctionJobControl( jobControl ) );

        File imageFile = TempFiles.file( "dose_output.png" );

        String rScript = getRScript( inputFile, imageFile );
        log.log( Level.FINE, rScript );
        final LogScriptEnvironment env = new LogScriptEnvironment( log );

        SecurityManager.runPrivileged( () -> ScriptTypeRegistry.execute( "R", rScript, env, false ) );

        TableDataCollection result = importTable( imageFile, parameters.getOutputTable(), new SubFunctionJobControl( jobControl ) );
        result.getColumnModel().getColumn( "core_enrichment" ).setType( DataType.fromClass( StringSet.class ) );

        return result;
    }

    private TableDataCollection importTable(File file, DataElementPath path, FunctionJobControl jc) throws Exception
    {
        if( !file.exists() )
            throw new BiosoftFileNotFoundException( file );
        DataElementImporter importer = new TableCSVImporter();
        DataCollection<DataElement> outputDC = path.getParentCollection();
        if( importer.accept( outputDC, file ) == DataElementImporter.ACCEPT_UNSUPPORTED )
            throw new Exception( "Can not import into selected collection" );
        NullImportProperties importParameters = (NullImportProperties) importer.getProperties( outputDC, file, path.getName() );
        importParameters.setColumnForID( "id" ); // It seems that this is useless. From DESeq2 a column named "id" was not used as ID column upon import
        importParameters.setHeaderRow( 1 );
        importParameters.setDataRow( 2 );
        return (TableDataCollection) importer.doImport( outputDC, file, path.getName(), jc, log );
    }

    private String getRScript(File inputFile, File outputFile)
    {
        StringBuilder script = new StringBuilder();
        String dbName = getRPackage( parameters.getSpecies().getLatinName() );
        script.append( "suppressPackageStartupMessages({\n"
                + "  library(DOSE)\n"
                + "  library(clusterProfiler)\n"
                + "  library("+dbName+")\n"
                + "})\n");
        script.append( "input_file  <- \""+inputFile.getAbsolutePath()+"\"\n");
        
        script.append( "output_file <- \""+outputFile.getAbsolutePath()+"\"\n");
        script.append( "gene_df <- read.delim(input_file, header = TRUE, stringsAsFactors = FALSE)\n"
                + "colnames(gene_df) <- c(\"GeneID\", \"Score\")\n"
                + "gene_list <- gene_df$Score\n"
                + "names(gene_list) <- gene_df$GeneID\n"
                + "gene_list <- gene_list[!is.na(gene_list)]\n"
                + "gene_list <- sort(gene_list, decreasing = TRUE)\n"
                + "cat(\"Running GSEA with DOSE...\\n\")\n"
                + "gsea_res <- gseGO(gene_list, \n"
                + "              OrgDb = " + dbName + ",\n"
                + "              keyType = \"ENSEMBL\",\n"
                + "                  pvalueCutoff = " + parameters.getPvalueThreshold() + ",\n"
                + "                  pAdjustMethod = \"BH\")  # Benjamini-Hochberg FDR correction\n"
                + "\n"
                + "cat(\"GSEA completed.\\n\")\n"
                + "res_df <- as.data.frame(gsea_res)\n"
                + "required_cols <- c(\"ID\", \"Description\", \"setSize\", \"enrichmentScore\", \"NES\", \n"
                + "                   \"pvalue\", \"p.adjust\", \"leading_edge\", \"rank\", \"core_enrichment\")\n"
                + "final_df <- res_df[, intersect(required_cols, colnames(res_df))]\n"
                + "final_df$core_enrichment[is.na(final_df$core_enrichment)] <- \"\"\n"
                + "final_df$core_enrichment <- sapply(strsplit(final_df$core_enrichment, \"/\"), function(x) {\n"
                + "  if (length(x) == 1 && x == \"\") return(\"[]\")\n"
                + "  paste0(\"[\\\"\", paste(x, collapse = \"\\\",\\\"\"), \"\\\"]\")\n"
                + "})\n"
                + "write.table(final_df, \n"
                + "            file = output_file, \n"
                + "            sep = \"\\t\", \n"
                + "            row.names = FALSE, \n"
                + "            quote = FALSE)" );

        return script.toString();
    }
}
