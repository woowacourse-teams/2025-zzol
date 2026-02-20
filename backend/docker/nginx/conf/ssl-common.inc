ubuntu@zzol-vnic:~/nginx/conf$ cat ssl-common.inc
# 프로토콜
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers HIGH:!aNULL:!MD5;
ssl_prefer_server_ciphers on;

# 성능
ssl_session_cache shared: SSL:10m;
ssl_session_timeout 10m;
