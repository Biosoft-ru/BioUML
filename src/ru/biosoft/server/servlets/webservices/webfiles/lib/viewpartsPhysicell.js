/**
 * Viewparts for Physicell document
 */
function initPhysicellViewParts()
{
    if(viewPartsInitialized['physicell'])
        return;
    
    viewParts.push(new PhysicellEditorViewpart());    
    viewPartsInitialized['physicell'] = true;
}

function PhysicellEditorViewpart()
{
    createViewPart(this, "physicell.result", "Physicell editor");
    var _this = this;
    this.lockDiv = $('<div id="lock_physicell_bean" class="ui-widget-overlay" style="position:absolute; top:30px; left:0; z-index:1001;"></div>');
	this.controlDiv = $('<div></div>');
	this.containerDiv.append(this.controlDiv);
    this.containerDiv.append(this.lockDiv);
    //TODO: move messages to messageBundle.js
    
    this.isVisible = function(documentObject, callback)
    {
         if((documentObject != null) && (documentObject instanceof PhysicellDocument))
             callback(true);
         else
             callback(false);
    };
    
	this.explore = function(documentObject)
	{
		 if ((documentObject != null) && (documentObject instanceof PhysicellDocument))
		 {
		     this.physicell = documentObject;
             this.physicell.addLoadedListener(_this);
             this.lockDiv.hide();
	     }
	};
	  
    this.initOptions = function(data)
    {
        _this.propertyInspector.empty();

        if (data.type == 0) 
        {
        	var beanDPS = convertJSONToDPS(data.values);
            _this.propertyPane = new JSPropertyInspector();
            _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
            _this.propertyPane.setModel(beanDPS);
            _this.propertyPane.generate();
            _this.propertyPane.addChangeListener(function(ctl,oldval,newval) {
                _this.propertyPane.updateModel();
                var json = convertDPSToJSON(_this.propertyPane.getModel(), ctl.getModel().getName());
                _this.setOptionsFromJson(json);
            });  	       
        }
    };
    
    this.simulationLoaded = function()
    {
        _this.propertyInspector = $('<div id="' + _this.tabId + '_pi"></div>');
        _this.controlDiv.html(_this.propertyInspector);        
        _this.controlDiv.show();
        queryBean("physicell/result/"+_this.physicell.simulationName, {showMode: SHOW_EXPERT}, _this.initOptions);
    };
    
    this.show = function(documentObject) //[Optional]
    { 
    };
    
    this.save = function() 
    {
    };
    
    this.initActions = function(toolbarBlock)
    {
		this.applyAction = createToolbarButton("Apply", "apply.gif", this.applyActionClick);
	    toolbarBlock.append(this.applyAction);
        this.playAction = createToolbarButton("Play", "simulate.gif", this.playActionClick);
        toolbarBlock.append(this.playAction);
        this.pauseAction = createDisabledToolbarButton("Pause", "pause.gif", this.pauseActionClick);
        toolbarBlock.append(this.pauseAction);
		this.recordAction = createToolbarButton("Record", "record.gif", this.recordActionClick);
	    toolbarBlock.append(this.recordAction);
		this.recordStopAction = createDisabledToolbarButton("Stop record", "recordStop.gif", this.recordStopActionClick);
	    toolbarBlock.append(this.recordStopAction);
		this.rotateLeftAction = createToolbarButton("Rotate left", "redo.gif", this.rotateLeftActionClick);
	    toolbarBlock.append(this.rotateLeftAction);
		this.rotateRightAction = createToolbarButton("Rotate right", "undo.gif", this.rotateRightActionClick);
		toolbarBlock.append(this.rotateRightAction);
        //this.stopAction = createDisabledToolbarButton("Stop", "stopTask.gif", this.stopActionClick);
        //toolbarBlock.append(this.stopAction);
    };
    
	this.applyActionClick = function()
	{
		var dps = _this.propertyPane.getModel();
		var json = convertDPSToJSON(dps);
		queryBioUML("web/physicell/physicell_document_image", 
		{
		      de: _this.physicell.simulationName,
		      options: json
		}, function(data)
		{
		      _this.physicell.draw(data);
		});
	}
	
    this.playActionClick = function()
    {
		setToolbarButtonEnabled(_this.playAction, false);
	    setToolbarButtonEnabled(_this.pauseAction, _this.stopAction, true);
        _this.lockDiv.show();
        _this.physicell.play = true;
		_this.physicell.autoUpdate(_this.piUpdate);
        _this.piUpdate();
    };
    
    this.piUpdate = function()
    {
        queryBean("physicell/result/"+_this.physicell.simulationName, {showMode: SHOW_EXPERT}, _this.initOptions);
    }
    
	this.pauseActionClick = function()
	{
		_this.physicell.play = false;
        _this.lockDiv.hide();
        if(_this.piUpdateTimer)
            clearTimeout(_this.piUpdateTimer);
		setToolbarButtonEnabled(_this.playAction, true);
		setToolbarButtonEnabled(_this.pauseAction, _this.stopAction, false);
	};
	 
	this.stopActionClick = function()
	{
        _this.physicell.stopUpdate();
	};
	
	this.recordActionClick = function()
	{
	    setToolbarButtonEnabled(_this.recordAction, false);
		setToolbarButtonEnabled(_this.recordStopAction, true);
		queryBioUML("web/physicell/record", 
		{
			de: _this.physicell.simulationName
		}, function(data){});
	}
	 
	 this.recordStopActionClick = function()
	 {
	 	setToolbarButtonEnabled(_this.recordStopAction, false);
	    setToolbarButtonEnabled(_this.recordAction, true);
		queryBioUML("web/physicell/record_stop", 
		{
			de: _this.physicell.simulationName
		}, function(data){});
	 };
	 
	 this.rotateLeftActionClick = function()
	 {
		var dps = _this.propertyPane.getModel();
		var json = convertDPSToJSON(dps);
	 	queryBioUML("web/physicell/rotate_left", 
	 	{
	 		de: _this.physicell.simulationName,
			options: json
	 	}, function(data)
		{
			_this.physicell.draw(data);
            _this.piUpdate();
		});
	 }
	 
	 this.rotateRightActionClick = function()
     {
		var dps = _this.propertyPane.getModel();
		var json = convertDPSToJSON(dps);
        queryBioUML("web/physicell/rotate_right", 
		{
		 	de: _this.physicell.simulationName,
			options: json
		}, function(data)
		{
			_this.physicell.draw(data);
            _this.piUpdate();
		});
	}
	
    this.setOptionsFromJson = function(json)
    {
        var beanPath = "physicell/result/"+_this.physicell.simulationName;
        queryBioUML("web/bean/set",
            {
                de: beanPath,
                json: json
            }, 
            _this.initOptions);
    };
}