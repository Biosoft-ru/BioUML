/* $Id: image.js,v 1.10 2013/08/21 05:57:14 lan Exp $ */
/**
 * Image document implementation
 * 
 * @author lan
 */
function ImageDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId("image/"+completeName);
    var dc = getDataCollection(this.completeName);
    dc.addRemoveListener(this);
    
    var _this = this;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var imageDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(imageDocument);
        
        this.imageContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        imageDocument.append(this.imageContainerDiv);
        
        imageDocument.resize(function()
        {
        });
        
        var id = rnd(); 
        
        var image = $("<img/>").css({margin: "5pt", border: "1px dotted black"}).attr("src", appInfo.serverPath+"web/img?"+toURI({rnd: id, de: this.completeName}));
        
        var setToRealSize = function(callback)
        {
          $("<img/>")
            .attr("src", image.attr("src"))
            .on("load", function() {
              image.width(this.width);
              image.height(this.height);
              if(callback) callback();
            });
        };

        var makeResizable = function() {
        	image.resizable({
        		minHeight: 50,
        		minWidth: 100,
        		stop: function()
        		{
        			image.attr("src", appInfo.serverPath+"web/img?"+toURI({rnd: id, de: _this.completeName, w: image.width(), h: image.height()}));
        		}
        	});
        };

        var onload = function() { setToRealSize(makeResizable); };
        
        if(image.width() > 0)
        	onload();
        else
        	image.on("load", onload);
        
        this.imageContainerDiv.append(image);
        
        //update document size
        resizeDocumentsTabs();
    };
    
    this.exportElement = function(value)
    {
        $.chainclude(
    	    {
    	        'lib/export.js':function(){
    				exportElement(_this.completeName, "Element");
    	        }
    	    }
        );
    };

    this.save = function(callback)
    {
    };
    
    this.close = function(callback)
    {
        if(callback) callback();
    };

    this.isChanged = function()
    {
    	return false;
    };

    this.dataCollectionRemoved = function()
    {
        closeDocument(this.tabId);
    };
}
