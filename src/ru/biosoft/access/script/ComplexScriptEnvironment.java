package ru.biosoft.access.script;

import java.awt.image.BufferedImage;

import ru.biosoft.access.ImageElement;
import ru.biosoft.table.TableDataCollection;

public class ComplexScriptEnvironment implements ScriptEnvironment
{
    final ScriptEnvironment[] envs;

    public ComplexScriptEnvironment(ScriptEnvironment ... envs)
    {
        this.envs = envs;
    }

    @Override
    public void error(String msg)
    {
        for( ScriptEnvironment env : envs )
        {
            env.error( msg );
        }
    }

    @Override
    public void warn(String msg)
    {
        for( ScriptEnvironment env : envs )
        {
            env.warn( msg );
        }
    }

    @Override
    public void print(String msg)
    {
        for( ScriptEnvironment env : envs )
        {
            env.print( msg );
        }
    }

    @Override
    public void info(String msg)
    {
        for( ScriptEnvironment env : envs )
        {
            env.info( msg );
        }
    }

    @Override
    public void showTable(TableDataCollection dataCollection)
    {
        for( ScriptEnvironment env : envs )
        {
            env.showTable( dataCollection );
        }
    }

    @Override
    public void showGraphics(BufferedImage image)
    {
        for( ScriptEnvironment env : envs )
        {
            env.showGraphics( image );
        }
    }

    @Override
    public void showGraphics(ImageElement element)
    {
        for( ScriptEnvironment env : envs )
        {
            env.showGraphics( element );
        }
    }

    @Override
    public void showHtml(String html)
    {
        for( ScriptEnvironment env : envs )
        {
            env.showHtml( html );
        }
    }

    @Override
    public boolean isStopped()
    {
        for( ScriptEnvironment env : envs )
        {
            if( env.isStopped() )
                return true;
        }
        return false;
    }

    @Override
    public String addImage(BufferedImage image)
    {
        String result = null;
        for( ScriptEnvironment env : envs )
        {
            String envResult = env.addImage( image );
                if (envResult != null)
                    result = envResult;
        }
        return result;
    }
}
