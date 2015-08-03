myApp.controller("PlayerCtrl", ["$scope", "$http", function ($scope, $http){
    $scope.player = { volume: 75, playing: true};

    $scope.setVolume = function(volume) {
        $scope.player.volume = volume;
        $scope.volumeChanged();
    };

    $scope.volumeChanged = function(){
        $http.put('/playing/volume', {volume: parseInt($scope.player.volume)});
    };

    $scope.playPause = function(){
        $http.get('/playing/' + ($scope.player.playing ? 'pause' : 'play')).
            success(function(){
                $scope.player.playing = !$scope.player.playing
            });
    };
}]);