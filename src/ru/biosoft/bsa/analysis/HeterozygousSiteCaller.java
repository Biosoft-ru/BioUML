package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.PileupElement;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class HeterozygousSiteCaller extends AnalysisMethodSupport<HeterozygousSiteCaller.Parameters>
{
    private static PropertyDescriptor firstAlleleDescriptor = StaticDescriptor.create( "First allele" );
    private static PropertyDescriptor firstAlleleCountDescriptor = StaticDescriptor.create( "First allele count" );
    private static PropertyDescriptor secondAlleleDescriptor = StaticDescriptor.create( "Second allele" );
    private static PropertyDescriptor secondAlleleCountDescriptor = StaticDescriptor.create( "Second allele count" );
    private static PropertyDescriptor totalCountDescriptor = StaticDescriptor.create( "Total count" );
    private static PropertyDescriptor letterCountsDescriptor = StaticDescriptor.create( "Letter counts" );

    private static final int CHUNK_SIZE = 10000;

    public HeterozygousSiteCaller(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        //checkGreater( "minReads", 1 );
        checkRange( "minSecondAlleleFraction", 0, 1 );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final BAMTrack alignmentTrack = parameters.getAlignmentTrack().getDataElement(BAMTrack.class);
        final Track targetTrack = parameters.getTargetTrack() == null ? null : parameters.getTargetTrack().getDataElement( Track.class );

        final SqlTrack result = SqlTrack.createTrack( parameters.getOutputTrack(), alignmentTrack );
        jobControl.forCollection( alignmentTrack.getGenomeSelector().getSequenceCollectionPath().getChildren(),
                chrPath -> {
                    try
                    {
                        Sequence chr = chrPath.getDataElement(AnnotatedSequence.class).getSequence();

                        DataCollection<Site> targetSites = null;
                        if( targetTrack == null )
                        {
                            targetSites = new VectorDataCollection<>("Sites", Site.class, null);
                            targetSites.put( new SiteImpl( null, chrPath.getName(), SiteType.TYPE_MISC_FEATURE, Site.BASIS_ANNOTATED,
                                    chr.getStart(), chr.getLength(), Site.STRAND_NOT_APPLICABLE, chr ) );
                        }
                        else
                            targetSites = targetTrack.getSites( chrPath.toString(), chr.getStart(), chr.getStart() + chr.getLength() );

                        for(Site site : targetSites)
                        {
                            for( int chunkStart = site.getFrom(); chunkStart <= site.getTo(); chunkStart += CHUNK_SIZE )
                            {
                                int chunkEnd = Math.min( chunkStart + CHUNK_SIZE - 1, site.getTo() );
                                DataCollection alignments = alignmentTrack.getSites( chrPath.toString(), chunkStart, chunkEnd );
                                if( alignments.getSize() < parameters.getMinReads() )
                                    continue;
                                PileupElement[] elements = PileupElement.getElements( alignments, chunkStart, chunkEnd );
                                for( int i = 0; i < elements.length; i++ )
                                {
                                    PileupElement pileup = elements[i];
                                    callAt( chunkStart + i, chr, pileup, result );
                                }
                            }
                        }
                    }
                    catch( Exception e )
                    {
                        throw ExceptionRegistry.translateException(e);
                    }
                    return true;
                } );
        result.finalizeAddition();
        return result;
    }

    private void callAt(int pos, Sequence chr, PileupElement pileup, SqlTrack result) throws Exception
    {
        if( pileup.getSize() < parameters.getMinReads() )
            return;
        double[] letterCounts = pileup.getCodeCounts();

        double totalCount = 0;
        for( double c : letterCounts )
            totalCount += c;
        if( totalCount < parameters.getMinReads() )
            return;


        byte[] codes = pileup.getCodesOrderByCount();
        byte l1 = codes[0];
        byte l2 = codes[1];
        double c1 = pileup.getCodeCount( l1 );
        double c2 = pileup.getCodeCount( l2 );
        if( c2 < parameters.getMinSecondAlleleFraction() * totalCount )
            return;

        DynamicPropertySet properties = new DynamicPropertySetSupport();
        properties.add(new DynamicProperty(firstAlleleDescriptor, String.class, pileup.getAlphabet().codeToLetters(l1)));
        properties.add(new DynamicProperty(firstAlleleCountDescriptor, Float.class, c1));
        properties.add(new DynamicProperty(secondAlleleDescriptor, String.class, pileup.getAlphabet().codeToLetters(l2)));
        properties.add( new DynamicProperty( secondAlleleCountDescriptor, Float.class, c2 ) );
        properties.add( new DynamicProperty( totalCountDescriptor, Float.class, totalCount ) );

        Double[] counts = ArrayUtils.toObject(pileup.getCodeCounts());
        properties.add( new DynamicProperty( letterCountsDescriptor, String.class, StringUtils.join( counts, ";" ) ) );

        SiteImpl site = new SiteImpl( result, null, SiteType.TYPE_MISC_SIGNAL, Basis.BASIS_PREDICTED, pos, 1, Precision.PRECISION_EXACTLY,
                StrandType.STRAND_BOTH, chr, properties );
        result.addSite( site );
    }

    @SuppressWarnings ( "serial" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath alignmentTrack, targetTrack, outputTrack;
        private int minReads = 20;
        private double minSecondAlleleFraction = 0.3;

        @PropertyName ( "Alignment track" )
        @PropertyDescription ( "BAM track with aligned reads" )
        public DataElementPath getAlignmentTrack()
        {
            return alignmentTrack;
        }

        public void setAlignmentTrack(DataElementPath alignmentTrack)
        {
            Object oldValue = this.alignmentTrack;
            this.alignmentTrack = alignmentTrack;
            firePropertyChange( "alignmentTrack", oldValue, alignmentTrack );
        }

        @PropertyName( "Target track" )
        @PropertyDescription( "Restrict search to sites from this track" )
        public DataElementPath getTargetTrack()
        {
            return targetTrack;
        }

        public void setTargetTrack(DataElementPath targetTrack)
        {
            Object oldValue = this.targetTrack;
            this.targetTrack = targetTrack;
            firePropertyChange( "targetTrack", oldValue, targetTrack );
        }

        @PropertyName ( "Output track" )
        @PropertyDescription ( "Output track" )
        public DataElementPath getOutputTrack()
        {
            return outputTrack;
        }

        public void setOutputTrack(DataElementPath outputTrack)
        {
            Object oldValue = this.outputTrack;
            this.outputTrack = outputTrack;
            firePropertyChange( "outputTrack", oldValue, outputTrack );
        }

        @PropertyName ( "Min reads" )
        @PropertyDescription ( "Minimal number of reads at genomic position to be considered" )
        public int getMinReads()
        {
            return minReads;
        }

        public void setMinReads(int minReads)
        {
            Object oldValue = this.minReads;
            this.minReads = minReads;
            firePropertyChange( "minReads", oldValue, minReads );
        }

        @PropertyName ( "Minimal fraction of second allele" )
        @PropertyDescription ( "Minimal fraction of second allele" )
        public double getMinSecondAlleleFraction()
        {
            return minSecondAlleleFraction;
        }

        public void setMinSecondAlleleFraction(double minSecondAlleleFraction)
        {
            Object oldValue = this.minSecondAlleleFraction;
            this.minSecondAlleleFraction = minSecondAlleleFraction;
            firePropertyChange( "minSecondAlleleFraction", oldValue, minSecondAlleleFraction );
        }


    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "alignmentTrack" ).inputElement( BAMTrack.class ).add();
            property( "targetTrack" ).inputElement( Track.class ).canBeNull().add();
            add( "minReads" );
            add( "minSecondAlleleFraction" );
            property( "outputTrack" ).outputElement( SqlTrack.class ).auto( "$alignmentTrack$ heterozygous sites" ).add();
        }
    }


}
