myApp.service('FilesService', function ($q, $http, $websocket) {
    return ({
        getFiles: getFiles,
        open: open,
        connectWS: connect
    });

    function getFiles(dir) {
        var deferred = $q.defer();

        console.log('Get files for: ' + decodeURIComponent(dir));
        dir = dir === undefined ? '' : encodeURIComponent(decodeURIComponent(dir));

        $http.get('api/files/' + dir)
            .success(function (data) {
                console.log('Server response: ');
                console.log(data);
                deferred.resolve(data);
            })
            .error(function (error) {
                console.log('Server error: ' + error);
                deferred.reject('can\'t locate the server')
            });

        return deferred.promise;
    }

    function open(file) {
        var deferred = $q.defer();

        console.log('Opening: ' + decodeURIComponent(file));

        $http.get('api/files/' + encodeURIComponent(decodeURIComponent(file)))
            .success(function () {
                deferred.resolve();
            })
            .error(function () {
                deferred.reject('can\'t locate the server')
            });

        return deferred.promise;
    }

    function connect(){
        return $websocket('ws://localhost:9000/api/filesWS');
    }
});