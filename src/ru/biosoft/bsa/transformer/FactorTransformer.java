
package ru.biosoft.bsa.transformer;

import java.io.BufferedReader;

import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.TransfacTranscriptionFactor;

/** @PENDING FactorTransformerTest */
public class FactorTransformer extends AbstractTransformer<Entry, TransfacTranscriptionFactor>
{
    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    @Override
    public Class<TransfacTranscriptionFactor> getOutputType()
    {
        return TransfacTranscriptionFactor.class;
    }

    @Override
    public TransfacTranscriptionFactor transformInput(Entry entry) throws Exception
    {
        BufferedReader reader = new BufferedReader(entry.getReader());
        String line;

        String name                         = null;
        String taxonCompleteName            = null;
        String dnaBindingDomainCompleteName = null;
        String generalClassCompleteName     = null;
        String displayName                  = null;
        String positiveTissueSpecificity    = null;
        String negativeTissueSpecificity    = null;

        while(true)
        {
            line = reader.readLine();
            if(line == null || line.startsWith("//"))
                break;

            else if(line.startsWith("ID")) name = line.substring(3);
            else if(line.startsWith("TX")) taxonCompleteName = line.substring(3);
            else if(line.startsWith("DN")) dnaBindingDomainCompleteName = line.substring(3);
            else if(line.startsWith("GC")) generalClassCompleteName = line.substring(3);
            else if(line.startsWith("FA")) displayName = line.substring(3);
            else if(line.startsWith("CP")) positiveTissueSpecificity = line.substring(3);
            else if(line.startsWith("CN")) negativeTissueSpecificity = line.substring(3);

        }

        return new TransfacTranscriptionFactor(name, getTransformedCollection(),
            displayName, taxonCompleteName, dnaBindingDomainCompleteName, generalClassCompleteName,
            positiveTissueSpecificity, negativeTissueSpecificity);
    }

    @Override
    public Entry transformOutput(TransfacTranscriptionFactor tf)
    {
        String name            = tf.getName();
        StringBuffer strBuf    = new StringBuffer();
        if(name != null)
        {
            strBuf.append("ID ");
            strBuf.append(name);
            strBuf.append(lineSep);
        }

        //---------------- taxon -----------------------------------------------
        DataElementPath str = null;
        str = tf.getTaxonPath();
        if(str != null)
            addLine(strBuf, "TX", str.toString());

        //-----------------DNA Binding Domain ----------------------------------
        str = tf.getDNABindingDomainPath();
        if(str != null)
            addLine(strBuf, "DN", str.toString());

        //-------------------- General Class -----------------------------------
        str = tf.getGeneralClassPath();
        if(str != null)
            addLine(strBuf, "GC", str.toString());

        //----------------------------------------------------------------------
        addLine(strBuf, "FA", tf.getDisplayName());
        addLine(strBuf, "CP", tf.getPositiveTissueSpecificity());
        addLine(strBuf, "CN", tf.getNegativeTissueSpecificity());
        strBuf.append("//" + lineSep);
        return new Entry(getPrimaryCollection(), name, "" + strBuf, Entry.TEXT_FORMAT);
    }

    void addLine(StringBuffer strBuf, String field, String value)
    {
        if(value != null)
        {
            strBuf.append(field);
            strBuf.append(" ");
            strBuf.append(value);
            strBuf.append(lineSep);
        }
    }
}

