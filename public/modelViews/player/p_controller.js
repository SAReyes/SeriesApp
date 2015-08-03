myApp.controller("PlayerCtrl", function ($scope, $http, $stateParams) {

    $scope.navTitle = $stateParams.title === undefined ? 'Player' : $stateParams.title;
    $scope.player = {volume: 75, playing: true};

    console.log($scope.navTitle);

    $scope.setVolume = function (volume) {
        $scope.player.volume = volume;
        $scope.volumeChanged();
    };

    $scope.volumeChanged = function () {
        $http.put('/playing/volume', {volume: parseInt($scope.player.volume)});
    };

    $scope.playPause = function () {
        $http.get('/playing/' + ($scope.player.playing ? 'pause' : 'play')).
            success(function () {
                $scope.player.playing = !$scope.player.playing
            });
    };
});