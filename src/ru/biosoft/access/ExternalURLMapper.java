package ru.biosoft.access;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;
import biouml.standard.type.Base;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.LinkResolver;
import biouml.standard.type.Reaction;

import com.developmentontheedge.beans.DynamicPropertySet;

/**
 * Class to associate an URL with ru.biosoft.access.core.DataElement. Usually this URL should be opened if user clicks on such element in the UI
 * Some pseudo-URLs might be returned as well (like 'de:&lt;path&gt;')
 * @author lan
 * @todo Remove implicit dependency from Transpath & GeneWays
 */
public class ExternalURLMapper
{
    private static final Logger log = Logger.getLogger(ExternalURLMapper.class.getName());

    private static final Pattern ID_PATTERN = Pattern.compile("\\$id(.*)\\$");
    private static final Pattern BEAN_FIELD_PATTERN = Pattern.compile("(.*)\\:(.+[^/])");

    public static DataElementPath getPathToLinkedElement(DataElement de)
    {
        String externalUrl = getExternalUrl(de);
        if(externalUrl == null || !externalUrl.startsWith("de:")) return null;
        return DataElementPath.create(externalUrl.substring(3));
    }

    public static String getExternalUrl(DataElement de)
    {
        try
        {
            DataElementPath dePath = DataElementPath.create(de);
            if( de == null ) return "";
            DataCollection<?> parent = dePath.getParentCollection();
            String urlTemplate = parent.getInfo().getProperties().getProperty(DataCollectionConfigConstants.URL_TEMPLATE);
            if(urlTemplate == null || urlTemplate.equals("referenceType"))
            {
                ReferenceType referenceType = ReferenceTypeRegistry
                        .optReferenceType( parent.getInfo().getProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY ) );
                if(referenceType == null) return "";
                String url = referenceType.getURL(de.getName());
                return url == null ? "" : url;
            }
            urlTemplate = processUrl(urlTemplate);
            if( dePath.toString().toLowerCase().startsWith("databases/transpath") )
            {
                if( de instanceof Base )
                {
                    DynamicPropertySet attr = ( (Base)de ).getAttributes();
                    Object type = attr.getValue("moleculeType");
                    if( type != null && type.toString().endsWith("_mod.") )
                    {
                        Object baseFormProperty = attr.getValue("precursors");
                        if( baseFormProperty != null )
                        {
                            String baseFormId = TextUtil.stream( baseFormProperty ).findFirst().orElse( null );
                            int start = baseFormId.indexOf('<');
                            int end = baseFormId.indexOf('>');

                            String id = start < end ? baseFormId.substring(start + 1, end) : de.getName();
                            return urlTemplate.replaceAll("\\$id\\$", id);
                        }
                    }

                }
            }
            else if( dePath.toString().startsWith("databases/GeneWays/Data/Reaction") )
            {
                if( de instanceof Reaction )
                {
                    DatabaseReference[] refs = ( (Reaction)de ).getDatabaseReferences();
                    List<String> pubmedIds = new ArrayList<>();
                    DatabaseReference pubmedRef = null;
                    if( refs != null && refs.length > 0 )
                    {
                        LinkResolver resolver = new LinkResolver();
                        for( DatabaseReference ref : refs )
                        {
                            if( ref.getDatabaseName().equalsIgnoreCase("PubMed") )
                            {
                                pubmedIds.add(ref.getId());
                                //get url for the first id, then replace it with a list of ids joined with comma
                                if( pubmedRef == null )
                                    pubmedRef = ref;
                            }
                        }
                        if( pubmedIds.size() > 0 && pubmedRef != null )
                        {
                            String url = processUrl(resolver.getQueryById(de, pubmedRef));
                            return url.replace(pubmedRef.getId(), String.join(",", pubmedIds));
                        }
                        return resolver.getQueryById(de, refs[0]); //no pubmed link found
                    }
                }
            }
            Matcher m = ID_PATTERN.matcher(urlTemplate);
            int start = 0;
            while(m.find(start))
            {
                String regExp = m.group(1);
                Matcher m2 = BEAN_FIELD_PATTERN.matcher(regExp);
                String replacement = de.getName();
                if(m2.matches())
                {
                    regExp = m2.group(1);
                    replacement = BeanUtil.getBeanPropertyValue(de, m2.group(2)).toString();
                }
                if(!regExp.isEmpty())
                {
                    String[] regExpFields = regExp.split("/", -1);
                    replacement = replacement.replaceAll(regExpFields[1], regExpFields[2]);
                }
                urlTemplate = urlTemplate.substring(0, m.start())+replacement+urlTemplate.substring(m.end());
                start = m.start()+replacement.length();
                m = ID_PATTERN.matcher(urlTemplate);
            }
            return urlTemplate;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "getExternalURL:", e);
            return "";
        }
    }

    private static String processUrl(String url)
    {
        return url.replaceAll("&eq;", "=");
    }
}
