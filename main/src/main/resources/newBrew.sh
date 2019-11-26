#!/usr/bin/env bash
echo "Creating new brew"
echo "Enter brew name (no spaces) e.g SIPA"
read name
#echo "Enter iot write key"
#read iotKey

#now date in format yyyy-mm
d=`date +%Y-%m`
folderName="$d-$name"
#now date in format yyyy-MM-ddTHH:mm:ss
n=`date +%FT%T`

echo $n
echo $folderName

# create folder name
mkdir ~/$folderName

#udpdate and build
cd ~/raspyTemp/main
git pull
mvn clean install

#copy jar and config file
cp target/raspyTemp-1-jar-with-dependencies.jar  ~/$folderName
cp ~/raspyTemp/main/src/main/resources/config/app.properties ~/$folderName
cp ~/raspyTemp/main/src/main/resources/config/start.sh ~/$folderName
cp ~/raspyTemp/main/src/main/resources/log4j2.xml ~/$folderName
cp -R ~/raspyTemp/main/src/main/resources/webPkg ~/$folderName

cd ~/$folderName

#configuring properties with forlder name
sed -i s/XXX1/$n/ ~/$folderName/app.properties
sed -i s/XXX2/$folderName/ ~/$folderName/app.properties
sed -i s/XXX/$folderName/ ~/$folderName/webPkg/script.js

#dateNow=`date +%FT%T`
#nextMonth=`date +%FT%T --date='+1 month'`
#echo "$dateNow;$nextMonth;17" >> ~/$folderName/temperatureSettings

cp ~/ftp.properties ~/$folderName/ftp.properties
#sed -i s/XXX/$iotKey/ ~/$folderName/ftp.properties

chmod +x ~/$folderName/start.sh

# sym link for folder name in current
ln -sfrn ~/$folderName ~/current

echo please start using "sudo ./start.sh & >/dev/null"
#./start.sh & >/dev/null
