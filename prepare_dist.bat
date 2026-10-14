docker-compose -f docker-compose.prod.yml build
docker save localhost:5000/backend localhost:5000/frontend > images.tar
move /Y images.tar install/images.tar
