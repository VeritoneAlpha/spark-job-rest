/**
 * bootbox.js v4.4.0
 *
 * http://bootboxjs.com/license.txt
 */
!function(a,b){"use strict";"function"==typeof define&&define.amd?define(["jquery"],b):"object"==typeof exports?module.exports=b(require("jquery")):a.bootbox=b(a.jQuery)}(this,function a(b,c){"use strict";function d(a){var b=q[o.locale];return b?b[a]:q.en[a]}function e(a,c,d){a.stopPropagation(),a.preventDefault();var e=b.isFunction(d)&&d.call(c,a)===!1;e||c.modal("hide")}function f(a){var b,c=0;for(b in a)c++;return c}function g(a,c){var d=0;b.each(a,function(a,b){c(a,b,d++)})}function h(a){var c,d;if("object"!=typeof a)throw new Error("Please supply an object of options");if(!a.message)throw new Error("Please specify a message");return a=b.extend({},o,a),a.buttons||(a.buttons={}),c=a.buttons,d=f(c),g(c,function(a,e,f){if(b.isFunction(e)&&(e=c[a]={callback:e}),"object"!==b.type(e))throw new Error("button with key "+a+" must be an object");e.label||(e.label=a),e.className||(e.className=2>=d&&f===d-1?"btn-primary":"btn-default")}),a}function i(a,b){var c=a.length,d={};if(1>c||c>2)throw new Error("Invalid argument length");return 2===c||"string"==typeof a[0]?(d[b[0]]=a[0],d[b[1]]=a[1]):d=a[0],d}function j(a,c,d){return b.extend(!0,{},a,i(c,d))}function k(a,b,c,d){var e={className:"bootbox-"+a,buttons:l.apply(null,b)};return m(j(e,d,c),b)}function l(){for(var a={},b=0,c=arguments.length;c>b;b++){var e=arguments[b],f=e.toLowerCase(),g=e.toUpperCase();a[f]={label:d(g)}}return a}function m(a,b){var d={};return g(b,function(a,b){d[b]=!0}),g(a.buttons,function(a){if(d[a]===c)throw new Error("button key "+a+" is not allowed (options are "+b.join("\n")+")")}),a}var n={dialog:"<div class='bootbox modal' tabindex='-1' role='dialog'><div class='modal-dialog'><div class='modal-content'><div class='modal-body'><div class='bootbox-body'></div></div></div></div></div>",header:"<div class='modal-header'><h4 class='modal-title'></h4></div>",footer:"<div class='modal-footer'></div>",closeButton:"<button type='button' class='bootbox-close-button close' data-dismiss='modal' aria-hidden='true'>&times;</button>",form:"<form class='bootbox-form'></form>",inputs:{text:"<input class='bootbox-input bootbox-input-text form-control' autocomplete=off type=text />",textarea:"<textarea class='bootbox-input bootbox-input-textarea form-control'></textarea>",email:"<input class='bootbox-input bootbox-input-email form-control' autocomplete='off' type='email' />",select:"<select class='bootbox-input bootbox-input-select form-control'></select>",checkbox:"<div class='checkbox'><label><input class='bootbox-input bootbox-input-checkbox' type='checkbox' /></label></div>",date:"<input class='bootbox-input bootbox-input-date form-control' autocomplete=off type='date' />",time:"<input class='bootbox-input bootbox-input-time form-control' autocomplete=off type='time' />",number:"<input class='bootbox-input bootbox-input-number form-control' autocomplete=off type='number' />",password:"<input class='bootbox-input bootbox-input-password form-control' autocomplete='off' type='password' />"}},o={locale:"en",backdrop:"static",animate:!0,className:null,closeButton:!0,show:!0,container:"body"},p={};p.alert=function(){var a;if(a=k("alert",["ok"],["message","callback"],arguments),a.callback&&!b.isFunction(a.callback))throw new Error("alert requires callback property to be a function when provided");return a.buttons.ok.callback=a.onEscape=function(){return b.isFunction(a.callback)?a.callback.call(this):!0},p.dialog(a)},p.confirm=function(){var a;if(a=k("confirm",["cancel","confirm"],["message","callback"],arguments),a.buttons.cancel.callback=a.onEscape=function(){return a.callback.call(this,!1)},a.buttons.confirm.callback=function(){return a.callback.call(this,!0)},!b.isFunction(a.callback))throw new Error("confirm requires a callback");return p.dialog(a)},p.prompt=function(){var a,d,e,f,h,i,k;if(f=b(n.form),d={className:"bootbox-prompt",buttons:l("cancel","confirm"),value:"",inputType:"text"},a=m(j(d,arguments,["title","callback"]),["cancel","confirm"]),i=a.show===c?!0:a.show,a.message=f,a.buttons.cancel.callback=a.onEscape=function(){return a.callback.call(this,null)},a.buttons.confirm.callback=function(){var c;switch(a.inputType){case"text":case"textarea":case"email":case"select":case"date":case"time":case"number":case"password":c=h.val();break;case"checkbox":var d=h.find("input:checked");c=[],g(d,function(a,d){c.push(b(d).val())})}return a.callback.call(this,c)},a.show=!1,!a.title)throw new Error("prompt requires a title");if(!b.isFunction(a.callback))throw new Error("prompt requires a callback");if(!n.inputs[a.inputType])throw new Error("invalid prompt type");switch(h=b(n.inputs[a.inputType]),a.inputType){case"text":case"textarea":case"email":case"date":case"time":case"number":case"password":h.val(a.value);break;case"select":var o={};if(k=a.inputOptions||[],!b.isArray(k))throw new Error("Please pass an array of input options");if(!k.length)throw new Error("prompt with select requires options");g(k,function(a,d){var e=h;if(d.value===c||d.text===c)throw new Error("given options in wrong format");d.group&&(o[d.group]||(o[d.group]=b("<optgroup/>").attr("label",d.group)),e=o[d.group]),e.append("<option value='"+d.value+"'>"+d.text+"</option>")}),g(o,function(a,b){h.append(b)}),h.val(a.value);break;case"checkbox":var q=b.isArray(a.value)?a.value:[a.value];if(k=a.inputOptions||[],!k.length)throw new Error("prompt with checkbox requires options");if(!k[0].value||!k[0].text)throw new Error("given options in wrong format");h=b("<div/>"),g(k,function(c,d){var e=b(n.inputs[a.inputType]);e.find("input").attr("value",d.value),e.find("label").append(d.text),g(q,function(a,b){b===d.value&&e.find("input").prop("checked",!0)}),h.append(e)})}return a.placeholder&&h.attr("placeholder",a.placeholder),a.pattern&&h.attr("pattern",a.pattern),a.maxlength&&h.attr("maxlength",a.maxlength),f.append(h),f.on("submit",function(a){a.preventDefault(),a.stopPropagation(),e.find(".btn-primary").click()}),e=p.dialog(a),e.off("shown.bs.modal"),e.on("shown.bs.modal",function(){h.focus()}),i===!0&&e.modal("show"),e},p.dialog=function(a){a=h(a);var d=b(n.dialog),f=d.find(".modal-dialog"),i=d.find(".modal-body"),j=a.buttons,k="",l={onEscape:a.onEscape};if(b.fn.modal===c)throw new Error("$.fn.modal is not defined; please double check you have included the Bootstrap JavaScript library. See http://getbootstrap.com/javascript/ for more details.");if(g(j,function(a,b){k+="<button data-bb-handler='"+a+"' type='button' class='btn "+b.className+"'>"+b.label+"</button>",l[a]=b.callback}),i.find(".bootbox-body").html(a.message),a.animate===!0&&d.addClass("fade"),a.className&&d.addClass(a.className),"large"===a.size?f.addClass("modal-lg"):"small"===a.size&&f.addClass("modal-sm"),a.title&&i.before(n.header),a.closeButton){var m=b(n.closeButton);a.title?d.find(".modal-header").prepend(m):m.css("margin-top","-10px").prependTo(i)}return a.title&&d.find(".modal-title").html(a.title),k.length&&(i.after(n.footer),d.find(".modal-footer").html(k)),d.on("hidden.bs.modal",function(a){a.target===this&&d.remove()}),d.on("shown.bs.modal",function(){d.find(".btn-primary:first").focus()}),"static"!==a.backdrop&&d.on("click.dismiss.bs.modal",function(a){d.children(".modal-backdrop").length&&(a.currentTarget=d.children(".modal-backdrop").get(0)),a.target===a.currentTarget&&d.trigger("escape.close.bb")}),d.on("escape.close.bb",function(a){l.onEscape&&e(a,d,l.onEscape)}),d.on("click",".modal-footer button",function(a){var c=b(this).data("bb-handler");e(a,d,l[c])}),d.on("click",".bootbox-close-button",function(a){e(a,d,l.onEscape)}),d.on("keyup",function(a){27===a.which&&d.trigger("escape.close.bb")}),b(a.container).append(d),d.modal({backdrop:a.backdrop?"static":!1,keyboard:!1,show:!1}),a.show&&d.modal("show"),d},p.setDefaults=function(){var a={};2===arguments.length?a[arguments[0]]=arguments[1]:a=arguments[0],b.extend(o,a)},p.hideAll=function(){return b(".bootbox").modal("hide"),p};var q={bg_BG:{OK:"Ок",CANCEL:"Отказ",CONFIRM:"Потвърждавам"},br:{OK:"OK",CANCEL:"Cancelar",CONFIRM:"Sim"},cs:{OK:"OK",CANCEL:"Zrušit",CONFIRM:"Potvrdit"},da:{OK:"OK",CANCEL:"Annuller",CONFIRM:"Accepter"},de:{OK:"OK",CANCEL:"Abbrechen",CONFIRM:"Akzeptieren"},el:{OK:"Εντάξει",CANCEL:"Ακύρωση",CONFIRM:"Επιβεβαίωση"},en:{OK:"OK",CANCEL:"Cancel",CONFIRM:"OK"},es:{OK:"OK",CANCEL:"Cancelar",CONFIRM:"Aceptar"},et:{OK:"OK",CANCEL:"Katkesta",CONFIRM:"OK"},fa:{OK:"قبول",CANCEL:"لغو",CONFIRM:"تایید"},fi:{OK:"OK",CANCEL:"Peruuta",CONFIRM:"OK"},fr:{OK:"OK",CANCEL:"Annuler",CONFIRM:"D'accord"},he:{OK:"אישור",CANCEL:"ביטול",CONFIRM:"אישור"},hu:{OK:"OK",CANCEL:"Mégsem",CONFIRM:"Megerősít"},hr:{OK:"OK",CANCEL:"Odustani",CONFIRM:"Potvrdi"},id:{OK:"OK",CANCEL:"Batal",CONFIRM:"OK"},it:{OK:"OK",CANCEL:"Annulla",CONFIRM:"Conferma"},ja:{OK:"OK",CANCEL:"キャンセル",CONFIRM:"確認"},lt:{OK:"Gerai",CANCEL:"Atšaukti",CONFIRM:"Patvirtinti"},lv:{OK:"Labi",CANCEL:"Atcelt",CONFIRM:"Apstiprināt"},nl:{OK:"OK",CANCEL:"Annuleren",CONFIRM:"Accepteren"},no:{OK:"OK",CANCEL:"Avbryt",CONFIRM:"OK"},pl:{OK:"OK",CANCEL:"Anuluj",CONFIRM:"Potwierdź"},pt:{OK:"OK",CANCEL:"Cancelar",CONFIRM:"Confirmar"},ru:{OK:"OK",CANCEL:"Отмена",CONFIRM:"Применить"},sq:{OK:"OK",CANCEL:"Anulo",CONFIRM:"Prano"},sv:{OK:"OK",CANCEL:"Avbryt",CONFIRM:"OK"},th:{OK:"ตกลง",CANCEL:"ยกเลิก",CONFIRM:"ยืนยัน"},tr:{OK:"Tamam",CANCEL:"İptal",CONFIRM:"Onayla"},zh_CN:{OK:"OK",CANCEL:"取消",CONFIRM:"确认"},zh_TW:{OK:"OK",CANCEL:"取消",CONFIRM:"確認"}};return p.addLocale=function(a,c){return b.each(["OK","CANCEL","CONFIRM"],function(a,b){if(!c[b])throw new Error("Please supply a translation for '"+b+"'")}),q[a]={OK:c.OK,CANCEL:c.CANCEL,CONFIRM:c.CONFIRM},p},p.removeLocale=function(a){return delete q[a],p},p.setLocale=function(a){return p.setDefaults("locale",a)},p.init=function(c){return a(c||b)},p});


/**
 * Copyright (c) 2011-2014 Felix Gnass
 * Licensed under the MIT license
 */

/*

Basic Usage:
============

$('#el').spin(); // Creates a default Spinner using the text color of #el.
$('#el').spin({ ... }); // Creates a Spinner using the provided options.

$('#el').spin(false); // Stops and removes the spinner.

Using Presets:
==============

$('#el').spin('small'); // Creates a 'small' Spinner using the text color of #el.
$('#el').spin('large', '#fff'); // Creates a 'large' white Spinner.

Adding a custom preset:
=======================

$.fn.spin.presets.flower = {
  lines: 9,
  length: 10,
  width: 20,
  radius: 0
}

$('#el').spin('flower', 'red');

*/

(function(factory) {

  if (typeof exports == 'object') {
    // CommonJS
    factory(require('jquery'), require('spin.js'));
  }
  else if (typeof define == 'function' && define.amd) {
    // AMD, register as anonymous module
    define(['jquery', 'spin'], factory);
  }
  else {
    // Browser globals
    if (!window.Spinner) {throw new Error('Spin.js not present');}
    factory(window.jQuery, window.Spinner)
  }

}(function($, Spinner) {

  $.fn.spin = function(opts, color) {

    return this.each(function() {
      var $this = $(this),
        data = $this.data();

      if (data.spinner) {
        data.spinner.stop();
        delete data.spinner;
      }
      if (opts !== false) {
        opts = $.extend(
          { color: color || $this.css('color') },
          $.fn.spin.presets[opts] || opts
        );
        data.spinner = new Spinner(opts).spin(this);
      }
    })
  };

  $.fn.spin.presets = {
    tiny: { lines: 8, length: 2, width: 2, radius: 3 },
    small: { lines: 8, length: 4, width: 3, radius: 5 },
    large: { lines: 10, length: 8, width: 4, radius: 8 }
  }

}));


var errorHandler = function(msg) {
    console.log("=======ERROR=======");
    console.log(msg);
    console.log("===================");
};

var sparkJobTemplate = function () {
    "use strict";

    var Self = this,

        ajaxLoader = $('#ajaxLoader'),
        navTabs = $('#navTabs'),

        ctxTable = $('#contextsTable tbody'),
        addContextModal = $('#addContext'),
        ctx_name = $('#ctx_name'),
        ctx_param = $('#ctx_param'),
        ctx_jar = $('#ctx_jar'),
        addContextParams = $('#ctxParamsTable tbody'),
        addContextJars = $('#ctxJarsTable tbody'),
        jarSuggestList = $('#jarSuggestList'),
        jarDropdownToggle = $('#jarDropdownToggle'),
        generatedContext = {
            name: '',
            params: [],
            jars: []
        },


        jobsTable = $('#jobsTable tbody'),
        runJobModal = $('#runJob'),
        job_class = $('#job_class'),
        addJobParams = $('#jobParamsTable tbody'),
        ctxSuggestList = $('#ctxSuggestList'),
        job_param = $('#job_param'),
        runJob = {
            klass: '',
            context: '',
            params: []
        },

        jarsTable = $('#jarsTable tbody'),
        uploadJarModal = $('#uploadJar'),
        uploadJarInput = $('#uploadJarInput');

    Self.init = function(options) {
        Self.params = options;
        Self.addEvents();
    },

    Self.addEvents = function() {

        // start contexts tab
        navTabs.find('a[aria-controls="contexts"]').on('click', function () {
            Self.getAllContexts().done(function(data) {
                var response = data.contexts,
                    output = '';

                for(var i = 0; i < response.length; i++) {
                    output += '<tr>' +
                                '<td>' +  response[i].contextName + '</td>' +
                                '<td>' +  '<a href=' + Self.params.host + ':' + response[i].sparkUiPort + '>' + response[i].sparkUiPort + '</a>' + '</td>' +
                                '<td><a class="delete" data-context="'+ response[i].contextName +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>' +
                            '</tr>';
                }

                ctxTable.html(output);
            });
        });


        ctxTable.on('click','.delete', function() {
            var this_ = $(this),
                ctx = this_.data('context');

            Self.deleteContext(ctx).done(function() {
                this_.closest('tr').remove();
                Self.notifySuccess("Deleted context: " + ctx)
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

            Self.lockScreen();

            Self.addContext()
                .done(function(data) {
                    var response = data,
                        output = '';

                    output += '<tr>' +
                                '<td>'+ response.contextName  +'</td>' +
                                '<td>' +  '<a href=' + Self.params.host + ':' + response.sparkUiPort + '>' + response.sparkUiPort + '</a>' + '</td>' +
                                '<td><a class="delete" data-context="'+ response.contextName +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>' +
                            '</tr>';

                    ctxTable.prepend(output);
                    Self.notifySuccess("Created context: " + response.contextName);
                })
                .fail(function(data) {
                    Self.notify(data.responseJSON.error);
                })
                .always(function(data) {
                    Self.enableScreen();
                    addContextModal.modal('hide');
                });
        });

        addContextModal.find('.jar-add').on('click', function () {
            if($.trim(ctx_jar.val())=='') {
                ctx_jar.focus();
                return false;
            }
            var output = '';
            generatedContext.jars.push(ctx_jar.val());

            output += '<tr>' +
                            '<td>' + ctx_jar.val() + '</td>' +
                            '<td><a class="delete" data-jar="'+ ctx_jar.val() +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>' +
                        '</tr>';
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
            var output = '';
            generatedContext.params.push(ctx_param.val());

            output += '<tr>' +
                            '<td>' +  ctx_param.val() + '</td>' +
                            '<td><a class="delete" data-param="'+ ctx_param.val() +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>' +
                        '</tr>';

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
            addContextJars.html('');
            jarDropdownToggle.prop('disabled',true);
        });

        addContextModal.on('show.bs.modal', function () {
            Self.getAllJars().done(function(data) {
                var response = data.jars,
                    output = '';

                if(response.length) {
                    jarDropdownToggle.prop('disabled',false);
                } else {
                    jarDropdownToggle.prop('disabled',true);
                }

                for(var i = 0; i < response.length; i++) {
                    output += '<li>' +
                                '<a href="#">' + response[i].name + '</a>' +
                            '</li>';
                }

                jarSuggestList.html(output);
            });
        });

        jarSuggestList.on('click','a', function(e) {
            ctx_jar.val($(this).text());
            e.preventDefault();
        });
        // end contexts tab


        /**
         * Returns true only for job which status means they have result
         * @param {string} status job status
         * @returns {boolean}
         */
        function isJobWithResult(status) {
            return ['Submitted', 'Queued', 'Running'].indexOf(status) < 0;
        }

        /**
         * Renders job to HTML
         * @param {object} job
         * @returns {string}
         */
        function renderJob(job) {
            var output = '',
                result = '';

            if(isJobWithResult(job.status)) {
                result = '<a class="details">' +
                '<span aria-hidden="true" class="glyphicon glyphicon-modal-window"></span>' +
                '<div style="display:none" class="data">' + job.result + '</div>' +
                '</a>';
            } else {
                result = '';
            }

            output += '<tr>' +
                '<td>' + job.jobId  +'</td>' +
                '<td>' + job.contextName +'</td>' +
                '<td>' + job.status +'</td>' +
                '<td>' + (job.startTime || "") + '</td>' +
                '<td>' + (job.duration || "") + '</td>' +
                '<td>' + result +'</td>' +
                '</tr>';
            return output;
        }

        // start jobs tab
        navTabs.find('a[aria-controls="jobs"]').on('click', function () {
            Self.getAllJobs().done(function(data) {
                var output = '';

                for(var i = 0; i < data.length; i++) {
                    output += renderJob(data[i]);
                }

                jobsTable.html(output);
            });
        });


        jobsTable.on('click','.details', function() {
            var $this = $(this),
                result = $this.select('#data').text() + '';

            if(result) {
                bootbox.alert(result);
            }
        });


        runJobModal.find('.btn-save').on('click', function () {
            if($.trim(job_class.val())=='') {
                job_class.focus();
                return false;
            }

            if(!ctxSuggestList.val()) {
                ctxSuggestList.focus();
                return false;
            }

            runJob.context = ctxSuggestList.val();
            runJob.klass = job_class.val();


            Self.lockScreen();

            Self.runJob()
                .done(function(response) {
                    jobsTable.prepend(renderJob(response));
                    Self.notifyInfo("Started running job: " + response.jobId);
                })
                .fail(function(data) {
                    Self.notify(data.responseJSON.error);
                })
                .always(function() {
                    Self.enableScreen();
                    runJobModal.modal('hide');
                });
        });


        runJobModal.on('hidden.bs.modal', function () {
            job_class.val('');
            job_param.val('');
            runJob = {
                klass: '',
                context: '',
                params: []
            };

            addJobParams.html('');
            ctxSuggestList.html('');

        });

        runJobModal.on('show.bs.modal', function () {
            Self.getAllContexts().done(function(data) {
                var response = data.contexts,
                    output = '';

                for(var i = 0; i < response.length; i++) {
                    output += '<option>' + response[i].contextName + '</option>';
                }
                ctxSuggestList.html(output);
            });
        });


        runJobModal.find('.param-add').on('click', function () {
            if($.trim(job_param.val())=='') {
                job_param.focus();
                return false;
            }
            var output = '';
            runJob.params.push(job_param.val());

            output += '<tr>' +
                            '<td>' +  job_param.val() + '</td>' +
                            '<td><a class="delete" data-param="'+ job_param.val() +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>' +
                        '</tr>';

            addJobParams.append($(output));
            job_param.val('');
        });

        runJobModal.on('click','.delete', function() {
            var this_ = $(this),
                removeParam = this_.data('param');

            generatedContext.params = jQuery.grep(runJob.params, function(value) {
                return value != removeParam;
            });
            this_.closest('tr').remove();
        });
        // end jobs tab

        // start jars tab
        navTabs.find('a[aria-controls="jars"]').on('click', function () {
            Self.getAllJars().done(function(data) {
                var response = data.jars,
                    output = '';

                for(var i = 0; i < response.length; i++) {
                    output += '<tr>' +
                                '<td>' +  response[i].name + '</td>' +
                                '<td>' +  Self.computeJarSize(response[i].size) + '</td>' +
                                '<td>' +  Self.convertTimestamp(response[i].timestamp) + '</td>' +
                                '<td><a class="delete" data-jar="'+ response[i].name +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>' +
                            '</tr>';
                }

                jarsTable.html(output);
            });
        });

        jarsTable.on('click','.delete', function() {
            var this_ = $(this),
                ctx = this_.data('jar');

            Self.deleteJar(ctx).done(function(data) {
                Self.notifySuccess('Deleted jar: ' + ctx);
                this_.closest('tr').remove();
            });

        });
        uploadJarInput.fileinput({
            showPreview: false,
            showCancel: false,
            showUpload: false,
            uploadUrl: Self.params.url + "jars"
        });

        uploadJarInput.on('filebatchuploadsuccess', function(event, params) {
            Self.notifySuccess('Uploaded jar');
            Self.enableScreen();
            uploadJarModal.modal('hide');

            var response = params.response,
                output = '',
                duplicate = '';

            duplicate = jarsTable.find('a[data-jar="'+response.name+'"]');
            if(duplicate.length) {
                duplicate.closest('tr').remove();
            }
            output += '<tr>' +
                        '<td>'+ response.name  +'</td>' +
                        '<td>'+ Self.computeJarSize(response.size) +'</td>' +
                        '<td>'+ Self.convertTimestamp(response.timestamp) +'</td>' +
                        '<td><a class="delete" data-jar="'+ response.name +'"><span class="glyphicon glyphicon-trash" aria-hidden="true"></span></a></td>' +
                    '</tr>';

            jarsTable.prepend(output);
        });

        uploadJarInput.on('filebatchuploaderror', function(event, data) {
            Self.notify('Could not upload jar');
            Self.enableScreen();
            uploadJarModal.modal('hide');
        });

        uploadJarModal.find('.btn-save').on('click', function () {
            if($.trim(uploadJarInput.val())=='') {
                uploadJarModal.find('.file-caption').focus();
                return false;
            }
            Self.lockScreen();
            uploadJarInput.fileinput('upload');
        });

        uploadJarModal.on('hidden.bs.modal', function () {
            uploadJarInput.fileinput('reset');
        });

        uploadJarModal.on('show.bs.modal', function () {
            uploadJarInput.fileinput('enable');
        });

        // end jars tab

        navTabs.find('.active a').click();
        Self.refreshJobs();
    },

    Self.getAllContexts = function() {
        return $.ajax({
            url: Self.params.url + "contexts",
            type: "get"
        });
    },

    Self.deleteContext = function(ctxName) {
        return $.ajax({
            url: Self.params.url + "contexts/"+ ctxName,
            type: "delete"
        });
    },

    Self.addContext = function() {
        var output = 'jars="';
            output += generatedContext.jars.join();
            output +='"';
            for(var i = 0; i < generatedContext.params.length; i++) {
                output += '\n' + generatedContext.params[i];
            }


        return $.ajax({
            url: Self.params.url + "contexts/" + generatedContext.name,
            type: "post",
            processData: false,
            data: output
        });
    },

    Self.getAllJars = function() {
        return $.ajax({
            url: Self.params.url + "jars",
            type: "get"
        });
    },



    Self.getAllJobs = function() {
        return $.ajax({
            url: Self.params.url + "jobs",
            type: "get"
        });
    },


    Self.deleteJar = function(jarName) {
        return $.ajax({
            url: Self.params.url + "jars/"+ jarName,
            type: "delete"
        });
    },

    Self.runJob = function() {
        var output = '';
            for(var i = 0; i < runJob.params.length; i++) {
                output +=  runJob.params[i] + '\n';
            }


        return $.ajax({
            url: Self.params.url + "jobs?runningClass=" + runJob.klass + "&contextName=" + runJob.context,
            type: "post",
            processData: false,
            data: output
        });
    },

    Self.convertTimestamp = function(timestamp) {
      var d = new Date(timestamp),   // Convert the passed timestamp to milliseconds
            yyyy = d.getFullYear(),
            mm = ('0' + (d.getMonth() + 1)).slice(-2),  // Months are zero based. Add leading 0.
            dd = ('0' + d.getDate()).slice(-2),         // Add leading 0.
            hh = d.getHours(),
            h = hh,
            min = ('0' + d.getMinutes()).slice(-2),     // Add leading 0.
            time;

        // ie: 2013-02-18, 8:35
        time = yyyy + '-' + mm + '-' + dd + ', ' + h + ':' + min + ' ';

        return time;
    },

    Self.lockScreen = function() {
        ajaxLoader.show().spin();
    },

    Self.enableScreen = function() {
        ajaxLoader.spin(false).hide();
    },

    Self.notify = function(msg) {
        $.notify(
            {message: msg},
            {
                type: 'danger',
                delay: 3000,
                mouse_over: 'pause',
                animate: {
                    enter: 'animated fadeInDown',
                    exit: 'animated fadeOutUp'
                }
            }
        );
    },

    Self.notifyInfo = function(msg) {
        $.notify(
            {message: msg},
            {
                type: 'info',
                delay: 3000,
                animate: {
                    enter: 'animated fadeInDown',
                    exit: 'animated fadeOutUp'
                }
            }
        );
    },

    Self.notifySuccess = function(msg) {
            $.notify(
                {message: msg},
                {
                    type: 'success',
                    delay: 3000,
                    animate: {
                        enter: 'animated fadeInDown',
                        exit: 'animated fadeOutUp'
                    }
                }
            );
    },

    Self.computeJarSize = function(bytes) {
        var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
        if (bytes == 0) return 'n/a';
        var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
        if (i == 0) return bytes + ' ' + sizes[i];
        return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + sizes[i];
    },

    Self.refreshJobs = function() {
        var jobsLk = navTabs.find('a[aria-controls="jobs"]');
        var checkJobsTab = function () {
            if(jobsLk.closest('.active').length) {
                jobsLk.click();
            }
        };
        var t = setInterval(checkJobsTab,3000);
    };
};

$(function() {
    var sparkJob;
    try {
        sparkJob = new sparkJobTemplate();
        sparkJob.init({
            url: $(location).attr('origin') + '/',
            host: $(location).attr('protocol') + '//' + $(location).attr('hostname')
        });
    } catch (e) {
        errorHandler(e.message);
    };
});





