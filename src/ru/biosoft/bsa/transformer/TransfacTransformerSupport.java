package ru.biosoft.bsa.transformer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Publication;
import ru.biosoft.access.core.AbstractTransformer;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.Entry;
import ru.biosoft.bsa.Const;
import ru.biosoft.util.ObjectCache;

/**
 * @author lan
 *
 */
public abstract class TransfacTransformerSupport<O extends DataElement> extends AbstractTransformer<Entry, O>
{
    private static final Pattern PUBLICATION_YEAR_PATTERN = Pattern.compile("\\(" + "\\d\\d\\d\\d" + "\\)");
    private static ObjectCache<String> dbNames = new ObjectCache<>();
    /**
     * Returns Entry.class
     *
     * @return Entry.class
     */
    @Override
    public Class<Entry> getInputType()
    {
        return Entry.class;
    }

    protected DatabaseReference parseDatabaseReference(String refText)
    {
        refText = refText.substring(0, refText.length() - 1);
        String[] fields = refText.split(": ");
        String[] ac = fields[fields.length - 1].split("; ");
        DatabaseReference ref = new DatabaseReference(dbNames.get(fields[0]), ac[0]);
        if( ac.length > 1 )
            ref.setComment(ac[ac.length - 1]);
        ref.setRelationshipType("is");
        return ref;
    }

    /**
     * Updates publications list from source line
     * @param line (RN, RX, RA, RT or RL prefixes are parsed, other lines are ignored)
     * @param publications (list of publications to update)
     */
    protected void updatePublications(String key, String value, List<Publication> publications)
    {
        int pubNumb = publications.size() - 1;
        if( key.equals("RN") )
        {
            publications.add(new Publication(null, value.substring(0, value.length() - 1)));
        }
        else if( key.equals("RX") )
        {
            String pmId = value.substring(value.indexOf(":") + 2, value.length() - 1);
            publications.get(pubNumb).setIdName(pmId);
            publications.get(pubNumb).setPubMedId(pmId);
        }
        else if( key.equals("RA") )
        {
            publications.get(pubNumb).setAuthors(value);
        }
        else if( key.equals("RT") )
        {
            publications.get(pubNumb).setTitle(value);
        }
        else if( key.equals("RL") )
        {
            publications.get(pubNumb).setTitle(publications.get(pubNumb).getTitle() + " " + value);
            Matcher matcher = PUBLICATION_YEAR_PATTERN.matcher(value);
            if( matcher.find(0) )
            {
                int beginInd = matcher.start() + 1;
                int endInd = matcher.end() - 1;
                publications.get(pubNumb).setYear(value.substring(beginInd, endInd));
            }
        }
    }

    /**
     * @return path to classifications collection
     */
    protected DataElementPath getClassificationsPath()
    {
        return DataElementPath.create(primaryCollection).getRelativePath(
                primaryCollection.getInfo().getProperties().getProperty(Const.CLASSIFICATION_PATH_PROPERTY,
                        Const.TRANSFAC_CLASSIFICATIONS_RELATIVE));
    }

    /**
     * Converts TranscriptionFactor to the Entry
     *
     * @param output TranscriptionFactor
     * @return Entry of profile FileEntryCollection
     */
    @Override
    public Entry transformOutput(O output)
    {
        return null;
    }
}
