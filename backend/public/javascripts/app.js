var myApp = angular.module("app", ['ionic']);

myApp.config(function ($stateProvider, $urlRouterProvider) {

    // Ionic uses AngularUI Router which uses the concept of states
    // Learn more here: https://github.com/angular-ui/ui-router
    // Set up the various states which the app can be in.
    // Each state's controller can be found in controllers.js
    $stateProvider

        // setup an abstract state for the tabs directive
        .state('tab', {
            url: "/tab",
            abstract: true,
            templateUrl: "assets/templates/tabs.html"
        })

        // Each tab has its own nav history stack:

        .state('tab.files', {
            url: '/files',
            views: {
                'tab-files': {
                    templateUrl: 'assets/templates/files.html',
                    controller: 'FileMngCtrl'
                }
            }
        })

        .state('tab.player', {
            url: '/player',
            views: {
                'tab-player': {
                    templateUrl: 'assets/templates/player.html',
                    controller: 'PlayerCtrl'
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
        //.state('tab.account', {
        //    url: '/account',
        //    views: {
        //        'tab-account': {
        //            templateUrl: 'assets/templates/tab-account.html',
        //            controller: 'AccountCtrl'
        //        }
        //    }
        //});

    // if none of the above states are matched, use this as the fallback
    $urlRouterProvider.otherwise('/tab/files');

});