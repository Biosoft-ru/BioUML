
// Rewrite addModificationListener from ModificationListener.js 
addModificationListener = function( HTMLNode, func )
{
    if( HTMLNode.type === "checkbox" )
    { // check box
        prevValues [HTMLNode.id ] = HTMLNode.checked;
    }
    else if( HTMLNode.type === "select-one" )
    { //select control
        HTMLNode.onfocus = onFocus;
    }
    else
    { //other control
        HTMLNode.onfocus = onFocus;
        HTMLNode.onblur = onBlur;
    }
    HTMLNode.onchange = onChange;
    if(HTMLNode.type == "text")
    {
    	HTMLNode.onblur = onChange;
    	HTMLNode.onchange = null;
    	HTMLNode.onfocus = function(e)
    	{
    	    focusedComponent = getComponent(e);
    	    prevValues [focusedComponent.id ] = focusedComponent.value;
    	};
    }
    if( callbacks[ HTMLNode.id ] == null )
    {
        callbacks[ HTMLNode.id ] = [];
    }
    var listeners = callbacks[ HTMLNode.id ];
    listeners[ listeners.length++ ] = func;

    logByClass("LISTENERS", "Adding modification listener for " + HTMLNode.id + ", now " + listeners.length + " listeners.")
};

//Rewrite createControl for some BioUML-specific controls
var createCommonControl = ControlFactory.createControl;

ControlFactory.createControl = function( property, format, changeListener )
{
	if( property == null )
    {
        error( "ControlFactory.createControl: property is null" );
        return null;
    }
    var control = null;
    if( property.getType() == "data-element" )	// BioUML only
    {
        control = new JSDataElementSelector(property, format);
    }
    else if( property.getType() == "data-element-path" )	// BioUML only
    {
        control = property.isReadOnly()?new JSEditBox(property, format):new JSDataElementPathEditor(property, format);
    }
    else if( property.getType() == "uploaded-file" )	// BioUML only
    {
        control = new JSFileUploader(property, format);
    }
    else if( property.getType() == "workflow-expression" )	// BioUML only
    {
        control = new JSWorkflowExpressionEditBox(property, format);
    }
    else if( property.getType() == "text-script" )  //BioUML only
    {
        control = new JSTextScriptEditBox(property, format);
    }
    else if( property.getType() == "url" )  //BioUML only
    {
        control = new JSUrlBuilder(property, format);
    }
    else if( property.getType() == "collection" )  //BioUML only
    {
        control = new JSArrayControl(property, format);
    }
    
    if(control != null)
    {
        if( changeListener != null )
        {
            control.addChangeListener(changeListener);
        }
        return control;
    }
    else
    {
        return createCommonControl(property, format, changeListener);
    }
};

// Additional method for listeners removal
JSControl.prototype.removeChangeListener = function( listener )
{
    for( var i = this.changeListeners.length -1; i >= 0 ; i-- )
        if(this.changeListeners[i] == listener)
          this.changeListeners.splice(i,1);
}
