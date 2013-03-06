channel = {}

channel.open_channel = function(token) {
	var channel = new goog.appengine.Channel(token);

	var onError = function(error) {
		console.log(error);
	};
//	var onMessage = function(message) {
//		core.listener.report(message.data);
//	};
	var handler = {
			'onopen': function() {},
			'onmessage': core.controller.onmessage_channel,
			'onerror': onError,
			'onclose': function() {}
	};
	
    var	socket = channel.open(handler);
};
