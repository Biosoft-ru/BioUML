/**
 * 
 * Open diagrams with BPMN modeler
 * 
 * @author anna
 */

function bpmnDocument(completeName, isNew)
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
        var canvasId = this.tabId + '_canvas';
        this.canvasDiv = $('<div id="' + canvasId + '"></div>').css('width', '100%').css('height','100%').css('border', 'none');
        this.containerDiv.append(this.canvasDiv);
        documentDiv.append(this.containerDiv);
        resizeDocumentsTabs();

        // BpmnJS is the BPMN viewer/modeler instance
        this.modeler = new BpmnJS({ container: '#' + canvasId });
        //TODO: move to bpmn provider
        queryBioUML("web/doc/getcontent",
        {
             type: "json",
             de: _this.completeName
        }, function(data)
        {
            loadBpmnDiagram(data.values, _this.modeler, canvasId, function(){
                createTreeItemDroppable(_this.containerDiv, null, function(path,event) 
                {
                     _this.addDataElement(path, event);
                });
            });
        });
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
        _this.saveAs(_this.completeName, callback);
    };

//    this.exportElement = function(value)
//    {
//		var _this = this;
//	    $.chainclude(
//		    {
//		        'lib/export.js':function(){
//					exportElement(_this.completeName, "Element");
//		        }
//		    }
//	    );
//    };
//
    this.saveAs = function(newPath, callback)
    {
        saveBpmnDiagram(_this.modeler, function(xmlString){
            //var xmlString = (new XMLSerializer()).serializeToString(xmlData[0]);
            queryBioUML("web/bpmn/save", 
            { 
                de: _this.completeName,
                newPath: newPath,
                data: xmlString
            }, function(data)
            {
                if(callback) callback(data);
            }, function(data)
            {
                if(callback) callback(data);
            });
        })
    };
    
    //Add data element as DataReferenceObject to bpmn diagram
    this.addDataElement = function(path, event)
    {
        var ex = event.pageX - _this.containerDiv.offset().left + _this.containerDiv.scrollLeft();  
        var ey = event.pageY - _this.containerDiv.offset().top + _this.containerDiv.scrollTop();
        
        const modeler = _this.modeler;
        
        const elementFactory = modeler.get('elementFactory'),
        elementRegistry = modeler.get('elementRegistry'),
        modeling = modeler.get('modeling'),
        moddle = modeler.get('moddle');
        const process = modeler.get('canvas').getRootElement();//elementRegistry.get('Process_1');
        
        //var dataObj = moddle.create('bpmn:DataObject', {"name":"BBB2", "isCollection":true});
        //console.log(dataObj);
        //dataObj.name = getElementName(path)+"_data";
        //var dataObj = elementFactory.createShape({type:'bpmn:DataObject',"name":"BBB", "isCollection":true});
        //dataObj.businessObject["name"] = "BBBBB";
        //process.businessObject.flowElements.push(dataObj);
        //modeling.createShape(dataObj, { x: ex+100, y: ey+100 }, process);
        //console.log("dataObject " + dataObj.id);
        
        var property = {
                type: 'bpmn:DataObjectReference',
                //dataObjectRef: dataObj,
                name : 'DataObjectReference' 
            };
        var dataObjRef = elementFactory.createShape(property);
        //name propery is ignored when creating with elementFactory. .businessObject["name"] and modeling.updateProperties allow to set name
        dataObjRef.businessObject["name"] = getElementName(path);
        
        //dataObjRef.name = getElementName(path);
        modeling.createShape(dataObjRef, { x: ex, y: ey }, process);
        //manipulate automaticaly created DataObject 
        //it could be accessed via dataObjRef.businessObject.dataObjectRef
        //store custom info in $attrs property map
        dataObjRef.businessObject.dataObjectRef.$attrs["de_path"] = path;
        //var dataObjId = dataObjRef.businessObject["dataObjectRef"].id;
        
        //TODO: unique ID
        //var dataObj = moddle.create('bpmn:DataObject', {"id": "DOID_" +  getElementName(path), "name":"BBB2", "isCollection":false, "depath" : path});
        //process.businessObject.flowElements.push(dataObj); //this step is required to include DataObject to XML
        //console.log(dataObj);
        
        //console.log(dataObjRef);
        
        modeling.updateProperties(dataObjRef, {name: getElementName(path)});
        //modeling.updateProperties(dataObjRef, {name: getElementName(path), dataObjectRef: dataObj});
        //now XML will contain one extra dummy DataObject created along with DataObjectReference. 
        //it is not referenced by any DOReference and will be eliminated at next read/write cycle
        
        //console.log(dataObjRef);
        //console.log(elementRegistry.get(dataObjRef.id));
    };

}

async function loadBpmnDiagram(bpmnXML , modeler, canvasId, callback)
{
    // import a BPMN 2.0 diagram
    try {
        // imported
        await modeler.importXML(bpmnXML);
        modeler.get('canvas').zoom('fit-viewport');
        console.log('Imported XML');
        if(callback)
            callback();
      } 
    catch (err) {
        // import failed
          console.error(err);
      }
};

async function saveBpmnDiagram (modeler, callback)
{
    try {
        const { xml } = await modeler.saveXML({ format: true });
        if(callback)
            callback(xml);
        console.log('Saved XML');
    } 
    catch (err) {
        console.error(err);
    }
};