package ru.biosoft.galaxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Special GalaxyMethodInfo for DataSource tools
 * @author lan
 */
@ClassIcon( "resources/data-source.gif" )
@PropertyName ("galaxy data source method")
public class DataSourceMethodInfo extends GalaxyMethodInfo
{
    protected List<TranslationRule> translationRules;
    protected String action;
    protected String urlTitle = "Go to the external database";

    /**
     * @param name
     * @param title
     * @param description
     * @param parent
     */
    public DataSourceMethodInfo(String name, String title, String description, DataCollection parent)
    {
        super(name, title, description, parent);
        clazz = DataSourceStubMethod.class;
    }

    void addTranslationRule(TranslationRule rule)
    {
        if(translationRules == null) translationRules = new ArrayList<>();
        translationRules.add(rule);
    }

    public Map<String, String> translateParameters(Map<String, String> parameters)
    {
        Map<String, String> result = new HashMap<>(parameters);
        if(translationRules != null)
        {
            for(TranslationRule rule: translationRules)
            {
                result.put(rule.getLocalName(), rule.translate(parameters.get(rule.getRemoteName()), result));
            }
        }
        return result;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getAction()
    {
        return action;
    }

    public String getUrlTitle()
    {
        return urlTitle;
    }

    public void setUrlTitle(String urlTitle)
    {
        this.urlTitle = urlTitle;
    }
}
