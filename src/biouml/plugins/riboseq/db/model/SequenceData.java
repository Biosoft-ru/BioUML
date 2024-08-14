package biouml.plugins.riboseq.db.model;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;


public class SequenceData
{
    public enum Format {FASTA,FASTQ,SRA,ELAND};
    
    private int id;
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }

    private Format format = Format.SRA;
    @PropertyName("Format")
    @PropertyDescription("File format")
    public Format getFormat()
    {
        return format;
    }
    public void setFormat(Format format)
    {
        this.format = format;
    }
    
    private String url = "";
    @PropertyName("URL")
    @PropertyDescription("File URL")
    public String getUrl()
    {
        return url;
    }
    public void setUrl(String url)
    {
        this.url = url;
    }
    
    @Override
    public String toString()
    {
        return url;
    }
}
