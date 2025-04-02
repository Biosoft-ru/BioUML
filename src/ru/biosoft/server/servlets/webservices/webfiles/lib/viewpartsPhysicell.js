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
	this.controlDiv = $('<div></div>');
	this.containerDiv.append(this.controlDiv);
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
        //this.stopAction = createDisabledToolbarButton("Stop", "stopTask.gif", this.stopActionClick);
       // toolbarBlock.append(this.stopAction);
    };
    
	this.applyActionClick = function()
	{
		var dps = _this.propertyPane.getModel();
		var json = convertDPSToJSON(dps);
		queryBioUML("web/physicell/physicell_document_image", 
		{
		      de: _this.physicell.simulationName,
		      options: json,
		}, function(data)
		{
		      _this.physicell.draw(data);
		});
	}
	
    this.playActionClick = function()
    {
		setToolbarButtonEnabled(_this.playAction, false);
	    setToolbarButtonEnabled(_this.pauseAction, _this.stopAction, true);
        _this.physicell.play = true;
		_this.physicell.autoUpdate();
    };
    
	this.pauseActionClick = function()
	{
		_this.physicell.play = false;
		setToolbarButtonEnabled(_this.playAction, true);
		setToolbarButtonEnabled(_this.pauseAction, _this.stopAction, false);
	};
	 
	this.stopActionClick = function()
	{
        _this.physicell.stopUpdate();
	};
    
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