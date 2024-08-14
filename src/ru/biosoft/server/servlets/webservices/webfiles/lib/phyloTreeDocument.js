/**
 * 
 * Open diagrams with BPMN modeler
 * 
 * @author anna
 */

function PhyloTreeDocument(completeName, isNew)
{
    this.completeName = completeName;
    this.name =  getElementName(this.completeName);
    
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
        //data-pdb='2POR' data-backgroundcolor='0xffffff' data-style='stick'
        var frameId = this.tabId+'_frame';
        var frame = '<iframe src="lib/phylotree/index.html?' + this.completeName+'&' + this.name + '" id="'+frameId+'"/>'
        this.textContainer = $(frame).css('width', '100%').css('height','100%').css('border', 'none');
        this.containerDiv.append(this.textContainer);
        documentDiv.append(this.containerDiv);
        resizeDocumentsTabs();
        
        var prevFunction;
        var bubbleIframeMouseMove = function( iframe ){
            
            if( prevFunction )
                iframe.contentWindow.removeEventListener('mousemove', prevFunction);
            prevFunction = function( event ) {
                var boundingClientRect = iframe.getBoundingClientRect();
                var evt = new CustomEvent( 'mousemove', {bubbles: true, cancelable: false});
                evt.clientX = event.clientX + boundingClientRect.left;
                evt.clientY = event.clientY + boundingClientRect.top;
                iframe.dispatchEvent( evt );
            };
            iframe.contentWindow.addEventListener('mousemove', prevFunction);
        };
        var i = 0;
        var myIframe = document.getElementById(frameId);
        setTimeout(function run() {
            bubbleIframeMouseMove(myIframe);
            i++;
            if( i < 600 )
                setTimeout(run, 100);
        }, 100);
    
    };
    
    
    this.close = function()
    {
    };
    
    this.isChanged = function()
    {
    	return false;
    };
    
    this.save = function(callback)
    {
        var _this = this;
    };
}
