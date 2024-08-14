package ru.biosoft.templates;

import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import java.util.logging.Logger;

import ru.biosoft.access.HtmlDescribedElement;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil;

import com.developmentontheedge.application.ApplicationUtils;

public class TemplateUtils
{
    private static Logger log = Logger.getLogger(TemplateUtils.class.getName());

    public static final int TOOLBAR_BUTTON_SIZE = 20;



    protected static void configureButton(AbstractButton button, String text)
    {
        button.setAlignmentY(0.5f);

        Dimension btnSize = new Dimension(TOOLBAR_BUTTON_SIZE, TOOLBAR_BUTTON_SIZE);
        button.setSize(btnSize);
        button.setPreferredSize(btnSize);
        button.setMinimumSize(btnSize);
        button.setMaximumSize(btnSize);

        if( button.getIcon() != null )
            button.setText(null);
        else
            button.setText(text);
    }

    public static void generateBrowserView(URL url)
    {
        try
        {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if( desktop.isSupported(java.awt.Desktop.Action.BROWSE) )
            {
                desktop.browse(url.toURI());
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not generate browser view.");
        }
    }

    public static void generateBrowserView(Object model, String templateName)
    {
        StringBuffer result = TemplateRegistry.mergeTemplate(model, templateName);
        try
        {
            File subDirectory = TempFiles.dir("template");
            File htmlFile = new File(subDirectory, templateName + ".html");
            String html = result.toString().replaceAll("href=\"de:([^\"]+)\"", "");
            if( !html.startsWith( "<html>" ) )
                html = "<pre>" + html;
            html = html.replaceAll( "<math", "<math displaystyle=\"true\"" );
            Pattern pattern = Pattern.compile("<img([^>]*) src=\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(html);
            URL base = model instanceof HtmlDescribedElement ? ( (HtmlDescribedElement)model ).getBase() : null;
            int num = 0;
            int start = 0;
            while(matcher.find(start))
            {
                String src = matcher.group(2);
                URL url = src.contains("://")?new URL(src):new URL(base, src);
                URLConnection connection = url.openConnection();
                String type = TextUtil.split( connection.getContentType(), '/' )[1];
                InputStream inputStream = connection.getInputStream();
                String fileName = String.format("image%05d.%s", ++num, type);
                File newFile = new File(subDirectory, fileName);
                ApplicationUtils.copyStream(new FileOutputStream(newFile), inputStream);
                html = html.substring(0, matcher.start())+"<img"+matcher.group(1)+" src=\""+fileName+"\""+html.substring(matcher.end());
                start = matcher.end();
                matcher = pattern.matcher(html);
            }
            ApplicationUtils.writeString(htmlFile, html.replaceFirst("<html>", "<html>" + getStyle())); 

            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if( desktop.isSupported(java.awt.Desktop.Action.BROWSE) )
            {
                desktop.browse(htmlFile.toURI());
            }
        }
        catch( IOException ex )
        {
            log.log(Level.SEVERE, "Can not open template in web browser", ex);
        }
    }

    protected static File generateTmpFile(File dir, String suffix)
    {
        String name = String.valueOf(System.currentTimeMillis());
        String result = name + suffix;
        File file = new File(dir.getAbsolutePath() + File.separator + result);
        int i = 1;
        while( file.exists() )
        {
            i++;
            result = name + i + suffix;
            file = new File(dir.getAbsolutePath() + File.separator + result);
        }
        return file;
    }

    public static void applyStyle(HTMLDocument document)
    {
        applyStyle(document, "description");
    }

    public static void applyStyle(HTMLDocument document, String styleName)
    {
        URL styleURL = TemplateUtils.class.getResource("resources/" + styleName + ".css");
        StyleSheet styleSheet = document.getStyleSheet();
        try
        {
            styleSheet.loadRules(new InputStreamReader(styleURL.openStream(), StandardCharsets.UTF_8), styleURL);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static String getStyle()
    {
        return getStyle("description");
    }

    public static String getStyle(String styleName)
    {
        URL styleURL = TemplateUtils.class.getResource("resources/" + styleName + ".css");
        String style = "";
        if( styleURL != null )
        {
            try
            {
                style = "<style>\n"+ApplicationUtils.readAsString(styleURL.openStream())+"</style>";
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return style;
    }
}
