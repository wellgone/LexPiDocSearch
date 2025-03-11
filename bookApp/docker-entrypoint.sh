#!/bin/sh

# 替换环境变量
envsubst '${VITE_API_URL} ${VITE_ES_URL} ${VITE_ES_USERNAME} ${VITE_ES_PASSWORD}' < /usr/share/nginx/html/assets/*.js > /usr/share/nginx/html/assets/temp.js
mv /usr/share/nginx/html/assets/temp.js /usr/share/nginx/html/assets/*.js

# 执行CMD
exec "$@" 