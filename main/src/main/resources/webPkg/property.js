window.onload = function () {
    document.getElementById('info_brewName').innerText = 'Beer Title';
    document.getElementById('info_brewDayDate').innerText = '01-01-2019';
    document.getElementById('date_div_now').innerText = "Now: " +  new Date().toLocaleString();
};