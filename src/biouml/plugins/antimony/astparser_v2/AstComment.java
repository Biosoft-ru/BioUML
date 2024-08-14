package biouml.plugins.antimony.astparser_v2;

public @SuppressWarnings ( "all" ) class AstComment extends SimpleNode
{
    private String text;

    public AstComment(String text)
    {
        super(AntimonyNotationParserTreeConstants.JJTCOMMENT);
        this.text = text;
    }

    @Override
    public String toAntimonyString()
    {
        return "//" + text;
    }

    public String getText()
    {
        return text;
    }
}