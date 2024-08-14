package ru.biosoft.math.view;

import java.net.URL;
import java.net.URLConnection;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * @author lan
 *
 */
public class Handler extends AbstractURLStreamHandlerService
{
    @Override
    public URLConnection openConnection(URL u)
    {
        return new FormulaConnection(u);
    }
}
