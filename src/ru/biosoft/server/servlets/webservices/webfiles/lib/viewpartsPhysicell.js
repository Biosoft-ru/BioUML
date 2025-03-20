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
    this.selectionDiv = $('<div></div>').css("margin-bottom", "10px");
    this.containerDiv.append(this.selectionDiv);
    //this.table = $('<div>'+resources.commonLoading+'</div>');
    //this.containerDiv.append(this.table);
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
            if (this.physicell != documentObject) 
            {
                this.physicell = documentObject;
                //this.tableObj = undefined;
                //this.selectedParameters=[];
                //this.simulation.addLoadedListener(this);
            }
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
		queryBioUML("web/physicell/timestep",
		           {
		               de: _this.physicell.simulationName,
		               //jsonrows: $.toJSON(rows)
		           }, function(data)
		           {
		               _this.physicell.update();
		           });
    };
    
	this.pauseActionClick = function()
	{
	    
	};
	 
	this.stopActionClick = function()
	{
	     
	};
}