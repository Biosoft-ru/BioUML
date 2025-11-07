package biouml.plugins.riboseq.util;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.StringUtil;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import one.util.streamex.StreamEx;

import static java.lang.Character.isDigit;

public class SiteUtil
{
    public static final String INTERVAL_DELIMETER = ";";

    public static boolean isSiteReversed(Site site)
    {
        return ( site.getStrand() == StrandType.STRAND_MINUS );
    }

    public static Cigar getCigar(Site site)
    {
        final String CIGAR = "Cigar";
        final String cigarStr = (String)site.getProperties().getValue( CIGAR );

        return decodeCigar( cigarStr );
    }

    public static boolean isCigarContainsNoperator(Cigar cigar)
    {
        if( cigar.numCigarElements() == 1 )
        {
            return false;
        }
        else
        {
            final List<CigarElement> elementList = cigar.getCigarElements();
            for( CigarElement element : elementList )
            {
                final CigarOperator operator = element.getOperator();
                if( operator == CigarOperator.N )
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static Interval getIntronInterval(Site site)
    {
        final Cigar cigar = getCigar( site );

        int intronLength = 0;
        int intronOffset = 0;
        final List<CigarElement> cigarElements = cigar.getCigarElements();
        for( CigarElement element : cigarElements )
        {
            final int elementLength = element.getLength();
            if( element.getOperator() != CigarOperator.N )
            {
                intronOffset += elementLength;
            }
            else
            {
                intronLength = elementLength;
                break;
            }
        }

        final int intronFrom = site.getFrom() + intronOffset;
        final int intronTo = intronFrom + intronLength;
        final Interval intron = new Interval( intronFrom, intronTo );

        return intron;
    }

    public static List<Interval> getExonsFromCigar(Site site)
    {
        final Interval siteInterval = site.getInterval();
        final Interval intronInterval = getIntronInterval( site );

        return siteInterval.remainOfIntersect( intronInterval );
    }

    public static List<Interval> unionExons(List<Interval> exonList1, List<Interval> exonList2)
    {
        final List<Interval> unionExonList;

        if( exonList1.size() == exonList2.size() )
        {
            unionExonList = StreamEx.zip( exonList1, exonList2, Interval::union ).toList();
        }
        else
        {
            unionExonList = new ArrayList<>();
            final Interval exon10 = exonList1.get( 0 );
            final Interval exon20 = exonList2.get( 0 );
            if( exonList1.size() < exonList2.size() )
            {
                final Interval exon21 = exonList2.get( 1 );

                if( exon10.intersects( exon20 ) )
                {
                    unionExonList.add( exon10.union( exon20 ) );
                    unionExonList.add( exon21 );
                }
                else
                {
                    unionExonList.add( exon20 );
                    unionExonList.add( exon10.union( exon21 ) );
                }
            }
            else
            {
                final Interval exon11 = exonList1.get( 1 );

                if( exon10.intersects( exon20 ) )
                {
                    unionExonList.add( exon10.union( exon20 ) );
                    unionExonList.add( exon11 );
                }
                else
                {
                    unionExonList.add( exon10 );
                    unionExonList.add( exon11.union( exon20 ) );
                }
            }
        }

        return unionExonList;
    }

    public static boolean isSiteContainsIntron(Site site)
    {
        final Cigar cigar = getCigar( site );
        return isCigarContainsNoperator( cigar );
    }

    public static List<Interval> getExonsFromProperty(Site site)
    {
        final String EXON_PROPERTY = "exons";

        final Object exonsObj = site.getProperties().getValue( EXON_PROPERTY );
        if( exonsObj != null )
        {
            final String exons = exonsObj.toString();
            return StreamEx.of( exons.split( INTERVAL_DELIMETER ) ).map( str -> new Interval( str ).translateFromSite( site ) ).toList();
        }

        return Collections.emptyList();
    }

    public static String exonListToStr(List<Interval> exonIntervalList)
    {
        return StreamEx.of(exonIntervalList).joining( INTERVAL_DELIMETER );
    }

    public static void leftIntervalSort(List<Site> siteList)
    {
        Collections.sort( siteList, Comparator.comparingInt( Site::getFrom ) );
    }

    private static Cigar decodeCigar(String cigarStr)
    {
        final byte ZERO_BYTE = (byte)'0';

        if( SAMRecord.NO_ALIGNMENT_CIGAR.equals( cigarStr ) )
        {
            return new Cigar();
        }

        final Cigar resultCigar = new Cigar();
        final byte[] cigarBytes = StringUtil.stringToBytes( cigarStr );
        for( int i = 0; i < cigarBytes.length; ++i )
        {
            int length = ( cigarBytes[i] - ZERO_BYTE );
            for( ++i; isDigit( cigarBytes[i] ); ++i )
            {
                length = ( length * 10 ) + cigarBytes[i] - ZERO_BYTE;
            }

            final CigarOperator operator = CigarOperator.characterToEnum( cigarBytes[i] );
            final CigarElement cigarElement = new CigarElement( length, operator );

            resultCigar.add( cigarElement );
        }

        return resultCigar;
    }
}
