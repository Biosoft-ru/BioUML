package biouml.plugins.physicell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import ru.biosoft.physicell.ui.ResultGenerator;

public class VideoGenerator extends ResultGenerator
{
    public static void main(String... args) throws IOException
    {
        String path = "C:\\Users\\Damag\\eclipse_2024_6\\COVID19\\PhysiCell\\output\\Imgs";
        File f = new File(path);
        VideoGenerator generator = new VideoGenerator(new File("D:/BIOFVM/d.mp4"));
        generator.init();
        for (File imgFile: f.listFiles())
        {
            BufferedImage img = ImageIO.read(imgFile);
            
            generator.update( img );
        }
        generator.finish();
    }
    
    private int fps = 5;
    private SeekableByteChannel out = null;
    private AWTSequenceEncoder encoder = null;

    public VideoGenerator(File file)
    {
        super( file );
    }
    
    public VideoGenerator(File file, int fps)
    {
        super( file );
        this.fps = fps;
    }

    @Override
    public void init() throws IOException
    {
        out = NIOUtils.writableFileChannel( result.getAbsolutePath() );
        encoder = new AWTSequenceEncoder( out, Rational.R( fps, 1 ) );
    }

    @Override
    public void update(BufferedImage image) throws IOException
    {
        encoder.encodeImage( image );
    }

    @Override
    public void finish() throws IOException
    {
        encoder.finish();
        NIOUtils.closeQuietly( out );
    }
}