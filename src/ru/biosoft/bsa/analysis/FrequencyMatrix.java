package ru.biosoft.bsa.analysis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import biouml.standard.type.Publication;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.Util;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.ImageUtils;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * FrequencyMatrix store frequencies (probabilities) of letters
 * at positions [ 0, matrix.getLength() ) of certain  binding element.
 * Frequencies for ambiguous letters are automatically recalculated
 * as a sum of probabilities for corresponding basic letters.
 * @see Alphabet
 */
@ClassIcon ( "resources/matrix.gif" )
@PropertyName ("matrix")
public class FrequencyMatrix extends DataElementSupport implements ImageElement, CloneableDataElement
{
    public BindingElement bindingElement;
    private Alphabet alphabet;
    private double[][] matrix;
    private String accession;
    private Publication[] publications = null;

    /**
     * @param origin
     * @param name
     * @param alphabet
     * @param bindingElement
     * @param weights          weight[position][letterCode] either frequency or count of letter at position.
     * @param pseudoCounts     whether to add pseudocounts, if set weights should contain count data
     */
    public FrequencyMatrix(DataCollection<?> origin, String name, Alphabet alphabet, BindingElement bindingElement, double[][] weights,
            boolean pseudoCounts)
    {
        this(origin, name, name, alphabet, bindingElement, weights, pseudoCounts);
    }

    /**
     * @param origin
     * @param name
     * @param accession TODO
     * @param alphabet
     * @param bindingElement
     * @param weights          weight[position][letterCode] either frequency or count of letter at position.
     * @param pseudoCounts     whether to add pseudocounts, if set weights should contain count data
     */
    public FrequencyMatrix(DataCollection<?> origin, String name, String accession, Alphabet alphabet, BindingElement bindingElement,
            double[][] weights, boolean pseudoCounts)
    {
        super(name, origin);
        this.alphabet = alphabet;
        this.bindingElement = bindingElement;
        this.accession = accession;
        this.matrix = new double[weights.length][alphabet.size()];
        for( int i = 0; i < weights.length; i++ )
            for( int j = 0; j < alphabet.size() && j < weights[i].length; j++ )
                matrix[i][j] = weights[i][j];
        normalize(pseudoCounts);
    }

    /**
     * Copy matrix.
     * @param origin            origin of new matrix
     * @param name              name of new matrix
     * @param frequencyMatrix   matrix to copy
     */
    public FrequencyMatrix(DataCollection<?> origin, String name, FrequencyMatrix frequencyMatrix)
    {
        this(origin, name, name, frequencyMatrix.getAlphabet(), frequencyMatrix.getBindingElement(), frequencyMatrix.matrix, false);
    }

    public FrequencyMatrix(DataCollection<?> origin, String name, String accession, Alphabet alphabet, BindingElement bindingElement,
            double[][] weights, boolean pseudoCounts, Publication[] p)
    {
        this(origin, name, accession, alphabet,bindingElement, weights, pseudoCounts);
        this.publications = p;
    }
    /** @return Alphabet used by this matrix. */
    public Alphabet getAlphabet()
    {
        return alphabet;
    }

    public BindingElement getBindingElement()
    {
        return bindingElement;
    }

    public void setBindingElement(BindingElement bindingElement)
    {
        this.bindingElement = bindingElement;
    }

    public String getBindingElementName()
    {
        return bindingElement.getName();
    }

    /** @return the matrix length. */
    public int getLength()
    {
        return matrix.length;
    }

    public String getAccession()
    {
        return accession;
    }

    public double getFrequency(int position, byte letterCode)
    {
        return matrix[position][letterCode];
    }

    public Publication[] getPublications()
    {
        return publications;
    }

    public CompositeView getView()
    {
        if( view == null )
        {
            view = PWMLogoCreator.createLogoView(this);
        }
        return view;
    }
    
    @Override
    public Dimension getImageSize()
    {
        View view = getView();
        return ImageUtils.correctImageSize(view.getBounds().getSize());
    }

    @Override
    public BufferedImage getImage(Dimension dimension)
    {
        View view = getView();
        Dimension d = dimension == null ? getImageSize() : ImageUtils.correctImageSize(dimension);
        Rectangle r = view.getBounds();
        BufferedImage image = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(((double)d.width)/r.width, ((double)d.height)/r.height);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(1, 1, 1, 0.f));
        graphics.fill(new Rectangle(0, 0, r.width, r.height));
        view.paint(graphics);
        return image;
    }

    public void updateFromSequences(Collection<Sequence> sequences)
    {
        for( int i = 0; i < matrix.length; i++ )
            for( int j = 0; j < matrix[i].length; j++ )
                matrix[i][j] = 0;
    
        for( Sequence seq : sequences )
        {
            if( seq.getLength() != matrix.length )
                throw new IllegalArgumentException("Can not update matrix, length of sequence and matrix differ");
            if( seq.getAlphabet() != getAlphabet() )
                throw new IllegalArgumentException("Sequence alphabet and matrix alphabet should be same");
            for( int i = 0; i < matrix.length; i++ )
                matrix[i][seq.getLetterCodeAt(i + seq.getStart())] += 1;
        }
    
        byte codesCount = getAlphabet().size();
        for( int i = 0; i < matrix.length; i++ )
            for( byte code = 0; code < codesCount; code++ )
            {
                byte[] basicCodes = getAlphabet().basicCodes(code);
                if( basicCodes.length <= 1 )
                    continue;
                double value = getFrequency(i, code) / basicCodes.length;
                for( byte basicCode : basicCodes )
                    setFrequency(i, basicCode, getFrequency(i, basicCode) + value);
            }
    
        normalize(false);
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public static double L1NormDiff(FrequencyMatrix m1, FrequencyMatrix m2)
    {
        return Util.matrixL1NormForDifference(m1.matrix, m2.matrix);
    }

    @Override
    public FrequencyMatrix clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        FrequencyMatrix result = (FrequencyMatrix)super.clone(origin, name);
        result.view = null;
        if(publications != null)
            result.publications = Arrays.copyOf(publications, publications.length);
        result.matrix = Util.copy(matrix);
        return result;
    }

    /**
     * Computes information content (in bits) for each position of FrequencyMatrix.
     */
    public double[] informationContent()
    {
        double[] ic = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        {
            ic[i] = Math.log(alphabet.basicSize()) / Math.log(2);// maximum possible value of information content
            for( byte code : getAlphabet().basicCodes() )
            {
                double p = getFrequency(i, code);
                if( p != 0 )
                    ic[i] += p * Math.log(p) / Math.log(2);
            }
        }
        return ic;
    }

    protected void setFrequency(int position, byte code, double value)
    {
        matrix[position][code] = value;
    }

    /**
     * Normalize matrix columns, such that sum of values for basic letters equal to one.
     * Frequencies for ambiguous letters will be recalculated.
     * @param pseudoCounts
     */
    private void normalize(boolean pseudoCounts)
    {
        byte codesCount = alphabet.basicSize();
        for( int i = 0; i < matrix.length; i++ )
        {
            double sum = 0;
            for( byte code=0; code<codesCount; code++ )
                sum += matrix[i][code];
            for( byte code=0; code<codesCount; code++ )
            {
                if( pseudoCounts )
                    matrix[i][code] = ( matrix[i][code] + 0.25 / Math.sqrt(sum) ) / ( sum + 1.0 / Math.sqrt(sum) );
                else
                    matrix[i][code] /= sum;
            }
        }
        updateAmbiguousLetters();
    }

    /**
     * Update matrix values for ambiguous letters based on values for basic letters.
     * The matrix value for ambiguous letter is the sum of values for corresponding basic letters.
     */
    private void updateAmbiguousLetters()
    {
        byte codesCount = alphabet.size();
        for( int i = 0; i < matrix.length; i++ )
        {
            for(byte code = 0; code < codesCount; code++ )
            {
                double value = 0;
                for( byte basicCode : alphabet.basicCodes(code) )
                    value += matrix[i][basicCode];
                matrix[i][code] = value;
            }
        }
    }

    private CompositeView view = null;
}
