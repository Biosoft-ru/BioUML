/**
 * Document which can be represented by browser itself (text file/html file/image/etc.).
 * Will be opened in the iFrame
 * 
 * @author anna
 */
function WebDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId(completeName);
    var _this = this;
    this.type = "text";
    this.scrollPos = undefined;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var documentDiv = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(documentDiv);
        
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        
        var dc = getDataCollection(getElementPath(this.completeName));
        var type = dc.getChildClass(this.name);
        var frameId = this.tabId+'_frame';
        var frame = '<iframe src="'+appInfo.serverPath+'web/content/'+this.completeName+'" id="'+frameId+'"/>'
        this.textContainer = $(frame).css('width', '100%').css('height','100%').css('border', 'none');
        this.containerDiv.append(this.textContainer);
        documentDiv.append(this.containerDiv);
        resizeDocumentsTabs();
        
        var scrollIframe = function(iframe, prevScrollFunction)
        {
            if(iframe.contentWindow == null)
                return;
        	if( prevScrollFunction )
        		iframe.contentWindow.removeEventListener('scroll', prevScrollFunction);
        	var scrollFunction = function( event ) {
	        	var scrollPrev = _this.scrollPos;
	            var pageYOffset = this.pageYOffset;
	          	if( pageYOffset != 0 || pageYOffset==0 && scrollPrev<200) //dirty hack to avoid scroll to top when frame is going to hide
	          		_this.scrollPos = pageYOffset;
        	};
            iframe.contentWindow.addEventListener('scroll', scrollFunction);
            return scrollFunction;
        }
        
        var prevMoveFunction = null;
        var prevScrollFunction = null;
        
        var $iframe = $('#'+frameId);
        $iframe.on("load", function() {
            var myIframe = $iframe.get(0); 
            prevScrollFunction = scrollIframe(myIframe, prevScrollFunction);
            //prevMoveFunction = addIframeMouseMoveListener(myIframe, prevFunction);
            addIframeListener(myIframe, 'focus');
            addIframeListener(myIframe, 'blur');
            var cssLink = $("<link>").attr("href", "/bioumlweb/css/iframe_extra.css").attr("rel", "stylesheet").attr("type", "text/css");
            $iframe.contents().find("body").append(cssLink);
            
        });
    };
    
    this.close = function()
    {
    };
    
    this.isChanged = function()
    {
    	return false;
    };

    this.exportElement = function(value)
    {
		var _this = this;
	    $.chainclude(
		    {
		        'lib/export.js':function(){
					exportElement(_this.completeName, "Element");
		        }
		    }
	    );
    };

    this.saveAs = function(newPath, callback)
    {
        var _this = this;
        queryBioUML("web/doc/save", { de: _this.completeName, newPath: newPath }, function(data)
        {
        	if(callback) callback(data);
        }, function(data)
        {
        	if(callback) callback(data);
        });
    };
  
    this.activate = function()
    {
    	var frameId = this.tabId+'_frame';
    	var myIframe = document.getElementById(frameId);
    	if(this.scrollPos > 0)
    		myIframe.contentWindow.scrollTo(0, this.scrollPos);
    };
}
