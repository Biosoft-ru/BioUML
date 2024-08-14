package biouml.model.xml._test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

public class ImageBase64FromFile
{
    public static void main(String[] args) throws IOException
    {
        if( args.length != 2 )
        {
            System.err.println("<program> inputfile outputfile");
        }
        else
        {
            BufferedImage image = ImageIO.read(new FileInputStream(args[0]));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            try(OutputStream out = new FileOutputStream(args[1]))
            {
                out.write( Base64.getEncoder().encode( baos.toByteArray() ) );
            }
        }
    }
}
