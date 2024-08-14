package ru.biosoft.tasks.process;

public class MachineResources
{
    private final int cpu;
    private final long memory, disk;

    public MachineResources(int cpu, long memory, long disk)
    {
        this.cpu = cpu;
        this.memory = memory;
        this.disk = disk;
    }
    
    public MachineResources(MachineResources resources)
    {
        this.cpu = resources.getCpu();
        this.memory = resources.getMemory();
        this.disk = resources.getDisk();
    }

    /**
     * Number of CPU cores
     */
    public int getCpu()
    {
        return cpu;
    }

    /**
     *  RAM in bytes
     */
    public long getMemory()
    {
        return memory;
    }

    /**
     * Disk size in bytes 
     */
    public long getDisk()
    {
        return disk;
    }

    public boolean canProvide(MachineResources subResources)
    {
        return subResources.getCpu() <= getCpu() && subResources.getMemory() <= getMemory() && subResources.getDisk() <= getDisk();
    }

    public MachineResources add(MachineResources resources)
    {
        return new MachineResources(getCpu() + resources.getCpu(), getMemory() + resources.getMemory(), getDisk() + resources.getDisk());
    }

    public MachineResources subtract(MachineResources resources)
    {
        return new MachineResources(getCpu() - resources.getCpu(), getMemory() - resources.getMemory(), getDisk() - resources.getDisk());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + cpu;
        result = prime * result + (int) ( disk ^ ( disk >>> 32 ) );
        result = prime * result + (int) ( memory ^ ( memory >>> 32 ) );
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
        MachineResources other = (MachineResources)obj;
        if( cpu != other.cpu )
            return false;
        if( disk != other.disk )
            return false;
        if( memory != other.memory )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "MachineResources [cpu=" + cpu + ", memory=" + memory + ", disk=" + disk + "]";
    }
}
