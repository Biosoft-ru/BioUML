package ru.biosoft.bsa.view;

import java.util.Collection;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import one.util.streamex.StreamEx;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;

public class TaggedCompositeView extends CompositeView {
	
	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject result = super.toJSON();
		if(tag != null) result.put("tag", tag);
		return result;
	}

	@Override
	public JSONObject toJSONIfChanged(View v) throws JSONException {
		JSONObject result = super.toJSONIfChanged(v);
		if(tag != null) result.put("tag", tag);
		return result;
	}

	@Override
	protected void initFromJSON(JSONObject from) {
		super.initFromJSON(from);
		try
		{
			tag = from.optString("tag", null);
		}
		catch( JSONException e )
		{
			
		}
	}
	private String tag;
	public void setTag(String tag)
    {
		this.tag = tag;
    }
	
	public String getTag()
	{
		return tag;
	}
	
	public Collection<String> setTags (ViewTagger tagger)
    {
    	Set<String> result = StreamEx.of(children).select( TaggedCompositeView.class ).flatMap( child -> child.setTags( tagger ).stream() ).toSet();
        if(getModel() != null)
        {
            String tag = tagger.getTag(getModel());
            if(tag != null)
            {
                setTag(tag);
                result.add(tag);
            }
        }
        return result;
    }
}
