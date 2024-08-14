package biouml.plugins.gtrd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;

import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * Hub to match ClassGTRDType to ProteinGTRDType
 * @author lan
 */
public class ClassGTRDHub extends BioHubSupport
{
    protected Logger log = Logger.getLogger(ClassGTRDHub.class.getName());
    private boolean valid = true;
    private static final ReferenceType[] inputTypes = ReferenceTypeRegistry.getReferenceTypes(ClassGTRDType.class);
    private static final ReferenceType[] outputTypes = ReferenceTypeRegistry.getReferenceTypes(ProteinGTRDType.class);

    public ClassGTRDHub(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        return null;
    }

    public Connection getConnection()
    {
        if( !valid )
            return null;
        try
        {
            DataElementPath path = getModulePath();
            DataCollection<?> module = path == null ? null : path.optDataCollection();
            if( module == null )
            {
                log.log(Level.SEVERE, getName()+": no module found (" + path + "); hub is disabled");
                valid = false;
                return null;
            }
            Connection connection = SqlConnectionPool.getConnection(module);
            SqlUtil.checkConnection(connection);
            return connection;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
        return null;
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        return inputTypes.clone();
    }

    @Override
    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        if(inputType.getClass().equals(ClassGTRDType.class)) return outputTypes.clone();
        return null;
    }

    @Override
    public double getMatchingQuality(ReferenceType inputType, ReferenceType outputType)
    {
        if(inputType.getClass().equals(ClassGTRDType.class) && outputType.getClass().equals(ProteinGTRDType.class))
            return 1;
        return 0;
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType,
            Properties properties, FunctionJobControl jobControl)
    {
        Map<String, String[]> result = new HashMap<>();
        try (PreparedStatement ps = getConnection().prepareStatement( "SELECT name FROM classification WHERE name LIKE ?" ))
        {
            for(String input: inputList)
            {
                int nFields = input.split("\\.").length;
                if(nFields == 5) result.put(input, new String[] {input});
                else if(nFields == 6) result.put(input, new String[] {input.substring(0, input.lastIndexOf("."))});
                else
                {
                    ps.setString(1, input+".%");
                    try (ResultSet resultSet = ps.executeQuery())
                    {
                        List<String> list = new ArrayList<>();
                        while(resultSet.next())
                        {
                            list.add(resultSet.getString(1));
                        }
                        result.put(input, list.toArray(new String[list.size()]));
                    }
                }
            }
            return result;
        }
        catch(Exception e)
        {
            if(jobControl != null) jobControl.functionTerminatedByError(e);
            return null;
        }
    }
}
