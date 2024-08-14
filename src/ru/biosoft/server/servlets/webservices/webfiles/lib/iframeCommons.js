var types = {
		'blur' : updateOnRefocus,
		'focus' : updateOnRefocus
};

function addIframeListener(iframe, type) {
	var listener = types[type];
	if( !iframe || !listener )
		return;

	iframe.contentWindow.removeEventListener(type, listener);
	iframe.contentWindow.addEventListener(type,	listener);
}

function addIframeMouseMoveListener(iframe, prevFunction) {
	if( !iframe || !iframe.contentWindow)
		return;
	var type = 'mousemove';
	if( prevFunction )
		iframe.contentWindow.removeEventListener(type, prevFunction);
	var mousemoveListener = function(event) {
		var boundingClientRect = iframe.getBoundingClientRect();
		var evt = new CustomEvent( type, {bubbles: true, cancelable: false});
		evt.clientX = event.clientX + boundingClientRect.left;
		evt.clientY = event.clientY + boundingClientRect.top;
		iframe.dispatchEvent( evt );
	};
	iframe.contentWindow.addEventListener(type,	mousemoveListener);
	return mousemoveListener;
}
