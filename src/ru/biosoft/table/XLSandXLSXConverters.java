package ru.biosoft.table;

public interface XLSandXLSXConverters
{
    
    public String[] getSheetNames();
    public String getSheetData(int numberOfSheet);
    public void process() throws Exception;
}
