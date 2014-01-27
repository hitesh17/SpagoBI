/** SpagoBI, the Open Source Business Intelligence suite

 * Copyright (C) 2012 Engineering Ingegneria Informatica S.p.A. - SpagoBI Competency Center
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0, without the "Incompatible With Secondary Licenses" notice. 
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/. **/
 
  
 
  
 
  
 
/**
  * Object name 
  * 
  * [description]
  * 
  * 
  * Public Properties
  * 
  * [list]
  * 
  * 
  * Public Methods
  * 
  *  [list]
  * 
  * 
  * Public Events
  * 
  *  [list]
  * 
  * Authors
  * 
  * - Antonella Giachino (antonella.giachino@eng.it)
  */

Ext.ns("Sbi.cockpit.editor");

Sbi.cockpit.editor.WidgetEditorFieldPalette = function(config) {
	
	var defaultSettings = {
		title: LN('sbi.formbuilder.queryfieldspanel.title')
		, displayRefreshButton : true
	};
	
	if(Sbi.settings && Sbi.cockpit && Sbi.cockpit.editor && Sbi.cockpit.editor.WidgetEditorFieldPalette) {
		defaultSettings = Ext.apply(defaultSettings, Sbi.cockpit.editor.WidgetEditorFieldPalette);
	}
		
	var c = Ext.apply(defaultSettings, config || {});
		
	Ext.apply(this, c);
			
	this.services = this.services || new Array();	
	////////////////////  T E S T //////////////////////////////////
	var baseParams = {dataset: 'myqbe'}; //solo per TEST ...ovviamente dovr� essere dinamico!
	////////////////////T E S T //////////////////////////////////
	this.services["getQueryFields"] = Sbi.config.serviceRegistry.getRestServiceUrl({
		serviceName : 'datasets/metafields', 
		baseParams : baseParams || {}
	});	

	this.addEvents("validateInvalidFieldsAfterLoad");
		
		
	this.initGrid(c.gridConfig || {});

	
	c = Ext.apply(c, {
		title: this.title,
		border: true,
		//bodyStyle:'background:green',
		bodyStyle:'padding:3px',
      	layout: 'fit',   
      	items: [this.grid]
	,   tools: (!this.displayRefreshButton) ? [] : [{ 
		    id:'refresh',
		    qtip: LN('sbi.formbuilder.queryfieldspanel.tools.refresh'),
		    handler: function(){
      			this.refresh();
		    }
		    , scope: this
      	}]
	});

	// constructor
	Sbi.cockpit.editor.WidgetEditorFieldPalette.superclass.constructor.call(this, c);
};

Ext.extend(Sbi.cockpit.editor.WidgetEditorFieldPalette, Ext.Panel, {
    
    services: null
    , grid: null
    , store: null
    , displayRefreshButton: null  // if true, display the refresh button
    
    // private
    
    , initGrid: function(c) {
	
		this.store = new Ext.data.JsonStore({
			autoLoad : false
			, idProperty : 'alias'
			, root: 'results'
			, fields: ['id', 'alias', 'funct', 'iconCls', 'nature', 'values', 'precision', 'options']
			, url: this.services['getQueryFields']
		}); 
    	
		
		this.store.on('loadexception', function(store, options, response, e){
			Sbi.exception.ExceptionHandler.handleFailure(response, options);
		}, this);
		

		this.store.on('load', 
				function(){
					this.fireEvent("validateInvalidFieldsAfterLoad", this); 		
				}
				, this);
		
        this.template = new Ext.Template( // see Ext.Button.buttonTemplate and Button's onRender method
        		// margin auto in order to have button center alignment
                '<table id="{4}" cellspacing="0" class="x-btn {3} {6}"><tbody class="{1}">',
                '<tr><td class="x-btn-tl"><i>&#160;</i></td><td class="x-btn-tc"></td><td class="x-btn-tr"><i>&#160;</i></td></tr>',
                '<tr><td class="x-btn-ml"><i>&#160;</i></td><td class="x-btn-mc"><button type="{0}" class=" x-btn-text {5}"></button>{7}</td><td class="x-btn-mr"><i>&#160;</i></td></tr>',
                '<tr><td class="x-btn-bl"><i>&#160;</i></td><td class="x-btn-bc"></td><td class="x-btn-br"><i>&#160;</i></td></tr>',
                '</tbody></table>');
        this.template.compile();
		
		this.grid = new Ext.grid.GridPanel(Ext.apply(c || {}, {
	        store: this.store,
	        hideHeaders: true,
	        columns: [
	            {id:'alias' 
            	, header: LN('sbi.formbuilder.queryfieldspanel.fieldname')
            	, width: 160
            	, sortable: true
            	, dataIndex: 'alias'
            	, renderer : function(value, metaData, record, rowIndex, colIndex, store) {
		        	return this.template.apply(
		        			// by now cssborder is defined only for segment_attribute
		        			['button', 'x-btn-small x-btn-icon-small-left', '', 'x-btn-text-icon', Ext.id(), record.data.iconCls, record.data.iconCls+'_text', record.data.alias]		
		        	);
		    	}
	            , scope: this
            	}
	        ],
	        stripeRows: false,
	        autoExpandColumn: 'alias',
	        enableDragDrop: true
	        //ddGroup: c.ddGroup //'formbuilderDDGroup'
	    }));
    }
    
    
    // public methods 
    
	, refresh: function() {		
		this.store.load();
	}

    , getFields : function () {
    	var fields = [];
    	var count = this.store.getCount();
    	for (var i = 0; i < count; i++) {
    		fields.push(this.store.getAt(i).data);
    	}
    	return fields;
    }
    
});