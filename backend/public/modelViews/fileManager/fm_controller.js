myApp.controller("FileMngCtrl", function ($scope, FilesService, $stateParams, $location, $state, $ionicListDelegate) {
    $scope.data = [];
    $scope.loading = true;

    if ($stateParams.dir === undefined) {
        $scope.path = undefined;
        $scope.title = 'File Manager';
        $scope.dir = '';
    } else {
        $scope.path = $location.path().split('/').slice(2).join('/');
        $scope.title = decodeURIComponent($scope.path).split('/').pop();
        $scope.dir = $scope.path + '/';
    }

    loadData();

    $scope.videoClicked = function (file) {
        if (file.dir) {
            $state.go('tab.files-deep', {
                dir: $scope.dir + file.name
            });
        } else {
            FilesService.open($scope.dir + file.name).then(
                function (success) {
                }
                , function (error) {
                    alert(error)
                }
            );

            $state.go('tab.player', {
                title: file.name
            })
        }
    };

    $scope.doRefresh = function() {
        console.log("Refreshing data at - " + $scope.title);
        loadData();
        $scope.$broadcast('scroll.refreshComplete');
    };

    $scope.getFileImage = function (file){
        function getType(file){
            return file.dir ? 'dir' : 'file';
        }

        return 'assets/images/icons/' + getType(file) + '_' + file.state + '.png';
    };

    $scope.updateFileState = function (file, state) {
        file.state = state;
        $ionicListDelegate.closeOptionButtons();
    };

    // the controller's code is executed on load, no need for $ionicView.loaded
    var socket = FilesService.connectWS();

    socket.onMessage(function (msg) {
        loadData();
    });

    $scope.$on('$ionicView.unloaded', function(){
        socket.close();
    });

    function loadData(){
        FilesService.getFiles($scope.path).then(
            function (data) {
                $scope.data = data;
                $scope.loading = false;
            }, function (error) {
                alert(error);
            }
        );
    }
});