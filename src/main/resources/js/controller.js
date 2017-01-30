var s3Project = angular.module('s3Project', ['angular.atmosphere']);

s3Project.controller('s3Ctrl', ['$scope', '$http', '$interval', 'atmosphereService',
    function ($scope, $http, $interval, atmosphereService) {
        var isProcessingData = false;
        var dataDisplay;
        var displayDataIndex = 0;
        $scope.generatedData = [];
        $scope.displayDataSet = [];
        $scope.tempData = [];
        $scope.status = true;
        $scope.buttonLabel = "Start generating data.";

        $scope.isDeleting = false;

        $scope.kinesisCtrl = function () {
            //$http.get("http://localhost:8080/kinesis?status=" + $scope.status);
            $http.get("http://52.202.187.66:8080/kinesis?status=" + $scope.status);

            if (!$scope.status) {
                $scope.buttonLabel = "Start generating data.";
                $interval.cancel(dataDisplay);
                //$scope.displayDataSet = [];
                isProcessingData = false;
                atmosphereService.unsubscribe();
            }
            else {
                $scope.buttonLabel = "Stop generating data.";
                atmosphereService.subscribe(request);
            }

            $scope.status = !$scope.status;
        }

        $scope.deleteStream = function () {
            //$http.get("http://localhost:8080/kinesis/deleteStream")
            $http.get("http://52.202.187.66:8080/kinesis/deleteStream")
                .then(function () {
                    alert("Delete Complete.");
                    $scope.isDeleting = false;
                });
        }

        var request = {
            //url: 'http://localhost:8080/atmosphere',
            url: 'http://52.202.187.66:8080/atmosphere',
            contentType: 'application/json',
            logLevel: 'debug',
            transport: 'websocket',
            trackMessageLength: true,
            reconnectInterval: 5000,
            enableXDR: true,
            timeout: 60000
        };

        request.onMessage = function (response) {
            var responseText = response.responseBody;
            try {
                if (responseText == 'Set Finish') {
                    $scope.generatedData = $scope.tempData;
                    $scope.tempData = [];

                    if (!isProcessingData) {
                        showData();
                    }
                }
                else {
                    var broadcastInfo = atmosphere.util.parseJSON(responseText);
                    $scope.tempData.push(broadcastInfo);
                }
            } catch (e) {
                throw e;
            }
        };

        function showData() {
            isProcessingData = true;
            //var i = 0;
            dataDisplay = $interval(function () {
//			console.log("test here.");
                $scope.displayDataSet.push($scope.generatedData[displayDataIndex]);
                displayDataIndex++;
                if (displayDataIndex > $scope.generatedData.length) {
                    $interval.cancel(dataDisplay);
                    isProcessingData = false;
                }
            }, 1000);
        }
    }]);