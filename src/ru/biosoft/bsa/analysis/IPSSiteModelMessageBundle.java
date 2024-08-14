package ru.biosoft.bsa.analysis;

import java.util.ListResourceBundle;

public class IPSSiteModelMessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        {"CN_CLASS"                         , "IPS site model options"},
        {"CD_CLASS"                         , "IPS site model options"},

        {"PN_NAME"                          , "Name"},
        {"PD_NAME"                          , "Site model name"},

        {"PN_MATRICES"                          , "Matrices"},
        {"PD_MATRICES"                          , "Matrices"},

        {"PN_CRITICAL_IPS"                        , "Critical IPS"},
        {"PD_CRITICAL_IPS"                        , "Critical IPS"},

        {"PN_DISTMIN"                        , "Minimal distance"},
        {"PD_DISTMIN"                        , "Minimal distance between found sites"},

        {"PN_WINDOW"                        , "Window site"},
        {"PD_WINDOW"                        , "Site of the window to calculate frequencies on"},

        {"PN_STRAND"                        , "Site strand"},
        {"PD_STRAND"                        , "Site strand"},
    };
}
