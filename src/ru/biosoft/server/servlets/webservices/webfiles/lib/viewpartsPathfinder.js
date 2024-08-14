/**
 * Pathfinder viewparts
 * @author anna
 */

function initPathfinderViewParts()
{
    if(!viewPartsInitialized['pathfinder'])
    {
        var startIndex = viewPartsInitialized['diagram'] ? 3 : 0;
        var vp = new LinkedElementsViewPart();
        vp.init();
        var vp2 = new ShortestPathViewPart();
        vp2.init();
        var vp3 = new AssociationViewPart();
        vp3.init();
        var vp4 = new LegendViewpart();
        vp4.init();
        viewParts.splice(startIndex, 0, vp4, vp, vp2, vp3)
        viewPartsInitialized['pathfinder'] = true;
    }
    updateViewParts();
}

/*
 * Pathfinder linked elements
 */
function LinkedElementsViewPart()
{
    this.tabId = "pathfinder.linked";
    this.tabName = "Linked elements";
    this.linkedTabNames = ["Reactions", "Participants"];
    this.tabTables = [];
    this.diagram = null;
    var _this = this;

    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId).addClass("viewPartTab");
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.containerDiv.html('<div id="pfSelectedElement" style="padding: 10px 0;">Select element on diagram...</div>'+
        '<div id="linkedTabs">'+
        '<ul>'+
        '<li><a href="#reactions_container"><span>Reactions</span></a></li>'+
        '<li><a href="#participants_container"><span>Participants</span></a></li>'+
        '</ul>'+
        '<div id="reactions_container" class="floating_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
        '<div class="linked_elements_table" ></div>'+
        '</div>'+
        '<div id="participants_container" class="floating_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
        '<div class="linked_elements_table"></div>'+
        '</div>'+
        '</div>');

        var tabDiv = this.containerDiv.find("#linkedTabs");
        
        tabDiv.addClass( "ui-tabs-vertical ui-helper-clearfix" );
        tabDiv.css("width","auto");
        tabDiv.find("ul").css({"float":"left","border-right":0});
        
        tabDiv.find(".floating_container").css({float:"left", "max-width":"600px"});
        tabDiv.find( "li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
        
        _.bindAll(this, _.functions(this));
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if( perspective && perspective.name && perspective.name.startsWith("PathFinder") &&
                (documentObject != null) && documentObject instanceof Diagram )
           callback(true);       
        else 
            callback(false);
    };

    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            var newDiagram = documentObject.getDiagram();
            if(this.diagram != newDiagram)
            {
                if(this.diagram != null)
                    this.diagram.removeSelectListener(this);
                this.diagram = newDiagram;
                this.diagram.addSelectListener(this);
            }
        }
    };
    
    this.show = function()
    {
        if(!this.diagram)
            return;
        this.diagram.addSelectListener(this);
        var tabs = this.containerDiv.find("#linkedTabs").tabs();
        var selection = this.diagram.getSelection();
        if(selection && selection.length > 0)
            this.nodeSelected(selection[0]);
        else
            this.selectionChanged();
    };

    this.save = function()
    {
        this.diagram.removeSelectListener(this);
        this.containerDiv.find("#linkedTabs").tabs("destroy");
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.loadAction = createToolbarButton("Load selected", "applyLayout2.gif", this.loadActionClick);
        toolbarBlock.append(this.loadAction);
    };
    
    /*
     * Load button action handler
     */
    this.loadActionClick = function()
    {
        var i = this.containerDiv.find("#linkedTabs").tabs( "option", "selected" );
        var tableObj = this.tabTables[i];
        if(tableObj == undefined)
            return;
        var elements = getTableSelectedRows(tableObj).join("\n");
        queryBioUML("web/diagram/add_elements", 
                {
                    de: _this.diagram.completeName,
                    "elements": elements,
                    "parent": getElementPath(this.selection),
                    resptype: "json"
                }, function(data)
                {
                    _this.diagram.update(data, true);
                    var selection = _this.diagram.getSelection();
                    if(selection && selection.length > 0)
                        _this.nodeSelected(selection[0]);
                });
    };
    
    this.nodeSelected = function(nodePath)
    {
        var nodeDC = new DataCollection(this.diagram.completeName + "/" + nodePath);
        var dlg = this.tabDiv;
        var linkedTabs = dlg.find("#linkedTabs");
        
        nodeDC.getBeanFields('title', function(result)
        {
            dlg.find("#pfSelectedElement").html(result.getValue('title')); //TODO: get kernel/attributes/moleculeType 
            linkedTabs.show();
            
        });
        
        for(var i=0; i <this.linkedTabNames.length; i++)
        {
            var name = this.linkedTabNames[i];
            linkedTabs.tabs("select", false);
            this.loadTable(nodeDC.getName(), name, $("#"+name.toLowerCase()+"_container").find(".linked_elements_table"), _.partial(function (i, table)
            {
                if(table.find("thead").find("th").length > 1)
                {
                    _this.tabTables[i] = table;
                    linkedTabs.tabs("enable", i);
                    linkedTabs.tabs("select", i);
                    for(var j = 0; j < _this.linkedTabNames.length; j++)
                        if(i != j) linkedTabs.tabs("disable", j);
                }
            }, i));
        }
    };
    
    this.selectionChanged =  function(sel)
    {
        var dlg = this.tabDiv;
        if(sel == undefined || sel.length == 0)
        {
            dlg.find("#pfSelectedElement").html("Select element on diagram...");
            dlg.find("#linkedTabs").hide();
            this.tabTables = [];
        }
    };
    
    this.loadTable = function(nodePath, tabletype, table, after)
    {
        this.selection = nodePath;
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
}

function ShortestPathViewPart ()
{
    this.tabId = "pathfinder.shortpath";
    this.tabName = "Find shortest path";
    this.diagram = null;
    this.tableObj = null;
    this.species = null;
    var _this = this;
    
    
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId).addClass("viewPartTab");
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.containerDiv.html(
                '<div id="shortestpath_parameters" style="float:left; overflow:hidden; ">'+
                '<div style="font-weight:bold; text-align:center;">Find shortest path</div>'+
                '<div style="float:left;">'+
                '<div style="padding: 10px 0 5px 0; font-weight:bold;">From:</div>'+
                '<div id="sources_list"style="width:200px;min-height:50px;border:1px solid #ccc;">'+
                '<div class="placeholder" style="color:#777;">Click on components of the diagram to select sources</div></div>'+
                '<input type="button" id="add_source" value="Add" style="margin:2px 2px 0 0;"/>'+
                '<input type="button" id="remove_source" value="Remove" style="margin:2px 0 0 0;"/>'+
                '</div>'+
                '<div style="float:left; padding-left: 30px;">'+
                    '<div style="padding: 10px 0 5px 0; font-weight:bold;">To:</div>'+
                    '<input type="radio" name="searchType" id="targets" value="Targets" checked><label for="targets">Targets</label></input>'+
                    '<div id="targets_div">'+
                    '<div id="targets_list" style="width:200px;min-height:50px;border:1px solid #ccc;">'+
                    '<div class="placeholder" style="color:#777;">Click on components of the diagram to select sources</div></div>'+
                    '<input type="button" id="add_target" value="Add" style="margin:2px 2px 0 0;"/>'+
                    '<input type="button" id="remove_target" value="Remove" style="margin:2px 0 0 0;"/>'+
                    '</div><br/>'+
                    '<input type="radio" name="searchType" id="ligands" value="Ligands"><label for="ligands">Ligands</label></input><br/>'+
                    '<input type="radio" name="searchType" id="receptors" value="Receptors"><label for="receptors">Receptors</label></input><br/>'+
                    '<input type="radio" name="searchType" id="tfs" value="Transcription Factors"><label for="tfs">Transcription Factors</label></input><br/>'+
                '</div>'+
                '<div style="float:left; clear:both;">'+
                '<div style="font-weight:bold; padding: 10px 0 5px 0;">Search direction:</div>'+
                '<select id="shp_direction" style="width:200px;">'+
                    '<option value="Upstream">Upstream</option>'+
                    '<option value="Downstream">Downstream</option>'+
                    '<option value="Both" selected>Both</option>'+
                '</select>'+
                '<div style="font-weight:bold;padding: 10px 0 5px 0;">Species:</div>'+
                '<select id="shp_species" style="width:200px;">'+
                '</select></div>'+
                '</div>'+
                '</div>'+
                '<div id="progressDiv" style="float:left; padding: 10px 0 0 30px;"><div id="progress_message" style="padding-bottom: 5px;">Computing shortest path...</div></div>'+
                '<div id="shortestpath_results" style="float:left; padding: 10px 0 0 30px; width:400px;">'+
                '<div style="font-weight:bold; text-align:center; padding-bottom: 10px;">Results</div>'+
                '<div id="results_table"></div></div>');
                
        this.progressbar = $('<div></div>');
        this.containerDiv.find("#progressDiv").append(this.progressbar);
        this.containerDiv.find("#progressDiv").hide();
        this.table = this.containerDiv.find("#results_table");
        this.containerDiv.find("#shortestpath_results").append(this.table);
        this.containerDiv.find("#shortestpath_results").hide();
        
        this.loadSpecies();
        
        this.containerDiv.find('input[type="button"]').attr("class", "ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only");
        
        _.bindAll(this, _.functions(this));
    }
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if( perspective && perspective.name && perspective.name.startsWith("PathFinder") &&
                (documentObject != null) && documentObject instanceof Diagram )
           callback(true);       
        else 
            callback(false);
    };
    
    this.addToList = function (listType)
    {
        var list = this.containerDiv.find("#"+listType+"_list");
        if(_this.selectedNode != undefined)
        {
            if(list.find("[value='"+getElementName(_this.selectedNode)+"']").length > 0)
                return;
            addSelectableListItem(_this.selectedNode, function(item){
                list.append(item);
                list.find(".placeholder").hide();
            });
        }
    };
    
    this.removeFromList = function(listType)
    {
        var list = this.containerDiv.find("#"+listType+"_list");
        list.find(".genericComboItemSelected").remove();
        showPlaceholder(list);
    };

    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            var newDiagram = documentObject.getDiagram();
            if(this.diagram != newDiagram)
            {
                if(this.diagram != null)
                    this.diagram.removeSelectListener(this);
                this.diagram = newDiagram;
                this.diagram.addSelectListener(this);
            }
            this.clearControls();
        }
    };
    
    this.clearControls = function()
    {
        this.containerDiv.find("#sources_list").find(".genericComboItem").remove();
        this.containerDiv.find("#targets_list").find(".genericComboItem").remove();
        this.containerDiv.find("#shortestpath_results").hide();
        this.tableObj = null;
    };
    
    this.loadSpecies = function()
    {
        if(this.species == null)
        {
            var speciesSelector = _this.containerDiv.find("#shp_species");
            speciesSelector.append('<option>Loading species...</option>');
            queryBioUML("web/pathfinder/species_list",{},
                function(data)
                {
                    if(data.values && Array.isArray(data.values))
                    {
                        _this.species = data.values;
                        speciesSelector.children('option').remove();
                        for (i = 0; i < data.values.length; i++) 
                        {
                            speciesSelector.append('<option id="' + data.values[i] + '">' + data.values[i] + '</option>');
                        }
                        setToolbarButtonEnabled(_this.runAction, true);
                    }
                    else
                    {
                        logger.error("Can not load species list. Shortes path search will be unavailable.");
                    }
                }, 
                function(){
                    logger.error("Can not load species list. Shortes path search will be unavailable.");
                });
        }
    }
    
    /*
     * Viewpart is shown
     */
    this.show = function()
    {
        this.diagram.addSelectListener(this);
        
        var selection = this.diagram.getSelection();
        if(selection && selection.length > 0)
            this.nodeSelected(selection[0]);
        else
            this.selectionChanged();
    };

    /*
     * Viewpart will be switched off
     */
    this.save = function()
    {
        this.diagram.removeSelectListener(this);
        this.containerDiv.find("#linkedTabs").tabs("destroy");
    };
    
    /*
     * Creates toolbar actions for this tab and reinit control actions
     */
    this.initActions = function(toolbarBlock)
    {
        this.runAction = createToolbarButton("Calculate shortest path", "search.gif", this.runActionClick);
        toolbarBlock.append(this.runAction);
        if(this.species == null)
            setToolbarButtonEnabled(this.runAction, false);
        
        this.addAction = createToolbarButton("Add selected paths to diagram", "applyLayout2.gif", this.addActionClick);
        toolbarBlock.append(this.addAction);
        this.clearAction = createToolbarButton("Clear highlights", "clear.png", this.clearActionClick);
        toolbarBlock.append(this.clearAction);
        if(this.tableObj == null)
            setToolbarButtonEnabled(this.addAction, false);
        
        this.containerDiv.find('#add_source').unbind("click");
        this.containerDiv.find('#add_source').click(function() {
            _this.addToList("sources");
        });
        
        this.containerDiv.find('#remove_source').unbind("click");
        this.containerDiv.find('#remove_source').click(function() {
            _this.removeFromList("sources");
        });
        
        this.containerDiv.find('#add_target').unbind("click");
        this.containerDiv.find('#add_target').click(function() {
            _this.addToList("targets");
        });
        this.containerDiv.find('#remove_target').unbind("click");
        this.containerDiv.find('#remove_target').click(function() {
            _this.removeFromList("targets");
        });
        
        this.containerDiv.find('input[type="radio"][name=searchType]').unbind("change");
        this.containerDiv.find('input[type="radio"][name=searchType]').change(function() {
            if (this.value == 'Targets')
                $('#targets_div').show();
            else 
                $('#targets_div').hide();
        });
    };
    
    this.nodeSelected = function(nodePath)
    {
        this.selectedNode = this.diagram.completeName + "/" + nodePath;
    };
    
    this.selectionChanged = function(sel)
    {
        if(sel == undefined || sel.length == 0)
        {
            this.selectedNode = undefined;
        }
    };
    
    this.runActionClick = function()
    {
        var sources = [];
        var cnt = 0;
        this.containerDiv.find("#sources_list").find(".genericComboItem").each(function(){
            sources[cnt++]=$(this).attr("value");
        });
        if(sources.length == 0)
        {
            logger.error("Please, select source molecules");
            return;
        }
        var targets = [];
        var searchType = this.containerDiv.find('input[type="radio"][name=searchType]:checked').val();
        if(searchType == "Targets")
        {
            cnt = 0;
            this.containerDiv.find("#targets_list").find(".genericComboItem").each(function(){
                targets[cnt++]=$(this).attr("value");
            });
            if(targets.length == 0)
            {
                logger.error("Please, select target molecules");
                return;
            }
        }
        
        this.jobID = rnd();
        setToolbarButtonEnabled(this.addAction, false);
        this.tableObj = null;
        this.containerDiv.find("#results_table").html("");
        queryBioUML("web/pathfinder/shortest_path",
        {
            diagram: _this.diagram.completeName,
            searchType: searchType,
            direction: this.containerDiv.find("#shp_direction").val(),
            species: this.containerDiv.find("#shp_species").val(),
            sources: $.toJSON(sources),
            targets: $.toJSON(targets),
            jobID: this.jobID
        }, 
        function(data)
        {
            _this.containerDiv.find("#progressDiv").show();
            _this.containerDiv.find("#shortestpath_results").hide();
            createProgressBar(_this.progressbar, _this.jobID, function(status, message)
            {
                _this.searchFinished(status, message, searchType);
            });
            _this.diagram.dataCollectionChanged();
        },
        function(data)
        {
            logger.error(data.message);
        });
    };
    
    
    this.addActionClick = function()
    {
        var rows = getTableSelectedRows(this.tableObj);
        queryBioUML("web/pathfinder/shortest_path_add",
        {
            diagram: _this.diagram.completeName,
            "rows": $.toJSON(rows)
        },
        function(data)
        {
            if(data.values=="ok")
            {
                _this.diagram.dataCollectionChanged();
            }
        });
    };
    
    this.searchFinished = function(status, message, searchType)
    {
        if (status == JobControl.COMPLETED) 
        {
            queryBioUML("web/pathfinder/shortest_path_results",
            {
                diagram: _this.diagram.completeName
            },
            function(data)
            {
                _this.containerDiv.find("#progressDiv").hide();
                _this.containerDiv.find("#shortestpath_results").show();
                if(data.values && Array.isArray(data.values) && data.values.length > 0)
                {
                    setToolbarButtonEnabled(_this.addAction, true);
                    _this.containerDiv.find("#results_table").html('<table cellpadding="0" cellspacing="0" border="0" class="display selectable_table">'+
                    '<thead><tr><th>ID</th><th>Sources</th><th>'+searchType+'</th></tr></thead><tbody></tbody></table>');
                    for(var i = 0; i < data.values.length; i++)
                    {
                        var value = data.values[i];
                        var tr = $('<tr id="' +value.id + '"><td><p class="cellControl">'+value.id+'</p></td><td>'+value.from_name+'</td><td>'+value.to_name+'</td></tr>')
                        _this.table.find('tbody').append(tr);
                    }
                    //TODO: do not parse table, use data source for dataTable instead
                    _this.tableObj = _this.containerDiv.find("#results_table").children("table");
                    var features = 
                    {
                        "bFilter": false,
                        "iDisplayLength" : 50,
                        "sPaginationType": "full_numbers",
                        "sDom" : "tlpir",
                        "read" : true,
                        "ordering": false,
                        "info": false
                    };
                    if(data.values.length < 50)
                    {
                        features.bPaginate = false;
                        features.bInfo = false;
                    }
                    _this.tableObj.dataTable(features);
                    _this.tableObj.fnSetColumnVis( 0, false);
                }
                else
                {
                    _this.containerDiv.find("#results_table").html(data.values);
                    _this.tableObj = null;
                }
            },
            function(data){
                logger.error(data.message);
                _this.tableObj = null;
            });  
        }
        else
        {
            _this.containerDiv.find("#progressDiv").hide();
            _this.containerDiv.find("#shortestpath_results").hide();
            logger.error(message);
        }
    };
    
    this.clearActionClick = function()
    {
        queryBioUML("web/pathfinder/clear_source_target",
        {
            diagram: _this.diagram.completeName
        },
        function(data)
        {
            if(data.values=="ok")
            {
                _this.diagram.dataCollectionChanged();
            }
        });
    };
}

function AssociationViewPart()
{
    this.tabId = "pathfinder.association";
    this.tabName = "Diseases, drugs, tissues";
    this.diagram = null;
    this.tableObj = null;
    this.currentType = "Disease";
    this.availableTypes = ["Disease", "Drug", "Tissue"];
    this.tblDisplayLength = 50;
    var _this = this;
    
    this.isVisible = function(documentObject, callback)
    {
        if( perspective && perspective.name && 'PathFinder' === perspective.name &&
                (documentObject != null) && documentObject instanceof Diagram )
           callback(true);       
        else 
            callback(false);
    };
    
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId).addClass("viewPartTab");
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.containerDiv.html('<div id="progressDiv" style="float:left;padding: 10px 0 0 30px;display: none;"><p><img height="16" width="16" src="images/trobber.gif"/>&nbsp;Loading annotations list...</p></div>'+
        '<div id="notfoundDiv" style="padding: 10px 0 0 30px;display: none;">No <span class="annot_type_name"></span> annotations found for current diagram</div>'+
        '<div id="annot_table" ></div>');
        this.selectTypeElement = $('<select name="annotation_type" style="width:150px;"></select>');
        for (i = 0; i < this.availableTypes.length; i++) 
        {
            var option = $('<option value="' + this.availableTypes[i] + '">' + this.availableTypes[i] + '</option>');
            this.selectTypeElement.append(option);
        }
        this.containerDiv.prepend(this.selectTypeElement);
        _.bindAll(this, _.functions(this));
    };
    
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            var newDiagram = documentObject.getDiagram();
            if(this.diagram != newDiagram)
            {
                if(this.diagram != null){
                    this.diagram.removeSelectListener(this);
                    this.diagram.removeChangeListener(this);
                }
                this.diagram = newDiagram;
                this.diagram.addSelectListener(this);
                this.diagram.addChangeListener(this);
                this.reloadTable();
            }
        }
    };
    
    this.show = function()
    {
        this.diagram.addSelectListener(this);
        this.diagram.addChangeListener(this);
    };

    this.save = function()
    {
        this.diagram.removeSelectListener(this);
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.showAction = createToolbarButton("Load selected annotations", "applyLayout2.gif", this.showActionClick);
        toolbarBlock.append(this.showAction);
        this.clearAction = createToolbarButton("Clear selected annotations", "clear_selected.png", this.clearActionClick);
        toolbarBlock.append(this.clearAction);
        this.clearAction = createToolbarButton("Clear all loaded annotations", "clear.png", this.clearAllActionClick);
        toolbarBlock.append(this.clearAction);
        this.selectTypeElement.unbind("change");
        this.selectTypeElement.change(function()
        {
            _this.currentType = _this.selectTypeElement.val();
            _this.reloadTable();
        });
    };
    
    this.reloadActionClick = function()
    {
        this.reloadTable();
    };
    
    this.reloadTable = function()
    {
        var progressDiv = _this.containerDiv.find("#progressDiv");
        progressDiv.show();
        var notfoundDiv = _this.containerDiv.find("#notfoundDiv");
        notfoundDiv.hide();
        var table = _this.containerDiv.find("#annot_table");
        table.html("");
        var annot_type = _this.currentType;
        queryBioUML("web/pathfinder/annotation_list",
        {
            diagram: _this.diagram.completeName,
            type: annot_type
        },
        function(data)
        {
            if(data.values=="ok")
                _this.loadTable(annot_type);
            else
            {
                progressDiv.hide();
                notfoundDiv.find(".annot_type_name").text(annot_type.toLowerCase());
                notfoundDiv.show();
            }
        });
    };
    
    this.showActionClick = function()
    {
        var rows = getTableSelectedRows(this.tableObj);
        queryBioUML("web/pathfinder/show_annotation",
        {
            diagram: _this.diagram.completeName,
            type: _this.currentType,
            "rows": $.toJSON(rows)
            
        },
        function(data)
        {
            if(data.values=="ok")
            {
                _this.reloadTable();
                _this.diagram.dataCollectionChanged();
            }
        });
    };
    
    this.loadTable = function(tabletype)
    {
        var table = _this.containerDiv.find("#annot_table");
        queryBioUML("web/table/sceleton",
        {
            "de": _this.diagram.completeName,
            "type": "pf_annotations",
            "tabletype": tabletype
        }, 
        function(data)
        {
            var progressDiv = _this.containerDiv.find("#progressDiv");
            progressDiv.hide();
            table.html(data.values);
            var tableObj = table.children("table");
            _this.tableObj = tableObj;
            
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "iDisplayLength" : _this.tblDisplayLength,
                "sPaginationType": "full_numbers",
                "sDom" : "tlpir",
                "read" : true,
                "ordering": false,
                "info": false,
                "fnDrawCallback": function(oSettings) {
                    _this.tblDisplayLength = oSettings._iDisplayLength;
                    _this.tableObj.find('.table_script_node').each(function() {
                            eval($(this).text());
                        }).remove();
                },
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI({de: _this.diagram.completeName, rnd: rnd(), type: "pf_annotations", "tabletype": tabletype, "read" : true}),
            };
            tableObj.addClass('selectable_table');
            tableObj.dataTable(features);
            tableObj.fnSetColumnVis( 0, false);
            tableObj.css('width', '100%');
            mainTableObj = tableObj;
            
        }, function(data)
        {
            table.html("");
            tableObj = null;
        });
    };
    
    this.clearActionClick = function()
    {
        var rows = getTableSelectedRows(this.tableObj);
        queryBioUML("web/pathfinder/clear_annotation",
        {
            diagram: _this.diagram.completeName,
            type: _this.currentType,
            "rows": $.toJSON(rows)
        },
        function(data)
        {
            _this.reloadTable();
            _this.diagram.dataCollectionChanged();
        });
    };
    
    this.clearAllActionClick = function()
    {
        queryBioUML("web/pathfinder/clear_annotation",
        {
            diagram: _this.diagram.completeName,
            type: _this.currentType
        },
        function(data)
        {
            _this.reloadTable();
            _this.diagram.dataCollectionChanged();
        });
    };
    
    this.diagramChanged = function()
    {
        this.reloadTable();
    };
}

function LegendViewpart()
{
    createViewPart(this, "pathfinder.legend", "Legend");
    var iconID = "";
    this.image = $('<img/>');
    this.tabDiv.append(this.image);
    var _this = this;

    this.init = function()
    {
        _this.image.attr("src", appInfo.serverPath+"web/img?"+toURI({de:"databases/Utils/PathFinder/legend_small"}));
//        queryBioUML("web/pathfinder/legend_image",
//        {
//        },
//        function(data)
//        {
//            _this.image.attr("src", appInfo.serverPath+"web/img?"+toURI({id:data.values}));
//        });
    };
    
    this.isVisible = function(documentObject, callback)
    {
        if( perspective && perspective.name && 'PathFinder' === perspective.name)
            callback(true);
        else 
            callback(false);
    };
    
    this.explore = function(documentObject)
    {
    };
    
    this.save = function()
    {
    };
}
