var ServerMessages = new function() {
  var handlers = {};

  this.subscribe = function(messageType, messageHandler, callback) {
    if(!(messageType in handlers))
      handlers[messageType] = [];
    handlers[messageType].push(messageHandler);
    queryBioUML("web/serverMessages/subscribe", {
      "msgType": messageType
    }, function(data) {
      console.log("Subscribed to " + messageType);
      if(callback)
        callback(data);
    });
  }

  this.unsubscribe = function(messageType, messageHandler, callback) {
    if(messageType in handlers) {
      var handlersForType = handlers[messageType];
      if(messageHandler) {
        var filtered = [];
        for(var i = 0; i < handlersForType.length; i++)
          if(handlersForType[i] != messageHandler)
            filtered.push(handlersForType[i]);
      }
      else handlersForType = [];
      handlers[messageType] = handlersForType;
    }
    queryBioUML("web/serverMessages/unsubscribe", {
      "msgType": messageType
    }, function(data) {
      console.log("Unsubscribed from " + messageType);
      if(callback)
        callback(data);
    });
  }

  function dispatch(message) {
      console.log("Dispatching ");
      console.log(message);
      var messageType = message.type;
      var messageContent = message.content;
      if(messageType) {
        if(messageType in handlers) {
          var handlersForType = handlers[messageType];
          for(var j = 0; j < handlersForType.length; j++)
            handlersForType[j](messageContent);
        } else {
          console.log("No handler for " + message);
        }
      }
  }

  function run() {
    queryBioUML("web/serverMessages/listen", {},
    function(data) {
      try {
        dispatch(data.values);
      } catch(e) {
        console.error("Error dispatching server message", e);
      } finally {
        run();
      }
    },
    function() {
      console.log("Server query failed, retrying in 1s");
      setTimeout(run, 1000);
    });
  }

  this.start = run;
}
