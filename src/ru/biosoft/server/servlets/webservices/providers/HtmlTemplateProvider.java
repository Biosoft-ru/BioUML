package ru.biosoft.server.servlets.webservices.providers;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.HtmlDescribedElement;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.templates.TemplateInfo;
import ru.biosoft.templates.TemplateRegistry;
import ru.biosoft.util.TextUtil2;

/**
 * @author lan
 *
 */
public class HtmlTemplateProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElement de = arguments.getDataElement();
        String templateName = arguments.get("templateName");
        TemplateInfo[] suitableTemplates = TemplateRegistry.getSuitableTemplates(de);
        String html = null;
        if(suitableTemplates.length == 0)
        {
            html = "No template available";
        } else
        {
            if( templateName == null )
            {
                templateName = suitableTemplates[0].getName();
            }
            StringBuffer result = TemplateRegistry.mergeTemplate(de, templateName);
            String baseId = "";
            if( de instanceof HtmlDescribedElement )
            {
                baseId = ( (HtmlDescribedElement)de ).getBaseId() + "/";
            }
            html = result.toString();
            String htmlNoLeading = html.stripLeading();
            if( !htmlNoLeading.startsWith("<html>") )
                html = "<pre>" + html;//non html text should keep its formatting
            html = html.replaceAll( "href=\"de:([^\"]+)\"", "href=\"#de=$1\"" );
            html = html.replaceAll( "<math", "<math displaystyle=\"true\"" );
            html = TextUtil2.processHTMLImages( html, baseId );
        }
        TemplateInfo[] infos = suitableTemplates;
        JSONArray templates = new JSONArray();
        for(TemplateInfo info: infos)
            templates.put(info.getName());
        JSONObject output = new JSONObject();
        output.put("html", html);
        output.put("templates", templates);
        response.sendJSON(output);
    }
}
