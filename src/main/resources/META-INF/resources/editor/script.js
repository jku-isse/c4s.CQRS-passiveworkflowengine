var workspace;

function initBlockly(options) {
  const blocklyArea = document.getElementById('blocklyArea');
  const blocklyDiv = document.getElementById('blocklyDiv');
  
 /**/ var opt = {
        comments: true,
        collapse: true,
        disable: true,
        grid:
          {
            spacing: 25,
            length: 3,
            colour: '#ccc',
            snap: true
          },
        horizontalLayout: false,
        maxBlocks: Infinity,
        maxInstances: {'test_basic_limit_instances': 3},
        maxTrashcanContents: 256,
        media : 'https://blockly-demo.appspot.com/static/media/', 
        readOnly: false,
        rtl: false,
        move: {
          scrollbars: true,
          drag: true,
          wheel: true,
        },
        toolbox: document.getElementById('toolbox'),
        toolboxPosition: 'start',
        renderer: 'geras',
        zoom:
          {
            controls: true,
            wheel: false,
            startScale: 1.0,
            maxScale: 4,
            minScale: 0.25,
            scaleSpeed: 1.1
          },
        sounds : false, 
        oneBasedIndex : false
};/**/

  Blockly.FieldMultilineInput.prototype.spellcheck_ = false;
  workspace = Blockly.inject(blocklyDiv, opt);
  

  const onresize = function (e) {
    // Compute the absolute coordinates and dimensions of blocklyArea.
    let element = blocklyArea;
    let x = 0;
    let y = 0;
    do {
      x += element.offsetLeft;
      y += element.offsetTop;
      element = element.offsetParent;
    } while (element);
    // Position blocklyDiv over blocklyArea.
    blocklyDiv.style.left = x + 'px';
    blocklyDiv.style.top = y + 'px';
    blocklyDiv.style.width = blocklyArea.offsetWidth + 'px';
    blocklyDiv.style.height = blocklyArea.offsetHeight + 'px';
    Blockly.svgResize(workspace);
  };
  window.addEventListener('resize', onresize, false);
  onresize();
  Blockly.svgResize(workspace);
}

function enableAutoBackup() {
  // Auto-save in browser local storage
  BlocklyStorage.backupOnUnload();
  setTimeout(BlocklyStorage.restoreBlocks, 0);
}

function load(model) {
  const dom = Blockly.Xml.textToDom(model);
  workspace.clear();
  Blockly.Xml.domToWorkspace(dom, workspace);
}

function downloadFile(text, filename, type) {
  const blob = new Blob([text], { type: type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  setTimeout(() => {
    URL.revokeObjectURL(url);
  }, 1000);
}

function transformXMLtoJson(xml, url) {
	var req = new XMLHttpRequest();
	req.open('POST',url+'/transform',false);
	//req.open('POST','http://localhost:7171/transform', false);
	//req.open('POST','http://140.78.115.5:7171/transform', false);
	req.setRequestHeader('Content-Type', 'application/xml');
	req.send(xml );
	var response = req.response;
	console.log(response);
	var errorResp = req.errorResp;
	if (errorResp)
		console.log(errorResp);
	return JSON.parse(response);
}

function deployToDS(xml, url) {
	var req = new XMLHttpRequest();
	req.open('POST',url+'/deploySnapshotFromXML', false);
	//req.open('POST','http://localhost:7171/deploySnapshotFromXML', false);
	//req.open('POST','http://140.78.115.5:7171/deploySnapshotFromXML', false);
	req.setRequestHeader('Content-Type', 'application/xml');
	req.send(xml);
	var response = req.response;
	console.log(response);
	return JSON.parse(response);
}
