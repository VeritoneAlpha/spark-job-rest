var allContexts = [
    ["gugu","16002"],
    ["radu", "16001"]
];

$(function() {
    var contextsTable = function () {
        var ctxTable = $('#contextsTable');
        if(!ctxTable.length) {
            return false;
        }
        var tbody = ctxTable.find('tbody');
            addContextModal = $('#addContext'),
            ctx_name = $('#ctx_name'),
            ctx_param = $('#ctx_param'),
            ctx_jar = $('#ctx_jar'),
            addContextParams = $('#paramsTable tbody'),
            addContextJars = $('#jarsTable tbody'),
            jarSuggestList = $('#jarSuggestList'),
            jarDropdownToggle = $('#jarDropdownToggle');

        function getAllJars() {
            return $.ajax({
                url: "http://localhost:8097/jars/",
                type: "get"
            });
        }

        function getAllContexts() {
            return $.ajax({
                url: "http://localhost:8097/contexts/",
                type: "get"
            });
        }

        function deleteContext(ctxName) {
            return $.ajax({
                url: "http://localhost:8097/contexts/"+ ctxName,
                type: "delete",
                dataType: "text"
            });
        }

        var generatedContext = {
            name: '',
            params: [],
            jars: []
        };

        function addContext() {
            var output = 'jars="';
                output += generatedContext.jars.join();
                output +='"';
                for(var i = 0; i < generatedContext.params.length; i++) {
                    output += '\n' + generatedContext.params[i];
                }


            return $.ajax({
                url: "http://localhost:8097/contexts/"+ generatedContext.name,
                type: "post",
                processData: false,
                data: output
            });
        }

        getAllContexts().done(function(data) {
            var output = '';
            for(var i = 0; i < data.length; i++) {
                output += '<tr>';
                for(var ii = 0; ii < data[i].length; ii++) {
                    output += '<td>';
                    output += data[i][ii];
                    output +='</td>';
                }
                output += '<td><a class="delete" data-context="'+ data[i][0] +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>';
                output += '</tr>';
            }

            tbody.html(output);
        });


        tbody.on('click','.delete', function() {
            var this_ = $(this),
                ctx = this_.data('context');

            deleteContext(ctx).done(function(data) {
                this_.closest('tr').remove();
            });

        });

        addContextModal.find('.btn-save').on('click', function () {
            if($.trim(ctx_name.val())=='') {
                ctx_name.focus();
                return false;
            }

            if(!generatedContext.jars.length) {
                ctx_jar.focus();
                return false;
            }


            generatedContext.name = ctx_name.val();

            addContext()
                .done(function(data) {
                    var output = '<tr>';
                    output += '<td>'+ generatedContext.name +'</td>';
                    output += '<td>'+ data +'</td>';
                    output += '<td><a class="delete" data-context="'+ generatedContext.name +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>';
                    output += '</tr>';


                    tbody.prepend(output);
                    addContextModal.modal('hide');
                })
                .fail(function(data) {
                    alert('ERROR');
                });
        });

        addContextModal.find('.jar-add').on('click', function () {
            if($.trim(ctx_jar.val())=='') {
                ctx_jar.focus();
                return false;
            }
            generatedContext.jars.push(ctx_jar.val());
            var output = '<tr>';
                    output += '<td>';
                        output += ctx_jar.val();
                    output += '</td>';
                    output += '<td><a class="delete" data-jar="'+ ctx_jar.val() +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>';
                output += '</tr>';
            addContextJars.append($(output));
            ctx_jar.val('');
        });

        addContextJars.on('click','.delete', function() {
            var this_ = $(this),
                removeJar = this_.data('jar');

            generatedContext.jars = jQuery.grep(generatedContext.jars, function(value) {
                return value != removeJar;
            });
            this_.closest('tr').remove();
        });

        addContextModal.find('.param-add').on('click', function () {
            if($.trim(ctx_param.val())=='') {
                ctx_param.focus();
                return false;
            }
            generatedContext.params.push(ctx_param.val());
            var output = '<tr>';
                    output += '<td>';
                        output += ctx_param.val();
                    output += '</td>';
                    output += '<td><a class="delete" data-param="'+ ctx_param.val() +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>';
                output += '</tr>';
            addContextParams.append($(output));
            ctx_param.val('');
        });

        addContextParams.on('click','.delete', function() {
            var this_ = $(this),
                removeParam = this_.data('param');

            generatedContext.params = jQuery.grep(generatedContext.params, function(value) {
                return value != removeParam;
            });
            this_.closest('tr').remove();
        });

        addContextModal.on('hidden.bs.modal', function () {
            ctx_name.val('');
            ctx_param.val('');
            ctx_jar.val('');
            generatedContext = {
                name: '',
                params: [],
                jars: []
            };
            addContextParams.html('');
            jarSuggestList.html('');
            jarDropdownToggle.prop('disabled',true);
        });

        addContextModal.on('show.bs.modal', function () {
            getAllJars().done(function(data) {
                if(data.length) {
                    jarDropdownToggle.prop('disabled',false);
                }
                var output = '';
                for(var i = 0; i < data.length; i++) {
                    output += '<li>';
                    output += '<a href="#">';
                    output += data[i][0];
                    output += '</a>';
                    output += '</li>';
                }

                jarSuggestList.html(output);
            });
        });

        jarSuggestList.on('click','a', function(e) {
            ctx_jar.val($(this).text());
            e.preventDefault();
        });









    };
    contextsTable();
});
