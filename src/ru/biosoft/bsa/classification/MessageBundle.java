package ru.biosoft.bsa.classification;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    /** @pending refine description of class name and number. */
    private final static Object[][] contents =
    {
        {"CN_CLASSIFICATION_UNIT",                  "Classification unit"},
        {"CD_CLASSIFICATION_UNIT",                  "Classification unit."},

        {"PN_CLASSIFICATION_UNIT_NAME",             "Name"},
        {"PD_CLASSIFICATION_UNIT_NAME",             "Classification unit name."},

        {"PN_CLASSIFICATION_UNIT_CLASS_NUMBER",     "Class"},
        {"PD_CLASSIFICATION_UNIT_CLASS_NUMBER",     "Classification unit class number."},

        {"PN_CLASSIFICATION_UNIT_CLASS_NAME",       "Name"},
        {"PD_CLASSIFICATION_UNIT_CLASS_NAME",       "Classification unit class name."},

        {"PN_CLASSIFICATION_UNIT_DESCRIPTION",      "Description"},
        {"PD_CLASSIFICATION_UNIT_DESCRIPTION",      "Classification unit description."},

        {"PN_CLASSIFICATION_UNIT_SIZE",             "Size"},
        {"PD_CLASSIFICATION_UNIT_SIZE",             "Number of subclasses."},
        
        {"PN_CLASSIFICATION_UNIT_ATTRIBUTES",       "Attributes"},
        {"PD_CLASSIFICATION_UNIT_ATTRIBUTES",       "Attributes of classification unit"}
    };
}
