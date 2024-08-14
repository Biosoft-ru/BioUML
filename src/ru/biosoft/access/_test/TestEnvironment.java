package ru.biosoft.access._test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.ImageElement;
import ru.biosoft.access.script.ScriptEnvironment;
import ru.biosoft.table.TableDataCollection;

public class TestEnvironment implements ScriptEnvironment
{
    public final List<String> help;
    public final List<String> error;
    public final List<String> warn;
    public final List<String> print;
    public final List<BufferedImage> images;
    public final List<TableDataCollection> tables;
    public final List<ImageElement> imageElements;

    public TestEnvironment()
    {
        this.help = new ArrayList<>();
        this.error = new ArrayList<>();
        this.warn = new ArrayList<>();
        this.print = new ArrayList<>();
        this.images = new ArrayList<>();
        this.imageElements = new ArrayList<>();
        this.tables = new ArrayList<>();
    }

    @Override
    public void error(String msg)
    {
        this.error.add(msg);
    }

    @Override
    public void print(String msg)
    {
        this.print.add(msg);
    }

    @Override
    public void showGraphics(BufferedImage image)
    {
        this.images.add(image);
    }

    @Override
    public void showGraphics(ImageElement element) 
    {
        this.imageElements.add(element);
    }
    
    @Override
    public void showHtml(String help)
    {
        this.help.add(help);
    }

    @Override
    public void showTable(TableDataCollection dataCollection)
    {
        this.tables.add(dataCollection);
    }

    @Override
    public void warn(String msg)
    {
        this.warn.add(msg);
    }

    @Override
    public void info(String msg)
    {
        print( msg );
    }

    @Override
    public boolean isStopped()
    {
        return false;
    }

    @Override
    public String addImage(BufferedImage image)
    {
        return null; //TODO: implement
    }
}