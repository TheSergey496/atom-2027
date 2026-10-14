#!/bin/bash

echo "{\"baseUrl\":\"$baseUrl\"}" > /usr/share/nginx/html/browser/assets/config.json
/docker-entrypoint.sh
nginx -g "daemon off;"
