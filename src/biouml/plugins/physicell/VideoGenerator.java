package biouml.plugins.physicell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import ru.biosoft.physicell.ui.ResultGenerator;

public class VideoGenerator extends ResultGenerator
{
    private SeekableByteChannel out = null;
    private AWTSequenceEncoder encoder = null;

    public VideoGenerator(File file)
    {
        super( file );
    }


    @Override
    public void init() throws IOException
    {
        out = NIOUtils.writableFileChannel( result.getAbsolutePath() );
        encoder = new AWTSequenceEncoder( out, Rational.R( 10, 1 ) );
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