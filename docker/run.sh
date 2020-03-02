docker kill $(docker ps -q)
#docker rm $(docker ps -a -q)
docker rmi -f $(docker images -q)

cd ..
gradle clean fatJar

cd docker
docker build -t mailguard .
docker run -p 80:80 -p 25:25 mailguard:latest
