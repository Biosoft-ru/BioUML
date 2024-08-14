package ru.biosoft.server.servlets.webservices.imports;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.CustomImageLoader;

public class OmicsImageLoader implements CustomImageLoader
{
    private OmicsType type;
    private Image omicsImage;
    protected OmicsImageLoader(OmicsType type)
    {
        this.type = type;
        omicsImage = new ImageIcon( getClass().getResource( "resources/" + type.abbrev + ".png" ) ).getImage();
    }
    
    @Override
    public ImageIcon loadImage(String imageId)
    {
        ImageIcon icon = ApplicationUtils.getImageIcon( imageId );
        return addOmicsLabel(icon);
    }

    private ImageIcon addOmicsLabel(ImageIcon icon)
    {
        Image origImg = icon.getImage();
        BufferedImage image = new BufferedImage( 16, 16, BufferedImage.TYPE_INT_ARGB );
        Graphics graphics = image.getGraphics();
        graphics.drawImage( origImg, 0, 0, null );
        graphics.drawImage( omicsImage, 0, 0, null );
        return new ImageIcon( image  );
    }
    
    public static class T extends OmicsImageLoader
    {
        public T()
        {
            super( OmicsType.Transcriptomics );
        }
    }
    
    public static class P extends OmicsImageLoader
    {
        public P()
        {
            super( OmicsType.Proteomics );
        }
    }
    
    public static class G extends OmicsImageLoader
    {
        public G()
        {
            super( OmicsType.Genomics );
        }
    }
    
    public static class E extends OmicsImageLoader
    {
        public E()
        {
            super( OmicsType.Epigenomics );
        }
    }
    
    public static class M extends OmicsImageLoader
    {
        public M()
        {
            super( OmicsType.Metabolomics );
        }
    }
    
    public static Class<? extends OmicsImageLoader> getImageLoaderForType(OmicsType type)
    {
        switch( type )
        {
            case Transcriptomics: return T.class;
            case Proteomics: return P.class;
            case Genomics: return G.class;
            case Epigenomics: return E.class;
            case Metabolomics: return M.class;
            default:
                throw new AssertionError();
        }
    }
    
}
