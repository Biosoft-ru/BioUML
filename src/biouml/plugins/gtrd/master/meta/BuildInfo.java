package biouml.plugins.gtrd.master.meta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//Information about datasets/files used to build master track
//TODO: store version of clusters
public class BuildInfo
{
    //PEAKSXXXXXX identifiers used to build MasterTrack, grouped by peak caller
    public Map<String, Set<String>> chipSeqPeaksByPeakCaller = new HashMap<>();
    public Map<String, Set<String>> chipExoPeaksByPeakCaller = new HashMap<>();
    public Map<String, Set<String>> dnasePeaksByPeakCaller = new HashMap<>();
    public Map<String, Set<String>> dnaseClustersByPeakCaller = new HashMap<>();
    public Map<String, Set<String>> footprints = new HashMap<>();
    public Map<String, Set<String>> footprintClusters = new HashMap<>();
    public Map<String, Set<String>> atacClusters = new HashMap<>();
    public Map<String, Set<String>> faireClusters = new HashMap<>();
    public Map<String, Set<String>> mnasePeaks = new HashMap<>();
    public Map<String, Set<String>> histonePeaks = new HashMap<>();
    public Set<HistoneClustersInfo> histoneClusters = new HashSet<>();
    public Set<String> motifs = new HashSet<>();
    
    public static class HistoneClustersInfo
    {
        public String modification;
        public String cellId;
        public String peakCaller;
        public int version;
        
        public HistoneClustersInfo()
        {
            
        }
        
        public HistoneClustersInfo(String modification, String cellId, String peakCaller, int version)
        {
            this.modification = modification;
            this.cellId = cellId;
            this.peakCaller = peakCaller;
            this.version = version;
        }
        
        public String getFileName()
        {
            //H4K20me1_ChIP-seq_HM_from_cell_id_887_MACS2.bb
            return modification+"_ChIP-seq_HM_from_cell_id_" +cellId+ "_" + peakCaller.toUpperCase() + ".v" + version + ".bb";
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( cellId == null ) ? 0 : cellId.hashCode() );
            result = prime * result + ( ( modification == null ) ? 0 : modification.hashCode() );
            result = prime * result + ( ( peakCaller == null ) ? 0 : peakCaller.hashCode() );
            result = prime * result + version;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( obj == null )
                return false;
            if( getClass() != obj.getClass() )
                return false;
            HistoneClustersInfo other = (HistoneClustersInfo)obj;
            if( cellId == null )
            {
                if( other.cellId != null )
                    return false;
            }
            else if( !cellId.equals( other.cellId ) )
                return false;
            if( modification == null )
            {
                if( other.modification != null )
                    return false;
            }
            else if( !modification.equals( other.modification ) )
                return false;
            if( peakCaller == null )
            {
                if( other.peakCaller != null )
                    return false;
            }
            else if( !peakCaller.equals( other.peakCaller ) )
                return false;
            if( version != other.version )
                return false;
            return true;
        }
    }
    
    public BuildInfo() {}
    public BuildInfo(BuildInfo m)
    {
        copy(m.chipSeqPeaksByPeakCaller, this.chipSeqPeaksByPeakCaller);
        copy(m.chipExoPeaksByPeakCaller, this.chipExoPeaksByPeakCaller);
        copy(m.dnasePeaksByPeakCaller, this.dnasePeaksByPeakCaller);
        copy(m.dnaseClustersByPeakCaller, this.dnaseClustersByPeakCaller);
        copy(m.footprints, this.footprints);
        copy(m.footprintClusters, this.footprintClusters);
        copy(m.atacClusters, this.atacClusters);
        copy(m.faireClusters, this.faireClusters);
        copy(m.mnasePeaks, this.mnasePeaks);
        copy(m.histonePeaks, this.histonePeaks);
        
        this.histoneClusters.addAll( m.histoneClusters );
        this.motifs.addAll(m.motifs);
    }
    
    public void addChipSeqPeaks(String peakCaller, String peakId)
    {
        add(this.chipSeqPeaksByPeakCaller, peakCaller, peakId);
    }
    
    public void addChipExoPeaks(String peakCaller, String peakId)
    {
        add(this.chipExoPeaksByPeakCaller, peakCaller, peakId);
    }
    
    public void addDNasePeaks(String peakCaller, String peakRep)
    {
        add(this.dnasePeaksByPeakCaller, peakCaller, peakRep);
    }
    
    public void addDNaseClusters(String peakCaller, String cellId)
    {
        add(this.dnaseClustersByPeakCaller, peakCaller, cellId);
    }
    
    public void addFootprints(String peakCaller, String peakId)
    {
        add(this.footprints, peakCaller, peakId);
    }
    
    public void addFootprintClusters(String peakCaller, String cellId)
    {
        add(this.footprintClusters, peakCaller, cellId);
    }
    
    public void addAtacClusters(String peakCaller, String cellId)
    {
        add(this.atacClusters, peakCaller, cellId);
    }
    
    public void addFaireClusters(String peakCaller, String cellId)
    {
        add(this.faireClusters, peakCaller, cellId);
    }
    
    public void addMNasePeaks(String peakCaller, String peakId)
    {
        add(this.mnasePeaks, peakCaller, peakId);
    }
    
    public void addHistonePeaks(String peakCaller, String peakId)
    {
        add(this.histonePeaks, peakCaller, peakId);
    }
    
    public void addHistoneClusters(String peakCaller, String modification, String cellId, int version)
    {
        this.histoneClusters.add( new HistoneClustersInfo( modification, cellId, peakCaller, version ) );
    }
    
    private static void add(Map<String, Set<String>> dataset, String type, String id)
    {
        dataset.computeIfAbsent( type, k->new HashSet<>()).add( id );
    }
    
    
    private static void copy(Map<String, Set<String>> src, Map<String, Set<String>> dst)
    {
        for(Map.Entry<String, Set<String>> srcEntry : src.entrySet())
        {
            Set<String> dstSet = dst.computeIfAbsent( srcEntry.getKey(), k->new HashSet<>() );
            dstSet.addAll( srcEntry.getValue() );
        }
    }
    
    public void clear() {
        chipSeqPeaksByPeakCaller.clear();
        clearAnnotation();
    }
    
    public void clearAnnotation()
    {
        chipExoPeaksByPeakCaller.clear();
        dnasePeaksByPeakCaller.clear();
        dnaseClustersByPeakCaller.clear();
        footprints.clear();
        footprintClusters.clear();
        atacClusters.clear();
        faireClusters.clear();
        mnasePeaks.clear();
        histonePeaks.clear();
        histoneClusters.clear();
        motifs.clear();
    }
    
}
