package ru.biosoft.proteome.table;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import ru.biosoft.util.Pair;

/**
 * Class for special column type
 * TODO: move this class out of ru.biosoft.table plugin
 */
public class Structure3D
{
    protected List<Pair<String, String>> links;

    public Structure3D()
    {
        this.links = new ArrayList<>();
    }

    public Structure3D(String content)
    {
        this();
        try
        {
            JSONArray jsonArray = new JSONArray(content);
            for( int i = 0; i < jsonArray.length(); i += 2 )
            {
                links.add(new Pair<>(jsonArray.getString(i), jsonArray.getString(i + 1)));
            }
        }
        catch( JSONException e )
        {
        }
    }

    public void addLink(String title, String link)
    {
        links.add(new Pair<>(title, link));
    }

    public int getSize()
    {
        return links.size();
    }

    public Pair<String, String> getLink(int i)
    {
        return links.get(i);
    }

    @Override
    public String toString()
    {
        List<String> result = new ArrayList<>();
        for( Pair<String, String> link : links )
        {
            result.add(link.getFirst());
            result.add(link.getSecond());
        }
        return new JSONArray(result).toString();
    }
}
