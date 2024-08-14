package biouml.plugins.lucene;

import java.io.Serializable;

public class Formatter implements Serializable
{
    String prefix;
    String postfix;
    public Formatter(String htmlPrefix, String htmlPostfix)
    {
        prefix = htmlPrefix;
        postfix = htmlPostfix;
    }
    public String getPrefix()
    {
        return "" + prefix;
    }
    public String getPostfix()
    {
        return "" + postfix;
    }
}