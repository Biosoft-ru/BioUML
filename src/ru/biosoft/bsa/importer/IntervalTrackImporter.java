package ru.biosoft.bsa.importer;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.TextUtil;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author lan
 *
 */
public class IntervalTrackImporter extends TrackImporter
{
    public static final String DELIMITER = "\t";
    public static final String HEADER_PREFIX = "#";
    private static final String COLUMN_CHROM = "CHROM";
    private static final String COLUMN_START = "START";
    private static final String COLUMN_END = "END";
    private static final List<String> REQUIRED_COLUMN_NAMES = Arrays.asList( COLUMN_CHROM, COLUMN_START, COLUMN_END );
    public static final String STRAND = "STRAND";

    private boolean hasHeader;
    private List<String> columnNames;
    private boolean valid;
    private int chromIdx = 0;
    private int startIdx = 1;
    private int endIdx = 2;

    @Override
    protected Site parseLine(String line)
    {
        if( columnNames == null )
        {
            parseHeader( line );
            if( hasHeader )
            {
                return null;
            }
        }
        if( !valid )
        {
            return null;
        }

        String[] fields = line.split( DELIMITER, -1 );
        if( hasHeader )
        {
            if( fields.length != columnNames.size() )
            {
                return null;
            }
        }
        else
        {
            if( fields.length < 3 )
            {
                return null;
            }
        }

        String chrom = normalizeChromosome( fields[chromIdx]);
        int start, length;
        try
        {
            int zeroBasedStart = Integer.parseInt(fields[startIdx]);
            int end = Integer.parseInt(fields[endIdx]);
            length = end - zeroBasedStart;
            start = zeroBasedStart + 1;
        }
        catch( NumberFormatException e )
        {
            return null;
        }

        int strand = parseStrand( fields );
        if( strand == StrandType.STRAND_MINUS )
        {
            start = start + length - 1;
        }

        DynamicPropertySet properties = fillDynamicProperties( fields );
        if( properties == null )
        {
            return null;
        }

        return new SiteImpl(null, chrom, SiteType.TYPE_UNSURE, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY, strand, null,
                properties);
    }

   @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "interval";
        return true;
    }

    @Override
    protected synchronized ImporterProperties getProperties()
    {
        if(importerProperties == null) importerProperties = new ImporterProperties();
        return (ImporterProperties)importerProperties;
    }

    @SuppressWarnings ( "serial" )
    public static class ImporterProperties extends TrackImportProperties
    {
        private String fieldNames = "";

        @PropertyName ( "Field names" )
        @PropertyDescription ( "Comma separated list of additional field names" )
        public String getFieldNames()
        {
            return fieldNames;
        }

        public void setFieldNames(String fieldNames)
     {
            Object oldValue = this.fieldNames;
            this.fieldNames = fieldNames;
            firePropertyChange( "fieldNames", oldValue, fieldNames );
  }

        public String[] generateFieldNames(int nFields)
        {
            String[] result = new String[nFields];

            int zeroCount = 0;
            while( nFields >= 10 )
            {
                nFields /= 10;
                zeroCount++;
            }
            String nZeroes = TextUtil.times('0', zeroCount);

            String[] names = fieldNames.isEmpty() ? new String[0] : fieldNames.split( ",", -1 );

            for( int i = 0; i < result.length; i++ )
                result[i] = i < names.length ? names[i] : ( "field" + nZeroes + ( i + 1 ) );

            return result;
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
            add( new PropertyDescriptorEx( "fieldNames", beanClass ) );
        }

    }

    @Override
    protected boolean isComment(String line)
    {
        return false;
    }

    private void parseHeader(String line)
    {
        if( line.startsWith( HEADER_PREFIX ) )
        {
            final String header = line.substring( HEADER_PREFIX.length() );
            columnNames = Arrays.asList( header.split( DELIMITER ) );

            valid = columnNames.containsAll( REQUIRED_COLUMN_NAMES );
            chromIdx = columnNames.indexOf( COLUMN_CHROM );
            startIdx = columnNames.indexOf( COLUMN_START );
            endIdx = columnNames.indexOf( COLUMN_END );
            hasHeader = true;
        }
        else
        {
            final int columnNumber = line.split( DELIMITER, -1 ).length;
            columnNames = Arrays.asList( getProperties().generateFieldNames( columnNumber - 3 ) );

            hasHeader = false;
            valid = true;
        }
    }

    private int parseStrand(String[] fields)
    {
        int strand = StrandType.STRAND_NOT_KNOWN;

        if( hasHeader )
        {
            if( columnNames.contains( STRAND ) )
            {
                final int indexOfStrand = columnNames.indexOf( STRAND );
                if( fields[indexOfStrand].equals( "+" ) )
                {
                    strand = StrandType.STRAND_PLUS;
                }
                else if( fields[indexOfStrand].equals( "-" ) )
                {
                    strand = StrandType.STRAND_MINUS;
                }
            }
        }

        return strand;
    }

    private DynamicPropertySet fillDynamicProperties(String[] fields)
    {
        final DynamicPropertySet properties = new DynamicPropertySetAsMap();
        if( hasHeader )
        {
            final List<String> additionColumnNames = new ArrayList<>( columnNames );
            additionColumnNames.removeAll( REQUIRED_COLUMN_NAMES );
            additionColumnNames.remove( STRAND );

            for( String columnName : additionColumnNames )
            {
                final int indColumn = columnNames.indexOf( columnName );
                final DynamicProperty property = createDynamicProperty( fields[indColumn], columnName );

                properties.add( property );
            }
        }
        else
        {
            try
            {
                for( int i = 0; i < columnNames.size(); i++ )
                {
                    final String columnName = columnNames.get( i );
                    final DynamicProperty property = createDynamicProperty( fields[i+3], columnName );

                    properties.add( property );
                }
            }
            catch( Exception e )
            {
                return null;
            }
        }

        return properties;
    }

    public static DynamicProperty createDynamicProperty(String fieldString, String columnName)
    {
        final Object value = detectType( fieldString );
        final Class<?> type = value.getClass();
        return new DynamicProperty( columnName, type, value );
    }

    public static Object detectType(String value)
    {
        if( TextUtil.isIntegerNumber( value ) )
        {
            return Integer.valueOf( value );
        } else if( TextUtil.isFloatingPointNumber( value ) )
        {
            return Double.valueOf( value );
        }

        return value;
    }
}
