package ru.biosoft.analysiscore;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private final static Object[][] contents = {{"CN_CLASS", "Parameters"}, {"CD_CLASS", "Parameters"}, {"PN_METHOD_NAME", "Method name"},
            {"PD_METHOD_NAME", "Method name"}, {"PN_METHOD_DESCRIPTION", "Method description"},
            {"PD_METHOD_DESCRIPTION", "Method description"}, {"PN_METHOD_SHORT_DESCRIPTION", "Short description"},
            {"PD_METHOD_SHORT_DESCRIPTION", "Short description"},

            //Import
            {"PN_IMPORT_FILE", "File to import"}, {"PD_IMPORT_FILE", "Specify file from your computer"},
            {"PN_IMPORT_RESULT_PATH", "Target"}, {"PD_IMPORT_RESULT_PATH", "Path in repository to put imported file to"},
            {"PN_IMPORT_PROPERTIES", "Properties"}, {"PD_IMPORT_PROPERTIES", "Additional importer parameters"},

            {"PN_EXPERIMENTS_COUNT", "Number of experiments"}, {"PD_EXPERIMENTS_COUNT", "Number of experiments"},
            {"PN_EXPERIMENTS", "Experiments"}, {"PD_EXPERIMENTS", "Experiments"},
    };
}
