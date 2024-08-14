package biouml.plugins.gtrd.master.sites.histones;

import biouml.plugins.gtrd.CellLine;
import biouml.plugins.gtrd.master.sites.GenomeLocation;

public abstract class HistonesCluster extends GenomeLocation
{
    private CellLine cell;
    private String target;

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

    public String getTarget()
    {
        return target;
    }
    public void setTarget(String target)
    {
        this.target = target;
    }

    public abstract String getPeakCaller();

    @Override
    public String getStableId()
    {
        //hc.H3K27me.CELL001234.macs2.3343
        return String.format( "hc.%s.CELL%06d.%s.%d", target, Integer.parseInt( cell.getName() ), getPeakCaller(), id );
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
        HistonesCluster other = (HistonesCluster)obj;
        if( id != other.getId() )
            return false;
        if(!cell.getName().equals( other.getCell().getName() ))
            return false;
        if(!target.equals( other.target ))
            return false;
        return true;
    }
    
    @Override
    public long _fieldsSize()
    {
        return super._fieldsSize() + 8 + 4;
    }
}
