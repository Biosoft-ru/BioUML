package ru.biosoft.treetable;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //----- ViewModel constants -----------------------------------------/
            {"CN_VIEWMODEL",                "View"},
            {"CD_VIEWMODEL",                "Model for treetable view"},
        
            {"PN_VIEWMODEL_TREE",           "Tree"},
            {"PD_VIEWMODEL_TREE",           "Tree"},
            
            {"PN_VIEWMODEL_TABLE",          "Table"},
            {"PD_VIEWMODEL_TABLE",          "Table"}
        };
    }
}

