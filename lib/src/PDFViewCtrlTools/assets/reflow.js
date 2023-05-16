isDebugging = true;
if (isDebugging) {
  console.log("Script Found!!");
}

const AnnotationType = {
  Highlight: 8,
  Underline: 9,
  Strikeout: 11,
  Squiggly: 10
};
const annotationTypes = Object.values(AnnotationType);
const ANNOT_ID = "annot-id";
const annotClassname = "text-annot";
const selectedAnnotClassname = "selected-annot";
const selectedAnnotStartClassname = "selected-annot-start";
const selectedAnnotEndClassname = "selected-annot-end";

this.annotNodeMap = new Map(); //Annotation ID -> [Annotation HTML nodes]
this.addAnnotConfig = {}; //{type: AnnotationType, color: string, opacity: number}
this.selectionStyle = undefined;
this.selectedAnnot = undefined; //{color, opacity, type, target}

var ReflowJS = {};

ReflowJS.loadAnnotations = (annots) => {
  const paragraphs = document.getElementsByTagName('p');

  // Get offset for each paragraph
  const offsets = [0];
  for (const p of paragraphs) {
    offsets.push(offsets[offsets.length - 1] + p.textContent.length);
  }

  // Get all annotations
  const allAnnots = annots;
  if (isDebugging) {
    console.log("All annotations:");
    console.log(allAnnots);
  }
  // E.g. [{ "id":"1","type":8,"color":"FFFF00","opacity":1,"ranges":[55,72] }]
  const annotations = allAnnots.map(parseAnnot).filter(validateAnnot);

  // Add each annotation to paragraphs
  for (const annot of annotations) {
    for (let i = 0; i < offsets.length - 1; i++) {
      if (annot.endOffset <= offsets[i]) {
        break;
      }
      if (annot.startOffset >= offsets[i + 1]) {
        continue;
      }
      const startOffset = Math.max(offsets[i], annot.startOffset);
      const endOffset = Math.min(offsets[i + 1], annot.endOffset);

      addAnnotToParagraph(
        paragraphs[i],
        {
          ...annot,
          startOffset: startOffset - offsets[i],
          endOffset: endOffset - offsets[i]
        }
      );
    }
  }
  
  enableAddAnnotSupport();
  addSelectedStyle();
}

const addAnnotToParagraph = (p, annot, _startNode, _endNode) => {
  const startNode = _startNode || findNode(p, annot.startOffset, true, 0);
  const endNode = _endNode || findNode(p, annot.endOffset, false, 0);

  const handleSameNodeCase = (startNode, endNode) => {
    if (startNode.offset === endNode.offset) return;

    const textNode = startNode.textNode.splitText(startNode.offset);
    const annotNode = insertAnnotBeforeNode(annot, textNode);
    textNode.splitText(endNode.offset - startNode.offset);
    moveNodeIntoAnnot(textNode, annotNode, annot.type);
  };

  if (startNode.textNode === endNode.textNode) {
    // Case 1: Start and End are in the same text node.

    handleSameNodeCase(startNode, endNode);
  } else if (startNode.textNode.parentNode === endNode.textNode.parentNode) {
    // Case 2: Start and End have the same parent node.

    const textNode = startNode.textNode.splitText(startNode.offset);
    const annotNode = insertAnnotBeforeNode(annot, textNode);
    const endTextNode = endNode.textNode.splitText(endNode.offset);;
    let nextSibling = textNode;
    while (nextSibling && nextSibling != endTextNode) {
      const tmp = nextSibling.nextSibling; // Need to get nextSibling first as it'll be null after being moved into annot node
      moveNodeIntoAnnot(nextSibling, annotNode, annot.type);
      nextSibling = tmp;
    }
  } else {
    // Other cases

    const processNode = (currentNode, isHandlingEnd) => {
      if (!currentNode) return false;

      let isEndNodeHandled = false;

      if (currentNode === endNode.textNode) {
        handleSameNodeCase({ textNode: endNode.textNode, offset: 0 }, endNode);
        return true;
      } else {
        let annotNode;
        if (isHandlingEnd) {
          annotNode = insertAnnotBeforeNode(annot, currentNode.childNodes[0]);
        } else {
          annotNode = insertAnnotBeforeNode(annot, currentNode);
        }
        let nextSibling = annotNode.nextSibling;
        while (nextSibling) {
          if (nextSibling.textContent === "") {
            nextSibling = nextSibling.nextSibling;
            continue;
          }
          if (containsNode(nextSibling, endNode.textNode)) {
            processNode(nextSibling, true);
            isEndNodeHandled = true;
            break;
          } else {
            const tmp = nextSibling.nextSibling;
            moveNodeIntoAnnot(nextSibling, annotNode, annot.type);
            nextSibling = tmp;
          }
        }
        if (annotNode.textContent === "") {
          annotNode.remove();
        }
        return isEndNodeHandled;
      }
    };

    let nextNode = startNode.textNode.splitText(startNode.offset);
    while (nextNode.previousSibling !== p) {
      let parentNode = nextNode.parentNode;
      while (!parentNode.nextSibling && parentNode !== p) {
        parentNode = parentNode.parentNode;
      }
      const tmp = parentNode.nextSibling;
      if (!processNode(nextNode, false)) {
        nextNode = tmp;
      } else {
        break;
      }
    }
  }
}

const addAnnotFromRange = (range, annotWithoutID, pagePosition) => {
  if (!validateAnnot(annotWithoutID)) {
    console.error("Invalid annotation.");
    return;
  }

  const startNode = range.startContainer;
  const startOffset = range.startOffset;
  const endNode = range.endContainer;
  const endOffset = range.endOffset;

  const absStartOffset = getNodeOffset(startNode, startOffset);
  const absEndOffset = getNodeOffset(endNode, endOffset);

  const newAnnot = {
    ...annotWithoutID,
    color: annotWithoutID.color.substring(1),
    ranges: [absStartOffset, absEndOffset - 1]
  };
  if (isDebugging) {
    console.log("New annotation to add:");
    console.log(newAnnot);
  }
  const result = pdfNetReflow.setAnnot(JSON.stringify(newAnnot), pagePosition);

  if (result && result !== "") {
    const annot = {
      ...annotWithoutID,
      id: result
    };

    const startParagraph = findParagraphParent(startNode);
    const endParagraph = findParagraphParent(endNode);

    if (startParagraph === endParagraph) {
      addAnnotToParagraph(
        startParagraph,
        annot,
        {
          textNode: startNode,
          offset: startOffset
        },
        {
          textNode: endNode,
          offset: endOffset
        }
      );
    } else {
      // Add to start paragraph
      const lastNodeInStartP = findLastTextNode(endParagraph);
      addAnnotToParagraph(
        startParagraph,
        annot,
        {
          textNode: startNode,
          offset: startOffset
        },
        {
          textNode: lastNodeInStartP,
          offset: lastNodeInStartP.textContent.length
        }
      );

      // Add to middle paragraphs
      let currentP = startParagraph.nextSibling;
      while (currentP !== endParagraph) {
        if (isParagraphNode(currentP) && currentP.textContent.length > 0) {
          const firstChild = currentP.firstChild;
          const annotNode = insertAnnotBeforeNode(annot, firstChild);
          let nextSibling = annotNode.nextSibling;
          while (nextSibling) {
            const tmp = nextSibling.nextSibling;
            moveNodeIntoAnnot(nextSibling, annotNode, annot.type);
            nextSibling = tmp;
          }
        }
        currentP = currentP.nextSibling;
      }

      // Add to end paragraph
      const firstNodeInEndP = findFirstTextNode(endParagraph);
      addAnnotToParagraph(
        currentP,
        annot,
        {
          textNode: firstNodeInEndP,
          offset: 0
        },
        {
          textNode: endNode,
          offset: endOffset
        }
      );
    }
  } else {
    console.error(`Calling 'setAnnot()' to create annotation failed.`);
  }
}

/**
 * Insert newly created annotation node before the given node.
 */
const insertAnnotBeforeNode = (annot, node) => {
  const annotNode = createAnnotNode(annot, window.getComputedStyle(node.parentNode).color);
  const annotId = annot.id;
  if (this.annotNodeMap.has(annotId)) {
    this.annotNodeMap.get(annotId).push(annotNode);
  } else {
    this.annotNodeMap.set(annotId, [annotNode]);
  }

  // Add tooltip support
  annotNode.addEventListener('click', (e) => {
    e.stopPropagation(); //Needed for overlap cases.

    // Update current selected annotation
    if (this.selectedAnnot) {
      if (this.selectedAnnot.id === annotId) {
        cleanUpSelectedAnnot();
        return;
      } else {
        cleanUpSelectedAnnot();
      }
    }
    this.selectedAnnot = {
      ...annot,
      target: annotNode,
      origAnnot: annot
    };

    // Show selection border
    const annotNodes = this.annotNodeMap.get(annotId) || [];
    annotNodes.forEach((aNode, i) => {
      aNode.classList.add(selectedAnnotClassname);
      if (i === 0) {
        aNode.classList.add(selectedAnnotStartClassname);
      }
      if (i === annotNodes.length - 1) {
        aNode.classList.add(selectedAnnotEndClassname);
      }
    });

    // callback to show edit menu
    pdfNetReflow.annotClicked(annotId, JSON.stringify(annot));

    // Create and show tooltip
    // if (!this.tooltipContent) {
    //   this.tooltipContent = this.tooltip.firstChild;
    // }
    // this.tippy = tippy(annotNode, {
    //   content: this.tooltipContent,
    //   interactive: true,
    //   trigger: 'manual',
    //   theme: 'light-border',
    //   arrow: false,
    //   appendTo: getPageBody(window),
    //   onClickOutside: () => {
    //     if (this.selectedAnnot) {
    //       this.cleanUpSelectedAnnot();
    //     }
    //   }
    // });
    // this.tippy.show();
  });

  node.parentNode.insertBefore(annotNode, node);
  return annotNode;
}

const cleanUpSelectedAnnot = (removeBorder = true) => {
  if (!this.selectedAnnot) return;
  if (removeBorder) {
    const annotNodes = this.annotNodeMap.get(this.selectedAnnot.id) || [];
    annotNodes.forEach(aNode => {
      const classList = aNode.classList;
      classList.remove(selectedAnnotClassname);
      classList.remove(selectedAnnotStartClassname);
      classList.remove(selectedAnnotEndClassname);
    });
  }
  this.selectedAnnot = undefined;
}

ReflowJS.removeSelectedAnnot = (pagePosition) => {
  if (!this.selectedAnnot) return;
  const annotId = this.selectedAnnot.id;
  const result = pdfNetReflow.setAnnot(JSON.stringify({
    id: annotId
  }), pagePosition);
  if (result === annotId) {
    cleanUpSelectedAnnot(false);
    const annotNodes = this.annotNodeMap.get(annotId) || [];
    annotNodes.forEach(removeAnnotNode);
    this.annotNodeMap.set(annotId, []);
  } else {
    console.error(`Calling 'setAnnot()' to remove annotation failed.`);
  }
}

ReflowJS.setSelectedAnnotStyle = ({ color, opacity }, pagePosition) => {
  if (!this.selectedAnnot) return;
  const annotId = this.selectedAnnot.id;
  const result = pdfNetReflow.setAnnot(JSON.stringify({
    id: annotId,
    color: color.substring(1),
    opacity
  }), pagePosition);
  if (result === annotId) {
    this.selectedAnnot.origAnnot.color = color;
    this.selectedAnnot.origAnnot.opacity = opacity;
    const annotNodes = this.annotNodeMap.get(annotId) || [];
    annotNodes.forEach(annotNode => setAnnotStyle(annotNode, { color, opacity }));
  } else {
    console.error(`Calling 'setAnnot()' to change annotation style failed.`);
  }
}

const setAddAnnotConfig = (config) => {
  if (isDebugging) {
    console.log("setAddAnnotConfig(), config:");
    console.log(config);
  }
  window.getSelection().removeAllRanges();
  this.addAnnotConfig = config;
  setTextSelectionStyle();
}

const isValidAddAnnotConfig = () => {
  return this.addAnnotConfig && this.addAnnotConfig.type;
}

const enableAddAnnotSupport = () => {
  // Add style for selection
  const pageHead = getPageHead(window);
  this.selectionStyle = document.createElement("style");
  this.selectionStyle.type = "text/css";
  pageHead.appendChild(this.selectionStyle);

  // Set style for text selection
  setTextSelectionStyle();

  // Handle mouse event for adding new annotation from selection
  // window.addEventListener('mouseup', async () => {
  //   if (isValidAddAnnotConfig()) {
  //     const selection = window.getSelection();
  //     if (!selection || selection.rangeCount < 1 || !selection.toString()) return;
  //     const range = selection.getRangeAt(0);
  //     if (!range) return;

  //     addAnnotFromRange(
  //       range,
  //       this.addAnnotConfig
  //     );

  //     window.getSelection().removeAllRanges();
  //   }
  // });

  // clear selection when click outside of a node
  window.addEventListener('click', (e) => {
    cleanUpSelectedAnnot();
    pdfNetReflow.cleanSelectedAnnot();
  });
}

const setTextSelectionStyle = () => {
  let styleStr = "";
  if (isValidAddAnnotConfig()) {
    // Only a few CSS properties can be applied to the ::selection selector: color, background, cursor, and outline.
    let styles = "";
    const rgbaColor = hexToRgbAColor(this.addAnnotConfig.color, this.addAnnotConfig.opacity);
    if (this.addAnnotConfig.type === AnnotationType.Highlight) {
      styles = `background-color: ${rgbaColor};`;
    }/* else if (this.addAnnotConfig.type === AnnotationType.Underline) {
      styles = `text-decoration: underline ${rgbaColor};`;
    } else if (this.addAnnotConfig.type === AnnotationType.Strikeout) {
      styles = `text-decoration: line-through ${rgbaColor};`;
    } else if (this.addAnnotConfig.type === AnnotationType.Squiggly) {
      styles = `text-decoration: underline ${rgbaColor} wavy;`;
    }*/
    styleStr = `::-moz-selection { ${styles} } ::selection { ${styles} }`;
  }
  this.selectionStyle.innerHTML = styleStr;
}

const addSelectedStyle = () => {
  const pageHead = getPageHead(window);
  if (!pageHead) return;
  const style = document.createElement("style");
  style.type = "text/css";
  const borderStyle = '1px solid #3183C8';
  style.innerHTML = `
    .${annotClassname}{cursor:pointer}
    .${selectedAnnotClassname}{border-top:${borderStyle};border-bottom:${borderStyle};z-index:10;position:relative;}
    .${selectedAnnotStartClassname}{border-left:${borderStyle};margin-left:-1px;}
    .${selectedAnnotEndClassname}{border-right:${borderStyle};margin-right:-1px;}
  `;
  pageHead.appendChild(style);
}

ReflowJS.addAnnot = (annotType, pagePosition, { color, opacity, uniqueID }) => {
  const selection = window.getSelection();
  if (!selection || selection.rangeCount < 1 || !selection.toString()) return;
  const range = selection.getRangeAt(0);
  if (!range) return;
  setAddAnnotConfig({type: annotType, color: color, opacity: opacity, uniqueID: uniqueID});

  addAnnotFromRange(range, this.addAnnotConfig, pagePosition);
}

ReflowJS.deselectAnnot = () => {
  cleanUpSelectedAnnot();
  pdfNetReflow.cleanSelectedAnnot();
}

/**
* Find start/end anchor node in the container node by the given offset.
* @param {*} node Container node to find the target node.
* @param {*} offset Character offset of the target node.
* @param {*} isStart Whether to find the start anchor noe.
* @param {*} charCount The number of characters in the nodes have been searched.
*/
function findNode(node, offset, isStart, charCount) {
for (const child of node.childNodes) {
  if (child.nodeType === Node.TEXT_NODE) {
    const textLength = child.textContent.length;
    charCount += textLength;
    if ((isStart && charCount > offset) || (!isStart && charCount >= offset)) {
      return {
        textNode: child,
        offset: offset - (charCount - textLength)
      };
    }
  } else {
    const result = findNode(child, offset, isStart, charCount);
    if (result) {
      return result;
    } else {
      charCount += child.textContent.length;
    }
  }
}
return undefined;
}

/**
* Create new HTML element for the given annotation.
* @param {*} annot The annotation object.
* @param {*} textColor Text color.
*/
function createAnnotNode(annot, textColor) {
const span = document.createElement("span");
if (annot.type === AnnotationType.Highlight) {
  setAnnotStyle(span, annot);
  span.setAttribute(ANNOT_ID, annot.id);
  span.className = annotClassname;
  return span;
} else {
  span.style.color = textColor;
  let annotNode;
  if (annot.type === AnnotationType.Underline) {
    annotNode = document.createElement("u");
  } else if (annot.type === AnnotationType.Strikeout) {
    annotNode = document.createElement("s");
  } else if (annot.type === AnnotationType.Squiggly) {
    annotNode = document.createElement("u");
    annotNode.style.textDecorationStyle = "wavy";
  }
  setAnnotStyle(annotNode, annot);
  annotNode.setAttribute(ANNOT_ID, annot.id);
  annotNode.appendChild(span);
  annotNode.className = annotClassname;
  return annotNode;
}
}

/**
* Move the 'node' into the given 'annotNode' as last child.
* @param {*} node
* @param {*} annotNode
* @param {*} annotType
*/
function moveNodeIntoAnnot(node, annotNode, annotType) {
if (annotType === AnnotationType.Highlight) {
  annotNode.appendChild(node);
} else {
  annotNode.firstChild.appendChild(node);
}
}

/**
* Return if the 'currentNode' contains the given 'node'.
* @param {*} currentNode
* @param {*} node
*/
function containsNode(currentNode, node) {
if (currentNode.childNodes.length === 0) {
  return currentNode === node;
} else {
  for (const n of currentNode.childNodes) {
    if (containsNode(n, node)) {
      return true;
    }
  }
  return false;
}
}

/**
* Return the first <p> parent of the given node.
* @param {*} node
*/
function findParagraphParent(node) {
if (!node || isParagraphNode(node)) {
  return node;
} else {
  return findParagraphParent(node.parentNode);
}
}

/**
* Find the last child that is Text Node if there's any.
* @param {*} currentNode
*/
function findLastTextNode(currentNode) {
const childNodes = currentNode.childNodes || [];
for (let i = childNodes.length - 1; i >= 0; i--) {
  const node = childNodes[i];
  if (node.nodeType === Node.TEXT_NODE && node.textContent.length > 0) {
    return node;
  } else {
    const result = findLastTextNode(node);
    if (result) {
      return result;
    }
  }
}
return undefined;
}

/**
* Find the first child that is Text Node if there's any.
* @param {*} currentNode
*/
function findFirstTextNode(currentNode) {
const childNodes = currentNode.childNodes || [];
for (let i = 0; i < childNodes.length; i++) {
  const node = childNodes[i];
  if (node.nodeType === Node.TEXT_NODE && node.textContent.length > 0) {
    return node;
  } else {
    const result = findFirstTextNode(node);
    if (result) {
      return result;
    }
  }
}
return undefined;
}

/**
* Remove the given annotation node from DOM but keeping its children.
* @param {*} annotNode
*/
function removeAnnotNode(annotNode) {
const nextSibling = annotNode.nextSibling;
let childNodes;
if (isAnnotNodeHighlight(annotNode)) {
  childNodes = annotNode.childNodes;
} else {
  childNodes = annotNode.firstChild.childNodes;
}
childNodes = Array.from(childNodes);
for (const child of childNodes) {
  annotNode.parentNode.insertBefore(child, nextSibling);
}
annotNode.remove();
}

/**
* Convert Core annotation object to our format.
* @param {*} annot The annotation object from Core API.
*/
function parseAnnot(annot) {
return {
  ...annot,
  color: `#${annot.color}`,
  startOffset: annot.ranges[0],
  endOffset: annot.ranges[annot.ranges.length - 1] + 1,
};
}

/**
* Check if the given annotation object is valid.
*/
function validateAnnot(annot) {
return annotationTypes.includes(annot.type);
}

function isParagraphNode(node) {
return node.tagName === "P";
}

/**
* Return the absolute offset of the given node.
* @param {*} node Target node.
* @param {*} offset The relative offset within the target node.
*/
function getNodeOffset(node, offset) {
if (!node) {
  return offset;
}

const parentNode = node.parentNode;
const previousSibling = node.previousSibling;
if (parentNode.tagName === "BODY") {
  if (previousSibling) {
    if (isParagraphNode(previousSibling)) {
      return getNodeOffset(previousSibling, offset + previousSibling.textContent.length);
    } else {
      return getNodeOffset(previousSibling, offset);
    }
  }
} else {
  if (previousSibling) {
    return getNodeOffset(previousSibling, offset + previousSibling.textContent.length);
  } else {
    return getNodeOffset(parentNode, offset);
  }
}
return offset;
}

/**
 * Set new color for the annotation node.
 */
function setAnnotStyle(annotNode, { color, opacity }) {
  const rgbaColor = hexToRgbAColor(color, opacity);
  if (isAnnotNodeHighlight(annotNode)) {
    annotNode.style.backgroundColor = rgbaColor;
  } else {
    annotNode.style.color = rgbaColor;
  }
}

function isAnnotNodeHighlight(annotNode) {
  return annotNode.tagName === "SPAN";
}

function hexToRgbAColor(hex, alpha = 1) {
  const r = parseInt(hex.slice(1, 3), 16), g = parseInt(hex.slice(3, 5), 16), b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}


// helper.js

// pageNum is 1-indexed
function getPageElementId(pageNum) {
  return `read-mode-page-${pageNum}`;
}

// pageNum is 0-indexed
function getPageElement(pageNum) {
  return document.getElementById(getPageElementId(pageNum + 1));
}

// Is Safari browser
function isSafari() {
  return navigator.userAgent.indexOf("Safari") > -1;
}

function getPageDocHtml(pageWindow) {
  if (pageWindow) {
    if (pageWindow.document) {
      return pageWindow.document.getElementsByTagName('html')[0];
    }
  }
  return null;  
}

function getPageBody(pageWindow) {
  if (getPageDocHtml(pageWindow)) {
    return getPageDocHtml(pageWindow).getElementsByTagName('body')[0];
  }
  return null;
}

function getPageHead(pageWindow) {
  if (getPageDocHtml(pageWindow)) {
    return getPageDocHtml(pageWindow).getElementsByTagName('head')[0];
  }
  return null;
}