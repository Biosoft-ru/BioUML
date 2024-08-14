/**
 * Diagram document realization with canvas
 *
 * @author tolstyh, anna
 */

function Diagram(completeName)
{
    this.DiagramSupportConstructor(completeName);
    
    this.loaded = false;
    this.dimension = new Dimension(0,0);
    this.history = undefined;
    this.edgeCreation = undefined;
    this.selectedParameters = {};
    this.changeListeners = new Array();
    this.isDroppable = true;
    //if(perspective && perspective.hideDiagramPanel)
    //additional panel is always hidden, it is not used
    this.hidePanel = true;
    
	var _this = this;
    
    this.open = function(parent)
    {
        this.checkToolbar();
        opennedDocuments[this.tabId] = this;
        var diagramDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(diagramDocument);

        if (this.showToolbar)
        {
            this.toolbar = $('<div id="' + this.tabId + '_toolbar" class="fg-toolbar ui-widget-header ui-corner-all ui-helper-clearfix"></div>').height(25);
            diagramDocument.append(this.toolbar);
            this.loadDiagramToolbar(this.toolbar);
            this.searchBox = $('<input type="text">').hide().keyup(function(event)
            {
                if (event.keyCode == 13 && _this.viewPane && _this.viewPane.view) 
                {
                    var views = _this.viewPane.view.searchText(_this.searchBox.val().toLowerCase());
                    if(!views)
                        logger.error(resources.dlgOpenSaveErrorNotFound.replace('{element}', _this.searchBox.val()));
                    else
                    {
                        _this.selector.selectViews(views);
                    }
                }
            });
        }
        this.createDiagram();
        
        this.checkAnnotation(function(isAnnotation) {
        	
        	if(isAnnotation)
        	{
        		updateToolbar();
        	}
        });
        
    };
    
    this.close = function()
    {
    	clearTimeout(this.autoUpdateTimer);
    	abortQueries("document.diagram.autoupdate");
    };
    
    
    this.panelStateChanged = function()
    {
    	if(!this.panelButtons || this.hidePanel) return;
    	var buttons = this.panelButtons.children();
    	buttons.removeClass("ui-state-active");
    	_.each(["users", "history"], function(tabName, i)
    	{
    		if(this.sharedPane.currentTabName == tabName)
    			buttons.eq(i).addClass("ui-state-active");
    	}, this);
    };
    
    this.setSearchTerms = function(view)
    {
        if(this.searchBox)
            this.searchBox.show().autocomplete({source: view.availableTags()}).css("display", "inline");
    };

    this.scale = function(value)
    {
        if (this.viewPane.width * value > this.viewPane.maxAllowedSize
				|| this.viewPane.height * value > this.viewPane.maxAllowedSize
				|| this.viewPane.width * this.viewPane.height * value * value > this.viewPane.maxAllowedArea)
			return;
        var cx = (this.diagramContainerDiv.scrollLeft() + this.diagramContainerDiv.innerWidth()/2) / this.zoom;
        var cy = (this.diagramContainerDiv.scrollTop() + this.diagramContainerDiv.innerHeight()/2) / this.zoom;
        this.zoom = this.zoom * value;
        this.viewPane.scale(value, value);
        this.diagramContainerDiv.scrollLeft(cx * this.zoom - this.diagramContainerDiv.innerWidth()/2);
        this.diagramContainerDiv.scrollTop(cy * this.zoom - this.diagramContainerDiv.innerHeight()/2);
        this.viewAreaChanged();
        this.restoreSelection();
    };
    
    this.fitToScreen = function()
    {
        if (documentTabs) 
        {
            var w = parseFloat(documentTabs.find('.documentTab').css('width'));
            var h = parseFloat(documentTabs.find('.documentTab').css('height'));
            var minScale = Math.min(w/this.viewPane.width, h/this.viewPane.height)*0.95; 
            this.scale(minScale);
        }
    };
    
    this.showVersion = function(version, version2)
    {
    	clearTimeout(this.autoUpdateTimer);
    	abortQueries("document.diagram.autoupdate");
    	setToolbarButtonEnabled(this.toolbar.find(".fg-buttonset .fg-button"), false);
    	this.changesMode = true;
    	this.readOnly = true;
    	updateToolbar();
        queryBioUML("web/diagram",
        {
             type: "json",
             de: this.completeName,
             version: version,
             version2: version2,
             scale: this.zoom
        }, function(data)
        {
        	_this.update(data);
        	clearTimeout(_this.autoUpdateTimer);
        	abortQueries("document.diagram.autoupdate");
        });
    };
    
    this.revertToVersion = function(version)
    {
    	queryBioUML("web/diagram/revert",
    	{
    		type: "json",
    		de: this.completeName,
    		scale: this.zoom,
    		revertVersion: version
    	}, function(data)
    	{
    		_this.cancelChangesMode();
    		_this.update(data);
    		updateViewParts();
    	});
    };
    
    this.showChanges = function(editFrom, editTo)
    {
    	clearTimeout(this.autoUpdateTimer);
    	abortQueries("document.diagram.autoupdate");
    	setToolbarButtonEnabled(this.toolbar.find(".fg-buttonset .fg-button"), false);
    	this.changesMode = true;
    	this.readOnly = true;
    	updateToolbar();
        queryBioUML("web/diagram",
        {
             type: "json",
             de: this.completeName,
             editFrom: editFrom,
             editTo: editTo,
             scale: this.zoom
        }, function(data)
        {
        	_this.update(data);
        	clearTimeout(_this.autoUpdateTimer);
        	abortQueries("document.diagram.autoupdate");
        });
    };
    
    this.activate = function()
    {
        resizeDocumentsTabs();
        //switch autoUpdate to current diagram
        if(this.loaded)
    		this.autoUpdate();
        selectViewPart('diagram.overview');
	
//    	this.checkAnnotation(function(isAnnotation) {
//        	if(isAnnotation)
//        	{	
//    			_this.autoSave();
//        	}
//    	});
    	//this.splitterDiv.resize();
    };
    
    this.cancelChangesMode = function(redraw)
    {
    	if(!this.changesMode) return;
    	this.changesMode = false;
    	this.readOnly = false;
    	updateToolbar();
    	setToolbarButtonEnabled(this.toolbar.find(".fg-buttonset .fg-button"), true);
    	if(redraw)
    	{
	        queryBioUML("web/diagram",
	        {
	             type: "json",
	             de: this.completeName,
	             scale: this.zoom
	        }, this.update);
    	}
    };
    
    this.autoUpdate = function()
    {
        clearTimeout(this.autoUpdateTimer);
        this.autoUpdateTimer = setTimeout(function()
        {
        	if(isActiveDocument(_this))
        	{
        		abortQueries("document.diagram.autoupdate");
	        	queryBioUMLWatched("document.diagram.autoupdate", "web/diagram/get_changes",
	        	{
	        		de: _this.completeName
	        	}, _this.update, function() {});
        	} else
        	{
        		_this.autoUpdate();
        	}
        }, 0);
    };
    
    this.update = _.bind(function(data, fireChanged)
    {
    	if(data.values.view["class"] != "DummyView")
    	{
    		this.setChanged(true);
    		this.reloadChanged(data.values.view);
    		var elements = data.values.elements;
    		if(elements && elements.length > 0)
    		{
    		    var views = [];
    			for(var i=0; i<elements.length; i++)
    			{
    				var model = this.completeName + "/" + elements[i];
    				var view = this.viewPane.view.findViewByModel(model);
    				if(view != null && !instanceOf(getElementClass(model), "biouml.model.Edge"))
    				{
    					views.push(view);
    				}
    			}
    			this.selector.selectViews(views);
                if(views.length > 0)
                    showElementInfo(views[0].model);
    		} else
    		{
            	this.restoreSelection();
    		}
    		this.checkAnnotation( function(isAnnotation){
    			if(isAnnotation)
    				_this.save();
    		});
    	}
    	if(!this.hidePanel)
    	{
			if(data.values.transactions)
	    	{
	    		this.sharedPane.updateChanges(data.values.transactions);
	    	}
			if(data.values.users)
	    	{
	    		this.sharedPane.updateUsers(data.values.users);
	    		this.updateCollaborativeSelectors(data.values.users);
	    	}
    	}
		this.setHistory(data.values.history);
    	this.autoUpdate();
    	if(fireChanged)
            this.fireChanged();
    }, this);
    
    this.setHistory = function(history)
    {
    	if(!_.isEqual(this.history, history))
    	{
        	this.history = history;
        	updateViewParts();
    	}
    };
    
    this.getHistory = function()
    {
    	return this.history;
    };
    
    this.setDimensions = function()
    {
    	if(this.viewPane && this.viewPane.view)
    	{
    		var rect = this.viewPane.view.getBounds();
    		this.dimension.width = rect.width+rect.x;
    		this.dimension.height = rect.height+rect.y;
    	}
    };

    this.createDiagram = function()
    {
    	this.showDiagram();
    };
    
    this.addDiagramFilter = function(path)
    {
    	var _this = this;
        queryBioUML("web/diagramFilter/add_quick", 
        {
            de: _this.completeName,
            table: path
        }, function(data)
        {
			_this.dataCollectionChanged();
			if(isActiveDocument(_this))
			{
				var filterViewPart = lookForViewPart("diagram.expression");
				if(filterViewPart != null)
				{
					selectViewPart("diagram.expression");
					filterViewPart.explore(_this);
				}
			}
        });
    };
    
    this.showDiagram = function(name)
    {
        var tab = $(getJQueryIdSelector(this.tabId));
        this.splitterDiv = $('<div class="documentTab"/>').css({overflow: "hidden"});
        tab.append(this.splitterDiv);
        this.diagramContainerDiv = $('<div id="' + this.tabId + '_container" class="diagramDocumentTab"></div>');
        this.splitterDiv.append(this.diagramContainerDiv);
        
        var _this = this;
        
        //special additional pane at the right part of document
        if(!this.hidePanel)
        {
            this.sharedPane = new AdditionalPanel(this);
            this.splitterDiv.append(this.sharedPane.mainDiv);
            this.splitterDiv.split(
            {
                orientation: "vertical",
                limit: 0,
                position: "80%"
            });
            
//            this.splitterDiv.splitter(
//            	    {
//            	        type: "v",
//            	        sizeRight: true
//            	    });
        }
        
        this.diagramContainerDiv.resize(function(event)
        {
        	_this.viewAreaChanged();
        });
        this.splitterDiv.resize(function(event)
        {
        	if(!isActiveDocument(_this)) return;
        	if(_this.splitterDiv.has(event.target).length) return;
        	if(!_this.hidePanel)
        	{
        		_this.sharedPane.mainDiv.width(Math.min(_this.sharedPane.mainDiv.width(), _this.splitterDiv.width()/2)).height(_this.splitterDiv.innerHeight()).trigger("resize");
        		_this.diagramContainerDiv.width(_this.splitterDiv.width()-_this.sharedPane.mainDiv.width).height(_this.splitterDiv.innerHeight()).trigger("resize");
        	} else
        	{
        		_this.diagramContainerDiv.width(_this.splitterDiv.width()).height(_this.splitterDiv.innerHeight()).trigger("resize");
        	}
        });
        if(this.showToolbar && !this.hidePanel)
        {
	        this.panelButtons = $('<div class="fg-rightcomponent ui-helper-clearfix"></div>');
	        _.each([[resources.dgrButtonUsers, "users"],[resources.dgrButtonHistory, "history"]], function(v)
	        {
	        	this.panelButtons.append(createToolbarButton(v[0], v[1]+".gif", function()
	            {
	            	if(_this.sharedPane)
	            	{
	            		_this.sharedPane.show(v[1]);
	            	}
	            }));
	        }, this);
	        this.toolbar.append(this.panelButtons);
	        this.panelStateChanged();
        }
        this.loadDiagram();
        
        //scroll event
        this.diagramContainerDiv.scroll(function(event)
        {
            _this.viewAreaChanged();
        });
        
        if(this.isDroppable)
        {
            createTreeItemDroppable(this.diagramContainerDiv, null, function(path,event) 
    		{
            	if(instanceOf(getElementClass(path), "ru.biosoft.table.TableDataCollection") && !_this.isAnnotation)
        		{
            		_this.addDiagramFilter(path);
        		}
    //        	else
    //    		{
        			_this.addDiagramElement(path,event);
    //    		}
            });
        }
        this.diagramContainerDiv.scrollview({scrollStart: function(e) {
        	return !e.ctrlKey && !e.metaKey;
        }});

        this.diagramContainerDiv.click(function(event)
        {
        	if (_this.mouseDown && (_this.mouseDown != null))
            {
                _this.selectClickedArea(event);
            }
            _this.mouseDown = null;
            return false;
        });
        this.diagramContainerDiv.mousedown(function(event)
        {
        	_this.clickedView = _this.getViewByEvent(event);
            _this.mouseDown = {
				x : event.originalEvent.clientX,
				y : event.originalEvent.clientY
			};
        	if(_this.rectangularSelection)
        	{
            	_this.rectangularSelection.remove();
            	_this.rectangularSelection = null;
        	}
            if(!_this.edgeCreation && (event.ctrlKey || event.metaKey))
            {
            	_this.rectangularSelection = $("<div/>").addClass("selector_dotes").css({
            		position: "absolute", background: "none", width: 0, height: 0, top: 0, left: 0, 
            		cursor: _this.diagramContainerDiv.children().css("cursor") 
            	}).appendTo(_this.diagramContainerDiv);
            	return;
            }
        	event.preventDefault();
        });
        var getSelection = function(event)
        {
            var offsetX = _this.diagramContainerDiv.offset().left - _this.diagramContainerDiv.scrollLeft();
            var offsetY = _this.diagramContainerDiv.offset().top - _this.diagramContainerDiv.scrollTop();
            var x1 = event.originalEvent.clientX - offsetX;
            var y1 = event.originalEvent.clientY - offsetY;
        	var x2 = _this.mouseDown.x - offsetX;
        	var y2 = _this.mouseDown.y - offsetY;
        	if(x1 > x2)
    		{
        		var tmp = x1;
        		x1 = x2;
        		x2 = tmp;
    		}
        	if(y1 > y2)
    		{
        		var tmp = y1;
        		y1 = y2;
        		y2 = tmp;
    		}
        	return new Rectangle(x1, y1, x2-x1, y2-y1);
        }
        this.diagramContainerDiv.mouseup(function(event)
        {
            if(_this.rectangularSelection)
            {
            	_this.rectangularSelection.remove();
            	_this.rectangularSelection = null;
            	var rect = getSelection(event);
            	rect.x /= _this.zoom;
            	rect.y /= _this.zoom;
            	rect.width /= _this.zoom;
            	rect.height /= _this.zoom;
            	var views = (function getViews(rect, view) 
            	{
            		var result = [];
            		for(var i=0; i<view.children.length; i++)
            		{
            			var child = view.children[i];
            			if(child.model && rect.contains(child.getBounds()))
            			{
            				if( !instanceOf(getElementClass(child.model), "biouml.model.Edge") )
            					result.push(child);
            				
            			} else if(child instanceof CompositeView)
            			{
            				result = result.concat(getViews(rect, child));
            			}
            		}
            		return result;
            	})(rect, _this.viewPane.view);
            	if(views.length > 0)
            		_this.selector.selectViews(views);
            	return;
            }
        	event.preventDefault();
        });
        
        var tooltip;
        this.diagramContainerDiv.mousemove(function(event)
        {
            if(!_this.zoom || !_this.viewPane.view)
                return;
            var parentX = _this.diagramContainerDiv.offset().left;
            var parentY = _this.diagramContainerDiv.offset().top;
            var x = event.originalEvent.clientX - parentX + _this.diagramContainerDiv.scrollLeft();
            var y = event.originalEvent.clientY - parentY + _this.diagramContainerDiv.scrollTop();
            if(_this.rectangularSelection)
            {
            	var rect = getSelection(event);
            	_this.rectangularSelection.css({position: "absolute", left: rect.x-1, top: rect.y-1, width: rect.width, height: rect.height});
            	return;
            }
            var activeView = _this.viewPane.view.getDeepestActive(new Point(x/_this.zoom, y/_this.zoom), null);
            if (!_this.edgeCreation && activeView && activeView.model && instanceOf(getElementClass(activeView.model), "biouml.model.Edge")) 
            {
                _this.viewPane.highlightView(activeView);
            }
            else if(_this.edgeCreation && _this.edgeCreationView)
        	{
            	var endX = x/_this.zoom;
            	var endY = y/_this.zoom;
            	if(activeView && activeView != _this.edgeCreationView && activeView.model && !instanceOf(getElementClass(activeView.model), "biouml.model.Edge"))
            	{
            		_this.selector.selectViews([_this.edgeCreationView, activeView]);
            		endX = activeView.getBounds().getCenterX();
            		endY = activeView.getBounds().getCenterY();
            	} else
            	{
            		_this.selector.selectViews([_this.edgeCreationView]);
            	}
            	var arrowView = new ArrowView(Pen.BLACK, undefined, new Path(_this.edgeCreationView.getBounds().getCenterX(), _this.edgeCreationView.getBounds().getCenterY(),
            			endX, endY), undefined, undefined);
            	_this.viewPane.highlightView(arrowView);
        	}
            else
            {
                _this.viewPane.highlightView();    
            }
            if (activeView && activeView.description != undefined)
        	{
            	if(!tooltip || tooltip.data("view") != activeView)
            	{
            		if(tooltip) tooltip.remove();
            		tooltip = $("<div/>").addClass("viewTooltip").text(activeView.description).css("left", x+2).css("top", y+10);
            		_this.diagramContainerDiv.append(tooltip);
            	}
        	} else
        	{
            	if(tooltip)
            	{
            		tooltip.remove();
            		tooltip = undefined;
            	}
        	}
        });
        this.diagramContainerDiv.dblclick(function(event)
        {
        	if(!_this.isAnnotation && !event.ctrlKey && !event.metaKey)
        	{
        		var nodes = _this.selector.getSelectedNodes();
        		if(nodes.length != 1 && _this.edgeEditor.isActive())
        			nodes = [_this.edgeEditor.node];
        		if(nodes.length == 1)
        		{
        			queryBioUML("web/diagram/get_neighbors",
	    			{
	    				de: _this.completeName,
	    				path: $.toJSON(nodes) 
	    			}, function(data)
	    			{
	    				var views = [];
	    				var neighbors = data.values.nodes;
	    				for(var i=0; i<neighbors.length; i++)
	    				{
	    					var view = _this.viewPane.view.findViewByModel(_this.completeName+"/"+neighbors[i]);
	    					if(view)
	    						views.push(view);
	    				}
	    				_this.selector.selectViews(views);
	    				_this.edgeEditor.clear();
	    				if(views.length == 1 && views[0].model)
	    				    showElementInfo(views[0].model);
	    			});
        		}
        	}
        });
        this.selector = new Selector(this.diagramContainerDiv, this);
        this.checkAnnotation( function(isAnnotation){
    		_this.selector.initContextMenu();
		});
        this.edgeEditor = new EdgeEditor(this.diagramContainerDiv, this);
    };
    
    this.loadDiagram = function (autoscale)
    {
        if(this.loaded)
            return false;
        var x=0;
        var y=0;
        this.viewPane = new ViewPane(this.diagramContainerDiv, 
        {
            dragAxis: 'none',
            fullWidth:false, 
            fullHeight:false,
			tile: 200 
        });
        var _this = this;
        getDataCollection(this.completeName).getBeanFields('attributes/autoscale', function(result)
        {
            var autoscale = false;
            if(result)
            {
                var attr = result.getValue('attributes');
                if(attr)
                    autoscale = attr.getValue('autoscale')=="true" || attr.getValue('autoscale')=="yes";
            }
            queryBioUML("web/diagram",
            {
                 type: "json",
                 de: _this.completeName,
                 scale: _this.zoom
            }, function(data)
            {
                var diagramView = CompositeView.createView(data.values.view, _this.viewPane.getContext());
                _this.setSearchTerms(diagramView);
                _this.expandView(diagramView);
                _this.viewPane.setView(diagramView, true);
                
                if(autoscale)
                    _this.fitToScreen();
                else
                    _this.rescale();

                _this.loaded = true;
                resizeDocumentsTabs();
                
                _this.viewChanged();
                _this.viewAreaChanged();
                
                if(_this.loadedListener)
                    _this.loadedListener();
                    
                
                _this.viewPane.scrollToVisible();
                _this.autoUpdate();
                
                if(!_this.hidePanel)
                {
    	            if(data.values.users)
    	        	{
    	        		_this.sharedPane.updateUsers(data.values.users);
    	        		_this.updateCollaborativeSelectors(data.values.users);
    	        	}
    	    		if(data.values.transactions)
    	        	{
    	        		_this.sharedPane.updateChanges(data.values.transactions);
    	        	}
                }
        		_this.setHistory(data.values.history);
            });
        });
    };
    
    this.updateCollaborativeSelectors = function(users)
    {
    	if(this.hidePanel) return;
    	this.diagramContainerDiv.children(".collaborativeSelector").remove();
    	var selectors = {};
    	for(var i in users)
    	{
    		selectors[users[i].name] = users[i].selections;
    	}
    	for(var user in selectors)
		{
    		_.each(selectors[user], function(sel)
    		{
    			var selectorDiv = $("<div/>").addClass("collaborativeSelector")
						.css({
							left : (sel.x-1) + "px",
							top : (sel.y-1) + "px",
							width : sel.width + "px",
							height : sel.height + "px",
							borderColor : this.sharedPane.getUserColor(user)
						});
    			selectorDiv.append($("<span/>").css({backgroundColor: this.sharedPane.getUserColor(user)}).text(user));
    			this.diagramContainerDiv.append(selectorDiv);
    		}, this);
		}
    };
    
    this.expandView = function(view)
    {
    	var rect = view.getBounds();
    	var box = new BoxView(null, null, rect.x+rect.width+500, rect.y+rect.height+100, 0, 0);
        box.model = "__extents__";
    	view.add(box);
    };
    
    this.reloadChanged = function (changedData, needRescale)
    {
        if(changedData['class'] != 'DummyView')
        {
            var changedView = CompositeView.createView(changedData, this.viewPane.getContext(), this.viewPane.view);
            changedView.type = 0;
            if(this.searchBox)
                this.searchBox.show().autocomplete({source: changedView.availableTags()});
            this.expandView(changedView);
            this.rescale(changedView.getBounds());
            this.viewPane.setView(changedView, true);
            this.viewChanged();
			this.viewAreaChanged();
        }
    };
    
    this.restoreSelection = function()
    {
    	this.selector.restoreSelection();
    	if(this.edgeEditor.isActive())
    		this.selectEdge(this.edgeEditor.node);
    };
    
    this.selectEdge = function(edge)
    {
    	this.selector.clear();
    	this.edgeEditor.clear();
    	var view = edge == undefined?undefined:this.viewPane.view.findViewByModel(this.completeName+"/"+edge);
    	if(view != undefined)
    	{
    		var rect = view.getBounds();
            var activeView = view;
            while(activeView instanceof CompositeView)
            {
                activeView = activeView.children[0];
            }
            if (activeView.path) 
            {
                this.edgeEditor.setRect(rect);
                this.edgeEditor.select(edge, activeView.path);
                this.viewPane.selectView(activeView);
            }
    	}
    };

    this.setViewArea = function(x, y)
    {
        this.diagramContainerDiv.scrollLeft(x * this.zoom);
        this.diagramContainerDiv.scrollTop(y * this.zoom);
    };
    
    this.getViewByEvent = function(event, ignoreModels)
    {
        var parentX = this.diagramContainerDiv.offset().left;
        var parentY = this.diagramContainerDiv.offset().top;
        var x = event.originalEvent.clientX - parentX + this.diagramContainerDiv.scrollLeft();
        var y = event.originalEvent.clientY - parentY + this.diagramContainerDiv.scrollTop();
        return this.viewPane.view.getDeepestActive(new Point(x/this.zoom, y/this.zoom), ignoreModels);
    };
    
    this.selectClickedArea = function(event)
    {
        var _this = this;
        if (_this.clickAction(event) == true)
        {
            var ctrlClick = event.ctrlKey || event.metaKey;
            var shiftClick = event.shiftKey;
            
            var activeView = this.getViewByEvent(event);
            
            if(this.edgeCreation)
            {
            	if(activeView && activeView == this.clickedView && 
            			activeView != this.edgeCreationView && !instanceOf(getElementClass(activeView.model), "biouml.model.Edge"))
            	{
                    var node = activeView.model;
            		if(!this.edgeCreationView)
            		{
            			this.edgeCreationView = activeView;
            			this.selector.selectView(activeView);
                		showElementInfo(node);
            		} else
            		{
            			var dlg = new NewEdgeDialog(this, this.edgeCreation[0], this.edgeCreation[1]);
            			dlg.addElement(this.getRelativeNodeName(this.edgeCreationView.model), 
            					this.getRelativeNodeName(activeView.model));
            			this.selector.clear();
            			this.edgeCreationView = undefined;
            			this.viewPane.highlightView();
            			this.setSelectMode();
            		}
            	} else
            	{
        			this.selector.clear();
        			this.edgeCreationView = undefined;
        			this.viewPane.highlightView();
            	}
            }
            else if(this.copyElement != undefined) //copy element mode
            {
                this.selector.clear();
                this.addDiagramElement(this.copyElement,event);
                this.copyElement = undefined;
            }
            else
            {
                if(activeView && activeView == this.clickedView)
                {
                    var node = activeView.model;
                    if( instanceOf(getElementClass(node), "biouml.model.Edge") )
                    {
                        var activeEdgeView = activeView;
                        while(activeEdgeView instanceof CompositeView)
                        {
                            activeEdgeView = activeEdgeView.children[0];
                        }
                        if (activeEdgeView.path) 
                        {
                            var path = activeEdgeView.path;
                            this.selector.clear();
                            this.edgeEditor.setRect(activeView.getBounds());
                            this.edgeEditor.select(_this.getRelativeNodeName(node), path);
                            this.viewPane.selectView(activeView);
                    		showElementInfo(node);
                        }
                    }
                    else
                    {
                    	this.edgeEditor.clear();
                    	if(ctrlClick)
                    	{
                    		this.selector.toggleView(activeView);
                    	} else if(shiftClick)
                		{
                    		this.selector.addView(activeView);
                		} else
                		{
                			this.selector.selectView(activeView);
                    		showElementInfo(node);
                		}
                    }
                } else
                {
                    if(!ctrlClick && !shiftClick) 
                    {
                    	this.selector.clear();
                    	this.edgeEditor.clear();
                    }
            		showElementInfo(this.completeName);
                }
            }
        }
    };

    this.viewAreaChanged = function()
    {
        if (this.diagramContainerDiv)
        {
			this.viewPane.repaint(new Rectangle(this.diagramContainerDiv.scrollLeft(), this.diagramContainerDiv.scrollTop(),
				this.diagramContainerDiv.get(0).clientWidth, this.diagramContainerDiv.get(0).clientHeight));
            var left = this.diagramContainerDiv.scrollLeft() / this.zoom;
            var top = this.diagramContainerDiv.scrollTop() / this.zoom;
            var width = this.diagramContainerDiv.get(0).clientWidth / this.zoom;
            if (width > this.dimension.width)
            {
                width = this.dimension.width;
            }
            var height = this.diagramContainerDiv.get(0).clientHeight / this.zoom;
            if (height > this.dimension.height)
            {
                height = this.dimension.height;
            }
            for (li = 0; li < this.viewAreaListeners.length; li++)
            {
                this.viewAreaListeners[li].viewAreaChanged(left, top, width, height);
            }
        }
    };

    this.viewChanged = function()
    {
    	var vp = getActiveViewPart();
		this.setDimensions();
		for (li = 0; li < this.viewAreaListeners.length; li++)
		{
			if (vp instanceof DiagramOverviewViewPart) 
            {
                this.viewAreaListeners[li].imageChanged();
            }
            else
            {
                this.viewAreaListeners[li].setToReload();
            }
		}
    };
    
    this.rescale = function(rect)
    {
        var width = rect ? Math.max((rect.x + rect.width)*this.viewPane.canvasScale.width + 5, 20) : this.viewPane.width;
        var height = rect ? Math.max((rect.y + rect.height)*this.viewPane.canvasScale.height + 5, 20) : this.viewPane.height;
        
        if (width > this.viewPane.maxAllowedSize || height > this.viewPane.maxAllowedSize
        		|| width * height > this.viewPane.maxAllowedArea ) 
        {
            var scale = 1.0;
            var maxSize = Math.max(width, height);
            var area = width * height;
            while(maxSize > this.viewPane.maxAllowedSize || area > this.viewPane.maxAllowedArea)
            {
                scale *= 0.8;
                maxSize *= 0.8;
                area *= 0.8*0.8;
            }
        	this.scale(scale);
        }
    };
    
    this.convert = function()
    {
    	var _this = this;
    	var diagramTypesParameters = 
    	{
    		dc: _this.completeName,
    		command: 215,
            service: "diagram.service"
        };
    	
    	var property = new DynamicProperty("Diagram", "data-element-path", _this.completeName );
        property.getDescriptor().setDisplayName(resources.dlgCreateOptimizationDiagram);
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        property.setAttribute("dataElementType", "biouml.model.Diagram");
        property.setAttribute("elementMustExist", false);
        property.setAttribute("promptOverwrite", true);
        
    	var pathEditor = new JSDataElementPathEditor(property, null);
        pathEditor.setModel(property);
        var dgrNode = pathEditor.createHTMLNode();
        pathEditor.setValue(_this.completeName + "_converted");
        
        var dialogDiv = $('<div title="'+resources.dlgConvertDiagramTitle+'" id="convert_diagram_dlg'+ rnd() + '">' + resources.dlgCreateDiagramName + '</div>');
        dialogDiv.append(dgrNode);
        dialogDiv.append('<br/>' + resources.dlgCreateDiagramType+' <span id="loading_dummy" ><img src="icons/busy.png"/>'+resources.commonLoading+'</span><div id="type_container"/><br/>');
        var loadingDummy = dialogDiv.find('#loading_dummy');
        var typeContainer = dialogDiv.find('#type_container');
        
        dialogDiv.dialog(
        {
            autoOpen: false,
            width: 320,
            buttons: 
            {
                "Ok": function()
                {
                	if(typeContainer.find('select').val())
                	{
                		var diagramConvertParameters = 
                		{
                			dc: _this.completeName,
                			newdc: pathEditor.getValue(),
                			id: typeContainer.find('select').val(),
                			command: 216,
                			service: "diagram.service"
                		};
                		queryBioUML("web/data", diagramConvertParameters, function(data)
                		{
                			logger.message(resources.dlgConvertDiagramSuccess);
                		});
                	}
                	$(this).dialog("close");
                    $(this).remove();
                },
                "Cancel" : function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                }
            }
        });
        addDialogKeys(dialogDiv);
        sortButtons(dialogDiv);
        dialogDiv.dialog("open");
        queryBioUML("web/data", diagramTypesParameters, function(data)
        {
        	loadingDummy.hide();
        	var diagramTypes = JSON.parse(data.values);
        	if(diagramTypes.length == 0)
        	{
        		typeContainer.html("<p>No available types for this diagram</p>");
        	}
        	else
        	{
        		var typesCombo = $('<select style="width : 250"/>');
        		typeContainer.html(typesCombo);
        		for(var i in diagramTypes)
        		{
        			typesCombo.append('<option value="'+diagramTypes[i].id+'">'+diagramTypes[i].title+'</option>');
        		}
        	}
        });
    };
    
    this.highlightOn = function(model, rows)
    {
        _this.selectedParameters[model] = rows;
        var dataParams = {
                de: _this.completeName,
                jsonrows: $.toJSON(rows),
                what: model,
                type: "json"
            };
        queryBioUML("web/diagram/highlight_on", dataParams, function(data)
        {
            _this.update(data);
        });
    };
    
    this.highlightOff = function(model)
    {
        if(model)
            delete _this.selectedParameters[model];
        else
        {
            for(type in _this.selectedParameters) 
                delete _this.selectedParameters[type];
        }
        var dataParams = {
                de: _this.completeName,
                type: "json"
            };
        queryBioUML("web/diagram/highlight_off", dataParams, function(data)
        {
            _this.update(data);
        });
    };
    
    this.removeVariables = function(model, rows)
    {
        for(type in _this.selectedParameters) 
            delete _this.selectedParameters[type];
        
        var dataParams = {
                de: _this.completeName,
                jsonrows: $.toJSON(rows),
                what: model,
                type: "json"
            };
        queryBioUML("web/diagram/remove_variables", dataParams, function(data)
        {
            _this.update(data, true);
        });
    };
    
    this.addVariable = function(callback)
    {
        var dataParams = {
                de: _this.completeName,
                type: "json"
            };
        queryBioUML("web/diagram/add_variable", dataParams, function(data)
        {
            if(callback)
                callback();
        });
    }
    
    /*
     * Collection changes processing
     */
    this.addChangeListener = function(listener)
    {
        var alreadyAdded = false;
        for (li = 0; li < this.changeListeners.length; li++)
        {
            if (this.changeListeners[li] == listener)
            {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded)
        {
            this.changeListeners.push(listener);
        }
    };
    
    this.fireChanged = function()
    {
        for (li = 0; li < this.changeListeners.length; li++)
        {
            this.changeListeners[li].diagramChanged();
        }
    };
    
    this.removeChangeListener = function(listener)
    {
        var newListeners = new Array();
        for (li = 0; li < this.changeListeners.length; li++)
        {
            if (this.changeListeners[li] != listener)
            {
                newListeners.push(this.changeListeners[li]);
            }
        }
        this.changeListeners = newListeners;
    };
	
	this.updateDocumentView = function()
	{
		this.refresh();
	};
}

function AdditionalPanel(parent)
{
	this.parent = parent;
	var _this = this;
	
	this.colorMap = [];//map of colors for users
	this.mainDiv = $('<div id="' + this.parent.tabId + '_shared" class="additionalTab"/>');
	this.currentTabName = '';
	this.position = 0;
	this.defaultWidth = 200;
	
	this.initPanes = function()
	{
		this.mainDiv.bind("resize", _.bind(this.resize, this));

		this.tabs = [];
		
		var userTab = $('<div/>');
		userTab.append($("<div class=\"additionalTabTitle\"/>").text(resources.dgrUsersPanelTitle));
		this.usersPane = $('<div class="additionalTabPane"/>');
		userTab.append(this.usersPane);
		userTab.append($("<div class=\"additionalTabTitle\"/>").text(resources.dgrChatPanelTitle));
		this.chatPane = $('<div class="additionalTabPane"/>');
		userTab.append(this.chatPane);
		this.chatInput = $('<input type="text" class="additionalTabPane"/>').css('height', 20);
		userTab.append(this.chatInput);
		this.chatInput.keyup(function(event)
		{
			if (event.keyCode == 13 && !event.ctrlKey) 
		    {
		       	var msg = _this.chatInput.val();
		    	if(msg == '') return;
		       	_this.sendMessage(msg);
				return true;
		    }
		});
		this.tabs['users'] = userTab;
		this.mainDiv.append(userTab);
		
		var historyTab = $('<div/>');
		var historyTabTitle = $("<div class=\"fg-toolbar ui-widget-header ui-corner-all ui-helper-clearfix\"/>");
		var block2 = $('<div class="fg-rightcomponent ui-helper-clearfix"></div>');
		this.historyOkButton = createDisabledToolbarButton(resources.dgrHistoryPanelApply, "ok.gif", function()
		{
			_this.parent.undo(_this.position);
			_this.lastChanges = null;
			_this.parent.cancelChangesMode(false);
			setToolbarButtonEnabled(_this.historyCancelButton, _this.historyOkButton, false);
		});
		block2.append(this.historyOkButton);
		this.historyCancelButton = createDisabledToolbarButton(resources.dgrHistoryPanelRestore, "cancel.gif", function()
		{
			_this.parent.cancelChangesMode(true);
			var changes = _this.lastChanges;
			_this.lastChanges = null;
			_this.updateChanges(changes);
			setToolbarButtonEnabled(_this.historyCancelButton, _this.historyOkButton, false);
		});
		block2.append(this.historyCancelButton);
		historyTabTitle.append($("<div style=\"float: left; padding: 3pt 1pt 1pt 2pt;\"/>").text(resources.dgrHistoryPanelTitle)).append(block2);
		historyTab.append(historyTabTitle);
		this.changesPane = $('<div class="additionalTabPane"/>').addClass("separatedList");
		historyTab.append(this.changesPane);
		this.tabs['history'] = historyTab;
		this.mainDiv.append(historyTab);

		this.mainDiv.children().hide();
		if(this.currentTabName === '') 
		{
		    this.mainDiv.width(1);
		} else
		{
		    this.tabs[this.currentTabName].show();
		    this.mainDiv.width(this.defaultWidth);
		}
		
		this.initChat();
	};
	
	this.updateUsers = function(users)
	{
		_this.usersPane.empty();
		if(users.length == 1)
			_this.iaminviter = true;
		if(_this.iaminviter)
		{
			if(!_this.userCache)
				_this.userCache = {};
			for(var i=0;i<users.length; i++)
			{
				if(!_this.userCache[users[i].name] == 1)
				{
					var userJID = toJIDString(users[i].name);
					if(window.messageConnector && userJID != messageConnector.username)
					{
						messageConnector.inviteUser(_this.groupName, userJID);
					}
				}
			}
		}
		_this.userCache = {};
		for(var i=0;i<users.length; i++)
		{
			_this.userCache[users[i].name] = 1;
			var userDiv = $("<div>"+users[i].name+"</div>").css('color', this.getUserColor(users[i].name)).css('font-weight', 'bold').css('padding-left', 10);
			_this.usersPane.append(userDiv);
		}
		_this.resize();
	};
	
	this.updateChanges = function(changes)
	{
		// TODO: gracefully update changes
		if(_.isEqual(changes, _this.lastChanges) || _this.parent.changesMode) return;
		_this.lastChanges = changes;
		_this.changesPane.empty();
		var userCache = [];
		var separator = '<div class="listSeparatorBlock"><div class="listSeparatorLeft">&#x25BA;</div><div class="listSeparator"></div><div class="listSeparatorRight">&#x25C4;</div></div>';
		_this.changesPane.append(separator);
		_this.changesPane.mousedown(function(event) {event.preventDefault();});
		var nextChange = changes.length;
		var separators;
		for(var i=0;i<changes.length; i++)
		{
			var userSpan = $("<span/>").text(changes[i].user).css('color', this.getUserColor(changes[i].user)).css('font-weight', 'bold');
			var nameSpan = $("<span/>").text(changes[i].name).css('padding-left', 10);
			if(changes[i].next) nextChange = i;
			var userDiv = $("<div/>").addClass("content").append(userSpan).append(nameSpan);
			$(userDiv).data("changeNum", i);
			$(userDiv).data("changeNum", i).click(function(event)
			{
				var e = $(this);
				_this.changesPane.children(".content").removeClass("historySelected");
				var changeNum = e.data("changeNum");
				if(event.pageY-e.offset().top < e.height()/2) changeNum++;
				setToolbarButtonEnabled(_this.historyOkButton, changeNum != nextChange);
				setToolbarButtonEnabled(_this.historyCancelButton, true);
				separators.children().removeClass("active");
				separators.eq(separators.length-changeNum-1).children().addClass("active");
				_this.parent.showChanges(changeNum, changeNum);
				_this.position = changeNum;
			});
			_this.changesPane.prepend(userDiv).prepend(separator);
		}
		var updateChangesSelection = function()
		{
			var leftList = _this.changesPane.find(".listSeparatorLeft");
			var leftIndex = leftList.index(leftList.filter(".active"));
			var rightList = _this.changesPane.find(".listSeparatorRight");
			var rightIndex = rightList.index(rightList.filter(".active"));
			if(leftIndex > rightIndex)
			{
				var tmp = leftIndex;
				leftIndex = rightIndex;
				rightIndex = tmp;
			}
			_this.changesPane.children(".content").removeClass("historySelected")
				.slice(leftIndex, rightIndex).addClass("historySelected");
			var separatorList = _this.changesPane.find(".listSeparator");
			setToolbarButtonEnabled(_this.historyOkButton, leftIndex === rightIndex && leftList.length-rightIndex-1 != nextChange);
			setToolbarButtonEnabled(_this.historyCancelButton, true);
			separatorList.removeClass("active");
			separatorList.eq(leftIndex).addClass("active");
			separatorList.eq(rightIndex).addClass("active");
			_this.position = leftList.length-rightIndex-1;
			_this.parent.showChanges(leftList.length-rightIndex-1, leftList.length-leftIndex-1);
		};
		_this.changesPane.find(".listSeparatorLeft").click(function()
		{
			_this.changesPane.find(".listSeparatorLeft").removeClass("active");
			$(this).addClass("active");
			updateChangesSelection();
		});
		_this.changesPane.find(".listSeparatorRight").click(function()
		{
			_this.changesPane.find(".listSeparatorRight").removeClass("active");
			$(this).addClass("active");
			updateChangesSelection();
		});
		separators = _this.changesPane.children(".listSeparatorBlock");
		separators.eq(separators.length-nextChange-1).children().addClass("active");
	};
	
	this.show = function(tabName)
	{
		if(this.currentTabName == tabName)
		{
			this.mainDiv.width(1);
			this.currentTabName = '';
			this.parent.splitterDiv.position("100%");
	    }
		else
		{
			if(this.mainDiv.width() <= 2) this.mainDiv.width(200);
			this.mainDiv.children().hide();
			this.currentTabName = tabName;
			this.tabs[_this.currentTabName].show();
			this.parent.splitterDiv.position("80%");
		}
		this.mainDiv.parent().trigger("resize");
		this.parent.panelStateChanged();
	};
	
	this.resize = function()
	{
		if(!isActiveDocument(this.parent)) return;
		var width = this.mainDiv.width();
		var height = this.mainDiv.height();
		if(width > 2 && !this.currentTabName)
		{
			this.currentTabName = _.find(["users", "history"], function(t)
			{
				return this.tabs[t].is(":visible");
			}, this);
			this.parent.panelStateChanged();
		}
		if(width <= 2)
		{
			this.currentTabName = "";
			this.parent.panelStateChanged();
		}
		
		this.mainDiv.find(".additionalTabPane").width(Math.max(0, width-10));
		
		this.chatPane.css('height', Math.max(0, height-this.chatPane.position().top - 45));
		this.changesPane.css('height', Math.max(0, height-this.changesPane.position().top - 20));
	};
	
	this.getUserColor = function(userID)
    {
    	var color = _this.colorMap[userID];
    	if(!color)
    	{
    		var r = Math.ceil( Math.random() * 255 );
    		var g = Math.ceil( Math.random() * 255 );
    		var b = Math.ceil( Math.random() * 255 );
    		var delta = Math.ceil(((r+g+b)-255)/3);
    		if(r > delta) r = r - delta; else r = 0; 
    		if(g > delta) g = g - delta; else g = 0; 
    		if(b > delta) b = b - delta; else b = 0; 
    		color = 'rgb('+r+','+g+','+b+')';
    		_this.colorMap[userID] = color;
    	}
    	return color;
    };
    
    this.initChat = function()
    {
    	if(window.messageConnector)
    	{
    		this.jid = toJIDString(_this.parent.completeName)+'@'+CONFERENCE_SERVER_PREFIX+chatPreferences.serverName;
    		this.groupName = this.jid.substring(0, this.jid.indexOf('@'));
    		window.messageConnector.createRoom(this.groupName);
    		
        	//add dialog as jabber connector listener
    		//TODO: remove listener when document closed
    		window.messageConnector.addListener(this.jid, this);
    	}
    };
    
    this.sendMessage = function(msg)
	{
		messageConnector.sendMsg(_this.jid, msg, 'groupchat');
		_this.chatInput.val('');
	};
    
    // MessageConnector listener implementation
	this.messageReceived = function(messageItem)
	{
		var timeStr = messageItem.date.toTimeString();
	    timeStr = timeStr.substring(0, timeStr.indexOf(' '));
	    if(messageItem.resource)
	    {
	    	var from = getUserByJID(messageItem.resource);
	    	var fromSpan = $('<span>'+from+'('+timeStr+'):</span>');
	    	fromSpan.css('color', _this.getUserColor(from)).css('font-weight', 'bold');
	    	var html = $('<div></div>');
	    	html.append(fromSpan).append('<br/>').append('<span>'+messageItem.msg+'</span>');
	    	_this.chatPane.append(html);
	    	_this.chatPane.scrollTop(_this.chatPane[0].scrollHeight);
	    }
	};
	
	this.initPanes();
}

Diagram.prototype = new DiagramSupport();
