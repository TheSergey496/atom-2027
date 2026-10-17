docker-compose -f docker-compose.prod.yml build
docker save localhost:5000/backend localhost:5000/frontend postgres:18.6-alpine > images.tar
move /Y images.tar install/images.tar
