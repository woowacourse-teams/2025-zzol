#!/usr/bin/env python3
"""알림 룰의 job 스코프 검사 — 집계가 job 라벨을 지우는 것을 막는다.

배경(#1592·#1596·#1598): 집계 함수에 job을 빠뜨리는 같은 실수가 여러 룰에서 반복됐다.
`sum(rate(...))`처럼 by 절 없이 집계하면 두 가지가 동시에 깨진다.

  1. dev·prod 값이 합산되어, 한쪽 환경만 이상해도 다른 환경 이름으로 알림이 발화한다.
  2. 집계가 job 라벨을 지워, 발화한 알림에 환경 정보가 남지 않는다.
     → Alertmanager 환경별 라우팅 불가, zzol-bot이 어느 환경 로그를 볼지 판단 불가.

실제로 이 결함 때문에 dev 스캐너 트래픽이 prod 알림으로 13건 발화했고, zzol-bot은
prod 로그를 근거로 오진했다. 사람 리뷰만으로는 네 번 연속 놓쳤으므로 CI로 고정한다.

검사 규칙
  - 집계 연산자(sum/max/avg/count/...)는 `by (...)`에 job을 포함해야 한다.
  - `without (...)`을 쓰면 job을 제외해서는 안 된다.
  - 집계가 없는 단순 셀렉터는 라벨이 그대로 보존되므로 통과시킨다.

예외 표기
  룰 위 주석에 `# job-scope-exempt: <사유>`를 두면 그 룰 하나를 건너뛴다.
  사유를 반드시 적게 해서, 예외가 조용히 늘어나지 않게 한다.
"""
import re
import sys
from pathlib import Path

AGGREGATIONS = (
    "sum", "min", "max", "avg", "group", "stddev", "stdvar",
    "count", "count_values", "topk", "bottomk", "quantile",
)
AGG_PATTERN = re.compile(
    r"\b(" + "|".join(AGGREGATIONS) + r")\s*"          # 집계 연산자
    r"(?:(by|without)\s*\(([^)]*)\)\s*)?"              # 앞에 오는 by/without (sum by (job) (...))
    r"\(",
    re.IGNORECASE,
)
# 뒤에 오는 형태: sum(...) by (job)
TRAILING_GROUPING = re.compile(r"\)\s*(by|without)\s*\(([^)]*)\)", re.IGNORECASE)
ALERT_LINE = re.compile(r"^\s*-\s*alert:\s*(\S+)")
EXEMPT_MARKER = re.compile(r"#\s*job-scope-exempt:\s*(.+)")
BLOCK_END = re.compile(r"^\s*(for|labels|annotations|record):")


def extract_rules(path):
    """(alert명, expr 텍스트, 예외 사유) 목록을 원문 라인에서 뽑는다.

    YAML 파서를 쓰지 않는 이유는 예외 표기가 주석이라 파싱 시 사라지기 때문이다.
    주석을 룰 옆에 두는 편이 별도 예외 목록 파일보다 읽는 사람에게 낫다.
    """
    rules = []
    name = None
    expr_lines = []
    exempt = None
    pending_exempt = None
    in_expr = False

    def flush():
        if name is not None:
            rules.append((name, "\n".join(expr_lines), exempt))

    for line in path.read_text(encoding="utf-8").split("\n"):
        marker = EXEMPT_MARKER.search(line)
        if marker:
            pending_exempt = marker.group(1).strip()
            continue

        alert = ALERT_LINE.match(line)
        if alert:
            flush()
            name, expr_lines, exempt, in_expr = alert.group(1), [], pending_exempt, False
            pending_exempt = None
            continue

        if name is None:
            continue
        if re.match(r"\s*expr:", line):
            in_expr = True
            expr_lines.append(re.sub(r"^\s*expr:\s*\|?\s*", "", line))
            continue
        if in_expr and BLOCK_END.match(line):
            in_expr = False
            continue
        if in_expr and not line.strip().startswith("#"):
            expr_lines.append(line)

    flush()
    return rules


def violations(expr):
    """job 라벨을 보존하지 않는 집계를 찾는다."""
    found = []
    trailing = TRAILING_GROUPING.findall(expr)
    for match in AGG_PATTERN.finditer(expr):
        op, modifier, labels = match.group(1), match.group(2), match.group(3)
        if modifier is None:
            # sum(...) by (job) 형태를 뒤에서 찾는다. 어느 집계에 붙는지 정확히
            # 대응시키려면 파서가 필요하므로, 하나라도 job을 보존하면 통과시킨다.
            if any(m == "by" and "job" in lbls for m, lbls in trailing):
                continue
            if any(m == "without" and "job" not in lbls for m, lbls in trailing):
                continue
            found.append(f"{op}(...) — by 절이 없어 job 라벨이 사라진다")
        elif modifier.lower() == "by" and "job" not in labels:
            found.append(f"{op} by ({labels.strip()}) — job이 빠져 있다")
        elif modifier.lower() == "without" and "job" in labels:
            found.append(f"{op} without ({labels.strip()}) — job을 제외하고 있다")
    return found


def main():
    roots = [Path(p) for p in sys.argv[1:]] or [Path(".")]
    files = sorted({f for root in roots for f in root.rglob("*.yml")})
    if not files:
        print(f"검사할 룰 파일을 찾지 못했다: {[str(r) for r in roots]}", file=sys.stderr)
        return 1

    failures = []
    exempted = []
    checked = 0

    for path in files:
        for name, expr, exempt in extract_rules(path):
            if exempt is not None:
                exempted.append((path, name, exempt))
                continue
            checked += 1
            for problem in violations(expr):
                failures.append((path, name, problem))

    for path, name, reason in exempted:
        print(f"⏭️  {path}::{name} — 예외: {reason}")

    if failures:
        print(f"\n❌ job 스코프 위반 {len(failures)}건\n")
        for path, name, problem in failures:
            print(f"  {path}")
            print(f"    {name}: {problem}")
        print(
            "\n집계는 job을 보존해야 한다 — `sum by (job) (...)` 또는 `max by (job) (...)`.\n"
            "환경을 하나로 좁히려면 셀렉터에 `{job=\"prod-app\"}`을 함께 준다.\n"
            "의도적으로 job이 필요 없는 룰이면 룰 위에 `# job-scope-exempt: <사유>`를 적는다."
        )
        return 1

    print(f"\n✅ 알림 룰 {checked}개 job 스코프 검사 통과 (예외 {len(exempted)}건)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
