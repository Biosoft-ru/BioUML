package biouml.plugins.sbml.celldesigner;

/**
 * Bean for reaction species info
 */
public class SpeciesInfo
{
    protected String name;
    protected String alias;
    protected float portX = 0f;
    protected float portY = 0f;

    public SpeciesInfo(String name, String alias)
    {
        this.name = name;
        this.alias = alias;
    }

    public String getName()
    {
        return name;
    }

    public String getAlias()
    {
        return alias;
    }

    public float getPortX()
    {
        return portX;
    }

    public float getPortY()
    {
        return portY;
    }

    /**
     * Fill port info by celldesigner position value.
     * @param positionStr
     */
    public void setPort(String positionStr)
    {
        if( positionStr.equals("N") )
        {
            portX = 0f;
            portY = -0.5f;
        }
        else if( positionStr.equals("NNE") )
        {
            portX = 0.25f;
            portY = -0.5f;
        }
        else if( positionStr.equals("NE") )
        {
            portX = 0.5f;
            portY = -0.5f;
        }
        else if( positionStr.equals("ENE") )
        {
            portX = 0.5f;
            portY = -0.25f;
        }
        else if( positionStr.equals("E") )
        {
            portX = 0.5f;
            portY = 0f;
        }
        else if( positionStr.equals("ESE") )
        {
            portX = 0.5f;
            portY = 0.25f;
        }
        else if( positionStr.equals("SE") )
        {
            portX = 0.5f;
            portY = 0.5f;
        }
        else if( positionStr.equals("SSE") )
        {
            portX = 0.25f;
            portY = 0.5f;
        }
        else if( positionStr.equals("S") )
        {
            portX = 0f;
            portY = 0.5f;
        }
        else if( positionStr.equals("SSW") )
        {
            portX = -0.25f;
            portY = 0.5f;
        }
        else if( positionStr.equals("SW") )
        {
            portX = -0.5f;
            portY = 0.5f;
        }
        else if( positionStr.equals("WSW") )
        {
            portX = -0.5f;
            portY = 0.25f;
        }
        else if( positionStr.equals("W") )
        {
            portX = -0.5f;
            portY = 0f;
        }
        else if( positionStr.equals("WNW") )
        {
            portX = -0.5f;
            portY = -0.25f;
        }
        else if( positionStr.equals("NW") )
        {
            portX = -0.5f;
            portY = -0.5f;
        }
        else if( positionStr.equals("NNW") )
        {
            portX = -0.25f;
            portY = -0.5f;
        }
    }

    @Override
    public String toString()
    {
        return name + " : " + alias + " (" + portX + "," + portY + ")";
    }
}
