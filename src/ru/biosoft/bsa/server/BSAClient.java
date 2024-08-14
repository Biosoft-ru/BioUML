package ru.biosoft.bsa.server;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.Slice;
import ru.biosoft.server.Request;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class BSAClient extends BSAServiceProtocol
{
    protected Logger log;
    protected Request connection;

    public BSAClient(Request conn, Logger log)
    {
        connection = conn;
        this.log = log;
    }

    public void close()
    {
        if( connection != null )
            connection.close();
    }

    public byte[] request(int command, Map<String, String> arguments, boolean readAnswer) throws BiosoftNetworkException
    {
        if( connection != null )
        {
            return connection.request(BSAServiceProtocol.BSA_SERVICE, command, arguments, readAnswer);
        }
        return null;
    }

    public Site[] loadSlice(String completeName, Sequence sequence, String serverSequenceName, Interval interval) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(BSAServiceProtocol.KEY_DE, completeName);
        map.put(BSAServiceProtocol.SEQUENCE_NAME, serverSequenceName);
        map.put(BSAServiceProtocol.FROM, String.valueOf(interval.getFrom()));
        map.put(BSAServiceProtocol.TO, String.valueOf(interval.getTo()));
        byte[] bytes = request(BSAServiceProtocol.DB_TRACK_SITES, map, true);
        if(bytes == null) return new Site[0];
        JSONArray result = new JSONArray(new String(bytes, "UTF-16BE"));
        Map<String, Class<?>> propertyClasses = new HashMap<>();
        Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        JSONObject descriptorsJSON = result.getJSONObject(0);
        Iterator<?> keys = descriptorsJSON.keys();
        while(keys.hasNext())
        {
            String name = keys.next().toString();
            JSONObject descriptorJSON = descriptorsJSON.getJSONObject(name);
            PropertyDescriptor descriptor = StaticDescriptor.create(name, descriptorJSON.getString("displayName"), descriptorJSON.getString("description"), null, true, false);
            descriptors.put(name, descriptor);
            propertyClasses.put( name, ClassLoading.loadClass( descriptorJSON.getString( "class" ) ) );
        }
        JSONArray sitesJSON = result.getJSONArray(1);
        Site[] sites = new Site[sitesJSON.length()];
        for(int i=0; i<sitesJSON.length(); i++)
        {
            JSONArray siteJSON = sitesJSON.getJSONArray(i);
            JSONObject propertiesJSON = siteJSON.getJSONObject(7);
            DynamicPropertySet properties = new DynamicPropertySetAsMap();
            keys = propertiesJSON.keys();
            while(keys.hasNext())
            {
                String name = keys.next().toString();
                properties.add(new DynamicProperty(descriptors.get(name), propertyClasses.get(name), TextUtil.fromString(
                        propertyClasses.get(name), propertiesJSON.getString(name))));
            }
            sites[i] = new SiteImpl(null, siteJSON.getString(0), siteJSON.getString(6), siteJSON.getInt(1), siteJSON.getInt(2), siteJSON.getInt(3), siteJSON.getInt(4), siteJSON.getInt(5), sequence,
                    properties);
        }
        return sites;
    }

    public int calculateSiteCount(String completeName, String serverSequenceName, Interval interval) throws IOException
    {
        Map<String, String> map = new HashMap<>();
        map.put(BSAServiceProtocol.KEY_DE, completeName);
        map.put(BSAServiceProtocol.SEQUENCE_NAME, serverSequenceName);
        map.put(BSAServiceProtocol.FROM, String.valueOf(interval.getFrom()));
        map.put(BSAServiceProtocol.TO, String.valueOf(interval.getTo()));
        byte[] bytes = request(BSAServiceProtocol.DB_TRACK_SITE_COUNT, map, true);
        if( bytes != null )
        {
            return Integer.parseInt(new String(bytes, "UTF-16BE"));
        }
        return -1;
    }

    public int getSequenceStart(String completeName) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(BSAServiceProtocol.KEY_DE, completeName);
        byte[] bytes = request(BSAServiceProtocol.DB_SEQUENCE_START, map, true);
        if( bytes != null )
        {
            return Integer.parseInt(new String(bytes, "UTF-16BE"));
        }
        return 0;
    }

    public int getSequenceLength(String completeName) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(BSAServiceProtocol.KEY_DE, completeName);
        byte[] bytes = request(BSAServiceProtocol.DB_SEQUENCE_LENGTH, map, true);
        if( bytes != null )
        {
            return Integer.parseInt(new String(bytes, "UTF-16BE"));
        }
        return 0;
    }

    public Slice getSlice(String completeName, int pos) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(BSAServiceProtocol.KEY_DE, completeName);
        map.put(BSAServiceProtocol.POSITION, String.valueOf(pos));
        byte[] bytes = request(BSAServiceProtocol.DB_SEQUENCE_PART, map, true);
        if( bytes == null ) throw new Exception("Unable to get server response");
        String[] answer = new String(bytes, "UTF-16BE").split(":");
        if( answer.length != 3 )
            throw new IllegalArgumentException("Invalid answer from the server");
        Slice slice = new Slice();
        slice.from = Integer.parseInt(answer[0]);
        slice.to = Integer.parseInt(answer[1]);
        slice.data = answer[2].getBytes();
        return slice;
    }
}
