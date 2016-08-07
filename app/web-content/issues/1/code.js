highlight = function(timePoint) {
	$('.highlight').removeClass('highlight');
	$('[data-start=' + timePoint + ']').addClass('highlight');
}

adjustLayout = function(options) {
	topMargin = (options['topMargin'] || 0) / window.devicePixelRatio;
	bottomMargin = (options['bottomMargin'] || 0) / window.devicePixelRatio;
	height = (options['height'] || 0) / window.devicePixelRatio;
	verticalMargin = (options['verticalMargin'] || 0) / window.devicePixelRatio;
	horizontalMargin = (options['horizontalMargin'] || 0) / window.devicePixelRatio;
	spacing = (options['spacing'] || 8) / window.devicePixelRatio;
	normalPadding = (options['normalPadding'] || options['verticalMargin']) / window.devicePixelRatio;

	textColor = ((options['textColor'] || 0) & 0xffffff).toString(16);
	while(textColor.length < 6) textColor = '0' + textColor;

	accentColor = ((options['accentColor'] || 0) & 0xffffff).toString(16);
	while(accentColor.length < 6) accentColor = '0' + accentColor;

	if(tinycolor(textColor).isDark()) {
		highlightColor = tinycolor(accentColor).lighten().lighten().lighten().toHexString();
	}	else {
		highlightColor = tinycolor(accentColor).darken().darken().darken().toHexString();
	}
	newWordColor = tinycolor(textColor).saturate().saturate().toHexString();

	css = "\
		.highlight { background-color: " + highlightColor + "; } \
		body { color: #" + textColor + "; } \
		.accent { color: #" + accentColor + "; } \
		.new-word, dd, dt { color: " + newWordColor + "; } \
	"

	if(options['backgroundColor']) {
		backgroundColor = (options['backgroundColor'] & 0xffffff).toString(16);
		while(backgroundColor.length < 6) backgroundColor = '0' + backgroundColor;
	} else {
		backgroundColor = 'ffffff';
	}

	$('#dynamic-rules').text(css);

	adjustCustomLayout(options);
}

$(document).ready(function() {
	$('a.new-word').click(function() {
		if(typeof(app) != 'undefined') {
			var rect = this.getBoundingClientRect();
			app.showGlossary($(this).data('word'), rect.left  * window.devicePixelRatio, rect.top * window.devicePixelRatio, $(this).height() * window.devicePixelRatio);
		} else {
			alert($(this).data('en') + "\n" + $(this).data('fa'));
		}
		return false;
	});
});
