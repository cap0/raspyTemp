window.onload = function () {
    document.getElementById('info_brewName').innerText = 'American Wheat';
    document.getElementById('info_brewDayDate').innerText = '24-02-2019';
    document.getElementById('info_chamberFan').innerText = false;
    document.getElementById('info_chamberColdSettings').innerText = 1;
    document.getElementById('info_hotWire').innerText = 'belt';
    document.getElementById('info_stcTempDiff').innerText = '0.5';
    document.getElementById('info_stcColdLag').innerText = '3';
    document.getElementById('info_notes').innerText = '';
    document.getElementById('date_div_now').innerText = "Now: " +  new Date().toISOString();
};

opt =[{
    'd': "2019-02-21T00:00",
    'v': 18
},{
    'd': "2019-02-26T23:00",
    'v': 19
},{
    'd': "2019-03-01T20:00",
    'v': 20
}
];
