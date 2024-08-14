package ru.biosoft.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.beans.SimpleBeanInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;

import java.util.Arrays;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Preferences;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.entry.BundleEntry;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.util.entry.RegularFileEntry;

public class ApplicationUtils
{
    private static final String THREADS_NUMBER_PREFERENCE = "threadsNumber";
    private static final String MAX_SORTING_SIZE_PREFERENCE = "maxSortingSize";

    public static final int DEFAULT_MAX_SORTING_SIZE = 100000;

    static Logger log = Logger.getLogger( ApplicationUtils.class.getName() );

    static public ImageIcon getImageIcon(URL url)
    {
        if( url == null )
            return null;

        ImageIcon imageIcon = new ImageIcon(url);
        return imageIcon;
    }

    static Map<String, ImageIcon> imageMap = new ConcurrentHashMap<>();

    static public ImageIcon getImageIcon(String imagename)
    {
        ImageIcon imageIcon = imageMap.get(imagename);

        if( imageIcon != null )
            return imageIcon;

        int idx = imagename.indexOf(':');
        if( idx > 2 )
        {
            String pluginName = imagename.substring(0, idx);
            log.fine( "Loading image from plugin " + pluginName );
            String resource = imagename.substring(idx + 1);
            if(pluginName.equals("default"))
            {
                URL url = ApplicationUtils.class.getClassLoader().getResource(resource);
                if( url != null )
                {
                    imageIcon = getImageIcon(url);
                    imageMap.put(imagename, imageIcon);
                    return imageIcon;
                }
            }
            Bundle bundle = null;
            try
            {
                bundle = Platform.getBundle(pluginName);
            }
            catch( Throwable t )
            {
                log.log( Level.SEVERE, "can not load plugin", t );
            }
            if( bundle != null )
            {
                log.fine( "Loading image from bundle " + bundle );
                int idx2 = resource.indexOf("?");
                if( idx2 != -1 ) // Probably it's CustomImageLoader
                {
                    try
                    {
                        String className = resource.substring(0, idx2);
                        CustomImageLoader imageLoader = (CustomImageLoader)ClassLoading.loadClass(className).newInstance();
                        String path = resource.substring(idx2 + 1);
                        imageIcon = imageLoader.loadImage(pluginName + ":" + path);
                        if( imageIcon != null )
                            imageMap.put(imagename, imageIcon);
                        return imageIcon;
                    }
                    catch( Exception e )
                    {
                        log.log( Level.WARNING, "can not load image from resource: " + resource, e );
                    }
                }
                URL url = bundle.getResource(resource);
                if( url != null )
                {
                    imageIcon = getImageIcon(url);
                    imageMap.put(imagename, imageIcon);
                    return imageIcon;
                }
            }
        }

        URL url = ClassLoader.getSystemResource(imagename);

        if( url != null )
        {
            imageIcon = getImageIcon(url);
            imageMap.put(imagename, imageIcon);
            return imageIcon;
        }

        SimpleBeanInfo sbi = new SimpleBeanInfo();
        Image img = sbi.loadImage(imagename);

        if( img != null )
        {

            imageIcon = new ImageIcon(img);
            imageMap.put(imagename, imageIcon);
            return imageIcon;
        }
        
        //In some cases we try to get image by the path to file. Here we check if such file exists 
        //However not sure what should happened if such file does not exist  or is of wrong type
        //TODO: do something in that regard
        if( !new File( imagename ).exists() )
        {   
            log.log( Level.SEVERE, "Image file doesn't exists: " + imagename, new Exception() );
        }   
        imageIcon = new ImageIcon( imagename );
        imageMap.put(imagename, imageIcon);
        return imageIcon;
    }

    static public ImageIcon getImageIcon(String basePath, String name)
    {
        return name.indexOf(':') != -1 ? getImageIcon(name) : getImageIcon(basePath + File.separator + name);
    }

    static public void moveToCenter(Component f)
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        f.setLocation(screenSize.width / 2 - f.getSize().width / 2, screenSize.height / 2 - f.getSize().height / 2);
    }

    public static String getFileNameWithoutExtension(String fileName)
    {
        int whereDot = fileName.lastIndexOf('.');
        if( 0 < whereDot && whereDot <= fileName.length() - 2 )
        {
            return fileName.substring(0, whereDot);
        }
        return fileName;
    }

    public static URL getResourceURL(String pluginName, String fileName)
    {
        try
        {
            Bundle plugin = Platform.getBundle(pluginName);
            if(plugin == null)
            {
                return new File("../plugconfig/"+pluginName, fileName).toURI().toURL();
            }
            return new URL(plugin.getEntry( "/" ), fileName);
        }
        catch( MalformedURLException e )
        {
            throw new InternalException( e );
        }
    }

    @Deprecated
    public static File getPluginPath(String pluginName)
    {
        Bundle plugin = Platform.getBundle(pluginName);
        if(plugin == null)
        {
            return new File("../plugins/"+pluginName+"_"+"0.9.8");
        }
        String bundle = plugin.getLocation();
        String path = bundle.substring(bundle.indexOf("@") + 1, bundle.lastIndexOf("/")).replace('/', File.separatorChar);
        String home = System.getProperty("biouml.server.path");
        if( home == null )
            home = System.getProperty("user.dir");
        return new File(home, path);
    }

    /**
     * Resolves path to the plugin file resource
     * @param pluginPath path like "ru.biosoft.access:resource"
     * @return File object pointing to the resource
     */
    public static PluginEntry resolvePluginPath(String pluginPath)
    {
        return resolvePluginPath( pluginPath, "" );
    }

    public static PluginEntry resolvePluginPath(String pluginPath, String parentPath)
    {
        if(pluginPath == null)
            return null;
        int colonPos = pluginPath.indexOf( ':' );
        if(colonPos < 3)
        {
            File f = parentPath.isEmpty() ? new File( pluginPath ) : new File( parentPath, pluginPath );
            return new RegularFileEntry( f );
        }
        String pluginName = pluginPath.substring( 0, colonPos );
        String path = pluginPath.substring( colonPos+1 );
        Bundle plugin = Platform.getBundle(pluginName);
        if(plugin == null)
        {
            if(new File("../plugconfig").exists())
                return new RegularFileEntry(new File(new File("../plugconfig", pluginName), path));
            else
                return new RegularFileEntry( new File( pluginPath ) );
        }
        path = "/" + path;
        if( plugin.getEntryPaths( path ) != null )
        {
            path += "/";
        }
        return new BundleEntry( plugin, path );
    }

    public static int getMaxSortingSize()
    {
        int maxSortingSize = DEFAULT_MAX_SORTING_SIZE;
        try
        {
            Preferences preferences = Application.getPreferences();
            DynamicProperty property = preferences.getProperty(MAX_SORTING_SIZE_PREFERENCE);
            if( property == null || property.getValue().equals(0) )
            {
                property = new DynamicProperty(MAX_SORTING_SIZE_PREFERENCE, "Max sorting size",
                        "Maximum size of the table which supports sorting", Integer.class, DEFAULT_MAX_SORTING_SIZE);
                preferences.add(property);
            }
            maxSortingSize = (Integer)property.getValue();
        }
        catch( Exception e )
        {
        }
        return maxSortingSize;
    }

    public static int getPreferredThreadsNumber()
    {
        int nThreads = 0;

        Preferences preferences = Application.getPreferences();
        if( preferences != null )
        {
            DynamicProperty property = preferences.getProperty(THREADS_NUMBER_PREFERENCE);
            if( property == null )
            {
                property = new DynamicProperty(THREADS_NUMBER_PREFERENCE, "Preferred number of threads",
                        "Set 0 for number of processors available", Integer.class, 0);
                preferences.add(property);
            }

            try
            {
                nThreads = (Integer)property.getValue();
            }
            catch( Exception e )
            {
                property.setValue(0);
            }
        }

        if( nThreads <= 0 )
        {
            nThreads = Runtime.getRuntime().availableProcessors();
        }
        return nThreads;
    }

    public static String detectEncoding(File file) throws Exception
    {
        String[] Styles={"Cp1251", "UTF-8", "UTF-16", "US-ASCII", "ISO-8859-1"};
        for (String style: Styles)
        {
            String line="";
            int ind=0;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader( new InputStreamReader( new FileInputStream( file ), style ) ))
            {
                while( ( line = br.readLine() ) != null && ( ind < 20 ) )
                {
                    sb.append( line );
                    ind++;
                }
            }
            String text = sb.toString();
            int lowCode=2, upCode=150, sum=0;
            long num=Math.round(0.9*text.length());
            for (int i=0;i<text.length();i++)
            {
                if (text.charAt(i)==0) break;
                if ((text.charAt(i)<upCode)&&(text.charAt(i)>lowCode))
                {
                    sum++;
                }
            }
            if (sum>num)
                return style;
        }
        throw new RuntimeException("The code style of text from file can not be recognized");
    }

    public static String trimStackAsString( Throwable exc, int nLines )
    {
        StringBuilder sb = new StringBuilder();

        List<StackTraceElement> stackList = Arrays.asList( exc.getStackTrace() );
        if( stackList.size() > nLines )
        {
            stackList = stackList.subList( 0, nLines );
        }

        for( StackTraceElement stackEl : stackList )
        {
            sb.append( "     at " ).append( stackEl.toString() ).append( "\n" );
        }
        return sb.toString();
    }

}
