package ru.biosoft.bsa.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.PropertiesDPS;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class VCFTrackImporter extends TrackImporter
{
    private static final String TYPE_FLOAT = "Float";
    private static final String TYPE_INTEGER = "Integer";
    private static final Logger log = Logger.getLogger( VCFTrackImporter.class.getName() );
    protected static final Pattern NUCLEOTIDE_PATTERN = Pattern.compile("[actgn]+|\\.", Pattern.CASE_INSENSITIVE);
    public static final String VCF_FORMAT_PREFIX = "Format_";

    protected void putParameter(Properties parameters, String id, String s)
    {
        putParameter( parameters, id, s, false );
    }

    protected void putParameter(Properties parameters, String id, String s, boolean putEmpty)
    {
        if(s != null && !s.isEmpty() && !s.equals("."))
            parameters.put(id, s);
        else if( putEmpty )
            parameters.put( id, "" );
    }

    private final Map<String, String> formatTypeMap = new HashMap<>();
    private final Map<String, String> infoTypeMap = new HashMap<>();
    private List<String> sampleIdsList;

    //store current file for logging issues
    private String curFile = "";
    @Override
    public DataElement doImport(@Nonnull DataCollection parent, @Nonnull File file, String elementName, FunctionJobControl jobControl,
            Logger log) throws Exception
    {
        curFile = parent.getCompletePath().toString() + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + elementName;
        DataElement result = super.doImport( parent, file, elementName, jobControl, log );
        curFile = "";
        return result;
    }

    @Override
    protected Site parseLine(String line)
    {
        String[] fields = TextUtil.split(line, '\t');
        if(fields.length < 8) return null;
        String chr = normalizeChromosome(fields[0]);
        int start;
        try
        {
            start = Integer.parseInt(fields[1]);
        }
        catch( NumberFormatException e )
        {
            return null;
        }
        Properties parameters = new Properties();
        putParameter(parameters, "name", fields[2]);
        String altAllele = fields[4];
        if(!NUCLEOTIDE_PATTERN.matcher(altAllele).matches())
            return null;
        putParameter( parameters, "AltAllele", altAllele, true );
        if(!fields[5].equals("."))
        {
            try
            {
                if(Double.isNaN(Double.parseDouble(fields[5]))) return null;
            }
            catch( NumberFormatException e )
            {
                return null;
            }
        }
        putParameter(parameters, "Quality", fields[5]);
        putParameter( parameters, "Filter", fields[6].replace( ",", ";" ) );
        String refAllele = fields[3];
        if(!NUCLEOTIDE_PATTERN.matcher(refAllele).matches())
            return null;
        int length = refAllele.length();
        putParameter( parameters, "RefAllele", refAllele, true );
        for(String info: TextUtil.split( fields[7], ';' ))
        {
            int pos = info.indexOf("=");
            if(pos > 0)
            {
                String value = info.substring(pos+1);
                String key = info.substring(0, pos);
                if(key.equals("END"))
                {
                    try
                    {
                        length = Integer.parseInt(value)-start;
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
                String fieldType = infoTypeMap.getOrDefault( key, null );
                Object realValue = getValueWithRealType( value, fieldType );
                parameters.put( "Info_" + key, realValue );
            } else
                parameters.put("Info_"+info, "+");
        }
        if(fields.length > 9)
        {
            String[] formatFields = TextUtil.split( fields[8], ':' );
            for( int j = 0; j < fields.length - 9; j++ )
            {

                String[] formatValues = TextUtil.split( fields[j + 9], ':' );
                String sampleName = ( sampleIdsList.size() > j ) ? sampleIdsList.get( j ) : j + "";
                for( int i = 0; i < Math.min( formatFields.length, formatValues.length ); i++ )
                {
                    String fieldType = formatTypeMap.getOrDefault( formatFields[i], null );
                    Object value = getValueWithRealType( formatValues[i], fieldType );
                    parameters.put( VCF_FORMAT_PREFIX + formatFields[i] + "_" + sampleName, value );
                }
            }
        }
        String type = SiteType.TYPE_VARIATION;
        if(altAllele.startsWith("<") && altAllele.endsWith(">")) type = altAllele.substring(1, altAllele.length()-1);
        return new SiteImpl(null, chr, type, Basis.BASIS_USER, start, length, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_NOT_APPLICABLE, null, new PropertiesDPS(parameters));
    }

    @Override
    public boolean init(Properties properties)
    {
        super.init(properties);
        format = "vcf";
        return true;
    }

    @Override
    protected boolean isComment(String line)
    {
        //##INFO=<ID=AD,Number=2,Type=Integer,Description="Allelic depth for the ref and alt alleles in the order listed">
        if(line.startsWith("##INFO=<") && importerProperties != null)
        {
            try
            {
                String id = null, type = null;
                StringBuilder descriptionSB = new StringBuilder();
                boolean insideDescription = false;
                String number = null;
                for(String infoField: line.substring("##INFO=<".length(), line.length()-1).split(","))
                {
                    if( insideDescription )
                    {
                        descriptionSB.append( "," ).append( infoField );
                        if( infoField.endsWith( "\"" ) && !infoField.endsWith( "\\\"" ) )
                            insideDescription = false;
                        continue;
                    }
                    String[] fields = TextUtil.split( infoField, '=' );
                    if( fields[0].equals( "ID" ) )
                        id = fields[1];
                    else if( fields[0].equals( "Type" ) )
                        type = fields[1];
                    else if( fields[0].equals( "Number" ) )
                        number = fields[1];
                    else if( fields[0].equals( "Description" ) )
                    {
                        descriptionSB.append( fields[1] );
                        if( !fields[1].endsWith( "\"" ) || fields[1].endsWith( "\\\"" ) )
                            insideDescription = true;
                    }
                }
                String description = descriptionSB.toString();
                if( id != null && type != null )
                {
                    if( description != null && !description.isEmpty()
                            && !importerProperties.getTrackProperties().containsKey( "Info_" + id ) )
                        importerProperties.getTrackProperties().setProperty( "Info_" + id, type + ":" + description );

                    //TODO: do something with multiple numbers (e.g. comma-separated)
                    if( saveType( type ) && "1".equals( number ) )
                        infoTypeMap.put( id, type );
                }
            }
            catch( Exception e )
            {
            }
        }
        //##FILTER=<ID=noisy_intronic_variant,Description="Noisy Intronic Variant, by someone">
        else if( line.startsWith( "##FILTER=<" ) && importerProperties != null )
        {
            try
            {
                String id = null;
                StringBuilder descriptionSB = new StringBuilder();
                boolean insideDescription = false;
                for( String filterField : line.substring( "##FILTER=<".length(), line.length() - 1 ).split( "," ) )
                {
                    if( insideDescription )
                    {
                        descriptionSB.append( "," ).append( filterField );
                        if( filterField.endsWith( "\"" ) && !filterField.endsWith( "\\\"" ) )
                            insideDescription = false;
                        continue;
                    }
                    String[] fields = TextUtil.split( filterField, '=' );
                    if( fields[0].equals( "ID" ) )
                        id = fields[1];
                    else if( fields[0].equals( "Description" ) )
                    {
                        descriptionSB.append( fields[1] );
                        if( !fields[1].endsWith( "\"" ) || fields[1].endsWith( "\\\"" ) )
                            insideDescription = true;
                    }
                }
                String description = removeQuotes( descriptionSB.toString(), "Invalid 'Description' format in FILTER" );
                if( id != null && description != null && !importerProperties.getTrackProperties().containsKey( "Filter_" + id ) )
                {
                    importerProperties.getTrackProperties().setProperty( "Filter_" + id, description );
                }
            }
            catch( Exception e )
            {
            }
        }
        //##contig=<ID=chrY,length=59373566>
        //##contig=<ID=chr1_gl000191_random,length=106433>
        else if( line.startsWith( "##contig=<" ) && importerProperties != null )
        {
            try
            {
                String id = null;
                String length = null;
                for( String contigField : line.substring( "##contig=<".length(), line.length() - 1 ).split( "," ) )
                {
                    String[] fields = TextUtil.split( contigField, '=' );
                    if( fields[0].equals( "ID" ) )
                        id = fields[1];
                    else if( fields[0].equals( "length" ) )
                        length = fields[1];
                }
                if( id != null && length != null && !importerProperties.getTrackProperties().containsKey( "contig_" + id ) )
                {
                    importerProperties.getTrackProperties().setProperty( "contig_" + id, length );
                }
            }
            catch( Exception e )
            {
            }
        }
        //##FORMAT=<ID=GT,Number=1,Type=String,Description="Genotype">
        //##FORMAT=<ID=GQ,Number=1,Type=Integer,Description="Genotype Quality">
        else if( line.startsWith( "##FORMAT=<" ) && importerProperties != null )
        {
            try
            {
                String id = null;
                String type = null;
                String number = null;
                for( String contigField : line.substring( "##FORMAT=<".length(), line.length() - 1 ).split( "," ) )
                {
                    String[] fields = TextUtil.split( contigField, '=' );
                    if( fields[0].equals( "ID" ) )
                        id = fields[1];
                    else if( fields[0].equals( "Type" ) )
                        type = fields[1];
                    else if( fields[0].equals( "Number" ) )
                        number = fields[1];
                }
                if( id != null && saveType( type ) && number != null )
                {
                    if( "1".equals( number ) )
                        formatTypeMap.put( id, type );

                    if( !importerProperties.getTrackProperties().containsKey( VCF_FORMAT_PREFIX + id ) )
                        importerProperties.getTrackProperties().setProperty( VCF_FORMAT_PREFIX + id, type );
                }
            }
            catch( Exception e )
            {
            }
        }
        //Header line
        //#CHROM POS ID REF ALT QUAL FILTER INFO
        else if( line.startsWith( "#CHROM" ) )
        {
            String[] fields = TextUtil.split( line, '\t' );
            if( fields.length > 8 )
            {
                if( fields[8].equals( "FORMAT" ) )
                {
                    sampleIdsList = new ArrayList<>();
                    for( int i = 9; i < fields.length; i++ )
                    {
                        sampleIdsList.add( fields[i] );
                    }
                }
            }
        }

        return line.startsWith("#");
    }

    protected Object getValueWithRealType(String value, String type)
    {
        try
        {
            //process only integer and float
            if( TYPE_INTEGER.equals( type ) )
            {
                return Integer.valueOf( value );
            }
            else if( TYPE_FLOAT.equals( type ) )
            {
                return Double.valueOf( value );
            }
        }
        catch( NumberFormatException e )
        {
        }
        return value;
    }

    private boolean saveType(String type)
    {
        return TYPE_INTEGER.equals( type ) || TYPE_FLOAT.equals( type );
    }

    //TODO: the better idea is to fail instead of logging
    private String removeQuotes(String quotedValue, String msgPrefix)
    {
        String result = quotedValue;
        if( result.isEmpty() )
        {
            log.log( Level.SEVERE, msgPrefix + ": empty value in file '" + curFile + "'" );
            return result;
        }
        if( result.startsWith( "\"" ) )
            result = result.substring( 1 );
        else
            log.log( Level.SEVERE, msgPrefix + ": opening '\"' missed in file '" + curFile + "'" );

        if( result.endsWith( "\"" ) && !result.endsWith( "\\\"" ) )
            result = result.substring( 0, result.length() - 1 );
        else
            log.log( Level.SEVERE, msgPrefix + ": closing '\"' missed in file '" + curFile + "'" );
        return result;
    }

    @Override
    protected Class<? extends WritableTrack> getTrackClass()
    {
        return VCFSqlTrack.class;
    }
}
