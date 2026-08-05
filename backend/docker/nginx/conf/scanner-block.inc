# 스캐너 경로 차단 (dev.conf·prod.conf에서 include)
#
# 444 = 응답 없이 연결 종료. 앱까지 가지 않으므로 IpBlockFilter의 403 응답·WARN 로그 비용이 사라진다.
#
# ── 왜 "점(.)이 있으면 차단"이 아닌가 ────────────────────────────────────────
# 정적 경로에는 점이 없지만 **경로 파라미터에는 있다.** 실제로 세 군데가 깨진다:
#   /admin/ip-blocks/1.2.3.4/unblock   IPv4 (postmortem 0003의 차단 해제 수단)
#   /rooms/{code}/players/{nickname}   PlayerName은 문자 집합을 제한하지 않는다
#   /swagger-ui/swagger-ui.css         dev 프로필의 springdoc 정적 자산
# 점 전체를 막으면 "앞으로 경로에 점을 넣지 마라"는 제약이 API 설계 전체에 생기고,
# 어기면 조용히 444가 된다. 그래서 스캐너가 실제로 노리는 두 가지만 막는다.
#
# 실패 방향도 이쪽이 맞다. 목록에 없는 확장자는 앱까지 가지만 거기서 IpBlockFilter가
# 받는다 — 뚫리는 게 아니라 원래 위치로 돌아갈 뿐이다. 반대 방향(정상 경로 444)은
# 조용히 깨지고 아무도 모른다.
#
# ── 정상 경로 예외 ──────────────────────────────────────────────────────────
# ^~ 프리픽스는 정규식보다 우선하므로 아래 두 블록과 acme-challenge.inc가 선점한다.
# 둘 다 정적 자산 루트라 경로가 고정이다(경로 파라미터가 없어 늘어날 일이 없다).

# 관리자 UI 스타일시트
location ^~ /css/ {
    proxy_pass $upstream;
    include /etc/nginx/conf.d/proxy-http.inc;
}

# springdoc Swagger UI. dev 전용이지만(application-dev.yml) prod에서도 include한다 —
# prod는 springdoc이 꺼져 있어 앱이 404를 주므로 무해하고, 환경별로 갈라두면 한쪽만 고치게 된다.
location ^~ /swagger-ui {
    proxy_pass $upstream;
    include /etc/nginx/conf.d/proxy-http.inc;
}

# ── 차단 규칙 ───────────────────────────────────────────────────────────────

# 1) 닷파일·닷디렉터리 — 세그먼트가 점으로 시작한다.
#    /.env, /.git/config, /publics/.git/config, /api/.env.save, /.aws/credentials, /.bash_history
#    정상 경로 파라미터(IPv4·닉네임)는 점으로 **시작**하지 않으므로 걸리지 않는다.
#    /.well-known/acme-challenge/ 는 acme-challenge.inc의 ^~ 가 선점한다.
location ~ "(^|/)\." {
    return 444;
}

# 2) 파일 확장자 — REST 엔드포인트도 Thymeleaf 관리자 페이지도 확장자를 쓰지 않는다.
#    끝의 ($|[^a-z0-9]) 는 `.php.old`·`.php~`·`.php'` 처럼 뒤에 뭐가 붙은 변형까지 잡되
#    `.phpx` 같은 무관한 문자열은 건드리지 않는다(nginx가 %2e 를 먼저 디코딩하므로
#    로그의 /aws%2ephp%2eold 도 여기서 걸린다).
location ~* "\.(php\d?|phtml|aspx?|jsp|cgi|pl|py|rb|sh|bash|js|ts|css|map|env|ini|conf|cfg|config|properties|toml|ya?ml|json|xml|sql|db|sqlite|dump|mongodump|mysql|pgsql|bak|backup|old|orig|save|swp|tmp|temp|log|zip|gz|tgz|bz2|xz|rar|7z|tar|pem|key|crt|cer|p12|pfx|jks|keystore|tfstate|htpasswd|htaccess|inc)($|[^a-z0-9])" {
    return 444;
}

# 알려진 확장자 목록이라 새 확장자에는 뚫린다 — 의도한 것이다(위 "실패 방향" 참조).
# 앱의 MaliciousPathMatcher는 그대로 둔다: nginx를 통과한 요청과 내부 리스너를 받는 뒷단이다.
