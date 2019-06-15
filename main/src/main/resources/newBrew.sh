#!/usr/bin/env bash
echo "Creating new brew"
echo "Enter brew name (no spaces)"
read name
echo "Enter iot write key"
read iotKey

d=`date +%Y-%m`
folderName="$d-$name"
n=`date +%FT%T`

echo $n
echo $folderName

mkdir ~/$folderName
cd ~/raspyTemp/main
git pull
mvn clean install

cp target/raspyTemp-1-jar-with-dependencies.jar  ~/$folderName
cp ~/raspyTemp/main/src/main/resources/config/app.properties ~/$folderName
cp ~/raspyTemp/main/src/main/resources/config/start.sh ~/$folderName
cp ~/raspyTemp/main/src/main/resources/log4j2.xml ~/$folderName

cd ~/$folderName

sed -i s/XXX1/$n/ ~/$folderName/app.properties
sed -i s/XXX2/$folderName/ ~/$folderName/app.properties

dateNow=`date +%FT%T`
nextMonth=`date +%FT%T --date='+1 month'`
echo "$dateNow;$nextMonth;17" >> ~/$folderName/temperatureSettings

cp ~/ftp.properties ~/$folderName/ftp.properties
sed -i s/XXX/$iotKey/ ~/$folderName/ftp.properties

chmod +x ~/$folderName/start.sh

ln -sf ~/$folderName ~/current

echo start
