function changeMenuStyle(obj,new_style) {
  obj.className = new_style
}

function showCursor() {
    document.body.style.cursor = "hand"
}

function hideCursor() {
    document.body.style.cursor = "default"
}

function confirmDelete() {
    if (confirm("Are you sure you want to delete?"))
        return true
    return false
}

function setUrl ( url, target ) {
  if ( target == '_self' ) {
    document.location.href = url;
  } else {
    open(url,target);
  }
}

function spawn(url,winw,winh) {
  var w = window.open(url, "_blank",
      "screenX=0,screenY=0,status=yes,toolbar=yes,menubar=yes,location=yes,width=" + winw + ",height=" + winh +
      ",scrollbars=yes,resizable=yes");
}

function spawnX(url,winw,winh) {
  var w = window.open(url, "_blank",
      "screenX=0,screenY=0,status=no,toolbar=yes,menubar=no,location=no,width=" + winw + ",height=" + winh +
      ",scrollbars=yes,resizable=yes");
}

function showWindow(imgscr) {
       myWind = window.open(imgscr,"subWindow","HEIGHT=520,WIDTH=520,resizable");
       myWind.focus()
}

function URLencode(sStr) {
    return escape(sStr).replace(/\+/g, '%2C').replace(/\"/g,'%22').replace(/\'/g, '%27')
}

// *********************** Begin URL encode javascript function from CMAP **********************

var active = 'false';
var NS6 = (document.getElementById && !document.all) ? true : false;

function ifenter(e,form) {
  if (!active) return;

  var keycode;
  if (window.event)         // IE
    keycode = window.event.keyCode;
  else
  if (e) {                  // NS
    if (NS6)
      keycode = e.keyCode;  // NS6: this shows bksp, enter ...
    else
      keycode = e.which;    // NS6: real   NS4: all keys
  }
  else return true;

  if (NS6) {
    if (keycode == 13) {
      Search(form);
      e.cancelBubble = true;
      return false;
    }
    else
      return true;
  }
  else if (keycode == 13) {
    Search(form);
    return false;
  }
  else
    return true;
}

function Search(form) {

    var searchTerm1 = document.SiteSearch.term.value;
    var searchTerm = searchTerm1.replace(/ /g, "%20");

    if ( ( searchTerm == null ) || ( searchTerm == "" ) || ( searchTerm == " ") ) {
        alert("Please enter a search term.");
    } else {
        location = '/NCICB/ZopeSearch?start:int=0&term=' + searchTerm;
    }

}

function setSize() {
 if(!document.layers) {
   document.forms[0].term.size=15;
  }
}
