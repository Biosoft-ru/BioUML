package ru.biosoft.bsa.importer;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.PropertiesDPS;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.TextUtil2;

/**
 * Columns in format description: 
 * chromosome    base_pair_location  variant_id  effect_allele   other_allele    beta    standard_error  p_value
 * @author anna
 */

public class GWASTrackImporter extends VCFTrackImporter
{
    private static final String ID_COLUMN = "variant_id";
    private static final String PVALUE_COLUMN = "p_value";

    private static final String CHROMOSOME_COLUMN = "chr";
    private static final String REF_ALLELE_COLUMN = "RefAllele";
    private static final String ALT_ALLELE_COLUMN = "AltAllele";
    private static final String POSITION_COLUMN = "pos";

    private static final Map<String, Pattern> requiredFields = new HashMap<String, Pattern>()
    {
        {
            put( CHROMOSOME_COLUMN, Pattern.compile( "chromosome|chr", Pattern.CASE_INSENSITIVE ) );
            put( POSITION_COLUMN, Pattern.compile( "pos|base_pair_location", Pattern.CASE_INSENSITIVE ) );
            put( REF_ALLELE_COLUMN, Pattern.compile( "ref_allele|other_allele|REF|RefAllele", Pattern.CASE_INSENSITIVE ) );
            put( ALT_ALLELE_COLUMN, Pattern.compile( "alt_allele|effect_allele|ALT|AltAllele", Pattern.CASE_INSENSITIVE ) );
        }
    };

    private Map<String, Integer> columnName2Index = new HashMap<>();
    private List<String> columnNames = new ArrayList<>();
    private int maxRequiredIndex = -1;
    private Set<Integer> skipIndex = new HashSet<Integer>();

    @Override
    protected Site parseLine(String line)
    {
        String[] fields = TextUtil2.split( line, '\t' );
        if( fields.length < maxRequiredIndex + 1 )
            return null;
        String chr = normalizeChromosome( fields[columnName2Index.get( CHROMOSOME_COLUMN )], true );
        int start;
        try
        {
            start = Integer.parseInt( fields[columnName2Index.get( POSITION_COLUMN )] );
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        Properties parameters = new Properties();
        String altAllele = fields[columnName2Index.get( ALT_ALLELE_COLUMN )];
        if( !NUCLEOTIDE_PATTERN.matcher( altAllele ).matches() )
            return null;
        putParameter( parameters, "AltAllele", altAllele, true );
        String refAllele = fields[columnName2Index.get( REF_ALLELE_COLUMN )];
        if( !NUCLEOTIDE_PATTERN.matcher( refAllele ).matches() )
            return null;
        int length = refAllele.length();
        putParameter( parameters, "RefAllele", refAllele, true );
        for( int j = 0; j < fields.length; j++ )
        {
            if( skipIndex.contains( j ) )
                continue;

            String name = j < columnNames.size() ? columnNames.get( j ) : "Info_" + j;
            String value = fields[j];

            if( name.equals( PVALUE_COLUMN ) )
            {
                name = "p-value";
                try
                {
                    parameters.put( name, Double.parseDouble( value ) );
                }
                catch( NumberFormatException e )
                {
                }
                continue;
            }
            else if( name.equals( ID_COLUMN ) )
                name = "name";
            putParameter( parameters, name, value );
        }

        String type = SiteType.TYPE_VARIATION;
        if( altAllele.startsWith( "<" ) && altAllele.endsWith( ">" ) )
            type = altAllele.substring( 1, altAllele.length() - 1 );
        return new SiteImpl( null, chr, type, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS( parameters ) );
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        ImporterProperties properties = getProperties();
        int headerRowIndex = properties.getHeaderRow();
        try (BufferedReader br = ApplicationUtils.utfReader( file ))
        {
            for( int i = 1; i <= headerRowIndex; i++ )
            {
                String line = br.readLine();
                if( null == line )
                    throw new EOFException();
                if( i == headerRowIndex )
                {
                    parseHeaderLine( line );
                }
            }
        }
        DataElement result = super.doImport( parent, file, elementName, jobControl, log );
        return result;
    }

    private void parseHeaderLine(String line)
    {
        columnName2Index.clear();
        skipIndex.clear();
        columnNames.clear();
        maxRequiredIndex = -1;
        //trim starting comment symbols if any
        if( line.startsWith( "#" ) )
            line = line.replaceAll( "^#+", "" );
        String[] values = TextUtil2.split( line, '\t' );
        for( int i = 0; i < values.length; i++ )
        {
            String colName = values[i];
            String reqKey = requiredFields.keySet().stream().filter( k -> requiredFields.get( k ).matcher( colName ).matches() ).findAny()
                    .orElse( null );
            if( reqKey != null )
            {
                columnName2Index.put( reqKey, i );
                maxRequiredIndex = Math.max( i, maxRequiredIndex );
                skipIndex.add( i );
                columnNames.add( i, reqKey );
            }
            else
            {
                columnName2Index.put( colName, i );
                columnNames.add( i, colName );
            }
        }
        List<String> missed = requiredFields.keySet().stream().filter( k -> !columnName2Index.containsKey( k ) )
                .collect( Collectors.toList() );
        if( !missed.isEmpty() )
            throw new IllegalArgumentException( "Column(s) required but absent in input file: " + String.join( ",", missed ) );
    }


    @Override
    public boolean init(Properties properties)
    {
        super.init( properties );
        format = "tsv";
        return true;
    }

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent == null || !parent.isMutable() || !DataCollectionUtils.isAcceptable( parent, getResultType() ) )
            return ACCEPT_UNSUPPORTED;
        if( file == null )
            return ACCEPT_HIGH_PRIORITY;
        //try to read first 20 lines and found header with required columns and appropriate data lines
        try (BufferedReader input = ApplicationUtils.utfReader( file ))
        {
            int totalLines = 0;
            int goodLines = 0;
            boolean headerFound = false;
            String line;
            while( ( line = input.readLine() ) != null && totalLines < 20 )
            {
                if( headerFound )
                {
                    Site site = parseLine( line );
                    if( site != null )
                        goodLines++;
                }
                else
                {
                    try
                    {
                        parseHeaderLine( line );
                        headerFound = true;
                    }
                    catch( IllegalArgumentException ex )
                    {
                    }
                }
                totalLines++;
            }
            if( totalLines > 0 && headerFound && (float)goodLines / totalLines > 0.9 )
                return ACCEPT_MEDIUM_PRIORITY;
            if( totalLines > 0 && headerFound && (float)goodLines / totalLines > 0.5 )
                return ACCEPT_BELOW_MEDIUM_PRIORITY;
            if( totalLines > 0 && headerFound && (float)goodLines / totalLines > 0.3 )
                return ACCEPT_LOW_PRIORITY;
        }
        catch( Exception e )
        {
        }
        return ACCEPT_UNSUPPORTED;
    }

    @Override
    protected synchronized ImporterProperties getProperties()
    {
        if( importerProperties == null )
            importerProperties = new ImporterProperties();
        return (ImporterProperties)importerProperties;
    }

    @SuppressWarnings ( "serial" )
    public static class ImporterProperties extends TrackImportProperties
    {
        private Integer headerRow = 1;
        private Integer dataRow = 2;

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
        @PropertyDescription ( "Index for first row with data" )
        public Integer getDataRow()
        {
            return dataRow;
        }

        public void setDataRow(Integer dataRow)
        {
            this.dataRow = dataRow;
            this.setSkipLines( dataRow - 1 );
        }
    }

    public static class ImporterPropertiesBeanInfo extends TrackImportPropertiesBeanInfo
    {
        public ImporterPropertiesBeanInfo()
        {
            super( ImporterProperties.class );
        }
        protected ImporterPropertiesBeanInfo(Class<? extends ImporterProperties> beanClass)
        {
            super( beanClass );
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            add( "headerRow" );
            add( "dataRow" );
        }
    }

}
