package biouml.plugins.gtrd.master.sites;

import biouml.plugins.gtrd.master.utils.SizeOf;

public class HistoryEntry implements SizeOf
{
    private int version;
    private int from, to;
    private int release;//when it last appears (version of master track)
    
    public int getVersion()
    {
        return version;
    }
    public void setVersion(int version)
    {
        this.version = version;
    }
    public int getFrom()
    {
        return from;
    }
    public void setFrom(int from)
    {
        this.from = from;
    }
    public int getTo()
    {
        return to;
    }
    public void setTo(int to)
    {
        this.to = to;
    }
    public int getRelease()
    {
        return release;
    }
    public void setRelease(int release)
    {
        this.release = release;
    }
    
    @Override
    public long _fieldsSize()
    {
        return 4//version
                +4+4//from,to
                +4//release
                ;
    }
}
