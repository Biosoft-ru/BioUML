package ru.biosoft.table.export;

import java.util.regex.Pattern;

public class CSVExportTransformer extends TableExportTransformer
{
    public static final String DEFAULT_SEPARATOR = ",";
    private String separator = DEFAULT_SEPARATOR;
    
    private String decorateString(String str)
    {
        if(Pattern.matches(".*[\\\"\\,\\n].*", str))
        {
            str = str.replaceAll("\\\"", "\"\"");
            str = "\""+str+"\"";
        }
        return str;
    }

    @Override
    public void writeColumnSectionStart()
    {
    }

    @Override
    public void writeColumnTitle(String title)
    {
        pw.print(decorateString(title));
    }

    @Override
    public void writeColumnTitleSeparator()
    {
        pw.print(separator);
    }

    @Override
    public void writeColumnSectionEnd()
    {
        pw.print("\n");
    }

    @Override
    public void writeData(Object data)
    {
        pw.print(decorateString(data.toString()));
    }

    @Override
    public void writeDataSectionStart()
    {
    }

    @Override
    public void writeDataSeparator()
    {
        pw.print(separator);
    }

    @Override
    public void writeFooter()
    {
        pw.flush();
    }

    @Override
    public void writeHeader(String documentTitle)
    {
    }

    @Override
    public void writeLineSeparator()
    {
        pw.print("\n");
    }
}
