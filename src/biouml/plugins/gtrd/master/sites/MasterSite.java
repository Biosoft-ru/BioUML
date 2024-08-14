package biouml.plugins.gtrd.master.sites;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.gtrd.master.MasterTrack;
import biouml.plugins.gtrd.master.sites.chipexo.ChIPexoPeak;
import biouml.plugins.gtrd.master.sites.chipseq.ChIPSeqPeak;
import biouml.plugins.gtrd.master.sites.dnase.DNaseCluster;
import biouml.plugins.gtrd.master.sites.dnase.DNaseFootprint;
import biouml.plugins.gtrd.master.sites.dnase.DNasePeak;
import biouml.plugins.gtrd.master.sites.dnase.FootprintCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesCluster;
import biouml.plugins.gtrd.master.sites.histones.HistonesPeak;
import biouml.plugins.gtrd.master.sites.mnase.MNasePeak;
import biouml.plugins.gtrd.master.utils.SizeOfUtils;
import ru.biosoft.util.bean.StaticDescriptor;

public class MasterSite extends GenomeLocation
{
    public static final String PREFIX = "ms.";
    protected int version = 1;
    
    protected int summit;
    
    protected String reliabilityLevel = "stable";// stable/candidate/unreliable
    protected float reliabilityScore = 1;
    
    protected List<ChIPSeqPeak> chipSeqPeaks = new ArrayList<>();
    protected List<ChIPexoPeak> chipExoPeaks = new ArrayList<>();
    
    protected List<HistonesPeak> histonesPeaks = new ArrayList<>();
    protected List<HistonesCluster> histonesClusters = new ArrayList<>();
    
    protected List<PWMMotif> motifs = new ArrayList<>();
    
    protected List<DNasePeak> dnasePeaks = new ArrayList<>();
    protected List<DNaseCluster> dnaseClusters = new ArrayList<>();
    
    protected List<DNaseFootprint> dnaseFootprints = new ArrayList<>();
    protected List<FootprintCluster> footprintClusters = new ArrayList<>();
    
    protected List<MNasePeak> mnasePeaks = new ArrayList<>();
    
    protected List<DNaseCluster> atacClusters = new ArrayList<>();
    protected List<DNaseCluster> faireClusters = new ArrayList<>();
    
    
    public static enum Status { CURRENT, RETIRED };
    protected Status status = Status.CURRENT;
    protected List<HistoryEntry> history = new ArrayList<>();
    
    public String getStableId()
    {
        return PREFIX + getOrigin().getMetadata().tf.uniprotName + "." + id + ".v" + version;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    @Override
    public boolean hasSummit()
    {
        return true;
    }
    
    @Override
    public int getSummit()
    {
        return summit;
    }

    public void setSummit(int summit)
    {
        this.summit = summit;
    }

    public String getReliabilityLevel()
    {
        return reliabilityLevel;
    }

    public void setReliabilityLevel(String reliabilityLevel)
    {
        this.reliabilityLevel = reliabilityLevel;
    }

    public float getReliabilityScore()
    {
        return reliabilityScore;
    }

    public void setReliabilityScore(float reliabilityScore)
    {
        this.reliabilityScore = reliabilityScore;
    }
    
    public Status getStatus()
    {
        return status;
    }
    public void setStatus(Status status)
    {
        this.status = status;
    }

    public List<HistoryEntry> getHistory()
    {
        return history;
    }

    public List<ChIPSeqPeak> getChipSeqPeaks()
    {
        return chipSeqPeaks;
    }

    public List<ChIPexoPeak> getChipExoPeaks()
    {
        return chipExoPeaks;
    }
    
    public List<HistonesPeak> getHistonesPeaks()
    {
        return histonesPeaks;
    }
    
    public List<HistonesCluster> getHistonesClusters()
    {
        return histonesClusters;
    }

    public List<PWMMotif> getMotifs()
    {
        return motifs;
    }

    public List<DNasePeak> getDnasePeaks()
    {
        return dnasePeaks;
    }
    
    public List<DNaseCluster> getDnaseClusters()
    {
        return dnaseClusters;
    }

    public List<DNaseFootprint> getDnaseFootprints()
    {
        return dnaseFootprints;
    }
    
    public List<FootprintCluster> getFootprintClusters()
    {
        return footprintClusters;
    }
    
    
    
    public List<MNasePeak> getMnasePeaks()
    {
        return mnasePeaks;
    }

    public List<DNaseCluster> getAtacClusters()
    {
        return atacClusters;
    }

    public List<DNaseCluster> getFaireClusters()
    {
        return faireClusters;
    }

    @Override
    public MasterTrack getOrigin()
    {
        return (MasterTrack)super.getOrigin();
    }
    @Override
    public double getScore()
    {
        return reliabilityScore;
    }
    
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize()
                + 4 + 4 //version summit
                + 8 + 4 //reliability level and score
                + 8//chipSeqPeaks
                +8 //chipExoPeaks
                +8 //dnasePeaks
                +8 //dnaseClusters
                +8 //dnaseFootprints
                +8 //dnaseFootprintClusters
                +8 //histonesPeaks
                +8 //histonesCluster
                +8 //mnasePeaks
                +8 //atacClusters
                +8 //faireClusters
                +8 //motifs
                +8 //status
                +8 //history
                ;
    }
    
    @Override
    public long _childsSize()
    {
        return super._childsSize()
                + SizeOfUtils.sizeOfArrayList( (ArrayList<ChIPSeqPeak>)chipSeqPeaks )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<ChIPexoPeak>)chipExoPeaks )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<DNasePeak>)dnasePeaks )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<DNaseCluster>)dnaseClusters )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<DNaseFootprint>)dnaseFootprints )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<FootprintCluster>)footprintClusters )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<HistonesPeak>)histonesPeaks )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<HistonesCluster>)histonesClusters )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<MNasePeak>)mnasePeaks )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<DNaseCluster>)atacClusters )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<DNaseCluster>)faireClusters )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<PWMMotif>)motifs )
                + SizeOfUtils.sizeOfArrayList( (ArrayList<HistoryEntry>)history )
                ;
    }
    
    protected static final PropertyDescriptor RELIABILITY_SCORE_PD = StaticDescriptor.create("reliabilityScore", "Reliability score");
    protected static final PropertyDescriptor RELIABILITY_LEVEL_PD = StaticDescriptor.create("reliabilityLevel", "Reliability level");
    protected static final PropertyDescriptor STATUS_PD = StaticDescriptor.create("status", "Status");
    @Override
    public DynamicPropertySet getProperties()
    {
        DynamicPropertySet dps = super.getProperties();
        dps.add( new DynamicProperty( STATUS_PD, String.class, getStatus().toString() ) );
        dps.add( new DynamicProperty( RELIABILITY_LEVEL_PD, String.class, getReliabilityLevel() ) );
        dps.add( new DynamicProperty( RELIABILITY_SCORE_PD, Float.class, getReliabilityScore()) );
        return dps;
    }
}