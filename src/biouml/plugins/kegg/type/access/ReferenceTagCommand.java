package biouml.plugins.kegg.type.access;

import java.util.ArrayList;
import java.util.StringTokenizer;

import one.util.streamex.EntryStream;

import biouml.standard.type.Protein;
import ru.biosoft.access.support.TagCommand;

public class ReferenceTagCommand implements TagCommand
{
    private static final char TAB = '\t';
    private static final String LN = System.getProperty("line.separator");
    private static final String UI_PREFIX = "UI:";
    private static final String PMID_PREFIX = "PMID:";
    private static final String L_BR = "[";
    private static final String R_BR = "]";

    private String tag;
    private StringBuffer reference;
    private ArrayList<String> references;
    private EnzymeTransformer transformer;

    public ReferenceTagCommand(String tag, EnzymeTransformer transformer)
    {
        this.tag = tag;
        this.transformer = transformer;
    }

    @Override
    public void start(String tag)
    {
        reference = new StringBuffer();
        references = new ArrayList<>(0);
    }

    @Override
    public void addValue(String value)
    {
        if( value == null || value.length() == 0 )
            return;

        int id = 0;
        StringTokenizer st = new StringTokenizer(value, " \t");
        if( st.hasMoreTokens() )
        {
            try
            {
                id = Integer.parseInt(st.nextToken().trim());
            }
            catch( NumberFormatException e )
            {

            }
        }

        // TODO: check this algorithm: seems that it works strangely
        if( id > 0 )
        {
            nextReference();
            if( st.hasMoreTokens() )
            {
                parseReferenceId(st.nextToken().trim());
            }
        }
        else
        {
            if( reference.length() > 0 )
                reference.append(" ");

            reference.append(value);
        }
    }

    private void nextReference()
    {
        //System.err.println("nextReference");
        if( reference.length() > 0 )
        {
            /*if (pmid != null)
                ref.setPmid(pmid);
            if (ui != null)
                ref.setMedlineUI(ui);*/

            references.add(reference.toString());
        }

        reference = new StringBuffer();
    }

    private String parseReferenceId(String refId)
    {
        if( refId.startsWith(L_BR) && refId.endsWith(R_BR) )
        {
            refId = refId.substring(1, refId.length() - 1);
            if( refId.startsWith(UI_PREFIX) )
            {
                refId = refId.substring(UI_PREFIX.length());
            }
            else if( refId.startsWith(PMID_PREFIX) )
            {
                refId = refId.substring(PMID_PREFIX.length());
            }
        }
        return refId;
    }

    @Override
    public void complete(String tag)
    {
        if( reference.length() > 0 )
            nextReference();

        //System.err.println("references:" + references);
        String[] refs = references.toArray(new String[references.size()]);
        Protein enzyme = transformer.getProcessedObject();
        enzyme.setLiteratureReferences(refs);
    }

    @Override
    public String getTag()
    {
        return tag;
    }

    @Override
    public String getTaggedValue()
    {
        Protein enzyme = transformer.getProcessedObject();
        String[] refs = enzyme.getLiteratureReferences();
        if( refs != null )
        {
            return tag + TAB + EntryStream.of( refs ).mapKeyValue( (i, ref) -> ( i + 1 ) + LN + ref + LN ).joining();
        }
        return null;
    }

    @Override
    public String getTaggedValue(String value)
    {
        throw new UnsupportedOperationException();
    }
}
