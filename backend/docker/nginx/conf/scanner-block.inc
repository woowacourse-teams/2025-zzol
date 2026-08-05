# 스캐너 경로 차단 (dev.conf·prod.conf에서 include)
#
# 이 호스트의 정상 경로에는 점(.)이 없다 — REST 엔드포인트도, Thymeleaf 관리자 페이지도 확장자를 쓰지 않는다.
# 점이 들어간 요청(/.env, /.git/config, /wp-admin/*.php, /backup.sql, /config.json …)은 전부 스캐너다.
# 확장자 블랙리스트 대신 이 한 줄로 잡는 이유: 블랙리스트는 늘 새 확장자에 뚫린다.
#
# 444 = 응답 없이 연결 종료. 앱까지 가지 않으므로 IpBlockFilter의 403 응답·WARN 로그 비용도 사라진다.
# 앱의 MaliciousPathMatcher는 그대로 둔다(다층 방어 — nginx를 우회한 경로용).
#
# 점을 쓰는 정상 경로 예외는 ^~ 프리픽스 location이 정규식보다 우선하므로 아래·acme-challenge.inc가 선점한다:
#   /.well-known/acme-challenge/  (acme-challenge.inc)
#   /css/*.css                    (관리자 UI, 아래)
#   /ws/**                        (SockJS iframe.html 등, dev.conf·prod.conf의 location ^~ /ws)

location ^~ /css/ {
    proxy_pass $upstream;
    include /etc/nginx/conf.d/proxy-http.inc;
}

location ~ "\." {
    return 444;
}
