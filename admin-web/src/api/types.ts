/**
 * 백엔드 응답 타입. <b>손으로 유지한다.</b>
 *
 * <p>한동안 `npm run gen:api`(springdoc → openapi-typescript)로 생성물로 갈아탈
 * 계획이 있었고 스크립트도 있었다. 걷어냈다. 세 가지가 걸렸다.
 *
 * <p>첫째, 생성하려면 서버가 떠 있어야 한다. CI 에서 백엔드를 띄워 스키마를 뽑는
 * 단계를 프론트 빌드 앞에 두면, 백엔드가 못 뜨는 날 프론트 배포까지 같이 멈춘다.
 *
 * <p>둘째, 생성 타입은 <b>모양만</b> 옮긴다. 여기 달린 "참여자는 사람이 아니라 참여
 * 건수다", "완료된 게임만 센다" 같은 주석이 이 파일의 값어치인데 생성물은 그걸 매번
 * 덮어쓴다. 화면을 만드는 사람이 실수하는 지점은 필드 이름이 아니라 <b>그 숫자가
 * 무엇을 세는가</b>다.
 *
 * <p>셋째, 백오피스 API 는 우리가 같은 PR 에서 양쪽을 고친다. 계약이 밖에서 바뀌는
 * 관계가 아니라서 자동 동기화로 얻는 게 적다.
 *
 * <p>대신 규칙이 하나 생긴다. <b>백엔드 DTO 를 고치면 여기도 같은 PR 에서 고친다.</b>
 * 안 고치면 화면이 조용히 빈칸을 그린다.
 */

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/* ── 홈 대시보드 ─────────────────────────────────────────── */

export type ActionQueue = {
  pendingReports: number;
  flaggedNicknames: number;
  pendingNicknames: number;
  blockedIps: number;
  /**
   * 발행·소비에 실패해 격리된 메시지. 다른 넷과 성격이 다르다. 신고나 검열은 사람이
   * 판단해 줄 일이고, 이건 시스템이 멈춘 것이다. 정산 메시지 하나가 격리되면 그 방의
   * 정산은 영영 안 된다.
   */
  deadLetters: number;
  hasWork: boolean;
};

/** 작업함 한 줄의 출처. 화면이 이 값으로 칩과 이동할 곳을 정한다. */
export type InboxKind = 'REPORT' | 'NICKNAME' | 'DEAD_LETTER';

/**
 * 통합 작업함 한 줄.
 *
 * <p>세 출처를 한 모양으로 맞춘 것이다. 담긴 정보가 서로 다른데도 같은 네 칸으로
 * 줄인 이유는, 목록에서 묻는 것이 늘 같기 때문이다. 무엇에 대한 일이고, 왜 올라왔고,
 * 언제 들어왔는가.
 *
 * <p>서버가 <b>최신 20건</b>만 준다. 페이지가 없다. 이 화면은 전부를 훑는 자리가 아니라
 * 다음에 처리할 것을 보는 자리고, 전체 목록은 각 화면에 있다.
 */
export type InboxItem = {
  kind: InboxKind;
  /** 출처 안에서의 식별자. 격리 메시지만 `OUTBOX:3` 처럼 출처가 앞에 붙는다. */
  id: string;
  title: string;
  /** 왜 올라왔는지. 신고는 카테고리, 닉네임과 격리는 사유다. 없을 수 있다. */
  detail: string | null;
  occurredAt: string;
};

export type Funnel = {
  created: number;
  gameStarted: number;
  /** 미니게임을 한 판이라도 끝낸 방. 게임 시작과의 차이가 하다가 나간 방이다. */
  miniGamePlayed: number;
  rouletteReached: number;
  completed: number;
  completionRate: number;
};

export type DailyTrend = {
  date: string;
  created: number;
  completed: number;
  players: number;
};

/**
 * 게임별 플레이.
 *
 * <p>`started` 와 `finished` 의 차이가 <b>중간에 끊긴 판</b>이다. `mini_game_play` 행은
 * 게임이 시작될 때 쌓이고 결과는 끝날 때 쌓이기 때문에 이 차이가 남는다. 한때 완료 판만
 * 받았는데, 그러면 중간에 끊기는 게임이 "아무도 안 고르는 게임"과 똑같이 생긴다.
 */
export type GamePlayStat = {
  miniGameType: string;
  /** 화면에 찍는 한글 이름. 서버 enum 이 들고 있는 값을 그대로 받는다. */
  label: string;
  /** 시작한 판. */
  started: number;
  /** 그중 결과가 남은 판. */
  finished: number;
  /** 전체 시작 판 대비 비중. 0.0 ~ 1.0 */
  share: number;
};

export type AdminAuditResult = 'SUCCESS' | 'FAILURE';

/**
 * 조치 이력 화면 상단 그래프.
 *
 * <p>표는 "이 조치가 무엇이었나"에 답한다. 감사 로그를 열기 전에 알아야 하는 것이 둘
 * 남는다. 실패가 늘고 있는가, 어떤 조치가 실제로 쓰이는가. 표의 필터와 이어져 있지 않다.
 * 그래프가 하는 일이 필터를 무엇으로 걸지 정하는 것이라, 함께 좁아지면 그 판단을 할
 * 근거가 사라진다.
 */
export type AdminAuditStats = {
  total: number;
  failed: number;
  /** 많은 순. action 은 매핑 패턴 그대로다. */
  actions: { action: string; count: number }[];
  /** 많은 순. 여섯 번째부터는 '그 외' 한 칸으로 묶여 온다. */
  actors: { actorEmail: string; count: number }[];
  daily: { date: string; success: number; failure: number }[];
};

/** 감사 로그 필터. 비운 값은 서버에서 조건이 걸리지 않는다. */
export type AuditLogFilters = {
  /** 부분 일치. 운영자는 이메일 전체가 아니라 앞자리만 기억한다. */
  actorEmail?: string;
  result?: AdminAuditResult;
  /** 최근 며칠. 비우면 기간 제한 없음. */
  days?: number;
  page: number;
  size?: number;
};

export type AdminAuditLog = {
  id: number;
  actorEmail: string;
  /** `DELETE /admin/api/ip-blocks/{ip}` 형태의 매핑 패턴. 화면이 문장으로 번역한다. */
  action: string;
  targetType: string | null;
  targetId: string | null;
  detail: string | null;
  result: AdminAuditResult;
  createdAt: string;
};

export type DailySummary = {
  date: string;
  funnel: Funnel;
  players: number;
  signups: number;
};

/**
 * 기간 합계. {@link DailySummary} 와 모양이 비슷하지만 따로 둔다.
 * 홈의 "오늘"은 날짜 하나를, 분석의 "최근 30일"은 구간을 말한다.
 */
export type PeriodSummary = {
  days: number;
  /** 포함 */
  from: string;
  /** 포함 */
  to: string;
  funnel: Funnel;
  players: number;
  signups: number;
  /** 분모는 생성된 방 전체다. 아무도 안 온 방도 포함이라 값이 낮으면 그게 신호다. */
  avgPlayersPerRoom: number;
};

/* ── 신고 ───────────────────────────────────────────────── */

export type ReportStatus = 'PENDING' | 'RESOLVED';

/**
 * 신고 유형. 목록 필터가 이 값을 서버로 보낸다.
 *
 * <p>{@link Report#category} 는 여전히 string 이다. 서버에 새 유형이 생겼을 때 화면이
 * 그 값을 그대로 찍어야 한다 - 알 수 없는 값을 빈칸으로 바꾸면 새 유형이 들어온 것을
 * 화면에서 알아챌 방법이 사라진다. 이 union 은 <b>우리가 고를 수 있는 것</b>의 목록이다.
 */
export type ReportCategory = 'BUG' | 'SUGGESTION' | 'GAME_REQUEST' | 'OTHER';

export type Report = {
  id: number;
  category: string;
  gameType: string | null;
  joinCode: string | null;
  content: string;
  status: ReportStatus;
  createdAt: string;
  resolvedAt: string | null;
  ip: string | null;
};

/**
 * 신고가 지금 얼마나 밀렸는가.
 *
 * <p>처리 시간의 중앙값과 p95 도 함께 받았는데 걷어냈다. 백분위는 표본이 쌓여야 뜻이
 * 생기는 수인데 신고는 하루에 몇 건이라, 한 건이 들어오고 나갈 때마다 크게 흔들렸다.
 */
export type ReportBacklog = {
  /** 아직 처리하지 않은 신고. 지금 손대야 할 건수다. */
  pendingCount: number;
  /** 가장 오래 기다린 미처리 신고의 나이(분). 없으면 0. */
  oldestPendingMinutes: number;
};

/**
 * 신고 화면 상단 그래프.
 *
 * <p>목록은 "이 신고가 무엇인가"에, 미처리 타일은 "지금 밀렸나"에 답한다. 둘 다 답하지
 * 못하는 질문이 남는다. 무엇 때문에 신고가 들어오는가. 신고의 절반이 한 게임에서 나오면
 * 그건 신고 처리로 풀 일이 아니라 그 게임을 고칠 일이다.
 */
export type ReportStats = {
  total: number;
  /** 신고가 없는 카테고리도 0으로 온다. */
  categories: { category: string; count: number }[];
  /** 많은 순. gameType 이 null 이면 게임과 무관한 신고다. */
  games: { gameType: string | null; label: string | null; count: number }[];
  /** 처리는 접수일이 아니라 처리일로 센다. */
  daily: { date: string; received: number; resolved: number }[];
};

/* ── 닉네임 검열 ─────────────────────────────────────────── */

export type NicknameAuditStatus =
  'UNAUDITED' | 'FLAGGED' | 'PENDING' | 'CLEAN' | 'ALLOWED' | 'BLOCKED';

export type NicknameAudit = {
  id: number;
  nickname: string;
  status: NicknameAuditStatus;
  confidence: { value: number };
  reason: string;
  createdAt: string;
  auditedAt: string | null;
};

export type ProfanityWord = {
  word: string;
  language: 'KOREAN' | 'ENGLISH';
  source: string;
  active: boolean;
};

export type NicknameAuditQuality = {
  total: number;
  agreed: number;
  falsePositive: number;
  falseNegative: number;
  overrideRate: number;
};

/**
 * 검열 화면 상단 그래프.
 *
 * <p>대기 목록은 지금 손이 필요한 것만 보여준다. 그 옆에 있어야 하는 것은 검열이 어디로
 * 가고 있는가다. 목록이 길어진 것이 욕이 늘어서인지 모델이 예민해져서인지는 판정 분포와
 * 신뢰도 분포를 나란히 봐야 갈린다.
 */
export type NicknameAuditStats = {
  total: number;
  /** 하나도 없는 판정도 0으로 온다. */
  statuses: { status: NicknameAuditStatus; count: number }[];
  /** flagged 는 사람이 봐야 하는 판정(FLAGGED, PENDING, BLOCKED)이다. */
  daily: { date: string; flagged: number; passed: number }[];
  /** 걸린 닉네임만 센다. 지나간 닉네임의 신뢰도는 0에 몰려 나머지를 눌러 버린다. */
  confidenceBuckets: Bucket[];
};

/* ── IP 차단 ─────────────────────────────────────────────── */

export type BlockedIp = {
  ip: string;
  remainingTtlSeconds: number;
};

/* ── 방 ──────────────────────────────────────────────────── */

export type RoomState = 'READY' | 'PLAYING' | 'SCORE_BOARD' | 'ROULETTE' | 'DONE';

export type RoomSummary = {
  id: number;
  joinCode: string;
  status: RoomState;
  createdAt: string;
  finishedAt: string | null;
  playerCount: number;
};

/**
 * 방 화면 상단 그래프.
 *
 * <p>목록만 있는 화면은 "이 방이 어땠나"에만 답한다. 그 위에 하나 더 있다. 요즘 방들이
 * 어떤 모양인가. 2인 방이 절반이면 밸런스를 2인 기준으로 봐야 하고, 끝까지 가는 방이
 * 30%면 목록을 아무리 뒤져도 그 사실은 안 보인다.
 */
export type RoomStats = {
  roomCount: number;
  games: GamePlayStat[];
  playerBuckets: Bucket[];
  /** 방이 하나도 없는 상태도 0으로 온다. 순서는 방이 거쳐 가는 순서다. */
  statuses: { status: RoomState; count: number }[];
  /** 끝난 방만 센다. 진행 중인 방을 0분으로 넣으면 방이 빨리 끝나는 것처럼 보인다. */
  durationBuckets: Bucket[];
  /** 참여자가 방장 하나뿐인 방. 칸 이름으로 찾지 않도록 서버가 따로 센다. */
  soloRoomCount: number;
};

export type RoomPlayer = {
  id: number;
  playerName: string;
  playerType: 'HOST' | 'GUEST';
  guest: boolean;
  userId: number | null;
  nickname: string | null;
  userCode: string | null;
  joinedAt: string;
};

export type RoomMiniGameResult = {
  miniGameType: string;
  playerId: number;
  playerName: string;
  rank: number;
  score: number | null;
  createdAt: string;
};

export type RoomRouletteResult = {
  winnerPlayerId: number;
  winnerPlayerName: string;
  winnerProbability: number;
  createdAt: string;
};

export type RoomDetail = {
  summary: RoomSummary;
  players: RoomPlayer[];
  miniGameResults: RoomMiniGameResult[];
  roulette: RoomRouletteResult | null;
};

/* ── 유저 ────────────────────────────────────────────────── */

export type UserSummary = {
  id: number;
  userCode: string;
  nickname: string | null;
  createdAt: string;
};

/**
 * 유저 목록 한 줄. 요약에 활동량 셋이 더 붙는다.
 *
 * <p>목록에서 판단이 끝나는 일이 많아서다. 문의 대응에서 먼저 묻는 것은 "이 사람이 얼마나,
 * 무엇을, 언제까지 했나"인데 그동안은 행을 하나씩 열어야 알 수 있었다.
 *
 * <p>당첨 수는 뺐다. 룰렛 확률의 결과라 그 사람에 대해 말해 주는 것이 거의 없다.
 */
export type UserRow = UserSummary & {
  /** 끝낸 미니게임 판 수. 방에 들어오기만 한 것은 세지 않는다. */
  playCount: number;
  /** 가장 많이 한 게임의 enum 이름. 한 판도 안 했으면 null. */
  topGame: string | null;
  /** 마지막으로 방에 들어온 때. */
  lastPlayedAt: string | null;
};

/** 분포 한 칸. 구간 경계는 서버가 정한다. 화면이 나누면 경계가 두 곳에서 관리된다. */
export type Bucket = { label: string; count: number };

/**
 * 유저 화면 상단 그래프.
 *
 * <p>{@code signups} 만 기간을 탄다. 참여도와 잔존은 회원 전체가 대상이다. 기간을 걸면
 * 그 기간에 활동한 사람만 남아, 빠져나간 사람을 묻는 그래프에서 빠져나간 사람이 사라진다.
 */
export type UserStats = {
  userCount: number;
  providers: { provider: string; count: number }[];
  /** 가입이 없는 날도 0으로 온다. 빠진 날이 있으면 선이 이어져 없던 날이 사라진다. */
  signups: { date: string; count: number }[];
  playBuckets: Bucket[];
  activityBuckets: Bucket[];
  /**
   * 한 판이라도 끝낸 회원 수와 최근 7일 안에 방에 들어온 회원 수.
   *
   * <p>칸에서 더해 쓰지 않고 서버가 따로 준다. 칸 이름은 사람이 읽는 글자라, 화면이 그
   * 글자로 칸을 찾아 합계를 내면 라벨을 한 번 다듬는 순간 숫자가 조용히 0이 된다.
   */
  playedUserCount: number;
  activeUserCount: number;
};

/**
 * 소셜 제공자 분포.
 *
 * <b>연결 수의 합은 회원 수와 다르다.</b> 한 사람이 구글과 카카오를 모두 연결할 수 있다.
 * 그래서 서버가 회원 수를 따로 준다. 합을 회원 수로 읽으면 다른 화면의 숫자와 어긋난다.
 */
export type ProviderStats = {
  /** 활성 회원 수. 탈퇴 회원은 빠진다. */
  userCount: number;
  /** 연결이 있는 제공자만. 많은 순. provider 는 소문자 google/kakao/naver. */
  providers: { provider: string; count: number }[];
};

export type UserDetail = {
  summary: UserSummary;
  providers: string[];
  roomCount: number;
  winCount: number;
  winRate: number;
};

/* ── 패치노트 ────────────────────────────────────────────── */

export type PatchNoteCategory = 'NOTICE' | 'EVENT' | 'UPDATE' | 'MAINTENANCE';

export type PatchNote = {
  id: number;
  category: PatchNoteCategory;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
};

/* ── 관리자 ──────────────────────────────────────────────── */

export type AdminAccount = {
  id: number | null;
  email: string;
  source: 'BOOTSTRAP' | 'DATABASE';
  removable: boolean;
  createdByEmail: string | null;
  createdAt: string | null;
};

export type AdminMe = { email: string };
export type AdminToken = { accessToken: string };

/* ── ZzolBot ─────────────────────────────────────────────── */

export type ZzolBotFeedback = 'GOOD' | 'BAD';

export type ZzolBotSession = {
  id: number;
  question: string;
  answer: string;
  feedback: ZzolBotFeedback | null;
  /** 서버가 "MM/dd HH:mm" 으로 이미 포맷한 문자열이다. ISO 가 아니라 Timestamp 에 못 넣는다. */
  createdAt: string;
};

export type MonitorAlert = {
  id: number;
  anomalous: boolean;
  severity: string;
  /** 원문 JSON 문자열. 서버가 파싱하지 않고 그대로 준다. */
  signalsJson: string | null;
  fingerprint: string | null;
  analysisSummary: string | null;
  suggestedActionsJson: string | null;
  notified: boolean;
  createdAt: string;
};

export type EvalRun = {
  id: number;
  label: string;
  model: string;
  status: string;
  scenarioCount: number;
  passCount: number;
  startedAt: string;
  finishedAt: string | null;
};

export type EvalResult = {
  scenarioId: number;
  accuracy: number;
  groundedness: number;
  hallucination: boolean;
  verdict: string;
  latencyMs: number;
  missingToolCalls: number;
  rationale: string | null;
  answer: string | null;
};

export type EvalRunDetail = {
  run: EvalRun;
  results: EvalResult[];
};

export type EvalScenario = {
  id: number;
  name: string;
  kind: string;
  question: string;
  rubric: string;
  sourceType: string;
  createdAt: string | null;
};

/* ── 시스템 운영 ─────────────────────────────────────────── */

export type DeadLetterSource = 'OUTBOX' | 'SETTLEMENT';

export type DeadLetter = {
  source: DeadLetterSource;
  id: number;
  /** 원본을 로그에서 되짚을 때 쓰는 값. outbox 는 스트림 키, 정산은 record_id. */
  reference: string;
  reason: string;
  /** 원문. 적체 건수는 Grafana 가 보여주지만 무엇이 왜 막혔는지는 이걸 봐야 안다. */
  payload: string | null;
  retryCount: number | null;
  /** 서버가 정한다. 프론트가 소스별 규칙표를 따로 들면 한쪽만 고치는 날이 온다. */
  requeueable: boolean;
  createdAt: string;
};

export type MigrationItem = {
  version: string | null;
  description: string;
  type: string;
  installedOn: string | null;
  success: boolean;
  executionTimeMs: number | null;
};

export type Migrations = {
  /** 이 환경이 Flyway 로 스키마를 관리하는지. 로컬은 ddl-auto 라 false 다. */
  managed: boolean;
  current: string | null;
  records: MigrationItem[];
};

export type Deployment = {
  version: string | null;
  commit: string | null;
  builtAt: string | null;
  profile: string;
};
