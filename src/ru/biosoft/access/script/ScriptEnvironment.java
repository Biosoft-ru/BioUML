package ru.biosoft.access.script;

import java.awt.image.BufferedImage;

import ru.biosoft.access.ImageElement;
import ru.biosoft.table.TableDataCollection;

/**
 * Basic interface for JavaScript environment
 */
public interface ScriptEnvironment
{
    // messages output
    public void error(String msg);
    public void warn(String msg);
    public void print(String msg);
    public void info( String msg );

    // to form instructions for results output
    public void showTable(TableDataCollection dataCollection);
    public void showGraphics(BufferedImage image);
    public void showGraphics(ImageElement element);
    public void showHtml(String html);

    public default String tryShowObject( Object object )
    {
        return null; 
    }  

    public default String tryShowException( Throwable exc )
    {
        return null; 
    }  
        
    /**
     * Tells environment that this image will be displayed in html and so the environment should made preparations.
     * @return correct path which should be used in html so that this environment will understand it.     
     */
    public String addImage(BufferedImage image);
    
    public boolean isStopped();
}
