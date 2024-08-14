/* $Id: sequence.js,v 1.91 2013/08/09 06:26:07 ivan Exp $ */
/**
 * JavaScript SequenceDocument
 *
 * @author lan
 */
function SequenceDocument(completeName, paramHash, customId)
{
    var _this = this;
    var dc = getDataCollection(completeName);
    if(instanceOf(dc.getClass(),"ru.biosoft.bsa.project.Project"))
    {
        this.completeName = completeName;
        this.projectName = completeName;
    } else if(instanceOf(dc.getClass(), "ru.biosoft.bsa.AnnotatedSequence"))
    {
        this.completeName = getElementPath(completeName);
        this.sequenceName = completeName;
        this.sequenceShortName = getElementName(completeName);
    } else
    {
        this.completeName = completeName;
        var pos = parsePosition(paramHash.pos);
        this.initFrom = pos[1];
        this.initTo = pos[2];
        var name = "";
        if(pos[0] != undefined)
        {
            name = pos[0];
        }
        if(name === "" || !dc.getElementInfo(name))
        {
            var info = dc.getElementInfoAt(0);
            if(info)
            {
                name = info.name;
            } else
            {
                logger.error(resources.gbErrorInvalidSequence.replace("{sequence}", pos[0]));
                this.isValid = function() {return false;}
                return;
            }
        }
        this.sequenceName = createPath(completeName, name);
        this.sequenceShortName = name;
    }
    this.name = getElementName(this.completeName);
    this.initialTracks = null;
    if(paramHash.track)
      this.initialTracks = [paramHash.track];
    this.tracksCount = 0;
    this.defaultScale = 0;
    this.zoomSelection = false;
    this.enabledTracks = {};
    this.defaultIndexedTracks = [];
    this.tabId = customId != undefined? customId : allocateDocumentId("sequence_"+this.completeName);
    
    function parsePosition(pos)
    {
        var result = [undefined,undefined,undefined];
        if(pos === "" || pos === undefined)
        {
            return result;
        }
        var fields = pos.match(/^\s*(.+)\:(\d+)[\-\.]+(\d+)\s*$/);
        if(fields) result = [fields[1],parseInt(fields[2]),parseInt(fields[3])];
        else
        {
            fields = pos.match(/^\s*(.+)\:(\d+)\s*$/);
            if(fields) result = [fields[1],parseInt(fields[2]),undefined];
            else
            {
                fields = pos.match(/^\s*(.+)\:\s*$/);
                if(fields) result = [fields[1],1,undefined];
                else
                {
                    fields = pos.match(/^\s*(\d+)[\-\.]+(\d+)\s*$/);
                    if(fields) result = [undefined,parseInt(fields[1]),parseInt(fields[2])];
                    else
                    {
                        fields = pos.match(/^\s*(\d+)\s*$/);
                        if(fields) result = [undefined,parseInt(fields[1]),undefined];
                    }
                }
            }
        }
        if(isNaN(result[1]) || result[1]==Infinity || result[1]<=0) result[1] = 1;
        if(isNaN(result[2]) || result[2]==Infinity || result[2]<=0) result[2] = undefined;
        if(result[2] !== undefined && result[2] <= result[1])
        {
            result[2] = undefined;
        }
        return result;
    };

    this.getTrackByPath = function(path)
    {
        for (var i in this.enabledTracks)
        {
            var track = this.enabledTracks[i];
            if (track.de == path)
              return track;
        }
        return null;
    };
    
    
    //init track list from Tracks collection of current module
    this.initAvailableTracks = function()
    {
        var trackSource;
        if(getDataCollection(this.sequenceName).getSize()>0)
            trackSource = this.sequenceName;
        else
        {
            var components = getPathComponents(this.sequenceName);
            if(components.length >= 3)
            {
                var basePath = createPath(components[0], components[1]);
                if(getDataCollection(basePath).getElementInfo("Tracks"))
                    trackSource = createPath(basePath, "Tracks");
            }
        }
        
        _this.initDefaultIndexedTracks();
        _this.loadIndexNames();
        _this.openTracksDialog(trackSource);
    };

    this.initDefaultIndexedTracks = function()
    {
        this.defaultIndexedTracks = [];
        var components = getPathComponents(this.sequenceName);
        if(components.length >= 3)
        {
            var ensemblPath = createPath(components[0], components[1]);
            var tracksPath = createPath(ensemblPath, "Tracks");
            var genesPath = createPath(tracksPath, "Genes");
            var variationPath = createPath(tracksPath, "Variations");
            if(isPathExists(genesPath))
              this.defaultIndexedTracks.push(genesPath);
            if(isPathExists(variationPath))
              this.defaultIndexedTracks.push(variationPath);
        }
    }
    
    this.initTracksForProject = function()
    {
        getDataCollection(this.completeName).getBean(function(bean) {
            var regions = bean.getProperty("regions").getValue();
            var sequenceName = regions[0].getProperty("sequenceName").getValue();
            _this.initFrom = parseInt(regions[0].getProperty("from").getValue());
            _this.initTo = parseInt(regions[0].getProperty("to").getValue());
            var tracks = bean.getProperty("tracks").getValue();
            _this.tracksCount = 0;
            _this.enabledTracks = {};
            _this.sequenceName = sequenceName; 
            _this.sequenceShortName = getElementName(_this.sequenceName);
            _this.loadSequence(_this.buildDocument);
            _.each(tracks, function(track)
            {
                var track = new Track(++_this.tracksCount, track.getProperty("dbName").getValue(), track.getProperty("title").getValue());
                _this.enabledTracks[track.id] = track;
                _this.loadTrack(track);
            });
            _this.initDefaultIndexedTracks();
            _this.loadIndexNames();
        });
    };

    this.extendInitialTracks = function()
    {
        var changed = false;
        if(this.initialTracks)
        {
            var extendedTracks = [];
            _.each(this.initialTracks, function(trackPath)
            {
                var otherTracks = getDataCollection(trackPath).getAttributes()['openWithTracks'];
                if(otherTracks)
                {
                    _.each(otherTracks.split(";"), function(path) {
                        if(!isPathExists(path))
                          return;
                        if(extendedTracks.indexOf(path) == -1)
                            extendedTracks.push(path);
                        changed = true;
                    });
                }
                if(extendedTracks.indexOf(trackPath) == -1)
                    extendedTracks.push(trackPath);
            });
            this.initialTracks = extendedTracks;
         }
         return changed;
    }
    
    this.openTracksDialog = function(path)
    {
        this.enabledTracks = {};
        var displayDialog = true;
        _this.extendInitialTracks()
        _this.loadSequence(_this.buildDocument);
        if(this.initialTracks)
        {
            displayDialog=false;//we already know which tracks to open
            _.each(this.initialTracks, function(trackPath)
            {
                var track = new Track(++_this.tracksCount, trackPath, getElementName(trackPath), paramHash);
                _this.enabledTracks[track.id] = track;
                _this.loadTrack(track);
            });
            this.loadIndexNames();
        }
        if(!displayDialog || !path)
        {
            return;
        }
        var dc = getDataCollection(path);
        dc.setFilters("ru.biosoft.bsa.Track", null, null);
        dc.getElementInfoRange(0,10, function(data)
        {
            var defValue;
            if(dc.getSize()==0)
            {
                dc.setFilters(null, null, null);
                return;
            }
            if(dc.getSize()>10)
            {
                defValue = [createPath(path, "")];
            } else
            {
                defValue = [];
                for(var i=0; i<data.length; i++)
                {
                    if(data[i].name == "GC content" || data[i].name == "Genes")
                    {
                        defValue.push(createPath(path, data[i].name));
                    }
                }
                if(!defValue.length)
                {
                    for(var i=0; i<data.length; i++)
                    {
                        defValue.push(createPath(path, data[i].name));
                    }
                }
            }
            var property = new DynamicProperty("path", "data-element-path", defValue);
            property.getDescriptor().setDisplayName(resources.gbProjectDialogTitle);
            property.getDescriptor().setReadOnly(false);
            property.setCanBeNull("no");
            property.setAttribute("dataElementType", "ru.biosoft.bsa.Track");
            property.setAttribute("elementMustExist", true);
            property.setAttribute("promptOverwrite", false);
            property.setAttribute("multiSelect", true);
            var pathEditor = new JSDataElementPathEditor(property, null);
            pathEditor.setModel(property);
            pathEditor.createHTMLNode();
            pathEditor.setValue(defValue);
            pathEditor.setValue = function(value)
            {
                for(var i=0; i<value.length; i++)
                {
                    var track = new Track(++_this.tracksCount, value[i], getElementTitle(value[i]));
                    _this.enabledTracks[track.id] = track;
                    _this.loadTrack(track);
                };
                _this.loadIndexNames();
            };
            pathEditor.openDialog();
        });
    };
    
    this.open = function(parent)
    {
        var _this = this;
        opennedDocuments[this.tabId] = this;
        this.sequenceDocument = $('<div id="' + this.tabId + '"></div>').css('overflow', 'hidden').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(this.sequenceDocument);
        
        this.scale = 3;
        
        this.sequenceContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>').css('overflow', 'hidden');
        this.sequenceDocument.append(this.sequenceContainerDiv);
        this.sequenceControlsDiv = $('<div id="' + this.tabId + '_controls" class="sequenceControls"></div>');
        this.sequenceContainerDiv.append(this.sequenceControlsDiv);
        this.chromosomeSelector = $('<select/>');
        this.chromosomeSelector.change(function()
        {
            if(_this.chromosomeSelector.val()=="...")
            {
                createOpenElementDialog(resources.gbSequenceSelectTitle, "ru.biosoft.bsa.AnnotatedSequence", _this.sequenceName, function(path)
                {
                    if(getElementPath(path) !== getElementPath(_this.sequenceName))
                    {
                        logger.error(resources.gbErrorWrongSequenceSet);
                        return;
                    }
                    _this.setPosition(getElementName(path)+":");
                });
            } else
            {
                _this.setPosition(_this.chromosomeSelector.val()+":");
            }
        });
        this.sequenceControlsDiv.append($('<span style="padding: 20pt;">'+resources.gbSequencePrompt+'</span>').append(this.chromosomeSelector));
        
        this.positionSelector = $('<span style="padding: 20pt;">'+resources.gbPositionPrompt+' <input type="text" class="seq_position" value="0"><input type="button" class="seq_position_button ui-state-default" value="'+resources.gbPositionButton+'"></span>');
        this.positionSelectorInput = this.positionSelector.children(".seq_position");
        this.sequenceControlsDiv.append(this.positionSelector);
        this.positionSelectorInput.keyup(function(event)
        {
            if (event.keyCode == 13) 
                _this.positionSelector.children(".seq_position_button").click();
        });
        this.positionSelector.children(".seq_position_button").click(function()
        {
            _this.setPosition(_this.positionSelectorInput.val());
        });
        
        this.searchSelector = $('<span style="padding: 20pt;">'+resources.gbFindPrompt
           +' <select class="seq_search_index"></select>'
           +' <input type="text" class="seq_search" title="'+resources.gbFindTitle+'" value="">'
           +' <input type="button" class="seq_search_button ui-state-default" value="'+resources.gbFindButton+'">'
        +'</span>');
        this.searchSelector.hide();
        this.searchSelectorInput = this.searchSelector.children(".seq_search");
        this.sequenceControlsDiv.append(this.searchSelector);
        this.searchSelectorInput.keyup(function(event)
        {
            if (event.keyCode == 13) 
                _this.searchSelector.children(".seq_search_button").click();
        });
        this.searchSelector.children(".seq_search_button").click(function()
        {
            var searchString = _this.searchSelectorInput.val();
            var indexInfo = _this.searchSelector.children(".seq_search_index").val();
            var sep = indexInfo.indexOf(':');
            var indexName = indexInfo.substring(0,sep);
            var track = indexInfo.substring(sep+1);
            _this.search(searchString, indexName, track);
        });
        
        this.trackLabelsDiv = $('<div class="trackLabelsDiv"></div>').css('overflow', 'hidden').css('position', 'absolute').css('top', '0px');
        this.tracksDiv = $('<div></div>').css('right', '5px').css('position', 'absolute');
        this.viewPanesContainerDiv = $('<div></div>').css('height', '100%').css('width', '100%').css('overflow', 'hidden');
        this.tracksDiv.append(this.viewPanesContainerDiv);
        this.sequenceDiv = $('<div></div>').css('position', 'absolute').width('100%').height('100%').css('overflow-y', 'auto').css('overflow-x', 'hidden');
        this.sequenceDiv.append(this.tracksDiv);
        this.tracksDiv.append(this.trackLabelsDiv);
        this.positionInformer = new PositionInformer(this);
        this.sequenceContainerDiv.append(this.sequenceDiv);
        this.sequenceContainerDiv.resize(function()
        {
            _this.sequenceDiv.height(_this.sequenceContainerDiv.height() - (_this.sequenceDiv.offset().top - _this.sequenceContainerDiv.offset().top));
            _this.tracksDiv.height("auto");
            _this.tracksDiv.height(Math.max(_this.sequenceDiv.height(), _this.tracksDiv.height()));
            _this.trackLabelsDiv.height(_this.tracksDiv.height());
            _this.tracksDiv.width(_this.sequenceDiv.width() - _this.trackLabelsDiv.width() - 10);

            _this.setScaleLimit();
            if(_this.lastWidth !== _this.sequenceContainerDiv.width())
            {
                _this.lastWidth = _this.sequenceContainerDiv.width();
                if(_this.panes)
                {
                    _.invoke(_this.panes, "resize");
                    _this.updateCurrentPos();
                }
            }
        });
        
        this.initZoomSelection();
        this.initDragDrop();

        if(this.projectName)
        {
            this.initTracksForProject();
        } else
        {
            this.initAvailableTracks();
        }
        
        createTreeItemDroppable(this.sequenceDiv, "ru.biosoft.bsa.Track", function(path)
            {
                if(!_this.addTrack(path))
                    logger.error(resources.gbErrorTrackAlreadyPresent.replace("{path}", path));
            }
        );
        //update document size
        resizeDocumentsTabs();
        this.hasUnsavedChanges = false;
    };
    
    this.initZoomSelection = function()
    {
        this.zoomSelection = false;
        var startPos;
        var zoomSelectionDiv = $("<div/>").css({zIndex: "1", position: "absolute", height: "100%", borderWidth: "0 1px", borderColor: "blue", borderStyle: "dotted", background: "rgba(128,128,128,0.5)"}).hide();
        this.viewPanesContainerDiv.prepend(zoomSelectionDiv);
        this.viewPanesContainerDiv.mousedown(function(event)
        {
            if(event.ctrlKey || event.shiftKey)
            {
                //_this.viewPanesContainerDiv.draggable("disable");
                _this.zoomSelection = true;
                _this.positionInformer.updatePos = true;
                startPos = event.pageX-_this.panes.rulerPane.viewPane.offset().left+1; 
                zoomSelectionDiv.show().css({left: startPos+"px", width: "1px"});
                event.stopImmediatePropagation();
            }
        });
        this.viewPanesContainerDiv.mousemove(function(event)
        {
            if(_this.zoomSelection)
            {
                var curPos = event.pageX-_this.panes.rulerPane.viewPane.offset().left;
                zoomSelectionDiv.css({left: Math.min(curPos,startPos)+"px", width: Math.abs(curPos-startPos)+"px"});
                event.preventDefault();
            }
        });
        this.viewPanesContainerDiv.mouseup(function(event)
        {
            if(_this.zoomSelection)
            {
                //_this.viewPanesContainerDiv.draggable("enable");
                _this.zoomSelection = false;
                zoomSelectionDiv.hide();
                var endPos = event.pageX-_this.panes.rulerPane.viewPane.offset().left+1;
                if(endPos === startPos) return;
                var pos = _this.panes.rulerPane.clipRectangle.x;
                var from = (pos + startPos) / _this.scale;
                var to = (pos + endPos) / _this.scale;
                if(from > to)
                {
                    var tmp = from;
                    from = to;
                    to = tmp;
                }
                _this.scrollTo(from, to);
                event.stopImmediatePropagation();
            }
        });
    };
    
    this.initDragDrop = function()
    {
        var _this = this;
        var dragProperties = 
        {
            cursor: 'crosshair',
            start: function(event, ui)
            {
                removeSelector();
            },
            stop: function(event, ui)
            {
                var dx = (_this.tracksDiv.offset().left - _this.viewPanesContainerDiv.offset().left) / _this.scale;
                var offset = dx * _this.scale;
                _this.viewPanesContainerDiv.css('left', 0).css('top', 0);
                _.each(_this.panes, function(pane)
                {
                    pane.viewPane.css('left', parseInt(pane.viewPane.css('left'))-offset);
                });
                _this.shift(dx);
                _.each(_this.panes, function(pane)
                {
                    pane.clickEnabled = false;
                });
            },
            axis: "x",
            scroll: false
        };
        this.viewPanesContainerDiv.draggable(dragProperties);
    };
    
    /*
     * Export track in one of the selected formats
     */
    this.exportElement = function(value)
    {
        var _this = this;
        if (_.isEmpty(this.enabledTracks)) 
        {
            logger.message(resources.gbErrorNoTracksToExport);
            return;
        }
        queryBioUML('web/export', 
        {
            type: "deInfo",
            detype: "Track",
            de: _.values(this.enabledTracks)[0].de,
            sequence: this.sequenceName,
            from: this.from,
            to: this.to
        }, function(data)
        {
            var fromSeq = _this.from;
            var toSeq = _this.to;
            if(fromSeq < _this.sequenceStart) fromSeq = _this.sequenceStart;
            if(toSeq > _this.sequenceStart+_this.sequenceLength) toSeq = _this.sequenceStart+_this.sequenceLength;
            var dialogDiv = $('<div title="'+resources.gbExportDialogTitle+'">'+resources.gbExportDialogTrack+' <select width="100%" id="tracks"></select><br>' +
                resources.gbExportDialogFormat+' <select width="100%" id="formats"></select><br>' +
                '<input type="radio" name="trackrange" id="currange" checked><label for="currange">'+resources.gbExportDialogRangeDisplayed+' ('+fromSeq+' &ndash; '+toSeq+')</label><br>' +
                '<input type="radio" name="trackrange" id="wholesequence"><label for="wholesequence">'+resources.gbExportDialogRangeWhole+' ('+_this.sequenceStart+' &ndash; '+(_this.sequenceLength+_this.sequenceStart)+')</label><br>' +
                '<input type="radio" name="trackrange" id="customrange"><label for="customrange">'+resources.gbExportDialogRangeCustom+'</label>: <input type="text" id="fromPos" value='+fromSeq+'> &ndash; <input type="text" id="toPos" value='+toSeq+'>' +
                '<br><div id="msgpane"></div><div id="progressbar"/>'+
                '</div>');
            dialogDiv.find('#fromPos,#toPos').keydown(function() {
                dialogDiv.find("#customrange").attr("checked", "checked");
            });
            var formats = dialogDiv.find('#formats');
            var splitStr = data.values;
            for (i = 0; i < splitStr.length; i++) 
            {
                if (splitStr[i].length > 0) 
                {
                    formats.append($('<option/>').val(splitStr[i]).text(splitStr[i]));
                }
            }
            var tracks = dialogDiv.find("#tracks");
            _.each(_this.enabledTracks, function(track, i)
            {
                tracks.append($('<option/>').val(i).text(track.displayName));
            });
            var jobID = undefined;
            dialogDiv.dialog(
            {
                autoOpen: true,
                width: 450,
                height: 250,
                modal: true,
                beforeClose: function()
                {
                    if (jobID != undefined) 
                    {
                        cancelJob(jobID);
                    }
                    $(this).remove();
                },
                buttons: 
                {
                    "Ok": function()
                    {
                        jobID = rnd();
                        var exporterName = formats.val();
                        var from = fromSeq, to = toSeq;
                        $("#msgpane").text(resources.gbExportDialogProgress);
                        $(":button:contains('Ok')").attr("disabled", "disabled");
                        if ($("#wholesequence").attr("checked")) 
                        {
                            from = _this.sequenceStart;
                            to = _this.sequenceLength+_this.sequenceStart;
                        }
                        if ($("#customrange").attr("checked")) 
                        {
                            from = parseInt($("#fromPos").val());
                            to = parseInt($("#toPos").val());
                        }
                        helperFrame.attr("src", appInfo.serverPath+'web/export?'+toURI({
                            type: "de",
                            detype: "Track",
                            de: _this.enabledTracks[tracks.val()].de,
                            sequence: _this.sequenceName,
                            from: from,
                            to: to,
                            exporter: exporterName,
                            jobID: jobID
                        }));
                        createProgressBar($("#progressbar"), jobID, function(status) {
                            $(":button:contains('Ok')").removeAttr("disabled");
                            jobID = undefined;
                        });
                    },
                    "Cancel": function()
                    {
                        if (jobID != undefined) 
                        {
                            cancelJob(jobID);
                        }
                        else 
                        {
                            $(this).dialog("close");
                            $(this).remove();
                        }
                    }
                }
            });
            addDialogKeys(dialogDiv);
            sortButtons(dialogDiv);
        });
    };
    
    this.onHashChange = function(params)
    {
        if(params.pos) this.setPosition(params.pos);
        if(params.track) {
          this.addTrack(params.track, params);
        }
    };
    
    this.getHashParameters = function()
    {
        if(this.position != undefined) return {pos: this.position};
    };
    
    this.updateCurrentPos = function()
    {
        removeSelector();
        this.from = Math.floor(this.panes.rulerPane.clipRectangle.x / this.scale);
        this.to = Math.ceil((this.panes.rulerPane.clipRectangle.x + this.panes.rulerPane.width) / this.scale);
        this.position = this.sequenceShortName+":"+Math.max(this.from, this.sequenceStart)+"-"+Math.min(this.to, this.sequenceStart + this.sequenceLength - 1);
        this.positionSelectorInput.val(this.position);
        updateURL();
        this.positionSelectorInput.get(0).select(0, -1); // select all
        this.positionInformer.setOffsetZoom(Math.max(this.from, this.sequenceStart), Math.min(this.to, this.sequenceStart + this.sequenceLength - 1), this.scale);
        this.updateViewParts(false);
        this.sequenceDocument.trigger("positionChanged");
    };
    
    this.updateViewParts = function(tracks)
    {
        var vp = getActiveViewPart();
        if (vp instanceof SitesViewPart) 
            vp.show(this);
        if (vp instanceof TracksViewPart)
        {
            vp.update(this);
            if(tracks)
                vp.loadTracksTable(this);
        }
    };
    
    this.getRulerFont = function()
    {
        var font = new Font("courier new", Font.PLAIN, 12);
        return new ColorFont(font);
    };
    
    this.setPosition = function(position)
    {
        if(this.insideSetPosition) return;
        this.insideSetPosition = true;
        var fields = parsePosition(position);
        var newSequence = this.sequenceName;
        if(fields[0] !== undefined)
        {
            var seqCollection = getElementPath(this.sequenceName);
            var info = getDataCollection(seqCollection).getElementInfo(fields[0]);
            if(!info)
            {
                logger.error(resources.gbErrorInvalidSequence.replace("{sequence}", fields[0]));
                return;
            }
            newSequence = createPath(seqCollection, fields[0]);
        }
        this.hasUnsavedChanges = true;
        if(newSequence !== this.sequenceName)
        {
            this.sequenceName = newSequence;
            this.sequenceShortName = getElementName(newSequence);
            this.loadSequence(function()
            {
                _this.panes.rulerPane.sequenceName = _this.sequenceName;
                _this.panes.rulerPane.sequenceLength = _this.sequenceLength;
                _.each(_this.panes, function(pane)
                {
                    if(pane.ajaxParam)
                    {
                        pane.ajaxParam.sequence = _this.sequenceName;
                    }
                });
                if(fields[1] !== undefined && fields[2] !== undefined)
                {
                    this.scrollTo(fields[1], fields[2]);
                }
                else
                {
                    this.scrollTo(fields[1], fields[1]+_this.panes.rulerPane.width/_this.scale);
                }
                this.insideSetPosition = false;
            }, function()
            {
                this.insideSetPosition = false;
            });
        } else
        {
            if(fields[1] !== undefined)
            {
                if(fields[2] !== undefined)
                {
                    if(Math.abs(fields[1]-this.from)>1 || Math.abs(fields[2]-this.to)>1)
                    {
                        this.scrollTo(fields[1], fields[2]);
                    }
                } else
                {
                    if(fields[1] != this.from)
                    {
                        this.shift(fields[1] - this.from);
                    }
                }
            }
            this.insideSetPosition = false;
        }
    };
    
    this.scrollTo = function(from, to)
    {
        var newLogScale = Math.log(this.panes.rulerPane.width / 3 / (to-from)) / Math.log(2);
        if(newLogScale < this.minLogScale) newLogScale = this.minLogScale;
        if(newLogScale > this.maxLogScale) newLogScale = this.maxLogScale;
        this.setLogScale(newLogScale, undefined, from);
        this.hasUnsavedChanges = true;
    };
    
    /**
     * Zooms displayed sequence to given logarithmic scale value (0 = default scale)
     */
    this.setLogScale = function(newLogScale, centerPoint, newFrom)
    {
        var oldPos = this.panes.rulerPane.clipRectangle.x;
        if(centerPoint == undefined)
        {
            if(Math.floor(oldPos/this.scale) == 1) centerPoint = 0;
            else centerPoint = this.panes.rulerPane.width/2;
        }
        var from = (oldPos + centerPoint) / this.scale;
        this.logScale = newLogScale;
        this.scale = 3 * Math.pow(2, this.logScale);
        if (this.logScale > this.maxLogScale - 1) 
        {
            var newScale = this.getRulerFont().font.getExtent("A", this.panes.rulerPane.getContext())[0];
            if (newScale != this.scale) 
            {
                this.scale = newScale;
                this.logScale = Math.log(newScale / 3) / Math.log(2);
            }
        }
        var newPos = newFrom == undefined ? from * this.scale - centerPoint : newFrom * this.scale;
        if ((newPos + this.panes.rulerPane.width) / this.scale > this.sequenceLength) 
            newPos = this.sequenceLength * this.scale - this.panes.rulerPane.width;
        if (newPos / this.scale < this.sequenceStart) 
            newPos = this.sequenceStart * this.scale;
        _.each(this.panes, function(pane, p)
        {
            if (p != "rulerPane") 
            {
                pane.ajaxParam.logscale = _this.logScale;
            }
            pane.scaleX = _this.scale;
            pane.initUpdate(newPos, 0);
        });
        this.updateCurrentPos();
        this.hasUnsavedChanges = true;
    };
    
    /**
     * Shifts displayed sequence to given offset. Sequence will not be shifted beyond its length
     * @param {Object} dx - offset to shift to
     */
    this.shift = function(dx)
    {
        var forceUpdate = false;
        if (this.from == undefined) 
        {
            this.from = 0;
            forceUpdate = true;
        }
        if (this.to + dx > this.sequenceStart + this.sequenceLength) 
            dx = this.sequenceStart + this.sequenceLength - this.to;
        if (isNaN(dx) || this.from + dx < this.sequenceStart) 
            dx = this.sequenceStart - this.from;
        if (dx == 0 && !forceUpdate) 
            return;
        dx = (dx+this.from)*this.scale;
        _.invoke(this.panes, "initUpdate", dx, 0);
        this.updateCurrentPos();
        this.hasUnsavedChanges = true;
    };
    
    this.loadIndexRequest = 0;//used to make only the last call to loadIndexNames() to take effect
    this.loadIndexNames = function()
    {
      _this.loadIndexRequest++;
      var curLoadIndexRequest = _this.loadIndexRequest;
 
      var indexedTracks = [];
      _.each(this.defaultIndexedTracks, function(track) {
         if(!indexedTracks.includes(track))
           indexedTracks.push(track)
       });
      _.each(this.enabledTracks, function(track) {
        if(!indexedTracks.includes(track.de))
          indexedTracks.push(track.de)
      });
      var remainingTracks = indexedTracks.length;
      if(remainingTracks == 0)
         _this.searchSelector.css('display', 'none');
      else
         _this.searchSelector.css('display', 'inline');
      var indexNamesByTrack = {};
      _.each(indexedTracks, function(track) {
            queryService("bsa.service", 57, 
            {
                de: track
            }, function(data)
            {
                if(curLoadIndexRequest != _this.loadIndexRequest)
                  return;
                remainingTracks--;
                if(data.values!="")
                {
                  var names = data.values.split(":");
                  indexNamesByTrack[track] = names;
                }
                if(remainingTracks <= 0)
                  _this.fillSearchSelector(indexedTracks, indexNamesByTrack);
            });
      });
    }

    this.fillSearchSelector = function(trackOrder, indexNamesByTrack) {
        if(!_this.searchSelector)
            return;
        var options = _this.searchSelector.children(".seq_search_index");
        options.empty();
        for(var i = 0; i < trackOrder.length; i++)
        {
            var track = trackOrder[i];
            var names = indexNamesByTrack[track];
            if(names != undefined)
                for(var j = 0; j < names.length; j++)
                {
                    var name = names[j];
                    var option = $('<option value="'+ name + ":" + track +'">'+name+'</option>');
                    options.append(option);
                }
        }
        if(options.length == 0)
          _this.searchSelector.css('display', 'none');
        else
          _this.searchSelector.css('display', 'inline')
    }
    /**
     * Search sites and move to its position
     * @param searchString - string to search
     */
    this.search = function(searchString, indexName, trackPath)
    {
        var _this = this;
        queryService("bsa.service", 56, 
        {
            de: trackPath,
            query: searchString,
            index: indexName
        }, function(data)
        {
            var site = JSON.parse(data.values)
            var gbLen = (_this.to - _this.from);
            var gbFrom = site.from - Math.floor(gbLen/2);
            var gbTo = gbFrom + gbLen;
            var pos = site.chr + ":" + gbFrom + "-" + gbTo;
            var seqCollection = getElementPath(_this.sequenceName);
            var chrPath = createPath(seqCollection, site.chr);
            _this.setPosition(pos);
            var track = _this.getTrackByPath(trackPath);
            if(track != null) {
              _this.selectSiteInTrack(trackPath, site.name);
              _this.showSiteInfo(track, site.name, chrPath, site.from, site.to);
            }
        }, function() {
                logger.error(resources.gbErrorSiteNotFound);
        });
    };

    this.showSiteInfo = function(track, site, chr, from, to)
    {
        var requestParams = {
            sequence: chr,
            site: site,
            from: from,
            to: to
        };
        var templateName = perspective.template;
        if(templateName)
            requestParams["templateName"]= templateName
            
        $.extend(requestParams, track.getRequestParams());
        queryService("bsa.service", 47, requestParams, function(data)
        {
            $("#info_area").html(data.values);
        });
    };
    
    /**
     * Shifts displayed sequence one page forward
     */
    this.pageForward = function()
    {
        this.shift(this.to - this.from);
    };
    
    /**
     * Shifts displayed sequence one page backward
     */
    this.pageBackward = function()
    {
        this.shift(this.from - this.to);
    };
    
    /**
     * Shifts displayed sequence forward to specified percent of page width
     */
    this.shiftForward = function(percent)
    {
        this.shift((this.to - this.from)*(percent/100.0));
    };
    
    /**
     * Shifts displayed sequence backward  to specified percent of page width
     */
    this.shiftBackward = function(percent)
    {
        this.shift((this.from - this.to)*(percent/100.0));
    };
    
    /**
     * Zooms displayed sequence in (usually 2x)
     */
    this.zoomIn = function(centerPoint)
    {
        if (this.logScale > this.maxLogScale - 1) 
            return;
        this.setLogScale(Math.floor(this.logScale+0.1) + 1, centerPoint);
    };
    
    /**
     * Zooms displayed sequence out (usually 2x)
     */
    this.zoomOut = function(centerPoint)
    {
        if (this.logScale <= this.minLogScale) 
            return;
        if (this.logScale > this.maxLogScale - 1) 
            this.setLogScale(this.maxLogScale - 1, centerPoint);
        else 
            this.setLogScale(Math.ceil(this.logScale-0.1) - 1, centerPoint);
    };
    
    /**
     * Zooms to display the whole sequence
     */
    this.zoomFull = function()
    {
        this.setLogScale(this.minLogScale);
    };
    
    /**
     * Zooms to the most detailed mode when nucleotides are visible
     */
    this.zoomDetailed = function()
    {
        this.setLogScale(this.maxLogScale);
    };
    
    /**
     * Zooms to default size
     */
    this.zoomDefault = function()
    {
        this.setLogScale(this.defaultScale);
    };
    
    this.setScaleLimit = function()
    {
        if (this.sequenceLength == undefined) 
            return;
        var curwidth = Math.max(this.viewPanesContainerDiv.width(), 200);
        this.minLogScale = -Math.ceil(Math.log(this.sequenceLength * 3 / curwidth) / Math.log(2));
        this.defaultScale = Math.floor(2.5-Math.max(Math.log(this.sequenceLength/1000)/Math.log(2)/2,0));
        this.maxLogScale = 2;
    };
    
    this.onTracksResize = function()
    {
        _.each(this.panes, function(pane) 
        {
            if (pane.labelDiv) 
            {
                pane.labelDiv.css('top', pane.viewPane.offset().top - _this.tracksDiv.offset().top).css("max-height", pane.viewPane.height() + "px");
            }
        });
    };
    
    this.buildDocument = function()
    {
        this.panes = new Object();
        this.panes.rulerPane = new RulerViewPane(this.viewPanesContainerDiv, this.sequenceName, this.sequenceLength, this.getRulerFont()); 
        this.panes.rulerPane.width = this.tracksDiv.width();
        this.shift(0);
        if(this.initFrom > 0 && this.initTo > this.initFrom)
            this.scrollTo(this.initFrom, this.initTo);
        else
            this.zoomDefault();
        this.selectionLayer = $("<div/>").css({width: "100%", height: "100%", background: "blue"});
        this.selectionLayer.hide();
        this.tracksDiv.append(this.selectionLayer);
        this.viewPanesContainerDiv.mousewheel(function(event, delta)
        {
            if(event.ctrlKey)
            {
                var centerPoint = event.pageX-_this.panes.rulerPane.viewPane.offset().left;
                if(delta > 0) _this.zoomIn(centerPoint); else if(delta < 0) _this.zoomOut(centerPoint);
                event.preventDefault();
            }
        });
        this.trackLabelsDiv.sortable(
        {
            axis: 'y',
            cursor: 'crosshair',
            update: function(event, ui)
            {
                var orderCnt = 1;
                _this.trackLabelsDiv.children().each(function()
                {
                    var id =$(this).attr('id').substring(6); 
                    if(_this.enabledTracks[id])
                    {
                        _this.enabledTracks[id].sortPos = orderCnt++;
                    }
                    var track = $(this).data("track");
                    if (track) 
                    {
                        track.viewPane.appendTo(_this.viewPanesContainerDiv);
                    }
                });
                if (browserApp.msie) _.invoke(_this.panes, "repaint");
                _this.onTracksResize();
                _this.updateViewParts(true);
            }
        });
        this.sequenceDocument.trigger("sequenceLoaded");
        this.hasUnsavedChanges = false;
    };
    
    this.loadSequence = function(callback, failureCallback)
    {
        var dc = getDataCollection(getElementPath(this.sequenceName));
        var size = dc.getSize();
        var chrList = dc.getElementInfoRange(0,40);
        var curNameAppear = false;
        this.chromosomeSelector.empty();
        for(var i=0; i<chrList.length; i++)
        {
            if(chrList[i].name == this.sequenceShortName) curNameAppear = true;
            this.chromosomeSelector.append($("<option/>").text(chrList[i].name));
        }
        if(!curNameAppear) this.chromosomeSelector.prepend($("<option/>").text(this.sequenceShortName));
        if(chrList.length < size) this.chromosomeSelector.append($("<option/>").text("..."));
        this.chromosomeSelector.val(this.sequenceShortName);
        this.sequenceStart = 0;
        this.sequenceLength = 0;
        queryService('bsa.service', 54,
        {
            de: this.sequenceName
        }, function(data)
        {
            var result = $.parseJSON(data.values);
            _this.sequenceStart = result.start;
            _this.sequenceLength = result.length;
            _this.setScaleLimit();
            callback.apply(_this);
        }, function(data)
        {
            logger.error(data.message);
            if(failureCallback)
                failureCallback.apply(_this);
        });
    };
    
    this.loadTrack = function(track)
    {
        if(!this.panes)
        {
            this.sequenceDocument.bind("sequenceLoaded", function() {_this.loadTrack(track);});
            return;
        }
        var sequenceName = this.sequenceName;
        
        var ajaxParam = {
          sequence: sequenceName,
          project: this.projectName,
          command: 45,
          service: "bsa.service",
          logscale: this.logScale
        };
        $.extend(ajaxParam, track.getRequestParams())
        var pane = new AjaxViewPane(this.viewPanesContainerDiv, 
        {
            dragAxis: 'none',
            URL: appInfo.serverPath+"web/data",
            ajaxParam: ajaxParam,
            ajaxParamFromX: "from",
            ajaxParamToX: "to",
            scaleX: this.scale,
            fullHeight: false
        });
        viewPanesContainerDivResizeHandler = function() {
          pane.resize();
          return true;
        }
        new ResizeObserver(viewPanesContainerDivResizeHandler).observe(this.viewPanesContainerDiv[0]);
        pane.track = track;
        pane.updateTrackLabel = function(label)
        {
            var span = this.labelDiv.children().eq(0);
            fitElement(span, label, true, _this.trackLabelsDiv.width()-60);
            span.attr("title", label);
        };
        this.panes[track.id] = pane;
	
	function resizeHandler() {
            _this.onTracksResize();
        }
        resizeHandler();
        new ResizeObserver(resizeHandler).observe(pane.viewPane[0]);
        
	pane.labelDiv = $('<div class="trackLabel"></div>')
            .append('<span/>')
            .css("max-width", this.trackLabelsDiv.width() - 20 + "px")
            .css("top", "-200px")
            .css("white-space", "nowrap")
            .attr("id", "label_"+track.id);
        pane.waiterDiv = $('<div class="trackWaiter">&nbsp;</div>').hide();
        pane.updateTrackLabel(track.displayName);
        var scrollToDownSite = $('<span class="track-arrow-button"/>').attr("title", resources.gbPreviousSite)
            .append($('<img class="track-arrow-button-img"/>').attr("src", "icons/plus_rtl.gif")).click(function()
        {
            _this.scrollToNextSite(track, "down");
        });
        var scrollToUpSite = $('<span class="track-arrow-button"/>').attr("title", resources.gbNextSite)
            .append($('<img class="track-arrow-button-img"/>').attr("src", "icons/plus.gif")).click(function()
        {
            _this.scrollToNextSite(track, "up");
        });
        pane.labelDiv.append(scrollToDownSite).append(scrollToUpSite).append(pane.waiterDiv).data("track", pane).click(function(event)
        {
            if(!track.isRemote())
              showElementInfo(track.de);
        });
        if(!track.isRemote())
        pane.labelDiv.attr("data-path", track.de);
        addTreeItemContextMenu(pane.labelDiv, 'tree-menu-sequence', {
            "open_genome_browser": false, 
            "remove_from_view": {label: resources.menuRemoveFromView, icon: "icons/remove.gif", action: function(itemKey, options, originalEvent) 
                {
                    var trackId = options.$trigger.attr("id").substring(6);
                    _this.removeTrack(trackId);
                }}
        });
        pane.onClick = function() {};
        this.trackLabelsDiv.append(pane.labelDiv);
        pane.initUpdate(this.from * this.scale, 0);
        this.onTracksResize();
        this.updateViewParts(true);
        this.hasUnsavedChanges = false;
    };
    
    this.scrollToNextSite = function(track, dir)
    {
        var from, to;
        if(dir == "up")
        {
            from = this.to;
            to = _this.sequenceStart + _this.sequenceLength;
        }
        else
        {
            from = _this.sequenceStart;
            to = this.from;
        }
        this.panes[track.id].waiterDiv.show();
        var requestParams = {
            sequence: _this.sequenceName,
            from: from,
            to: to,
            direction: dir
        };
        $.extend(requestParams, track.getRequestParams());
        queryService("bsa.service", 49, requestParams, function(data)
        {
            _this.panes[track.id].waiterDiv.hide();
            var ind = data.values.lastIndexOf(':');
            if (ind != -1) 
            {
                var ind2 = data.values.substring(0, ind).lastIndexOf(':');
                if (ind2 != -1) 
                {
                    var siteId = data.values.substring(0, ind2);
                    var from = parseInt(data.values.substring(ind2 + 1, ind));
                    var to = parseInt(data.values.substring(ind+1));
                    _this.showSiteInfo(track, siteId, _this.sequenceName, from, to);
                    _this.shift(from - (_this.to + _this.from)/2);
                }
            }
        }, function(data)
        {
            _this.panes[track.id].waiterDiv.hide();
            logger.error(data.message);
        });
    };
    
    this.addTrack = function(de, params)
    {
        if(_.any(this.enabledTracks, function(track)
        {
            return track.de == de; 
        })) return false;

        var trackName = getElementName(de);
        this.lastTrackPath = getElementPath(de)+"/";
        var newTrack = new Track(++this.tracksCount, de, trackName, params); 
        this.enabledTracks[newTrack.id] = newTrack;
        this.loadTrack(newTrack);
        this.hasUnsavedChanges = true;
        this.loadIndexNames();
        return true;
    };
    
    this.removeTrack = function(trackId)
    {
        this.panes[trackId].viewPane.remove();
        this.panes[trackId].labelDiv.remove();
        delete this.panes[trackId];
        delete this.enabledTracks[trackId];
        this.onTracksResize();
        this.updateViewParts(true);
        this.hasUnsavedChanges = true;
        this.loadIndexNames();
    };
    
    this.reloadTracks = function ()
    {
        _.each(this.enabledTracks, function(track) 
        {
            _this.panes[track.id].initUpdate(_this.from*_this.scale, 0);
        });
    };
    
    this.reloadTrack = function (trackId)
    {
        _this.panes[trackId].initUpdate(_this.from*_this.scale, 0);
    };
    
    this.close = function(callback)
    {
        if(callback) callback();
    };

    this.hasUnsavedChanges = false;
    this.isChanged = function()
    {
        return this.projectName && this.hasUnsavedChanges;
    };
    
    if(this.projectName)
    {
        this.save = function(callback)
        {
            this.saveAs(this.completeName, callback);
        };
    }

    this.saveAs = function(newPath, callback)
    {
        queryService("bsa.service", 55,
        {
            sequence: _this.sequenceName,
            tracks : $.toJSON(_.map(_.sortBy(_this.enabledTracks,
                    function(t) {
                        return t.sortPos;
                    }), 
                    function(t) {
                        return [t.de, t.displayName, t.visible];
                    })),
            from: _this.from,
            to: _this.to,
            targetPath: newPath,
            project: this.projectName
        }, function(data)
        {
            if(newPath == _this.completeName)
              _this.hasUnsavedChanges = false;
            getDataCollection(newPath).invalidateCollection();
            if(callback) callback(data);
        });
    };
    
    this.selectSiteInTrack = function(track, site)
    {
        var trackID = this.getTrackByPath(track).id;
        var selectedView = this.panes[trackID].view.findViewByModel(site);
        if (selectedView)
        {
            var bounds = selectedView.getBounds();
            showSelector(this.panes[trackID].canvasDiv, bounds.x, bounds.y, bounds.width, bounds.height)
        }
    }
    
    this.combineTracks = function()
    {
        createSaveElementDialog("Create combined track", "ru.biosoft.bsa.Track", "Combined track",
            function(completePath)
            {
                var tracksPaths = [];
                for (var i in _this.enabledTracks)
                {
                    tracksPaths.push(_this.enabledTracks[i].de);
                }
                queryService("bsa.service", 58, 
                {
                     sequence: _this.sequenceName,
                     from : _this.from,
                     to : _this.to,
                     tracks : $.toJSON(_.map(_.sortBy(_this.enabledTracks,
                             function(t) {
                         return t.sortPos;
                     }), 
                     function(t) {
                         return [t.de, t.displayName, t.visible];
                     })),
                     targetPath: completePath
                }, function(data)
                {
                    refreshTreeBranch(getElementPath(completePath));
                    _this.addTrack(completePath);
                });
            });
    }
    
    this.updateItemView = function(item)
    {
        if(instanceOf(item.getClass(), "ru.biosoft.bsa.Track"))
        {
            var tr = _.find(this.enabledTracks, function(track)
            {
                return track.de == item.completeName; 
            });
            if(tr)
                this.reloadTrack(tr.id);
        }
    }
}

/**
 * Add horizontal position informer box to the container
 * @param {jQuery} parent - container
 */
function PositionInformer(document)
{
    var _this = this;
    this.document = document;
    this.parent = document.viewPanesContainerDiv;
    this.positionParent = this.parent.parent();
    
    this.zoom = 1;
    this.from = 0;
    this.to = -1;
    this.pixelWidth = 1;
    this.updatePos = true;
    this.lastX = 0;
    this.lastY = 0;
    
    this.positionInformerText = $("<div class='positionInformerText'></div>");
    this.positionInformerLine = $("<div class='positionInformerLine'></div>").css('z-index', 1).append(this.positionInformerText);
    
    this.parent.append(this.positionInformerLine);
    
    this.setOffsetZoom = function(from, to, zoom)
    {
        this.from = from;
        this.to = to;
        this.zoom = zoom;
        this.pixelWidth = 1;
        var scaleCoeff = [2, 2.5, 2];
        var newPixelWidth = 1;
        for (var pos = 0; newPixelWidth * this.zoom < 2; pos = (pos + 1) % scaleCoeff.length) 
        {
            this.pixelWidth = newPixelWidth;
            newPixelWidth *= scaleCoeff[pos];
        }
        this.updateInformer();
    };
    
    this.updateInformer = function(x, y)
    {
        if (x == undefined) 
            x = this.lastX;
        if (y == undefined) 
            y = this.lastY;
        x = Math.floor( x / this.zoom ) * this.zoom;
        this.lastX = x;
        this.lastY = y;
        if (this.updatePos)
        {
            this.pos = Math.floor((x / this.zoom + this.from) / this.pixelWidth + 0.5) * this.pixelWidth;
            if (isNaN(this.pos) || this.pos < this.from || this.pos > this.to) 
                this.positionInformerLine.hide();
            else 
                this.positionInformerLine.show();
            var top = this.parent.offset().top;
            _.each(this.document.panes, function(pane)
            {
                var offsetY = y-(pane.viewPane.offset().top-top);
                if(offsetY>0 && offsetY<pane.height)
                {
                    var activeView = pane.view?pane.view.getDeepestActive(new Point(x, offsetY), null):null;
                    if(activeView && activeView.description) this.pos = activeView.description;
                    return false;
                }
            }, this);
        }
        if(typeof(this.pos) === "number")
        {
            this.pos = (this.pos+"");
            var len = this.pos.length;
            if(len > 6) this.pos = this.pos.substring(0,len-6)+" "+this.pos.substring(len-6,len-3)+" "+this.pos.substring(len-3);
            else if(len > 3) this.pos = this.pos.substring(0,len-3)+" "+this.pos.substring(len-3);
        }
        this.positionInformerLine.css('left', x + "px");
        this.positionInformerText.css('top', y - 25 + "px");
        this.positionInformerText.text(this.pos);
        this.positionInformerText.css('left', -this.positionInformerText.width() / 2 + "px");
    };
    
    this.parent.mousemove(function(event)
    {
        _this.updateInformer(event.pageX - _this.parent.offset().left, event.pageY - _this.parent.offset().top);
    });
    
    this.parent.mousedown(function()
    {
        _this.updatePos = false;
    });
    
    this.parent.mouseup(function(event)
    {
        _this.updatePos = true;
        _this.updateInformer(event.pageX - _this.positionParent.offset().left, event.pageY - _this.positionParent.offset().top);
    });
    
    this.parent.bind("contextmenu", function(event)
    {
        var y = event.pageY;
        _.each(_this.document.panes, function(pane)
        {
            var offsetY = y-pane.viewPane.offset().top;
            if(offsetY>0 && offsetY<pane.height)
            {
                pane.viewPane.trigger("contextmenu", event);
                return false;
            }
        }, _this);
    });
    
    this.parent.click(function(event)
    {
        if(this.zoomSelection) return;
        var x = event.pageX - _this.parent.offset().left;
        var y = event.pageY - _this.parent.offset().top;
        var top = _this.parent.offset().top;
        _.each(_this.document.panes, function(pane)
                {
                    var offsetY = y-(pane.viewPane.offset().top-top);
                    if(offsetY>0 && offsetY<pane.height)
                    {
                        var activeView = pane.view?pane.view.getDeepestActive(new Point(x, offsetY), null):null;
                        if(activeView && activeView.model)
                        {
                            var elementBounds = activeView.getBounds();
                            showSelector(pane.canvasDiv, elementBounds.x, elementBounds.y, elementBounds.width, elementBounds.height);
                            _this.document.showSiteInfo(pane.track, activeView.model, _this.document.sequenceName, _this.document.from, _this.document.to);
                        }
                        return false;
                    }
                });
    });
}

function Track(count, path, displayName, params)
{
    this.id = 'track_' + count;
    this.displayName = displayName;
    this.de = path;
    this.visible = true;
    this.sortPos = count;
    this.props = {};
    if(params && params.chr_name_mapping)
      this.props.chr_name_mapping = params.chr_name_mapping;
    
    this.isVisible = function()
    {
        return this.visible;
    };
    this.toggleVisibility = function()
    {
        this.visible = !this.visible;
    };

    this.getRequestParams = function()
    {
       var params = {de: this.de};
       $.extend(params, this.props);
       return params;
    };

    this.isRemote = function() {
      return this.de.startsWith("http://") || this.de.startsWith("https://") || this.de.startsWith("ftp://");
    }
}
