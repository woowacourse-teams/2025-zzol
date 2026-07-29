#!/usr/bin/env python3
"""nginx 프록시 헤더 검사 — X-Forwarded-For 신뢰 사슬이 끊기는 것을 막는다.

배경(#1620): 앱의 내부 IP 화이트리스트(IpBlockFilter)가 X-Forwarded-For 스푸핑에 뚫리지 않는
이유는 필터 코드가 아니라 nginx + Tomcat RemoteIpValve 조합에 있다.

  1. nginx가 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for`로
     실제 TCP peer($remote_addr)를 XFF 맨 뒤에 덧붙인다 (proxy-http.inc·proxy-ws.inc).
  2. RemoteIpValve가 XFF를 오른쪽부터 훑으며 internal-proxies에 해당하는 항목만 벗겨내고
     첫 비내부 IP에서 멈춘다 → 공인 IP에서 온 요청이라면 클라이언트가 왼쪽에 주입한 값은
     그 지점을 넘지 못한다.

이 사슬은 1번이 있어야만 성립한다. XFF를 세팅하지 않는 proxy_pass가 생기면 클라이언트가 보낸
XFF가 그대로 앱에 전달되고, 항목이 전부 사설 IP면 RemoteIpValve가 끝까지 벗겨내
**공격자가 넣은 사설 IP가 getRemoteAddr()가 되어 화이트리스트를 통과**한다.

가드가 조용히 사라지는 사고는 이미 한 번 겪었다(postmortem 0003 — 병렬 PR 머지로 화이트리스트
유실). ADR-0023이 기록한 엣지 설정 회귀도 정확히 이 줄이었다(`X-Forwarded-For $remote_addr`로
덮어써 체인이 끊김). 사람 리뷰로 지키는 불변조건은 신뢰하지 않고 CI로 고정한다.

검사 규칙
  1. proxy-http.inc·proxy-ws.inc는 `X-Forwarded-For $proxy_add_x_forwarded_for`를 세팅해야 한다.
     `$remote_addr`·`$http_x_forwarded_for`로 덮어쓰면 include가 있어도 사슬이 끊긴다.
  2. proxy_pass가 있는 블록은 XFF를 세팅해야 한다 — proxy-*.inc include 또는 위 지시자 직접 선언.
     nginx는 **현재 레벨에 proxy_set_header가 하나라도 있으면 상위 레벨 값을 전부 버리므로**,
     상위 블록의 include는 하위 블록이 자체 proxy_set_header를 선언하지 않은 경우에만 인정한다.

예외 표기
  proxy_pass와 같은 줄 또는 바로 윗줄 **주석**에 `# proxy-header-exempt: <사유>`를 두면
  그 proxy_pass 하나를 건너뛴다. 사유를 반드시 적게 해서 예외가 조용히 늘어나지 않게 한다.

한계
  정규식 + 중괄호 깊이로만 파싱한다. 따옴표 리터럴은 비워서 그 안의 `#`·중괄호를 배제하지만,
  따옴표 없는 정규식 location(`location ~ ^/v\\d{1,2}/`)의 중괄호는 그대로 세므로 균형이
  맞지 않으면 블록 스코프가 어긋난다. 저장소의 설정은 모두 균형이 맞아 현재는 문제되지 않는다.
"""
import re
import sys
from pathlib import Path

PROXY_PASS = re.compile(r"(?:^|[;{])\s*proxy_pass\s")
PROXY_INCLUDE = re.compile(r"(?:^|[;{])\s*include\s+\S*proxy-(?:http|ws)\.inc\s*;")
SET_HEADER = re.compile(r"(?:^|[;{])\s*proxy_set_header\s")
XFF_CHAIN = re.compile(r"proxy_set_header\s+X-Forwarded-For\s+\$proxy_add_x_forwarded_for\s*;")
EXEMPT_MARKER = re.compile(r"#\s*proxy-header-exempt:\s*(.+)")
QUOTED = re.compile(r"\"[^\"]*\"|'[^']*'")

HEADER_INCLUDES = ("proxy-http.inc", "proxy-ws.inc")
ROOT_BLOCK = 0


def check_include_content(path):
    """규칙 1 — 프록시 헤더 include 자체가 XFF 체인을 누적하는지 확인한다."""
    if path.name not in HEADER_INCLUDES or XFF_CHAIN.search(path.read_text(encoding="utf-8")):
        return []
    return [
        f"{path} — `X-Forwarded-For $proxy_add_x_forwarded_for` 설정이 없다. "
        f"$remote_addr / $http_x_forwarded_for로 덮어쓰면 클라이언트가 보낸 XFF가 살아남아 "
        f"실제 접속 IP가 유실된다."
    ]


def check_proxy_pass(path):
    """규칙 2 — proxy_pass가 있는 블록이 XFF를 세팅하는지(상속 포함) 확인한다."""
    parent = {ROOT_BLOCK: None}  # 블록 id -> 상위 블록 id
    sets_xff = set()  # XFF를 세팅하는 블록 id
    sets_headers = set()  # proxy_set_header를 선언해 상위 상속을 끊는 블록 id
    proxy_passes = []  # (블록 id, 줄번호, 예외 사유 or None)
    stack = [ROOT_BLOCK]
    next_id = ROOT_BLOCK + 1
    previous_exempt = None

    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        # 리터럴 내용을 비운 뒤 주석을 분리한다. 따옴표 안의 #·중괄호가 주석 시작이나
        # 블록 경계로 오인되는 것을 막는다.
        line, hash_sign, comment = QUOTED.sub('""', raw).partition("#")
        marker = EXEMPT_MARKER.search(hash_sign + comment) if hash_sign else None
        exempt = marker.group(1).strip() if marker else None

        if PROXY_PASS.search(line):
            proxy_passes.append((stack[-1], lineno, exempt or previous_exempt))
        if PROXY_INCLUDE.search(line) or XFF_CHAIN.search(line):
            sets_xff.add(stack[-1])
        elif SET_HEADER.search(line):
            sets_headers.add(stack[-1])

        # 여는 중괄호는 블록 선언과 같은 줄에 온다(nginx 관례)이므로 위 검사 뒤에 처리한다
        for char in line:
            if char == "{":
                parent[next_id] = stack[-1]
                stack.append(next_id)
                next_id += 1
            elif char == "}" and len(stack) > 1:
                stack.pop()

        previous_exempt = exempt

    errors = []
    for block, lineno, exempt in proxy_passes:
        if has_xff(block, parent, sets_xff, sets_headers):
            continue
        if exempt:
            print(f"  건너뜀 {path}:{lineno} — 예외 사유: {exempt}")
            continue
        errors.append(
            f"{path}:{lineno} — proxy_pass가 있으나 X-Forwarded-For를 세팅하지 않는다. "
            f"클라이언트가 보낸 XFF가 그대로 앱에 전달된다."
        )
    return errors


def has_xff(block, parent, sets_xff, sets_headers):
    """자신 또는 상속되는 상위 블록이 XFF를 세팅하는지 확인한다.

    nginx는 현재 레벨에 proxy_set_header가 있으면 상위 레벨 설정을 **대체**한다(누적 아님).
    따라서 자체 헤더를 선언한 블록에서 상위 탐색을 멈춘다.
    """
    while block is not None:
        if block in sets_xff:
            return True
        if block in sets_headers:  # 자체 헤더 선언 → 상위 상속이 끊긴다
            return False
        block = parent[block]
    return False


def main(paths):
    files = sorted(
        file
        for target in (Path(p) for p in paths)
        for file in (
            [f for pattern in ("*.conf", "*.inc") for f in target.rglob(pattern)]
            if target.is_dir()
            else [target]
        )
    )
    if not files:
        print(f"검사 대상 파일이 없다: {paths}", file=sys.stderr)
        return 1

    errors = [
        error
        for file in files
        for error in check_include_content(file) + check_proxy_pass(file)
    ]
    if errors:
        print(f"\nX-Forwarded-For 사슬 결함 {len(errors)}건:\n", file=sys.stderr)
        for error in errors:
            print(f"  ✗ {error}", file=sys.stderr)
        print(
            "\nproxy_pass 하는 location에는 다음 중 하나를 include한다:\n"
            "  include /etc/nginx/conf.d/proxy-http.inc;   # HTTP\n"
            "  include /etc/nginx/conf.d/proxy-ws.inc;     # WebSocket\n"
            "상위 블록의 include에 기대는 경우, 그 하위 블록은 자체 proxy_set_header를 선언하면 안 된다\n"
            "(nginx는 현재 레벨 설정이 있으면 상위 레벨을 통째로 버린다).\n"
            "의도적 예외라면 proxy_pass 위에 `# proxy-header-exempt: <사유>` 주석을 단다.",
            file=sys.stderr,
        )
        return 1

    print(f"✓ nginx 프록시 헤더 검사 통과 ({len(files)}개 파일)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:] or ["backend/docker/nginx/conf"]))
