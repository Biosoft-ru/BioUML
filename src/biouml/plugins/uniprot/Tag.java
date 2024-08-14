package biouml.plugins.uniprot;

public class Tag
{
    private String id = EMPTY;
    private String description = EMPTY;
    private String comment = EMPTY;
    private String quantity = OPTIONAL;

    public static final String EMPTY = "";

    // Quantity constants.
    public static final String OPTIONAL = "*";
    public static final String ONCE = "1";
    public static final String TWICE = "2";
    public static final String THRICE = "3";
    public static final String ONCE_OR_MORE = "+";

    public Tag(String description, String quantity, String comment)
    {
        this.description = description;
        this.quantity = quantity;
        this.comment = comment;
    }
    public Tag(String description, String quantity)
    {
        this.description = description;
        this.quantity = quantity;
    }
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        if( null == comment )
            this.comment = EMPTY;
        else
            this.comment = comment;
    }
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        if( null == description )
            this.description = EMPTY;
        else
            this.description = description;
    }
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        if( null == id )
            this.id = EMPTY;
        else
            this.id = id;
    }
    public String getQuantity()
    {
        return quantity;
    }
    public void setQuantity(String quantity)
    {
        if( null == quantity )
            this.quantity = OPTIONAL;
        else
            this.quantity = quantity;
    }
}
