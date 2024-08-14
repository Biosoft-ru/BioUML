package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName ( "User parameter" )
@PropertyDescription ( "User variables common for model." )
public class UserParameters implements Cloneable
{
    private UserParameter[] parameters = new UserParameter[0];

    @PropertyName ( "Parameters" )
    public UserParameter[] getParameters()
    {
        return parameters;
    }
    public void setParameters(UserParameter[] parameters)
    {
        this.parameters = parameters;
    }

    public void addUserParameter(UserParameter parameter)
    {
        UserParameter[] newParameters = new UserParameter[parameters.length + 1];
        System.arraycopy( parameters, 0, newParameters, 0, parameters.length );
        newParameters[parameters.length] = parameter;
        setParameters( newParameters );
    }

    @Override
    public UserParameters clone()
    {
        try
        {
            return (UserParameters)super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            e.printStackTrace();
            return null;
        }
    }
}