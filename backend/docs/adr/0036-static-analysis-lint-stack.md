# 0036. 정적 분석 스택 도입 — Spotless(포맷) + PMD(구조 규칙)

- 날짜: 2026-07-31
- 상태: 적용됨 (#1628 — 실측으로 규칙 구성이 바뀌었다. 「구현 결과」 참조)

## 컨텍스트

백엔드에 **Java 정적 분석 도구가 하나도 없다.** 현재 "검사" 역할을 하는 것은 셋뿐이다.

| 도구 | 위치 | 잡는 것 |
| --- | --- | --- |
| `-Xlint:deprecation` | `build.gradle.kts:37` | deprecated API — **경고만** (`-Werror` 없어 빌드가 깨지지 않음) |
| ArchUnit | `app/src/test/java/coffeeshout/arch/`, `admin/.../AdminArchitectureTest` | 모듈 의존 방향·계층 위반 |
| CodeQL | `.github/workflows/codeql.yml` | 보안 취약점 패턴 |

`backend-ci.yml`은 `test`와 `bootJar`만 실행한다. 즉 포맷·중첩 깊이·메서드 길이·`else` 사용 같은 `docs/conventions-production.md`의 **「코드 작성 원칙」은 전부 사람이 지키고 사람이 잡는다.** 기계가 판정할 수 있는 규칙까지 `deep-review`·`code-reviewer`가 매 PR마다 다시 지적하면서 리뷰 대역폭을 소모하고, 리뷰어의 주의력에 따라 같은 위반이 통과하기도 한다.

컨벤션 규칙을 판정 가능성으로 분류하면 경계가 뚜렷하다.

| 규칙 | 기계 판정 | 담당 |
| --- | --- | --- |
| 중첩 깊이 제한, 긴 메서드 분리, `else` 대신 early return, 지역 변수 `final` | ✅ | 신규 도구 |
| 계층별 클래스 네이밍, `@WsTopic`/`@WsQueue` 누락, 예외 상속 계층 | ✅ | **ArchUnit — 이미 있음** |
| 단일 책임, 추상화 수준 일치 | ⚠️ 간접 (복잡도 지표) | 신규 도구 + 사람 |
| 트랜잭션 스크립트 지양, 계산/사이드이펙트 분리, 하드코딩 대신 `@ConfigurationProperties` | ❌ | 사람·`deep-review` |

`backend/*/src/main/java` 807개 파일 / 35,376줄 실측 결과(휴리스틱 측정, 람다·빌더 체인 때문에 과대 집계 경향):

| 항목 | 위반 추정 | 분포 |
| --- | --- | --- |
| 순수 `else` (`else if` 제외) | **10곳** | — |
| 제어문 중첩 ≥ 3인 메서드 | **44개** | zzolbot 15 · game 10 · websocket 8 |
| 메서드 ≥ 20줄 | **67개** | 20–24줄 32 · 25–29줄 15 · 30–39줄 13 · 40줄+ 7 |
| non-final 지역 변수 | **거의 0** (`final` 선언 901곳) | 이미 지켜짐 |

**규모가 감당 가능할 때 도입해야 한다.** 위반이 수백 개로 불어나면 도입 자체가 대형 리팩터링이 되어 영영 미뤄진다.

## 결정

### 1. 두 축으로 나눈다 — 모양은 자동 수정, 구조는 지적

| | Spotless | PMD |
| --- | --- | --- |
| 대상 | 공백·줄바꿈·import 순서 (의미 불변) | 중첩·길이·`else`·`final` (구조) |
| 동작 | **자동 수정**(`spotlessApply`) | 지적만, 사람이 고침 |
| 효과 | 포맷 논쟁 소멸 + diff 노이즈 제거 | 설계 논쟁만 리뷰에 남김 |

겹치는 규칙은 "미사용 import" 하나뿐이라 **대체재가 아니라 보완재**다. Spotless가 diff에서 공백 노이즈를 걷어내면 PMD와 리뷰가 볼 것이 선명해진다.

**포맷터는 `palantirJavaFormat()`(4칸 / 120열)을 쓴다.**

| 포맷터 | 판단 |
| --- | --- |
| **palantir-java-format (채택)** | google-java-format의 포크로 "무설정·결정론적" 철학을 그대로 상속하면서 체이닝·람다를 **문법 상위 단위에서 끊는다** — Stream·빌더 비중이 높은 이 코드베이스에 마찰이 적다. 120열이라 재배치 대상이 817줄(2.3%)로 절반 이하 |
| google-java-format `.aosp()` | 업계 표준이고 자료가 많지만 **100열 고정**이라 재배치 대상이 1,906줄(5.4%). 한국어 주석과 긴 도메인명(`SelectCardCommandEventConsumer`)이 자주 넘긴다. 체이닝을 인자 단위로 잘게 쪼갠다. JDK 17+ 에서 javac 내부 API 접근(`--add-exports`) 이슈가 따라붙는다 |
| Eclipse formatter | XML 프로필로 전부 조정 가능 = "탭이냐 공백이냐" 논쟁이 되살아난다. 포맷터를 넣는 목적 자체를 무너뜨려 후보에서 제외 |

palantir는 생태계가 작아 **JDK 신문법 대응이 느릴 수 있다는 리스크**를 안는다(현재 JDK 21 + Boot 4 마이그레이션 중). 다만 Spotless가 포맷터를 버전 핀으로 감싸므로 **교체 비용은 `spotlessApply` 한 번**이다 — 되돌리기 싼 결정이라 길게 고민하지 않는다. 리스크가 현실화되면 그때 google로 갈아탄다.

### 2. 구조 규칙 도구는 Checkstyle이 아니라 PMD

요구 규칙 3개 중 **2개가 어느 도구에도 내장돼 있지 않다.**

- **혼합 중첩 깊이 제한**: Checkstyle의 `NestedIfDepth`/`NestedForDepth`/`NestedTryDepth`는 종류별로 따로 세므로 `for { if { if } }`를 통과시킨다 — 요구사항("깊이 2")을 못 채운다. PMD의 `AvoidDeeplyNestedIfStmts`도 `if`만 센다.
- **`else` 금지(early return 강제)**: 양쪽 다 내장 규칙이 없다.

따라서 **커스텀 규칙 작성 비용이 도구 선택을 가른다.** PMD는 룰셋 XML에 XPath 표현식을 넣으면 끝이고, Checkstyle은 `AbstractCheck`를 상속한 Java 클래스를 작성해 별도 jar로 배포·버전 관리해야 한다. 둘 다 Gradle 코어 플러그인이라 도입 비용 자체는 동일하다.

### 3. 네이밍·계층 규칙은 ArchUnit이 계속 담당한다

`{Domain}Service`·`{Domain}Notifier` 같은 계층별 네이밍, `@WsTopic` 누락, 예외 상속 계층은 **이미 있는 ArchUnit으로 표현 가능**하다. PMD `ClassNamingConventions`로 옮기지 않는다 — 패키지·계층 문맥을 아는 쪽이 ArchUnit이고, 새 도구에 규칙을 분산시키면 "네이밍 규칙이 어디 있나"가 두 곳으로 갈린다.

### 4. 규칙을 3묶음으로 나눠 도입 강도를 다르게 건다

**현재 위반 수가 도입 강도를 정한다.** 위반이 없는 규칙은 정리 비용이 0이므로 처음부터 실패로 걸고, 위반이 있는 규칙만 완충 후 조인다.

**묶음 A — 즉시 실패(`ignoreFailures` 없이).** 실측 위반이 0~2곳이라 리팩터링 없이 게이트가 생긴다.

| 규칙 | 현재 위반 | 근거 |
| --- | --- | --- |
| 테스트 `Thread.sleep` 금지 | 2곳 | `conventions-test.md:163`. 포스트모템 0001·0002가 모두 플레이키 사건 |
| JUnit `assertTrue`/`assertEquals` 금지 (AssertJ 통일) | 0곳 (`assertThat` 2,145곳) | 이미 100% 일관 — 잠그기만 |
| `System.out`/`printStackTrace` 금지 | 0곳 | PMD 내장 `SystemPrintln`·`AvoidPrintStackTrace` |
| `@Autowired` 필드 주입 금지 | 0곳 | 생성자 주입 강제. 6곳은 전부 `:test-support` 기반 클래스라 모듈 예외 |
| 인스턴스 필드 `final` | 0곳 | `conventions-production.md:44` |
| 지역 변수 `final` | 거의 0 (`final` 901곳) | `conventions-production.md:45`. PMD 내장 `LocalVariableCouldBeFinal` |

**묶음 B — 계층을 한정해 적용.** 전역으로 걸면 정당한 사용까지 잡아 억제 주석이 폭발한다. `domain`·`application` 패키지(317개 파일)로 좁히면 오탐이 사라지고 규모도 감당 가능해진다.

| 규칙 | 전역 | **한정 시** | 좁히는 이유 |
| --- | --- | --- | --- |
| `throw new IllegalArgumentException`/`IllegalStateException` | 53곳 | **11곳 / 9파일** | 도메인에서 던지면 `ErrorCode` 매핑을 우회해 에러 응답 포맷이 깨진다 — 스타일이 아니라 **버그**. 인프라·설정 계층의 인자 검증은 정당 |
| `catch (Exception)`/`catch (Throwable)` | 112곳 | **29곳 / 16파일** | 나머지 83곳은 Redis Consumer·스케줄러 등 **경계에서 삼켜야 하는** 정당한 사용 |

**묶음 C — 구조 규칙.** 중첩 깊이·메서드 길이·`else`. 위반이 121곳(44+67+10)이라 완충 임계값으로 시작한다(결정 5). 커스텀 XPath가 필요한 것도 이 묶음이다.

```xml
<!-- backend/config/pmd/ruleset.xml -->
<rule name="MaxNestingDepth2" language="java"
      message="제어문 중첩은 2단계까지. 메서드로 분리하라"
      class="net.sourceforge.pmd.lang.rule.xpath.XPathRule">
  <properties><property name="xpath"><value><![CDATA[
    //(IfStatement|ForStatement|ForeachStatement|WhileStatement|DoStatement|SwitchStatement|TryStatement)
      [count(ancestor::*[self::IfStatement or self::ForStatement or self::ForeachStatement
                      or self::WhileStatement or self::DoStatement or self::SwitchStatement
                      or self::TryStatement]) >= 2]
  ]]></value></property></properties>
</rule>

<rule name="NoElseAfterReturn" language="java"
      message="then 절이 return/throw로 끝나면 else를 없애고 early return"
      class="net.sourceforge.pmd.lang.rule.xpath.XPathRule">
  <properties><property name="xpath"><value><![CDATA[
    //IfStatement[@Else = true()]
      [*[2][self::ReturnStatement or self::ThrowStatement
         or self::Block[*[last()][self::ReturnStatement or self::ThrowStatement]]]]
  ]]></value></property></properties>
</rule>
```

**AST 노드명과 `@Else` 속성명은 PMD 7 Rule Designer로 실제 파일에 붙여 검증한 뒤 확정한다.** PMD 7에서 Java AST가 크게 개편되어 6.x 예제 XPath가 그대로 동작하지 않는다.

길이·복잡도는 내장 `NcssCount`·`CognitiveComplexity`를 쓴다. 메서드 길이를 물리 줄 수가 아니라 `NcssCount`(문장 수)로 재는 이유는, PMD 7에 줄 수 기반 규칙이 없기도 하지만 문장 수가 어노테이션·빈 줄·포맷에 흔들리지 않아 더 정확하기 때문이다. `MethodArgumentCouldBeFinal`은 **끈다** — 컨벤션이 "매개변수에는 `final`을 붙이지 않는다"이기 때문이다.

**제외: 시간 API 직접 호출 금지(`Instant.now()`·`currentTimeMillis()`, 126곳).** 「외부 의존성(시간·랜덤·I/O)은 파라미터로 주입」 원칙의 대상이지만 규칙화하지 않는다. `createdAt`처럼 **"그 일이 일어난 시각을 기록하는" 용도는 주입 대상이 아니고**, 컨벤션 자신도 도메인 이벤트의 `timestamp`를 명시적으로 예외로 둔다(`conventions-production.md:75`). 126곳 중 35개 파일이 이벤트 계열이라, 기계는 "분기에 쓰이는 시각"과 "기록되는 시각"을 구분하지 못해 예외 목록이 규칙보다 길어진다. **판정이 의도에 달린 규칙은 리뷰가 맡는다.**

### 5. 임계값은 묶음별로 다르게 시작한다

**PMD에는 baseline 기능이 없다**(Sonar의 "new code" 게이트가 유일한 대안). 그래서 전 규칙에 목표 임계값을 한 번에 걸면 CI가 즉시 빨개진다. 대신 묶음별로:

- **묶음 A** — 처음부터 실패. 위반이 0~2곳이라 완충이 불필요하다.
- **묶음 B** — 40곳(11+29)을 이 작업 안에서 정리하고 실패로 건다. 계층 한정이라 규모가 닫혀 있다.
- **묶음 C** — **한 칸 느슨한 값으로 걸고 그 위반만 정리한다.** 목표값(깊이 2)까지 조이는 것은 후속 작업으로 미룬다.

| 규칙 | 시작 임계값 | 그때 위반 | 목표값 | 목표 시 위반 |
| --- | --- | --- | --- | --- |
| 중첩 깊이 | **3** | **9개** (zzolbot 5) | 2 | 44개 |
| `NcssCount` | **문장 25** (≈40줄) | 7개 | 문장 20 (≈30줄) | 20개 |
| `NoElseAfterReturn` | 즉시 | 10곳 | — | — |

**목표값으로 바로 걸지 않는 이유는 리뷰 가능성이다.** 깊이 2로 시작하면 44개 메서드 리팩터링이 이 작업에 들어와 설정 변경을 압도한다. 깊이 3이면 9개라 커밋 하나로 끝나고, 나머지 35개는 규칙이 이미 CI에 있는 상태에서 별도로 정리할 수 있다. `else`는 10곳뿐이라 처음부터 목표값으로 건다.

`isIgnoreFailures`는 쓰지 않는다 — 시작 임계값 자체가 완충 역할을 하므로 무시 플래그를 켤 이유가 없고, 켜두면 "언젠가 끄겠다"가 영영 안 온다.

Spotless는 `ratchetFrom("origin/dev")`로 **변경된 파일만** 검사해 baseline 문제 자체가 없다(결정 6).

### 6. 단일 PR로 진행하고 커밋으로 나눈다

작업 단위는 PR 하나이고, 리뷰 가능성은 **커밋 분리**로 확보한다. 설정 추가와 위반 정리를 한 커밋에 섞지 않는다.

| 커밋 | 내용 |
| --- | --- |
| 1 | 이 ADR + `docs/adr/index.md` |
| 2 | Spotless 플러그인 + 포맷터 + `ratchetFrom` |
| 3 | PMD 스캐폴딩 + **묶음 A**(즉시 실패) |
| 4 | **묶음 B** 규칙 추가 |
| 5 | 묶음 B 위반 40곳 정리 |
| 6 | **묶음 C** 규칙 + 시작 임계값(깊이 3 / 문장 25 / `else` 즉시) |
| 7 | 묶음 C 위반 정리 — 깊이 9개 + 길이 7개 + `else` 10곳 |
| 8 | 컨벤션 문서 강제 주체 표기 + 리뷰 렌즈 중복 제거 |
| 9 | `backend-ci.yml` 편입 |

**규칙 추가와 위반 정리는 항상 다른 커밋이다**(3↔없음, 4↔5, 6↔7). 설정 몇 줄과 코드 수십 곳이 한 커밋에 섞이면 리뷰어가 규칙 자체를 보지 못한다.

**이 결정이 Q1(Spotless 적용 범위)의 답을 좁힌다.** PR은 squash로 합쳐지므로([git-push-safety](../../../.claude/rules/git-push-safety.md)) 8개 커밋은 `dev`에서 **커밋 1개가 된다.** 전체 재포맷을 택하면 그 해시 하나에 재포맷과 룰셋 변경이 함께 들어가, `.git-blame-ignore-revs`에 넣는 순간 룰셋 변경 이력까지 blame에서 사라진다. 따라서 **단일 PR을 유지하려면 `ratchetFrom`이 사실상 유일한 선택**이고, 전체 재포맷을 원한다면 그것만 별도 PR로 떼야 한다.

`ratchetFrom`은 부작용도 작다 — 손대지 않은 파일은 그대로 두고 변경된 파일만 포맷되므로, 이 PR의 diff에 무관한 재포맷 노이즈가 섞이지 않는다.

### 7. 린트가 잡는 항목은 리뷰 렌즈에서 뺀다

`deep-review`·`code-reviewer`가 중첩·길이·`else`·`final`을 계속 지적하면 같은 문제가 CI와 리뷰 코멘트로 두 번 온다. 커밋 7에서 해당 렌즈의 체크 항목을 제거하고, `docs/conventions-production.md`·`docs/conventions-test.md`의 각 규칙에 강제 주체(PMD/ArchUnit/사람)를 표기한다. **규칙 텍스트의 SSOT는 컨벤션 문서, 판정의 SSOT는 `ruleset.xml`** 이다.

리뷰가 계속 맡는 영역도 명시한다 — 트랜잭션 스크립트 지양, 계산/사이드이펙트 분리, 도메인 로직 위치, 그리고 위에서 제외한 시간 API 주입 여부. **린트 도입의 목적은 리뷰를 줄이는 게 아니라 리뷰를 이쪽으로 몰아주는 것이다.**

## 구현 결과 (#1628)

**추정으로 세운 규칙 구성이 실측에서 여러 군데 뒤집혔다.** 아래는 결정과 달라진 것과 그 근거다.

| 초안 | 실제 | 이유 |
| --- | --- | --- |
| 묶음 A에 지역 변수 `final` 포함(위반 ≈0 추정) | **제외** | 실제 위반 **1,346곳**. `final` 901곳은 이미 붙은 것을 센 것이었다(준수율 40%). 별도 이슈로 분리 |
| 묶음 A에 필드 `final` 포함 | **제외** | non-final 필드 286곳 중 **130곳이 JPA 엔티티**. JPA가 non-final을 요구하므로 규칙화 불가 |
| `@Autowired` 필드 주입 금지(위반 0) | **프로덕션만** | 217곳 중 213곳이 테스트. 테스트의 필드 주입은 JUnit+Spring 관용이라 대상이 아니다 |
| 룰셋 1개 | **소스셋별 2개** | 위 항목의 귀결. `ruleset-main.xml` / `ruleset-test.xml`, `:test-support`의 main은 테스트 인프라라 테스트 룰셋을 적용 |
| `System.out` 금지(위반 0) | **위반 48곳 → exclude** | 전부 `QueryPerformanceTest`(test 태스크에서도 제외된 수동 벤치마크). stdout 리포트가 목적이라 파일째 제외 |
| 묶음 B에 도메인 raw 예외 금지 | **유지 (한 번 철회했다 복원)** | 아래 「예외 규칙을 두 번 뒤집은 과정」 참조 |
| 묶음 B 포괄 catch = domain+application | **domain만** | 29곳이 전부 application이고 domain은 0곳. 그 29곳은 브로드캐스트·스케줄러 콜백 실패를 삼키고 흐름을 잇는 의도된 경계다(`NunchiFlowOrchestrator.notifyQuietly`) |
| 묶음 C 위반 26곳 예상 | **13곳** | 중첩 6 + 길이 7 + `else` **0**. grep으로 세던 `else` 10곳은 then이 `return`으로 끝나지 않아 규칙 대상이 아니었다 |

### 예외 규칙을 두 번 뒤집은 과정

기록해 둘 값어치가 있는 오판이다.

1. **처음**: "도메인의 `IllegalArgumentException`은 `RestExceptionHandler:27`을 타 500이 되고 `ErrorCode` 매핑을 우회하니 버그다" → 규칙 추가.
2. **철회**: 11곳을 전수 추적하니 **사용자 입력이 닿는 경로가 0곳**이었다. 전부 내부 불변식(`RacingRange`는 상수로만 생성, `BlockStackingPlayerProgress.advanceTo`는 `BlockStackingGame:80`이 클라이언트 입력을 먼저 걸러냄 — ADR-0002)이라 500이 옳고, `BusinessException`으로 바꾸면 서버 버그가 4xx로 위장된다고 보고 규칙을 뺐다.
3. **복원**: 2번은 **`BusinessException`만 선택지로 놓은 판단**이었다. `SystemException`은 `toStatus(ErrorCode)`로 상태를 정하므로 **500짜리 `ErrorCode`를 쓰면 상태는 그대로 500**이고, 바뀌는 것은 응답·로그에 식별 코드가 붙는다는 것뿐이다. `Room:181`·`QrCode:24`·`PlayerNameGenerator:35`가 이미 그 패턴을 쓰고 있었다 — 11곳이 오히려 예외였다.

교훈: **"예외에 코드를 붙이는 것"과 "상태 코드를 낮추는 것"은 별개 축이다.** 둘을 묶어 생각하면
"코드를 붙이면 버그가 숨는다"는 잘못된 배타 선택이 만들어진다. 예외 계층이 세 갈래
(`BusinessException`·`InfrastructureException`·`SystemException`)인 이유가 이것이다.

**XPath는 전부 프로브로 검증했다.** 초안에 적었던 AST 형태가 PMD 7과 달랐다 — 한정자는 `ClassType`이 아니라
`TypeExpression/ClassType`, 어노테이션은 `FieldDeclaration/Annotation`이 아니라 `ModifierList/Annotation` 아래에 있다.
초안 그대로 넣었다면 **규칙이 아무것도 잡지 못한 채 통과했을 것이다.** 새 XPath 규칙은 반드시
"걸려야 할 코드"와 "걸리면 안 되는 코드"를 함께 넣은 프로브로 확인한 뒤 커밋한다.

**억제는 3곳이고 전부 사유 주석을 달았다** — `IpBlockFilter.doFilterInternal`(가드 순서가 보안 의미, 포스트모템 0003),
`ReportMockDataInitializer.buildMockData`(로직이 아니라 mock 데이터 표), `RacingGameTest`의 `Thread.sleep`
(`RacingGame.moveAll()`이 내부에서 `Instant.now()`를 읽는 프로덕션 제약 — 근본 해결은 별도 이슈).

**CI에는 `fetch-depth: 0`이 필요하다.** `actions/checkout` 기본값(depth 1)이면 `origin/dev` ref가 없어
`ratchetFrom`이 실패한다.

## 고려한 대안

| 대안 | 장점 | 단점 |
| --- | --- | --- |
| **Spotless + PMD (채택)** | 요구 규칙 3개를 모두 표현 가능. 커스텀 규칙이 XML 한 조각. Gradle 코어 플러그인이라 인프라 추가 없음 | 커스텀 XPath 2개를 직접 작성·검증해야 함. baseline 없음 |
| Checkstyle | 규칙 카탈로그가 넓고 자료가 많음 | 혼합 중첩을 못 잡고(`for{if{if}}` 통과) `else` 규칙이 없어, **요구사항 2개를 Java 클래스 + jar 배포로 직접 구현**해야 함 |
| SonarQube/SonarCloud | `CognitiveComplexity`가 중첩을 가중 계산. **"new code" 게이트로 baseline 문제 없음**. 대시보드 제공 | 서버·계정·토큰 운영 필요. 규칙 커스터마이즈가 제한적이라 "깊이 2"·"else 금지"를 우리 기준대로 못 만듦 |
| ErrorProne | 컴파일 시점에 실제 버그 패턴 검출 | 스타일·구조 규칙이 아예 대상이 아님. 요구사항과 무관(별건으로 검토 가능) |
| 현상 유지 (`deep-review`만) | 당장 작업 없음 | 기계 판정 가능한 규칙이 리뷰 대역폭을 계속 소모. 리뷰어 주의력에 따라 통과가 갈림. 위반이 누적되면 도입 비용이 계속 증가 |

## 트레이드오프

- **계층 한정 규칙은 경계에서 새어나간다.** 묶음 B는 `domain`·`application`에서만 금지하므로, 같은 코드가 `infra`로 옮겨가면 규칙이 따라가지 않는다. 이는 의도된 것이지만(경계에서의 포괄 catch는 정당) 패키지 이동만으로 위반이 사라지는 회피 경로가 생긴다. 전역 적용은 억제 주석 83개(catch 기준)를 부르므로 이쪽을 택한다.
- **거짓 양성을 감수한다.** 중첩 깊이 규칙은 Stream 람다 안의 `if`, 빌더 체인, switch 표현식에서 오탐을 낸다. 억제는 `@SuppressWarnings("PMD.MaxNestingDepth2")`로 하되 **억제할 때마다 이유를 주석으로 남기는 것**을 규약으로 한다. 억제가 늘면 규칙이 현실과 안 맞는다는 신호로 보고 임계값을 재검토한다.
- **린트는 "나쁜 코드"가 아니라 "긴 코드"를 잡는다.** 20줄 메서드를 5줄짜리 4개로 쪼갠 결과가 항상 더 읽기 좋은 것은 아니다. 지표를 목표로 삼으면 지표를 만족시키는 무의미한 분해가 나온다(굿하트의 법칙). 그래서 임계값을 **평균이 아니라 이상치를 잡는 선**에 두고, "왜 이 규칙이 있는가"는 컨벤션 문서에 남긴다.
- **CI 시간이 늘어난다.** `pmdMain`·`spotlessCheck`는 컴파일보다 빠르지만 0은 아니다. 테스트보다 앞에 배치해 fail-fast로 상쇄한다.
- **Spotless는 되돌리기 어렵다.** 전체 재포맷을 택하면(Q1) 그 커밋 이후 모든 diff가 새 포맷 기준이 되어 사실상 되돌릴 수 없다. 이 ADR을 코드보다 먼저 두는 이유다.
- **도구가 커버하지 못하는 규칙이 남는다.** 트랜잭션 스크립트 지양, 계산/사이드이펙트 분리, 도메인 로직 위치 같은 **설계 규칙은 여전히 사람이 본다.** 린트 도입이 리뷰를 대체하지 않으며, 오히려 리뷰가 이쪽에 집중하게 만드는 것이 목적이다.

## 결과

- `backend/build.gradle.kts` `subprojects` 블록에 `spotless`·`pmd` 플러그인이 추가되어 **전 모듈이 영향을 받는다.** 룰셋은 `backend/config/pmd/ruleset.xml` 한 곳에서 관리한다.
- `.github/workflows/backend-ci.yml`에 `spotlessCheck`·`pmdMain`이 `Run Tests` 앞에 추가된다. 로컬에서는 `./gradlew spotlessApply`로 먼저 고친다.
- `docs/conventions-production.md`의 「코드 작성 원칙」 각 항목에 강제 주체(PMD/ArchUnit/사람)를 표기한다. 규칙 텍스트는 문서, 판정은 룰셋이 SSOT다.
- `deep-review`·`code-reviewer`의 체크 항목에서 린트가 잡는 항목(중첩·길이·`else`·`final`·묶음 A·B)을 제거한다.
- 묶음 B 정리로 **도메인 예외 11곳이 `CoffeeShoutException` 계열로 교체**되어 에러 응답의 `ErrorCode` 일관성이 올라간다. 포괄 catch 29곳은 구체 예외로 좁히거나 경계 계층으로 옮긴다.
- 프론트엔드는 이 ADR의 범위 밖이다(별도 ESLint 스택 보유).
- **후속 이슈(별도)** — 클래스명·패키지·의존이 판정 기준인 규칙은 PMD가 아니라 **ArchUnit**의 몫이라 이 작업에 넣지 않는다. 대상: 테스트 지원 클래스 5패턴(`*Fixture`·`TestDataHelper`·`*Fake`·`*Dummy`·`Stub*`, `conventions-test.md:120`), Handler의 Application Service 직접 호출 금지(Stream 경유), `convertAndSend` 메서드의 `@WsTopic`/`@WsQueue` 필수, 게임별 테스트 미러 빈 존재 검증. 마지막 항목은 **포스트모템 0004에서 체크리스트 문서로 두 번 실패한 규칙**이라 우선순위가 높다.
- **후속 이슈(임계값 조임)** — 중첩 깊이 3→2, `NcssCount` 25→20. 규칙이 이미 CI에 있는 상태에서 리팩터링만 하므로 순수 코드 작업이 된다.
- **후속 이슈(지역 변수 `final`)** — 위반 1,346곳. IDE 일괄 적용이 가능한 기계적 작업이라 전용 PR로 분리한다. 이 작업을 안 할 것이라면 컨벤션에서 항목을 빼는 게 낫다 — 준수율 40%로 방치된 규칙은 리뷰에서 자의적으로 적용된다.
- **후속 이슈(`RacingGame.moveAll()` 시간 주입)** — 프로덕션이 내부에서 `Instant.now()`를 읽어 테스트가 `Thread.sleep`으로 완주 시각을 벌려야 한다. `moveAll(Instant)`로 바꾸면 억제도 사라진다.
- 후속 검토 대상: 문서 린트(`markdownlint-cli2`)를 Spotless로 흡수할지, ErrorProne을 버그 패턴 축으로 별도 도입할지.

---

## 개정 — #1716 (2026-08-28)

위 결정문은 #1628 시점의 기록이라 그대로 둔다. #1716(백엔드 규칙 기계 검증)에서 **결정 3의 전제가 테스트 소스에서는 성립하지 않는다**는 것이 드러나 범위를 나눴고, 후속 이슈로 미뤄뒀던 항목 2개가 다른 도구로 구현됐다. 어긋난 채로 두면 다음 사람이 ADR을 근거로 반대 방향을 고르게 되므로 여기 적는다.

### 결정 3 개정 — 네이밍 규칙은 **프로덕션은 ArchUnit, 테스트 소스는 PMD**

결정 3은 "패키지·계층 문맥을 아는 쪽이 ArchUnit"이라는 근거로 네이밍 규칙을 ArchUnit에 몰았다. 프로덕션 코드에서는 맞지만 **테스트 클래스에는 적용할 수 없다.**

`:app` 테스트 런타임 클래스패스에는 다른 모듈의 main jar와 testFixtures만 올라온다 — `game/src/test`, `room/src/test` 같은 **다른 모듈의 테스트 클래스는 아예 없다.** 기존 `LayerArchitectureTest`도 `ImportOption.DoNotIncludeTests`로 테스트를 빼고 돈다. 즉 "`*ServiceTest`는 베이스를 상속한다" 같은 규칙은 `:app` ArchUnit으로 **판정 자체가 불가능**하다.

모듈마다 2줄짜리 stub 상속 클래스를 두면 가능하다(테스트가 있는 모듈 11개). 그 안을 택하지 않은 이유는 **stub 누락이 곧 무검사인데 그게 침묵하기 때문**이다 — 새 모듈을 추가하면서 stub을 안 만들면 그 모듈만 조용히 규칙 밖으로 빠진다. PMD는 루트 `subprojects{}`가 이미 전 모듈 test 소스셋에 룰셋을 걸어두어 빌드 설정 변경이 0줄이고, 모듈이 늘어도 자동으로 적용된다.

결정 3이 경계한 "규칙이 두 곳으로 갈린다"는 문제는 **소스셋 경계로 나뉘므로** 남는다: 프로덕션 네이밍을 찾으면 `arch/ConventionArchitectureTest`, 테스트 네이밍을 찾으면 `ruleset-test.xml`이다. 계층이 아니라 소스셋이 기준이라 "어디 있나"가 모호하지 않다.

### 후속 이슈 4개 중 2개 처리 — 도구가 다르다

| 후속 항목 | 상태 | 실제 구현 |
| --- | --- | --- |
| 테스트 지원 클래스 5패턴 | ✅ #1716 | PMD `FixtureClassNaming`(위 결정 3 개정 적용). 판정 범위는 testFixtures 소스셋이 아니라 **`..fixture` 패키지**다 — 소스셋은 배포 단위지 역할이 아니라서, 소스셋으로 잡으면 그 안의 설정 클래스까지 걸린다 |
| 게임별 테스트 미러 빈 존재 검증 | ✅ #1716 | ArchUnit이 아니라 **순수 리플렉션 JUnit**(`app/src/test/java/coffeeshout/arch/SchedulerMirrorTest`) |
| Handler의 Application Service 직접 호출 금지 | 미착수 | ArchUnit 유지 |
| `convertAndSend`의 `@WsTopic`/`@WsQueue` 필수 | 미착수 | ArchUnit 유지 |

**미러 빈 검증에 ArchUnit을 안 쓴 이유**: 필요한 것은 `@Bean` 이름 집합 비교뿐인데, ArchUnit은 클래스패스에 올라온 형태(jar인지 클래스 디렉터리인지)에 따라 `ImportOption` 판정이 달라져 그 형태에 검증이 얽힌다. Spring의 `ClassPathScanningCandidateComponentProvider` + 리플렉션은 두 형태에서 동일하게 동작하고 40줄이면 끝난다. 다만 **`@Profile` 판정이 리플렉션 쪽에 직접 들어오므로**(메서드·클래스 레벨, 표현식 형태) 그 부분이 이 검증의 취약점이다 — #1716 리뷰에서 실제로 지적됐다.

**패턴이 5개에서 6개로 늘었다**: `*Stub`(접미)과 `*TestDataHelper`(접미)를 허용한다. 전자는 이미 두 형태가 다 쓰이고, 후자는 정확 일치로 두면 모듈이 둘 이상일 때 둘째 모듈이 이름을 못 만들어 DB 영속화 헬퍼를 순수 팩토리를 뜻하는 `*Fixture`로 부르게 된다. `conventions-test.md`도 함께 고친다.

### 결정 6 미준수 기록

"규칙 추가와 위반 정리는 항상 별도 커밋"을 #1716은 지키지 않았다 — 룰 5개, 프로덕션 리네임 3건, 테스트 메서드명 11건이 한 커밋에 들어갔다. squash merge라 커밋 분리 이득이 제한적이라고 판단했으나, 그 판단은 이 ADR이 아니라 작업자가 한 것이므로 어긋남으로 남긴다. 다음 규칙 도입 작업은 결정 6을 따르거나, 따르지 않을 것이면 결정 6을 먼저 고친다.
