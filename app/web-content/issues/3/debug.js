if(typeof(app) == 'undefined') { // on web browser
	$(document).ready(function() {
		if(typeof(lock) == 'function') {
				$('body').append('<button id="buttonToggleLock" style="position: fixed; top: 0; z-index: 2;">Toggle Show/Hide</button>');
				$('#buttonToggleLock').click(function() {
					lock(!window.transcriptLocked);
				});
		}

		if(typeof(setCurrentSlide) == 'function')
			$('body').append("<button  style='position: absolute; top: 0; float: right; right: 50px; z-index: 2;' onclick='swipeable.prev()'>Previous</button> <button  style='position: absolute; top: 0; float: right; right: 0; z-index: 2;' onclick='swipeable.next()'>Next</button>");

		adjustLayout({
			topMargin: 12,
			horizontalMargin: 10,
			verticalMargin: 16,
			bottomMargin: 30,
			height: $(window).height(),
			backgroundColor: -1,
			accentColor: -16537100,
			primaryColor: -26624,
			textColor: -13487566
		});

		restoreInstanceState('{}');
	});
}
