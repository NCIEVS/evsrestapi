//----------------------------------------------------------------------------//
// Note: The Firefox browser is initial configure to display a URL
//   within a separate tab when calling the window.open method.
//   To configure Firefox display within a separate window:
//     * Start up Firefox,
//     * Select Menubar --> Tools --> Options,
//     * Select "Tabs" tab,
//     * Select "a new window" radio button.
//----------------------------------------------------------------------------//
function displayLinkInNewWindow(id) {
  var element = document.getElementById(id);
  var url = element.value;
  if (url != "")
    element.onclick=window.open(url);
  else
	alert("URL not set.");
}

function displayVocabLinkInNewWindow(id) {
  var element = document.getElementById(id);
  var url = element.value;
  if (url != "")
    element.onclick=window.open(url);
  else
	alert("This vocabulary does not have\nan associated home page.");
}

function go(loc) {
  window.location.href = loc;
}

//----------------------------------------------------------------------------//
// From: http://www.interwebby.com/
// 
// Close a window or tab in Firefox with JavaScript…
// February 4th, 2006
// 
// Update... Unfortunately this code no longer works in Firefox 2.x ~ 
// I guess they saw this and fixed it, (for them - broke it for us!).
// 
// Recently at work I was asked to make javascript:window.close(); work in
// FireFox.  I searched hard and long across numerous forums and on each and
// every one the answer was the same - it cannot be done unless the page was
// opened by a script! Or at least it couldn't until I was required to make
// it happen ;-)
// 
// I'm by no means a JavaScript expert - in fact I hardly know any at all -
// but I couldn't believe that this was completely impossible so I came up
// with the following:
// 
// The first step is to fool the browser into thinking that it was opened
// with a script.
// 
//   * window.open('','_parent','');
// 
// This opens a new page, (non-existent), into a target frame/window, 
// (_parent which of course is the window in which the script is executed,
// so replacing itself), and defines parameters such as window size etc, 
// (in this case none are defined as none are needed). Now that the browser 
// thinks a script opened a page we can quickly close it in the standard way...
// 
//   * window.close();
// 
// and there you have it - I told you it was simple! In case you didn't follow
// that, here is the complete solution in two easy steps:
// 
//   1. Copy/paste the following code to the head of your page:
//      <script language="javascript" type="text/javascript">
//        function closeWindow() {
//          window.open('','_parent','');
//          window.close();
//        }
//      </script>
// 
//   2. Set your link like this:
//      <a href="javascript:closeWindow();">Close Window</a>
// 
// and there you have it - a problem that it seemed, (according to my trying to 
// find a solution before coming up with this one anyway), to have baffled everyone
// else.
//----------------------------------------------------------------------------//
function closeWindow() {
  window.open('','_parent','');
  window.close();
}
