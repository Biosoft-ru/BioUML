package biouml.standard;

import java.util.ListResourceBundle;

/**
 * Created by IntelliJ IDEA.
 * User: artem
 * Date: 07.04.2004
 * Time: 11:16:34
 * To change this template use Options | File Templates.
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private static final Object[][] contents =
    {
        {"CN_PATHWAY_QUERY_OPTIONS", "Pathway query options"},
        {"CD_PATHWAY_QUERY_OPTIONS", "Pathway query options"},
        {"PN_PATHWAY_QUERY_OPTIONS_INCLUDE_SMALL_MOLECULES", "Include small molecules"},
        {"PD_PATHWAY_QUERY_OPTIONS_INCLUDE_SMALL_MOLECULES", "Include small molequles in search result."}
    };
}
