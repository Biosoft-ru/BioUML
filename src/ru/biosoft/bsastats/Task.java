package ru.biosoft.bsastats;

public class Task
{
    private byte[] sequence;
    private byte[] quality;
    private Object data;

    public Task(byte[] sequence, byte[] quality, Object data)
    {
        super();
        this.sequence = sequence;
        this.quality = quality;
        this.data = data;
    }

    public byte[] getSequence()
    {
        return sequence;
    }

    public byte[] getQuality()
    {
        return quality;
    }

    public Object getData()
    {
        return data;
    }
}