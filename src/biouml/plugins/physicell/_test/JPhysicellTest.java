package biouml.plugins.physicell._test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import biouml.plugins.physicell.VideoGenerator;
import ru.biosoft.physicell.biofvm.Microenvironment;
import ru.biosoft.physicell.core.Model;

public class JPhysicellTest
{

    public static void main(String... args) throws IOException
    {
        String path = "D:/BIOFVM/Images All";
        File f = new File(path);
        VideoGenerator generator = new VideoGenerator(new File(path+"/3d.mp4"));
        generator.init();
        for (File imgFile: f.listFiles())
        {
            BufferedImage img = ImageIO.read(imgFile);
            generator.update( img );
        }
        generator.finish();
    }
//    public static void main(String ... args)
//    {
//
//        Model model = new Model();
//        Microenvironment m = model.getMicroenvironment();
//        m.options.initial_condition_vector = new double[1];
//        m.addDensity( "oxygen2", "", 0, 0 );
//
//        m.resizeSpace( 0, 100, 0, 100, 0, 100, 20, 20, 20 );
//        System.out.println( model.display() );
//    }
}