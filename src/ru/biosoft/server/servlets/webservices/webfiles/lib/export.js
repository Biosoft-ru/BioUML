function getExportParameters(de, type, exporterName, params, callback)
{
	queryBioUML("web/export", 
    {
		type: "deParams",
		detype: type,
		de: de,
		exporter: exporterName,
        json: (params==null?"":params)
    }, function(data)
    {
    	callback(data);
    }, function(data)
    {
    	callback(null);
    });
};
/*
 * Select exporter format 
 */
function exportElement(de, type, extraParameters)
{
    queryBioUML("web/export",
    {
        de: de,
        type: "deInfo",
        detype: type
    }, function(data)
    {
        var dialogDiv = $('<div title="'+resources.dlgExportTitle+'">'+resources.dlgExportFormat+' <select width="100%" id="formats"></select><br></div>');
        var formats = dialogDiv.find('#formats');
        var splitStr = data.values;
        var numExporters = 0;
        for (i = 0; i < splitStr.length; i++)
        {
            if (splitStr[i].length > 0)
            {
                formats.append('<option value="' + splitStr[i] + '">' + splitStr[i] + '</option>');
                numExporters++;
            }
        }
        if (numExporters == 1) 
        {
            doExport(de, type, formats.val(), extraParameters);
        }
        else 
        {
            dialogDiv.dialog({
                autoOpen: true,
                width: 300,
                height: 120,
                modal: true,
                buttons: {
                    "Ok": function()
                    {
                        var __this = $(this);
                        doExport(de, type, formats.val(), extraParameters, __this);
                    },
                    "Cancel": function()
                    {
                        $(this).dialog("close");
                        $(this).remove();
                    }
                }
            });
            addDialogKeys(dialogDiv);
            sortButtons(dialogDiv);
        }
    });
};

/*
 * Export element with selected exporter
 */
function doExport(de, type, exporterName, extraParameters, __this)
{
	var updatedParameters = null;
	var exportFunction = function()
			{
				var form = toForm(appInfo.serverPath+'web/export',{
					type: "de",
					detype: type,
					de: de,
					exporter: exporterName,
					parameters: updatedParameters,
					rnd: rnd()
				}).attr("target", "_blank");
				$('body').append(form);
				form.get(0).submit();
				form.remove();
                if (__this) 
                {
                    $(__this).dialog("close");
                    $(__this).remove();
                }
			};

	var propertyPane = new JSPropertyInspector();
    var setParameters = function(exportParameters) {
		if(exportParameters != null)
		{
	        var beanDPS = convertJSONToDPS(exportParameters.values);
			if(extraParameters != undefined)
			{
				for(var parameter in extraParameters)
				{
					if(beanDPS.getProperty(parameter))
					{
						beanDPS.setValue(parameter, extraParameters[parameter]);
					}
				}
			}
            propertiesDialogDiv.empty();
	        propertyPane = new JSPropertyInspector();
	        propertyPane.setParentNodeId(propertiesDialogDiv.attr('id'));
	        propertyPane.setModel(beanDPS);
	        propertyPane.generate();
            propertyPane.addChangeListener(function(){
               propertyPane.updateModel();
               updatedParameters = convertDPSToJSON(propertyPane.getModel());
               getExportParameters(de, type, exporterName, updatedParameters, setParameters);
            });
		} else {
			exportFunction();
			$(propertiesDialog).dialog("close");
            $(propertiesDialog).remove();
		}
    };
    var propertiesDialog;
    var propertiesDialogDiv = $('<div title="'+resources.dlgExportPropertiesTitle+" ("+exporterName+")"+'" id="export-properties"></div>');
    propertiesDialogDiv.dialog(
    {
        autoOpen: true,
        width: 500,
        height: 300,
		modal: true,
		open: function()
		{
			propertiesDialog = this;
	      getExportParameters(de, type, exporterName, null, setParameters);
		},
        buttons:
        {
            "Ok": function()
            {
                $(this).dialog("close");
                $(this).remove();
				updatedParameters = convertDPSToJSON(propertyPane.getModel());
				exportFunction();
			},
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
            }
		}
	});
}
