$(function() {
    var infoTabs = $("#infoTabs");
    var infoArea = $("#info_area");
    var currentTemplate = {};

    initInfoTabs();
    BioUML.selection.addListener(function(completeName) 
    {
        var dc = getDataCollection(completeName);
        var elementClass = dc.getClass();
        var preferredTemplate = currentTemplate[elementClass];
        dc.getHtml(function(info, templates)
        {
            if(!/^databases\//.test(completeName))
                infoTabs.tabs('option', 'active', 1);
            infoArea.html(info);
            if(!preferredTemplate)
                currentTemplate[elementClass] = templates[0];
            updateInfoToolbar(null, null, completeName, templates);
        }, preferredTemplate);
    });
    
    /*
     * Init info tabs
     */
    function initInfoTabs()
    {
        infoTabs.tabs(
        {
            select: function(event, ui)
            {
                if(ui.panel.id == "info_tab_container")
                    infoTabs.find('.fg-rightcomponent').show();
                else
                    infoTabs.find('.fg-rightcomponent').hide();
            }
        });
        infoTabs.bind("resize", function(event) {
            infoTabs.children('div')
                .width(infoTabs.width())
                .height(infoTabs.height()-infoTabs.children('ul').outerHeight());
        });
        infoTabs.tabs("option", "active", 1);
    }

    /*
     * Update info toolbar actions
     */
    function updateInfoToolbar(node, treeObj, path, templates)
    {
        var nodePath = null;
        if (!node || node == null) 
        {
            nodePath = path;
        }
        else 
        {
            nodePath = getTreeNodePath(node);
        }
        
        infoTabs.find('.fg-rightcomponent').remove();
        var block2 = $('<div class="fg-rightcomponent ui-helper-clearfix"></div>');
        var editAction = new Action();
        editAction.id = "info_edit";
        editAction.label = resources.commonTooltipEditElementInfo;
        editAction.icon = "icons/edit.gif";
        editAction.isVisible = function(path)
        {
            if (BioUML.selection.lastSelected != null) 
            {
                return true;
            }
            return false;
        };
        editAction.doAction = function(path)
        {
            if (BioUML.selection.lastSelected != null) 
            {
                createBeanEditorDialog(resources.commonEditElementInfoTitle, BioUML.selection.lastSelected, function()
                {
                    var dc = getDataCollection(BioUML.selection.lastSelected);
                    dc.invalidateCollection();
                    dc.getHtml(function(info)
                    {
                        infoArea.html(info);
                    });
                    var curDoc = opennedDocuments[activeDocumentId];
                    if(curDoc)
                    {
                        if( curDoc.updateDocumentView &&  BioUML.selection.lastSelected.startsWith(curDoc.completeName) )
                    	{
                        	curDoc.updateDocumentView();
                    	}
                        else if(curDoc.updateItemView)
                        {
                            curDoc.updateItemView(dc);
                        }
                    }
                }, true);
            }
        };
        var action2 = $('<span class="fg-button ui-state-default fg-button-icon-solo  ui-corner-all" title="' + editAction.label + '"><img class="fg-button-icon-span" src="' + editAction.icon + '"></img></span>');
        (function(actionHandler)
        {
            action2.click(function(event)
            {
                actionHandler(nodePath);
            });
        })(editAction.doAction);
        block2.append(action2);
        
        var viewAction = new Action();
        viewAction.id = "info_view";
        viewAction.label = resources.commonTooltipViewElementInfo;
        viewAction.icon = "icons/view.gif";
        viewAction.isVisible = function(path)
        {
            if (BioUML.selection.lastSelected != null) 
            {
                return true;
            }
            return false;
        };
        viewAction.doAction = function(path)
        {
            if (BioUML.selection.lastSelected != null) 
            {
                window.open(appInfo.serverPath+'web/html_page?'+
                        toURI({templateName: templateBox.val(), de: BioUML.selection.lastSelected}));
            }
        };
        var action = $('<span class="fg-button ui-state-default fg-button-icon-solo  ui-corner-all" title="' + viewAction.label + '"><img class="fg-button-icon-span" src="' + viewAction.icon + '"></img></span>');
        (function(actionHandler)
        {
            action.click(function(event)
            {
                actionHandler(nodePath);
            });
        })(viewAction.doAction);
        block2.append(action);
        
        var templateBox = $('<select id="templateComboBox" class="ui-state-default" style="font-size: 10pt;float:right;"></select>');
        templateBox.width(150);
        templateBox.height(24);
        templateBox.children('option').remove();
        if (templates == null) 
        {
            templateBox.append('<option id="Default">Default</option>');
        }
        else
        {
            for (i = 0; i < templates.length; i++) 
            {
                templateBox.append('<option id="' + templates[i] + '">' + templates[i] + '</option>');
            }
            if (BioUML.selection.lastSelected != null) 
            {
                dc = getDataCollection(BioUML.selection.lastSelected);
                var elementClass = dc.getClass();
                var defaultTemplate = perspective.template;
                if(currentTemplate[elementClass] != null && templateBox.find('option[id="'+currentTemplate[elementClass]+'"]').length>0)
                    templateBox.val(currentTemplate[elementClass]);
                else if(defaultTemplate != undefined && templateBox.find('option[id="'+defaultTemplate+'"]').length>0)
                    templateBox.val(defaultTemplate);
            }
        }
        templateBox.change(function()
        {
            if (BioUML.selection.lastSelected != null) 
            {
                dc = getDataCollection(BioUML.selection.lastSelected);
                currentTemplate[dc.getClass()] = templateBox.val();
                dc.getHtml(function(info)
                {
                    infoArea.html(info);
                }, templateBox.val());
            }
        });
        
        block2.append(templateBox);
        infoTabs.find('.ui-tabs-nav').append(block2);
        
        if(infoTabs.tabs('option', 'active') != 1)//show only for Info tab
        {
            block2.hide();
        }
    }
    
    /**
     * Show @info in infoPane and set lastSelection to null to enable re-selection of currently selected element   
     */
    function showSimpleElementInfo(info)
    {
    	BioUML.selection.lastSelected = null;
    	infoTabs.find('.fg-rightcomponent').remove();
    	infoArea.html(info);
    }
    
    window.showSimpleElementInfo = showSimpleElementInfo;
})
