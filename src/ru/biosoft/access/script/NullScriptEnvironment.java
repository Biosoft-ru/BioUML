package ru.biosoft.access.script;

import java.awt.image.BufferedImage;

import ru.biosoft.access.ImageElement;
import ru.biosoft.table.TableDataCollection;

/**
 * This environment does nothing
 * @author lan
 */
public class NullScriptEnvironment implements ScriptEnvironment
{
    @Override
    public void error(String msg)
    {
    }

    @Override
    public void warn(String msg)
    {
    }

    @Override
    public void print(String msg)
    {
    }

    @Override
    public void info(String msg)
    {
    }

    @Override
    public void showTable(TableDataCollection dataCollection)
    {
    }

    @Override
    public void showGraphics(BufferedImage image)
    {
    }

    @Override
    public void showGraphics(ImageElement element)
    {
    }

    @Override
    public void showHtml(String html)
    {
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
