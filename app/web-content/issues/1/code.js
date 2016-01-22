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

	adjustCustomLayout();

	// if(typeof(app) == 'undefined')
		// $('.poster').css('height', '100%');
}

if(typeof(app) == 'undefined') { // on web browser
	$(document).ready(function() {
		$('body').append('<button id="buttonToggleLock" style="position: absolute; top: 0; z-index: 2;">Toggle Show/Hide</button>');
		$('#buttonToggleLock').click(function() {
			lock(!transcriptLocked);
		});

		$('body').append("<button  style='position: absolute; top: 0; float: right; right: 50px; z-index: 2;' onclick='swipeable.prev()'>Previous</button> <button  style='position: absolute; top: 0; float: right; right: 0; z-index: 2;' onclick='swipeable.next()'>Next</button>");

		adjustLayout(0, 0, $(window).height(), 0x9688, 0xc5c5c5, 0x212121, 0xf8f8f8);
		lock(transcriptLocked);
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
