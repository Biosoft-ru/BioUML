package biouml.plugins.sbml.celldesigner._test;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import javax.imageio.ImageIO;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.workbench.diagram.ImageExporter;
import ru.biosoft.access.DataElementExporterRegistry;

public class TestUtil
{
    public static void setProxy()
    {
        String proxyHost = "192.168.199.1";
        String proxyPort = "8080";
        System.setProperty("proxyHost", proxyHost);
        System.setProperty("proxyPort", proxyPort);
        System.setProperty("proxySet", "true");

        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("http.proxySet", "true");

        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);
        System.setProperty("https.proxySet", "true");
    }
    
    public static void downloadImage(String link, File file) throws Exception
    {
        double time = System.currentTimeMillis();
        System.out.println("Downloading: " + link);
        URL server = new URL(link);
        HttpURLConnection connection = (HttpURLConnection)server.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection
                .addRequestProperty(
                        "Accept",
                        "image/png, image/x-xbitmap, image/jpeg, image/pjpeg, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/x-shockwave-flash, */*");
        connection.addRequestProperty("Accept-Language", "en-us,zh-cn;q=0.5");
        connection.addRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; .NET CLR 2.0.50727; MS-RTC LM 8)");
        connection.connect();
        ApplicationUtils.copyStream(new FileOutputStream(file), connection.getInputStream());
        System.out.println("Elapsed time "+  (System.currentTimeMillis() - time) / 1000);
    }
    
    public static void download(String link, File file) throws Exception
    {
        double time = System.currentTimeMillis();
        System.out.println("Downloading: " + link);
        URL server = new URL(link);
        HttpURLConnection connection = (HttpURLConnection)server.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.addRequestProperty("Accept",
                "text/xml, image/png, image/x-xbitmap, image/jpeg, image/pjpeg, application/msword, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/x-shockwave-flash, */*");
        connection.addRequestProperty("Accept-Language", "en-us,zh-cn;q=0.5");
        connection.addRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0; .NET CLR 2.0.50727; MS-RTC LM 8)");
        connection.connect();
        ApplicationUtils.copyStream(new FileOutputStream(file), connection.getInputStream());
        System.out.println("Elapsed time " + ( System.currentTimeMillis() - time ) / 1000);
    }


    public static void resizeImage(File source, File result) throws IOException
    {
        Image image = ImageIO.read(new FileInputStream(source));

        int thumbWidth = 500;
        int thumbHeight = 500;

        // Make sure the aspect ratio is maintained, so the image is not skewed
        double thumbRatio = (double)thumbWidth / (double)thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double)imageWidth / (double)imageHeight;
        if( thumbRatio < imageRatio )
            thumbHeight = (int) ( thumbWidth / imageRatio );
        else
            thumbWidth = (int) ( thumbHeight * imageRatio );

        if( thumbWidth > imageWidth && thumbHeight > imageHeight )
        {
            ApplicationUtils.copyFile(result, source);
        }
        else
        {
            Image thumbImage = image.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH);

            BufferedImage renderedImage = new BufferedImage(thumbImage.getWidth(null), thumbImage.getHeight(null),
                    BufferedImage.TYPE_INT_RGB);
            renderedImage.getGraphics().drawImage(thumbImage, 0, 0, null);
            ImageIO.write(renderedImage, "png", result);
        }
    }
    
    public static void writeImage(Diagram diagram, File outputFile) throws Exception
    {
        diagram.setView(null);//clean old view
        ImageExporter writer = new ImageExporter();
        Properties properties = new Properties();
        properties.setProperty(DataElementExporterRegistry.FORMAT, "PNG");
        properties.setProperty(DataElementExporterRegistry.SUFFIX, ".png");
        writer.init(properties);
        writer.doExport(diagram, outputFile);
    }
}
