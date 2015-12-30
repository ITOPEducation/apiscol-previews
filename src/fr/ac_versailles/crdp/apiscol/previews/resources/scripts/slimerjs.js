var latency=250, secondTimeout=100;
/**
 * See https://github.com/ariya/phantomjs/blob/master/examples/waitfor.js
 * 
 */
function waitFor(testFx, onReady) {
	var maxtimeOutMillis = ${timeout}, 
	// timeout
	start = new Date().getTime(), condition = (typeof (testFx) === "string" ? eval(testFx)
			: testFx()), interval = setInterval(
			function() {
				if ((new Date().getTime() - start < maxtimeOutMillis)
						&& !condition) {
					condition = (typeof (testFx) === "string" ? eval(testFx)
							: testFx());
				} else {

					if (condition)
						console.log("'waitFor()' finished in "
								+ (new Date().getTime() - start) + "ms.");
					else
						console.log("'waitFor()' timeout after "
								+ (new Date().getTime() - start) + "ms.");
					typeof (onReady) === "string" ? eval(onReady) : onReady();

					clearInterval(interval); // < Stop this interval

				}
			}, latency);
};

var page = require('webpage').create(), address, output, size;

address = '${url}';
output = 'output.${ext}';
if (address.substr(-4) === ".pdf") {
	page.paperSize = {
		format : "A4",
		orientation : 'portrait',
		margin : {
			left : "5mm",
			top : "8mm",
			right : "5mm",
			bottom : "9mm"
		}
	};
}

var resources = [];
page.onResourceRequested = function(request) {
	resources[request.id] = request.stage;
};
page.onResourceReceived = function(response) {
	resources[response.id] = response.stage;

};
page.viewportSize = {
	width : ${viewport_width},
	height : ${viewport_height}
};
page.onResourceError = function(resourceError) {
	console.log('Unable to load resource (#' + resourceError.id + 'URL:'
			+ resourceError.url + ')');
	console.log('Error code: ' + resourceError.errorCode + '. Description: '
			+ resourceError.errorString);
};
page.open(address, function(status) {
	if (status !== 'success') {
		console.log('Unable to load the address with status ' + status);
		phantom.exit();
	} else {
		console.log('Launching snapshot ');

		waitFor(function() {
			for (var i = 1; i < resources.length; ++i) {
				if (resources[i] != 'end') {
					return false;
				}
			}
			return true;
		}, function() {
			var title = page.evaluate(function() {
				return document.title;
			});
			console.log("Page title " + title);
			setTimeout(function() {
				page.render(output);
				phantom.exit();
			}, secondTimeout);
		});
		;

	}
});
