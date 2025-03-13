var pathfinderObject = {
        isOpenedLinked: false,
        isOpenedShortpath: false,
        highlighted: new Set()
};

var dialogTemplate = $('<div title="Linked elements">'+
        '<div id="pfSelectedElement" style="padding: 10px 0;">Select element on diagram...</div>'+
        '<div id="linkedTabs">'+
        '<ul>'+
        '<li><a href="#reactions_container"><span>Reactions</span></a></li>'+
        '<li><a href="#interactions_container"><span>Interactions</span></a></li>'+
        '<li><a href="#participants_container"><span>Participants</span></a></li>'+
        '</ul>'+
        '<div style="padding: 20px 0;"><button id="pfLoadSelected">Load selected</button></div>'+
        '<div id="reactions_container">'+
        '<div class="linked_elements_table" style="overflow:auto;"></div>'+
        '</div>'+
        '<div id="interactions_container">'+
        '<div class="linked_elements_table" style="overflow:auto;"></div>'+
        '</div>'+
        '<div id="participants_container">'+
        '<div class="linked_elements_table"></div>'+
        '</div>'+
        '</div>'+
        '</div>');

var linkedTabNames = ["Reactions", "Interactions", "Participants"];
var tabTables = [];

function LinkedElementsDialog(diagram)
{      
    if(pathfinderObject.isOpenedLinked)
        return;
    var dialogDiv = dialogTemplate.clone();
    
    for(var i=0; i <linkedTabNames.length; i++)
    {
        var name = linkedTabNames[i];
        dialogDiv.find("#"+name.toLowerCase()+"_container").css({padding:0, overflow:"auto", height: "340px", "border-top":"1px solid #aaa"});
    }
    var listener = new PFEventListener(diagram, dialogDiv);
    addActiveDocumentListener(listener);
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 600,
        minHeight: 200,
        height: 500,
        open: function()
        {
            $(this).css('overflow', 'hidden');
            pathfinderObject.isOpenedLinked = true;
            var tabDiv = dialogDiv.find("#linkedTabs").tabs();
            listener.init();
        },
        beforeclose: function()
        {
            pathfinderObject.isOpenedLinked = false;
            removeActiveDocumentListener(listener);
            tabTables = []
            dialogDiv.find("#linkedTabs").tabs("destroy");
        },
        close: function (event, ui)
        {
            $(this).remove();
        },
        resizeStop: function()
        {
            var baseHeight = $(this).outerHeight()-$(this).find("#pfSelectedElement").outerHeight()-15;
            for(var i=0; i <linkedTabNames.length; i++)
            {
                var name = linkedTabNames[i];
                var containerDiv = $(this).find("#"+name.toLowerCase()+"_container");
                var height = baseHeight-containerDiv.position().top;
                containerDiv.css({height: height + "px"});
            }
        }
    });
    if(!pathfinderObject.isOpenedLinked)
    {
        dialogDiv.dialog("open");
        dialogDiv.dialog("widget").position( {my: "right top", at: "right top", of: "#rightTopPane", offset: "-20 50", collision: "none"});
    }
}

function getActiveDiagram()
{
    if ((activeDocumentId != null) && (opennedDocuments[activeDocumentId] != null) && opennedDocuments[activeDocumentId] instanceof Diagram)
        return opennedDocuments[activeDocumentId];
    else
        return null;
}

function getActiveTabIndex(dlg)
{
    return dlg.find("#linkedTabs").find("ul > li.ui-tabs-active > a").attr("href");
}

function PFEventListener(diagram, dialog)
{
    this.diagram = diagram;
    this.dialog = dialog;
    
    this.init = function()
    {
        this.diagram.addSelectListener(this);
        var dlg = this.dialog;
        var diagram = this.diagram;
        
        dlg.dialog( "option", "title", "Linked elements in " + getElementName(diagram.completeName) );
        dlg.find("#pfLoadSelected").off( "click")
            .on("click", function(){
            var i = dlg.find("#linkedTabs").tabs( "option", "selected" );
            var tableObj = tabTables[i];
            if(tableObj == undefined)
                return;
            var elements = getTableSelectedRows(tableObj).join("\n");
            queryBioUML("web/diagram/add_elements", 
                    {
                        de: diagram.completeName,
                        "elements": elements,
                        resptype: "json"
                    }, function(data)
                    {
                        diagram.update(data);
                    });
        });
        var selection = diagram.getSelection();
        if(selection && selection.length > 0)
            this.nodeSelected(selection[0]);
        else
            this.selectionChanged();
    };
    
    this.activeDocumentChanged = function(document)
    {
        if(this.diagram)
        {
            this.diagram.removeSelectListener(this);
        }
        if(document == null || !(document instanceof Diagram))
            dialog.dialog("close");
        else
        {
            this.diagram = document;
            this.init();
        }
    };

    this.nodeSelected = function(nodePath)
    {
        var nodeDC = new DataCollection(diagram.completeName + "/" + nodePath);
        var dlg = this.dialog;
        var linkedTabs = dlg.find("#linkedTabs");
        
        nodeDC.getBeanFields('title', function(result)
        {
            dlg.find("#pfSelectedElement").html(result.getValue('title')); //TODO: get kernel/attributes/moleculeType 
            linkedTabs.show();
            
        });
        for(var i=0; i <linkedTabNames.length; i++)
        {
            var name = linkedTabNames[i];
            linkedTabs.tabs("select", false);
            loadTableLinked(nodeDC.getName(), name, $("#"+name.toLowerCase()+"_container").find(".linked_elements_table"), _.partial(function (i, table)
                    {
                if(table.find("thead").find("th").length > 1)
                {
                    tabTables[i] = table;
                    linkedTabs.tabs("enable", i);
                    linkedTabs.tabs("select", i);
                    for(var j = 0; j < linkedTabNames.length; j++)
                        if(i != j) linkedTabs.tabs("disable", j);
                }
            }, i));
        }
    };
    
    this.selectionChanged =  function(sel)
    {
        var dlg = this.dialog;
        if(sel == undefined || sel.length == 0)
        {
            dlg.find("#pfSelectedElement").html("Select element on diagram...");
            dlg.find("#linkedTabs").hide();
        }
    };
}

function loadTableLinked(nodePath, tabletype, table, after)
{
    queryBioUML("web/table/sceleton",
    {
        "de": nodePath,
        "type": "linkedelements",
        "tabletype": tabletype
    }, 
    function(data)
    {
        table.html(data.values);
        var tableObj = table.children("table");
        
        var features = 
        {
            "bProcessing": true,
            "bServerSide": true,
            "bFilter": false,
            "iDisplayLength" : 50,
            "sPaginationType": "full_numbers",
            "sDom" : "tlpir",
            "read" : true,
            "ordering": false,
            "info": false,
            "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI({de: nodePath, rnd: rnd(), type: "linkedelements", "tabletype": tabletype, "read" : true}),
        };
        tableObj.addClass('selectable_table');
        tableObj.dataTable(features);
        tableObj.css('width', '100%');
        tableObj.fnSetColumnVis( 0, false);
        mainTableObj = tableObj;
        if(after)
            after(tableObj);
        
    }, function(data)
    {
        table.html("");
        tableObj = null;
    });
}

var shortPathDialogTemplate = $('<div title="Find shortest path between nodes">'+
        '<div style="padding: 10px 0 5px 0; font-weight:bold;">Sources:</div>'+
        '<div id="sources_list"style="width:250px;min-height:50px;border:1px solid #ccc;">'+
        '<div class="placeholder" style="color:#777;">Click on components of the diagram to select sources</div></div>'+
        '<input type="button" id="add_source" value="Add" style="margin:2px 2px 0 0;"/>'+
        '<input type="button" id="remove_source" value="Remove" style="margin:2px 0 0 0;"/>'+
        '<div style="font-weight:bold;padding: 20px 0 5px 0;">Search direction:</div>'+
        '<select id="direction" style="width:250px;">'+
            '<option value="Upstream">Upstream</option>'+
            '<option value="Downstream">Downstream</option>'+
            '<option value="Both" selected>Both</option>'+
        '</select>'+
        '<div style="font-weight:bold;padding: 20px 0 5px 0;">Compute shortest path from sources to:</div>'+
        '<input type="radio" name="searchType" id="ligands" value="Ligands"><label for="ligands">Ligands</label></input><br/>'+
        '<input type="radio" name="searchType" id="receptors" value="Receptors"><label for="receptors">Receptors</label></input><br/>'+
        '<input type="radio" name="searchType" id="tfs" value="Transcription Factors"><label for="tfs">Transcription Factors</label></input><br/>'+
        '<input type="radio" name="searchType" id="targets" value="Targets" checked><label for="targets">Targets</label></input>'+
        '<div id="targets_div">'+
        '<div id="targets_list" style="width:250px;min-height:50px;border:1px solid #ccc;">'+
        '<div class="placeholder" style="color:#777;">Click on components of the diagram to select sources</div></div>'+
        '<input type="button" id="add_target" value="Add" style="margin:2px 2px 0 0;"/>'+
        '<input type="button" id="remove_target" value="Remove" style="margin:2px 0 0 0;"/>'+
        '</div>'+
        '</div>');

function ShortestPathDialog(diagram)
{
    if(pathfinderObject.isOpenedShortpath)
        return;
    var dialogDiv = shortPathDialogTemplate.clone();
    var listener = new shortPathDialogListener(diagram, dialogDiv);
    listener.init();
    diagram.addSelectListener(listener);
    addActiveDocumentListener(listener);
    var sourcesList = dialogDiv.find("#sources_list");
    var targetsList = dialogDiv.find("#targets_list");
    
    dialogDiv.find('input[type="button"]').attr("class", "ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only");
    
    dialogDiv.find('#add_source').click(function() {
        if(listener.selectedNode != undefined)
        {
            if(sourcesList.find("[value='"+getElementName(listener.selectedNode)+"']").length > 0)
                return;
            addSelectableListItem(listener.selectedNode, function(item){
                sourcesList.append(item);
                sourcesList.find(".placeholder").hide();
            });
        }
    });
    
    dialogDiv.find('#remove_source').click(function() {
        sourcesList.find(".genericComboItemSelected").remove();
        showPlaceholder(sourcesList);
    });
    
    dialogDiv.find('#add_target').click(function() {
        if(listener.selectedNode != undefined)
        {
            if(targetsList.find("[value='"+getElementName(listener.selectedNode)+"']").length > 0)
                return;
            addSelectableListItem(listener.selectedNode, function(item){
                targetsList.append(item);
                targetsList.find(".placeholder").hide();
            });
        }
    });
    
    dialogDiv.find('#remove_target').click(function() {
        targetsList.find(".genericComboItemSelected").remove();
        showPlaceholder(targetsList);
    });
    
    dialogDiv.find('input[type="radio"][name=searchType]').change(function() {
        if (this.value == 'Targets')
            $('#targets_div').show();
        else 
            $('#targets_div').hide();
    });
    
    var dialogButtons = {};
    dialogButtons[ "Ok" ] = function()
            {
                var sources = [];
                var cnt = 0;
                dialogDiv.find("#sources_list").find(".genericComboItem").each(function(){
                    sources[cnt++]=$(this).attr("value");
                });
                if(sources.length == 0)
                {
                    logger.error("Please, select source molecules");
                    return;
                }
                var targets = [];
                cnt = 0;
                dialogDiv.find("#targets_list").find(".genericComboItem").each(function(){
                    targets[cnt++]=$(this).attr("value");
                });
                var searchType = dialogDiv.find('input[type="radio"][name=searchType]:checked').val();
                if(searchType == "Targets" && targets.length == 0)
                {
                    logger.error("Please, select target molecules");
                    return;
                }
                var jobID = rnd();
                queryBioUML("web/pathfinder/shortest_path",
                {
                    diagram: listener.diagram.completeName,
                    searchType: searchType,
                    direction: dialogDiv.find("#direction").val(),
                    sources: $.toJSON(sources),
                    targets: $.toJSON(targets),
                    jobID:jobID
                }, 
                function(data)
                {
                    showProgressDialog(jobID, "Computinig shortest path", function() {
                        var currentDiagram = listener.diagram;
                          queryBioUML("web/diagram/refresh", 
                          {
                               type: "json",
                               de: currentDiagram.completeName,
                               scale: currentDiagram.zoom
                          }, 
                          function(diagramdata)
                          {
                              currentDiagram.update(diagramdata);
                          });
                    });
                },
                function(data)
                {
                    logger.error(data.message);
                });
                $(this).dialog("close");
            };
    dialogButtons[ resources.dlgButtonCancel ] = function()
            {
                $(this).dialog("close");
            };
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 350,
        height: 450,
        resizable: false,
        buttons: dialogButtons,
        open: function()
        {
            pathfinderObject.isOpenedShortpath = true;
        },
        beforeclose: function()
        {
            pathfinderObject.isOpenedShortpath = false;
            removeActiveDocumentListener(listener);
            diagram.removeSelectListener(listener);
        },
        close: function (event, ui)
        {
            $(this).remove();
        }
    });
    if(!pathfinderObject.isOpenedShortpath)
    {
        dialogDiv.dialog("open");
        sortButtons(dialogDiv);
    }
}

function shortPathDialogListener(diagram, dialog)
{
    this.diagram = diagram;
    this.dialog = dialog;
    this.selectedNode = undefined;
    
    this.init = function()
    {
        var selection = this.diagram.getSelection();
        if(selection && selection.length > 0)
            this.selectedNode = this.diagram.completeName + "/" + selection[0];
    };
    
    this.nodeSelected = function(nodePath)
    {
        this.selectedNode = this.diagram.completeName + "/" + nodePath;
    };
    
    this.selectionChanged =  function(sel)
    {
        if(sel == undefined || sel.length == 0)
        {
            this.selectedNode = undefined;
        }
    };
    
    this.activeDocumentChanged = function(document)
    {
        if(this.diagram)
        {
            this.diagram.removeSelectListener(this);
        }
        this.dialog.dialog("close");
    };
}

function addSelectableListItem(nodePath, callback)
{
    var nodeDC = new DataCollection(nodePath);
    nodeDC.getBeanFields('title;role/shortName', function(result)
    {
        var role = result.getValue('role');
        if(role.getValue('shortName') == null)
            return;
        var item = $('<div class="genericComboItem" value='+getElementName(nodePath)+'>' + result.getValue('title') + '</div>');
        item.click(function() {
            var currentClass = $(this).attr("class");
            $(this).attr("class", currentClass == "genericComboItem" ? "genericComboItemSelected": "genericComboItem");
        });
        callback(item);
    });
}

function showPlaceholder(control)
{
    if(control.find(".genericComboItem").length == 0)
        control.find(".placeholder").show(); 
}

function highlightOriginalNodes(diagram)
{
    var action;
    if(pathfinderObject.highlighted.has(diagram.completeName))
    {
        pathfinderObject.highlighted.delete(diagram.completeName);
        action = "off";
    }
    else
    {
        pathfinderObject.highlighted.add(diagram.completeName);
        action = "on";
    }
           
    queryBioUML("web/pathfinder/highlight",
    {
        diagram: diagram.completeName,
        highlight: action
    },
    function(data)
    {
        if(data.values == "ok")
        {
            queryBioUML("web/diagram/refresh",
            {
                de: diagram.completeName,
                type: "json"
            }, function(diagramdata)
            {
                diagram.update(diagramdata);
            });
        }
    });
}