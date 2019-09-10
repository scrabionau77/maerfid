/**
 * MAE CAEN PLUGIN
 * 
 * Copyright (c) 2019, Daniele Pellerucci @ Maestrale Information Technology
 * www.maestrale.it
 */

var maerfid = {
    requestPermission: function (opts, successCallback, errorCallback) {
        if (typeof opts === 'function') {  //user did not pass opts
            errorCallback = successCallback;
            successCallback = opts;
            opts = {};
        }
        if (errorCallback == null) {
            errorCallback = function () {
            };
        }
    
        if (typeof errorCallback != "function") {
            console.log("MaeRfid.requestPermission failure: error callback parameter not a function");
            return;
        }
    
        if (typeof successCallback != "function") {
            console.log("MaeRfid.requestPermission failure: success callback parameter must be a function");
            return;
        }
    
        cordova.exec(successCallback, errorCallback, 'MaeRfid', 'requestPermission', [{'opts': opts}]);
    }
}


module.exports = maerfid;