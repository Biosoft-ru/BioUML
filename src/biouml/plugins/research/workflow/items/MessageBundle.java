package biouml.plugins.research.workflow.items;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable th )
        {

        }
        return key;
    }

    private final static Object[][] contents =
    {
        {"CN_ITEM"               , "Item"},
        {"CD_ITEM"               , "Item"},
        {"CN_VARIABLE"               , "Variable"},
        {"CD_VARIABLE"               , "Variable"},
        {"CN_PARAMETER"               , "Parameter"},
        {"CD_PARAMETER"          , "Parameter"},
        {"CN_PARAMETERS"               , "Workflow parameters"},
        {"CD_PARAMETERS"          , "Workflow parameters"},
        {"CN_CALCULATED"         , "Computed variable"},
        {"CD_CALCULATED"         , "Computed variable"},
        {"CN_EXPRESSION"               , "Expression"},
        {"CD_EXPRESSION"          , "Expression"},
        {"CN_CYCLE_VARIABLE"               , "Cycle variable"},
        {"CD_CYCLE_VARIABLE"               , "Cycle variable"},
        {"PN_EXPRESSION"               , "Expression"},
        {"PD_EXPRESSION"          , "Use $var$ to refer to other expressions or parameters"},
        {"PN_PARALLEL", "Parallel"},
        {"PD_PARALLEL", "Run cycle iterations in parallel"},
        {"PN_NAME"               , "Name"},
        {"PD_NAME"               , "Name"},
        {"PN_TYPE"               , "Type"},
        {"PD_TYPE"               , "Type"},
        {"PN_VALUE"              , "Value"},
        {"PD_VALUE"              , "Value"},
        {"PN_RANK"               , "Rank (sort order)"},
        {"PD_RANK"               , "Lowest ranked parameters will be displayed on top"},
        {"PN_ROLE"               , "Role"},
        {"PD_ROLE"               , "Parameter role"},
        {"PN_DEFAULT_VALUE"      , "Default value"},
        {"PD_DEFAULT_VALUE"      , "Parameter default value"},
        {"PN_DESCRIPTION"      , "Description"},
        {"PD_DESCRIPTION"      , "Will be displayed as tooltip"},
        {"PN_CYCLE_TYPE"      , "Cycle type"},
        {"PD_CYCLE_TYPE"      , "Way of iterating over the cycle"},
        {"PN_DROP_DOWN_OPTIONS"      , "Drop-down options"},
        {"PD_DROP_DOWN_OPTIONS"      , "Select a way to fill drop-down options for this parameter"},
        {"PN_DROP_DOWN_OPTIONS_EXPRESSION"      , "Options expression"},
        {"PD_DROP_DOWN_OPTIONS_EXPRESSION"      , "Input expression for selected drop-down options type"},
        {"PN_DATA_ELEMENT_TYPE"      , "Element type"},
        {"PD_DATA_ELEMENT_TYPE"      , "Allowed type of the elements"},
        {"PN_REFERENCE_TYPE"      , "Reference type"},
        {"PD_REFERENCE_TYPE"      , "Allowed type of the references"},
    };
}
