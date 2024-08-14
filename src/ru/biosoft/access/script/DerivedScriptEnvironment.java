package ru.biosoft.access.script;

import java.awt.image.BufferedImage;

import ru.biosoft.access.ImageElement;
import ru.biosoft.table.TableDataCollection;

public class DerivedScriptEnvironment implements ScriptEnvironment
{
    private ScriptEnvironment parent;
    private boolean hasData = false;
    
    public DerivedScriptEnvironment(ScriptEnvironment parent)
    {
        this.parent = parent;
    }
    
    public boolean hasData()
    {
        return hasData;
    }
    
    @Override
    public void error(String msg)
    {
        hasData = true;
        parent.error( msg );
    }

    @Override
    public void warn(String msg)
    {
        hasData = true;
        parent.warn( msg );
    }

    @Override
    public void print(String msg)
    {
        hasData = true;
        parent.print( msg );
    }


    @Override
    public void info(String msg)
    {
        hasData = true;
        parent.info( msg );
    }

    @Override
    public void showTable(TableDataCollection dataCollection)
    {
        hasData = true;
        parent.showTable( dataCollection );
    }

    @Override
    public void showGraphics(BufferedImage image)
    {
        hasData = true;
        parent.showGraphics( image );
    }

    @Override
    public void showGraphics(ImageElement element)
    {
        hasData = true;
        parent.showGraphics( element );
    }

    @Override
    public void showHtml(String html)
    {
        hasData = true;
        parent.showHtml( html );
    }

    @Override
    public boolean isStopped()
    {
        return parent.isStopped();
    }

    @Override
    public String addImage(BufferedImage image)
    {
        return parent.addImage( image );
    }
    
    @Override
    public String tryShowException(Throwable exc)
    {
        return parent.tryShowException( exc );
    }
    
    @Override
    public String tryShowObject(Object object)
    {
        return parent.tryShowObject( object );
    }
}
