package ru.biosoft.server.servlets.webservices.imports;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.ImageIcon;

import biouml.standard.type.Species;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.CustomImageLoader;
import ru.biosoft.util.SpeciesDrawer;

public class OmicsImageLoader implements CustomImageLoader, SpeciesDrawer
{
    private OmicsType type;
    private Image omicsImage;
    private Image omicsImageWithSpecies;
    protected OmicsImageLoader(OmicsType type)
    {
        this.type = type;
        URL url = getClass().getResource( "resources/" + type.abbrev + ".png" );
        System.out.println( url.toString() );
        omicsImage = new ImageIcon( url ).getImage();
        omicsImageWithSpecies = new ImageIcon( getClass().getResource( "resources/" + type.abbrev + "_sp.png" ) ).getImage();
    }
    
    @Override
    // imageId = "pluginName:resources/genes.png|species:Homo sapiens" or "pluginName:resources/genes.png"
    public ImageIcon loadImage(String imageId)
    {
        int pipeIdx = imageId.indexOf("|species:");
        String iconPath = (pipeIdx > 0) ? imageId.substring(0, pipeIdx) : imageId;
        ImageIcon icon = ApplicationUtils.getImageIcon(iconPath);
        if (icon == null) return null;
        return addOmicsLabel( icon, imageId );
    }

    private ImageIcon addOmicsLabel(ImageIcon icon, String imageId)
    {
        Image origImg = icon.getImage();
        BufferedImage image = new BufferedImage( 16, 16, BufferedImage.TYPE_INT_ARGB );
        Graphics graphics = image.getGraphics();
        graphics.drawImage( origImg, 0, 0, null );
        String speciesName = getSpecies( imageId );
        if( speciesName != null && Species.getSpecies( speciesName ) != null )
            graphics.drawImage( omicsImageWithSpecies, 0, 0, null );
        else
            graphics.drawImage( omicsImage, 0, 0, null );
        // code below draw 2 letters abbreviation in left bottom corner, but due to small icon size it is not readable
        //        if( speciesName != null )
        //        {
        //            Species species = Species.getSpecies( speciesName );
        //            if( species != null && species.getAbbreviation() != null )
        //            {
        //                String abbrev = species.getAbbreviation();
        //                graphics.setFont( new Font( "Monospaced", Font.PLAIN, 8 ) );
        //                FontMetrics fm = graphics.getFontMetrics();
        //                int tw = fm.stringWidth( abbrev );
        //                int th = fm.getHeight();
        //                int x = 0;
        //                int y = 16 - 1;
        //                //                graphics.setColor( Color.WHITE );
        //                //                graphics.fillRect( x - 1, y - th + 1, tw + 2, th );
        //                graphics.setColor( Color.BLACK );
        //                graphics.drawString( abbrev, x, y );
        //            }
        //        }

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

    @Override
    public String getSpecies(String imageId)
    {
        int pipeIdx = imageId.indexOf( "|species:" );
        return (pipeIdx > 0) ? imageId.substring( pipeIdx + 9 ) : null;
    }
    
}
