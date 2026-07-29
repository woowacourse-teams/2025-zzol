#!/usr/bin/env python3
"""nginx location 블록의 프록시 헤더 include 검사 — XFF 신뢰 사슬이 끊기는 것을 막는다.

배경(#1620): 앱의 내부 IP 화이트리스트(IpBlockFilter)가 X-Forwarded-For 스푸핑에 뚫리지 않는
이유는 필터 코드가 아니라 nginx + Tomcat RemoteIpValve 조합에 있다.

  1. nginx가 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for`로
     실제 TCP peer($remote_addr)를 XFF 맨 뒤에 덧붙인다 (proxy-http.inc·proxy-ws.inc).
  2. RemoteIpValve가 XFF를 오른쪽부터 훑으며 internalProxies(RFC1918)만 벗겨내고
     첫 비내부 IP에서 멈춘다 → 클라이언트가 왼쪽에 주입한 값은 도달하지 못한다.

이 사슬은 1번이 있어야만 성립한다. include 없이 proxy_pass 하는 location이 생기면 클라이언트가
보낸 XFF가 그대로 앱에 전달되고, 항목이 전부 사설 IP면 RemoteIpValve가 끝까지 벗겨내
**공격자가 넣은 사설 IP가 getRemoteAddr()가 되어 화이트리스트를 통과**한다.

가드가 조용히 사라지는 사고는 이미 한 번 겪었다(postmortem 0003 — 병렬 PR 머지로 화이트리스트
유실). 사람 리뷰로 지키는 불변조건은 신뢰하지 않고 CI로 고정한다.

검사 규칙
  - proxy_pass가 있는 블록은 자신 또는 상위 블록에서 proxy-http.inc / proxy-ws.inc를
    include해야 한다 (nginx의 proxy_set_header는 상위 컨텍스트에서 상속된다).

예외 표기
  proxy_pass와 같은 줄 또는 바로 윗줄에 `# proxy-header-exempt: <사유>` 주석을 두면
  그 proxy_pass 하나를 건너뛴다. 사유를 반드시 적게 해서 예외가 조용히 늘어나지 않게 한다.
"""
import re
import sys
from pathlib import Path

PROXY_PASS = re.compile(r"^\s*proxy_pass\s")
PROXY_INCLUDE = re.compile(r"^\s*include\s+\S*proxy-(http|ws)\.inc\s*;")
EXEMPT_MARKER = re.compile(r"#\s*proxy-header-exempt:\s*(.+)")

ROOT_BLOCK = 0


def check_file(path):
    parent = {ROOT_BLOCK: None}  # 블록 id -> 상위 블록 id
    includes = set()  # 프록시 헤더를 include하는 블록 id
    proxy_passes = []  # (블록 id, 줄번호, 예외 사유 or None)
    stack = [ROOT_BLOCK]
    next_id = ROOT_BLOCK + 1
    previous_exempt = None

    for lineno, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        marker = EXEMPT_MARKER.search(raw)
        exempt = marker.group(1).strip() if marker else None
        line = raw.split("#", 1)[0]

        if PROXY_PASS.search(line):
            proxy_passes.append((stack[-1], lineno, exempt or previous_exempt))
        elif PROXY_INCLUDE.search(line):
            includes.add(stack[-1])

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
        if has_proxy_headers(block, parent, includes):
            continue
        if exempt:
            print(f"  건너뜀 {path}:{lineno} — 예외 사유: {exempt}")
            continue
        errors.append(
            f"{path}:{lineno} — proxy_pass가 있으나 proxy-http.inc / proxy-ws.inc "
            f"include가 없다. XFF가 클라이언트 값 그대로 앱에 전달된다."
        )
    return errors


def has_proxy_headers(block, parent, includes):
    """자신 또는 상위 블록에 프록시 헤더 include가 있는지 확인한다."""
    while block is not None:
        if block in includes:
            return True
        block = parent[block]
    return False


def main(paths):
    files = sorted(
        file
        for target in (Path(p) for p in paths)
        for file in (
            [f for pattern in ("*.conf", "*.inc") for f in target.glob(pattern)]
            if target.is_dir()
            else [target]
        )
    )
    if not files:
        print(f"검사 대상 파일이 없다: {paths}", file=sys.stderr)
        return 1

    errors = [error for file in files for error in check_file(file)]
    if errors:
        print(f"\n프록시 헤더 include 누락 {len(errors)}건:\n", file=sys.stderr)
        for error in errors:
            print(f"  ✗ {error}", file=sys.stderr)
        print(
            "\nproxy_pass 하는 location에는 다음 중 하나를 include한다:\n"
            "  include /etc/nginx/conf.d/proxy-http.inc;   # HTTP\n"
            "  include /etc/nginx/conf.d/proxy-ws.inc;     # WebSocket\n"
            "의도적 예외라면 proxy_pass 위에 `# proxy-header-exempt: <사유>` 주석을 단다.",
            file=sys.stderr,
        )
        return 1

    print(f"✓ nginx 프록시 헤더 검사 통과 ({len(files)}개 파일)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:] or ["backend/docker/nginx/conf"]))
