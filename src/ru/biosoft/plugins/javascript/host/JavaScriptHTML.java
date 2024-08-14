package ru.biosoft.plugins.javascript.host;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.plugins.javascript.Global;

public class JavaScriptHTML
{
    public HTMLTable createTable()
    {
        return new HTMLTable();
    }

    public void showImage(BufferedImage img)
    {
        ScriptEnvironment environment = Global.getEnvironment();
        if( environment != null )
            environment.showGraphics( img );
    }

    public void showHtml(String html)
    {
        ScriptEnvironment environment = Global.getEnvironment();
        if( environment != null )
            environment.showHtml( html );
    }
    
    public static class HTMLTable
    {
        private String html;
        private List<Object[]> rows = new ArrayList<>();
        private List<String> alignment = new ArrayList<>();
        
        private int border = 0;
        private String curAlignment = "center";
        
        public HTMLTable()
        {
        }
        
        public HTMLTable(int border)
        {
            this.border = border;
        }
        
        public void setRowAlignment(String alignemnt)
        {
            this.curAlignment = alignemnt;
        }
        
        public void addRow(Object ... objs)
        {
            rows.add( objs );
            alignment.add( curAlignment );
        }            
                
        public void clear()
        {
            html = null;
            rows.clear();
            alignment.clear();
        }

        public void out() throws Exception
        {
            if( html == null )
                html = generateHTML();

            ScriptEnvironment environment = Global.getEnvironment();
            if( environment != null )
                environment.showHtml( html );
        }

        public String generateHTML() throws Exception
        {
            ScriptEnvironment environment = Global.getEnvironment();
            if( environment != null )
            {                
                StringBuffer buffer = new StringBuffer();
                buffer.append( "<table border = "+border+">" );
                for( int i = 0; i < rows.size(); i++ )
                {
                    buffer.append( "<tr>" );
                    Object[] row = rows.get(i);
                    for( int j=0; j< row.length; j++ )
                    {                                   
                        Object obj = row[j];
                        String insert;
                        if( obj instanceof BufferedImage )
                            insert = "<img src='" +environment.addImage( (BufferedImage)obj)+ "'></td>";                       
                        else if (obj instanceof HTMLTable)
                            insert = ( (HTMLTable)obj ).generateHTML();
                        else
                            insert = obj.toString();                        
                        buffer.append( "<td style='text-align: "+alignment.get(i)+"'>" + insert + "</td>" );
                    }
                    buffer.append( "</tr>" );
                }
                buffer.append( "</table>" );
                return buffer.toString();
            }
            return "";
        }
    }
}
