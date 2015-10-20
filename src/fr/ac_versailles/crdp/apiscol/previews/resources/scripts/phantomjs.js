var page = require('webpage').create();
page.open('${url}', function() {
	page.render('output.${ext}');
	phantom.exit();
});
