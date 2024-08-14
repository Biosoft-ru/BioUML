package biouml.standard.type;

public interface LocalisedString
{
    /** Returns languages for which value of this string is defined. */
    public String[] getLanguages();

    /** Returns string value for the specified language. */
    public String getValue(String language);

    /** Set up specified string value for the specified language. */
    public void setValue(String value, String language);

    /** Returns string value for the language that is set up as default. */
    public String getValue();

    /** Set up the specified string value for the language that is set up as default. */
    public void setValue(String value);
}
