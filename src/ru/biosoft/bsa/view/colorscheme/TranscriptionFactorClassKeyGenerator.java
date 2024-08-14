package ru.biosoft.bsa.view.colorscheme;

//////////
//TODO: Use MatchMethodInfo after it will be refactored.
//import de.biobase.matchpro.analysis.MatchMethodInfo;
//////////
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.TransfacTranscriptionFactor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.classification.ClassificationUnit;

/**
 * TranscriptionFactorKeyGenerator - keys are generating using information of name field in site.
 *
 * This KeyGenerator uses cash to optimise key getting.
 * The key is TranscriptionFactor or WeightMatrixName.

 * @pending
 * its appear that several matrixes with the same name will be put in cash.
 * What happens: do we have several instances with the same name?
 */
public class TranscriptionFactorClassKeyGenerator extends SiteToKeyGenerator
{
    protected static final Logger cat = Logger.getLogger(TranscriptionFactorClassKeyGenerator.class.getName());
    public static final String UNCLASSIFIED = "unclassified";

    static final Hashtable keys = new Hashtable();

    /**
    * Is the <CODE>site</CODE> suitable for this generator
    * @return true if a site is suitable for this generator
    */
    @Override
    public boolean isSuitable(Site site)
    {
        String type = site.getType();
        if(type != null && type.equals(SiteType.TYPE_TRANSCRIPTION_FACTOR ))
            return true;

        return false;
    }

    /**
    * Get key for site
    * @param site a site
    * @return key
    *
    */
    @Override
    public String getKey(Site site)
    {
        String key = UNCLASSIFIED;

        if(site.getProperties() != null)
        {
            DynamicProperty matrix = site.getProperties().getProperty("siteModel");
            if(matrix != null && matrix.getValue() instanceof FrequencyMatrix)
                key = getKey((FrequencyMatrix)(matrix.getValue()));
        }
        /*TranscriptionFactor tf = site.getProperties().getTranscriptionFactor();
        if(tf!= null)
        {
            key = keys.contains(tf) ? (String)keys.get(tf) : createKey(tf);
        }
        else
        {
            MethodInfo mi = site.getProperties().getMethodInfo();
            //////////
            //TODO: Use MatchMethodInfo after it will be refactored.
            //if(mi instanceof MatchMethodInfo)
            //{
            //    MatchMethodInfo mmi = (MatchMethodInfo)mi;
            //    WeightMatrix matrix = mmi.getWeightMatrix();
            //    key = getKey(matrix);
            //}
            //////////
        }*/

        return key;
    }

    /**
     * @todo implement this brunch
     */
    protected String createKey(TransfacTranscriptionFactor tf)
    {
        return UNCLASSIFIED;
    }

    protected String getKey(FrequencyMatrix matrix)
    {
        String key = UNCLASSIFIED;
        try
        {
            String name = matrix.getName();
            key = (String)keys.get(name);

            if(key != null)
                return key;

            key = UNCLASSIFIED;
            BindingElement be = matrix.getBindingElement();
            if( be!=null )
            {
                String commonName = null;
                for(TranscriptionFactor tf: be)
                {
                    if(tf instanceof TransfacTranscriptionFactor)
                    {
                        String fullName = ((TransfacTranscriptionFactor)tf).getGeneralClassPath().toString();

                        if(fullName != null)
                            commonName = (commonName == null) ? fullName : ApplicationUtils.getCommonParent(commonName, fullName);
                    }
                }

                if(commonName != null)
                {
                    DataElement de = CollectionFactory.getDataElement( commonName ) ;
                    if(de instanceof ClassificationUnit)
                    {
                        key = getClassCompleteName((ClassificationUnit)de, ';');
                    }
                }
            }
            keys.put(name, key);
        }
        catch (Exception e)
        {
            cat.log(Level.SEVERE, "Getting key for matrix: " + matrix, e);
        }
        return key;
    }

    /**
     * Returns complete name of classification unit.
     */
    public static String getClassCompleteName(ClassificationUnit unit, char delimiter)
    {
        StringBuffer buffer = new StringBuffer();
        ClassificationUnit parent = unit;
        ClassificationUnit root;
    
        while( true )
        {
            buffer.insert(0, delimiter);
            buffer.insert(0, parent.getClassName());
    
            parent = parent.getParent();
            if( parent == null )
                break;
            root = parent.getParent();
            if( root == null )
                break;
        }
    
        return buffer.toString();
    }
}

