highlight = function(timePoint) {
	$('.highlight').removeClass('highlight');
	$('[data-start=' + timePoint + ']').addClass('highlight');
}

adjustLayout = function(topMargin_, bottomMargin_, height_, accentColor_, textColor_, backgroundColor__, newWordColor_) {
	backgroundColor_ = backgroundColor__;

	topMargin = topMargin_ / window.devicePixelRatio;
	bottomMargin = bottomMargin_ / window.devicePixelRatio;
	height = height_ / window.devicePixelRatio;

	textColor = (textColor_ & 0xffffff).toString(16);
	while(textColor.length < 6) textColor = '0' + textColor;

	accentColor = (accentColor_ & 0xffffff).toString(16);
	while(accentColor.length < 6) accentColor = '0' + accentColor;

	backgroundColor = (backgroundColor_ & 0xffffff).toString(16);
	while(backgroundColor.length < 6) backgroundColor = '0' + backgroundColor;

	newWordColor = (newWordColor_ & 0xffffff).toString(16);
	while(newWordColor.length < 6) newWordColor = '0' + newWordColor;

	highlihtColor = Math.floor((accentColor_ & 0xffffff) / 1.5).toString(16);
	while(highlihtColor.length < 6) highlihtColor = '0' + highlihtColor;
	highlihtColor = '#' + highlihtColor;

	$('#dynamic-rules').text("\
		.card { margin-top: " + (topMargin+28) + "px; background-color: #" + accentColor + "; } \
		.highlight { background-color: " + highlihtColor + "; } \
		body { background-color: #" + backgroundColor + "; color: #" + textColor + "; } \
		.accent { color: #" + accentColor + "; } \
		.new-word, dd, dt { color: #" + newWordColor + "; } \
		#page-indicator { margin-top: " + topMargin + "px; } \
		.container { margin-top: " + (topMargin+28) + "px; height: " + (height-topMargin-bottomMargin-28) + "px; } \
		h1 { color: #" + newWordColor + "; } \
		h1.alt { color: #" + accentColor + "; } \
		h3.alt { color: #" + accentColor + "; } \
		.swipe-wrap > div { height: " + height + "px; } \
		#properties { background-color: " + highlihtColor + "; } \
		.card-content { color: #" + newWordColor + "; } \
		.dot { background-color: #" + textColor + "; } \
		.dot.active { background-color: #" + newWordColor + "; } "
	);

	adjustCustomLayout();
}

if(typeof(app) == 'undefined') { // on web browser
	$(document).ready(function() {
		if(typeof(lock) == 'function') {
				$('body').append('<button id="buttonToggleLock" style="position: absolute; top: 0; z-index: 2;">Toggle Show/Hide</button>');
				$('#buttonToggleLock').click(function() {
					lock(!transcriptLocked);
				});
		}

		if(typeof(setCurrentSlide) == 'function')
			$('body').append("<button  style='position: absolute; top: 0; float: right; right: 50px; z-index: 2;' onclick='swipeable.prev()'>Previous</button> <button  style='position: absolute; top: 0; float: right; right: 0; z-index: 2;' onclick='swipeable.next()'>Next</button>");

		//adjustLayout(0, 0, $(window).height(), 0x9688, 0x888888, 0xf5f5f5, 0);
		adjustLayout(0, 0, $(window).height(), 0x9688, 0xc5c5c5, 0x212121, 0xf8f8f8);

		if(typeof(lock) == 'function')
			lock(false);

		if(typeof(setCurrentSlide) == 'function')
			setCurrentSlide(0);
	});

} else { // on mobile app
	$(document).ready(function() {
		$('a').click(function() {
			var rect = this.getBoundingClientRect();
			app.showGlossary($(this).data('word'), rect.left  * window.devicePixelRatio, rect.top * window.devicePixelRatio, $(this).height() * window.devicePixelRatio);
			return false;
		});
	});
}
