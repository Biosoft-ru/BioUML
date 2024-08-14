/**
 * Common dialogs 
 */

function createConfirmDialog(message, okCallback)
{
    var dialogDiv = $('<div title="'+resources.commonConfirmBoxTitle+'"></div>');
    var buttonNames = [resources.buttonYes, resources.buttonNo];
    var confirmVerb = message.match(/^(.+)\|([\w ]+)$/);
    if(confirmVerb)
    {
    	message = confirmVerb[1];
    	buttonNames[0] = confirmVerb[2];
    	buttonNames[1] = resources.buttonCancel;
    }
    dialogDiv.html($("<p/>").html(message));
    var buttons = {};
    buttons[buttonNames[0]] = function()
    {
        $(this).dialog("close");
        $(this).remove();
		okCallback();
    }; 
    buttons[buttonNames[1]] = function()
    {
        $(this).dialog("close");
        $(this).remove();
    }; 
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 300,
		modal: true,
        buttons: buttons 
    });
    addDialogKeys(dialogDiv, null, buttonNames[0], buttonNames[1]);
    sortButtons(dialogDiv);
    dialogDiv.dialog("open");
    dialogDiv.parent().find(":button:contains('"+buttonNames[0]+"')").focus();
}

function createYesNoConfirmDialog(message, callback)
{
    var dialogDiv = $('<div title="'+resources.commonConfirmBoxTitle+'"></div>');
    dialogDiv.html($("<p/>").html(message));
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 300,
		modal: true,
        buttons: 
        {
            "No": function()
            {
                $(this).dialog("close");
                $(this).remove();
                callback(false);
            },
            "Yes": function()
            {
                $(this).dialog("close");
                $(this).remove();
				callback(true);
            },
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
            }
        }
    });
    dialogDiv.dialog("open");
    addDialogKeys(dialogDiv, null, "Yes");
    sortButtons(dialogDiv);
    dialogDiv.parent().find(":button:contains('Yes')").focus();
}

function createPromptDialog(title, prompt, callback, defaultValue, validateField)
{
    var dialogDiv = $('<div title="'+title+'"></div>');
    if(prompt)
        dialogDiv.html("<p><b>"+prompt+"</b></p>");
    var inputField = $('<input type="text"/>').width(250);
    if(defaultValue !== undefined)
    	inputField.val(defaultValue);
    dialogDiv.append(inputField);
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 300,
        modal: true,
        buttons: 
        {
            "Ok": function()
            {
                if(validateField &&!isDataElementNameValid(inputField.val()))
                {
                    logger.error(resources.dlgOpenSaveErrorInvalidCharactersVerbose);
                    return;
                }
            	callback(inputField.val());
                $(this).dialog("close");
                $(this).remove();
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
    dialogDiv.dialog("open");
    inputField.focus();
    inputField.get(0).select(0, -1);
}

function createTextAreaDialog(title, prompt, callback)
{
    var dialogDiv = $('<div title="'+title+'"></div>');
    dialogDiv.html("<p><b>"+prompt+"</b></p>");
    var inputField = $('<textarea/>').width(300).height(200);
    dialogDiv.append(inputField);
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 320,
        modal: true,
        buttons: 
        {
            "Ok": function()
            {
            	callback(inputField.val());
                $(this).dialog("close");
                $(this).remove();
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
    dialogDiv.dialog("open");
    inputField.focus();
}

/**
 * Dialog with option selector
 */
function createSelectorDialog(title, message, selectedValue, allValues, callback)
{
	var dialogDiv = $('<div title="'+title+'"></div>');
    dialogDiv.html("<p>"+message+"</p>");
    var selectControl = $('<select/>').css('width', 200);
    for(var i=0; i < allValues.length; i++)
    {
        var option = $('<option value="'+allValues[i]+'">' + allValues[i]+'</option>');
        if(allValues[i] == selectedValue)
            option.attr('selected', 'selected');
        selectControl.append(option);    
    }
    dialogDiv.append(selectControl);
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 320,
        modal: true,
        buttons: 
        {
            "Ok": function()
            {
            	callback(selectControl.val());
                $(this).dialog("close");
                $(this).remove();
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
    dialogDiv.dialog("open");
}

function createSelectElementDialog(title, defValue, params, action)
{
    var property = new DynamicProperty("path", "data-element-path", defValue);
    property.getDescriptor().setDisplayName(title);
    property.getDescriptor().setReadOnly(false);
    property.setCanBeNull("no");
    for(var i in params)
    {
    	property.setAttribute(i, params[i]);
    }
    this.pathEditor = new JSDataElementPathEditor(property, null);
    this.pathEditor.setModel(property);
    this.pathEditor.createHTMLNode();
    this.pathEditor.setValue(defValue);
    this.pathEditor.setValue = function(value)
    {
        action(value);
    };
    this.pathEditor.openDialog();
}

function createSaveElementDialog(title, type, defValue, action)
{
	createSelectElementDialog(title, defValue, {dataElementType: type, elementMustExist: false, promptOverwrite: true}, action);
}

function createOpenElementDialog(title, type, defValue, action)
{
	createSelectElementDialog(title, defValue, {dataElementType: type, elementMustExist: true, promptOverwrite: false}, action);
}

function createOpenElementsDialog(title, type, defValue, action)
{
	createSelectElementDialog(title, defValue, {dataElementType: type, elementMustExist: true, promptOverwrite: false, multiSelect: true}, action);
}

function createBeanEditorDialog(title, beanPath, callback, autoUpdate)
{
    var propertyPane = new JSPropertyInspector();
    var parentID = "property_inspector_dialog_" + rnd();
    var origData;
    var wasChanged = false;
    
    queryBean(beanPath, {}, function(data)
    {
        function syncronizeData(control)
        {
            queryBioUML("web/bean/set", 
            {
                de: beanPath,
                json: convertDPSToJSON(propertyPane.getModel(), control)
            }, function(data)
            {
                wasChanged = true;
                $(getJQueryIdSelector(parentID)).empty();
                var beanDPS = convertJSONToDPS(data.values);
                propertyPane = new JSPropertyInspector();
                propertyPane.setParentNodeId(parentID);
                propertyPane.setModel(beanDPS);
                propertyPane.generate();
                propertyPane.addChangeListener(function(control, oldValue, newValue) {
                    syncronizeData(control);
                });
            });
        }
        
        var beanDPS = convertJSONToDPS(data.values);
        origData = convertDPSToJSON(beanDPS);
        var dialogDiv = $('<div title="'+title+'"></div>');
        dialogDiv.append('<div id="' + parentID + '"></div>');
        var closeDialog = function()
        {
            dialogDiv.dialog("close");
            dialogDiv.remove();
        };
        dialogDiv.dialog(
        {
            autoOpen: false,
            width: 500,
            height: 500,
            buttons: 
            {
                "Cancel": function()
                {
                    if(autoUpdate && wasChanged)
                    {
                        queryBioUML("web/bean/set",
                        {
                            de: beanPath,
                            json: origData
                        }, closeDialog, closeDialog);
                    } else closeDialog();
                },
                "Save": function()
                {
                    var data = convertDPSToJSON(propertyPane.getModel());
                    queryBioUML("web/bean/set", 
                    {
                        de: beanPath,
                        json: data
                    }, function()
                    {
                        if(callback) callback(data);
                        closeDialog();
                    }, closeDialog);
                }
            }
        });
        addDialogKeys(dialogDiv);
        sortButtons(dialogDiv);
        dialogDiv.dialog("open");
        
        propertyPane.setParentNodeId(parentID);
        propertyPane.setModel(beanDPS);
        propertyPane.generate();
        if(autoUpdate)
        {
            propertyPane.addChangeListener(function(control, oldValue, newValue) {
                syncronizeData(control);
            });
        }
    });
};


function createBeanEditorDialogWithSelector(title, getBeanPath, callback, autoUpdate, values, selectedValue, onChange)
{
    var propertyPane = new JSPropertyInspector();
    var parentID = "property_inspector_dialog_" + rnd();
    var origData;
    
    var selectControl = $('<select id="beanEditorSelector"></select>').addClass("genericComboContainer").css({"max-width":"475px", "background":"white", "margin":"10px 0"});
    selectControl.children('option').remove();
    for (i = 0; i < values.length; i++) 
    {
        if (values[i].length > 0) 
        {
            var option = $('<option value="'+values[i]+'">' + values[i]+'</option>');
            if(values[i] == selectedValue)
                option.attr('selected', 'selected');
            selectControl.append(option);   
        }
    }
    selectControl.change(function()
    {
        var selectedVal = $(this).val(); 
        onChange(selectedVal, function (){
            reloadBean(getBeanPath(selectedVal));
        });
    });
    
    var dialogDiv = $('<div title="'+title+'"></div>');
    dialogDiv.append(selectControl);
    dialogDiv.append('<div id="' + parentID + '"></div>');
    var closeDialog = function()
    {
        dialogDiv.dialog("close");
        dialogDiv.remove();
    };
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 500,
        height: 500,
        buttons: 
        {
            "Cancel": function()
            {
                if(autoUpdate)
                {
                    var beanPath = getBeanPath(selectControl.val());
                    queryBioUML("web/bean/set",
                    {
                        de: beanPath,
                        json: origData
                    }, closeDialog, closeDialog);
                } else closeDialog();
            },
            "Save": function()
            {
                var beanPath = getBeanPath(selectControl.val());
                var data = convertDPSToJSON(propertyPane.getModel());
                queryBioUML("web/bean/set", 
                {
                    de: beanPath,
                    json: data
                }, function()
                {
                    if(callback) callback(data);
                    closeDialog();
                }, closeDialog);
            }
        }
    });
    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    dialogDiv.dialog("open");
    
    var reloadBean = function(beanPath) 
    {
        queryBean(beanPath, {}, function(data)
        {
            function syncronizeData(control)
            {
                queryBioUML("web/bean/set", 
                {
                    de: beanPath,
                    json: convertDPSToJSON(propertyPane.getModel(), control)
                }, function(data)
                {
                    $(getJQueryIdSelector(parentID)).empty();
                    var beanDPS = convertJSONToDPS(data.values);
                    propertyPane = new JSPropertyInspector();
                    propertyPane.setParentNodeId(parentID);
                    propertyPane.setModel(beanDPS);
                    propertyPane.generate();
                    propertyPane.addChangeListener(function(control, oldValue, newValue) {
                        syncronizeData(control);
                    });
                });
            }
        
            var beanDPS = convertJSONToDPS(data.values);
            origData = convertDPSToJSON(beanDPS);
            $(getJQueryIdSelector(parentID)).empty();
            
            propertyPane.setParentNodeId(parentID);
            propertyPane.setModel(beanDPS);
            propertyPane.generate();
            if(autoUpdate)
            {
                propertyPane.addChangeListener(function(control, oldValue, newValue) {
                    syncronizeData(control);
                });
            }
        });
    };
    
    reloadBean(getBeanPath(selectedValue));
};

