package ru.biosoft.table.export;

import ru.biosoft.templates.TemplateRegistry;


public class HTMLExportTransformer extends TableExportTransformer
{
    public static final String CSS =
        "body {font-family: tahoma, helvetica, arial, sans-serif; font-size: 10pt;}\n"+
        "table {font-size: 10pt; border-collapse: true; border-width: 2px 0px 1px 0px; border-color: #404040; border-style: solid;}\n"+
        "tr.oddRow {background: #DDDDDD;}\n"+
        "th {padding: 2pt 4pt; border-bottom: 1px solid #404040;}\n"+
        "td {padding: 2pt 4pt; border-bottom: 1px solid #C0C0C0;}\n";
    private int rowNumber = 0;
    
    private String decorateString(String str)
    {
        // string processing is switched off until better times come
        return str.equals("")?"&nbsp;":str;
/*        if(str.length()>60)
            return "<span title=\""
                    + str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;") + "\">"
                    + str.substring(0, 50).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "...</span>";
        else
            return str.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");*/
    }

    @Override
    public void writeHeader(String documentTitle)
    {
        pw.print("<html>\n<head>\n<title>"+documentTitle+"</title>\n<style>\n"+CSS+"</style>\n</head>\n<body>\n");
        pw.print(TemplateRegistry.mergeTemplate(de, "Default").toString().replaceAll("href=\"de:([^\"]+)\"", ""));
        pw.print("<table cellpadding=\"0\" cellspacing=\"0\">\n");
        rowNumber = 0;
    }

    @Override
    public void writeColumnSectionStart()
    {
        pw.print("<thead>\n<tr>\n");
    }

    @Override
    public void writeColumnTitle(String title)
    {
        pw.print("\t<th>"+decorateString(title)+"</th>\n");
    }

    @Override
    public void writeColumnTitleSeparator()
    {
    }

    @Override
    public void writeColumnSectionEnd()
    {
        pw.print("</tr>\n</thead>\n");
    }

    @Override
    public void writeData(Object data)
    {
        pw.print("<td>"+decorateString(data.toString())+"</td>");
    }

    @Override
    public void writeDataSectionStart()
    {
        pw.print("<tbody>\n");
        writeRowStart();
    }

    private void writeRowStart()
    {
        pw.print("<tr class=\""+((rowNumber++)%2==0?"evenRow":"oddRow")+"\">\n\t");
    }

    @Override
    public void writeDataSeparator()
    {
    }

    @Override
    public void writeFooter()
    {
        pw.print("</tr>\n</tbody>\n</table>\n</body>\n</html>\n");
        pw.flush();
    }

    @Override
    public void writeLineSeparator()
    {
        pw.print("\n</tr>\n");
        writeRowStart();
    }
}
