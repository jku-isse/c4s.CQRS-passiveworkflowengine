Blockly.Blocks['artifact'] = {
  init: function() {
    this.appendDummyInput()
        .appendField("Artifact Type")
        .appendField(new Blockly.FieldDropdown([["Jira","jira_artifact"], ["Jama","jama_item"], ["Azure","azure_workitem"], ["GitHub Issue","git_issue"], ["Process Definition","ProcessDefinition"],["ProcessConfiguration","process_config_base"], ["ACME-RA Issue", "acme_issue"], ["ACME-RA Engineer", "acme_engineer"]]), "Type");
    this.setOutput(true, null);
    this.setColour(45);
 this.setTooltip("Defines what type of artifact is involved");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['step'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldTextInput("StepId"), "StepId");
    this.appendStatementInput("Input")
        .setCheck("artuse")
        .appendField("Input");
    this.appendStatementInput("Transitions")
        .setCheck("transition")
        .appendField("Transitions");
    this.appendStatementInput("Datamappings")
        .setCheck("datamapping")
        .appendField("Datamappings");    
    this.appendStatementInput("Output")
        .setCheck("artuse")
        .appendField("Output");
    this.appendStatementInput("QA")
        .setCheck("qacheck")
        .appendField("QA");    
    this.setInputsInline(false);
    this.setPreviousStatement(true, "decisionnode");
    this.setNextStatement(true, "decisionnode");
    this.setColour(230);
 this.setTooltip("Steps");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['config'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldTextInput("Config"), "ConfigId");
    this.appendStatementInput("Properties")
        .setCheck("configproperty")
        .appendField("Properties");        
    this.setInputsInline(false);
    this.setPreviousStatement(true, "function");
    this.setNextStatement(true, null);
    this.setColour(130);
 this.setTooltip("Process Configuration Properties");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['configproperty'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldTextInput("Property Name"), "propertyName")
        .appendField(new Blockly.FieldDropdown([["Boolean","BOOLEAN"], ["String","STRING"], ["Integer","INTEGER"], ["Double","REAL"], ["Date","DATE"]]), "propertyType")
        .appendField(new Blockly.FieldDropdown([["Single","SINGLE"], ["List (not supported yet)","LIST"], ["Set (not supported yet)","SET"], ["Map (not supported yet)","MAP"]]), "cardinality")   
        .appendField(' IsRepairable:')
        .appendField(new Blockly.FieldCheckbox(true), 'isRepairable');

    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(130);
    this.setInputsInline(false);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};


Blockly.Blocks['artuse'] = {
  init: function() {
    this.appendValueInput("NAME")
        .setCheck(null)
        .appendField(new Blockly.FieldLabelSerializable("Param"), "roletext");
        //.appendField(new Blockly.FieldTextInput("defaultParam"), "NAME");
    this.setInputsInline(false);
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(30);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};



Blockly.Blocks['transition'] = {
  init: function() {
    this.appendValueInput("condition")        
        .appendField(new Blockly.FieldDropdown([["Enabled","PRECONDITION"], ["Activated","ACTIVATION"], ["Completed","POSTCONDITION"], ["Canceled","CANCELATION"]]), "State")
        .appendField("Upon/If")
        .setCheck(null);
    this.appendDummyInput()
        .appendField(' IsOverridable:')
        .appendField(new Blockly.FieldCheckbox(false), 'isOverridable');
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['datamapping'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldTextInput("MappingId"), "mappingId")
        .appendField(new Blockly.FieldMultilineInput('provide mapping as ARL here'),
            'mappingSpec');

    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(65);
    this.setInputsInline(false);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['qacheck'] = {
  init: function() {
	this.appendDummyInput()
		.appendField("QA Check");
    this.appendDummyInput()
        .appendField(new Blockly.FieldTextInput("Const1"), "qacheckId");
    this.appendDummyInput()
        .appendField(new Blockly.FieldTextInput("Description"), "description");
 	this.appendValueInput("constraint")
        .setCheck(null);
    this.appendDummyInput()
    	.appendField(' IsOverridable:')
        .appendField(new Blockly.FieldCheckbox(false), 'isOverridable');    
        
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(65);
 this.setTooltip("Add constraint identifier and human readable description");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['constraint'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldMultilineInput('provide constraint as ARL rule here'),'arlRule');
    this.setOutput(true, null);
    this.setColour(45);
 this.setTooltip("ARL rule content");
 this.setHelpUrl("");
  }
};

/*

Blockly.Blocks['noopstep'] = {
  init: function() {
    this.appendDummyInput()
        .appendField("NoOpStep");
    this.setInputsInline(false);
    this.setPreviousStatement(true, "decisionnode");
    this.setNextStatement(true, "decisionnode");
    this.setColour(230);
 this.setTooltip("Steps");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['comment'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldTextInput("% "), "comment");
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(180);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['fetchartifact'] = {
  init: function() {
    this.appendValueInput("Type")
        .setCheck("ArtifactType")
        .appendField(new Blockly.FieldLabelSerializable("Type"), "NAME");
    this.appendValueInput("IdAtOrigin")
        .setCheck("String")
        .appendField(new Blockly.FieldTextInput("IdType"), "IdType");
    this.setInputsInline(false);
    this.setOutput(true, "artifact");
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['fieldaccessor'] = {
  init: function() {
    this.appendValueInput("var")
        .setCheck("artifacttype");
    this.appendDummyInput()
        .appendField("value of")
        .appendField(new Blockly.FieldTextInput("fieldName"), "NAME");
    this.setOutput(true, null);
    this.setColour(45);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['checkpoint'] = {
  init: function() {
    this.appendValueInput("NAME")
        .setCheck("Boolean")
        .appendField("wait until:");
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['streambooleanop'] = {
  init: function() {
    this.appendValueInput("NAME")
        .setCheck("Boolean")
        .appendField(new Blockly.FieldDropdown([["none match","NONEMATCH"], ["exactly one match","ONEMATCH"], ["at least one match","MOREMATCH"], ["all match","ALLMATCH"]]), "operator");
    this.setInputsInline(false);
    this.setPreviousStatement(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['stream'] = {
  init: function() {
    this.appendValueInput("NAME")
        .setCheck(null)
        .appendField("with each")
        .appendField(new Blockly.FieldVariable("el"), "NAME")
        .appendField("from");
    this.appendStatementInput("do")
        .setCheck(["streambooleanop", "streamnumberop", "streamtransformop", "streamcollectop"])
        .appendField("do");
    this.setOutput(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['streamnumberop'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown([["count","COUNT"], ["min","MIN"], ["max","MAX"]]), "operator");
    this.setInputsInline(false);
    this.setPreviousStatement(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['streamtransformop'] = {
  init: function() {
    this.appendValueInput("NAME")
        .setCheck(null)
        .appendField(new Blockly.FieldDropdown([["map","MAP"], ["flat map","FLATMAP"]]), "operator");
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['streamcollectop'] = {
  init: function() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown([["to List","LIST"], ["to Set","SET"]]), "operator");
    this.setPreviousStatement(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
};

Blockly.Blocks['streamfilterop'] = {
  init: function() {
    this.appendValueInput("NAME")
        .setCheck("Boolean")
        .appendField("filter");
    this.setInputsInline(false);
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    this.setColour(230);
 this.setTooltip("");
 this.setHelpUrl("");
  }
}; */

