/*
 *	Initialize view parts for bsa projects 
 */
function initBSAViewParts()
{
	if(viewPartsInitialized['bsa'])
        return;
    
    var sitesViewPart = new SitesViewPart();
    sitesViewPart.init();
    viewParts.push(sitesViewPart);
    
    var tracksViewPart = new TracksViewPart();
    tracksViewPart.init();
    viewParts.push(tracksViewPart);

    var trackFinderViewPart = new TrackFinderViewPart();
    trackFinderViewPart.init();
    viewParts.push(trackFinderViewPart);
    
    viewPartsInitialized['bsa'] = true;
}

/*
 * Sites view part class
 */
function SitesViewPart()
{
    this.tabId = "sequence.sites";
    this.tabName = resources.vpSitesTitle;
    this.tabDiv;
    this.visible = true;
    this.selectedTrack;
    this.currentDocument;
    
    var _this = this;
    
    /*
	 * Create div for view part
	 */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.trackSelector = $("<select/>");
        this.trackSelectionBlock = $("<div/>").text(resources.vpSitesTrackPrompt).append(this.trackSelector);
        this.tableBlock = $("<div/>").text(resources.commonLoading);
        this.tabDiv.append(this.trackSelectionBlock).append(this.tableBlock);
    };
    
    /*
	 * Init table object
	 */
    this.loadTable = function(documentObject)
    {
        if ((documentObject == null) || !(documentObject instanceof SequenceDocument)) return;
        this.currentDocument = documentObject;

        this.trackSelector.empty();
        var newSelectedTrack;
    	_.each(documentObject.enabledTracks, function(track)
    	{
    		this.trackSelector.append($("<option/>").text(track.displayName).val(track.de));
    		if(newSelectedTrack === undefined) newSelectedTrack = track.de;
    		if(track.de === this.selectedTrack) newSelectedTrack = this.selectedTrack;
    	}, this);
    	this.selectedTrack = newSelectedTrack;
    	if(!this.selectedTrack)
    	{
    		this.trackSelectionBlock.hide();
    		this.tableBlock.html(resources.vpSitesNoTracks);
    		return;
    	}
        this.trackSelectionBlock.show();
    	this.trackSelector.val(this.selectedTrack);
    	var params = {
    		from: documentObject.from,
    		to: documentObject.to,
    		track: this.selectedTrack,
    		read: "true",
    		add_row_id: "true",
    		de: documentObject.sequenceName,
    		type: "sites"
    	};
    	abortQueries("viewpart."+this.tabId);
    	queryBioUMLWatched("viewpart."+this.tabId, "web/table/sceleton", params, function(data)
        {
	        _this.tableBlock.html(data.values);
	        _this.table = _this.tableBlock.children("table");
	        _this.table.addClass("selectable_table single_row_selected");
	        _this.table.change(function(event)
	        {
	        	var site = getTableSelectedRowIds(_this.table);
	        	if (site.length > 0) 
	        	{
	        	    documentObject.showSiteInfo(_this.selectedTrack, site[0], documentObject.sequenceName);
	        	    documentObject.selectSiteInTrack(_this.selectedTrack, site[0]);
	        	}
	        });
	        var features = 
	        {
	            "bProcessing": true,
	            "bServerSide": true,
	            "bFilter": false,
	            "aaSorting": [[Math.min(_this.table.find("th").length-1, 2), 'asc']],
	            "bSort": _this.table.hasClass("sortable_table"),
                "sPaginationType": "input",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
	            "sDom": "pflirt",
                "fnDrawCallback": function() {
                    _this.table.find('.table_script_node').each(function() {
                        eval($(this).text());
                    }).remove();
                },
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI(params)
	        };
            _this.table.dataTable(features);
        });
    };
    
    /*
	 * Indicates if view part is visible
	 */
    this.isVisible = function(documentObject, callback)
    {        
    	var isCallback = documentObject instanceof SequenceDocument;
        callback(isCallback);
    };
    
    /*
	 * Show viewpart event handler
	 */
    this.show = function(documentObject)
    {
    	this.loadTable(documentObject);
    };
    
    this.explore = function(documentObject)
    {
    	this.trackSelector.change(function()
        {
        	_this.selectedTrack = _this.trackSelector.val();
        	_this.tableBlock.text(resources.commonLoading);
        	_this.loadTable(_this.currentDocument);
        });
    };
    
    /*
	 * Save function
	 */
    this.save = function()
    {
        // nothing to do
    };
}

/*
 * Tracks view part class
 */
function TracksViewPart()
{
    this.tabId = "sequence.tracks";
    this.tabName = resources.vpTracksTitle;
    this.tabDiv;
    this.sequenceObject = null;
    this.selectedRow = null;
    
    var _this = this;
    
    /*
	 * Create div for view part
	 */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
    };
        
    /*
	 * Indicates if view part is visible
	 */
    this.isVisible = function(documentObject, callback)
    {
    	var isCallback = documentObject instanceof SequenceDocument;
        callback(isCallback);
    };
    
    /*
	 * Show viewpart event handler
	 */
    this.show = function(documentObject)
    {
        if(this.sequenceObject == null || this.sequenceObject != documentObject)
        {
            this.sequenceObject = documentObject;
        }
        this.loadTracksTable(documentObject);
    };
    
    this.update = function()
    {
        this.loadLegend();
    };
    
    this.explore = function(documentObject)
    {
        if ((documentObject == null) || !(documentObject instanceof SequenceDocument)) return;
        if(this.sequenceObject == null || this.sequenceObject != documentObject)
        {
            this.sequenceObject = documentObject;
        }
                
        if(! this.trackTable)
        {
            this.trackTable = $('<table class="clipboard"></table>');
            
            this.colorLegendDiv = $('<div id="colorlegend"></div>');
            
            this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
            this.tabDiv.append(this.containerDiv);
            var containerTable = $('<table border=0 width="100%">')
                .append($('<tr></tr>')
                .append($('<td width="400px" valign="top"></td>')
                .append(this.trackTable))
            .append($('<td valign="top"></td>').append(this.colorLegendDiv)));
                
            this.containerDiv.append(containerTable);
        }
    };
    
    /*
	 * Save function
	 */
    this.save = function()
    {
        // nothing to do
    };
    
    /*
	 * Creates toolbar actions for this tab
	 */
    this.initActions = function(toolbarBlock)
    {
        this.addAction = createToolbarButton("Add track", "addtrack.gif");
        this.addAction.click(function()
        {
            _this.addActionClick();
        });
        toolbarBlock.append(this.addAction);
        this.removeAction = createDisabledToolbarButton("Remove track", "removetrack.gif");
        this.removeAction.click(function()
        {
            _this.removeActionClick();
        });
        this.removeAction.disable = function()
        {
            this.addClass('ui-state-disabled');
            this.removeClass('ui-state-default');
            
        };
        this.removeAction.enable = function()
        {
            this.addClass('ui-state-default');
            this.removeClass('ui-state-disabled');
        };
        toolbarBlock.append(this.removeAction);
    };
    
    this.removeActionClick = function ()
    {
        if(this.selectedRow != null)
        {
            var trackName = this.sequenceObject.enabledTracks[this.selectedRow].displayName;
            this.sequenceObject.removeTrack(this.selectedRow);
            this.loadTracksTable(this.sequenceObject);
            this.selectedRow = null;
            this.removeAction.disable();
        }
        else
        {
            logger.message("Please, select track to delete.");
        }
    };
    
    this.addActionClick = function ()
    {
        createOpenElementDialog("Track to add", "ru.biosoft.bsa.Track", "", function(trackPath)
        {
            if(_this.sequenceObject.addTrack(trackPath))
        		_this.loadTracksTable(_this.sequenceObject);
        });
    };
    
    this.addTrackRow = function(documentObject, track, projectName)
    {
        var checkClick;
        var checked = track.isVisible() ? "checked" : "";
        var row = $('<tr></tr>')
	        .attr('id', track.id)
	        .append($('<td/>').text(track.displayName).attr("title", track.de))
	        .append($('<td/>')
	            .append($('<input type="checkbox" '+checked+'/>')
	                .attr('name', track.id)
	                .click(
	                    function(event)
	                    {
	                        var trackId = this.name;
	                        documentObject.panes[trackId].labelDiv.toggle();
	                        documentObject.panes[trackId].viewPane.toggle();
	                        documentObject.onTracksResize();
	                        documentObject.enabledTracks[trackId].toggleVisibility();
	                        checkClick = trackId;
	                    })
	                )
	            .css("text-align", "center")
	            )
	        .append($('<td/>').append($('<input type="button">')
	        		.addClass("ui-state-default")
	        		.val(resources.vpTracksTrackOptions)
	        		.click(
	        			function()
	        			{
	        				createBeanEditorDialog(track.displayName, "bsa/siteviewoptions/"+ (projectName ? projectName + ";" : "") + track.de, function()
	        				{
	        					_this.loadLegend();
	        					_this.sequenceObject.reloadTrack(track.id);
	        				}, true);
	            			return false;
	        			}
	        		))
	        	)
	        .click(function()
	        {
	            if(checkClick != undefined)
	            {
	                checkClick = undefined;
	                return true;
	            }
	            _this.trackTable.find('tr[id!="headerRow"]').css("background", "#fff");
	            if(_this.selectedRow == $(this).attr('id'))
	            {
	                _this.removeAction.disable();
	                _this.selectedRow = null;
	            }
	            else
	            {
	                $(this).css("background", "#ccc");
	                _this.selectedRow = $(this).attr('id');
	                _this.removeAction.enable();
	            }
	            _this.loadLegend();
	        });
        makeEditable(row.children().eq(0), function(newVal)
        {
        	track.displayName = newVal;
        	_this.sequenceObject.panes[track.id].updateTrackLabel(newVal);
        });
        this.trackTable.append(row);
    };
    
    /*
	 * Init table object
	 */
    this.loadTracksTable = function(documentObject)
    {
        if ((documentObject == null) || !(documentObject instanceof SequenceDocument)) return;
        
        this.selectedRow = null;
        this.trackTable.html('<tr id="headerRow"><th>Track</th><th>Visible</th><th>'+resources.vpTracksTrackOptions+'</th></tr>');
        
        var sortOrder = new Array();
        for (var i in documentObject.enabledTracks)
        {
            sortOrder.push(i);
        }
        sortOrder.sort(function(a,b){
            return documentObject.enabledTracks[a].sortPos - documentObject.enabledTracks[b].sortPos;});
        if(sortOrder.length == 0)
        {
            this.trackTable.append($('<tr></tr>').append($('<td colspan="3"></td>').text("No tracks available...")));
        }
        for (var i in sortOrder)
        {
            this.addTrackRow(documentObject, documentObject.enabledTracks[sortOrder[i]], documentObject.projectName);
        }
    };
    
    this.loadLegend = function ()
    {
    	if(this.selectedRow == undefined)
    	{
    		_this.drawLegend({});
    	} else
    	{
            queryService("bsa.service", 53, 
            {
                 sequence: _this.sequenceObject.sequenceName,
                 de: this.sequenceObject.enabledTracks[this.selectedRow].de,
                 project: this.sequenceObject.projectName
            }, function(data)
            {
                _this.drawLegend($.evalJSON(data.values));
            });
    	}
    };
    
    this.drawLegend = function (data)
    {
        if (!this.viewPaneDiv) 
        {
            this.viewPaneDiv = $('<div class="diagramOverview"></div>');
            this.colorLegendDiv.append(this.viewPaneDiv);
            this.viewPane = new ViewPane(this.viewPaneDiv, {
                dragAxis: 'none',
                fullWidth: false,
                fullHeight: false,
                tile: 20
            });
        }
        var legendView = CompositeView.createView(data, this.viewPane.getContext());
        this.viewPane.setView(legendView, true);
        this.viewPane.repaint();
    };
}


/*
 * TrackFinder view part class
 */
function TrackFinderViewPart()
{
    this.tabId = "track.finder";
    this.tabName = "Track finder";

    //DOM elements
    this.tabDiv;
    this.databaseSelect;
    this.propertyInspector;
    this.runButton;
    this.searchStatus;
    this.resultDiv;

    //this view part
    var viewpart = this;

    //Openned SequenceDocument (genome browser)
    this.sequenceDocument = null;
    
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);

        var tabContainerTable = $('<table border=0 width="100%" height="100%">'
        		+ '<tr><td width="400px" valign="top" id="properties"></td><td valign="top" id="search"></td></tr></table>');
        this.tabDiv.append(tabContainerTable);
        var propsContainer = tabContainerTable.find('#properties');
        var searchContainer = tabContainerTable.find('#search');

        this.databaseSelect = $('<select>');
        var databaseSelectionBlock = $("<div/>").text("Database:").append(this.databaseSelect);
        propsContainer.append(databaseSelectionBlock);
        propsContainer.off('change', 'select', this.updateViewOnChange);
        this.databaseSelect.change(this.updateViewOnChange);

        this.propertyInspector = $('<div id="' + this.tabId + '_pi"></div>').css("margin", "5pt").text(resources.anLoading);
        propsContainer.append(this.propertyInspector);

        this.searchStatus = $('<div></div>').appendTo(searchContainer);

        this.resultDiv = $('<div id="' + this.tabId + '_result"></div>');
        searchContainer.append(this.resultDiv);
    };

    this.updateViewOnChange = function()
    {
    	viewpart.resultDiv.empty();
        viewpart.openProperties();
    }

    this.initActions = function(toolbarBlock)
    {
        this.runButton = createToolbarButton('Search', "simulate.gif", this.runSearch);
        toolbarBlock.append(this.runButton);
    }
        
    this.isVisible = function(documentObject, callback)
    {
      var isGenomeBrowser = (documentObject instanceof SequenceDocument && documentObject.sequenceName != undefined);
      callback(isGenomeBrowser);
    };
    
    this.show = function(documentObject)
    {
    };
    
    this.explore = function(documentObject)
    {
    	viewpart.tabDiv.find('#properties').off('change', 'select', this.updateViewOnChange);
    	viewpart.databaseSelect.change(this.updateViewOnChange);

        this.sequenceDocument = documentObject;
        this.loadDatabaseSelector();
    };
    
    this.save = function()
    {
        // nothing to do
    };

    this.getDatabase = function() {
      return viewpart.databaseSelect.val();
    };

    this.getGenome = function() {
      return getElementPath(viewpart.sequenceDocument.sequenceName);
    };
    
    this.loadDatabaseSelector = function() {
      viewpart.resultDiv.empty();
      var genome = viewpart.getGenome();
      queryBioUML("web/track-finder", {
          action: "list-databases",
          genome: genome
      }, function(data) {
        var databases = data.values;
        viewpart.databaseSelect.empty();
        for(var i = 0; i < databases.length; i++)
          viewpart.databaseSelect.append($('<option>')
           .text(databases[i])
           .val(databases[i]));
        if(databases.length > 0) {
          viewpart.databaseSelect.val(databases[0]);
          viewpart.openProperties();
        }
      });
    };

    this.initPropertyInspectorFromJSON = function(data) {
      viewpart.propertyInspector.empty();
      var beanDPS = convertJSONToDPS(data.values);
      viewpart.propertyPane = new JSPropertyInspector();
      viewpart.propertyPane.setParentNodeId(viewpart.propertyInspector.attr('id'));
      viewpart.propertyPane.setModel(beanDPS);
      viewpart.propertyPane.generate();
      viewpart.propertyPane.addChangeListener(function(control, oldValue, newValue) {
        viewpart.syncronizeData(control);
      });
    };

    this.openProperties = function() {
      var database = viewpart.getDatabase();
      if(!database) {
        viewpart.propertyInspector.empty();
        return;
      }
      var genome = viewpart.getGenome();
      queryBean("trackFinder/parameters/"+database + "/" + genome, {}, function(data)
      {
        viewpart.initPropertyInspectorFromJSON(data);
      });
    };

    this.setProperties = function(successCallback, control) {
      disableDPI(viewpart.propertyInspector);
      viewpart.propertyPane.updateModel();
      var json = convertDPSToJSON(viewpart.propertyPane.getModel(), control);
      var database = viewpart.getDatabase();
      var genome = viewpart.getGenome();
      var requestParameters =  {
          de: "trackFinder/parameters/"+database + "/" + genome,
          json: json
      };

      queryBioUML("web/bean/set", requestParameters, successCallback);    
    };

    this.syncronizeData = function(control) {
      viewpart.setProperties(function(data) {
        viewpart.initPropertyInspectorFromJSON(data);
      }, control);
    };

    this.showSearchResults = function()
    {
        var tablePath = "beans/trackFinder/results/"+viewpart.getDatabase()+"/"+viewpart.getGenome();
        queryBioUML("web/table/sceleton",
        {
            "de": tablePath,
            "read" : true
        }, 
        function(data)
        {
            viewpart.resultDiv.empty();
            var tableSceleton = $(data.values);
            if(tableSceleton.find("thead tr th").length == 0) {
              viewpart.resultDiv.text("Nothing found");
              return;
            }
            viewpart.resultDiv.append(tableSceleton);
            viewpart.tableObj = viewpart.resultDiv.children("table");
            var params = 
            {
              rnd: rnd(),
              read: true,
              de: tablePath
            };
            var features = 
            {
                    "bProcessing": true,
                    "bServerSide": true,
                    "bFilter": false,
                    "sPaginationType": "full_numbers_no_ellipses",
                    "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                    "pageLength": 50,
                    "sDom": "pfirlt",
                    "fnRowCallback": function( nRow, aData, iDisplayIndex ) 
                    {
                        var cols = $(nRow).children();
                        return nRow;
                    },
                    "fnDrawCallback": function() {
                        var pathColIdx = viewpart.tableObj.find('thead th')
                         .filter(function() { return $(this).text() == "Visibility"; })
                         .index();
                       if(pathColIdx != -1)
                         viewpart.tableObj.find('tbody tr td:nth-child(' + (pathColIdx+1) + ')').each(function() {
                           var path = $(this).text();
                           var btnDiv = $('<div></div>');
                           var showBtn = $('<input type="button" value="Show">').css('min-width','50px');
                           var hideBtn = $('<input type="button" value="Hide">').css('min-width','50px');
                           
                           var getTrackFromSequenceDocument = function(trackPath) {
                        	   var enabledTracks = viewpart.sequenceDocument.enabledTracks;
                        	   return _.find( enabledTracks, function(track) { return track.de == trackPath; } );
                           }
                           
                           var changeButtonVisibility = function(toShow, toHide) {
                        	   $(toShow).show();
                        	   $(toHide).hide();
                           }
                           
                           if( getTrackFromSequenceDocument(path) )
                        	   changeButtonVisibility(hideBtn, showBtn);
                           else
                        	   changeButtonVisibility(showBtn, hideBtn);

                           showBtn.click(function() {
                             viewpart.sequenceDocument.addTrack(path);
                             changeButtonVisibility(hideBtn, showBtn);
                           });
                           hideBtn.click(function() {
                        	  var enabledTracks = viewpart.sequenceDocument.enabledTracks;
                        	  var toRemove = getTrackFromSequenceDocument(path);
                        	  if( toRemove && toRemove.id )
                        		  viewpart.sequenceDocument.removeTrack(toRemove.id);
                        	  changeButtonVisibility(showBtn, hideBtn);
                           });
                           btnDiv.append(showBtn);
                           btnDiv.append(hideBtn);
                           $(this).html(btnDiv);
                         });
                    },
                    "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI(params)
            };
            viewpart.tableObj.dataTable(features);
            viewpart.tableObj.css('width', '100%');
        }, function(err) {
          console.log(err);
        });
    };

    this.runSearch = function(){
        viewpart.runButton.attr('disabled', true);
        viewpart.searchStatus.text("Searching...");
        viewpart.resultDiv.empty();
        var genome = viewpart.getGenome();
        var database = viewpart.getDatabase();
        queryBioUML("web/track-finder", {
            action: "search",
            genome: genome,
            databaseName: database
        }, function(data) {
          viewpart.runButton.removeAttr('disabled');
          viewpart.searchStatus.text("");
          viewpart.showSearchResults();
        }, function() {
          viewpart.runButton.removeAttr('disabled');
          viewpart.searchStatus.text("Search error");
        });
    };
}
