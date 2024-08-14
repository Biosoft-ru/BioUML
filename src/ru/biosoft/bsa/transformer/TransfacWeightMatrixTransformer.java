
package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import biouml.standard.type.Publication;


/**
 * Transform {@link Entry} data element of Transfac Matrix Library file to {@link
 * SimpleWeightMatrix} type.<br>
 * Implements Transformer interface.
 * <br><br>Sample of Transfac Matrix Library entry: <br>
 * <pre>
 * AC  M00001
 * XX
 * ID  V$MYOD_01
 * XX
 * DT  19.10.1992 (created); ewi.
 * DT  22.10.1997 (updated); dbo.
 * CO  Copyright (C), Biobase GmbH.
 * XX
 * NA  MyoD
 * XX
 * DE  myoblast determination gene product
 * XX
 * BF  T00525; MyoD; Species: human, Homo sapiens.
 * BF  T00524; MyoD; Species: clawed frog, Xenopus laevis.
 * BF  T00526; MyoD; Species: mouse, Mus musculus.
 * BF  T00527; MyoD; Species: monkey, Cercopithecus aethiops.
 * BF  T01128; MyoD; Species: chick, Gallus gallus.
 * BF  T01551; MyoD; Species: quail, Coturnix coturnix.
 * BF  T03902; MyoD; Species: common carp, Cyprinus carpio.
 * BF  T03907; MyoD; Species: zebra fish, Brachydanio rerio.
 * BF  T10027; MyoD; Species: Mammalia.
 * BF  T09177; MyoD; Species: mouse, Mus musculus.
 * BF  T09197; MyoD; Species: human, Homo sapiens.
 * BF  T10010; MyoD; Species: clawed frog, Xenopus laevis.
 * BF  T15547; MyoD; Species: chick, Gallus gallus.
 * BF  T15571; MyoD; Species: rat, Rattus norvegicus.
 * BF  T15572; MyoD; Species: rat, Rattus norvegicus.
 * BF  T03912; MyoD (275 AA); Species: rainbow trout, Oncorhynchus mykiss.
 * BF  T03911; MyoD (376 AA); Species: rainbow trout, Oncorhynchus mykiss.
 * XX
 * P0      A      C      G      T
 * 01      1      2      2      0      S
 * 02      2      1      2      0      R
 * 03      3      0      1      1      A
 * 04      0      5      0      0      C
 * 05      5      0      0      0      A
 * 06      0      0      4      1      G
 * 07      0      1      4      0      G
 * 08      0      0      0      5      T
 * 09      0      0      5      0      G
 * 10      0      1      2      2      K
 * 11      0      2      0      3      Y
 * 12      1      0      3      1      G
 * XX
 * BA  5 functional elements in 3 genes
 * XX
 * //
 * </pre>
 */

public class TransfacWeightMatrixTransformer extends AbstractTransformer<Entry, FrequencyMatrix>
{
    private static Logger log = Logger.getLogger(TransfacWeightMatrixTransformer.class.getName());
    /** tags of MatchPro Matrix Library file */
    public static final String FACTORS_COLLECTION_PROPERTY = "factors";

    /** @return Entry.class */
    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    /** @return  SimpleWeightMatrix.class */
    @Override
    public Class<FrequencyMatrix> getOutputType()
    {
        return FrequencyMatrix.class;
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
        return read(getTransformedCollection(), reader);
    }

    /**
     * Transform SimpleWeightMatrix data element to Entry data element.
     * @param output SimpleWeightMatrix ru.biosoft.access.core.DataElement
     * @return Entry data element
     */
    @Override
    public Entry transformOutput(FrequencyMatrix output)
    {
        //TODO: support saving
        //return new Entry( getPrimaryCollection(), matrix.getName(), "" + out, Entry.TEXT_FORMAT );
        throw new UnsupportedOperationException("Saving into TRANSFAC matrix library is not supported");
    }

    final private FrequencyMatrix read(DataCollection<?> parentCollection, BufferedReader reader) throws Exception
    {
        String line = null;
        String name = null, reference = null;
        double matrix[][] = null;
        DataElementPathSet factorLinks = new DataElementPathSet();
        List<TranscriptionFactor> factors = new ArrayList<>();
        DataElementPath factorsPath = DataElementPath.create(parentCollection).getRelativePath(parentCollection.getInfo().getProperties().getProperty(FACTORS_COLLECTION_PROPERTY, Const.TRANSFAC_FACTORS_RELATIVE));
        String factorName = null;
        ArrayList<Publication> publications = new ArrayList<>();
        int pubNumb=-1;
        while( ( line = reader.readLine() ) != null )
        {
            int idx[] = new int[4];
            if( line.startsWith("RN") )
            {
                pubNumb++;
                publications.add(new Publication(null, line.substring(4, line.length()-1)));
            }
            if( line.startsWith("RX") )
            {
                publications.get(pubNumb).setIdName(line.substring(line.indexOf(":") + 2, line.length() - 1));
                publications.get(pubNumb).setPubMedId(line.substring(line.indexOf(":") + 2, line.length() - 1));
                publications.get(pubNumb).setReference("http://www.ncbi.nlm.nih.gov/pubmed/"+line.substring(line.indexOf(":") + 2, line.length() - 1));
            }
            else if( line.startsWith("RA") )
            {
                publications.get(pubNumb).setAuthors(line.substring(4));
            }
            else if( line.startsWith("RT") )
            {
                publications.get(pubNumb).setTitle(line.substring(4));
            }
            else if( line.startsWith("RL") )
            {
                publications.get(pubNumb).setTitle(publications.get(pubNumb).getTitle() + " " + line.substring(4));
                Pattern pat = Pattern.compile("\\("+"\\d\\d\\d\\d"+"\\)");
                Matcher matcher = pat.matcher(line);
                if( matcher.find(0) )
                {
                    int beginInd = matcher.start()+1;
                    int endInd = matcher.end()-1;
                    publications.get(pubNumb).setYear(line.substring(beginInd, endInd));
                }
            }
            if( line.startsWith("//") )
            {
                break;
            }
            else if( line.startsWith("AC") )
            {
                reference = line.substring(3).trim();
            }
            else if( line.startsWith("ID") )
            {
                name = line.substring(3).trim();
            }
            else if( line.startsWith("NA") )
            {
                factorName = line.substring(3).trim();
            }
            else if( line.startsWith("BF") )
            {
                fillFactors(line, reader, factors, factorLinks, factorsPath);
            }
            else if( line.startsWith("P0") )
            {
                StringTokenizer strTok = new StringTokenizer(line.substring(3), " \t");
                for( int i = 0; strTok.hasMoreTokens(); i++ )
                {
                    switch( strTok.nextToken().charAt(0) )
                    {
                        case 'A':
                            idx[0] = i;
                            break;
                        case 'C':
                            idx[1] = i;
                            break;
                        case 'G':
                            idx[2] = i;
                            break;
                        case 'T':
                            idx[3] = i;
                            break;
                        default:
                            throw new Exception("Bad format");
                    }
                }

                Vector<double[]> vec = new Vector<>();

                while( ( line = reader.readLine() ) != null && !line.startsWith("XX") )
                {
//                    if( line.startsWith("XX") )
//                    {
//                        break;
//                    }
                    StringTokenizer st = new StringTokenizer(line.substring(3), " \t");
                    double temp[] = new double[4];


                    for( int i = 0; st.hasMoreTokens() && i < 4; i++ )
                    {
                        temp[idx[i]] = Double.parseDouble(st.nextToken());
                    }
                    vec.add(temp);
                }

                matrix = vec.toArray(new double[0][4]);
//                break;
            }
            line = null;
        }

        BindingElement be;
        if(!factors.isEmpty())
        {
            for(DataElementPath path: factorLinks)
            {
                TranscriptionFactor de = path.optDataElement(TranscriptionFactor.class);
                if(de != null)
                {
                    factors.add(de);
                }
            }
            be = new BindingElement(factorName, factors);
        } else
        {
            be = new BindingElement(factorName, factorLinks);
        }
        return new FrequencyMatrix(getTransformedCollection(), name, reference, Nucleotide15LetterAlphabet.getInstance(), be, matrix, false, publications.size()!=0? publications.toArray(new Publication [publications.size()]):null);
    }

    private static final Pattern bfPattern = Pattern.compile("BF\\s+([^;]+);\\s*([^;]+);\\s*Species: (.+)\\.");
    private void fillFactors(String line, BufferedReader reader, List<TranscriptionFactor> factors, DataElementPathSet factorLinks, DataElementPath factorsPath) throws IOException
    {
        StringBuilder bfLine = new StringBuilder(line);
        while( ( line = reader.readLine() ) != null && line.startsWith("BF") && line.indexOf(';') < 0 )
        {
            bfLine.append(line.substring(3));
        }

        Matcher matcher = bfPattern.matcher(bfLine.toString());
        if( !matcher.find() )
        {
            log.log(Level.SEVERE, "Can't extract transcription factor name from '" + bfLine.toString() + "'");
        }
        else
        {
            String factor = matcher.group(1);
            DataElementPath tf = factorsPath.getRelativePath(factor);
            factorLinks.add(tf);
        }
        if( line != null && !line.startsWith("XX") )
        {
            fillFactors(line, reader, factors, factorLinks, factorsPath);
        }
    }
}
