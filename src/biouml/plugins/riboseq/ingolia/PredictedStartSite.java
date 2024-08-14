package biouml.plugins.riboseq.ingolia;

import biouml.plugins.riboseq.transcripts.Transcript;
import ru.biosoft.bsa.Interval;

public class PredictedStartSite
{
    private Transcript transcript;
    public Transcript getTranscript()
    {
        return transcript;
    }
    public void setTranscript(Transcript transcript)
    {
        this.transcript = transcript;
    }

    private Interval peak;
    public Interval getPeak()
    {
        return peak;
    }
    public void setPeak(Interval peak)
    {
        this.peak = peak;
    }

    private String peakSequence;
    public String getPeakSequence()
    {
        return peakSequence;
    }
    public void setPeakSequence(String peakSequence)
    {
        this.peakSequence = peakSequence;
    }

    private Integer initCodonOffset;
    public Integer getInitCodonOffset()
    {
        return initCodonOffset;
    }
    public void setInitCodonOffset(Integer initCodonOffset)
    {
        this.initCodonOffset = initCodonOffset;
    }
    
    private Double initCodonScore;
    public Double getInitCodonScore()
    {
        return initCodonScore;
    }
    public void setInitCodonScore(Double initCodonScore)
    {
        this.initCodonScore = initCodonScore;
    }

    private int summitOffset;
    public int getSummitOffset()
    {
        return summitOffset;
    }
    public void setSummitOffset(int summitOffset)
    {
        this.summitOffset = summitOffset;
    }
    
    private double summitScore;
    public double getSummitScore()
    {
        return summitScore;
    }
    public void setSummitScore(double summitScore)
    {
        this.summitScore = summitScore;
    }

    private String initCodon;
    public String getInitCodon()
    {
        return initCodon;
    }
    public void setInitCodon(String initCodon)
    {
        this.initCodon = initCodon;
    }

    private Integer CDSLength;
    public Integer getCDSLength()
    {
        return CDSLength;
    }
    public void setCDSLength(Integer cDSLength)
    {
        CDSLength = cDSLength;
    }

    private Integer offsetFromKnownCDSStart;
    public Integer getOffsetFromKnownCDSStart()
    {
        return offsetFromKnownCDSStart;
    }
    public void setOffsetFromKnownCDSStart(Integer offsetFromKnownCDSStart)
    {
        this.offsetFromKnownCDSStart = offsetFromKnownCDSStart;
    }

    private Integer offsetFromKnownCDSEnd;
    public Integer getOffsetFromKnownCDSEnd()
    {
        return offsetFromKnownCDSEnd;
    }
    public void setOffsetFromKnownCDSEnd(Integer offsetFromKnownCDSEnd)
    {
        this.offsetFromKnownCDSEnd = offsetFromKnownCDSEnd;
    }

    private StartSiteType type;
    public StartSiteType getType()
    {
        return type;
    }
    public void setType(StartSiteType type)
    {
        this.type = type;
    }

    private String proteinSequence;
    public String getProteinSequence()
    {
        return proteinSequence;
    }
    public void setProteinSequence(String proteinSequence)
    {
        this.proteinSequence = proteinSequence;
    }

    private Integer readsNumber;
    public Integer getReadsNumber()
    {
        return readsNumber;
    }
    public void setReadsNumber(Integer readsNumber)
    {
        this.readsNumber = readsNumber;
    }
}
