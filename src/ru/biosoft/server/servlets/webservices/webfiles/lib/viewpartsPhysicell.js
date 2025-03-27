/**
 * Viewparts for Interactive Simulation document
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
    //this.selectionDiv = $('<div></div>').css("margin-bottom", "10px");
    //this.containerDiv.append(this.selectionDiv);
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
	    		
	  	//if(this.currentDiagram)
	  	//{
	  	//	this.currentDiagram.removeSelectListener(this);
	  	//}
	  	//if ((documentObject != null) && documentObject instanceof Diagram ) 
	      //{
	  		//this.currentDiagram = documentObject.getDiagram();
	  		//this.currentDiagram.addSelectListener(this);
	    //      if( this.currentDiagram.runMode == undefined )
	    //          this.currentDiagram.runMode = false;
	  		
	  		 this.propertyInspector = $('<div id="' + _this.tabId + '_pi"></div>');
	         this.controlDiv.html(_this.propertyInspector);
			 
			 _this.controlDiv.show();
			 //queryBean("physicell/result/Simulation |||| " +documentObject.diagramName+" |||| "+ documentObject.name +"||||"+documentObject.simulationName, {showMode: SHOW_EXPERT}, this.initOptions);
			 queryBean("physicell/result/"+documentObject.simulationName, {showMode: SHOW_EXPERT}, this.initOptions);
	         //this.showStopped();
	      }
	  };
	  
	  this.initOptions = function(data)
	      {
	  		_this.propertyInspector.empty();
	  		_this.selectNodeProperty();
	  	    if (data.type == 0) 
	  	    {
	  	    	var beanDPS = convertJSONToDPS(data.values);
	  	        _this.propertyPane = new JSPropertyInspector();
	  	        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
	  	        _this.propertyPane.setModel(beanDPS);
	  	        _this.propertyPane.generate();  	       
	  	    }
	      };
    
    this.simulationLoaded = function()
    {
        //this.loadTable();
    };
    
    this.show = function(documentObject) //[Optional]
    { 
    };
    
    this.save = function() 
    {
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.playAction = createToolbarButton("Play", "simulate.gif", this.playActionClick);
        toolbarBlock.append(this.playAction);
        this.pauseAction = createToolbarButton("Pause", "pause.gif", this.pauseActionClick);
        toolbarBlock.append(this.pauseAction);
        this.stopAction = createToolbarButton("Stop", "stopTask.gif", this.stopActionClick);
        toolbarBlock.append(this.stopAction);
    };
    
    this.playActionClick = function()
    {
		//queryBioUML("web/physicell/timestep",
		//           {
		//               de: _this.physicell.simulationName,
		////               //jsonrows: $.toJSON(rows)
		//           }, function(data)
		//           {
		               _this.physicell.autoUpdate();
		//           });
    };
    
	this.pauseActionClick = function()
	{
	    
	};
	 
	this.stopActionClick = function()
	{
	     
	};
}