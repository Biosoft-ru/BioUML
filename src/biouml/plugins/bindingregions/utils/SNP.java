package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;

/***********/////////////////////////////////////////////////*****************/

public class SNP
{
    String snpID;
    int startPosition;
    String referenceSequenceFragment;
    String changedSequenceFragment;
    
    SNP(String snpID, int startPosition, String referenceSequenceFragment, String changedSequenceFragment)
    {
        this.snpID = snpID;
        this.startPosition = startPosition;
        this.referenceSequenceFragment = referenceSequenceFragment;
        this.changedSequenceFragment = changedSequenceFragment;
    }
    
    public String getSnpID()
    {
        return snpID;
    }
    
    public int getStartPosition()
    {
        return startPosition;
    }

    public String getReferenceSequenceFragment()
    {
        return referenceSequenceFragment;
    }

    public String getChangedSequenceFragment()
    {
        return changedSequenceFragment;
    }
    
    public Sequence getReferenceRegion(Sequence sequence, int matrixlength, int ipsWindow)
    {
        int i = 1 - matrixlength / 2 - ipsWindow / 2;
        int start = getStartPosition() + i;
        int finish = getStartPosition() + getReferenceSequenceFragment().length() - i + 1;
        if( start < 1 )
            start = 1;
        if( finish > sequence.getLength() )
            finish = sequence.getLength();
        return new SequenceRegion(sequence, start, finish - start + 1, false, false);
    }

    public Sequence getReferenceRegion(Sequence sequence, int regionLength)
    {
        int start = getStartPosition() - regionLength / 2;
        return new SequenceRegion(sequence, start, regionLength, false, false);
    }

    public Sequence getChangedRegion(Sequence sequence, int matrixlength, int ipsWindow)
    {
        int i = matrixlength / 2 + ipsWindow / 2;
        int ii = 1 - i;
        int start = getStartPosition() + ii;
        int finish = getStartPosition() + getChangedSequenceFragment().length() - ii + 1;
        if( start < 1 || finish > sequence.getLength() )
            return null;
        Sequence result11 = new SequenceRegion(sequence, start, finish - start + 1, false, false);
        byte[] seq = new byte[finish - start + 1];
        for( int j = 0; j < finish - start + 1; j++ )
            seq[j] = result11.getLetterAt(j + 1);
        Sequence result1 = new LinearSequence(sequence.getName(), seq, Nucleotide15LetterAlphabet.getInstance());
        if( getReferenceSequenceFragment().length() < getChangedSequenceFragment().length() )
        {
            int jj = result1.getLength() - getChangedSequenceFragment().length() + getReferenceSequenceFragment().length();
            for ( int j = result1.getLength(); j >= i + getChangedSequenceFragment().length(); j-- )
            {
                byte letter = result1.getLetterAt(jj--);
                result1.setLetterAt(j, letter);
            }
        }
        if( getReferenceSequenceFragment().length() > getChangedSequenceFragment().length() )
        {
            int jj = i + getChangedSequenceFragment().length();
            for( int j = i + getReferenceSequenceFragment().length(); j <= result1.getLength(); j++ )
            {
                byte letter = result1.getLetterAt(j);
                result1.setLetterAt(jj++, letter);
            }
        }
        String changedFragment = getChangedSequenceFragment();
        int jj = i;
        for( int j = 0; j < getChangedSequenceFragment().length(); j++ )
        {
            char letter0 = changedFragment.charAt(j);
            byte letter = (byte) letter0;
            result1.setLetterAt(jj++, letter);
        }
        return result1;
    }

    /***
     * 
     * @param pathToSequences
     * @param chromosome
     * @param snp
     * @param regionLength
     * @return Sequence[]: Sequence[0] = snpReferenceRegion, Sequence[1] = snpChangedRegion
     */
    public Sequence[] getRegions(Sequence sequence, int regionLength)
    {
        Sequence snpReferenceRegion = getReferenceRegion(sequence, regionLength);
        int length = snpReferenceRegion.getLength();
        int half = length / 2 + 1;
        byte[] seq = new byte[length];
        for( int j = 0; j < length; j++ )
            seq[j] = snpReferenceRegion.getLetterAt(j + 1);
        Sequence snpChangedRegion = new LinearSequence(sequence.getName(), seq, Nucleotide15LetterAlphabet.getInstance());
        if( getReferenceSequenceFragment().length() < getChangedSequenceFragment().length() )
        {
            int jj = snpChangedRegion.getLength() - getChangedSequenceFragment().length() + getReferenceSequenceFragment().length();
            for ( int j = snpChangedRegion.getLength(); j >= half + getChangedSequenceFragment().length(); j-- )
            {
                byte letter = snpChangedRegion.getLetterAt(jj--);
                snpChangedRegion.setLetterAt(j, letter);
            }
        }
        if( getReferenceSequenceFragment().length() > getChangedSequenceFragment().length() )
        {
            int jj = half + getChangedSequenceFragment().length();
            for( int j = half + getReferenceSequenceFragment().length(); j <= snpChangedRegion.getLength(); j++ )
            {
                byte letter = snpChangedRegion.getLetterAt(j);
                snpChangedRegion.setLetterAt(jj++, letter);
            }
        }
        String changedFragment = getChangedSequenceFragment();
        int jj = half;
        for( int j = 0; j < getChangedSequenceFragment().length(); j++ )
        {
            char letter0 = changedFragment.charAt(j);
            byte letter = (byte) letter0;
            snpChangedRegion.setLetterAt(jj++, letter);
        }
        return new Sequence[] {snpReferenceRegion, snpChangedRegion};
    }

    public static Map<String, List<SNP>> getSnpsFromVcfTrack(DataElementPath dep)
    {
        Map<String, List<SNP>> result = new HashMap<>();
        Track snpTrack = dep.getDataElement(Track.class);
        for(Site site: snpTrack.getAllSites())
        {
            String snpId = (String)site.getProperties().getValue("name");
            int startPosition = site.getFrom();
            String referenceSequenceFragment = (String)site.getProperties().getValue("RefAllele");
            String changedSequenceFragment = (String)site.getProperties().getValue("AltAllele");
            String chromosome = site.getSequence().getName();
            if( chromosome == null )
                chromosome = site.getOriginalSequence().getName();
            SNP snp = new SNP(snpId, startPosition, referenceSequenceFragment, changedSequenceFragment);
            List<SNP> snps = null;
            if( result.containsKey(chromosome) )
                snps = result.get(chromosome);
            else
                snps = new ArrayList<>();
            snps.add(snp);
            result.put(chromosome, snps);
        }
        return result;
    }
}