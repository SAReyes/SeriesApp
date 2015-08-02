var myApp = angular.module("app", ['ionic','ngWebSocket']);

myApp.config(function ($stateProvider, $urlRouterProvider, $ionicConfigProvider) {

    //$ionicConfigProvider.tabs.position('bottom');

    $stateProvider

        // setup an abstract state for the tabs directive
        .state('tab', {
            abstract: true,
            templateUrl: "assets/templates/tabs.html"
        })

        // Each tab has its own nav history stack:

        .state('tab.files', {
            url: '/files',
            views: {
                'tab-files': {
                    templateUrl: 'assets/modelViews/fileManager/fm_template.html',
                    controller: 'FileMngCtrl'
                }
            },
            onEnter: function (){
                console.log('going into files')
            }
        })

        .state('tab.files-deep', {
            url: '/files/*dir',
            views: {
                'tab-files': {
                    templateUrl: 'assets/modelViews/fileManager/fm_template.html',
                    controller: 'FileMngCtrl'
                }
            },
            onEnter: function (){
                console.log('going into files, deeper ')
            }
        })

        .state('tab.player', {
            url: '/player?title',
            views: {
                'tab-player': {
                    templateUrl: 'assets/modelViews/player/p_template.html',
                    controller: 'PlayerCtrl'
                }
            }
        })
        .state('tab.settings', {
            url: '/settings',
            views: {
                'tab-settings': {
                    templateUrl: 'assets/templates/settings.html',
                    controller: 'SettingsCtrl'
                }
            }
        });
    //.state('tab.chat-detail', {
    //    url: '/chats/:chatId',
    //    views: {
    //        'tab-chats': {
    //            templateUrl: 'assets/templates/chat-detail.html',
    //            controller: 'ChatDetailCtrl'
    //        }
    //    }
    //})
    //


    // if none of the above states are matched, use this as the fallback
    $urlRouterProvider.otherwise('/files');

});