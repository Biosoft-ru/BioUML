package ru.biosoft.plugins.docker;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            {"CN_DOCKER_PLUGIN",         "Docker"},
            {"CD_DOCKER_PLUGIN",         "Docker plugin."}
        };
    }
}

