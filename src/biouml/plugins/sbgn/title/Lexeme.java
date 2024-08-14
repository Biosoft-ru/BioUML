package biouml.plugins.sbgn.title;


public class Lexeme
{
    private LexemeType type;
    private String text;

    public Lexeme(LexemeType type, String text)
    {
        super();
        this.type = type;
        this.text = text;
    }

    public LexemeType getType()
    {
        return type;
    }

    public String getText()
    {
        return text;
    }
    
    @Override
    public String toString()
    {
        return type+":"+text;
    }
}