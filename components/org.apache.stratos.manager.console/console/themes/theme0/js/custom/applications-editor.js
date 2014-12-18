// repaint
function Repaint(){
    $("#whiteboard").resize(function(){
        jsPlumb.repaintEverything();
    });
}
// drag
function DragEl(el){
    jsPlumb.draggable($(el) ,{
        containment:"#whiteboard"
    });
}


// JsPlumb Config
var color = "gray",
    exampleColor = "#00f",
    arrowCommon = { foldback:0.7, fillStyle:color, width:14 };

jsPlumb.importDefaults({
    Connector : [ "Bezier", { curviness:63 } ],
    /*Overlays: [
     [ "Arrow", { location:0.7 }, arrowCommon ],
     ]*/
});


var nodeDropOptions = {
    activeClass:"dragActive"
};

var bottomConnectorOptions = {
    endpoint:"Rectangle",
    paintStyle:{ width:25, height:21, fillStyle:'#666' },
    isSource:true,
    connectorStyle : { strokeStyle:"#666" },
    isTarget:false,
    maxConnections:5
};

var endpointOptions = {
    isTarget:true,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: nodeDropOptions,
    maxConnections:1
};

var groupOptions = {
    isTarget:true,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: nodeDropOptions,
    maxConnections:1
};

var generatedCartridgeEndpointOptions = {
    isTarget:false,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: '',
    maxConnections:1
};

var generatedGroupOptions = {
    isTarget:false,
    endpoint:"Dot",
    paintStyle:{
        fillStyle:"gray"
    },
    dropOptions: nodeDropOptions,
    maxConnections:1
};

jsPlumb.ready(function() {
    //create application level block
    jsPlumb.addEndpoint('applicationId', {
        anchor:"BottomCenter"
    }, bottomConnectorOptions);
});

var cartridgeCounter=0;
//add cartridge to editor
function addJsplumbCartridge(idname, cartridgeCounter) {

    var Div = $('<div>').attr({'id':cartridgeCounter+'-'+idname, 'data-type':'cartridge', 'data-ctype':idname } )
        .text(idname)
        .addClass('input-false')
        .appendTo('#whiteboard');
    $(Div).addClass('stepnode');
    jsPlumb.addEndpoint($(Div), {
        anchor: "TopCenter"
    }, endpointOptions);
    // jsPlumb.addEndpoint($(Div), sourceEndpoint);
    $(Div).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
    DragEl($(Div));
    Repaint();
}

//add group to editor
function addJsplumbGroup(groupJSON, cartridgeCounter){

    var divRoot = $('<div>').attr({'id':cartridgeCounter+'-'+groupJSON.name,'data-type':'group','data-ctype':groupJSON.name})
        .text(groupJSON.name)
        .addClass('input-false')
        .addClass('stepnode')
        .appendTo('#whiteboard');
    $(divRoot).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
    jsPlumb.addEndpoint($(divRoot), {
        anchor:"BottomCenter"
    }, bottomConnectorOptions);

    jsPlumb.addEndpoint($(divRoot), {
        anchor: "TopCenter"
    }, groupOptions);
    DragEl($(divRoot));

    for (var prop in groupJSON) {
        if(prop == 'cartridges'){
            genJsplumbCartridge(groupJSON[prop], divRoot, groupJSON.name)
        }
        if(prop == 'groups'){
            genJsplumbGroups(groupJSON[prop], divRoot, groupJSON.name)
        }
    }

    function genJsplumbCartridge(item, currentParent, parentName){
        for (var i = 0; i < item.length; i++) {
            var id = item[i];
            var divCartridge = $('<div>').attr({'id':cartridgeCounter+'-'+parentName+'-'+item[i],'data-type':'cartridge','data-ctype':item[i]} )
                .text(item[i])
                .addClass('input-false')
                .addClass('stepnode')
                .appendTo('#whiteboard');
            $(divCartridge).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
            jsPlumb.addEndpoint($(divCartridge), {
                anchor: "TopCenter"
            }, generatedCartridgeEndpointOptions);

            //add connection options
            jsPlumb.connect({
                source:$(currentParent),
                target:$(divCartridge),
                paintStyle:{strokeStyle:"blue", lineWidth:1 },
                Connector : [ "Bezier", { curviness:63 } ],
                anchors:["BottomCenter", "TopCenter"],
                endpoint:"Dot"
            });

            DragEl($(divCartridge));
        }
    }

    function genJsplumbGroups(item, currentParent, parentName) {
        for (var prop in item) {
            var divGroup = $('<div>').attr({'id':cartridgeCounter+'-'+parentName+'-'+item[prop]['name'],'data-type':'group','data-ctype':item[prop]['name'] })
                .text(item[prop]['name'])
                .addClass('stepnode')
                .addClass('input-false')
                .appendTo('#whiteboard');
            $(divGroup).append('<div class="notification"><i class="fa fa-exclamation-circle fa-2x"></i></div>');
            jsPlumb.addEndpoint($(divGroup), {
                anchor:"BottomCenter"
            }, bottomConnectorOptions);

            jsPlumb.addEndpoint($(divGroup), {
                anchor: "TopCenter"
            }, generatedGroupOptions);

            //add connection options
            jsPlumb.connect({
                source:$(currentParent),
                target:$(divGroup),
                paintStyle:{strokeStyle:"blue", lineWidth:1 },
                Connector : [ "Bezier", { curviness:63 } ],
                anchors:["BottomCenter", "TopCenter"],
                endpoint:"Dot"
            });

            DragEl($(divGroup));

            if(item[prop].hasOwnProperty('cartridges')) {
                genJsplumbCartridge(item[prop].cartridges, divGroup, parentName+'-'+item[prop]['name'] );
            }
            if(item[prop].hasOwnProperty('groups')) {
                genJsplumbGroups(item[prop].groups, divGroup, parentName+'-'+item[prop]['name'])
            }
        }
    }



}
//use to activate tab
function activateTab(tab){
    $('.nav-tabs a[href="#' + tab + '"]').tab('show');
};
//generate treefor Groups
function generateGroupTree(groupJSON){

    var rawout = [];
    //create initial node for tree
    var rootnode ={};
    rootnode.name = groupJSON.name;
    rootnode.parent = null;
    rootnode.type = 'groups';
    rawout.push(rootnode);

    for (var prop in groupJSON) {
        if(prop == 'cartridges'){
            getCartridges(groupJSON[prop],rawout, rootnode.name)
        }
        if(prop == 'groups'){
            getGroups(groupJSON[prop], rawout, rootnode.name)
        }
    }

    function getCartridges(item, collector, parent){
        for (var i = 0; i < item.length; i++) {
            var type = 'cartridges';
            var cur_name = item[i];
            collector.push({"name": cur_name, "parent": parent, "type": type});
        }
    }

    function getGroups(item, collector, parent){
        for (var prop in item) {
            var cur_name = item[prop]['name'];
            var type = 'groups';
            collector.push({"name": cur_name, "parent": parent, "type": type});
            if(item[prop].hasOwnProperty('cartridges')) {
                getCartridges(item[prop].cartridges, collector, cur_name);
            }
            if(item[prop].hasOwnProperty('groups')) {
                getGroups(item[prop].groups, collector, cur_name)
            }
        }
    }

    return rawout;

}

// ************** Generate the tree diagram	 *****************
function generateGroupPreview(data) {
    //clean current graph and text
    $(".description-section").html('');

    //mapping data
    var dataMap = data.reduce(function(map, node) {
        map[node.name] = node;
        return map;
    }, {});
    var treeData = [];
    data.forEach(function(node) {
        // add to parent
        var parent = dataMap[node.parent];
        if (parent) {
            // create child array if it doesn't exist
            (parent.children || (parent.children = []))
                // add node to child array
                .push(node);
        } else {
            // parent is null or missing
            treeData.push(node);
        }
    });

    var source = treeData[0];

//generate position for tree view
    var margin = {top: 25, right: 5, bottom: 5, left: 5},
        width = 320 - margin.right - margin.left,
        height = 500 - margin.top - margin.bottom;

    var i = 0;

    var tree = d3.layout.tree()
        .size([height, width]);

    var diagonal = d3.svg.diagonal()
        .projection(function(d) { return [d.x, d.y]; });

    var svg = d3.select(".description-section").append("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform", "translate(" + -90+ "," + margin.top + ")");

    // Compute the new tree layout.
    var nodes = tree.nodes(source).reverse(),
        links = tree.links(nodes);

    // Normalize for fixed-depth.
    nodes.forEach(function(d) { d.y = d.depth * 100; });

    // Declare the nodesâ€¦
    var node = svg.selectAll("g.node")
        .data(nodes, function(d) { return d.id || (d.id = ++i); });

    // Enter the nodes.
    var nodeEnter = node.enter().append("g")
        .attr("class", "node")
        .attr("transform", function(d) {
            return "translate(" + d.x + "," + d.y + ")"; });

    nodeEnter.append("circle")
        .attr("r", 4)
        .style("fill", "#fff");

    nodeEnter.append("text")
        .attr("y", function(d) {
            return d.children || d._children ? -20 : 20; })
        .attr("dy", ".35em")
        .attr("text-anchor", "middle")
        .text(function(d) { return d.name; })
        .style("fill-opacity", 1);

    // Declare the links
    var link = svg.selectAll("path.link")
        .data(links, function(d) { return d.target.id; });

    // Enter the links.
    link.enter().insert("path", "g")
        .attr("class", "link")
        .attr("d", diagonal);

}

// ************* Add context menu for nodes ******************
//remove nodes from workarea
function deleteNode(endPoint){
    if(endPoint.attr('id') != 'applicationId'){
        var that=endPoint;      //get all of your DIV tags having endpoints
        for (var i=0;i<that.length;i++) {
            var endpoints = jsPlumb.getEndpoints($(that[i])); //get all endpoints of that DIV
            for (var m=0;m<endpoints.length;m++) {
                // if(endpoints[m].anchor.type=="TopCenter") //Endpoint on right side
                jsPlumb.deleteEndpoint(endpoints[m]);  //remove endpoint
            }
        }
        jsPlumb.detachAllConnections(endPoint);
        endPoint.remove();
    }

}

//genrate context menu for nodes
$(function(){
    /*$.contextMenu({
        selector: '.stepnode',
        callback: function(key, options) {
            var m = "clicked: " + key + $(this);
            if(key == 'delete'){
                deleteNode($(this));
            }else if(key == 'edit'){

            }
        },
        items: {
            "edit": {name: "Edit", icon: "edit"},
            "delete": {name: "Delete", icon: "delete"},
            "sep1": "---------",
            "quit": {name: "Quit", icon: "quit"}
        }
    });*/

});

var applicationJson = {};
//Definition JSON builder
function generateJsplumbTree(collector, connections){

    //get general data
    $('input.level-root').each(function(){
        var inputId = $(this).attr('id');
        collector[inputId] = $(this).val();
    });
    collector['components']={};
    collector['components']['dependencies']={};
    collector['components']['groups']=[];
    collector['components']['cartridges']=[];
    collector['components']['dependencies']['startupOrders']=[];
    collector['components']['dependencies']['scalingDependents']=[];
    var startupOrders = $('input#startupOrders').val().split(' ').join('').split(/["][,]+/g);
    for (var i = 0; i < startupOrders.length; i++) {
        startupOrders[i] = startupOrders[i].replace(/"/g, "");
    }

    var scalingDependents = $('input#scalingDependents').val().split(' ').join('').split(/["][,]+/g);
    for (var i = 0; i < scalingDependents.length; i++) {
        scalingDependents[i] = scalingDependents[i].replace(/"/g, "");
    }
    collector['components']['dependencies']['startupOrders'] = startupOrders;
    collector['components']['dependencies']['scalingDependents'] = scalingDependents;
    collector['components']['dependencies']['terminationBehaviour']=$('select#terminationBehaviour').val();

    //generate raw data tree from connections
    var rawtree = [];
    $.each(jsPlumb.getConnections(), function (idx, connection) {
        var dataType = $('#'+connection.targetId).attr('data-type');
        var jsonContent = JSON.parse(decodeURIComponent($('#'+connection.targetId).attr('data-generated')));
        rawtree.push({
            parent: connection.sourceId,
            content: jsonContent,
            dtype:dataType,
            id: connection.targetId
        });
    });

    //generate heirache by adding json and extra info
    var nodes = [];
    var toplevelNodes = [];
    var lookupList = {};

    for (var i = 0; i < rawtree.length; i++) {
        var n = rawtree[i].content;
        if(rawtree[i].dtype == 'cartridge'){
            n.id =  rawtree[i].id;
            n.parent_id = ((rawtree[i].parent == 'applicationId') ? 'applicationId': rawtree[i].parent);
            n.dtype =rawtree[i].dtype;
        }else if(rawtree[i].dtype == 'group'){
            n.id =  rawtree[i].id;
            n.parent_id = ((rawtree[i].parent == 'applicationId') ? 'applicationId': rawtree[i].parent);
            n.dtype =rawtree[i].dtype;
            n.groups = [];
            n.cartridges =[];
        }

        lookupList[n.id] = n;
        nodes.push(n);

        if (n.parent_id == 'applicationId' && rawtree[i].dtype == 'cartridge') {
            collector['components']['cartridges'].push(n);
        }else if(n.parent_id == 'applicationId' && rawtree[i].dtype == 'group'){
            collector['components']['groups'].push(n);
        }

    }

    //merge any root level stuffs
    for (var i = 0; i < nodes.length; i++) {
        var n = nodes[i];
        if (!(n.parent_id == 'applicationId') && n.dtype == 'cartridge') {
            lookupList[n.parent_id]['cartridges'] = lookupList[n.parent_id]['cartridges'].concat([n]);
        }else if(!(n.parent_id == 'applicationId') && n.dtype == 'group'){
            lookupList[n.parent_id]['groups'] = lookupList[n.parent_id]['groups'].concat([n]);
        }
    }

    //cleanup JSON, remove extra items added to object level
    function traverse(o) {
        for (var i in o) {
            if(i == 'id' || i == 'parent_id' || i == 'dtype'){
                delete o[i];
            }else if(i == 'groups' && o[i].length == 0){
                delete o[i];
            }
            if (o[i] !== null && typeof(o[i])=="object") {
                //going on step down in the object tree!!
                traverse(o[i]);
            }
        }
    }

    traverse(collector);

    return collector;
}

//setting up schema and defaults
var cartridgeBlockTemplate = {
    "type":"object",
    "$schema": "http://json-schema.org/draft-03/schema",
    "id": "root",
    "format":"grid",
    "properties":{
        "type": {
            "type":"string",
            "id": "root/type",
            "default": "name",
            "required":false
        },
        "cartridgeMax": {
            "type":"number",
            "id": "root/cartridgeMax",
            "default":2,
            "required":false
        },
        "cartridgeMin": {
            "type":"number",
            "id": "root/cartridgeMin",
            "default":1,
            "required":false
        },
        "subscribableInfo": {
            "type":"object",
            "id": "root/subscribableInfo",
            "required":false,
            "properties":{
                "alias": {
                    "type":"string",
                    "id": "root/subscribableInfo/alias",
                    "default": "alias2",
                    "required":false
                },
                "autoscalingPolicy": {
                    "type":"string",
                    "id": "root/subscribableInfo/autoscalingPolicy",
                    "default": "autoscale_policy_1",
                    "required":false
                },
                "privateRepo": {
                    "type":"string",
                    "id": "root/subscribableInfo/privateRepo",
                    "default": "true",
                    "required":false
                },
                "repoPassword": {
                    "type":"string",
                    "id": "root/subscribableInfo/repoPassword",
                    "default": "password",
                    "required":false
                },
                "repoURL": {
                    "type":"string",
                    "id": "root/subscribableInfo/repoURL",
                    "default": "http://xxx:10080/git/default.git",
                    "required":false
                },
                "repoUsername": {
                    "type":"string",
                    "id": "root/subscribableInfo/repoUsername",
                    "default": "user",
                    "required":false
                }
            }
        }
    }
};

var cartridgeBlockDefault = {
    "type":"tomcat",
    "cartridgeMin":1,
    "cartridgeMax":2,
    "subscribableInfo":{
        "alias":"alias2",
        "autoscalingPolicy":"autoscale_policy_1",
        "privateRepo":"true",
        "repoPassword":"password",
        "repoURL":"http://xxx:10080/git/default.git",
        "repoUsername":"user"
    }
};

var groupBlockTemplate = {
    "type":"object",
    "$schema": "http://json-schema.org/draft-03/schema",
    "id": "root",
    "required":false,
    "properties":{
        "name": {
            "type":"string",
            "id": "root/name",
            "default": "name",
            "required":false
        },
        "alias": {
            "type":"string",
            "id": "root/alias",
            "default": "alias",
            "required":false
        },
        "groupMaxInstances": {
            "type":"number",
            "id": "root/groupMaxInstances",
            "default":2,
            "required":false
        },
        "groupMinInstances": {
            "type":"number",
            "id": "root/groupMinInstances",
            "default":1,
            "required":false
        },
        "isGroupScalingEnabled": {
            "type":"boolean",
            "id": "root/isGroupScalingEnabled",
            "default": "false",
            "required":false
        }
    }
};

var groupBlockDefault = {
    "name":"group2",
    "alias":"group2alias",
    "groupMinInstances":1,
    "groupMaxInstances":2,
    "isGroupScalingEnabled":"false"
};

//create cartridge list
var cartridgeListHtml='';
function generateCartridges(data){
    for(var cartridge in data){
        var cartridgeData = data[cartridge];
        cartridgeListHtml += '<div class="block-cartridge" ' +
            'data-info="'+cartridgeData.description+ '"'+
            'data-toggle="tooltip" data-placement="bottom" title="Single Click to view details. Double click to add"'+
            'id="'+cartridgeData.cartridgeType+'">'
            + cartridgeData.displayName+
            '</div>'
    }
    //append cartridge into html content
    $('#cartridge-list').append(cartridgeListHtml);
}

//create group list
var groupListHtml='';
function generateGroups(data){
    for(var group in data){
        var groupData = data[group];
        groupListHtml += '<div class="block-group" ' +
            ' data-info="'+encodeURIComponent(JSON.stringify(groupData))+'"'+
            'id="'+groupData.name+'">'
            + groupData.name+
            '</div>'
    }
    //append cartridge into html content
    $('#group-list').append(groupListHtml);
}
// Document ready events
$(document).ready(function(){

    $('#deploy').attr('disabled','disabled');

    $('#deploy').on('click', function(){
        var appJSON = generateJsplumbTree(applicationJson, jsPlumb.getConnections());
        var btn = $(this);
        var formtype = 'applications';
        btn.html("<i class='fa fa-spinner fa-spin'></i> Deploying...");
        $.ajax({
            type: "POST",
            url: caramel.context + "/controllers/applications/application_requests.jag",
            dataType: 'json',
            data: { "formPayload": JSON.stringify(appJSON), "formtype": formtype },
            success: function (data) {
                if (data.status == 'error') {
                    var n = noty({text: data.message, layout: 'bottomRight', type: 'error'});
                } else if (data.status == 'warning') {
                    var n = noty({text: data.message, layout: 'bottomRight', type: 'warning'});
                } else {
                    var n = noty({text: data.message, layout: 'bottomRight', type: 'success'});
                }
            }
        })
            .always(function () {
                btn.html('Deploy Application Definition');
            });

    });

    //*******************Adding JSON editor *************//
    JSONEditor.defaults.theme = 'bootstrap3';
    JSONEditor.defaults.iconlib = 'fontawesome4';
    JSONEditor.defaults.show_errors = "always";
    var editor, blockId;


    DragEl(".stepnode");
    Repaint();

    $('#whiteboard').on('click', '.stepnode', function(){
        //get tab activated
        if($(this).attr('id') == 'applicationId'){
            activateTab('general');
        }else{
            activateTab('components');
            $('#component-info-update').prop("disabled", false);
        }

        blockId = $(this).attr('id');
        var blockType = $(this).attr('data-type');
        var startval;
        var ctype = $(this).attr('data-ctype');
        if(blockType == 'cartridge' || blockType == 'group-cartridge'){
            startval = cartridgeBlockDefault;
            startval['type'] = ctype;
        }else{
            startval = groupBlockDefault;
            startval['name'] = ctype;
        }

        if($(this).attr('data-generated')) {
            startval = JSON.parse(decodeURIComponent($(this).attr('data-generated')));
        }
        $('#component-data').html('');

        switch (blockType){
            case 'cartridge':
                generateHtmlBlock(cartridgeBlockTemplate, startval);
                break;

            case 'group':
                generateHtmlBlock(groupBlockTemplate, startval);
                break;

            case 'group-cartridge':
                generateHtmlBlock(cartridgeBlockTemplate, startval);
                break;
        }

    });

    function generateHtmlBlock(schema, startval){
        // Initialize the editor
        editor = new JSONEditor(document.getElementById('component-data'), {
            ajax: false,
            disable_edit_json: true,
            schema: schema,
            format: "grid",
            startval: startval
        });
        if(editor.getEditor('root.type')){
            editor.getEditor('root.type').disable();
        }else{
            editor.getEditor('root.name').disable();
        }

    }

    //get component JSON data
    $('#component-info-update').on('click', function(){
        $('#'+blockId).attr('data-generated', encodeURIComponent(JSON.stringify(editor.getValue())));
        $('#'+blockId).removeClass('input-false');
        $('#'+blockId).find('div>i').removeClass('fa-exclamation-circle').addClass('fa-check-circle-o').css('color','#2ecc71');
        $('#deploy').prop("disabled", false);
    });

    //get create cartridge list
    generateCartridges(cartridgeList.cartridge);
    //get group JSON
    generateGroups(groupList.groups);

    //handle single click for cartridge
    $('#cartridge-list').on('click', ".block-cartridge", function(){
        $('.description-section').html($(this).attr('data-info'));
    });
    //handle double click for cartridge
    $('#cartridge-list').on('dblclick', ".block-cartridge", function(){
        addJsplumbCartridge($(this).attr('id'),cartridgeCounter);
        //increase global count for instances
        cartridgeCounter++;
    });

    //handle single click for groups
    $('#group-list').on('click', ".block-group", function(){
        var groupJSON = JSON.parse(decodeURIComponent($(this).attr('data-info')));
        mydata = generateGroupTree(groupJSON);
        generateGroupPreview(mydata);


    });
    //handle double click event for groups
    $('#group-list').on('dblclick', ".block-group", function(){
        var groupJSON = JSON.parse(decodeURIComponent($(this).attr('data-info')));
        addJsplumbGroup(groupJSON,cartridgeCounter);
        //increase global count for instances
        cartridgeCounter++;
    });


});


