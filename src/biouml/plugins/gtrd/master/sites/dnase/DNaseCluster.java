package biouml.plugins.gtrd.master.sites.dnase;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.GenomeLocation;

public abstract class DNaseCluster extends GenomeLocation
{
    public enum Design {
        DNASE_SEQ("DNase-seq", "dc"),
        ATAC_SEQ("ATAC-seq", "ac"),
        FAIRE_SEQ("FAIRE-seq", "fc");
        
        public final String label;
        public final String idPrefix;
        private Design(String label, String idPrefix)
        {
            this.label = label;
            this.idPrefix = idPrefix;
        }
        
        public static Design getByIdPrefix(String idPrefix)
        {
            switch(idPrefix)
            {
                case "dc": return DNASE_SEQ;
                case "ac": return ATAC_SEQ;
                case "fc": return FAIRE_SEQ;
                default:
                    throw new IllegalArgumentException("Unknown id prefix: " + idPrefix);
            }
        }
        
        public static Design getByLabel(String label)
        {
            switch(label)
            {
                case "DNase-seq": return DNASE_SEQ;
                case "ATAC-seq": return ATAC_SEQ;
                case "FAIRE-seq": return FAIRE_SEQ;
                default:
                    throw new IllegalArgumentException("Unknown label: " + label);
            }            
        }
        
        @Override
        public String toString()
        {
            return label;
        }
    }
    
    protected Design design;

    protected CellLine cell;
    
    private int peakCount;
    
    public int getPeakCount()
    {
        return peakCount;
    }
    public void setPeakCount(int peakCount)
    {
        this.peakCount = peakCount;
    }

    public CellLine getCell()
    {
        return cell;
    }
    public void setCell(CellLine cell)
    {
        this.cell = cell;
    }
    
    public abstract String getPeakCaller();
    
    public Design getDesign()
    {
        return design;
    }
    public void setDesign(Design design)
    {
        this.design = design;
    }
    
    
    @Override
    public String getStableId()
    {
        //dc.CELL001234.macs2.3343
        return String.format( design.idPrefix + ".CELL%06d.%s.%d", Integer.parseInt( cell.getName() ), getPeakCaller(), id );
    }
    
    @Override
    public double getScore()
    {
        return peakCount;
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
        DNaseCluster other = (DNaseCluster)obj;
        if( id != other.getId() )
            return false;
        if(!cell.getName().equals( other.getCell().getName() ))
            return false;
        if(!design.equals( other.design ))
            return false;
        return true;
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() 
                + 8 //design
                + 8 //cell
                + 4 //peak count
                ;
    }
}
