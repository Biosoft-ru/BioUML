package ru.biosoft.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TempFiles;

public class SignTest extends AnalysisMethodSupport<SignTestParameters>
{

    public SignTest(DataCollection<?> origin, String name)
    {
        super( origin, name, new SignTestParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        String sampleCol = parameters.getSampleCol();
        TableDataCollection outTable;
        Map<String, File> comparison = fillComparison( sampleCol );
        log.info( "Loading input table" );
        File dtmFile = createDataFile( parameters.getInputTablePath().getDataElement( TableDataCollection.class ) );
        jobControl.setPreparedness( 5 );
        log.info( "Preparing R inputs" );
        String rcmd = makeScript( dtmFile, comparison, parameters.getAdjMethod() );
        jobControl.setPreparedness( 10 );
        log.info( "Invoking R" );
        runR( rcmd );
        jobControl.setPreparedness( 20 );
        log.info( "Writing output" );
        outTable = createOutputTable( sampleCol, comparison );
        jobControl.setPreparedness( 100 );
        return outTable;
    }

    private File createDataFile(TableDataCollection inputTable) throws Exception
    {
        StringBuilder sbData = new StringBuilder();
        ArrayList<Integer> cols = new ArrayList<>();
        // Write header
        sbData.append( "ID" );
        Iterator<TableColumn> itc = inputTable.getColumnModel().iterator();
        while( itc.hasNext() )
        {
            TableColumn tc = itc.next();
            sbData.append( "\t" + tc.getName() );
            cols.add( inputTable.getColumnModel().getColumnIndex( tc.getName() ) );
        }
        sbData.append( "\n" );
        //            Write table body
        Iterator<RowDataElement> irde = inputTable.iterator();
        while( irde.hasNext() )
        {
            RowDataElement dataLine = irde.next();
            sbData.append( dataLine.getName() );
            Iterator<Integer> icols = cols.iterator();
            while( icols.hasNext() )
            {
                double val;
                try
                {
                    val = Double.parseDouble( dataLine.getValues()[icols.next()].toString() );
                }
                catch( Exception e )
                {
                    ExceptionRegistry.log( e );
                    throw e;
                }
                sbData.append( "\t" + val );
            }
            sbData.append( "\n" );
        }
        File dtmFile = TempFiles.file( "signTest" + "_" + new Random().nextInt() );
        try (BufferedWriter bw = new BufferedWriter( new FileWriter( dtmFile ) ))
        {
            bw.write( sbData.toString() );
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
            throw e;
        }
        return dtmFile;
    }

    private TableDataCollection createOutputTable(String sampleCol, Map<String, File> comparison) throws Exception
    {
        final String[][] signCols = {{"Successes", "Number of successes", "T"}, {"Trials", "Number of trials", "T"},
                {"Estimate", "Estimated probability of success", "T"}, {"P.Value", "Probability to reject the alternative hypothesis", "T"},
                {"adj.P.Val", "Adjusted p-value", "T"}};

        String resName = comparison.entrySet().iterator().next().getKey();
        File resFile = comparison.get( resName );

        DataElementPath outPath = parameters.getResultTablePath();
        TableDataCollection outTable;
        outTable = TableDataCollectionUtils.createTableDataCollection( outPath );
        DataElementPath inPath = parameters.getInputTablePath();
        TableDataCollection inTable = inPath.getDataElement( TableDataCollection.class );
        ReferenceTypeRegistry.setCollectionReferenceType( outTable, inTable.getReferenceType() );
        if( inTable.getInfo().getProperty( DataCollectionUtils.SPECIES_PROPERTY ) != null )
        {
            outTable.getInfo().getProperties().setProperty( DataCollectionUtils.SPECIES_PROPERTY,
                    inTable.getInfo().getProperty( DataCollectionUtils.SPECIES_PROPERTY ) );
        }
        prepareResTable( outTable, signCols );
        try (BufferedReader br = new BufferedReader( new FileReader( resFile ) ))
        {
            //            TODO: Get into shape
            br.readLine(); // Skip header, will create our own
            String line;
            while( ( line = br.readLine() ) != null )
            {
                String[] lineParts = line.split( " " );
                String rowName = lineParts[0];
                String[] sVals = Arrays.copyOfRange( lineParts, 1, lineParts.length );
                Object[] values = Stream.of( sVals ).map( Double::valueOf ).toArray();
                TableDataCollectionUtils.addRow( outTable, rowName, values );
            }
            TableDataCollectionUtils.setSortOrder( outTable, "adj.P.Val", true );
            CollectionFactoryUtils.save( outTable );
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
            throw e;
        }
        return outTable;
    }

    private void runR(final String rcmd) throws Exception
    {
        final LogScriptEnvironment env = new LogScriptEnvironment( log );
        log.info( "R script starting" );
        SecurityManager.runPrivileged( () -> ScriptTypeRegistry.execute( "R", rcmd, env, false ) );
        log.info( "R script complete" );
    }

    private String makeScript(File dtmFile, Map<String, File> comparison, String adjMethod) throws Exception
    {
        String adjArg = getAdjArg( adjMethod );
        String sampleCol = comparison.entrySet().iterator().next().getKey();
        File resFile = comparison.entrySet().iterator().next().getValue();
        StringBuilder sb = new StringBuilder();
        sb.append( "dt <- as.matrix(read.table(" + "\'" + dtmFile.getAbsolutePath() + "\'" + ",sep=\"\\t\",header=T,row.names=1));\n"
                + "sample <- grep(" + "'" + sampleCol + "'" + ",colnames(dt),fixed=TRUE);\n" );
        sb.append( "res <- apply(dt,1,function(x){" + "heads <- length(x) - rank(x,ties.method='min')[sample];" + "trials <- length(x) - 1;"
                + "stat <- binom.test(heads,trials, alternative='greater');"
                + "return(c(stat$statistic,stat$parameter,stat$estimate,stat$p.value))" + "});" + "res <- t(res);"
                + "colnames(res) <- c('Successes','Trials','Probability','p-value');\n" + "res <- as.data.frame(res);\n"
                + "res <- cbind(res, p.adjust(res$'p-value',method=" + "'" + adjArg + "'" + "));\n"
                + "colnames(res)[length(colnames(res))] = 'adj.p-value';\n"
                + "write.table(res, file=" + "'" + resFile.getAbsolutePath() + "'" + ",quote=FALSE" + ");\n" );
        sb.append( "print(head(res));\n" );
        return sb.toString();
    }

    private Map<String, File> fillComparison(String sampleCol) throws IOException
    {
        Map<String, File> comparison = new HashMap<>();
        File resFile = TempFiles.file( "col" + sampleCol + ".tsv" );
        comparison.put( sampleCol, resFile );
        return comparison;
    }

    private int prepareResTable(TableDataCollection outTable, String[][] signCols)
    {
        ColumnModel columnModel = outTable.getColumnModel();
        for( String[] signCol : signCols )
        {
            TableColumn tc = columnModel.addColumn( signCol[0], Double.class );
            tc.setShortDescription( signCol[1] );
            if( signCol[2].charAt( 0 ) == 'F' )
            {
                tc.setHidden( true );
            }
        }
        return columnModel.getColumnCount();
    }

    private String getAdjArg(String adjMethod)
    {
        String adjArg = "none";
        switch( adjMethod )
        {
            case "none":
                adjArg = "none";
                break;
            case "Bonferroni 1936":
                adjArg = "bonferroni";
                break;
            default:
                adjArg = "BH";
                break;
        }
        return adjArg;
    }
}
