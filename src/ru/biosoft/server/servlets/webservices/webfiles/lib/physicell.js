/**
 * Multicellular simulation result document
 */
function PhysicellDocument(completeName)
{
    var _this = this;
    this.completeName = null;
    this.diagramName = completeName;
    this.name = "Simulation result" + getElementName(completeName);
    this.tabId = allocateDocumentId(this.name + '/' + completeName);
    this.scrollPos = undefined;
    this.simulationName = undefined;
    this.loadedListeners = [];
    this.loaded = false;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        
        this.plotDocument = $('<div id="'+this.tabId+'"/>').css('padding', 0);
        this.loadingDiv = $('<div id="simulationLoadingDiv"><p><img height="16" width="16" src="images/trobber.gif"/>&nbsp;Creating simulation result...</p></div>');
        this.plotDocument.append(this.loadingDiv);
        this.plotDocumentContainer = $('<div id="' + this.tabId + '_container" class="documentTab"/>');
        this.plotDocument.append(this.plotDocumentContainer);
        parent.append(this.plotDocument);
        this.plotDiv = $('<div id="graph"></div>');
        this.plotDocumentContainer.append(this.plotDiv);

        this.createPhysicellDocument (function(){
            _this.loadingDiv.hide();
            _this.loaded = true;
            for (li = 0; li < _this.loadedListeners.length; li++)
            {
                _this.loadedListeners[li].simulationLoaded();
            }
            _this.loadedListeners = [];
            _this.update();
            _this.plotDocumentContainer.scroll(function() {
                _this.scrollPos = _this.plotDocumentContainer.scrollTop();
            }); 
            selectViewPart("physicell.result");
        });
        resizeDocumentsTabs();
        
    };
    
    this.update = function(callback)
    {
       queryBioUML("web/physicell/simulation_document_image",
       {
           de: _this.simulationName
       }, function(data)
       {
            _this.plotDiv.html("");
            for(var i=0; i < data.values.length; i++)
          {
               _this.plotDiv.append($('<img src="'+appInfo.serverPath+'web/img?de=' + data.values[i] + '&rnd=' + rnd() + '">'));
               _this.plotDiv.append($('<br>'));
           }
            
            if(_this.scrollPos > 0)
            {
               var curPos = _this.scrollPos;
               setTimeout(function(){_this.plotDocumentContainer.scrollTop(curPos);}, 250);
           }
           if( callback )
               callback();
        });
    }
    
    this.isChanged = function()
    {
        return false;
    }
    
    this.close = function()
    {
    }
    
    this.activate = function()
    {
        selectViewPart("physicell.result");
    };
    
    this.createPhysicellDocument = function(callback)
    {
        queryBioUML('web/physicell/physicell_document_create', 
            {
                de:_this.diagramName
            }, function(data) {
                _this.simulationName = data.values;
                
                
                if(callback)
                    callback();
            });
    };
    
    // Code to be called when simulation document is created
    this.addLoadedListener = function(listener)
    {
        if(this.loaded) // already loaded
            listener.simulationLoaded();
        else
            this.loadedListeners.push(listener);
    };
}

