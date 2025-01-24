/*
 * Stub for correct work of JSPropertyInspector
 */
function getMessage(key)
{
    return key;
}

var topMostWindow = window;
var EMPTY = "";

var SHOW_USUAL = 0;
var SHOW_EXPERT = 1;
var SHOW_HIDDEN = 2;
var SHOW_PREFERRED = 4;

/*
 * Converts JSON to DynamicPropertySet
 */
function convertJSONToDPS(json, prefix)
{
    var beanDPS = new DynamicPropertySet();
    if(prefix == undefined) prefix = "";
    for (var i = 0; i < json.length; i++) 
    {
        var name = json[i].name;
        var displayName = json[i].displayName;
        var dictionary = json[i].dictionary;
        var dictionaryName = dictionary == undefined ? undefined : addDictionary(dictionary);
        var shortDescription = json[i].description;
        var type = json[i].type;
		if (!type) 
        {
            type = "code-string";
        }
        var readOnly = json[i].readOnly;
        var isImplicit = json[i].implicitProperty;
        var value;
        if (type == "composite") 
        {
            value = convertJSONToDPS(json[i].value, prefix+name+"/");
        }
        else 
            if (type == "collection") 
            {
                var listDPS = [];
                for (var ci = 0; ci < json[i].value.length; ci++) 
                {
                    var childDPS = convertJSONToDPS(json[i].value[ci], prefix+name+"/["+ci+"]/");
                    listDPS.push(childDPS);
                }
                value = listDPS;
            }
            else 
				if(type == "bool")
					value = (json[i].value=="true" || json[i].value=="yes")?"yes":"no";
				else
	            {
	                if (type == "color-selector") 
                    {
                        var listDPS = [];
                        listDPS.push(json[i].value[0]);
                        for (var ci = 1; ci < json[i].value.length; ci++) 
                        {
                            var childDPS = convertJSONToDPS(json[i].value[ci], prefix+name+"/["+ci+"]/");
                            listDPS.push(childDPS);
                        }
                        value = listDPS;
                    }
                    else
                    {
                        value = json[i].value;
                    }
	            }
        if (dictionaryName && type=="code-string") type = "generic-combo"; 
		var property = new DynamicProperty(name, type, value);
        property.getDescriptor().setDisplayName(displayName);
        property.getDescriptor().setReadOnly(readOnly);
        property.setAttribute("implicit", isImplicit);
        if(shortDescription != undefined)
        	property.getDescriptor().setShortDescription(shortDescription);
		property.setCanBeNull(json[i].canBeNull?"yes":"no");
        if (dictionaryName) 
        {
            property.setAttribute("dictionaryName", dictionaryName);
        }
        property.setAttribute("completeName", prefix+name);
        if(type == "collection" && json[i].fixedLengthProperty)
            property.setAttribute("fixedLengthProperty", true);
        if(json[i].childClass) json[i].childElementType = json[i].childClass;
        if(json[i].elementClass) json[i].dataElementType = json[i].elementClass;
        var attributeNames = [ "childElementType", "dataElementType", "icon",
				"referenceType", "elementMustExist", "promptOverwrite",
				"multiSelect", "originalName", "parameters", "auto" ];
        for(var nameIndex in attributeNames)
    	{
        	var attributeName = attributeNames[nameIndex];
    		if(json[i][attributeName] !== undefined)
    			property.setAttribute(attributeName, json[i][attributeName]);
    	}
        beanDPS.add(property);
    }
    return beanDPS;
}

function refreshProperties(oldDps, newDps)
{
	
}

/*
 * Global dictionaries object
 */
var dictionaries;

/*
 * Append JSON to Dictionaries
 */
function addDictionary(json)
{
	if(!dictionaries)
	{
		dictionaries = new Dictionaries();
	}
	var dName = rnd();
    var dictionary = new Dictionary(dName);
    for (var n = 0; n < json.length; n++)
    {
    	dictionary.add(json[n][0], json[n][1]);
    }
    dictionaries.addDictionary(dictionary);
    return dName;
}

function convertPropertyToObject(property, lastObj)
{
	if(lastObj == undefined) lastObj = "";
	var lastObjPrefix = lastObj.replace(/\/.+$/, "");
	var lastObjSuffix = lastObj.substring(lastObjPrefix.length+1);
    var jsonProperty = new Object();
    jsonProperty.name = property.getName();
    var type = property.getType();
    if (type == "composite") 
    {
        var prop = property.getValue();
        var properties = prop.getAllProperties();
        var listJSON = [];
        for (var propName in properties) 
        {
    		if(lastObjPrefix == propName) continue;
            listJSON.push(convertPropertyToObject(properties[propName]));
        }
    	if(lastObjPrefix != "" && properties[lastObjPrefix] != undefined)
    		listJSON.push(convertPropertyToObject(properties[lastObjPrefix], lastObjSuffix));
        jsonProperty.value = listJSON;
    }
    else 
        if (type == "collection") 
        {
        	//write array operations if necessary
        	var actionName = property.getDescriptor().getValue("array-action");
        	if(actionName && actionName.length>0)
        	{
        		jsonProperty.action = actionName;
        		delete property.getDescriptor().attributes["array-action"];
        	}
        	
            var listDPS = property.getValue();
            var listJSON = [];
            for (var i = 0; i < listDPS.length; i++) 
            {
                listJSON[i] = [];
                var properties = listDPS[i].getAllProperties();
                if(lastObjPrefix=="["+i+"]")
                {
                    var lastObjPrefixArr = lastObjSuffix.replace(/\/.+$/, "");
                    var lastObjSuffixArr = lastObjSuffix.substring(lastObjPrefixArr.length+1);
                    for (var propName in properties) 
                    {
                        if(lastObjPrefixArr == propName) continue;
                        listJSON[i].push(convertPropertyToObject(properties[propName]));
                    }
                    if(lastObjPrefixArr != "" && properties[lastObjPrefixArr] != undefined)
                        listJSON[i].push(convertPropertyToObject(properties[lastObjPrefixArr], lastObjSuffixArr));
                }
                else
                {
                    for (var propName in properties) 
                    {
                        listJSON[i].push(convertPropertyToObject(properties[propName]));
                    }
                }
            }
            jsonProperty.value = listJSON;
        }
        else
			if(type == "bool")
				jsonProperty.value = property.getValue()=="yes";
			else 
            {
                if (type == "color-selector") 
                {
                    var listDPS = property.getValue();
                    var listJSON = [];
                    listJSON[0] = listDPS[0];
                    for (var i = 1; i < listDPS.length; i++) 
                    {
                        listJSON[i] = [];
                        var properties = listDPS[i].getAllProperties();
                        for (var propName in properties) 
                        {
                            listJSON[i].push(convertPropertyToObject(properties[propName]));
                        }
                    }
                    jsonProperty.value = listJSON;
                }
                else
                {
                    jsonProperty.value = property.getValue();
                }
            }
    if(jsonProperty.value == undefined) jsonProperty.value = null;
	return jsonProperty;
}

/*
 * Converts DynamicPropertySet to JSON
 */
function convertDPSToJSON(dps, lastObj)
{
	if(lastObj instanceof JSControl)
	{
		var model = lastObj.getModel();
		if(model.getDescriptor)
			lastObj = model.getDescriptor().getValue("completeName");
		else
			lastObj = "";
	}
	if(lastObj == undefined) lastObj = "";
	var lastObjPrefix = lastObj.replace(/\/.+$/, "");
	var lastObjSuffix = lastObj.substring(lastObjPrefix.length+1);
    var jsonObj = [];
    var properties = dps.getAllProperties();
    for (var propName in properties) 
    {
		if(lastObjPrefix == propName) continue;
        jsonObj.push(convertPropertyToObject(properties[propName]));
    }
	if(lastObjPrefix != "" && properties[lastObjPrefix] != undefined)
		jsonObj.push(convertPropertyToObject(properties[lastObjPrefix], lastObjSuffix));
    return $.toJSON(jsonObj);
}

// TODO: test (now not used and not tested)
function changedControlsToJSON(pane)
{
    var jsonObj = [];
	
	for(var i=0; i<pane.getChildCount(); i++)
	{
		var control = pane.getChildAt(i);
		if(control.hasChanges())
		{
			control.updateModel();
			jsonObj.push(convertPropertyToObject(control.getModel()));
		}
	}
    return $.toJSON(jsonObj);
}

function getDPSFromTable(table)
{
    var tableDPS = new DynamicPropertySet();
    table.find('input, select').each(function()
    {
        
            var id = $(this).attr('id');
            if(id)
            {
                
                if($(this).hasClass('color-picker-button'))
                {
                    var colorDiv = $(this);//.parent().children('span');
                    var colorValue = colorDiv.css('backgroundColor');
                    if(colorValue){
                        var rgb = colorValue.substring(4, colorValue.length-1).replace(/[^\d,.]/g, '').split(',');
                        
                        value = "[" + rgb[0] + "," + rgb[1]  + "," + rgb[2] + "]";
                    }
                    //var id = colorDiv.attr('id');
                    tableDPS.add(new DynamicProperty(id, "string", value));
                    return;
                }
                        
                
                var controlType = $(this).attr('type');
                if (controlType == "checkbox") 
                {
                    var value = $(this).prop('checked') ? 'checked' : '';
                }
                else 
                {
                    var value = $(this).val();
                }
                tableDPS.add(new DynamicProperty(id, "string", value));
            }
    });
    table.find('button.color-picker-button').each(function()
    {
        var id = $(this).attr('id');
        if(id)
        {
            var colorValue = $(this).css('backgroundColor');
            if(colorValue){
                var rgb = colorValue.substring(4, colorValue.length-1).replace(/[^\d,.]/g, '').split(',');
                value = "1;" + rgb[0] + ";" + rgb[1]  + ";" + rgb[2];
            }
            tableDPS.add(new DynamicProperty(id, "string", value));
            return;
        }
    });
    return tableDPS;
}

function makePropertyInspectorSelectable(dpi, callback)
{
	for(var i=0; i<dpi.getChildCount(); i++)
	{
		(function()
		{
			var control = dpi.getChildAt(i);
			var model = control.getModel();
			$(control.getHTMLNode()).closest("tr").click(function()
			{
				if($(this).hasClass("row_selected"))
				{
					$(this).removeClass("row_selected").siblings().removeClass("row_selected");
					if(callback) callback();
				} else
				{
					$(this).siblings().removeClass("row_selected");
					$(this).addClass("row_selected");
					if(callback) callback(model);
				}
			});
		})();
	}
}

function disableDPI(container)
{
	$(container).css("position", "relative").append($("<div/>").addClass("disableCover"));
}

function enableDPI(container)
{
	$(container).css("position", "").children(".disableCover").remove();
}

function findDPSValue(data, key)
{
    if(typeof(data) === 'string')
        data = JSON.parse(data);
    for(var i=0; i<data.length; i++)
    {
        if(data[i].name === key)
            return data[i].value;
    }
    return null;
}
