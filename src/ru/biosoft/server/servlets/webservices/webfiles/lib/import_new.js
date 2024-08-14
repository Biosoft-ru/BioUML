var uploadDialogDivTemplate = $('<div title="'+resources.dlgImportTitle+' (upload)">' +
			'<div id="fileInfoBlock" style="height: 60px; width: ' + defaultWidth + 'px;"><span>'+resources.dlgImportFile+'</span><span id="fileUpload"></span></div>'+
			'<div id="uploads"><table id="upload-list" class="table">'+
				'<thead id="upload-list-header">'+
					'<tr><th>Name</th><th>Upload progress</th><th></th><th></th></tr>'+
				'</thead>'+
				'<tbody>'+
				'</tbody></table>'+
				'<div class="dropFilesBlock">Drop files here</div>'+
			'</div>');
var rowTemplate = $('<tr id="file-row">'+
						'<td class="file-name" style="width:60px"></td>'+
						'<td>'+
							'<div class="progress"/>'+
						'</td>'+
						'<td>'+
							'<button class="import" style="width:70px;height:20px;display: none">Import</button>'+
						'</td>'+
					'</tr>');
var fileUploadDiv = $('<span id="fud" class="upload-div"><div class="fileUploadForm" title="'+resources.uploadFromComputerToolTip+'">'+
			'<div class="fileUploadText">'+resources.uploadFromComputerTitle+'</div>'+
			'<input type="file" name="file" id="fileUploadInput" class="fileUpload" multiple>'+
		'</div>'+
		'<div id="fileUrl" class="fileUploadForm" title="'+resources.uploadFromWebToolTip+'">'+
			'<div class="fileUploadText">'+resources.uploadFromWebTitle+'</div>'+
		'</div>'+
		'<div id="fileRepo" class="fileUploadForm" title="'+resources.uploadFromRepositoryToolTip+'">'+
			'<div class="fileUploadText">'+resources.uploadFromRepositoryTitle+'</div>'+
		'</div>'+
		'<div id="fileContent" class="fileUploadForm" title="'+resources.uploadFromContentToolTip+'">'+
			'<div class="fileUploadText">'+resources.uploadFromContentTitle+'</div>'+
		'</div></span>');
var progressBarTemplate = $("<div/>");

var defaultWidth = 520;
var uploadDialog; //we need this variable to close upload dialog when all imports are done

var vAutoOpen;
var importPath;
var inputPath;
var vCallback;
function doImportMultiple(path, autoOpen, callback)
{
	vCallback = callback;
	if (typeof(autoOpen)==='undefined') vAutoOpen = true;
	else vAutoOpen = autoOpen;

	inputPath = path;
    importPath = getImportPath(path);
    if( importPath == null )
    	return;

    doUpload();
}

function doUpload()
{
	var fud = fileUploadDiv.clone(true);
	var uploadDialogDiv = uploadDialogDivTemplate.clone(true);
	$(uploadDialogDiv).find('#fileUpload').replaceWith(fud);
	$(uploadDialogDiv).find('#upload-list-header').hide();
	$(uploadDialogDiv).find('.dropFilesBlock').on('dragover', false).on('dragenter',false).on('drop', function(event){
	    if(event.originalEvent.dataTransfer && event.originalEvent.dataTransfer.files.length)
	    {
	        event.preventDefault();
	        event.stopPropagation();
	        var files = event.originalEvent.dataTransfer.files;
	        addFiles(uploadDialogDiv, files);
	    }
	    else if(event.dataTransfer.files.length)
        {
	        event.preventDefault();
            event.stopPropagation();
            var files = event.dataTransfer.files;
            addFiles(uploadDialogDiv, files);
        }
	    return false;
	});
	
	uploadDialogDiv.dialog(
    {
        autoOpen: true,
        width: defaultWidth+30,
        height: 520,
        modal: true,
        open: function()
        {
        	var importAllBtn = $(":button:contains('Import all')");
        	importAllBtn.attr("title", "Import all files").removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled");

        	importsNumber = 0;
        	uploadsNumber = 0;
        	uploadDialog = this;
        	fud.find('input[type="file"]').change(function() {
					addFiles(uploadDialogDiv, this.files);
					fud.parent().remove();
                });
        	fud.find('#fileRepo').click(function() {
                createOpenElementsDialog(resources.dlgImportSelectFromRepo, "ru.biosoft.access.FileDataElement", [createPath(inputPath, "")], function(filePaths)
                {
                	uploadsNumber = filePaths.length;
                	for( var i = 0; i<uploadsNumber; i++ )
            		{
                	    var uploadID = rnd();
                	    var filePath = filePaths[i];
                	    var fileName = getElementName(filePath);
                        sendRepositoryUpload(uploadID, filePath);
                        addOtherUpload(uploadDialogDiv, uploadID, fileName);
            		}
                	fud.parent().remove();
                });
            });
        	fud.find('#fileUrl').click(function() {
        		var uploadID = rnd();
                getUploadURLDialog(uploadID, function(uploadName)
                {
                	uploadsNumber = 1;
                    var fileName = uploadName.replace(/^.+[\\\/]/, "");
                    addOtherUpload(uploadDialogDiv, uploadID, fileName);
                    fud.parent().remove();
                });
            });
        	fud.find('#fileContent').click(function() {
        		var uploadID = rnd();
                getUploadContentDialog(uploadID, function (uploadName)
                {
                	uploadsNumber = 1;
                	var fileName = uploadName.replace(/^.+[\\\/]/, "");
                    addOtherUpload(uploadDialogDiv, uploadID, fileName);
                    fud.parent().remove();
                });
            });
        },
        beforeClose: function()
		{
			activeUploads.forEach(cancelJob);
			activeUploads.forEach(uploadsToRemove.add, uploadsToRemove);
			allUploads.forEach(removeUploadedFile);

			activeUploads.clear();
			allUploads = [];
			waitForImport = [];
			uploadQueue = [];

			$(this).remove();
		},
        buttons: 
        {
            "Cancel": function()
            {
            	activeUploads.forEach(cancelJob);
            	activeUploads.forEach(uploadsToRemove.add, uploadsToRemove);
            	allUploads.forEach(removeUploadedFile);

            	activeUploads.clear();
            	allUploads = [];
            	waitForImport = [];
            	uploadQueue = [];

				$(this).dialog("close");
				$(this).remove();
            },
            "Import all": function()
            {
            	var failed = [];
            	var importNext = function(successCallback, failCallback) {
            		var next = waitForImport.shift();
        			if( next )
        				detectFormatAndImport(next, successCallback, failCallback);
        			else
        			{
        				//add upload IDs back at the end if import failed
        				for( var j = 0 ; j < failed.length; j = j + 1 )
        					waitForImport.push( failed[i] );
        			}
            	};
            	var fail = function(id){
            		failed.push(id);
            		importNext(success, fail);
            	};
            	var success = function(id){
            		if( importsNumber === uploadsNumber )
            		{
            			$(uploadDialog).dialog("close");
            			$(uploadDialog).remove();
            		}
            		else
            			importNext(success, fail);
            	};
            	importNext(success, fail);
            }
        }
    });
    addDialogKeys(uploadDialogDiv, null, "Import all");
    sortButtons(uploadDialogDiv);
}

var uploadsNumber;
var importsNumber;
var activeUploads = new Set();
var allUploads = [];
/*
 * Store uncompleted active uploads when close or cancel dialog. Uploaded files could be removed from server only when upload is finished.
 */
var uploadsToRemove = new Set();
/*
 * array of upload IDs that will be used if 'Import all' pressed
 */
var waitForImport = [];
function addFiles(uploadDialogDiv, files)
{
	uploadsNumber = files.length;
	for( var i=0; i<uploadsNumber; i=i+1 )
	{
		addLocalUpload(uploadDialogDiv, files[i]);
	}
	startNextUpload();
}

function removeUploadedFile(uploadID)
{
	var removeOptions = {
		type: "deleteUpload",
		fileID: uploadID
	};
	queryBioUML("web/import", removeOptions, function(data) {}, function(data) {});
}

function addFileRow(uploadDialogDiv, uploadID, fileName)
{
	$(uploadDialogDiv).find("#upload-list-header").show();
	var fileRow = rowTemplate.clone(true);
	fileRow.attr("id", uploadID);
	fileRow.find(".file-name").attr('title', fileName);
	var text = fileName;
	if( fileName.length > 16 )
		text = text.substring(0, 14) + "...";
	fileRow.find(".file-name").text(text);
	var progressBar = createUploadProgressBar();
	fileRow.find(".progress").replaceWith(progressBar);
	$(uploadDialogDiv).find("#upload-list > tbody").append(fileRow);
}

function createUploadProgressBar()
{
	var progressBar = progressBarTemplate.clone().attr('class', 'progress-bar');
	progressMeter = $('<span id="progressMeter"/>').css({float: 'left', margin: '0pt 2pt'}).width(300);
	progressText = $('<span id="progressText"/>');
	progressBar.data("progressMeter", progressMeter);
	progressBar.data("progressText", progressText);
	progressBar.append(progressMeter);
	progressBar.append(progressText);
    progressMeter.progressbar(
    {
        value: 0
    });
    progressText.text('0%');
    return progressBar;
}

function startUpload(uploadID, file)
{
	var data = new FormData();
	data.append('file', file);
	var xhr = new XMLHttpRequest();
	listenUploadProgress(uploadID, xhr);
	/* 
	 * id and action should be url parameters (not FormData parameter) since in
	 * the BioUML FormData parameters are processed only after file upload finishes
	 */
	xhr.open("POST", '/biouml/web/upload?fileID="'+ uploadID + '"&name="upload' + uploadID
			+ '" id="upload' + uploadID + '" enctype="multipart/form-data"');
	xhr.send(data);
	return xhr;
}
function listenUploadProgress(uploadID, xhr)
{
	xhr.upload.addEventListener('progress', function(e) {
		if (!e.lengthComputable)
			return;
		var percent = e.loaded * 100 / e.total;
		setProgressBar(uploadID, percent);
	}, false);
	// notice that the event handler is on xhr and not xhr.upload
	xhr.addEventListener('readystatechange', function(e) {
		if( this.readyState === 4 ) {
			// the transfer has completed and the server closed the connection.
		    if(isUploadPending(uploadID))
		        initImportButton(uploadID);
		    startNextUpload();
		}
	});
}
function setProgressBar(uploadID, percent)
{
	var text = percent.toFixed(0) + "%";
	if( percent === 100 )
		text = resources.commonProgressComplete;
	$("#"+uploadID+" .progress-bar").find('#progressText').text( text );
    $("#"+uploadID+" .progress-bar").find('#progressMeter').progressbar("value", percent);
}

var uploadQueue = [];
function startNextUpload()
{
	var nextUpload = uploadQueue.shift();
	if( nextUpload !== undefined && nextUpload !== null )
		startUpload(nextUpload.id, nextUpload.file);
}

function storeUpload(uploadID)
{
	//store all uploads to remove temporary files in the end
	allUploads.push(uploadID);
	//store active uploads to cancel them if they are still active when dialog is closed
	activeUploads.add(uploadID);
}

/**
 * Should be used to process upload of file from local computer
 */
function addLocalUpload(uploadDialogDiv, file)
{
	var uploadID = rnd();
	addFileRow(uploadDialogDiv, uploadID, file.name);
	storeUpload(uploadID);
	uploadQueue.push({'id' : uploadID, 'file': file});
}

/**
 * Should be used for upload by URL, raw data and importing from the repository 
 */
function addOtherUpload(uploadDialogDiv, uploadID, fileName)
{
    addFileRow(uploadDialogDiv, uploadID, fileName);
    storeUpload(uploadID);
    var progressBar = $("#"+uploadID+" .progress-bar");
    createProgressBar(progressBar, uploadID, function(status, message) {
		if(status == JobControl.COMPLETED)
		{
			initImportButton(uploadID);
		}
        else
		{
        	if(status == JobControl.TERMINATED_BY_ERROR) logger.error(resources.dlgImportErrorUploadFailed+"<br>"+message);
        	uploadDialogDiv.dialog("close");
        	uploadDialogDiv.remove();
		}
	});
}

function initImportButton(uploadID)
{
	var importSuccess = function(id){
		if( id )
		{
			var ind = waitForImport.indexOf(id);
			if( ind > -1 )
				waitForImport.splice(ind, 1);
		}
		if( importsNumber === uploadsNumber )
		{
			$(uploadDialog).dialog("close");
			$(uploadDialog).remove();
		}
	};
	var importBtn = $("#"+uploadID+" .import");
	importBtn.click( function() { detectFormatAndImport(uploadID, importSuccess); } );

	if( uploadsNumber === 1 )
		setTimeout(function(){importBtn.show(); $(importBtn).click();}, 1000);
	else
		importBtn.show();

	activeUploads.delete(uploadID);
	waitForImport.push(uploadID);
	//init 'Import all' button only if all uploads are finished
	if( activeUploads.size === 0 )
		$(":button:contains('Import all')").removeAttr("disabled").removeClass("ui-state-disabled");
}

//TODO: refactor
function detectFormatAndImport(uploadID, success, fail)
{
	var importID = rnd();
	var propertyPane;
	var fileName = $("#"+uploadID+" .file-name").attr('title');
	var importDialogDiv = $('<div title="'+resources.dlgImportTitle+'">' +
			'<div id="importTargetPaneBlock"></div><br>'+
			'<div id="importFileInfoBlock" style="height: 20px; width: '+ defaultWidth +'px;">'+resources.dlgImportImportingFile.replace("{file}", fileName.escapeHTML())+'</div><br>'+
			'<div id="importTypesBlock" style="position: relative; padding: 2px 0px;">'+resources.dlgImportFormat+' <select width="100%" id="formats"></select></div><br><br>' +
			'<div id="importOptionsBlock" style="overflow: auto; height: 300px; width: '+ defaultWidth +'px;"></div>'+
			'<div id="importHideBlock" class="disableCover" style="display: none;"></div>'+
			'</div>'+
		'</div>');
    var targetDps = convertJSONToDPS([
		{
			name: resources.dlgImportTargetFolder,
			displayName: resources.dlgImportTargetFolder,
			value: importPath,
			type: "data-element-path",
			elementMustExist: true,
			elementClass: "ru.biosoft.access.core.FolderCollection",
			canBeNull: false
		}
	]);

    var noOptionsDiv = $('<div style="color: gray; text-align: center; width: '+ defaultWidth +'px;">'+resources.dlgImportEmptyOptions+'</div>');
	var optionsBlock = importDialogDiv.find("#importOptionsBlock");
	optionsBlock.empty().append(noOptionsDiv);

    var noImportErrorMessage = importDialogDiv.find("#noImportErrorMessage");
    noImportErrorMessage.hide();

    var formats = importDialogDiv.find('#formats');
    var selectedFormat;
    var detectedFormat;

	var formatChangeHandler = function()
    {
    	selectedFormat = formats.val();
    	if(selectedFormat === "autodetect")
    	{
        	optionsBlock.empty().append(noOptionsDiv);
        	propertyPane = undefined;
        	return;
    	}
        var requestParameters =  {
                de: importPath,
                type: "properties",
                format: selectedFormat,
                jobID: importID,
                fileID: uploadID
            };
        queryBioUML("web/import", requestParameters, function(data)
        {
        	initPropertyInspectorFromJSON(data, uploadID, importID, selectedFormat, propertyPane, optionsBlock);
        }, function()
        {
        	optionsBlock.empty().append(noOptionsDiv);
        	propertyPane = undefined;
   		});
    };
    
	importDialogDiv.dialog(
	{
		autoOpen: true,
		width: defaultWidth+30,
		height: 520,
		modal: true,
		open: function()
		{
			$(":button:contains('Start')").removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled");
			formats.change(formatChangeHandler);
            queryBioUML("web/import",
    	    {
    	        de: importPath,
    	        type: "deInfo"
    	    }, function(data)
    	    {
    	    	initImportFormats(data.values, true, importDialogDiv);
    	    	formats.change();
    			detectImportFormat(uploadID, detectedFormat, selectedFormat, importDialogDiv);
    	    });
			var targetPropertyPane = new JSPropertyInspector();
			targetPropertyPane.setParentNodeId("importTargetPaneBlock");
			targetPropertyPane.setModel(targetDps);
			targetPropertyPane.generate();
			targetPropertyPane.addChangeListener(function(control, oldValue, newValue) {
				importPath = newValue;
				queryBioUML("web/import",
            	    {
            	        de: importPath,
            	        type: "deInfo"
            	    }, function(data)
            	    {
            	    	detectImportFormat(uploadID, detectedFormat, selectedFormat, importDialogDiv);
            	    });
			});
		},
		beforeClose: function()
		{
			$(this).remove();
		},
		buttons: 
		{
			"Cancel": function()
			{
				if( waitForImport.indexOf(uploadID) == -1 )
					waitForImport.push(uploadID);
				if (importID != undefined) 
				{
					cancelJob(importID);
				}
				$(this).dialog("close");
				$(this).remove();
			},
			"Start": function()
			{
				importDialogDiv.find("#importHideBlock").show();
            	$(":button:contains('Start')").removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled");
            	setProjectByCollection(getDataCollection(importPath));
            	doImport(selectedFormat, uploadID, importID, propertyPane, importDialogDiv, success, fail);
			}
		}
	});
    addDialogKeys(importDialogDiv, null, "Start");
    sortButtons(importDialogDiv);
};

function doImport(selectedFormat, uploadID, importID, propertyPane, importDialogDiv, success, fail)
{
    var importProperties = {
            de: importPath,
            type: "import",
            format: selectedFormat,
            fileID: uploadID,
            jobID: importID
        };
    if(propertyPane)
    {
        propertyPane.updateModel();
        importProperties.json = convertDPSToJSON(propertyPane.getModel());
    }
	queryBioUML("web/import", importProperties, function(data) {}, function(data)
	{
    	logger.error(resources.dlgImportErrorImportFailed+"<br>"+data.message);
		importDialogDiv.dialog("close");
		importDialogDiv.remove();
	});
	var progressBar = progressBarTemplate.clone(true);
	importDialogDiv.find("#importFileInfoBlock").empty().append(progressBar);
	createProgressBar(progressBar, importID, function(status, msg, pathsToOpen) 
	{
		if (status == JobControl.COMPLETED) 
		{
			$("#"+uploadID+" .import").removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled").html("Imported");
			importsNumber = importsNumber + 1;
			importDialogDiv.dialog("close");
			importDialogDiv.remove();
			if(vAutoOpen)
				reopenDocument(pathsToOpen[0]);
			refreshTreeBranch(getElementPath(pathsToOpen[0]));
			openBranch(getElementPath(pathsToOpen[0]), false);
			if( success )
				success(uploadID);
            if( vCallback )
            	vCallback(pathsToOpen);
		} else
		{
        	if(status == JobControl.TERMINATED_BY_ERROR) logger.error(resources.dlgImportErrorImportFailed+"<br>"+msg);
			importDialogDiv.dialog("close");
			importDialogDiv.remove();
			if( fail )
				fail(uploadID);
		}
	});
};

function initPropertyInspectorFromJSON(data, uploadID, importID, selectedFormat, propertyPane, optionsBlock)
{
    optionsBlock.empty();
    
    var beanDPS = convertJSONToDPS(data.values);
    
    propertyPane = new JSPropertyInspector();
    propertyPane.setParentNodeId("importOptionsBlock");//name should be variable
    propertyPane.setModel(beanDPS);
    propertyPane.generate();
	propertyPane.addChangeListener(function(control, oldValue, newValue) {
		propertyPane.updateModel();
        var json = convertDPSToJSON(propertyPane.getModel(), control);
        var requestParameters =  {
            de: importPath,
            type: "properties",
            format: selectedFormat,
            jobID: importID,
            json: json,
            fileID: uploadID
        };
        queryBioUML( "web/import", requestParameters, function(data) {
        	initPropertyInspectorFromJSON(data, uploadID, importID, selectedFormat, propertyPane, optionsBlock);
    	} );
	});
};

function initImportFormats(importers, addAutoDetect, importDialogDiv)
{
	var noImportErrorMessage = importDialogDiv.find("#noImportErrorMessage");
	var fileInfoBlock = importDialogDiv.find("#importFileInfoBlock");
	var formats = importDialogDiv.find('#formats');

	formats.empty();
	if(addAutoDetect)
	{
		formats.append($('<option/>').val("autodetect").text("autodetect"));
	}
	var hasFormat = false;
    for (i = 0; i < importers.length; i++) 
    {
        if (importers[i].format.length > 0) 
        {
        	hasFormat = true;
            formats.append($('<option/>').val(importers[i].format).text(importers[i].displayName));
        }
    }
    if(hasFormat)
    {
    	fileInfoBlock.show();
    	noImportErrorMessage.hide();
    	$(":button:contains('Start')").removeAttr("disabled").removeClass("ui-state-disabled");
    } else
    {
    	fileInfoBlock.hide();
    	noImportErrorMessage.show();
    	formats.empty().prepend($('<option/>').val("autodetect").text("autodetect"));
    	$(":button:contains('Start')").removeAttr("disabled").attr("disabled", "disabled").addClass("ui-state-disabled");
    }
};

function detectImportFormat(uploadID, detectedFormat, selectedFormat, importDialogDiv)
{
	queryBioUML("web/import", {
        de: importPath,
        type: "detect",
        fileID: uploadID
    }, function(data)
    {
    	var formats = importDialogDiv.find('#formats');

    	detectedFormat = data.values[0].format;
    	var availableFormats = {};
    	for(var i=0; i<data.values.length; i++) availableFormats[data.values[i].format] = true;
    	if(!availableFormats[selectedFormat]) selectedFormat = detectedFormat;
    	initImportFormats(data.values, false, importDialogDiv);
    	var detected = formats.children().eq(0);
    	detected.text(detected.text()+" "+resources.dlgImportDetectedFormat);
		formats.val(selectedFormat);
		formats.change();
    });
};

function getImportPath(path)
{
	var importPath = null;
	if(path)
		importPath = path;
	else
	{
		if(!activeDC || !activeDC.selectedElement)
		{
			logger.error(resources.dlgImportErrorNoCollection);
			return null;
		}
		importPath = getTargetPath(activeDC.selectedElement);
	}
	var genericParent = importPath;
	while(genericParent != "" && !instanceOf(getElementClass(genericParent), "ru.biosoft.access.core.FolderCollection"))
	{
		genericParent = getElementPath(genericParent);
	}
	if(genericParent != "") 
	    importPath = genericParent;
	else
	{ 
	    logger.error(resources.dlgImportErrorNoImportersForCollection);
	    return null;
	}
	return importPath;
}

function isUploadPending(uploadID)
{
    if(uploadsToRemove.has(uploadID))
    {
        uploadsToRemove.delete(uploadID);
        removeUploadedFile(uploadID);
        return false;
    }
    return true;
}
