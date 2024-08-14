package biouml.plugins.antimony.astparser;

public @SuppressWarnings ( "all" )
class AstComment extends SimpleNode
{
    private String text;

    public AstComment(String text)
    {
        super(AntimonyParserTreeConstants.JJTCOMMENT);
        this.text = text;
    }

    @Override
    public String toAntimonyString()
    {
        return "//"+text;
    }
}