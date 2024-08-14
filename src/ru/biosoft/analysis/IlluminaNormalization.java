package ru.biosoft.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.script.LogScriptEnvironment;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.plugins.jri.RUtility;
import ru.biosoft.table.TableCSVImporter;
import ru.biosoft.table.TableCSVImporter.NullImportProperties;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

@ClassIcon ( "resources/cel-normalization.gif" )
public class IlluminaNormalization extends AnalysisMethodSupport<IlluminaNormalizationParameters>
{
    private final HashMap<String, String> sampleOrigins = new HashMap<>();

    //private static final String[] patternsForIdColumn = {"GI_\\d+-\\w", "ILMN_\\d{7,}", "ILMN_\\d{1,6}"};
    private static final List<Pattern> patternsForIdColumn = StreamEx.of("GI_\\d+-\\w", "ILMN_\\d{7,}", "ILMN_\\d{1,6}")
            .map(pattern -> Pattern.compile( pattern, Pattern.CASE_INSENSITIVE )).toList();


    public IlluminaNormalization(DataCollection<?> origin, String name)
    {
        super(origin, name, new IlluminaNormalizationParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkNotEmpty("illuminaFiles");
        checkOutputs();
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        String[] dels = new String[] {"\\t", ",", "\\s+", "\\|"};
        int delIndex = -1;
        validateParameters();
        log.info("Generating R command...");
        IlluminaNormalizationParameters params = getParameters();

        DataElementPathSet celFilesSet = params.getIlluminaFiles();
        if( celFilesSet.size() == 1 )
        {
            DataElementPath filePath = celFilesSet.first();
            if( ! ( filePath.optDataElement() instanceof FileDataElement ) && filePath.optDataElement() instanceof DataCollection )
            {
                celFilesSet = filePath.getChildren();
            }
        }
        List<String> fileNames = new ArrayList<>();
        DataElementPath outputPath = params.getOutputPath();
        DataCollection<?> outputDC = outputPath == null ? null : outputPath.optParentCollection();
        String outputName = outputPath == null ? null : outputPath.getName();
        for(DataElementPath celPath: celFilesSet)
        {
            DataElement de = celPath.optDataElement();
            if( de == null || ! ( de instanceof FileDataElement ) )
                continue;
            if( outputName == null || outputName.equals("") )
                outputName = "Norm " + ( (FileDataElement)de ).getFile().getName();
            File file = ( (FileDataElement)de ).getFile();
            String encoding=ApplicationUtils.detectEncoding(file);
            if( delIndex == -1 )
            {
                delIndex = getDelimiterIndex(file, dels, encoding);
            }
            else if( delIndex != getDelimiterIndex(file, dels, encoding) )
            {
                throw new RuntimeException("Delimiter type can't be recognized");
            }
            File illu = checkIlluminaFile(file, dels[delIndex], encoding);
            if (!sampleOrigins.containsValue(file.getName())) setColOrigin(file,dels[delIndex],encoding);
            String path = illu.getAbsolutePath();
            path = RUtility.removeUnneccessaryDots(path);
            fileNames.add(RUtility.escapeRString(path));
            //            columnNames.add(RUtility.escapeRString(RUtility.removeUnneccessaryDots(file.getAbsolutePath())));
        }
        String sep; // The separator for R should be equal to delimiter
        if( delIndex == 2 )
        {
            sep = "[:space:]";
        }
        else if( delIndex == 3 )
        {
            sep = "|";
        }
        else
        {
            sep = dels[delIndex].replace("\\\\", "\\");
        }

        if( fileNames.size() == 0 )
            throw new IllegalArgumentException("No suitable Illumina files found");
        File rOutput = TempFiles.file("illu.Routput");

        int outputLogBaseCode = params.getOutputLogarithmBaseCode();
        int defaultAnalysisBaseCode = Util.LOG2;

        List<String> origFileNames = celFilesSet.stream().map(p->p.getName()).collect(Collectors.toList());
        final String rCommand = "library(lumi)\n"
                + "raw.data <- lumiR.batch(fileList=c('"
                + String.join("',\n'", fileNames)
                + "'), convertNuID = FALSE, QC=FALSE, sep='"
                + sep
                + "')\n"
                + "normalized <- lumiExpresso(raw.data)\n"
                + "colnames(normalized)=c('"
                + String.join("',\n'", origFileNames)
                + "')\n"
                + "result <- exprs(normalized)\n"
                + ( outputLogBaseCode == defaultAnalysisBaseCode ? "" : "result <- "
                        + Util.getRCodeLogTransform("result", defaultAnalysisBaseCode, outputLogBaseCode) + "\n" )
                + "result <- cbind(Probe=row.names(result), result)\n" + "write.table(result, sep='" + sep + "', quote=F, file='"
                + RUtility.escapeRString(rOutput.getAbsolutePath()) + "', row.names=F)";

        jobControl.setPreparedness(2);
        log.log(Level.FINE, rCommand);
        log.info("Invoking R command (that will take some time)..." );
        final LogScriptEnvironment env = new LogScriptEnvironment(log);
        env.reportErrorsAsWarnings( true );
        SecurityManager.runPrivileged(() -> ScriptTypeRegistry.execute("R", rCommand, env, false));
        if( env.isFailed() && !rOutput.exists() )
        {
            throw new Exception("R command failed.");
        }
        if(jobControl.isStopped()) return null;
        jobControl.setPreparedness(70);
        log.info("Importing...");
        TableCSVImporter importer = new TableCSVImporter();
        if( importer.accept(outputDC, rOutput) == DataElementImporter.ACCEPT_UNSUPPORTED )
            throw new Exception("Can not import into selected collection");
        ( (NullImportProperties) importer.getProperties(null, null, null) ).setAddSuffix(true);
        DataElement result = importer.doImport(outputDC, rOutput, outputName, null, log);
        TableDataCollection table = ( (TableDataCollection)result );
        List<String> names = table.getNameList().subList(0, Math.min(100, table.getSize()));
        ReferenceTypeRegistry.setCollectionReferenceType(table, ReferenceTypeRegistry.detectReferenceType(names
                .toArray(new String[names.size()])));
        CollectionFactoryUtils.save(table);
        rOutput.delete();
        log.info("Done: result stored as " + result.getCompletePath());
        for (TableColumn col : table.getColumnModel())
        {
            col.setValue("origin", sampleOrigins.get(col.getName()));
        }
        return table;
    }
    private void setColOrigin(File file, String del, String encoding) throws Exception
    {
        String line;
        try (FileInputStream fis = new FileInputStream( file );
                InputStreamReader isr = new InputStreamReader( fis, encoding );
                BufferedReader br = new BufferedReader( isr ))
        {
            line = br.readLine();
        }

        Pattern pat = Pattern.compile("\\d{10}_[A-Z]");
        Matcher match = pat.matcher(line);

        while( match.find() ) //For an ordered Map of uniq sample names
        {
            String sampleStr = match.group();
            if( !sampleOrigins.containsValue(sampleStr) )
            {
                sampleOrigins.put(sampleStr, file.getName());
            }
        }
    }

    private File checkIlluminaFile(File file, String del, String encoding) throws Exception
    {
        TempFile newFile = TempFiles.file( "IlluminaGEO.txt" );
        try (FileInputStream fis = new FileInputStream( file );
                InputStreamReader isr = new InputStreamReader( fis, encoding );
                BufferedReader br = new BufferedReader( isr );
                BufferedWriter bw = com.developmentontheedge.application.ApplicationUtils.utfWriter( newFile ))
        {

            char[] buf = new char[1024 * 8];
            int len = 0;//br.read(buf);
            String line = br.readLine();//new String(buf);
            String secondLine = br.readLine();
            if( line == null || secondLine == null )
                return file;

            Pattern pattern = Pattern.compile( del );
            Matcher matcher = pattern.matcher( line );
            matcher.find();
            String delimiterString = matcher.group( 0 );
            int columnForId = getNumberOfIdColumn( line, secondLine, del );
            line = checkIdColumn( line, secondLine, delimiterString, del );
            String newLine = processColumnNameLine( line, del, delimiterString, file.getName(), columnForId );

            bw.write(newLine);
            bw.write(secondLine + "\n");
            if( ( columnForId == 0 ) || ( columnForId == -1 ) ) //If Id column is first or can not be recognized
            {
                while( ( len = br.read(buf) ) > 0 )
                {
                    bw.write(buf, 0, len);
                }
            }
            else
            //If Id column is not first then make it first
            {
                while( ( line = br.readLine() ) != null )
                {
                    String[] columnData = line.split(del);
                    String idColumnName = columnData[columnForId];
                    columnData[columnForId] = columnData[0];
                    if( idColumnName.equalsIgnoreCase("NA") || ( idColumnName.length() == 0 ) )
                        continue;
                    columnData[0] = idColumnName;
                    String newDataLine = String.join(delimiterString, columnData) + "\n";
                    bw.write(newDataLine);
                }
            }
            return newFile;
        }
        catch( Exception e )
        {
            newFile.close();
            return file;
        }
    }


    private int getDelimiterIndex(File file, String[] dels, String encoding) throws Exception
    {
        try (FileInputStream fis = new FileInputStream( file );
                InputStreamReader isr = new InputStreamReader( fis, encoding );
                BufferedReader br = new BufferedReader( isr ))
        {
            String line = "";
            String[] first20Lines = new String[20];
            int n = 0;
            while( ( line = br.readLine() ) != null && n < 20 )
            {
                first20Lines[n] = line;
                n++;
            }
            for( int delNum = 0; delNum < dels.length; delNum++ )
            {
                String st = dels[delNum];
                int colNumber = first20Lines[0].split(st).length;
                int i = 1;
                for( ; i < n; i++ )
                {
                    int nextColNumber = first20Lines[i].split(st).length;
                    if( colNumber != nextColNumber )
                        break;
                }
                if( i == n )
                {
                    return delNum;
                }
            }

            throw new RuntimeException("Delimiter type can't be recognized");
        }
    }

    private String processColumnNameLine(String line, String del, String delimiterString, String fName, int columnForId)
    {

        String newLine = line;
        if( line.matches("^ID_REF\\s+VALUE.+") ) //workaround for GEO downloaded files, rename second column as AVG_SIGNAL
        {
            String patternStr = "^ID_REF\\s+VALUE\\s+(.+)\\.Detection Pval.+";
            Pattern pat = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher match = pat.matcher(line);
            boolean matchFound = match.find();
            if( matchFound && match.groupCount() >= 1 )
            {
                String groupStr = match.group(1);
                newLine = line.replaceFirst("VALUE", groupStr + ".AVG_SIGNAL");
            }
            else
            {
                newLine = line.replaceFirst("VALUE", "AVG_SIGNAL");
            }
        }

        line = newLine;
        String[] columnNames = line.split(del);

        String[] sampleProperties = {"AVG_Signal", "BEAD_STD", "DETECTION", "AVG_NBEADS"};
        Pattern pattern = Pattern.compile(del + "\"?AVG_Signal\\s?\"?.", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        boolean matcherFind = matcher.find();
        if( matcherFind ) //workaround for GEO downloaded files: columns do not contain experiment names suffixes
        {//".+(?i)AVG_Signal[\\s\"].+"
            log.info("Columns of file " + fName + " do not contain experiment name, adding file name instead");
            for( int i = 0; i < columnNames.length; i++ )
            {
                for( String st : sampleProperties )
                {
                    Pattern p = Pattern.compile("\"?" + st + "\"?", Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(columnNames[i]);
                    boolean mF = m.find();
                    if( mF && m.start(0) == 0 )
                    {
                        String name = columnNames[i].replaceAll("\"", "");
                        if( !name.contains("ID") )
                        {
                            String sampleName = fName.substring(0, fName.lastIndexOf('.'));
                            if( !sampleOrigins.containsKey(sampleName) )
                                sampleOrigins.put(sampleName, fName);
                            name = sampleName + "." + st;
                        }
                        columnNames[i] = name;
                        break;
                    }
                }
            }
            newLine = String.join(delimiterString, columnNames) + "\n";
        }

        line = newLine;
        if( ( columnForId != 0 ) && ( columnForId != -1 ) )
        {
            columnNames = line.split(del);
            String idColumnName = columnNames[columnForId];
            columnNames[columnForId] = columnNames[0];
            columnNames[0] = idColumnName;
            newLine = String.join(delimiterString, columnNames) + "\n";
        }

        line = newLine;
        pattern = Pattern.compile("\\D?\\d{10}_[A-Z]+[" + del + ",\\n]", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(line);
        matcherFind = matcher.find();
        if( matcherFind )
        {
            //            String [] sampleProperties={"[A,a][V,v][G,g]_[S,s][I,i][G,g][N,n][A,a][L,l]",
            //                    "[B,b][E,e][A,a][D,d]_[S,s][T,t][D,d]",
            //                    "[D,d][E,e][T,t][E,e][C,c][T,t][I,i][O,o][N,n]",
            //                    "[A,a][V,v][G,g]_[N,n][B,b][E,e][A,a][D,d][S,s]"};
            Pattern pat = Pattern.compile("\\d{10}_[A-Z][" + del + "\\n]");
            Matcher match = pat.matcher(line);

            TreeMap<Integer, String> sampleMap = new TreeMap<>();
            ArrayList<Integer> sampleKeys = new ArrayList<>();
            while( match.find() ) //For an ordered Map of uniq sample names
            {
                String sampleStr = match.group();
                String sample = sampleStr.substring(0, sampleStr.length() - delimiterString.length());
                if( sampleStr.charAt(sampleStr.length() - 1) == '\n' )
                {
                    sampleStr = sampleStr.substring(0, sampleStr.length() - 1);
                }
                else
                {
                    sampleStr = sampleStr.substring(0, sampleStr.length() - delimiterString.length());
                }
                if( !sampleMap.containsValue(sample) )
                {
                    sampleMap.put(match.start(), sample);
                    sampleKeys.add(match.start());
                }
            }


            //            for( String groupStr : sampleSet )
            //            {
            //                int ind = line.indexOf(groupStr);
            //                line=line.substring(0, ind + groupStr.length()) + "." + sampleProperties[0]
            //              + line.substring(ind + groupStr.length());
            //            }

            int shift = 0;
            for( String groupStr : sampleMap.values() )//Run over all samples - groopStr is i'th sample
            {
                if( !sampleOrigins.containsKey(groupStr) )
                    sampleOrigins.put(groupStr, fName);
                String[] samplePropertiesClone = sampleProperties.clone();
                int propAmount = samplePropertiesClone.length;
                for( String st : sampleProperties )//Run over all column properties of sample groopStr (it can be max four properties)
                {
                    int ind = newLine.indexOf(groupStr);
                    if( ind == -1 )
                        break;

                    int indLine = ind + shift;
                    boolean mF = false;
                    for( int j = 0; j < propAmount; j++ )//For current property check whether it already has propertySuffix if not add it
                    {
                        String checkSuffix = samplePropertiesClone[j];
                        Pattern p = Pattern.compile(checkSuffix, Pattern.CASE_INSENSITIVE);
                        int endIndForMatch = ( ind + groupStr.length() + 12 > newLine.length() ) ? newLine.length() : ind
                                + groupStr.length() + 12;
                        Matcher m = p.matcher(newLine.substring(ind + groupStr.length(), endIndForMatch));
                        mF = m.find();
                        if( mF )
                        {
                            propAmount--;
                            for( int i = j; i < samplePropertiesClone.length - 1; i++ )
                            {
                                samplePropertiesClone[i] = samplePropertiesClone[i + 1];
                            }
                            break;
                        }

                    }
                    if( !mF )
                    {
                        line = line.substring(0, indLine + groupStr.length()) + "." + samplePropertiesClone[0]
                                + line.substring(indLine + groupStr.length());
                        shift += ( samplePropertiesClone[0].length() + 1 );//sift is index for sift to achieve a correspondence between line and changing newLine
                    }

                    newLine = newLine.substring(ind + groupStr.length() + 1);//delete a processed part of newLine
                    shift += ( ind + groupStr.length() + 1 );
                }
            }
            newLine = line;
        }

        return newLine;
    }
    private int getNumberOfIdColumn(String line, String secondLine, String del)
    {
        String[] idColumnNames = {"Target.?ID", "Probe.?ID", "ID_REF"};
        String[] colData = secondLine.split(del);
        OptionalInt colIdx = IntStreamEx.ofIndices( colData,
                data -> StreamEx.of( patternsForIdColumn ).anyMatch( p -> p.matcher( data ).find() ) ).findFirst();
        if(colIdx.isPresent())
            return colIdx.getAsInt();
        String[] colNames = line.split(del);

        for( int i = 0; i < colNames.length; i++ )
        {
            boolean mF = false;
            for( String st : idColumnNames )
            {
                Pattern p = Pattern.compile(st, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(colNames[i]);
                mF = m.find();
                if( mF )
                    break;
            }
            if( mF )
                return i;
        }
        return -1;
    }

    private String checkIdColumn(String line, String secondLine, String delimiterString, String del)
    {
        String[] idColumnNames = {"Target.?ID", "Probe.?ID"};
        String[] colNames = line.split(del);

        for( int i = 0; i < colNames.length; i++ )
        {
            boolean mF = false;
            for( String st : idColumnNames )
            {
                Pattern p = Pattern.compile(st, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(colNames[i]);
                mF = m.find();
                if( mF )
                    break;
            }
            if( mF )
            {
                String data = secondLine.split(del)[i];
                if( StreamEx.of( patternsForIdColumn ).anyMatch( p -> p.matcher( data ).find() ) )
                {
                    return line;
                }
                colNames[i] = "stub";// This column will be ignored by R
                return String.join(delimiterString, colNames) + "\n";
            }
        }
        return line;
    }
}
