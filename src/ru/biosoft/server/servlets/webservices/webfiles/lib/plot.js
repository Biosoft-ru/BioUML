/**
 * Plot document for web
 * 
 * @author anna
 */

function PlotDocument(completeName)
{
    var _this = this;
	this.completeName = completeName;
	this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId("plot_"+completeName);
    this.viewAreaListeners = new Array();
    
	this.open = function(parent)
	{
        opennedDocuments[this.tabId] = this;
        this.plotDocument = $('<div id="'+this.tabId+'"/>').css('padding', 0);
        this.plotDocumentContainer = $('<div id="' + this.tabId + '_container" class="documentTab"/>');
        this.plotDocument.append(this.plotDocumentContainer);
        parent.append(this.plotDocument);
        this.titleDiv = $('<h3></h3>').css("margin", "5pt");
        getDataCollection(completeName).getBeanFields('title', function(result){
            if (result) 
            {
                var title = result.getValue('title');
                if (! title) 
                {
                    title = getElementName(completeName);
                }
                _this.titleDiv.html(title);
            }
        });
        //this.plotDocumentContainer.append(this.titleDiv);
        this.plotDiv = $('<div id="graph"></div>');
        this.plotDocumentContainer.append(this.plotDiv);
        this.update();
        resizeDocumentsTabs();
	};
    
    this.update = function(callback)
    {
        queryBioUML("web/plot",
        {
            action: "plot",
            de: _this.completeName
        }, function(data)
        {
            _this.plotDiv.html('<img src="'+appInfo.serverPath+'web/img?de=' + data.values.image + '&rnd=' + rnd() + '">');
            if(data.values.needUpdate)
            {
                setTimeout(function(){_this.update(callback);}, 2000);
            }
            if(data.values.path)
                _this.setAdditionalPath(data.values.path);
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
    
    this.save = function(callback)
    {
        var _this = this;
        queryBioUML("web/plot/save",
        {
            "de": _this.completeName,
            "plot_path" : _this.completeName
        }, callback);
    };
    
    /**
     * Set simulation result path if plot was created from simulation result. Will be used for PlotEditor viewpart 
     */
    this.setAdditionalPath = function(path)
    {
        if(this.additionalPath == path)
            return;
        
        this.additionalPath = path;
        var property = new DynamicProperty("defaultSource", "data-element-path", path);
        var jsonObj = [convertPropertyToObject(property)];
        queryBioUML("web/bean/set",
        {
            "de": _this.completeName,
            json: $.toJSON(jsonObj)
        }, function(){});
    };
    
   
}
