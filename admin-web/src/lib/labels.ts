/**
 * 서버 enum 을 화면에 쓸 한국어로 바꾼다.
 *
 * <p>지금까지는 `BUG`, `SUGGESTION` 이 표에 그대로 찍혔다. 한글 화면 한가운데 대문자
 * 영문이 서 있으면 그 열만 다른 시스템에서 온 것처럼 보이고, 운영자가 아닌 사람에게
 * 화면을 보여줄 때마다 무슨 뜻인지 설명해야 한다.
 *
 * <p>모르는 값은 <b>원문 그대로 내보낸다.</b> 빈 칸이나 "기타" 로 바꾸면 서버에 새 값이
 * 생긴 것을 화면에서 알아챌 방법이 사라진다. 낯선 영문이 보이는 편이 낫다.
 */

const REPORT_CATEGORY: Record<string, string> = {
  BUG: '버그',
  SUGGESTION: '건의',
  GAME_REQUEST: '게임 요청',
  OTHER: '기타',
};

const MINI_GAME: Record<string, string> = {
  CARD_GAME: '카드게임',
  RACING_GAME: '레이싱',
  SPEED_TOUCH: '스피드터치',
  BLIND_TIMER: '블라인드타이머',
  BLOCK_STACKING: '블록쌓기',
  LADDER_GAME: '사다리타기',
  NUNCHI_GAME: '눈치게임',
  WORM_GAME: '지렁이 게임',
};

/**
 * 작업함의 종류 표식.
 *
 * <p>짧게 둔다. 표의 첫 열이라 길면 내용 열을 밀어낸다. "격리 메시지"가 아니라 "격리"인
 * 것도 그래서다. 무엇이 격리됐는지는 바로 옆 칸이 말한다.
 */
const INBOX_KIND: Record<string, string> = {
  REPORT: '신고',
  NICKNAME: '닉네임',
  DEAD_LETTER: '격리',
};

/**
 * 닉네임 검열 대기의 두 상태.
 *
 * <p>세그먼트에 들어가는 짧은 말이라 "AI가" 를 떼었다. 세그먼트 옆 설명이 누가 걸렀는지를
 * 이미 말하고 있고, 두 칸에 같은 주어가 반복되면 정작 다른 부분이 눈에 안 들어온다.
 */
const NICKNAME_AUDIT_STATUS: Record<string, string> = {
  UNAUDITED: '판정 전',
  FLAGGED: '걸러냄',
  PENDING: '판단 못함',
  CLEAN: '정상',
  ALLOWED: '허용함',
  BLOCKED: '차단함',
};

/** 방이 어디까지 갔는지. 상태 배지가 쓴다. */
const ROOM_STATE: Record<string, string> = {
  READY: '시작 안 함',
  PLAYING: '게임 중',
  SCORE_BOARD: '점수판',
  ROULETTE: '룰렛',
  DONE: '완주',
};

/** 소셜 제공자. 서버는 registrationId 소문자를 준다. */
const PROVIDER: Record<string, string> = {
  google: '구글',
  kakao: '카카오',
  naver: '네이버',
};

/**
 * 감사 로그의 조치.
 *
 * <p>서버가 남기는 것은 요청 본문이 아니라 <b>매핑 패턴</b>이다. 본문을 남기면 로그인
 * 요청의 구글 ID 토큰이 그대로 저장되기 때문이다. 그래서 화면이 받는 값은
 * {@code POST /admin/api/reports/{id}/resolve} 같은 주소이고, 그대로 찍으면 조치 이력이
 * 사람이 읽는 기록이 아니라 서버 로그가 된다.
 *
 * <p>격리 메시지 폐기의 키에 주의한다. 실제 매핑은 출처가 경로에 들어가
 * {@code {source}} 자리를 갖는다. 한때 {@code outbox} 로 적혀 있어서 그 조치만 영영
 * 번역되지 않고 원문이 노출됐다.
 */
const AUDIT_ACTION: Record<string, string> = {
  'POST /admin/api/auth/login': '로그인',
  'POST /admin/api/accounts': '관리자 추가',
  'DELETE /admin/api/accounts/{id}': '관리자 삭제',
  'DELETE /admin/api/ip-blocks/{ip}': 'IP 차단 해제',
  'POST /admin/api/reports/{id}/resolve': '신고 처리',
  'DELETE /admin/api/reports/{id}/reporter-ip-block': '신고자 IP 해제',
  'POST /admin/api/profanity/audits/{id}/allow': '닉네임 허용',
  'POST /admin/api/profanity/audits/{id}/block': '닉네임 차단',
  'POST /admin/api/profanity/words': '금칙어 추가',
  'POST /admin/api/profanity/words/{word}/activate': '금칙어 켜기',
  'DELETE /admin/api/profanity/words/{word}/activate': '금칙어 끄기',
  'POST /admin/api/patch-notes': '패치노트 작성',
  'PUT /admin/api/patch-notes/{id}': '패치노트 수정',
  'DELETE /admin/api/patch-notes/{id}': '패치노트 삭제',
  'POST /admin/api/ops/dead-letters/outbox/{id}/requeue': '격리 메시지 재투입',
  'DELETE /admin/api/ops/dead-letters/{source}/{id}': '격리 메시지 폐기',
  'POST /admin/api/zzolbot/eval/runs': '평가 실행',
  'DELETE /admin/api/zzolbot/eval/scenarios/{id}': '평가 시나리오 삭제',
  'POST /admin/api/zzolbot/sessions/{id}/feedback': '봇 답변 평가',
};

/** 평가 시나리오의 종류. 봇에게 무엇을 시키는 상황인지. */
const EVAL_KIND: Record<string, string> = {
  CHAT: '대화',
  MONITOR: '모니터링',
};

/** 평가 시나리오가 어디서 왔는지. */
const EVAL_SOURCE: Record<string, string> = {
  MANUAL: '손으로 등록',
  RECORDED: '실환경 녹화',
  // 서버 enum 에 있는데 여기 빠져 있어 목록의 출처 열에 영문이 그대로 찍혔다.
  POSTMORTEM: '장애 회고',
};

/**
 * 알림 심각도. 알림 시스템이 주는 값을 그대로 받는다.
 *
 * <p>색을 붙이지 않는다. 이 시스템의 유채색은 코랄 하나이고 그 자리는 "손이 필요하다"가
 * 맡는다. 심각도가 셋이면 색도 셋이 되어 어느 것이 급한지가 오히려 흐려진다.
 */
const ALERT_SEVERITY: Record<string, string> = {
  critical: '심각',
  warning: '경고',
  info: '정보',
  none: '없음',
};

export function reportCategoryLabel(value: string): string {
  return REPORT_CATEGORY[value] ?? value;
}

export function inboxKindLabel(value: string): string {
  return INBOX_KIND[value] ?? value;
}

export function miniGameLabel(value: string): string {
  return MINI_GAME[value] ?? value;
}

export function nicknameAuditStatusLabel(value: string): string {
  return NICKNAME_AUDIT_STATUS[value] ?? value;
}

export function roomStateLabel(value: string): string {
  return ROOM_STATE[value] ?? value;
}

export function providerLabel(value: string): string {
  return PROVIDER[value] ?? value;
}

export function auditActionLabel(value: string): string {
  return AUDIT_ACTION[value] ?? value;
}

export function evalKindLabel(value: string): string {
  return EVAL_KIND[value] ?? value;
}

export function evalSourceLabel(value: string): string {
  return EVAL_SOURCE[value] ?? value;
}

export function alertSeverityLabel(value: string): string {
  return ALERT_SEVERITY[value?.toLowerCase()] ?? value;
}
