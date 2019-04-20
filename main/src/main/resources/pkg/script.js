var fileName="TEST.txt";
google.charts.load('current', {'packages':['annotatedtimeline', 'gauge']});
google.charts.setOnLoadCallback(drawChart);
var chart;
function drawChart() {
    // TIME SERIES CHART
    var data = new google.visualization.DataTable();
    data.addColumn('date', 'Date');
    data.addColumn('number', 'Room');
    data.addColumn('number', 'Wort');
    data.addColumn('number', 'Set Temperature');
 //   data.addColumn('number', 'Set Temperature up');
 //   data.addColumn('number', 'Set Temperature low');

    var d = processData(loadFile(fileName));
    data.addRows(
        d
    );

    chart = new google.visualization.AnnotatedTimeLine(document.getElementById('chart_div'));
    chart.draw(data, {displayAnnotations: true, dateFormat : 'dd MM yyyy HH:mm:ss'});
    google.visualization.events.addListener(chart, 'rangechange', selectHandler);

    function selectHandler(e) {
        var startDate = e['start'];
        var endDate = e['end'];

        calculateDetails(startDate, endDate);
    }

    function calculateDetails(startDate, endDate) {

        document.getElementById('start_div').innerText = startDate.toLocaleString();
        document.getElementById('end_div').innerText = endDate.toLocaleString();

        var sd = Date.parse(startDate);
        var ed = Date.parse(endDate);

        var sum = 0;
        var ele = 0;
        var min = 100;
        var max = 0;

        var sumWort = 0;
        var eleWort = 0;
        var minWort = 100;
        var maxWort = 0;

        for( var i = 0; i < d.length; i++ ){
            var date = Date.parse(d[i][0]);

            if(date > sd && date <ed) {
                var chamberValue = parseFloat(d[i][1]);
                var wortValue = parseFloat(d[i][2]);

                sum += chamberValue;
                ele = ele +1;

                if(chamberValue < min){
                    min = chamberValue;
                }
                if(chamberValue > max){
                    max = chamberValue;
                }

                //Wort
                sumWort += wortValue;
                eleWort = eleWort +1;

                if(wortValue < minWort){
                    minWort = wortValue;
                }
                if(wortValue > maxWort){
                    maxWort = wortValue;
                }
            }
        }

        var avg = sum/ele;
        var avgWort = sumWort/eleWort;
        document.getElementById('avg_chamber_div').innerText = avg.toFixed(2);
        document.getElementById('min_chamber_div').innerText = min.toFixed(2);
        document.getElementById('max_chamber_div').innerText = max.toFixed(2);

        document.getElementById('avg_wort_div').innerText = avgWort.toFixed(2);
        document.getElementById('min_wort_div').innerText = minWort.toFixed(2);
        document.getElementById('max_wort_div').innerText = maxWort.toFixed(2);

    }

    window.onload = function () { calculateDetails(param.startDate, param.endDate) }

    // GAUGE CHART

    var gaugeData = google.visualization.arrayToDataTable([
        ['Label', 'Value'],
        ['Room', lastTarr[1]],
        ['Wort', lastTarr[2]]
    ]);

    var options = {
        width: 600, height: 200,
        redFrom: 25, redTo: 30,
        yellowFrom:20, yellowTo: 25,
        minorTicks: 5,
        min: -5, max: 30
    };

    var gaugeChart = new google.visualization.Gauge(document.getElementById('gauge_div'));

    gaugeChart.draw(gaugeData, options);

}
function hideChamber() {
    chart.hideDataColumns(0);
}
function showChamber() {
    chart.showDataColumns(0);
}

function hideFermenter() {
    chart.hideDataColumns(1);
}
function showFermenter() {
    chart.showDataColumns(1);
}

function hideSettings() {
    chart.hideDataColumns(2);
}
function showSettings() {
    chart.showDataColumns(2);
}

function diff_data(d) {
    let date_diff = date_minus_now(d);
    let date_diff_as_date = new Date(date_diff);
    return  (date_diff_as_date.getDate() -1) + " Days " +(date_diff_as_date.getHours() - 1) + " Hours " + date_diff_as_date.getMinutes() + " Minutes " + date_diff_as_date.getSeconds() + " Seconds";
}

function diff_data_days(d) {
    let date_diff = date_minus_now(d);
    let date_diff_as_date = new Date(date_diff);
    return date_diff_as_date.getDate() -1;
}

function date_minus_now(d) {
    return new Date() - d;
}

function loadFile(filePath) {
    var result = null;
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.open("GET", filePath, false);
    xmlhttp.send();
    if (xmlhttp.status==200) {
        result = xmlhttp.responseText;
    }
    return result;
}

var lastTarr = [0, 0, 0];
var firstDate = null;
function processData(allText) {
    var allTextLines = allText.split(/\r\n|\n/);
    let dateLastUp = allTextLines[0];
    let dateLastAsDate = new Date(allTextLines[0]);

    document.getElementById('date_div').innerText = "Last update: " +dateLastUp;
    document.getElementById('date_div2').innerText = "Time since last update: "+ diff_data(dateLastAsDate);

    if(date_minus_now(dateLastAsDate) < 10*60*1000) {
        document.getElementById("dot").className = "greenDot";
    }else{
        document.getElementById("dot").className = "redDot";
    }

    var lines = [];

    for (var i=1; i<allTextLines.length; i++) {

        var data = allTextLines[i].split('|');
        if (data.length >=4) {

            var tarr = [];
            var d = new Date(data[0]);
            tarr.push(d);
            tarr.push(Number(data[1]));
            tarr.push(Number(data[2]));
            tarr.push(Number(data[3]));

//          tarr.push(Number(get_settings(d) - 0.3));
//          tarr.push(Number(get_settings(d) + 0.3));

            lastTarr = tarr;
            lines.push(tarr);

            if(firstDate == null){
                firstDate = d;
                document.getElementById('info_age').innerText= "Brew Day "+ diff_data_days(firstDate);
            }
        }
    }
    return lines;
}
