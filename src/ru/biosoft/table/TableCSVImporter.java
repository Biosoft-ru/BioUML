package ru.biosoft.table;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.TagEditorSupport;

import biouml.standard.type.Species;
import gnu.trove.iterator.TObjectLongIterator;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.ReferenceTypeSelector;

/**
 * Using for import microarray files to BioUML
 */
public class TableCSVImporter implements DataElementImporter
{
    private static final String UNESCAPED_QUOTE = "\"";
    private static final char NEW_LINE_CHAR = '\n';
    private static final String ESCAPED_QUOTE = "\"\"";
    private static final char QUOTE_CHAR = '"';

    protected static final Logger log = Logger.getLogger(TableCSVImporter.class.getName());
    protected NullImportProperties properties;
    private static String[] delimiterNames = new String[] {"Tabulation", "Spaces ( )", "Commas (,)", "Pipes (|)"};
    // How we favor delimiters when autodetecting
    private static int[] delimiterMultipliers = new int[] {100, 1, 3, 1};
    private static String[] dels = new String[] {"\\t", "\\s", ",", "\\|"};
    public final static String GENERATE_UNIQUE_ID = "Generate unique ID";

    public final static int MAX_COLUMN_PROGRESS = 20;

    private static final Pattern[] delimiterPatterns;
    static {
        delimiterPatterns = new Pattern[dels.length];
        for( int i = 0; i < dels.length; i++ )
            delimiterPatterns[i] = Pattern.compile( dels[i] );
    }

    @Override
    public int accept(DataCollection parent, File file)
    {
        if(parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable(parent, getResultType())) return ACCEPT_UNSUPPORTED;
        if(file == null) return ACCEPT_HIGH_PRIORITY;
        return getContentLevel(file);
    }

    //will ignore elementName argument if properties were set before.
    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        //        if( properties == null )
        //        {
        //            if( elementName == null || elementName.length() == 0 )
        //            {
        //                log.log(Level.SEVERE, "Element name is null");
        //                return null;
        //            }
        //            properties = (NullImportProperties)getProperties(parent, file, elementName);
        //        }

        properties = (NullImportProperties)getProperties( parent, file, elementName );
        String tableName = ( properties instanceof ImportProperties ) ? ( (ImportProperties)properties ).getTableName() : elementName;
        if( parent.contains(tableName) )
        {
            parent.remove(elementName);
        }

        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parent, tableName);

        try
        {
            if( properties.getColumnForID() != null && properties.getColumnForID().equals( GENERATE_UNIQUE_ID )
                    || properties.isGenerateUniqueID() == true )
            {
                table.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
                table.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
            }
            table.getInfo().getProperties().setProperty(DataCollectionUtils.SPECIES_PROPERTY, properties.species);
            table.setPropagationEnabled(false);
            table.setNotificationEnabled(false);
            fillTable(file, table, jobControl, properties);
            table.setPropagationEnabled(true);
            table.setNotificationEnabled(true);
            if( jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
            {
                parent.put(table);
                parent.remove(table.getName());
                return null;
            }
            ReferenceTypeRegistry.setCollectionReferenceType( table, properties.getTableType() );
            parent.put(table);
        }
        catch( IOException e )
        {
            parent.remove(elementName);
            if( jobControl != null )
            {
                jobControl.functionTerminatedByError(e);
                return null;
            }
            throw e;
        }
        return table;
    }
    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @PropertyName ( "Import properties" )
    @PropertyDescription ( "Import properties" )
    @Override
    public Object getProperties(DataCollection parent, File file, String elementName)
    {
        if( properties != null )
            return properties;
        try
        {
            if( file == null )
            {
                properties = new NullImportProperties();
                return properties;
            }
            properties = new ImportProperties(file, elementName);
            return properties;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error while attempting to create Import properties: " + ex.getMessage());
            properties = new NullImportProperties();
            return properties;
        }
    }

    public static void fillTable(@Nonnull File file, TableDataCollection table, FunctionJobControl jobControl, NullImportProperties properties)
            throws IOException
    {
        String columnForID = TextUtil2.nullToEmpty( properties.getColumnForID() );
        boolean addSuffix = properties.isAddSuffix();
        int headerRowIndex = properties.getHeaderRow();
        int firstDataRowIndex = properties.getDataRow();
        int delimiterType = properties.getDelimiterType();

        int columnForIDindex = -1;

        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        String sheetData = null;
        boolean isXLS = false;
        boolean tryNewXLSConverter = true;
        if( properties instanceof ImportProperties )
        {
            try
            {
                tryNewXLSConverter = ( (ImportProperties)properties ).isUseNewXLSConverter();
                XLSandXLSXConverters converter = XLSConverterFactory.getXLSConverter( file, tryNewXLSConverter );
                converter.process();
                sheetData = converter.getSheetData( ( (ImportProperties)properties ).getSheetIndex());
                isXLS = true;
            }
            catch( Exception e )
            {
            }
        }
        Class<?>[] types;
        try (BufferedReader br = isXLS ? new BufferedReader( new StringReader( sheetData ) )
                : new BufferedReader( new InputStreamReader( new FileInputStream( file ), Charset.forName( "UTF-8" ) ) ))
        {
            String[] header = new String[0];

            for( int i = 1; i < firstDataRowIndex; i++ )
            {
                if( i == headerRowIndex )
                {
                    header = resolveLine(br, delimiterType, properties);
                }
                else
                {
                    resolveLine(br, delimiterType, properties);
                }
            }

            //get types from all data rows
            types = detectColumnTypes( br, delimiterType, properties, jobControl );

            for( int i = 0; i < types.length; i++ )
            {
                String numericalColumnName = "Column#" + Integer.toString(i + 1);
                String columnDisplayName = ( headerRowIndex > 0 && header.length > i && !header[i].trim().isEmpty() ) ? header[i].trim()
                        : numericalColumnName;
                String columnName = table.getColumnModel().generateUniqueColumnName(columnDisplayName.replaceAll("\\/", "_"));
                if( ( columnForID.equals("") && i == 0 ) || columnDisplayName.equals(columnForID) && columnForIDindex == -1 )
                {
                    types[i] = String.class;
                    columnForIDindex = i;
                }
                else
                {
                    table.getColumnModel().addColumn(columnName, types[i]).setDisplayName(columnDisplayName);
                }
            }

            if( columnForIDindex == -1 )
                columnForIDindex = 0;
        }
        ReferenceType type = ReferenceTypeRegistry.optReferenceType( properties.getTableType() );
        if( type == null || properties.getTableType().equals( ReferenceTypeSelector.AUTO_DETECT_MESSAGE ) )
        {
            try
            {
                BufferedReader br;
                if( isXLS )
                {
                    br = new BufferedReader( new StringReader( sheetData ) );
                }
                else
                {
                    FileInputStream is = new FileInputStream( file );
                    br = new BufferedReader( new InputStreamReader( is, Charset.forName( "UTF-8" ) ) );
                }
                type = detectReferenceType( br, properties, columnForIDindex, 50 );
            }
            catch( IOException e )
            {
                type = ReferenceTypeRegistry.getDefaultReferenceType();
            }
            properties.setTableType( type.getDisplayName() );
        }

        if( jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
            return;

        FileChannel ch = null;
        BufferedReader br;
        if( isXLS )
        {
            br = new BufferedReader(new StringReader(sheetData));
        }
        else
        {
            FileInputStream is = new FileInputStream(file);
            br = new BufferedReader( new InputStreamReader( is, Charset.forName( "UTF-8" ) ) );
            ch = is.getChannel();
        }

        for( int i = 1; i < firstDataRowIndex; i++ )
            resolveLine(br, delimiterType, properties);

        Object typedValues[];
        int j = 0;
        Set<String> names = new HashSet<>();
        while( true )
        {
            j++;
            String[] values = null;
            try
            {
                values = resolveLine(br, delimiterType, properties);
                while( null == values )
                    values = resolveLine(br, delimiterType, properties);
            }
            catch( EOFException e )
            {
                break;
            }
            catch( IOException e )
            {
                throw e;
            }
            typedValues = typecastValues(values, types);

            //derive ID column value from typedValues and use it as key
            try
            {
                if( columnForID.equals(GENERATE_UNIQUE_ID) )
                {
                    TableDataCollectionUtils.addRow(table, Integer.toString(j), typedValues, true);
                }
                else
                {
                    Object[] newValues = removeElement(typedValues, columnForIDindex);
                    String id = values[columnForIDindex].trim();
                    id = type.preprocessId( id );
                    if( names.contains(id) )
                    {
                        if( addSuffix )
                            id = addSuffix(id, names);
                        else
                            continue;
                    }
                    names.add(id);
                    TableDataCollectionUtils.addRow(table, id, newValues, true);
                }

            }
            catch( Exception ex )
            {
                continue;
            }

            if( jobControl != null && j % 100 == 0 && ch != null )
            {
                jobControl.setPreparedness( (int) ( MAX_COLUMN_PROGRESS + ( 100 - MAX_COLUMN_PROGRESS ) * ch.position() / ch.size() ) );
                if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    return;
            }
        }
        br.close();

        try
        {
            table.finalizeAddition();
        }
        catch( Exception e )
        {
            //log.log(Level.SEVERE, e.getMessage(), e);
            log.log( Level.SEVERE, e.getMessage(), e );
        }

        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }

    private static int getContentLevel(File file)
    {
        try
        {

            XLSandXLSXConverters converter = XLSConverterFactory.getXLSConverter(file);
            //                converter.process();
            if( converter != null )
                return ACCEPT_HIGHEST_PRIORITY;

            String header = ApplicationUtils.readAsString(file, 255);
            char[] cc = header.toCharArray();
            int numRead = cc.length;
            int probBinary = 0;

            for( int i = 0; i < numRead; i++ )
            {
                int j = cc[i];

                if( ( j < 32 || j > 127 ) && j != '\n' && j != '\r' && j != '\t' )
                {
                    probBinary++;
                }
            }
            if( probBinary / 255.0 < 0.3 )
            {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader( new FileInputStream( file ), Charset.forName( "UTF-8" ) ) ))
                {
                    // At least one tab in first 20 rows
                    for( int i = 0; i < 20; i++ )
                    {
                        if( !br.ready() )
                            break;
                        String line = br.readLine();
                        if( !TextUtil2.isEmpty(line) )
                        {
                            for( String del : dels )
                            {
                                if( line.split( del, -1 ).length > 1 )
                                    return ACCEPT_MEDIUM_PRIORITY;
                            }
                        }
                    }
                    return ACCEPT_BELOW_MEDIUM_PRIORITY;
                }
            }
        }
        catch( Exception e )
        {
        }
        return ACCEPT_LOWEST_PRIORITY;
    }

    private static @Nonnull Object[] removeElement(Object[] array, int index)
    {
        if( index < 0 || index >= array.length )
            throw new IllegalArgumentException("Can't remove element, index is out of bounds: " + index);
        ArrayList<Object> list = new ArrayList<>();
        list.addAll(Arrays.asList(array));
        list.remove(index);
        @SuppressWarnings ( "null" )
        @Nonnull
        Object[] result = list.toArray();
        return result;
    }

    private static @Nonnull Object[] typecastValues(String[] values, Class<?>[] types)
    {
        Object ret[] = new Object[types.length];
        int nColumns = Math.min(values.length, types.length);
        for( int i = 0; i < nColumns; i++ )
        {
            try
            {
                if(values[i] != null)
                {
                    if( types[i] == Integer.class )
                        ret[i] = Integer.parseInt(values[i]);
                    else if( types[i] == Double.class )
                        ret[i] = Double.parseDouble(values[i]);
                    else
                        ret[i] = values[i];
                }
            }
            catch( NumberFormatException e )
            {
                // ignore
            }
        }
        return ret;
    }

    private static String[] resolveLine(BufferedReader br, int delimiterType, NullImportProperties properties) throws IOException
    {
        String line = getNextValueLine( br, properties );
        String values[] = null;

        if( properties.isProcessQuotes() )
        {
            int openQuote = 0;
            int closeQuote = 0;
            ArrayList<String> valuesTmp = new ArrayList<>();
            Pattern p = delimiterPatterns[delimiterType];
            boolean isMonolitLine = false;
            boolean isProcessingQuotes = false;
            StringBuilder quotedLinePart = new StringBuilder();
            //TODO: optimize algorithm (we recalculate closing quote position many times)
            while( ( openQuote != -1 ) && ( closeQuote != -1 ) && !isMonolitLine )
            {
                openQuote = line.indexOf( QUOTE_CHAR );
                isMonolitLine = false;
                Matcher matcher = p.matcher( line );
                int delimiterIndex = 0;
                int delimiterLength = 0;
                if( matcher.find( 0 ) )
                {
                    delimiterIndex = matcher.start();
                    delimiterLength = matcher.end( 0 ) - delimiterIndex;
                }
                else
                {
                    delimiterIndex = -1;
                }
                if( openQuote != -1 )
                {
                    int realIndex = openQuote + 1;
                    String checkString = line.substring( realIndex );
                    closeQuote = checkString.indexOf( QUOTE_CHAR );
                    //can not find closing quote
                    while( closeQuote != -1 )
                    {
                        //closing quote is last character in line, or does not escape another quote
                        if( closeQuote + 1 == checkString.length() || QUOTE_CHAR != checkString.charAt( closeQuote + 1 ) )
                            break;
                        realIndex += closeQuote + 2;
                        checkString = checkString.substring( closeQuote + 2 );
                        closeQuote = checkString.indexOf( QUOTE_CHAR );
                    }

                    if( closeQuote != -1 )
                    {
                        closeQuote += realIndex;
                        if( ( delimiterIndex > openQuote ) && ( delimiterIndex < closeQuote ) )
                        {
                            matcher = p.matcher(line.substring(closeQuote + 1));
                            if( matcher.find(0) )
                            {
                                delimiterIndex = matcher.start() + closeQuote + 1;
                                delimiterLength = matcher.end(0) + closeQuote + 1 - delimiterIndex;
                            }
                            else
                            {
                                delimiterIndex = -1;
                                isMonolitLine = true;
                            }
                        }
                        if( ( delimiterIndex < openQuote ) && ( delimiterIndex != -1 ) )
                        {
                            if( isProcessingQuotes )
                            {
                                quotedLinePart.append( line.substring( 0, delimiterIndex ) );
                                valuesTmp.add( quotedLinePart.toString().replace( ESCAPED_QUOTE, UNESCAPED_QUOTE ) );
                                quotedLinePart = new StringBuilder();
                                isProcessingQuotes = false;
                            }
                            else
                                valuesTmp.add( line.substring( 0, delimiterIndex ) );
                            line = line.substring(delimiterIndex + delimiterLength);
                        }
                        if( delimiterIndex > closeQuote )
                        {
                            isProcessingQuotes = true;
                            quotedLinePart.append( line.substring( 0, openQuote ) ).append( line.substring( openQuote + 1, closeQuote ) );
                            line = line.substring( closeQuote + 1 );
                        }

                        if( delimiterIndex == -1 )
                        {
                            line = line.substring( 0, openQuote ) + line.substring( openQuote + 1, closeQuote )
                                    + line.substring( closeQuote + 1, line.length() );
                        }
                    }
                    else
                    {
                        closeQuote = 0;
                        String newLine = getNextValueLine( br, properties );
                        // line is incomplete, process in old style - as monolithic line
                        if( newLine == null )
                            isMonolitLine = true; //TODO: think about better solution
                        else
                            line += NEW_LINE_CHAR + newLine; //TODO: correct line delimiter
                    }
                }
                else if( isProcessingQuotes && delimiterIndex != -1 )
                {
                    quotedLinePart.append( line.substring( 0, delimiterIndex ) );
                    valuesTmp.add( quotedLinePart.toString().replace( ESCAPED_QUOTE, UNESCAPED_QUOTE ) );
                    quotedLinePart = new StringBuilder();
                    isProcessingQuotes = false;

                    line = line.substring( delimiterIndex + delimiterLength );
                }
            }
            if( quotedLinePart.length() != 0 )
                line = quotedLinePart.toString().replace( ESCAPED_QUOTE, UNESCAPED_QUOTE ) + line;
            String[] val2;
            if( isMonolitLine )
            {
                val2 = new String[1];
                val2[0] = line;
            }
            else
            {
                val2 = line.split( dels[delimiterType], -1 );
            }
            values = new String[valuesTmp.size() + val2.length];
            for( int i = 0; i < valuesTmp.size() + val2.length; i++ )
            {
                if( i < valuesTmp.size() )
                {
                    values[i] = valuesTmp.get(i);
                }
                else
                {
                    values[i] = val2[i - valuesTmp.size()];
                }
            }
        }
        else
        {
            values = line.split( dels[delimiterType], -1 );
        }
        if( values == null || 0 == values.length )
            return null;
        return values;
    }

    private static String getNextValueLine(BufferedReader br, NullImportProperties properties) throws IOException, EOFException
    {
        String line = null;
        do
        {
            line = br.readLine();
            if( null == line )
                throw new EOFException();
        }
        while( !properties.getCommentString().isEmpty() && line.startsWith( properties.getCommentString() ) );
        return line;
    }

    private static Class<?>[] detectColumnTypes(BufferedReader br, int delimiterType, NullImportProperties properties,
            FunctionJobControl jobControl) throws IOException
    {
        List<TObjectLongMap<Class<?>>> columnScores = new ArrayList<>();
        int rowCnt = 0;
        int curProgress = 0;
        while( true )
        {
            String values[];
            try
            {
                values = resolveLine(br, delimiterType, properties);
                while( null == values )
                    values = resolveLine(br, delimiterType, properties);
            }
            catch( EOFException e )
            {
                break;
            }
            for( int i = 0; i < values.length; i++ )
            {
                if( columnScores.size() <= i )
                    columnScores.add(new TObjectLongHashMap<Class<?>>());
                if( TextUtil2.isEmpty(values[i]) )
                    continue;
                Class<?> type = detectColumnType(values[i]);
                int scoreAdd = type == Integer.class ? 1 : type == Double.class ? 30 : 1000;
                columnScores.get( i ).adjustOrPutValue( type, scoreAdd, scoreAdd );
            }
            rowCnt++;
            if( jobControl != null && rowCnt % 500 == 0 )
            {
                curProgress += ( MAX_COLUMN_PROGRESS - curProgress ) / 4;
                jobControl.setPreparedness( curProgress );
                if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    break;
            }
        }
        Class<?>[] ret = new Class<?>[columnScores.size()];
        for( int i = 0; i < columnScores.size(); i++ )
        {
            TObjectLongMap<Class<?>> scores = columnScores.get(i);
            long maxScore = 0;
            Class<?> bestType = String.class;
            for( TObjectLongIterator<Class<?>> it = scores.iterator(); it.hasNext(); )
            {
                it.advance();
                if( it.value() > maxScore )
                {
                    maxScore = it.value();
                    bestType = it.key();
                }
            }
            ret[i] = bestType;
        }
        if( jobControl != null )
            jobControl.setPreparedness( MAX_COLUMN_PROGRESS );
        return ret;
    }

    private static Class<?> detectColumnType(String string)
    {
        if( TextUtil2.isIntegerNumber(string) )
        {
            return Integer.class;
        }
        else if( TextUtil2.isFloatingPointNumber(string) )
        {
            return Double.class;
        }

        return String.class;
    }

    /**
     * Method add suffix _COPY_NN where NN is the smallest integer: table does not contain element with name string_COPY_NN
     * @param string
     * @param table
     * @return
     */
    private static String addSuffix(String string, Set<String> names)
    {
        int suffix = 1;
        StringBuffer result = new StringBuffer(string);
        result.append("_COPY_");
        int lengthWithoutIndex = result.length();
        result.append(1);
        while( names.contains(result.toString()) )
        {
            result = new StringBuffer(result.substring(0, lengthWithoutIndex));
            result.append(++suffix);
        }
        return result.toString();
    }

    /**
     * Defines table reference type before table is created
     * Reads limit lines of table data and collects ID values
     * Actually this method will be called for unknown file with NullImportProperties, since ImportProperties will detect appropriate type earlier
     * @throws IOException
     */
    private static @Nonnull ReferenceType detectReferenceType(@Nonnull BufferedReader br, NullImportProperties properties,
            int columnForIDindex,
            int limit) throws IOException
    {
        String columnForID = TextUtil2.nullToEmpty( properties.getColumnForID() );
        if( columnForID.equals( GENERATE_UNIQUE_ID ) )
            return ReferenceTypeRegistry.getDefaultReferenceType();

        int firstDataRowIndex = properties.getDataRow();
        int delimiterType = properties.getDelimiterType();

        for( int i = 1; i < firstDataRowIndex; i++ )
            resolveLine( br, delimiterType, properties );

        int j = 0;
        Set<String> names = new HashSet<>();
        while( true )
        {
            j++;
            if( j == limit )
                break;
            String[] values = null;
            try
            {
                values = resolveLine( br, delimiterType, properties );
                while( null == values )
                    values = resolveLine( br, delimiterType, properties );
            }
            catch( Exception e )
            {
                break;
            }
            String id = values[columnForIDindex].trim();
            names.add( id );
        }
        br.close();
        String[] ids = names.toArray( new String[names.size()] );

        return ReferenceTypeRegistry.detectReferenceType( ids );
    }

    /**
     * Import properties for unknown file
     */
    public static class NullImportProperties extends Option
    {
        protected String columnForID;
        private Boolean addSuffix = false;
        protected Boolean processQuotes = true;
        private Integer headerRow = 1;
        private Integer dataRow = 2;
        private Integer delimiterType = 0;
        private Boolean generateUniqueID = false;

        /** The line that starts with commentString should be skipped */
        private String commentString = "";

        private String tableType = ReferenceTypeSelector.AUTO_DETECT_MESSAGE;
        private String species = "Unspecified";

        @PropertyName ( "Column for ID" )
        @PropertyDescription ( "This column will be used as ID" )
        public String getColumnForID()
        {
            return columnForID;
        }

        public void setColumnForID(String columnForID)
        {
            this.columnForID = columnForID;
            firePropertyChange("*", null, null);
        }

        @PropertyName ( "Generate unique ID" )
        @PropertyDescription ( "Generate unique numeric identifier for each table row" )
        public Boolean isGenerateUniqueID()
        {
            return generateUniqueID;
        }

        public void setGenerateUniqueID(Boolean generateUniqueID)
        {
            Object oldValue = this.generateUniqueID;
            this.generateUniqueID = generateUniqueID;
            if( generateUniqueID )
                setColumnForID( GENERATE_UNIQUE_ID );
            else
                setColumnForID( null );
            firePropertyChange( "generateUniqueID", oldValue, generateUniqueID );
        }

        @PropertyName ( "Header row index" )
        @PropertyDescription ( "Index for row with column names, 0 means no header" )
        public Integer getHeaderRow()
        {
            return headerRow;
        }

        public void setHeaderRow(Integer headerRow)
        {
            this.headerRow = headerRow;
        }

        @PropertyName ( "First data row index" )
        @PropertyDescription ( "Index for fisrt row with data" )
        public Integer getDataRow()
        {
            return dataRow;
        }

        public void setDataRow(Integer dataRow)
        {
            this.dataRow = dataRow;
        }

        @PropertyName ( "Add suffixes to repeated ID" )
        @PropertyDescription ( "If ID is repeating, suffix _COPYNN will be added to it" )
        public Boolean isAddSuffix()
        {
            return addSuffix;
        }

        public void setAddSuffix(Boolean addSuffix)
        {
            Object oldValue = this.addSuffix;
            this.addSuffix = addSuffix;
            firePropertyChange("addSuffix", oldValue, this.addSuffix);
        }

        @PropertyName ( "Process quotes" )
        @PropertyDescription ( "Drop quotes and ignore delimiters between them" )
        public Boolean isProcessQuotes()
        {
            return processQuotes;
        }

        public void setProcessQuotes(Boolean processQuotes)
        {
            Object oldValue = this.processQuotes;
            this.processQuotes = processQuotes;
            firePropertyChange("processQuotes", oldValue, this.processQuotes);
        }

        @PropertyName ( "Type of the table" )
        @PropertyDescription ( "Assign table type" )
        public String getTableType()
        {
            return tableType;
        }

        public void setTableType(String tableType)
        {
            Object oldValue = this.tableType;
            this.tableType = tableType;
            firePropertyChange("tableType", oldValue, this.tableType);
        }

        @PropertyName ( "Column delimiter" )
        @PropertyDescription ( "Character used to split columns" )
        public Integer getDelimiterType()
        {
            return delimiterType;
        }

        public void setDelimiterType(Integer delimiterType)
        {
            Object oldValue = this.delimiterType;
            this.delimiterType = delimiterType;
            firePropertyChange("delimiterType", oldValue, delimiterType);
        }

        @PropertyName ( "Species" )
        @PropertyDescription ( "Species associated with given data set" )
        public String getSpecies()
        {
            return species;
        }

        public void setSpecies(String species)
        {
            this.species = species;
        }

        @PropertyName ( "Comment prefix" )
        @PropertyDescription ( "Any line that starts with this string will be skipped" )
        public String getCommentString()
        {
            return commentString;
        }

        public void setCommentString(String commentString)
        {
            Object oldValue = this.commentString;
            this.commentString = commentString;
            firePropertyChange("commentString", oldValue, commentString);
        }
    }

    public static class DelimiterEditor extends TagEditorSupport
    {
        public DelimiterEditor()
        {
            super(delimiterNames, 0);
        }
    }

    public static class NullImportPropertiesBeanInfo extends BeanInfoEx2<NullImportProperties>
    {
        public NullImportPropertiesBeanInfo()
        {
            super( NullImportProperties.class, MessageBundle.class.getName() );
        }
        @Override
        public void initProperties() throws Exception
        {
            property( "delimiterType" ).editor( DelimiterEditor.class ).add();
            add( "processQuotes" );
            add( "headerRow" );
            add( "dataRow" );
            add( "commentString" );
            add( "generateUniqueID" );
            property( "columnForID" ).hidden( "isGenerateUniqueID" ).add();
            add( "addSuffix" );
            add( ReferenceTypeSelector.registerSelector( "tableType", beanClass, true ) );
        }
    }

    public static class ImportProperties extends NullImportProperties
    {
        private String tableName;
        private String[] possibleColumns;
        private final File file;
        private String sheetData;
        private String[] sheets;
        private int selectedSheet = 0;
        private boolean isXLS;
        private boolean useNewXLSConverter = true;

        private int detectDelimiter() throws IOException
        {
            try (BufferedReader br = getReader())
            {
                int delimiterScores[] = new int[dels.length];
                int lineNum = 0;
                while( true )
                {
                    if( !br.ready() )
                        break;
                    String line = br.readLine();
                    if( !TextUtil2.isEmpty( line ) )
                    {
                        for( int i = 0; i < dels.length; i++ )
                        {
                            delimiterScores[i] += ( line.split( dels[i], -1 ).length - 1 ) * delimiterMultipliers[i];
                        }
                    }
                    if( ++lineNum > 100 )
                        break;
                }

                int bestDelimiter = 0;
                int bestScore = 0;
                for( int i = 0; i < dels.length; i++ )
                {
                    if( delimiterScores[i] > bestScore )
                    {
                        bestScore = delimiterScores[i];
                        bestDelimiter = i;
                    }
                }
                return bestDelimiter;
            }
        }

        private int detectHeaderSize() throws IOException
        {
            try (BufferedReader br = getReader())
            {
                List<DataType[]> types = new ArrayList<>();
                int maxLen = 0;
                while( true )
                {
                    String values[];
                    try
                    {
                        values = resolveLine( br, getDelimiterType(), this );
                    }
                    catch( EOFException e )
                    {
                        break;
                    }
                    if( values == null )
                        values = new String[0];
                    DataType[] rowTypes = new DataType[values.length];
                    for( int i = 0; i < values.length; i++ )
                    {
                        if( TextUtil2.isFloatingPointNumber( values[i] ) )
                            rowTypes[i] = DataType.Float;
                        else
                            rowTypes[i] = DataType.Text;
                    }
                    if( rowTypes.length > maxLen )
                        maxLen = rowTypes.length;
                    types.add( rowTypes );
                    if( types.size() > 100 )
                        break;
                }

                if( types.size() < 2 )
                    return 0;
                double bestScore = 0;
                int bestPos = 0;
                for( int i = 0; i < Math.min( types.size() - 2, 10 ); i++ )
                {
                    double score = 0;
                    for( int j = 0; j < maxLen; j++ )
                    {
                        int[] typeCounts = new int[3];
                        for( int row = i; row < types.size(); row++ )
                        {
                            if( types.get( row ).length <= j )
                                typeCounts[0]++;
                            else if( types.get( row )[j] == DataType.Float )
                                typeCounts[1]++;
                            else
                                typeCounts[2]++;
                        }
                        score += Math.max( Math.max( typeCounts[0], typeCounts[1] ), typeCounts[2] );
                    }
                    score /= types.size() - i;
                    if( score > bestScore )
                    {
                        bestScore = score;
                        bestPos = i;
                    }
                    else
                        break;
                }
                int shift = calculateHeaderSizeShift( bestPos, types.get( bestPos ) );
                return bestPos + shift;
            }
        }

        private int calculateHeaderSizeShift(int curPos, DataType[] types)
        {
            if( !StreamEx.of( types ).allMatch( dt -> DataType.Text == dt ) )
                return 0;
            try( BufferedReader br = getReader() )
            {
                int i = 0;
                String[] line;
                while( true )
                {
                    line = resolveLine( br, getDelimiterType(), this );
                    if( i == curPos )
                        break;
                    i++;
                }
                if( "ID".equals( line[0] ) )
                    return 1;
            }
            catch( Exception e )
            {
                ExceptionRegistry.log( e );
            }
            return 0;
        }

        private String[] initPossibleColumns()
        {
            try(BufferedReader br = getReader())
            {
                if( br == null )
                {
                    return IntStreamEx.rangeClosed( 1, 10 ).mapToObj( i -> "Column#" + i ).toArray( String[]::new );
                }

                for( int i = 1; i < getHeaderRow(); i++ )
                    resolveLine(br, getDelimiterType(), this);

                String[] header = ( getHeaderRow() > 0 ) ? resolveLine(br, getDelimiterType(), this) : new String[0];

                for( int i = getHeaderRow() + 1; i < getDataRow(); i++ )
                    resolveLine(br, getDelimiterType(), this);

                String[] firstDataRow = resolveLine(br, getDelimiterType(), this);
                br.close();

                return IntStreamEx.ofIndices( firstDataRow )
                        .mapToObj( i -> ( header.length > i ) ? header[i] : "Column#" + (i + 1) )
                        .toArray( String[]::new );
            }
            catch( IOException e )
            {
                return new String[0];
            }
        }

        private String[] getIDsForDetection(int idIndex) throws IOException
        {
            try( BufferedReader br = getReader() )
            {
                for( int i = 1; i < getDataRow(); i++ )
                    resolveLine( br, getDelimiterType(), this );

                String[] ids = new String[20];
                for( int i = 0; i < 20; i++ )
                {
                    try
                    {
                        ids[i] = resolveLine( br, getDelimiterType(), this )[idIndex];
                    }
                    catch( Exception e )
                    {
                    }
                }
                return ids;
            }
        }


        private boolean detectProcessQuotes() throws IOException
        {
            try (BufferedReader br = getReader())
            {
                for( int i = 1; i < getDataRow(); i++ )
                    getNextValueLine( br, this );

                int numQuoted = 0;
                int numLines = 0;
                while( true )
                {
                    if( !br.ready() )
                        break;
                    String line = br.readLine();
                    if( null == line )
                        break;
                    if( !TextUtil2.isEmpty( line ) )
                    {
                        numLines++;
                        String trimmedLine = line.trim();
                        String[] fields = trimmedLine.split( dels[getDelimiterType()], -1 );
                        int i = 0;

                        boolean quoteFound = false;
                        while( i < fields.length && !quoteFound )
                        {
                            String trimmed = fields[i].trim();
                            int firstQuoteIndex = trimmed.indexOf( QUOTE_CHAR );
                            if( firstQuoteIndex == 0 )
                            {
                                int secondQuoteIndex = trimmed.substring( 1 ).indexOf( QUOTE_CHAR );
                                //not found in same field, may be quoted string with delimiters, find next field with last position only
                                if(secondQuoteIndex == -1)
                                {
                                    int j = i+1;
                                    while( j < fields.length && !quoteFound )
                                    {
                                        String trimmed2 = fields[j].trim();
                                        j++;
                                        secondQuoteIndex = trimmed2.indexOf( QUOTE_CHAR );
                                        if( secondQuoteIndex == -1 )
                                            continue;
                                        else if( secondQuoteIndex == trimmed2.length() - 1 )
                                        {
                                            quoteFound = true;
                                            numQuoted++;
                                        }
                                        else
                                        {
                                            //found in incorrect position, treat as string with quotes not needed to process
                                            quoteFound = true;
                                        }
                                    }
                                    break; //closing quote was not found in any field, treat as string with quotes not needed to process
                                }
                                //found in same field on last position
                                else if(secondQuoteIndex == trimmed.length() - 2)
                                {
                                    quoteFound = true;
                                    numQuoted++;
                                }
                                //found in same field in incorrect position, treat as string with quotes not needed to process
                                else
                                {
                                    quoteFound = true;
                                }
                            }
                            else
                                i++;
                        }
                        if( numLines == 20 )
                            break;
                    }
                }
                return numQuoted > numLines / 2;
            }
            catch( IOException e )
            {
                return true;
            }
        }

        private BufferedReader getReader() throws IOException
        {
            return file == null ? null : isXLS ? new BufferedReader( new StringReader( sheetData ) )
                    : new BufferedReader( new InputStreamReader( new FileInputStream( file ), Charset.forName( "UTF-8" ) ) );
        }

        public ImportProperties(File file, String name) throws IOException
        {
            super();
            this.file = file;
            tableName = name;
            try
            {
                XLSandXLSXConverters converter = XLSConverterFactory.getXLSConverter( file, useNewXLSConverter );
                converter.process();
                sheets = converter.getSheetNames();
                sheetData = converter.getSheetData(selectedSheet);
                isXLS = true;
                processQuotes = false;
            }
            catch( Exception e )
            {
                isXLS = false;
            }
            if( !isXLS )
                setDelimiterType(detectDelimiter());
            setHeaderRow(detectHeaderSize());
            setDataRow(getHeaderRow() + 1);
            if( !isXLS )
                setProcessQuotes( detectProcessQuotes() );
            possibleColumns = initPossibleColumns();
            if( possibleColumns.length > 0 )
            {
                columnForID = possibleColumns[0];
                detectSubTypeAndSpecies( 0 );
            }
        }

        private void detectSubTypeAndSpecies(int idIndex) throws IOException
        {
            String[] ids = getIDsForDetection( idIndex );
            ReferenceType subType = ReferenceTypeRegistry.detectReferenceType( ids );
            setTableType( subType.toString() );
            setSpecies( subType.predictSpecies( ids ) );
        }

        public void setSheet(String sheetName)
        {
            for( int i = 0; i < sheets.length; i++ )
            {
                if( sheets[i].equals(sheetName) )
                {
                    if( selectedSheet != i )
                    {
                        int oldSheet = selectedSheet;
                        selectedSheet = i;
                        firePropertyChange("sheet", selectedSheet, oldSheet);
                        try
                        {
                            XLSandXLSXConverters converter = XLSConverterFactory.getXLSConverter(file);
                            converter.process();
                            sheetData = converter.getSheetData(selectedSheet);
                            super.setHeaderRow(detectHeaderSize());
                            super.setDataRow(getHeaderRow() + 1);
                            setPossibleColumns(initPossibleColumns());
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE, "Unable to get columns for sheet " + sheetName, e);
                        }
                    }
                    break;
                }
            }
        }

        public boolean isSheetsHidden()
        {
            return !isXLS;
        }

        public boolean isDelimiterTypeHidden()
        {
            return isXLS;
        }

        @PropertyName ( "Sheet name" )
        @PropertyDescription ( "Sheet name" )
        public String getSheet()
        {
            return sheets[selectedSheet];
        }

        public int getSheetIndex()
        {
            return selectedSheet;
        }

        public String[] getPossibleSheets()
        {
            return sheets.clone();
        }

        public void setPossibleColumns(String[] columns)
        {
            this.possibleColumns = columns;
            setColumnForID(possibleColumns.length > 0 ? possibleColumns[0] : GENERATE_UNIQUE_ID);
        }

        public String[] getPossibleColumns()
        {
            return possibleColumns.clone();
        }

        @PropertyName ( "Name for table" )
        @PropertyDescription ( "Name for table" )
        public String getTableName()
        {
            return tableName;
        }

        public void setTableName(String name)
        {
            String oldValue = tableName;
            this.tableName = name;
            firePropertyChange("tableName", name, oldValue);
        }

        @Override
        public void setHeaderRow(Integer headerRow)
        {
            super.setHeaderRow(headerRow);
            setPossibleColumns(initPossibleColumns());
        }

        @Override
        public void setDataRow(Integer dataRow)
        {
            super.setDataRow(dataRow);
            setPossibleColumns(initPossibleColumns());
        }

        @Override
        public void setColumnForID(String columnForID)
        {
            if( columnForID == null )
                return;
            if( columnForID.equals(GENERATE_UNIQUE_ID) )
            {
                super.setColumnForID(columnForID);
                setTableType(ReferenceTypeRegistry.getDefaultReferenceType().toString());
                return;
            }
            for( int i = 0; i < possibleColumns.length; i++ )
            {
                if( columnForID.equals(possibleColumns[i]) )
                {
                    super.setColumnForID(columnForID);
                    try
                    {
                        detectSubTypeAndSpecies( i );
                    }
                    catch( IOException e )
                    {
                    }
                    return;
                }
            }
            //setTableType(ReferenceTypeRegistry.getDefaultReferenceType().toString());
        }

        @Override
        public void setTableType(String tableType)
        {
            if( !tableType.equals(ReferenceTypeSelector.AUTO_DETECT_MESSAGE) )
                super.setTableType(tableType);
        }

        @Override
        public void setDelimiterType(Integer delimiterType)
        {
            super.setDelimiterType(delimiterType);
            try
            {
                super.setHeaderRow(detectHeaderSize());
                super.setDataRow(getHeaderRow() + 1);
                super.setProcessQuotes( detectProcessQuotes() );
                setPossibleColumns(initPossibleColumns());
            }
            catch( IOException e )
            {
            }
        }

        public boolean isUseNewXLSConverter()
        {
            return useNewXLSConverter;
        }
        public void setUseNewXLSConverter(boolean useNewXLSConverter)
        {
            boolean oldValue = this.useNewXLSConverter;
            this.useNewXLSConverter = useNewXLSConverter;
            firePropertyChange( "useNewXLSConverter", oldValue, useNewXLSConverter );
        }

    }

    public static class ImportPropertiesBeanInfo extends BeanInfoEx2<ImportProperties>
    {
        public ImportPropertiesBeanInfo()
        {
            this( ImportProperties.class );
        }

        public ImportPropertiesBeanInfo(Class<? extends ImportProperties> beanClass)
        {
            super( beanClass, MessageBundle.class.getName() );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "tableName" );
            property( "sheet" ).hidden( "isSheetsHidden" ).tags( bean -> StreamEx.of( bean.getPossibleSheets() ) ).add();
            property( "useNewXLSConverter" ).titleRaw( "Use new XLS(X) converter" )
                    .descriptionRaw( "New converter gives better results for processing empty cells" ).hidden( "isSheetsHidden" ).add();
            property( "delimiterType" ).hidden( "isDelimiterTypeHidden" ).editor( DelimiterEditor.class ).add();
            add( "processQuotes" );
            add( "headerRow" );
            add( "dataRow" );
            add( "commentString" );
            property( "columnForID" ).tags( bean -> StreamEx.of( bean.getPossibleColumns() ).prepend( GENERATE_UNIQUE_ID ) ).add();
            add( "addSuffix" );
            add( ReferenceTypeSelector.registerSelector( "tableType", beanClass, true ) );
            property( "species" ).tags( bean -> Species.allSpecies().map( Species::getName ).prepend( "Unspecified" ) ).add();
        }
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return TableDataCollection.class;
    }
}
