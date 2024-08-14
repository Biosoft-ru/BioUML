package ru.biosoft.table.export;

import java.io.OutputStream;
import java.io.PrintWriter;

import ru.biosoft.access.core.DataElement;

public abstract class TableExportTransformer
{
    protected OutputStream os;
    protected PrintWriter pw;
    protected DataElement de;
    
    public void setOutputStream(OutputStream os)
    {
        this.os = os;
        this.pw = new PrintWriter(os);
    }
    
    public void setDataElement(DataElement de)
    {
        this.de = de;
    }
    
    public abstract void writeHeader(String documentTitle);
    
    public abstract void writeFooter();
    
    public abstract void writeColumnSectionStart();

    public abstract void writeColumnTitle(String title);
    
    public abstract void writeColumnTitleSeparator();
    
    public abstract void writeColumnSectionEnd();
    
    public abstract void writeData(Object data);
    
    public abstract void writeDataSeparator();
    
    public abstract void writeDataSectionStart();
    
    public abstract void writeLineSeparator();
}
