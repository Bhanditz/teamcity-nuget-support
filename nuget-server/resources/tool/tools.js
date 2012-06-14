/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

if (!BS) BS = {};

if (!BS.NuGet) BS.NuGet = {};

BS.NuGet.Tools = {
  installUrl : '',

  refreshPackagesList : function() {
    $('nugetPackagesList').refresh();
  },

  removeTool : function(toolId) {
    BS.ajaxRequest(this.installUrl, {
      parameters : {
        whatToDo : "remove",
        toolId : toolId
      },

      onComplete: function() {
        BS.NuGet.Tools.refreshPackagesList();
      }
    })
  },

  InstallPopup : OO.extend(BS.PluginPropertiesForm, OO.extend(BS.AbstractModalDialog, {
    getContainer : function() {
      return $('nugetInstallFormDialog');
    },

    formElement : function() {
      return $('nugetInstallForm');
    },

    disableSubmit : function() {
      $('installNuGetApplyButton').disabled = 'disabled';
    },

    refreshForm : function(fresh) {
      var that = this;
      this.disableSubmit();
      BS.Util.hide($('nugetInstallFormResresh'));
      BS.Util.show($('nugetInstallFormLoading'));
      $('nugetInstallFormResresh').refresh(null, fresh ? "fresh=1": "", function() {
        BS.Util.hide($('nugetInstallFormLoading'));
        BS.Util.show($('nugetInstallFormResresh'));
        that.showCentered();
        $('installNuGetApplyButton').disabled = '';
      });
      return false;
    },

    show : function() {
      this.showCentered();
      this.refreshForm(false);
      return false;
    },

    closeToolsDialog : function() {
      this.close();
      $('nugetInstallFormResreshInner').update("");
      BS.Util.hide($('nugetInstallFormLoading'));
      BS.Util.hide($('nugetInstallFormResresh'));
    },

    save : function() {
      BS.Util.show($('installNuGetApplyProgress'));
      BS.FormSaver.save(this, this.formElement().action, OO.extend(BS.ErrorsAwareListener, {
        onCompleteSave: function(form, responseXML, err) {
          var wereErrors = BS.XMLResponse.processErrors(responseXML, {}, form.propertiesErrorsHandler);
          BS.ErrorsAwareListener.onCompleteSave(form, responseXML, err);

          BS.Util.hide($('installNuGetApplyProgress'));
          if (!wereErrors) {
            BS.NuGet.Tools.refreshPackagesList();
            form.close();
          } else {
            BS.Util.reenableForm(form.formElement());
          }
        }
      }));
      return false;
    }
  }))
};



