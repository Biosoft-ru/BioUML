package ru.biosoft.plugins.javascript;

import java.awt.image.BufferedImage;
import java.io.PrintStream;

import one.util.streamex.StreamEx;

import ru.biosoft.access.ImageElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.HtmlUtil;

/**
 * JSEnvironment implementation for console version
 */
public class JScriptShellEnvironment implements ScriptEnvironment
{
    private PrintStream out;

    /**
     * @param out
     */
    public JScriptShellEnvironment(PrintStream out)
    {
        this.out = out;
    }
    
    protected void write(String str)
    {
        out.print(str);
    }

    protected void println(String message)
    {
        out.println(message);
    }

    @Override
    public void error(String msg)
    {
        println(msg);
    }

    @Override
    public void print(String msg)
    {
        println(msg);
    }

    @Override
    public void info(String msg)
    {
        print( msg );
    }

    @Override
    public void showGraphics(BufferedImage image)
    {
        println("WARN: Graphics are not supported in console version");
    }

    @Override
    public void showGraphics(ImageElement element) 
    {
        showGraphics(element.getImage(null));
    }
    
    @Override
    public void showHtml(String html)
    {
        println(HtmlUtil.convertToText(html));
    }

    @Override
    public void showTable(TableDataCollection dataCollection)
    {
        write( "\t" );
        write( dataCollection.columns().map( TableColumn::getName ).joining( "\t" ) );
        write( "\n" );

        for( RowDataElement id : dataCollection )
        {
            write( StreamEx.of( id.getValues() ).prepend( id ).joining( "\t" ) );
            write( "\n" );
        }
    }

    @Override
    public void warn(String msg)
    {
        println(msg);
    }

    @Override
    public boolean isStopped()
    {
        return false;
    }

    @Override
    public String addImage(BufferedImage image)
    {
        return null;
    }
}
