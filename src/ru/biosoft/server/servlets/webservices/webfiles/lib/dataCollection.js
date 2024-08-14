/**
 * JavaScript realization of DataCollection class
 * 
 * @author tolstyh
 */

var NAME_LIST_CHUNK_SIZE = 100;

function DataCollection(completeName)
{
    var _this = this;
    this.completeName = completeName.replace(/&amp;/g,"&").replace(/&gt;/g,">").replace(/&lt;/g,"<");
    // These two strange rows are aimed to catch or work-around elusive Firefox bug with corrupting completeName string 
    this.cnm = completeName;
    this.cnm2 = ("!"+completeName).substring(1);
    this.nameList = null;
    this.size = -1;
    this.nameMap = {};
    this.elements = {};
    this.attributes = {};
    this.info = null;
    this.beanDPS = null;
    this.journal = null;
    this.gitEnabled = null;
    this.invalidList = [];
    // Symlinks which follow to this collection
    this.backLinks = {};
    // Filters for DataElementPathDialog: childClass, elementClass, referenceType
    this.filters = {};
    // False if for current set of filters nothing in current collection can be selected for sure
    // If false, DataElementPathDialog may disable 'Ok' button
    this.enabled = true;
    
    //listeners
    this.changeListeners = new Array();
    this.removeListeners = new Array();
    
    /*
     * Get collection name
     */
    this.getName = function()
    {
        return this.completeName;
    };
    
    this.getDatabaseName = function()
    {
        var splitPath = getPathComponents(completeName);
        if(/^data\//.test(completeName))
        {
            if(splitPath.length >=3 )
            {
                return createPathFromComponents(splitPath.slice(0, 3));
            }
            return "";
        } else if(splitPath.length >=2 )
        {
            return createPath(splitPath[0], splitPath[1]);
        }
        return "";
    };
    
    this.getElementName = function()
    {
        return getElementName(completeName);
    };

    this.fillElementInfoByResult = function(result)
    {
        //if(_this.size >= 0 && _this.size != result.size)
        //    _this.invalidateCollection();
        _this.size = result.size;
        var from = result.from;
        var icons = result.icons;
        var types = result.classes;
        var names = result.names;
        if(result.target)
        {
            _this.linkTarget = result.target;
            getDataCollection(_this.linkTarget).backLinks[_this.completeName] = 1;
        } else _this.linkTarget = null;
        _this.enabled = result.enabled;
        if(_this.nameList == undefined) _this.nameList = [];
        for(var i=0; i<names.length; i++)
        {
            names[i]["class"] = types[names[i]["class"]];
            if(names[i]["icon"] != undefined)
                names[i]["icon"] = icons[names[i]["icon"]];
            _this.nameList[i+from] = names[i];
            _this.nameMap[names[i].name] = i+from;
        }
    };
    
    var fillElementInfo = function(data)
    {
        if(data.type != QUERY_TYPE_SUCCESS)
        {
            if(_this.size < 0) _this.size = 0;
            return;
        }
        _this.fillElementInfoByResult(JSON.parse(data.values));
    };
    
    this.setFilters = function(elementClass, childClass, referenceType)
    {
        if(this.filters.elementClass == elementClass && this.filters.childClass == childClass && this.filters.referenceType == referenceType)
            return;
        this.filters.elementClass = elementClass;
        this.filters.childClass = childClass;
        this.filters.referenceType = referenceType;
        if(elementClass == undefined && childClass == undefined && referenceType == undefined)
        {
            for(var i in this.nameList)
            {
                if(this.nameList.hasOwnProperty(i))
                    delete this.nameList[i].enabled;
            }
            this.enabled = true;
        } else
        {
            this.nameList = null;
            this.nameMap = {};
        }
    };
    
    /**
     * If callback is specified, then call will be asynchronous, otherwise result will be returned in synchronous manner
     * @param name - name of the element to get info about
     * @param callback
     * @return
     */
    this.getElementInfo = function(name, callback)
    {
        if(this.nameMap[name] !== undefined)
        {
            if(callback) callback(this.nameList[this.nameMap[name]], this.enabled);
            return this.nameList[this.nameMap[name]];
        }
        if(this.invalidList[name] != undefined)
        {
            if(callback) callback(null);
            return null;
        }
        var parameters = 
        {
            dc: this.completeName,
            childClassName: this.filters.childClass == undefined?"":this.filters.childClass,
            elementClassName: this.filters.elementClass == undefined?"":this.filters.elementClass,
            referenceTypeName: this.filters.referenceType == undefined?"":this.filters.referenceType,
            name: name,
            chunk: NAME_LIST_CHUNK_SIZE
        };
        if(callback)
        {
            var callCallback = function()
            {
                if (_this.nameMap == undefined || _this.nameMap[name] == undefined || _this.nameList[_this.nameMap[name]] == undefined) 
                {                
                    _this.invalidList[name] = 1;
                    callback(null);
                }
                else
                    callback(_this.nameList[_this.nameMap[name]], _this.enabled);
            };
            queryService("access.service", 29, parameters, function(data) {
                fillElementInfo(data);
                callCallback();
            }, function(data) {
                callCallback();
            });
        } else
        {
            fillElementInfo(queryService("access.service", 29, parameters));
            if (this.nameMap == undefined || this.nameMap[name] == undefined || this.nameList[this.nameMap[name]] == undefined) 
            {                
                this.invalidList[name] = 1;
                return null;
            }
            else 
                return this.nameList[this.nameMap[name]];
        }
    };
    
    this.getElementInfoAt = function(index, callback)
    {
        if(callback)
        {
            this.getElementInfoRange(index, index+1, function(info)
            {
                callback(info?info[0]:undefined, this.enabled);
            });
        } else
        {
            var info = this.getElementInfoRange(index, index+1);
            return info?info[0]:undefined;
        }
    };
    
    this.getElementInfoRange = function(from, to, callback)
    {
        var newFrom = Math.floor(from/NAME_LIST_CHUNK_SIZE)*NAME_LIST_CHUNK_SIZE;
        var newTo = Math.floor((to-1)/NAME_LIST_CHUNK_SIZE)*NAME_LIST_CHUNK_SIZE+NAME_LIST_CHUNK_SIZE;
        if(this.nameList != undefined)
        {
            while(newFrom < newTo && this.nameList[newFrom]) newFrom+=NAME_LIST_CHUNK_SIZE;
            while(newTo > newFrom && this.nameList[newTo-NAME_LIST_CHUNK_SIZE]) newTo-=NAME_LIST_CHUNK_SIZE;
            if(newTo == newFrom)
            {
                if(callback)
                {
                    callback(this.nameList.slice(from, to), this.enabled);
                    return;
                } else
                {
                    return this.nameList.slice(from, to);
                }
            }
        }
        var parameters = 
        {
            dc: this.completeName,
            childClassName: this.filters.childClass == undefined?"":this.filters.childClass,
            elementClassName: this.filters.elementClass == undefined?"":this.filters.elementClass,
            referenceTypeName: this.filters.referenceType == undefined?"":this.filters.referenceType,
            from: newFrom,
            to: newTo
        };
        if(callback)
        {
            queryService("access.service", 29, parameters, function(data) {
                fillElementInfo(data);
                callback(_this.nameList.slice(from, to), this.enabled);
            });
        } else
        {
            fillElementInfo(queryService("access.service", 29, parameters));
            if(this.nameList == undefined)
                return [];
            return this.nameList.slice(from, to);
        }
    };
    
    var thisClass = undefined;
    this.getClass = function()
    {
        if(thisClass === undefined)
        {
            var parent = getDataCollection(getElementPath(this.completeName));
            thisClass = parent == null?"ru.biosoft.access.core.DataCollection":parent.getChildClass(getElementName(this.completeName));
        }
        return thisClass;
    };
    
    this.getChildClass = function(name)
    {
        if(!instanceOf(this.getClass(),"ru.biosoft.access.core.DataCollection"))
            return null;
        if(name!=null)
        {
            var info = this.getElementInfo(name);
            if(info) return info["class"];
        }
        var type = this.getAttributes()['data-element-class'];
        if(instanceOf(type,"ru.biosoft.access.generic.DataElementInfo"))
        {
            type = "ru.biosoft.access.core.DataElement";
            if(name!=null)
            {
                var info = this.getElementInfo(name);
                if(info) type = info["class"];
            }
        }
        return type;
    };
    
    this.isChildLeaf = function(name)
    {
        var info = this.getElementInfo(name);
        if(info) return !info.hasChildren;
        return true;
    };
    
    /*
     * Get collection attributes
     */
    this.getAttributes = function(callback)
    {
        if(callback)
        {
            this.loadAttributes(function() {callback(this.attributes);});
        } else
        {
            this.loadAttributes();
            return this.attributes;
        }
    };
    
    /*
     * Get collection size
     */
    this.getSize = function(callback)
    {
        if(!callback)
        {
            if(this.size == -1)
            {
                this.getElementInfoAt(0);
            }
            return this.size;
        }
        if (this.size == -1) 
        {
            this.getElementInfoAt(0, function()
            {
                callback(_this.size == -1 ? 0 : _this.size, _this.enabled);
            });
        }
        else 
        {
            callback(_this.size, _this.enabled);
        }
    };
    
    /*
     * Get link target (if this collection is a link)
     */
    this.getLinkTarget = function(callback)
    {
        if(!callback)
        {
            if(this.linkTarget === undefined)
            {
                if(!instanceOf(this.getClass(), "ru.biosoft.access.core.DataCollection"))
                    this.linkTarget = null;
                else
                    this.getElementInfoAt(0);
            }
            return this.linkTarget;
        }
        if (this.linkTarget === undefined) 
        {
            if(!instanceOf(this.getClass(), "ru.biosoft.access.core.DataCollection"))
            {
                this.linkTarget = null;
                callback(_this.linkTarget);
            }
            this.getElementInfoAt(0, function()
            {
                callback(_this.linkTarget);
            });
        }
        else 
        {
            callback(_this.linkTarget);
        }
    };
    
    /**
     * Returns array of known symlinks which point to current collection
     */
    this.getBackLinks = function()
    {
        var links = [];
        for(var i in this.backLinks)
            links.push(i);
        return links;
    };
    
    /*
     * Get collection name list
     */
    this.getNameList = function(success, failure)
    {
        if(this.size >= 0)
        {
            this.getElementInfoRange(0, this.size, success);
            return;
        }
        queryService("access.service", 29, 
        {
            childClassName: this.filters.childClass == undefined?"":this.filters.childClass,
            elementClassName: this.filters.elementClass == undefined?"":this.filters.elementClass,
            referenceTypeName: this.filters.referenceType == undefined?"":this.filters.referenceType,
            dc: this.completeName
        }, function(data)
        {
            fillElementInfo(data);
            success(_this.nameList, this.enabled);
        }, failure);
    };
    
    this.convertNameList = function(nameList, types, icons)
    {
        if(nameList == undefined) return undefined;
        for(var i=0; i<nameList.length; i++)
        {
            nameList[i]["class"] = types[nameList[i]["class"]];
            if(nameList[i]["icon"] != undefined)
                nameList[i]["icon"] = icons[nameList[i]["icon"]];
        }
        return nameList;
    };
    
    this.getNodeIcon = function(name)
    {
        var info = this.getElementInfo(name);
        if(info) return info.icon;
        return undefined;
    };
    
    /*
     * Get type of protection
     */
    this.getProtectionStatus = function(name)
    {
        var info = this.getElementInfo(name);
        if(info && info.protection!=undefined) return info.protection;
        return -1;
    };
    
    /*
     * Get permission for some types of protection
     */
    this.getPermission = function(name)
    {
        var info = this.getElementInfo(name);
        if(info && info.permissions!=undefined) return info.permissions;
        return 0xFF;
    };
    
    this.isMutable = function()
    {
        var path = this.completeName;
        while(true)
        {
            var name = getElementName(path);
            path = getElementPath(path);
            if(!path) return true;
            if(!(getDataCollection(path).getPermission(name) & 0x04)) return false;
        }
    };
    
    this.isJournal = function()
    {
        if(this.journal == undefined)
        {
            var data = queryBioUML('web/journal/module', {
                de: this.completeName
            }, undefined, function(){});
            if(data.type == QUERY_TYPE_SUCCESS)
            {
                this.journal = true; 
            } else
            {
                this.journal = false;
            }
        }
        return this.journal;
    };

    this.isGitEnabled = function()
    {
        if(this.gitEnabled == null)
        {
            this.gitEnabled = false;
            var localCollection = this; 
            var data = queryBioUML('web/git/isEnabled', { de: this.completeName }, 
                undefined, function(){}
            );

            if( data.type == QUERY_TYPE_SUCCESS )
            {
                /*
                console.log( "sync: isGitEnabled[" + localCollection.completeName + "]: '" + data.values + "'" );
                console.log( data );
                console.log( "data.values == ok: " + ( ["ok", "true"].indexOf( data.values ) != -1 ) );
                */ 
                localCollection.gitEnabled = ["ok", "true"].indexOf( data.values ) != -1;
            }

        }
        return this.gitEnabled;
    };
    
    /*
     * Get element by name
     */
    this.get = function(name)
    {
        if (!this.elements[name]) 
        {
            this.elements[name] = new DataCollection(createPath(this.getName(), name));
        }
        
        return this.elements[name];
    };
    
    /*
     * Remove element from data collection
     */
    this.remove = function(name)
    {
        if (this.elements[name]) 
        {
            this.elements[name].fireRemoved();
            delete this.elements[name];
            var nameIndex = this.nameMap[name];
            if(nameIndex == undefined)
                return;
            delete this.nameMap[name];
            this.nameList.splice(nameIndex,1);
            for(var i=nameIndex; i < this.nameList.length; i++)
            {
                this.nameMap[this.nameList[i].name] = i;
            }
        }
    };
    /*
     * Get bean as HTML, optionally use template
     */
    this.getHtml = function(callback, templateName)
    {
        var params = {de: completeName};
        if(templateName)
            params["templateName"] = templateName;
        var _this = this;
        queryBioUML("web/html", params, 
        function(data)
        {
            _this.info = data.values.html;
            _this.templateList = data.values.templates;
            callback(_this.info, _this.templateList);
        }, function(data) {});
    };
    
    /*
     * Get bean as DPS
     */
    this.getBean = function(callback)
    {
        if (this.beanDPS == null) 
        {
            var _this = this;
            queryBean(completeName, {}, function(data)
            {
                _this.beanDPS = convertJSONToDPS(data.values);
                callback(_this.beanDPS);
            }, function(data) {});
        }
        else 
        {
            callback(this.beanDPS);
        }
    };
    
    /*
     * Get bean as DPS
     */
    this.getBeanFields = function(fieldNames, callback)
    {
        var _this = this;
        queryBioUML("web/bean", 
        {
            action: "get",
            showMode: SHOW_HIDDEN,
            de: completeName,
            fields: fieldNames
        }, function(data)
        {
            var result = convertJSONToDPS(data.values);
            callback(result);
        });
    };
    
    /*
     * If current collection is diagram, this will return its type
     */
    var diagramTypeInfo;
    this.getDiagramTypeInfo = function(callback)
    {
        if(callback)
        {
            if(diagramTypeInfo)
            {
                callback(diagramTypeInfo);
            } else
            {
                queryBioUML('web/diagram/get_type', {
                    de: _this.completeName
                }, function(data) {
                    diagramTypeInfo = data.values;
                    callback(diagramTypeInfo);
                });
            }
        } else
        {
            if(!diagramTypeInfo)
            {
                data = queryBioUML('web/diagram/get_type', {
                    de: _this.completeName
                });
                diagramTypeInfo = data.values;
            }
            return diagramTypeInfo;
        }
    };
    
    this.getDiagramType = function(callback)
    {
        if(callback)
        {
            this.getDiagramTypeInfo(function(info)
            {
                callback(info.type);
            });
        } else return this.getDiagramTypeInfo().type;
    };
    
    this.getDiagramTypes = function(callback)
    {
        var _this = this;
        if(this.diagramTypes != undefined)
        {
            callback(this.diagramTypes);
        } else
        {
            var diagramTypesParameters = {
                    dc: this.completeName,
                    command: 213,
                    service: "diagram.service"
                };
            queryBioUML("web/data", diagramTypesParameters, function(data)
            {
                var diagramTypes = $.parseJSON(data.values);
                _this.diagramTypes = diagramTypes;
                callback(diagramTypes);
            });
        }
    };

    /*
     * If current collection is diagram, this will return true if diagram has model, false otherwise 
     */
    this.diagramHasModel = function(callback)
    {
        if(callback)
        {
            this.getDiagramTypeInfo(function(info)
            {
                callback(info.model == "true");
            });
        } else return this.getDiagramTypeInfo().model == "true";
    };
    
    /*
     * Get bean as HTML
     */
    this.getDescription = function(callback)
    {
        var _this = this;
        var type = this.getClass();
        var fieldNames = new Array();
        if (instanceOf(type,"biouml.model.Diagram") || instanceOf(type,"ru.biosoft.access.SqlDataCollection") ||
                instanceOf(type,"ru.biosoft.bsa.SqlTrack") ||
                instanceOf(type,"ru.biosoft.table.TableDataCollection") || instanceOf(type,"ru.biosoft.access.core.FolderCollection")
                 || instanceOf(type,"ru.biosoft.access.LocalRepository") || instanceOf(type, "ru.biosoft.bsa.classification.ClassificationUnit") ) 
        {
            fieldNames.push("description");
        }
        else if(instanceOf(type,"biouml.model.Module"))
        {
            fieldNames.push("descriptionHTML");
        }
        else
        {
            callback(null);
            return;
        }
        this.getBeanFields(fieldNames.join("/")+";baseId", function(beanDPS)
        {
            var prop = beanDPS;
            var p = prop.getProperty("baseId");
            var baseId;
            if(p && p.getValue()) baseId = p.getValue();
            for(var i = 0; i < fieldNames.length; i++)
            {
                if (prop) 
                {
                    var p = prop.getProperty(fieldNames[i]);
                    if (p && p.getValue()) 
                        prop = p.getValue();
                    else 
                        prop = null;
                }    
            }
            var description = "";
            if (prop && prop != beanDPS) 
            {
                description = prop;
                if(baseId) description = description.replace(/img src=\"/g, "img src=\""+appInfo.serverPath+"web/img?id="+baseId+"/");
            }
            else 
            {
                description = "";
            }
            callback(description, fieldNames, beanDPS);
        });
    };
    
    /*
     * Save bean changes
     */
    this.setBean = function(dps, callback)
    {
        this.beanDPS = null;
        this.info = null;
        var _this = this;
        queryBioUML("web/bean", 
        {
            action: "set",
            de: completeName,
            json: convertDPSToJSON(dps)
        }, function(data)
        {
            _this.fireChanged();
            callback();
        });
    };
    
    var attributesLoaded = false;
    this.loadAttributes = function(callback)
    {
        var success = function(data) {
            _this.attributes = parseProperties(data.values);
            attributesLoaded = true;
        };
        if (!attributesLoaded) 
        {
            var _this = this;
            // load data collection info
            var result = queryService("access.service", 21,
            {
                dc: getElementPath(this.completeName),
                de: getElementName(this.completeName)
            }, callback ? function(data) {success(data);callback();} : undefined,
            function(data) {
                _this.attributes = {};
                attributesLoaded = true;
                if(callback) callback();
            });
            if(!callback && result.type == QUERY_TYPE_SUCCESS) success(result);
        } else if(callback) callback();
    };
    
    /**
     * If callback is specified, then call will be asynchronous, otherwise result will be returned in synchronous manner
     * @param name - name of the element to get info about
     * @param callback
     * @return element descriptor as text properties
     */
    //TODO: store descriptor to avoid redundant queries
    this.getElementDescriptor = function(name, callback)
    {
        if(this.invalidList[name] != undefined)
        {
            if(callback) callback(null);
            return null;
        }
        var parameters = 
        {
            dc: this.completeName,
            de: name
        };
        if(callback)
        {
            queryService("access.service", 38, parameters, function(data) {
                if(data.type != QUERY_TYPE_SUCCESS)
                    callback(null);
                else
                    callback(parseProperties(data.values));
                callback(data);
            }, function(data) {
                callback(null);
            });
        } else
        {
            var data = queryService("access.service", 38, parameters);
            if(data.type != QUERY_TYPE_SUCCESS)
                return null;
            else
                return parseProperties(data.values);
        }
    };

    this.invalidateCollection = function()
    {
        var validelements = {};
        for (child in this.elements) 
        {
            var info = this.getElementInfo(child);
            if (info) 
            {
                validelements[child] = this.elements[child];
                validelements[child].invalidateCollection();
            }
            else 
            {
                this.elements[child].fireRemoved();
            }
        }
        attributesLoaded = false;
        this.elements = validelements;
        this.nameList = null;
        this.nameMap = {};
        this.invalidList = [];
        this.size = -1;
        this.info = null;
        this.templateList = null;
        this.beanDPS = null;
        if(this.getLinkTarget())
            getDataCollection(this.getLinkTarget()).invalidateCollection();
    };
    
    this.initLinkTargets = function()
    {
        var data = queryService("access.service", 40, {dc: this.completeName}); 
        if(data.type == QUERY_TYPE_SUCCESS)
        {
            if(data.values)
            {
                _this.linkTarget = data.values;
                getDataCollection(_this.linkTarget).backLinks[_this.completeName] = 1;
            } 
            else 
                _this.linkTarget = null;
        }
    };
    
    
    /*
     * Collection changes processing
     */
    this.addChangeListener = function(listener)
    {
        var alreadyAdded = false;
        for (li = 0; li < this.changeListeners.length; li++)
        {
            if (this.changeListeners[li] == listener)
            {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded)
        {
            this.changeListeners.push(listener);
        }
    };
    
    this.fireChanged = function()
    {
        for (li = 0; li < this.changeListeners.length; li++)
        {
            this.changeListeners[li].dataCollectionChanged();
        }
        this.propagateChanged();
    };
    
    this.propagateChanged = function()
    {
        var parentPath = getElementPath(this.completeName);
        if (parentPath) 
        {
            var parentDC = getDataCollection(parentPath);
            parentDC.fireChanged();
        }
    };
    
    this.removeChangeListener = function(listener)
    {
        var newListeners = new Array();
        for (li = 0; li < this.changeListeners.length; li++)
        {
            if (this.changeListeners[li] != listener)
            {
                newListeners.push(this.changeListeners[li]);
            }
        }
        this.changeListeners = newListeners;
    };
    /*
     * Collection removal processing
     */
    
    this.addRemoveListener = function(listener)
    {
        var alreadyAdded = false;
        for (li = 0; li < this.removeListeners.length; li++)
        {
            if (this.removeListeners[li] == listener)
            {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded)
        {
            this.removeListeners.push(listener);
        }
    };
    
    this.fireRemoved = function()
    {
        for (li = 0; li < this.removeListeners.length; li++)
        {
            this.removeListeners[li].dataCollectionRemoved();
        }
        this.propagateChanged();
    };
    
    this.removeRemoveListener = function(listener)
    {
        var newListeners = new Array();
        for (li = 0; li < this.removeListeners.length; li++)
        {
            if (this.removeListeners[li] != listener)
            {
                newListeners.push(this.removeListeners[li]);
            }
        }
        this.removeListeners = newListeners;
    };
    
    this.isDataCollectionClass = function()
    {
        return instanceOf(this.getClass(), 'ru.biosoft.access.core.DataCollection');
    }
    
    this.getSizeWithCallbacks = function(success, failure)
    {
        var parameters = 
        {
            dc: this.completeName,
            childClassName: this.filters.childClass == undefined?"":this.filters.childClass,
            elementClassName: this.filters.elementClass == undefined?"":this.filters.elementClass,
            referenceTypeName: this.filters.referenceType == undefined?"":this.filters.referenceType,
            from: 0,
            to: 1
        };
        
        queryService("access.service", 29, parameters, function(data) {
            if(data.type != QUERY_TYPE_SUCCESS)
            {
                if(_this.size < 0) _this.size = 0;
                if(failure)
                    failure(0);
            }
            else
            {
                let values = $.parseJSON(data.values);
                _this.size = values.size;
                if(success)
                    success(_this.size);
            }
        }, function(data){
            logger.error(data.message);
            if(failure)
                failure();
        });
    }
}
