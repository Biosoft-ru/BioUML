package biouml.plugins.pharm.nlme;

import ru.biosoft.plugins.jri.RUtility;

public class RUtils
{
    public String escapeString(String str)
    {
        return "'"+RUtility.escapeRString(str)+"'";
    }
}
