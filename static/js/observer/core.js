var socket, refreshIntervalId;
var ready = false;

function errorToString(error) {
    if (error.code == 1006) {
        return "Connection to server lost!";
    } else {
        return "Error[" + error.code + "]: " + error.message;
    }
}

function showError(error) {
    notify("danger", '<b>Observer Error</b>: ' + errorToString(error))
}

function notify(type, message) {
    $.notify({
        icon: "notifications",
        message: message
    }, {
        type: type,
        timer: 3000
    });
}

function checkUpdates() {
    if (ready) {
        if (typeof getUpdates === "function") {
            getUpdates();
        }
    }
}

function handleMessage(event) {
    var data = JSON.parse(event.data);

    if (data.notify) {
        notify((data.success) ? "success" : "danger", data.message);
    }

    if (typeof setUpdates === "function") {
        setUpdates(data);
    }
}

function send(data) {
    if (ready) {
        socket.send(JSON.stringify(data));
    }
}

function init(ws_path) {
    socket = new WebSocket(ws_path);

    socket.onmessage = handleMessage;

    socket.onopen = function() {
        console.log("Connection to Observer established");
        ready = true;
    };

    socket.onclose = function(event){
        ready = false;
        showError(event);

        clearInterval(refreshIntervalId);

        setTimeout(function(){
            init(ws_path);
        }, 7000);
    };

    socket.onerror = function(error) {
        showError(error);
    };

    refreshIntervalId = setInterval(checkUpdates, 2000);
}