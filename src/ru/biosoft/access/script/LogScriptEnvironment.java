package ru.biosoft.access.script;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import ru.biosoft.access.ImageElement;
import ru.biosoft.access.log.BiosoftLogger;
import ru.biosoft.access.log.JULLoggerAdapter;
import ru.biosoft.table.TableDataCollection;

/**
 * {@link ScriptEnvironment} implementation for {@link Logger} or {@link BiosoftLogger} output
 */
public class LogScriptEnvironment implements ScriptEnvironment
{
    private final BiosoftLogger log;
    private final List<String> errorList = new ArrayList<>();
    private boolean wasError = false;
    private boolean exceptionOnError = false;
    private boolean reportErrorsAsWarnings = false;

    public LogScriptEnvironment(BiosoftLogger log, boolean exceptionOnError)
    {
        this.exceptionOnError = exceptionOnError;
        this.log = log;
    }

    public LogScriptEnvironment(BiosoftLogger log)
    {
        this.log = log;
    }

    public LogScriptEnvironment(Logger log, boolean exceptionOnError)
    {
        this( new JULLoggerAdapter( log ), exceptionOnError );
    }

    public LogScriptEnvironment(Logger log)
    {
        this( new JULLoggerAdapter( log ) );
    }

    public void reportErrorsAsWarnings(boolean enabled)
    {
        this.reportErrorsAsWarnings = enabled;
    }

    @Override
    public void error(String msg)
    {
        wasError = true;
        if(exceptionOnError)
            throw new RuntimeException(msg);
        if(reportErrorsAsWarnings)
            log.warn( msg );
        else
        {
            errorList.add( msg );
            log.error( msg );
        }
    }

    @Override
    public void warn(String msg)
    {
        log.warn( msg );
    }

    @Override
    public void print(String msg)
    {
        log.info(msg);
    }

    @Override
    public void info(String msg)
    {
        print( msg );
    }

    public boolean isFailed()
    {
        return wasError;
    }

    private final List<BufferedImage> images = new ArrayList<>();
    @Override
    public void showGraphics(BufferedImage image)
    {
        images.add(image);
    }

    @Override
    public void showHtml(String html) {}
    @Override
    public void showTable(TableDataCollection dataCollection) {}

    @Override
    public void showGraphics(ImageElement element)
    {
        showGraphics(element.getImage(null));
    }

    public Collection<BufferedImage> getImages()
    {
        return Collections.unmodifiableCollection(images);
    }

    @Override
    public boolean isStopped()
    {
        return false;
    }

    public List<String> getErrorList()
    {
        return errorList;
    }

    @Override
    public String addImage(BufferedImage image)
    {
        return null;
    }
}
