echo "Stopping all containers"
docker stop $(docker ps -aq)
echo "Removing all containers"
docker rm $(docker ps -aq)
