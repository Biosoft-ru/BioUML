package ru.biosoft.access.security;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    
    private final static Object[][] contents =
    {
        {"CN_CLIENT_DC", "Client Data Collection"},
        {"CD_CLIENT_DC", "Client Data Collection"},
        
        { "USE_SSO_TEXT",             "Use Single Sign On"},
        { "CREATE_USER_TEXT",         "Create new user:"},
        { "USERNAME_TEXT",            "User name"},
        { "PASSWORD_TEXT",            "Password"},
        { "CONFIRM_PASSWORD_TEXT",    "Confirm password"},
        
        { "PASSWORD_ERROR",           "Confirm password value must be the same as password value"},
        { "USERNAME_ERROR",           "User with the same name already exists"}
    };
}
