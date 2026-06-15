# Let's Encrypt webroot 검증 (HTTP-01)
location ^~ /.well-known/acme-challenge/ {
    root /var/www/certbot;
    default_type "text/plain";   # 선택: 확장자 없는 토큰 파일 대응
}

