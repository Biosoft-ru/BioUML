package ru.biosoft.bsa.exporter;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import one.util.streamex.EntryStream;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MessageBundle;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.util.TextUtil;

/**
 * @author lan
 *
 */
public class VCFExporter extends TrackExporter
{
    public static class VCFInfo
    {
        Map<String, String> infoFields = new TreeMap<>();
        Map<String, String> formatFields = new TreeMap<>();
        Map<String, String> filterFields = new TreeMap<>();
        Map<String, String> contigFields = new TreeMap<>();
        List<String> sampleIds = new ArrayList<>();
    }
    
    @Override
    public boolean accept(TrackRegion trackRegion)
    {
        if(trackRegion.getSequence() == null)
        {
            DataCollection<Site> dc = null;
            try
            {
                dc = trackRegion.getTrack().getAllSites();
            }
            catch(UnsupportedOperationException e) {}
            return dc != null;
        }
        return true;
    }

    @Override
    public void doExport(TrackRegion trackRegion, File file, FunctionJobControl jobControl) throws Exception
    {
        boolean firstSiteWritten = false;
        try (PrintWriter pw = new PrintWriter( file, "UTF-8" ))
        {
            pw.println( "##fileformat=VCFv4.0" );
            Calendar calendar = Calendar.getInstance();
            pw.printf( "##fileDate=%04d%02d%02d\n", calendar.get( Calendar.YEAR ), calendar.get( Calendar.MONTH ),
                    calendar.get( Calendar.DAY_OF_MONTH ) );
            pw.println( "##source=" + Application.getGlobalValue( "ApplicationTitle" ) );
            if( trackRegion.getTrack() instanceof DataCollection )
            {
                String genomeId = ( (DataCollection<?>)trackRegion.getTrack() ).getInfo().getProperty( Track.GENOME_ID_PROPERTY );
                if( genomeId != null )
                    pw.println( "##reference=" + genomeId );
            }
            VCFInfo vcfInfo = null;
            if( trackRegion.getSequenceObject() == null )
            {
                DataCollection<Site> dc = trackRegion.getTrack().getAllSites();
                int nSites = dc.getSize();
                Iterator<Site> it = parameters.getIterator( trackRegion );
                int i = 0;
                while(it.hasNext())
                {
                    Site s = it.next();
                    if(!firstSiteWritten) {
                        vcfInfo = getVCFInfo(s.getProperties());
                        getFilter( vcfInfo, trackRegion );
                        getContig( vcfInfo, trackRegion );
                        writeHeader(pw, s.getProperties(), vcfInfo);
                        firstSiteWritten = true;
                    }
                    writeSite(pw, s, vcfInfo);
                    i++;
                    if( i % 5000 == 0 && jobControl != null )
                    {
                        jobControl.setPreparedness( (int)Math.floor( i * 100. / nSites ) );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        {
                            file.delete();
                            return;
                        }
                    }
                }
            }
            else
            {
                // Split sequence to regions of like 5000 sites each
                // Assuming that sites are distributed evenly through the sequence
                int nSites = trackRegion.countSites();
                for( Interval interval : trackRegion.getInterval().split( nSites / 5000 ) )
                {
                    for( Site s : trackRegion.getSites( interval ) )
                    {
                        if( s.getFrom() < interval.getFrom() && interval.getFrom() > trackRegion.getFrom() )
                            continue;
                        if( !firstSiteWritten )
                        {
                            vcfInfo = getVCFInfo( s.getProperties() );
                            getFilter( vcfInfo, trackRegion );
                            getContig( vcfInfo, trackRegion );
                            writeHeader( pw, s.getProperties(), vcfInfo );
                            firstSiteWritten = true;
                        }
                        writeSite( pw, s, vcfInfo );
                    }
                    if( jobControl != null )
                    {
                        jobControl.setPreparedness( (int)Math.floor( trackRegion.getInterval().getPointPos( interval.getTo() ) * 100 ) );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        {
                            file.delete();
                            return;
                        }
                    }
                }
            }
        }
    }
    
    private void writeSite(PrintWriter pw, Site s, VCFInfo vcfInfo)
    {
        DynamicPropertySet properties = s.getProperties();
        int numFields = 8;
        if( !vcfInfo.formatFields.isEmpty() )
        {
            numFields++;
            if( !vcfInfo.sampleIds.isEmpty() )
                numFields += vcfInfo.sampleIds.size();
            else
                numFields++;
        }
        String[] fields = new String[numFields];
        String chr = getChromosomeName( s );
        fields[0] = chr;
        fields[1] = String.valueOf(s.getFrom());
        Object value = properties.getValue("name");
        fields[2] = (value == null || value.equals("")) ? "." : value.toString();
        value = properties.getValue("RefAllele");
        fields[3] = (value == null || value.equals("")) ? "." : value.toString();
        value = properties.getValue("AltAllele");
        fields[4] = (value == null || value.equals("")) ? "." : value.toString();
        value = properties.getValue("Quality");
        fields[5] = (value == null || value.equals("")) ? "." : value.toString();
        value = properties.getValue("Filter");
        fields[6] = (value == null || value.equals("")) ? "PASS" : value.toString();
        final boolean skipSpaces = getParameters().skipSpaces();
        fields[7] = vcfInfo.infoFields.isEmpty() ? "." : EntryStream.of( vcfInfo.infoFields ).mapValues( properties::getValue )
                /*.filterValues( this::nonEmpty )*/.filterValues( v -> isValid( v, skipSpaces ) ).join( "=" ).joining( ";" );
        if(!vcfInfo.formatFields.isEmpty())
        {
            if( vcfInfo.sampleIds.isEmpty() )
            {
                StringBuilder formatNames = new StringBuilder();
                StringBuilder formatValues = new StringBuilder();
                if( vcfInfo.formatFields.containsKey( "GT" ) )//GT should be first as required by GATK
                {
                    formatNames.append( "GT" );
                    formatValues.append( properties.getValue( vcfInfo.formatFields.get( "GT" ) ) );
                }
                for( Entry<String, String> formatField : vcfInfo.formatFields.entrySet() )
                {
                    if( formatField.getKey().equals( "GT" ) )
                        continue;
                    value = properties.getValue( formatField.getValue() );
                    if( nonEmpty( value ) )
                    {
                        if( formatNames.length() > 0 )
                            formatNames.append( ":" );
                        formatNames.append( formatField.getKey() );
                        if( formatValues.length() > 0 )
                            formatValues.append( ":" );
                        formatValues.append( value );
                    }
                }
                fields[8] = formatNames.toString();
                fields[9] = formatValues.toString();
            }
            else
            {
                StringBuilder formatNames = new StringBuilder();
                Map<String, StringBuilder> formatValues = new HashMap<>();
                for( String sample : vcfInfo.sampleIds )
                    formatValues.put( sample, new StringBuilder() );
                if( vcfInfo.formatFields.containsKey( "GT" ) )//GT should be first as required by GATK
                {
                    formatNames.append( "GT" );
                    for( String sample : vcfInfo.sampleIds )
                        formatValues.get( sample ).append( properties.getValue( vcfInfo.formatFields.get( "GT" ) + "_" + sample ) );
                }
                for( Entry<String, String> formatField : vcfInfo.formatFields.entrySet() )
                {
                    if( formatField.getKey().equals( "GT" ) )
                        continue;
                    if(formatNames.length() > 0) formatNames.append(":");
                    formatNames.append( formatField.getKey() );
                    for( String sample : vcfInfo.sampleIds )
                    {
                        value = properties.getValue( formatField.getValue() + "_" + sample );
                        StringBuilder sb = formatValues.get( sample );
                        if( sb.length() > 0 )
                            sb.append( ":" );
                        if( nonEmpty( value ) )
                            sb.append( value );
                    }
                }
                fields[8] = formatNames.toString();
                int j = 0;
                for( String sample : vcfInfo.sampleIds )
                {
                    fields[9 + j++] = formatValues.get( sample ).toString();
                }
            }
        }
        pw.println(String.join("\t", fields));
    }

    /**
     * Checks that value is valid property for VCFv4.2 (white-spaces are allowed in VCFv4.3).
     * @param val property value
     * @return
     */
    protected boolean isValid(Object val, boolean skipSpaces)
    {
        if( val == null )
            return false;
        if( val.equals( "" ) )
            return false;
        if( skipSpaces && val instanceof String && ( (String)val ).contains( " " ) )
            return false;
        return true;
    }

    protected boolean nonEmpty(Object val)
    {
        return val != null && !val.equals( "" );
    }
    
    private VCFInfo getVCFInfo(DynamicPropertySet properties)
    {
        VCFInfo result=  new VCFInfo();
        Iterator<String> nameIterator = properties.nameIterator();
        Set<String> sampleIds = new HashSet<>();
        while(nameIterator.hasNext())
        {
            String propertyName = nameIterator.next();
            if(propertyName.equals("AltAllele") || propertyName.equals("name") || propertyName.equals("RefAllele") || propertyName.equals("Quality") || propertyName.equals("Filter"))
                continue;
            if(propertyName.startsWith("Format_"))
            {
                String format = propertyName.substring( "Format_".length() );
                int _pos = format.indexOf( "_" );
                if( _pos > -1 )
                {
                    //Multy-sample VCF
                    String fname = format.substring( 0, _pos );
                    String sample = format.substring( _pos + 1 );
                    result.formatFields.put( fname, "Format_" + fname );

                    sampleIds.add( sample );
                }
                else
                    result.formatFields.put( propertyName.substring( "Format_".length() ), propertyName );
            }
            else if(propertyName.startsWith("Info_"))
                result.infoFields.put(propertyName.substring("Info_".length()), propertyName);
            else result.infoFields.put(propertyName, propertyName);
        }
        if( !sampleIds.isEmpty() )
            result.sampleIds.addAll( sampleIds );
        return result;
    }

    private void getFilter(VCFInfo result, TrackRegion trackRegion)
    {
        try
        {
            Track track = trackRegion.getTrack();
            Properties props = null;
            if( track instanceof DataCollection )
                props = ( (DataCollection<?>)track ).getInfo().getProperties();
            if( props != null )
            {
                Iterator<Object> nameIterator = props.keySet().iterator();
                while( nameIterator.hasNext() )
                {
                    String propertyName = nameIterator.next().toString();
                    if( propertyName.startsWith( "Filter_" ) )
                    {
                        result.filterFields.put( propertyName.substring( "Filter_".length() ), props.getProperty( propertyName ) );
                    }
                }
            }
            if( result.filterFields.isEmpty() )
            {
                Iterator<Site> iterator = trackRegion.getTrack().getAllSites().iterator();
                while( iterator.hasNext() )
                {
                    Site site = iterator.next();
                    if( !site.getProperties().hasProperty( "Filter" ) )
                        break;
                    Object value = site.getProperties().getValue( "Filter" );
                    String filter = ( value == null || value.equals( "" ) ) ? "PASS" : value.toString();
                    String[] filterVals = TextUtil.split( filter, ';' );
                    for( String f : filterVals )
                        result.filterFields.put( f, f );
                }
            }
        }
        catch( Exception e )
        {
        }
    }

    private void getContig(VCFInfo result, TrackRegion trackRegion)
    {
        try
        {
            Track track = trackRegion.getTrack();
            Properties props = null;
            if( track instanceof DataCollection )
                props = ( (DataCollection<?>)track ).getInfo().getProperties();
            if( props != null )
            {
                Iterator<Object> nameIterator = props.keySet().iterator();
                while( nameIterator.hasNext() )
                {
                    String propertyName = nameIterator.next().toString();
                    if( propertyName.startsWith( "contig_" ) )
                    {
                        result.contigFields.put( propertyName.substring( "contig_".length() ), props.getProperty( propertyName ) );
                    }
                }
            }
        }
        catch( Exception e )
        {
        }
        if( result.contigFields.isEmpty() )
        {
            try
            {
                DataElementPath sequenceCollectionPath = TrackUtils.getTrackSequencesPath( trackRegion.getTrack() );
                DataCollection<AnnotatedSequence> sequenceCollection = sequenceCollectionPath.getDataCollection( AnnotatedSequence.class );
                for( AnnotatedSequence as : sequenceCollection )
                {
                    String chrName = as.getName();
                    if( getParameters().isPrependChrPrefix() )
                        chrName = convertChromosomeName( chrName );
                    result.contigFields.put( as.getName(), String.valueOf( as.getSequence().getLength() ) );
                }
            }
            catch( Exception e )
            {
            }
        }

    }

    private void writeHeader(PrintWriter pw, DynamicPropertySet properties, VCFInfo vcfInfo)
    {
        for( Entry<String, String> infoField : vcfInfo.filterFields.entrySet() )
        {
            pw.println( "##FILTER=<ID=" + infoField.getKey() + ",Description=\"" + infoField.getValue() + "\">" );
        }
        for(Entry<String, String> infoField: vcfInfo.infoFields.entrySet())
        {
            Class<?> type = properties.getProperty(infoField.getValue()).getType();
            pw.println("##INFO=<ID="+infoField.getKey()+",Number=.,Type="+(type==Integer.class?"Integer":type==Float.class?"Float":type==Double.class?"Float":"String")+",Description=\""+infoField.getKey()+"\">");
        }
        for(Entry<String, String> formatField: vcfInfo.formatFields.entrySet())
        {
            String propertyName = vcfInfo.sampleIds.isEmpty() ? formatField.getValue()
                    : formatField.getValue() + "_" + vcfInfo.sampleIds.iterator().next();
            Class<?> type = properties.getProperty( propertyName ).getType();
            pw.println("##FORMAT=<ID="+formatField.getKey()+",Number=.,Type="+(type==Integer.class?"Integer":type==Float.class?"Float":type==Double.class?"Float":"String")+",Description=\""+formatField.getKey()+"\">");
        }
        for( Entry<String, String> contigField : vcfInfo.contigFields.entrySet() )
        {
            pw.println( "##contig=<ID=" + convertChromosomeName( contigField.getKey() ) + ",length=" + contigField.getValue() + ">" );
        }
        pw.println( "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO"
                + ( vcfInfo.formatFields.isEmpty() ? ""
                        : ( vcfInfo.sampleIds.isEmpty() ? "\tFORMAT\tTOTAL"
                                : "\tFORMAT\t" + String.join( "\t", vcfInfo.sampleIds ) ) ) );
    }
    
    private String getChromosomeName(Site s)
    {
        String chr = s.getOriginalSequence().getName();
        return convertChromosomeName( chr );
    }
    private String convertChromosomeName(String chr)
    {
        if(chr.equals("MT")) chr = "M";
        if( getParameters().isPrependChrPrefix() && !chr.startsWith( "chr" ) )
            chr = "chr" + chr;
        return chr;
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return suffix.equalsIgnoreCase("vcf");
    }
    
    @Override
    protected BaseParameters createParameters(DataElement de, File file)
    {
        return new VCFExportProperties( getTrack( de ) );
    }
    
    public VCFExportProperties getParameters()
    {
        return (VCFExportProperties)parameters;
    }
    
    @SuppressWarnings ( "serial" )
    public static class VCFExportProperties extends BaseParameters
    {
        private static final String VCF_v_4_2 = "VCFv4.2";
        private static final String VCF_v_4_3 = "VCFv4.3";

        public VCFExportProperties(Track track)
        {
            super( track );
        }

        private boolean prependChrPrefix = false;
        private String vcfVersion = VCF_v_4_2;

        public boolean isPrependChrPrefix()
        {
            return prependChrPrefix;
        }

        public void setPrependChrPrefix(boolean prependChrPrefix)
        {
            Object oldValue = this.prependChrPrefix;
            this.prependChrPrefix = prependChrPrefix;
            firePropertyChange( "prependChrPrefix", oldValue, prependChrPrefix );
        }
        
        public String getVcfVersion()
        {
            return vcfVersion;
        }
        public void setVcfVersion(String vcfVersion)
        {
            if( vcfVersion == null )
                vcfVersion = VCF_v_4_2;
            String version;
            switch( vcfVersion )
            {
                case VCF_v_4_3:
                    version = VCF_v_4_3;
                    break;
                case VCF_v_4_2:
                    version = VCF_v_4_2;
                    break;
                default:
                    version = VCF_v_4_2;
                    break;
            }
            String oldValue = this.vcfVersion;
            this.vcfVersion = version;
            firePropertyChange( "vcfVersion", oldValue, version );
        }

        public boolean skipSpaces()
        {
            return VCF_v_4_2.equals( vcfVersion );
        }

        public static String[] getAvailableVCFVersions()
        {
            return new String[] {VCF_v_4_2, VCF_v_4_3};
        }

    }

    public static class VCFExportPropertiesBeanInfo extends BaseParametersBeanInfo
    {
        public VCFExportPropertiesBeanInfo()
        {
            super(VCFExportProperties.class, MessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();
            add( new PropertyDescriptorEx( "prependChrPrefix", beanClass ), getResourceString( "PN_EXPORT_PREPEND_CHR_PREFIX" ),
                    getResourceString( "PD_EXPORT_PREPEND_CHR_PREFIX" ) );
            property( "vcfVersion" ).tags( VCFExportProperties.getAvailableVCFVersions() ).titleRaw( "VCF version" )
                    .descriptionRaw( "Exported document will be correct VCF file of the specified version" ).add();
        }
    }

}
