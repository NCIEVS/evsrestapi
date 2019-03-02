/* Scan all links in the document and set classes on them if
 * they point outside the site, or are special protocols
 * To disable this effect for links on a one-by-one-basis,
 * give them a class of 'link-plain'
 */

function scanforlinks(){
	var extArray = new Array ('swf', 'mov', 'doc', 'pdf', 'ppt', 'rm', 'wmv', 'xls', 'zip');
	// securing against really old DOMs 

	// terminate if we hit a non-compliant DOM implementation
	if (!W3CDOM) { return false; }
	
	contentarea = getContentArea()
	if (! contentarea){return false}
	
	links = contentarea.getElementsByTagName('a');
	for (i=0; i < links.length; i++){      
		if ((links[i].getAttribute('href'))&&(links[i].className.indexOf('link-plain')==-1 )){
			var linkval = links[i].getAttribute('href');
			// check if the link href is a relative link, or an absolute link to the current host.
			if (linkval.toLowerCase().indexOf(window.location.protocol+'//'+window.location.host)==0) {
				// we are here because the link is an absolute pointer internal to our host

				// vps - show various icons if filename has extensions(.pdf, .xls, .ppt, .doc, .zip, .rm, .wmv) in it
				for (j = 0; j <= extArray.length; j++) {
					fileExt = '.' + extArray[j];
					if (linkval.toLowerCase().substring((linkval.length-4), linkval.length).indexOf(fileExt) == 0) {
							//one of the non-html files, add the appropriate class
							wrapNode(links[i], 'span', 'link-' + extArray[j]);
					}
				}

				if (linkval.toLowerCase().substring((linkval.length-3), linkval.length).indexOf('.rm') == 0) {
						//one of the non-html files, add the appropriate class
						wrapNode(links[i], 'span', 'link-rm');
				}

				// do nothing
		        } else if (linkval.indexOf('http:') != 0 && linkval.indexOf('https:') != 0){
				// not a http-link. Possibly an internal relative link, but also possibly a mailto ot other snacks
				// add tests for all relevant protocols as you like.
				
				protocols = ['mailto', 'ftp', 'news', 'irc', 'h323', 'sip', 'callto']
				// h323, sip and callto are internet telephony VoIP protocols
				
				for (p=0; p < protocols.length; p++){  
					 if (linkval.indexOf(protocols[p]+':') == 0){
					// this link matches the protocol . add a classname protocol+link
					//links[i].className = 'link-'+protocols[p]
						wrapNode(links[i], 'span', 'link-'+protocols[p])
						break;
					}
				}

				// vps - show various icons if filename has extensions(.swf, .mov, .pdf, .xls, .ppt, .doc, .zip, .rm, .wmv) in it
				for (s = 0; s <= extArray.length; s++) {
					fileExt = '.' + extArray[s];
					if (linkval.toLowerCase().substring((linkval.length-4), linkval.length).indexOf(fileExt) == 0) {
							//one of the non-html files, add the appropriate class
							wrapNode(links[i], 'span', 'link-' + extArray[s]);
					}
				}

				if (linkval.toLowerCase().substring((linkval.length-3), linkval.length).indexOf('.rm') == 0) {
						//one of the non-html files, add the appropriate class
						wrapNode(links[i], 'span', 'link-rm');
				}
			} else {
				// we are in here if the link points to somewhere else than our site.
                                
				if ( links[i].getElementsByTagName('img').length == 0 ) {

					// vps - show various icons if filename has extensions(.pdf, .xls, .ppt, .doc, .zip, .rm, .wmv) in it
					for (j = 0; j <= extArray.length; j++) {
						fileExt = '.' + extArray[j];
						if (linkval.toLowerCase().substring((linkval.length-4), linkval.length).indexOf(fileExt) == 0) {
								//one of the non-html files, add the appropriate class
								wrapNode(links[i], 'span', 'link-' + extArray[j]);
						}
					}

					if (linkval.toLowerCase().substring((linkval.length-3), linkval.length).indexOf('.rm') == 0) {
							//one of the non-html files, add the appropriate class
							wrapNode(links[i], 'span', 'link-rm');
					}

					// we do not want to mess with those links that already have images in them
					//links[i].className = 'link-external'
					wrapNode(links[i], 'span', 'link-external')
					//links[i].setAttribute('target','_blank')
				}					
			}
		}
	}
}
registerPloneFunction(scanforlinks) 
