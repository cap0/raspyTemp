window.onload = function () {
    document.getElementById('info_brewName').innerText = 'Beer Title';
    document.getElementById('info_brewDayDate').innerText = '01-01-2019';
    document.getElementById('info_chamberFan').innerText = false;
    document.getElementById('info_chamberColdSettings').innerText = 1;
    document.getElementById('info_hotWire').innerText = 'belt';
    document.getElementById('info_stcTempDiff').innerText = '';
    document.getElementById('info_stcColdLag').innerText = '';
    document.getElementById('info_notes').innerText = '';
    document.getElementById('date_div_now').innerText = "Now: " +  new Date().toISOString();
};

opt =[{
    'd': "2018-12-27T00:00",
    'v': 18
}];