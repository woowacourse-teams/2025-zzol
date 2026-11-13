#!/usr/bin/env python3
"""골든셋 러너가 남긴 행 단위 판정(TSV) 두 개를 짝지어 비교한다.

사용:
    python3 compare_models.py <기준 모델 이름> <기준.tsv> <비교 모델 이름> <비교.tsv>
    python3 compare_models.py --single <모델 이름> <판정.tsv>
    python3 compare_models.py --fill <판정.tsv> <다시 잰 판정.tsv>
    python3 compare_models.py --self-check

--fill 은 실패해서 비었던 배치를 NICKNAME_AUDIT_ONLY_CALLS 로 다시 잰 결과로 채운다. 판정.tsv 와 .calls.tsv 를 고쳐 쓴다.
--single 은 모델 하나의 지표, 표본별 판정 수, 배치 안 위치별 정답률, 틀리거나 뒤집힌 표본을 낸다.

TSV 는 NicknameAuditGoldenTest 가 build/reports/golden/ 에 쓴다.
표준 라이브러리만 쓴다. 결과는 마크다운으로 표준 출력에 나온다.
"""

import csv
import os
import sys
from collections import Counter, defaultdict
from itertools import combinations, product
from math import comb, sqrt

VERDICTS = ["CLEAN", "FLAGGED", "PENDING"]
CAUGHT = {"FLAGGED", "PENDING"}
TOKENS = ["prompt", "candidates", "cached", "thoughts"]
# 운영 설정 flagged-threshold. flagged=true 인데 confidence 가 이보다 낮으면 PENDING 이 된다
THRESHOLD = 0.85
# 비열등 한계. 정답률 차이가 이보다 작으면 운영에서 구분하지 않기로 미리 정한 값이다
MARGIN = 0.02


def load(path):
    """닉네임 -> {expected, category, runs: {회차: 판정}, obs: [(flagged, confidence)]} 과 호출 기록을 돌려준다.

    호출 기록은 옆에 있는 <이름>.calls.tsv 에서 읽는다. 없으면(순서를 섞기 전 측정) 행에서 되살린다.
    그때는 배치 전체가 MISSING 이면 실패한 호출로 본다.
    """
    items, batches = {}, {}
    with open(path, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE):
            item = items.setdefault(
                row["nickname"],
                {"expected": row["expected"], "category": row["category"], "runs": {}, "obs": [], "detail": {}},
            )
            item["runs"][int(row["run"])] = row["predicted"]
            seen = batches.get((row["run"], row["batch"]), {}).get("n", 0)
            # 배치 안에서 몇 번째였는지. 행은 보낸 순서대로 적혀 있다
            item["detail"][int(row["run"])] = (int(row["batch"]), seen + 1, row.get("confidence", ""))
            if row.get("confidence"):
                item["obs"].append((row["flagged"] == "true", float(row["confidence"]), row["predicted"]))
            batch = batches.setdefault((row["run"], row["batch"]), {"ok": False, "ms": int(row["batch_latency_ms"])})
            batch["ok"] |= row["predicted"] != "MISSING"
            batch["n"] = seen + 1
    calls_path = path[: -len(".tsv")] + ".calls.tsv"
    if not os.path.exists(calls_path):
        return items, list(batches.values())
    with open(calls_path, encoding="utf-8", newline="") as f:
        return items, [
            {"ok": row["succeeded"] == "true", "ms": int(row["latency_ms"]), "failure": row["failure"],
             **{key: int(row[key + "_tokens"]) for key in TOKENS}}
            for row in csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE)
        ]


def fill(base_path, patch_path):
    """base 의 실패한 배치 행을 patch 의 같은 회차·배치 행으로 바꾼다. 닉네임 구성이 같아야 한다."""
    def read(path):
        with open(path, encoding="utf-8", newline="") as f:
            reader = csv.DictReader(f, delimiter="\t", quoting=csv.QUOTE_NONE)
            return reader.fieldnames, list(reader)

    def write(path, fields, rows):
        with open(path, "w", encoding="utf-8", newline="") as f:
            writer = csv.DictWriter(f, fields, delimiter="\t", quoting=csv.QUOTE_NONE, quotechar=None,
                                    lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)

    calls = lambda path: path[: -len(".tsv")] + ".calls.tsv"  # noqa: E731
    call_fields, base_calls = read(calls(base_path))
    _, patch_calls = read(calls(patch_path))
    filled = {(c["run"], c["batch"]): c for c in patch_calls if c["succeeded"] == "true"}
    failed = {(c["run"], c["batch"]) for c in base_calls if c["succeeded"] != "true"}
    targets = filled.keys() & failed
    row_fields, base_rows = read(base_path)
    patch_rows = {(r["run"], r["batch"], r["position"]): r for r in read(patch_path)[1]}
    for i, row in enumerate(base_rows):
        if (row["run"], row["batch"]) in targets:
            new = patch_rows[(row["run"], row["batch"], row["position"])]
            assert new["nickname"] == row["nickname"], "배치 구성이 다르다. 시드와 배치 크기를 확인한다"
            base_rows[i] = new
    write(base_path, row_fields, base_rows)
    write(calls(base_path), call_fields, [filled.get((c["run"], c["batch"]), c) if (c["run"], c["batch"]) in targets
                                           else c for c in base_calls])
    print(f"채운 배치: {sorted(targets)}")


def majority(runs):
    """판정을 받은 회차의 다수결. 동률이면 앞선 회차 판정을 쓴다. 전부 MISSING 이면 None."""
    answered = [runs[r] for r in sorted(runs) if runs[r] != "MISSING"]
    if not answered:
        return None
    top = max(Counter(answered).values())
    return next(v for v in answered if answered.count(v) == top)


def correct(expected, verdict):
    """욕설·우회는 걸러내면(FLAGGED/PENDING), 경계는 PENDING, 정상은 CLEAN 이면 맞다."""
    if expected in ("PROFANE", "EVASION"):
        return verdict in CAUGHT
    return verdict == ("PENDING" if expected == "AMBIGUOUS" else "CLEAN")


def rate(hit, total):
    if total == 0:
        return "n/a"
    text = f"{100 * hit / total:.1f}% ({hit}/{total})"
    # 0건은 3의 법칙으로 95% 신뢰 상한을 붙인다
    return text + f", 상한 {100 * 3 / total:.1f}%" if hit == 0 else text


def mcnemar_exact(b, c):
    """양측 정확 McNemar. 엇갈린 b+c 건이 반반으로 갈린다는 귀무가설의 이항 검정이다."""
    n = b + c
    if n == 0:
        return 1.0
    return min(1.0, 2 * sum(comb(n, i) for i in range(min(b, c) + 1)) / 2**n)


def kappa(pairs):
    n = len(pairs)
    observed = sum(a == b for a, b in pairs) / n
    left, right = Counter(a for a, _ in pairs), Counter(b for _, b in pairs)
    expected = sum(left[v] * right[v] for v in VERDICTS) / n**2
    return observed, (1.0 if expected == 1 else (observed - expected) / (1 - expected))


def pair_disagreement(runs_a, runs_b, same_model):
    """회차 쌍마다 판정이 다른 닉네임 비율을 재고 쌍 평균을 낸다. 둘 다 판정을 받은 닉네임만 센다."""
    run_ids = sorted({r for runs in runs_a.values() for r in runs})
    pairs = list(combinations(run_ids, 2)) if same_model else list(product(run_ids, run_ids))
    rates = []
    for ra, rb in pairs:
        both = [
            (runs_a[n].get(ra), runs_b[n].get(rb))
            for n in runs_a
            if n in runs_b and "MISSING" not in (runs_a[n].get(ra, "MISSING"), runs_b[n].get(rb, "MISSING"))
        ]
        if both:
            rates.append(sum(a != b for a, b in both) / len(both))
    return sum(rates) / len(rates) if rates else float("nan")


def model_summary(items, calls):
    major = {n: majority(i["runs"]) for n, i in items.items()}
    by_expected = defaultdict(list)
    for n, i in items.items():
        if major[n] is not None:
            by_expected[i["expected"]].append(major[n])

    def share(expected, hit):
        pool = by_expected[expected]
        return rate(sum(v in hit for v in pool), len(pool))

    run_ids = sorted({r for i in items.values() for r in i["runs"]})
    comparable = [
        [v for v in i["runs"].values() if v != "MISSING"]
        for i in items.values()
        if sum(v != "MISSING" for v in i["runs"].values()) >= 2
    ]
    flipped = sum(len(set(v)) > 1 for v in comparable)
    missed = by_expected["PROFANE"] + by_expected["EVASION"]
    return {
        "욕설(PROFANE) 재현율": share("PROFANE", CAUGHT),
        "우회(EVASION) 재현율": share("EVASION", CAUGHT),
        "욕설·우회 미탐": rate(sum(v == "CLEAN" for v in missed), len(missed)),
        "정상(CLEAN) 오탐률": share("CLEAN", CAUGHT),
        "경계(AMBIGUOUS) PENDING 비율": share("AMBIGUOUS", {"PENDING"}),
        "경계 판정 분포 CLEAN / FLAGGED / PENDING": " / ".join(str(by_expected["AMBIGUOUS"].count(v)) for v in VERDICTS),
        "회차별 MISSING": " / ".join(
            str(sum(i["runs"].get(r, "MISSING") == "MISSING" for i in items.values())) for r in run_ids
        ),
        "성공한 호출 응답 시간 평균 / 최대": latency(calls),
        "실패한 호출": rate(sum(not c["ok"] for c in calls), len(calls)),
        "호출당 토큰 평균 (입력 / 출력 / 캐시 / 생각)": tokens(calls),
        "반복 간 판정 뒤집힘": rate(flipped, len(comparable)),
    }


def latency(calls):
    # 실패한 호출은 타임아웃까지 기다렸거나 곧바로 끊겨서 모델 응답 시간이 아니다
    ok = [c["ms"] for c in calls if c["ok"]]
    return f"{sum(ok) / len(ok) / 1000:.1f}s / {max(ok) / 1000:.1f}s ({len(ok)}회)" if ok else "n/a"


def tokens(calls):
    ok = [c for c in calls if c["ok"] and "prompt" in c]
    return " / ".join(f"{sum(c[key] for c in ok) / len(ok):.0f}" for key in TOKENS) if ok else "기록 없음"


def confidence_sections(name, items):
    """모델 하나의 confidence 분포. 다수결이 아니라 판정을 받은 모든 회차의 행을 센다."""
    obs = [(i["expected"], flagged, conf, correct(i["expected"], verdict))
           for i in items.values() for flagged, conf, verdict in i["obs"]]
    if not obs:
        return [f"### {name}", "", "confidence 열이 없는 측정이다."]

    def low(pool):
        return rate(sum(conf < THRESHOLD for conf in pool), len(pool))

    def mean(pool):
        return f"{sum(pool) / len(pool):.3f}" if pool else "n/a"

    out = [f"### {name}", "", "| confidence 구간 | flagged=true | flagged=false |", "| --- | --- | --- |"]
    # 0.05 폭 구간. 1.00 은 마지막 구간에 넣는다
    bins = Counter((min(int(round(conf * 100)) // 5, 19), flagged) for _, flagged, conf, _ in obs)
    for b in range(20):
        if bins[(b, True)] or bins[(b, False)]:
            upper = "1.00" if b == 19 else f"{(b + 1) * 0.05:.2f} 미만"
            out.append(f"| {b * 0.05:.2f} 이상 {upper} | {bins[(b, True)]} | {bins[(b, False)]} |")
    out += ["", f"| 묶음 | 행 수 | flagged=true | confidence 평균 | {THRESHOLD} 미만 |"
            f" flagged=true 중 {THRESHOLD} 미만 |", "| --- | --- | --- | --- | --- | --- |"]
    groups = [("전체", lambda e, c: True)]
    groups += [(e, lambda x, c, e=e: x == e) for e in ("PROFANE", "EVASION", "AMBIGUOUS", "CLEAN")]
    groups += [("정답 맞음", lambda e, c: c), ("정답 틀림", lambda e, c: not c)]
    for label, keep in groups:
        pool = [(flagged, conf) for e, flagged, conf, c in obs if keep(e, c)]
        confs = [conf for _, conf in pool]
        flagged_confs = [conf for flagged, conf in pool if flagged]
        out.append(f"| {label} | {len(pool)} | {len(flagged_confs)} | {mean(confs)} | {low(confs)} |"
                   f" {low(flagged_confs)} |")
    return out


def category_rows(name_a, items_a, name_b, items_b):
    """카테고리와 expected 묶음마다 두 모델이 걸러낸(FLAGGED/PENDING) 비율과 PENDING 비율을 낸다."""
    groups = defaultdict(lambda: ([], []))
    for n, a in items_a.items():
        va, vb = majority(a["runs"]), majority(items_b[n]["runs"]) if n in items_b else None
        if va is not None and vb is not None:
            groups[(a["category"], a["expected"])][0].append(va)
            groups[(a["category"], a["expected"])][1].append(vb)
    out = ["걸러냄은 FLAGGED 와 PENDING 을 합친 비율이다. 욕설·우회 행에서는 재현율이고 정상 행에서는 오탐률이다.", "",
           f"| 카테고리 | expected | 걸러냄 {name_a} | 걸러냄 {name_b} | PENDING {name_a} | PENDING {name_b} |",
           "| --- | --- | --- | --- | --- | --- |"]
    for (category, expected), (va, vb) in groups.items():
        caught = [rate(sum(v in CAUGHT for v in vs), len(vs)) for vs in (va, vb)]
        pending = [f"{vs.count('PENDING')}/{len(vs)}" for vs in (va, vb)]
        out.append(f"| {category} | {expected} | {caught[0]} | {caught[1]} | {pending[0]} | {pending[1]} |")
    return out


def single(name, items, calls):
    out = [f"## {name} 지표 (다수결 기준)", "", "| 지표 | 값 |", "| --- | --- |"]
    out += [f"| {key} | {value} |" for key, value in model_summary(items, calls).items()]

    votes = Counter(sum(v != "MISSING" for v in i["runs"].values()) for i in items.values())
    out += ["", "## 표본별 판정 수", "", "| 판정을 받은 회차 수 | 표본 수 |", "| --- | --- |"]
    out += [f"| {n} | {votes[n]} |" for n in sorted(votes, reverse=True)]

    groups = defaultdict(list)
    for i in items.values():
        verdict = majority(i["runs"])
        if verdict is not None:
            groups[(i["category"], i["expected"])].append(verdict)
    out += ["", "## 카테고리별", "", "걸러냄은 FLAGGED 와 PENDING 을 합친 비율이다. 욕설·우회 행에서는 재현율이고 정상 행에서는 오탐률이다.", "",
            "| 카테고리 | expected | 걸러냄 | CLEAN / FLAGGED / PENDING |", "| --- | --- | --- | --- |"]
    for (category, expected), vs in groups.items():
        out.append(f"| {category} | {expected} | {rate(sum(v in CAUGHT for v in vs), len(vs))} |"
                   f" {' / '.join(str(vs.count(v)) for v in VERDICTS)} |")

    # 배치 안 위치가 판정에 영향을 주는지 본다. 경계는 정답이 PENDING 이라 거의 다 틀리므로 뺀다
    by_position = defaultdict(lambda: [0, 0])
    for i in items.values():
        for run, verdict in i["runs"].items():
            if verdict != "MISSING" and i["expected"] != "AMBIGUOUS":
                bucket = by_position[(i["detail"][run][1] - 1) // 50]
                bucket[0] += not correct(i["expected"], verdict)
                bucket[1] += 1
    out += ["", "## 배치 안 위치별 오답률", "", "경계(AMBIGUOUS)를 뺀 모든 회차의 행을 센다.", "",
            "| 배치 안 위치 | 오답 |", "| --- | --- |"]
    out += [f"| {b * 50 + 1}~{b * 50 + 50} | {rate(*by_position[b])} |" for b in sorted(by_position)]

    out += ["", "## confidence 분포", "", "판정을 받은 모든 회차의 행을 센다. confidence 는 소수 둘째 자리로 반올림한 값이다.", ""]
    out += confidence_sections(name, items)

    out += ["", "## 부록: 다수결이 틀렸거나 회차마다 판정이 갈린 표본", "",
            "회차별 칸은 판정, 배치 번호와 배치 안 위치, confidence 순이다. 경계 표본은 PENDING 이 아니면 틀린 것으로 센다.", "",
            "| 닉네임 | expected | 카테고리 | 다수결 | " + " | ".join(f"{r}회차" for r in sorted(next(iter(items.values()))["runs"])) + " |",
            "| --- | --- | --- | --- |" + " --- |" * len(next(iter(items.values()))["runs"])]
    for n, i in items.items():
        verdict = majority(i["runs"])
        answered = {v for v in i["runs"].values() if v != "MISSING"}
        if verdict is None or (correct(i["expected"], verdict) and len(answered) == 1):
            continue
        cells = []
        for run in sorted(i["runs"]):
            batch, position, conf = i["detail"][run]
            cells.append("MISSING" if i["runs"][run] == "MISSING" else f"{i['runs'][run]} ({batch}-{position}, {conf or '기록 없음'})")
        out.append(f"| {n.replace('|', '¦')} | {i['expected']} | {i['category']} | {verdict} | " + " | ".join(cells) + " |")
    return "\n".join(out) + "\n"


def paired(items_a, items_b, include):
    """다수결 판정으로 정답 맞음/틀림을 짝짓는다. b: 기준만 맞음, c: 비교만 맞음."""
    both_right = b = c = both_wrong = 0
    for n, a in items_a.items():
        va, vb = majority(a["runs"]), majority(items_b[n]["runs"]) if n in items_b else None
        if va is None or vb is None or not include(a["expected"]):
            continue
        ca, cb = correct(a["expected"], va), correct(a["expected"], vb)
        both_right += ca and cb
        b += ca and not cb
        c += cb and not ca
        both_wrong += not ca and not cb
    n = both_right + b + c + both_wrong
    # 비교 모델 정답률 - 기준 모델 정답률. 짝지은 비율 차이의 Wald 95% 구간이다
    diff = (c - b) / n
    half = 1.96 * sqrt(max(b + c - (b - c) ** 2 / n, 0)) / n
    # 엇갈린 건이 없으면 Wald 구간이 0 폭이 된다. 3의 법칙 상한으로 대신한다
    lower = -3 / n if b + c == 0 else diff - half
    return {"n": n, "both_right": both_right, "b": b, "c": c, "both_wrong": both_wrong,
            "p": mcnemar_exact(b, c), "diff": diff, "lower": lower, "upper": 3 / n if b + c == 0 else diff + half}


def report(name_a, items_a, calls_a, name_b, items_b, calls_b):
    out = []
    sum_a, sum_b = model_summary(items_a, calls_a), model_summary(items_b, calls_b)
    out += ["## 모델별 지표 (3회 다수결 기준)", "", f"| 지표 | {name_a} | {name_b} |", "| --- | --- | --- |"]
    out += [f"| {key} | {sum_a[key]} | {sum_b[key]} |" for key in sum_a]

    runs_a = {n: i["runs"] for n, i in items_a.items()}
    runs_b = {n: i["runs"] for n, i in items_b.items()}
    within_a = pair_disagreement(runs_a, runs_a, True)
    within_b = pair_disagreement(runs_b, runs_b, True)
    between = pair_disagreement(runs_a, runs_b, False)
    out += ["", "## 반복 흔들림과 모델 간 차이", "", "회차 두 개를 골라 판정이 다른 닉네임 비율을 재고 쌍 평균을 냈다.", "",
            "| 비교 | 판정 불일치율 |", "| --- | --- |",
            f"| {name_a} 회차끼리 | {100 * within_a:.2f}% |",
            f"| {name_b} 회차끼리 | {100 * within_b:.2f}% |",
            f"| {name_a} 회차와 {name_b} 회차 | {100 * between:.2f}% |"]

    pairs = [(majority(items_a[n]["runs"]), majority(items_b[n]["runs"])) for n in items_a if n in items_b]
    pairs = [(a, b) for a, b in pairs if a is not None and b is not None]
    observed, k = kappa(pairs)
    table = Counter(pairs)
    out += ["", "## 판정 일치도", "",
            f"다수결 판정 {len(pairs)}건에서 일치 {100 * observed:.1f}%, Cohen's κ = {k:.3f}.", "",
            f"| {name_a} \\ {name_b} | " + " | ".join(VERDICTS) + " |", "| --- |" + " --- |" * len(VERDICTS)]
    out += [f"| {a} | " + " | ".join(str(table[(a, b)]) for b in VERDICTS) + " |" for a in VERDICTS]

    out += ["", "## McNemar 정확 검정 (정답 맞음/틀림)", "",
            f"b 는 {name_a} 만 맞은 건수, c 는 {name_b} 만 맞은 건수다. 차이는 {name_b} 정답률에서 {name_a} 정답률을 뺀 값이다.", "",
            "| 범위 | n | 둘 다 맞음 | b | c | 둘 다 틀림 | p (양측) | 정답률 차이 (95% 구간) |",
            "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    scopes = [("전체", lambda e: True), ("경계 제외", lambda e: e != "AMBIGUOUS"),
              ("욕설·우회만", lambda e: e in ("PROFANE", "EVASION")), ("정상만", lambda e: e == "CLEAN")]
    results = {}
    for label, include in scopes:
        r = results[label] = paired(items_a, items_b, include)
        out.append(f"| {label} | {r['n']} | {r['both_right']} | {r['b']} | {r['c']} | {r['both_wrong']} | {r['p']:.3f} |"
                   f" {100 * r['diff']:+.2f}%p ({100 * r['lower']:+.2f} ~ {100 * r['upper']:+.2f}) |")
    whole = results["전체"]
    verdict = "비열등하다" if whole["lower"] > -MARGIN else "비열등하다고 말할 수 없다"
    out += ["", f"비열등 한계 -{100 * MARGIN:.0f}%p 기준: 전체 범위 구간 하한이 {100 * whole['lower']:+.2f}%p 라서"
            f" {name_b} 는 {name_a} 보다 {verdict}."]

    out += ["", "## 카테고리별", ""] + category_rows(name_a, items_a, name_b, items_b)

    out += ["", "## confidence 분포", "",
            "판정을 받은 모든 회차의 행을 센다. confidence 는 소수 둘째 자리로 반올림한 값이다.", ""]
    out += confidence_sections(name_a, items_a) + [""] + confidence_sections(name_b, items_b)

    out += ["", "## 부록: 다수결 판정이 엇갈린 표본", "",
            f"| 닉네임 | expected | 카테고리 | {name_a} (회차별) | {name_b} (회차별) |", "| --- | --- | --- | --- | --- |"]
    for n, a in items_a.items():
        if n not in items_b:
            continue
        va, vb = majority(a["runs"]), majority(items_b[n]["runs"])
        if va != vb:
            runs = lambda i: "/".join(i["runs"][r] for r in sorted(i["runs"]))  # noqa: E731
            out.append(f"| {n.replace('|', '¦')} | {a['expected']} | {a['category']} |"
                       f" {va} ({runs(a)}) | {vb} ({runs(items_b[n])}) |")
    return "\n".join(out) + "\n"


def self_check():
    assert majority({1: "CLEAN", 2: "FLAGGED", 3: "FLAGGED"}) == "FLAGGED"
    assert majority({1: "PENDING", 2: "MISSING", 3: "CLEAN"}) == "PENDING"
    assert majority({1: "MISSING"}) is None
    assert correct("EVASION", "PENDING") and not correct("AMBIGUOUS", "FLAGGED") and correct("CLEAN", "CLEAN")
    assert mcnemar_exact(0, 0) == 1.0
    assert abs(mcnemar_exact(1, 5) - 0.21875) < 1e-9  # 2 * (C(6,0) + C(6,1)) / 64
    assert abs(mcnemar_exact(0, 6) - 0.03125) < 1e-9
    # 2x2 교과서 예: 일치 0.7, 우연 일치 0.5 -> κ 0.4
    pairs = [("CLEAN", "CLEAN")] * 20 + [("CLEAN", "FLAGGED")] * 5 + [("FLAGGED", "CLEAN")] * 10 + [("FLAGGED", "FLAGGED")] * 15
    observed, k = kappa(pairs)
    assert abs(observed - 0.7) < 1e-9 and abs(k - 0.4) < 1e-9
    assert rate(0, 300) == "0.0% (0/300), 상한 1.0%"
    same = {"가": {1: "CLEAN", 2: "CLEAN"}, "나": {1: "CLEAN", 2: "FLAGGED"}}
    assert pair_disagreement(same, same, True) == 0.5
    calls = [{"ok": False, "ms": 120000}, {"ok": True, "ms": 10000}, {"ok": True, "ms": 20000}]
    assert latency(calls) == "15.0s / 20.0s (2회)" and tokens(calls) == "기록 없음"
    items = {"가": {"expected": "AMBIGUOUS", "category": "경계", "runs": {1: "FLAGGED", 2: "MISSING", 3: "PENDING"},
                   "detail": {1: (1, 3, "0.90"), 2: (1, 9, ""), 3: (2, 51, "0.60")},
                   "obs": [(True, 0.9, "FLAGGED"), (True, 0.6, "PENDING"), (False, 1.0, "CLEAN")]}}
    text = single("모델", items, calls)
    assert "| 2 | 1 |" in text and "| 가 | AMBIGUOUS | 경계 | FLAGGED | FLAGGED (1-3, 0.90) | MISSING | PENDING (2-51, 0.60) |" in text, text
    lines = confidence_sections("모델", items)
    assert "| 0.60 이상 0.65 미만 | 1 | 0 |" in lines and "| 0.95 이상 1.00 | 0 | 1 |" in lines, lines
    assert "| 정답 맞음 | 1 | 1 | 0.600 | 100.0% (1/1) | 100.0% (1/1) |" in lines, lines
    print("self-check ok")


if __name__ == "__main__":
    if sys.argv[1:] == ["--self-check"]:
        self_check()
    elif len(sys.argv) == 4 and sys.argv[1] == "--fill":
        fill(sys.argv[2], sys.argv[3])
    elif len(sys.argv) == 4 and sys.argv[1] == "--single":
        sys.stdout.write(single(sys.argv[2], *load(sys.argv[3])))
    elif len(sys.argv) == 5:
        a, calls_a = load(sys.argv[2])
        b, calls_b = load(sys.argv[4])
        sys.stdout.write(report(sys.argv[1], a, calls_a, sys.argv[3], b, calls_b))
    else:
        sys.exit(__doc__)
