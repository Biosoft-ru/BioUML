package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.Entry;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.Repository;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.ExProperties;


/**
 * Transform {@link Entry} data element of MatchPro Matrix Library file to {@link
 * SimpleWeightMatrix} type.<br>
 * Implements Transformer interface.
 * <br><br>Sample of MatchPro Matrix Library entry: <br>
 * <pre>
 * ID  V$MYOD_01
 * MATR_LENGTH  12
 * CORE_START  4
 * CORE_LENGTH  5
 * MAXIMAL  2997.722566074246
 * MINIMAL  0.0
 * BF  localhost/factors/factor/T00526
 * WEIGHTS
 * 1 A:23.9036 C:47.80719 G:47.80719 T:0 N:29
 * 2 A:47.80719 C:23.9036 G:47.80719 T:0 N:29
 * 3 A:94.35741 C:0 G:31.45247 T:31.45247 N:39
 * 4 A:0 C:500 G:0 T:0 N:125
 * 5 A:500 C:0 G:0 T:0 N:125
 * 6 A:0 C:0 G:255.61438 T:63.9036 N:79
 * 7 A:0 C:63.9036 G:255.61438 T:0 N:79
 * 8 A:0 C:0 G:0 T:500 N:125
 * 9 A:0 C:0 G:500 T:0 N:125
 * 10 A:0 C:23.9036 G:47.80719 T:47.80719 N:29
 * 11 A:0 C:102.90494 G:0 T:154.35741 N:64
 * 12 A:31.45247 C:0 G:94.35741 T:31.45247 N:39
 * //
 * </pre>
 * @author DevelopmentOnTheEdge
 * @version 1.0
 * @see mgl3.access.TransformedDataCollection
 */

public class WeightMatrixTransformer extends AbstractTransformer<Entry, FrequencyMatrix>
{
    private static final Logger log = Logger.getLogger(WeightMatrixTransformer.class.getName());
    /** tags of MatchPro Matrix Library file */
    private static final DecimalFormat df;

    static
    {
        df = (DecimalFormat)DecimalFormat.getNumberInstance(Locale.ENGLISH);
        df.applyPattern(Const.DECIMAL_FORMAT_PATTERN);
    }

    private static final String DELIM = "//";
    private static final String ID = "ID";
    private static final String NA = "NA";
    private static final String MATR_LENGTH = "MATR_LENGTH";
    private static final String ALPHABET = "ALPHABET";
    private static final String WEIGHTS = "WEIGHTS";
    private static final String LINK_TRAN_FAC = "BF";

    /** @return Entry.class */
    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    /** @return  SimpleWeightMatrix.class */
    @Override
    public Class<? extends FrequencyMatrix> getOutputType()
    {
        return FrequencyMatrix.class;
    }

    @Override
    public boolean isOutputType(Class<?> type)
    {
        return FrequencyMatrix.class.isAssignableFrom(type);
    }

    /**
     * Transform  Entry data element to SimpleWeightMatrix data element.
     * @param input
     * @return SimpleWeightMatrix data element
     * @exception Exception any errors
     */
    @Override
    public FrequencyMatrix transformInput(Entry input) throws Exception
    {
        BufferedReader reader = new BufferedReader( input.getReader());
        //new StringReader( ( ( Entry )input ).getData() ) ) ;
        return read(getTransformedCollection(), reader);
    }

    /**
     * Transform SimpleWeightMatrix data element to Entry data element.
     * @param output SimpleWeightMatrix ru.biosoft.access.core.DataElement
     * @return Entry data element
     */
    @Override
    public Entry transformOutput(FrequencyMatrix matrix)
    {
        StringBuffer out = new StringBuffer();

        put(out, ID, matrix.getName());
        put(out, ALPHABET, matrix.getAlphabet().getClass().getName());
        put(out, MATR_LENGTH, "" + matrix.getLength());

        if( matrix.getBindingElement() != null )
        {
            //write down binding element
            String elementName = matrix.getBindingElement().getName();
            if( elementName != null )
            {
                put(out, NA, elementName);
            }
            for( TranscriptionFactor factor: matrix.getBindingElement() )
            {
                put(out, LINK_TRAN_FAC, DataElementPath.create(factor).toString());
            }
        }

        out.append(WEIGHTS);
        out.append(lineSep);

        for( int i = 0; i < matrix.getLength(); i++ )
        {
            out.append(i + 1);
            Alphabet alphabet = matrix.getAlphabet();
            for( byte code : alphabet.basicCodes() )
            {
                String letters = alphabet.codeToLetters(code).toUpperCase();
                out.append(" " + letters + ":");
                out.append(df.format(matrix.getFrequency(i, code)));
            }
            out.append(" " + lineSep);
        }

        out.append(DELIM);
        out.append(lineSep);
        return new Entry(getPrimaryCollection(), matrix.getName(), "" + out, Entry.TEXT_FORMAT);
    }

    final private FrequencyMatrix read(DataCollection parentCollection, BufferedReader in) throws Exception
    {
        String lineStr = null;
        String name = null;
        double[][] matrix = null;
        int length = 0;
        Vector<TranscriptionFactor> tranFactorLinks = new Vector<>();
        String BEName = null;
        Alphabet alphabet = null;
        while( ( lineStr = in.readLine() ) != null )
        {
            if( lineStr.startsWith(DELIM) )
            {
                break;
            }
            else if( lineStr.startsWith(ID) )
            {
                name = lineStr.substring(ID.length() + 1).trim();
            }
            else if ( lineStr.startsWith(ALPHABET) )
            {
                String className = lineStr.substring(ALPHABET.length() + 1).trim();
                Class<? extends Alphabet> clazz = Class.forName(className).asSubclass(Alphabet.class);
                Method method = clazz.getMethod("getInstance");
                alphabet = (Alphabet)method.invoke(null);
            }
            else if( lineStr.startsWith(MATR_LENGTH) )
            {
                length = Integer.parseInt(lineStr.substring(MATR_LENGTH.length() + 1).trim());
            }
            else if( lineStr.startsWith(LINK_TRAN_FAC) )
            {
                try
                {
                    TranscriptionFactor tf = DataElementPath.create(lineStr.substring(LINK_TRAN_FAC.length() + 1).trim())
                            .getDataElement(TranscriptionFactor.class);
                    tranFactorLinks.add(tf);
                }
                catch( DataElementNotFoundException e )
                {
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "While reading "+parentCollection.getCompletePath().getChildPath(name)+": "+ExceptionRegistry.log(e));
                }
            }
            else if( lineStr.startsWith(NA) )
            {
                BEName = lineStr.substring(NA.length() + 1).trim();
            }
            else if( lineStr.startsWith(WEIGHTS) )
            {
                if(alphabet == null)
                    throw new Exception("Alphabet not specified");

                if( length == 0 )
                    throw new Exception("Bad Matrix Format: length is null ");

                matrix = parseMatrix( in, length, alphabet );
            }
        }

        BindingElement be = null;
        if( BEName != null )
        {
            be = new BindingElement(BEName, tranFactorLinks);
        }

        return new FrequencyMatrix(parentCollection, name, alphabet, be, matrix, false);
    }

    static double[][] parseMatrix(BufferedReader in, int length, Alphabet alphabet) throws IOException, Exception
    {
        double[][] matrix;
        matrix = new double[length][alphabet.size()];
        boolean hasAmbiguousLetters = false;
        for( int i = 0; i < length; i++ )
        {
            String row = in.readLine();
            // check error
            if( row == null || !row.startsWith(String.valueOf(i + 1)) )
                throw new Exception("Bad Matrix Format: bad row number");

            StringTokenizer st = new StringTokenizer(row);
            st.nextToken(); // skip row number i.e. value of 'i'
            while(st.hasMoreTokens())
            {
                StringTokenizer oligSt = new StringTokenizer(st.nextToken(), ":");
                byte[] letters = oligSt.nextToken().getBytes();
                double weight = Double.valueOf(oligSt.nextToken()).doubleValue();
                byte code = alphabet.lettersToCode(letters, 0);
                if(code >= alphabet.basicSize())
                    hasAmbiguousLetters = true;
                matrix[i][code] = weight;
            }
        }
        if(!hasAmbiguousLetters)
        {
            double[][] basicMatrix = new double[length][alphabet.basicSize()];
            for(int i = 0; i < length; i++)
               for(int j = 0; j < alphabet.basicSize(); j++)
                   basicMatrix[i][j] = matrix[i][j];
            matrix = basicMatrix;
        }
        return matrix;
    }

    private void put(StringBuffer out, String tag, String value)
    {
        out.append(tag).append("  ").append(value).append(lineSep);
    }

    /**
     * Create new library for matrices
     * @param library path for newly created library (must point to non-existing element)
     * @throws Exception
     */
    public static void createMatrixLibrary(DataElementPath library) throws Exception
    {
        Properties primary = new ExProperties();
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        primary.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, library.getName() + ".lib");
        primary.setProperty(DataCollectionConfigConstants.NODE_IMAGE, ClassLoading.getResourceLocation( FrequencyMatrix.class, "resources/matrixlib.gif" ));
        primary.setProperty(DataCollectionConfigConstants.CHILDREN_NODE_IMAGE, ClassLoading.getResourceLocation( FrequencyMatrix.class, "resources/matrix.gif" ));
        primary.setProperty(FileEntryCollection2.ENTRY_DELIMITERS_PROPERTY, " \t");
        primary.setProperty(FileEntryCollection2.ENTRY_START_PROPERTY, "ID");
        primary.setProperty(FileEntryCollection2.ENTRY_ID_PROPERTY, "ID");
        primary.setProperty(FileEntryCollection2.ENTRY_END_PROPERTY, "//");

        Properties transformed = new ExProperties();
        transformed.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, WeightMatrixCollection.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.TRANSFORMER_CLASS, WeightMatrixTransformer.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, FrequencyMatrix.class.getName());
        transformed.setProperty(DataCollectionConfigConstants.NODE_IMAGE, ClassLoading.getResourceLocation( FrequencyMatrix.class, "resources/matrixlib.gif" ));
        transformed.setProperty(DataCollectionConfigConstants.CHILDREN_NODE_IMAGE, ClassLoading.getResourceLocation( FrequencyMatrix.class, "resources/matrix.gif" ));

        Repository parentRepository = (Repository)DataCollectionUtils.getTypeSpecificCollection(library.optParentCollection(),
                TransformedDataCollection.class);
        DataCollection<?> result = CollectionFactoryUtils.createDerivedCollection(parentRepository, library.getName(), primary, transformed, null);
        CollectionFactoryUtils.save(result);
    }
}
