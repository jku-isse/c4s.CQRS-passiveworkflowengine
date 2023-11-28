
'use strict';
/**
goog.provide('Blockly.Extension.Flow');

goog.require('Blockly');
goog.require('Blockly.Blocks');
goog.require('Blockly.FieldDropdown');
goog.require('Blockly.FieldLabel');
goog.require('Blockly.Mutator');
**/


/*Blockly.defineBlocksWithJsonArray([  // BEGIN JSON EXTRACT

  // Block for doStep.
  {
    "type": "constraint",
    "args0": [
    {
      "type": "input_dummy"
    },    
    {
      "type": "field_multilinetext",
      "name": "arlRule",
      "text": "provide constraint as ARL rule here",
      "spellcheck": false
    },
    ],
	"output":"String",  
    "colour": 45,
    "tooltip": "ARL rule content",
    "helpUrl": ""
  }

]);  // END JSON EXTRACT (Do not delete this comment.)*/

Blockly.defineBlocksWithJsonArray([  // BEGIN JSON EXTRACT

  // Block for doStep.
  {
    "type": "parallelexecution",
    "message0": "work in parallel; start Steps: %1 do %2 wait for  %3",
    "args0": [
 /*   {
      "type": "field_dropdown",
      "name": "OutFlowType",
      "options": [
        [
          "CONT",
          "CONT"
        ],
        [
          "all at the same time (Synchronous)",
          "SYNC"
        ],
        [
          "once their input requirement is satisfied (Asynchronous)",
          "ASYNC"
        ]
      ]
    },*/
    {
      "type": "input_dummy"
    },    
    {
      "type": "input_statement",
      "name": "DO0"
    },
    {
      "type": "field_dropdown",
      "name": "InFlowType",
      "options": [
        [
          "all to complete (AND)",
          "AND"
        ],
        [
          "first to complete and then others (OR)",
          "OR"
        ],
        [
          "only exactly one to complete (XOR)",
          "XOR"
        ]
      ]
    }
    ],
    "previousStatement": null,
    "nextStatement": null,    
    "colour": 75,
    "tooltip": "Defines control flow",
    "helpUrl": "",
    "mutator": "doBlock_mutator",
  }

]);  // END JSON EXTRACT (Do not delete this comment.)

Blockly.defineBlocksWithJsonArray([ // Mutator blocks. Do not extract.
  // Block representing the if statement in the controls_if mutator.
  {
    "type": "doBlock_First",
    "message0": "Do: ",
    "colour": 75,
    "nextStatement": null,
    "enableContextMenu": false,    
  },
  // Block representing the else-if statement in the controls_if mutator.
  {
    "type": "doBlock_Additional",
    "message0": "Do: ",
    "colour": 75,
    "previousStatement": null,
    "nextStatement": null,
    "enableContextMenu": false,    
  }
]);




/**
 * Mutator methods added to controls_if blocks.
 * @mixin
 * @augments Blockly.Block
 * @package
 * @readonly
 */
//Blockly.Extension.Flow.
Blockly.DOBLOCK_MUTATOR_MIXIN = {
  doCount_: 0,

  /**
   * Don't automatically add STATEMENT_PREFIX and STATEMENT_SUFFIX to generated
   * code.  These will be handled manually in this block's generators.
   */
  suppressPrefixSuffix: true,

  /**
   * Create XML to represent the number of else-if and else inputs.
   * @return {Element} XML storage element.
   * @this {Blockly.Block}
   */
  mutationToDom: function() {
    if (!this.doCount_ ) {
      return null;
    }
    var container = Blockly.utils.xml.createElement('mutation');
    if (this.doCount_) {
      container.setAttribute('doPart', this.doCount_);
    }

    return container;
  },
  /**
   * Parse XML to restore the else-if and else inputs.
   * @param {!Element} xmlElement XML storage element.
   * @this {Blockly.Block}
   */
  domToMutation: function(xmlElement) {
    this.doCount_ = parseInt(xmlElement.getAttribute('doPart'), 10) || 0;
    this.rebuildShape_();
  },
  /**
   * Populate the mutator's dialog with this block's components.
   * @param {!Blockly.Workspace} workspace Mutator's workspace.
   * @return {!Blockly.Block} Root block in mutator.
   * @this {Blockly.Block}
   */
  decompose: function(workspace) {
    var containerBlock = workspace.newBlock('doBlock_First');
    containerBlock.initSvg();
    var connection = containerBlock.nextConnection;
    for (var i = 1; i <= this.doCount_; i++) {
      var doBlock = workspace.newBlock('doBlock_Additional');
      doBlock.initSvg();
      connection.connect(doBlock.previousConnection);
      connection = doBlock.nextConnection;
    }
    return containerBlock;
  },
  /**
   * Reconfigure this block based on the mutator dialog's components.
   * @param {!Blockly.Block} containerBlock Root block in mutator.
   * @this {Blockly.Block}
   */
  compose: function(containerBlock) {
    var clauseBlock = containerBlock.nextConnection.targetBlock();
    // Count number of inputs.
    this.doCount_ = 0;
   
    var statementConnections = [null];
    while (clauseBlock) {
      switch (clauseBlock.type) {
        case 'doBlock_Additional':
          this.doCount_++;
          statementConnections.push(clauseBlock.statementConnection_);
          break;        
        default:
          throw TypeError('Unknown block type: ' + clauseBlock.type);
      }
      clauseBlock = clauseBlock.nextConnection &&
          clauseBlock.nextConnection.targetBlock();
    }
    this.updateShape_();
    // Reconnect any child blocks.
    this.reconnectChildBlocks_(statementConnections      );
  },
  /**
   * Store pointers to any connected child blocks.
   * @param {!Blockly.Block} containerBlock Root block in mutator.
   * @this {Blockly.Block}
   */
  saveConnections: function(containerBlock) {
    var clauseBlock = containerBlock.nextConnection.targetBlock();
    var i = 1;
    while (clauseBlock) {
      switch (clauseBlock.type) {
        case 'doBlock_Additional':          
          var inputDo = this.getInput('DO' + i);
          clauseBlock.statementConnection_ =
              inputDo && inputDo.connection.targetConnection;
          i++;
          break;
        default:
          throw TypeError('Unknown block type: ' + clauseBlock.type);
      }
      clauseBlock = clauseBlock.nextConnection &&
          clauseBlock.nextConnection.targetBlock();
    }
  },
  /**
   * Reconstructs the block with all child blocks attached.
   * @this {Blockly.Block}
   */
  rebuildShape_: function() {
     var statementConnections = [null];
    
    var i = 1;
    while (this.getInput('DO' + i)) {      
      var inputDo = this.getInput('DO' + i);
      statementConnections.push(inputDo.connection.targetConnection);
      i++;
    }
    this.updateShape_();
    this.reconnectChildBlocks_(statementConnections,
        );
  },
  /**
   * Modify this block to have the correct number of inputs.
   * @this {Blockly.Block}
   * @private
   */
  updateShape_: function() {
    // Delete everything.    
    var i = 1;
    while (this.getInput('DO' + i)) {      
      this.removeInput('DO' + i);
      i++;
    }
    // Rebuild block.
    for (i = 1; i <= this.doCount_; i++) {      
      this.appendStatementInput('DO' + i)
          .appendField(Blockly.Msg['CONTROLS_IF_MSG_THEN']);
    }
    
  },
  /**
   * Reconnects child blocks.
   * @param {!Array.<?Blockly.RenderedConnection>} statementConnections List of
   * statement connections for 'do' input.
   * @this {Blockly.Block}
   */
  reconnectChildBlocks_: function(statementConnections) {
    for (var i = 1; i <= this.doCount_; i++) {      
      Blockly.Mutator.reconnect(statementConnections[i], this, 'DO' + i);
    }    
  }
};

Blockly.Extensions.registerMutator('doBlock_mutator',
    Blockly.DOBLOCK_MUTATOR_MIXIN, null,
    ['doBlock_Additional']);
/** Extension.Flow.
 * "controls_if" extension function. Adds mutator, shape updating methods, and
 * dynamic tooltip to "controls_if" blocks.
 * @this {Blockly.Block}
 * @package
 */
/** 
Blockly.Extension.Flow.CONTROLS_IF_TOOLTIP_EXTENSION = function() {

  this.setTooltip(function() {
    if (!this.doCount_ ) {
      return Blockly.Msg['CONTROLS_IF_TOOLTIP_1'];
    } 
    return '';
  }.bind(this));
};

Blockly.Extensions.register('controls_if_tooltip',
    Blockly.Extension.Flow.CONTROLS_IF_TOOLTIP_EXTENSION);
**/


