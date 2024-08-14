package biouml.standard.type;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IMolecule;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.access.ViewUtils;
import ru.biosoft.util.ImageUtils;

/**
 * Represent 2D or 3D molecule/complex structure.
 * @pending use CDK library to show this structures.
 */
@ClassIcon("resources/structure.gif")
@PropertyName("Structure")
@PropertyDescription("2D or 3D molecule structure.")
public class Structure extends Referrer implements ImageElement
{
    private static final long serialVersionUID = -5883830389078341253L;
    
    /**
     * Name (identifier) of molecule to which this structure corresponds.
     * Generally, it could be structure of complex, in this case identifiers for all molecules could be stored in this array.
    */
    protected String[] moleculeReferences;
    
    /** Structure data format. */
    protected String format;
    
    /** Structure data (2D or 3D - MOL, ...) */
    protected String data;
    
    public Structure(DataCollection origin, String name)
    {
        super(origin,name);
    }

    @Override
    public String getType()
    {
        return TYPE_STRUCTURE;
    }

    @PropertyName("Molecules")
    @PropertyDescription("Molecule or molecules which structure is described.")
    public String[] getMoleculeReferences()
    {
        return moleculeReferences;
    }
    public void setMoleculeReferences(String[] moleculeReferences)
    {
        String[] oldValue = this.moleculeReferences;
        this.moleculeReferences = moleculeReferences;
        firePropertyChange("moleculeReferences", oldValue, moleculeReferences);
    }

    @PropertyName("Format")
    @PropertyDescription("Format of molecule structure data.")
    public String getFormat()
    {
        return format;
    }
    public void setFormat(String format)
    {
        String oldValue = this.format;
        this.format = format;
        firePropertyChange("format", oldValue, format);
    }

    @PropertyName("Data")
    @PropertyDescription("Molecule structure data.")
    public String getData()
    {
        return data;
    }
    public void setData(String data)
    {
        String oldValue = this.data;
        this.data = data;
        firePropertyChange("data", oldValue, data);
    }

    @Override
    public BufferedImage getImage(Dimension dimension)
    {
        Dimension d = dimension == null ? getImageSize() : ImageUtils.correctImageSize(dimension);
        try
        {
            return CDKRenderer.createStructureImage( this, d );
        }
        catch( Exception e )
        {
            return ViewUtils.paintException( e );
        }
    }
    
    @Override
    public Dimension getImageSize()
    {
        try
        {
            IMolecule molecule = CDKRenderer.loadMolecule( this );
            double avgBond = GeometryTools.getBondLengthAverage( molecule );
            if(Double.isNaN( avgBond ))
            {
                return new Dimension( 500, 500 );
            }
            double scale = 40.0/avgBond;
            double[] bounds = CDKRenderer.getBounds( molecule );
            bounds[0]*=scale;
            bounds[1]*=scale;
            return ImageUtils.correctImageSize( new Dimension( (int)bounds[0], (int)bounds[1] ) );
        }
        catch( Exception e )
        {
            return new Dimension( 500, 500 );
        }
    }
}