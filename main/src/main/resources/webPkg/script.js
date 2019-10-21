let fileName="TEST.txt";
let chart;
const unknown = 0;
const fermenting = 1;
const cooling = 2;
const warming = 3;
let lastDataForGauge = [0, 0];

google.charts.load('current', {'packages':['annotationchart', 'gauge']});
google.charts.setOnLoadCallback(drawChart);


function drawChart() {
    var data = new google.visualization.DataTable();
    data.addColumn('date', 'Date');
    data.addColumn('number', 'Room');
    data.addColumn('number', 'Wort');
    data.addColumn('number', 'Set Temperature');
    data.addColumn('string', 'Activation');

    //   data.addColumn('number', 'Set Temperature up');
    //   data.addColumn('number', 'Set Temperature low');

    var dataFromSelect = document.getElementById("dataFromSelect");
    var d;
    if (dataFromSelect.value === "Local"){
        d = processData(loadFile(fileName));
    } else{
        d = processData2(getIotData())
    }
    data.addRows(d);

    chart = new google.visualization.AnnotationChart(document.getElementById('chart_div'));
    chart.draw(data, {displayAnnotations: true, dateFormat : 'dd MM yyyy HH:mm:ss', allowHtml : true, displayAnnotationsFilter: true});
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
        ['Room', lastDataForGauge[0]],
        ['Wort', lastDataForGauge[1]]
    ]);

    var options = {
        width: 600, height: 200,
        redFrom: 21, redTo: 25,
        yellowFrom:19, yellowTo: 21,
        minorTicks: 5,
        min: -5, max: 25
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
    xmlhttp.open("GET", filePath +"?r=" + Math.random(), false);
    xmlhttp.setRequestHeader('cache-control', 'no-cache, must-revalidate, post-check=0, pre-check=0');
    xmlhttp.setRequestHeader('cache-control', 'max-age=0');
    xmlhttp.setRequestHeader('expires', '0');
    xmlhttp.setRequestHeader('expires', 'Tue, 01 Jan 1980 1:00:00 GMT');
    xmlhttp.setRequestHeader('pragma', 'no-cache');
    xmlhttp.send();
    if (xmlhttp.status==200) {
        result = xmlhttp.responseText;
    }
    return result;
}

function getIotData() {
    var result = null;
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.open("GET", "https://api.thingspeak.com/channels/798869/feeds.csv?api_key=20ZB1E6DWROCDPT7&results=8000", false);
    xmlhttp.send();
    if (xmlhttp.status==200) {
        result = xmlhttp.responseText;
    }
    return result;
}

function processData(allText) {
    var allTextLines = allText.split(/\r\n|\n/);
    let dateLastAsDate = new Date(allTextLines[0]);
    let dateFirstAsDate = new Date(allTextLines[1].split("|")[0]);

    lastUpdate(dateLastAsDate);
    greenRedDot(dateLastAsDate);
    brewDay(dateFirstAsDate);

    var lines = [];
    var previousActivationValue = 0;

    for (var i=1; i<allTextLines.length; i++) {
        var data = allTextLines[i].split('|');
        if (data.length >=4) { // date, room, wort, settings, activator

            var line = [];

            let dv = new Date(data[0]);
            line.push(dv); // data

            let roomValue = Number(data[1]);
            line.push(roomValue); // room

            let wortValue = Number(data[2]);
            line.push(wortValue); //wort

            line.push(Number(data[3])); // setting

            previousActivationValue = annotations(data, previousActivationValue, line);

//          line.push(Number(get_settings(d) - 0.3));
//          line.push(Number(get_settings(d) + 0.3));

            lastDataForGauge[0] = roomValue;
            lastDataForGauge[1] = wortValue;
            lines.push(line);
        }
    }
    //array : data, room, wort, settings
    return lines;
}

function processData2(allText) {
    var allTextLines = allText.split(/\r\n|\n/);
    var dateLastAsDate = null;
    var dateFirstAsDate = null;

    var lines = [];
    var previousActivationValue = 0;

    for (var i=1; i<allTextLines.length; i++) {
        var data = allTextLines[i].split(',');
        if (data.length >=4) { // date, room, wort, settings, activator

            var line = [];

            let dv = new Date(data[0]);
            if(dateFirstAsDate == null){
                dateFirstAsDate = dv; //remember the first date
            }
            dateLastAsDate= dv; //remember the last date

            line.push(dv); // data

            let roomValue = Number(data[2]);
            line.push(roomValue); // room

            let wortValue = Number(data[3]);
            line.push(wortValue); //wort

            line.push(Number(2)); // setting
            line.push(null);
           ////// previousActivationValue = annotations(data, previousActivationValue, line);

//          line.push(Number(get_settings(d) - 0.3));
//          line.push(Number(get_settings(d) + 0.3));

            lastDataForGauge[0] = roomValue;
            lastDataForGauge[1] = wortValue;
            lines.push(line);
        }
    }

    lastUpdate(dateLastAsDate);
    greenRedDot(dateLastAsDate);
    brewDay(dateFirstAsDate);

    //array : data, room, wort, settings
    return lines;
}

function lastUpdate(dateLastAsDate) {
    document.getElementById('date_div').innerText = "Last update: " + dateLastAsDate.toLocaleString();
    document.getElementById('date_div2').innerText = "Time since last update: " + diff_data(dateLastAsDate);
    document.getElementById('date_div_now').innerText = "Now: " +  new Date().toLocaleString();
}

function greenRedDot(dateLastAsDate) {
    if (date_minus_now(dateLastAsDate) < 10 * 60 * 1000) {
        document.getElementById("dot").className = "greenDot";
    } else {
        document.getElementById("dot").className = "redDot";
    }
}

function brewDay(dateLastAsDate) {
    document.getElementById('info_age').innerText = "Brew Day " + diff_data_days(dateLastAsDate);
}

function annotations(data, previousActivationValue, line) {
    if (data.length === 5) {
        let activator = parseInt(data[4]);
        if (previousActivationValue !== activator) {
            line.push(decode(previousActivationValue, activator)); // annotation
            previousActivationValue = activator;
        } else {
            line.push(null);
        }
    } else {
        line.push(null);
    }
    return previousActivationValue;
}

function decode(previous, current) {
    if (current === unknown) {
        return "unknown";
    }
    if (current === fermenting) {
        if (previous === cooling) {
            return "stop fridge";
        } else {
            return "stop belt";
        }
    }
    if (current === cooling) {
        return "<font color='#3366CC'>start fridge</font>";
    }
    if (current === warming) {
        return "<font color='#DC3912'>start belt</font>";
    }
}

