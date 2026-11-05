import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/api/client';
import type {
  ActionQueue,
  AdminAccount,
  AdminAuditLog,
  AdminAuditStats,
  AuditLogFilters,
  BlockedIp,
  DailySummary,
  DailyTrend,
  DeadLetter,
  DeadLetterSource,
  Deployment,
  EvalRun,
  EvalRunDetail,
  EvalScenario,
  GamePlayStat,
  InboxItem,
  Migrations,
  MonitorAlert,
  NicknameAudit,
  NicknameAuditQuality,
  NicknameAuditStats,
  NicknameAuditStatus,
  PageResponse,
  PatchNote,
  PatchNoteCategory,
  PeriodSummary,
  ProfanityWord,
  ProviderStats,
  Report,
  ReportBacklog,
  ReportCategory,
  ReportStats,
  ReportStatus,
  RoomDetail,
  RoomStats,
  RoomSummary,
  UserDetail,
  UserRow,
  UserStats,
  ZzolBotFeedback,
  ZzolBotSession,
} from '@/api/types';
/**
 * 쿼리 키를 한 곳에 모은다. 조치 후 무엇을 다시 불러올지 정할 때 문자열을 흩어 두면
 * 화면 하나를 고칠 때마다 갱신이 빠지는 곳이 생긴다.
 */
export const keys = {
  overview: {
    queue: ['overview', 'queue'] as const,
    summary: (date?: string) => ['overview', 'summary', date ?? 'today'] as const,
  },
  reports: {
    list: (filters: unknown) => ['reports', 'list', filters] as const,
    backlog: ['reports', 'backlog'] as const,
  },
  profanity: {
    audits: (status: NicknameAuditStatus, page: number) =>
      ['profanity', 'audits', status, page] as const,
    words: (filters: unknown) => ['profanity', 'words', filters] as const,
    quality: (days: number) => ['profanity', 'quality', days] as const,
    samples: (page: number) => ['profanity', 'samples', page] as const,
  },
  inbox: ['inbox'] as const,
  ipBlocks: ['ip-blocks'] as const,
  rooms: {
    search: (joinCode: string, page: number) => ['rooms', 'search', joinCode, page] as const,
    detail: (roomId: number) => ['rooms', 'detail', roomId] as const,
    stats: (days: number) => ['rooms', 'stats', days] as const,
  },
  users: {
    search: (keyword: string, page: number) => ['users', 'search', keyword, page] as const,
    detail: (userId: number) => ['users', 'detail', userId] as const,
    providers: ['users', 'providers'] as const,
    stats: (days: number) => ['users', 'stats', days] as const,
  },
  patchNotes: ['patch-notes'] as const,
  admins: ['admins'] as const,
};
/* ── 홈 ─────────────────────────────────────────────────── */
export function useActionQueue() {
  return useQuery({
    queryKey: keys.overview.queue,
    queryFn: () => api.get<ActionQueue>('/overview/action-queue'),
    // 조치하고 돌아오면 바로 줄어들어야 한다. 30초면 표를 훑는 동안 한 번은 갱신된다.
    refetchInterval: 30_000,
  });
}
/**
 * 통합 작업함. 신고와 닉네임 검열과 격리 메시지를 한 목록으로 받는다.
 *
 * <p>큐와 같은 주기로 갱신한다. 둘은 같은 것을 다르게 말하는 사이라, 배지는 줄었는데
 * 목록에는 그 줄이 남아 있으면 조치가 안 먹은 것으로 읽힌다.
 */
export function useInbox() {
  return useQuery({
    queryKey: keys.inbox,
    queryFn: () => api.get<InboxItem[]>('/inbox'),
    refetchInterval: 30_000,
  });
}

export function useTrend(days = 14) {
  return useQuery({
    queryKey: ['overview', 'trend', days],
    queryFn: () => api.get<DailyTrend[]>('/overview/trend', { days }),
  });
}
export function usePeriodSummary(days = 30) {
  return useQuery({
    queryKey: ['overview', 'period', days],
    queryFn: () => api.get<PeriodSummary>('/overview/period', { days }),
  });
}
export function useGamePlayStats(days = 30) {
  return useQuery({
    queryKey: ['overview', 'games', days],
    queryFn: () => api.get<GamePlayStat[]>('/overview/games', { days }),
  });
}
/** 조치 이력 화면 상단 그래프. 표의 필터와 이어지지 않는다. */
export function useAuditLogStats(days = 30) {
  return useQuery({
    queryKey: ['audit-logs', 'stats', days] as const,
    queryFn: () => api.get<AdminAuditStats>('/audit-logs/stats', { days }),
  });
}
export function useAuditLogs(filters: AuditLogFilters) {
  return useQuery({
    queryKey: ['audit-logs', filters],
    queryFn: () => api.get<PageResponse<AdminAuditLog>>('/audit-logs', { ...filters }),
  });
}
export function useDailySummary(date?: string) {
  return useQuery({
    queryKey: keys.overview.summary(date),
    queryFn: () => api.get<DailySummary>('/overview/summary', { date }),
  });
}
/* ── 신고 ───────────────────────────────────────────────── */
type ReportFilters = {
  status?: ReportStatus;
  category?: ReportCategory;
  gameType?: string;
  page: number;
};
export function useReports(filters: ReportFilters) {
  return useQuery({
    queryKey: keys.reports.list(filters),
    queryFn: () => api.get<PageResponse<Report>>('/reports', { ...filters }),
  });
}
/** 신고 화면 상단 그래프. SLA 와 따로 부른다. 저쪽은 처리할 때마다 다시 부른다. */
export function useReportStats(days = 30) {
  return useQuery({
    queryKey: ['quality', 'report-stats', days] as const,
    queryFn: () => api.get<ReportStats>('/quality/report-stats', { days }),
  });
}
/** 기간 인자가 없다. 미처리는 지금 쌓여 있는 것이고 거기에 기간은 뜻이 없다. */
export function useReportBacklog() {
  return useQuery({
    queryKey: keys.reports.backlog,
    queryFn: () => api.get<ReportBacklog>('/quality/report-backlog'),
  });
}
export function useResolveReport() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.post<void>(`/reports/${id}/resolve`),
    onSuccess: () => {
      // 목록과 대기 큐를 함께 갱신한다. 처리했는데 상단 숫자가 그대로면
      // 조치가 안 먹은 것으로 오해한다.
      client.invalidateQueries({ queryKey: ['reports'] });
      client.invalidateQueries({ queryKey: keys.overview.queue });
    },
  });
}
export function useUnblockReporterIp() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.delete<void>(`/reports/${id}/reporter-ip-block`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: keys.ipBlocks });
      client.invalidateQueries({ queryKey: keys.overview.queue });
    },
  });
}
/* ── 닉네임 검열 ─────────────────────────────────────────── */
export function useNicknameAudits(status: NicknameAuditStatus, page: number) {
  return useQuery({
    queryKey: keys.profanity.audits(status, page),
    queryFn: () => api.get<PageResponse<NicknameAudit>>('/profanity/audits', { status, page }),
  });
}
export function useAuditDecision() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, decision }: { id: number; decision: 'allow' | 'block' }) =>
      api.post<void>(`/profanity/audits/${id}/${decision}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['profanity'] });
      client.invalidateQueries({ queryKey: keys.overview.queue });
    },
  });
}
/** AI가 CLEAN으로 통과시킨 닉네임 중 검토용으로 뽑힌 것. 결정하면 목록에서 빠진다. */
export function useNicknameSamples(page: number) {
  return useQuery({
    queryKey: keys.profanity.samples(page),
    queryFn: () => api.get<PageResponse<NicknameAudit>>('/profanity/samples', { page }),
  });
}
/** 미탐은 차단과 같은 경로를 타 사전에 오른다. 그래서 사전과 품질 지표까지 다시 불러온다. */
export function useSampleDecision() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, decision }: { id: number; decision: 'ok' | 'miss' }) =>
      api.post<void>(`/profanity/samples/${id}/${decision}`),
    onSuccess: () => client.invalidateQueries({ queryKey: ['profanity'] }),
  });
}
type WordFilters = {
  search?: string;
  language?: string;
  source?: string;
  active?: boolean;
  page: number;
};
export function useProfanityWords(filters: WordFilters) {
  return useQuery({
    queryKey: keys.profanity.words(filters),
    queryFn: () => api.get<PageResponse<ProfanityWord>>('/profanity/words', { ...filters }),
  });
}
export function useAddProfanityWord() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (body: { word: string; language: ProfanityWord['language'] }) =>
      api.post<void>('/profanity/words', body),
    onSuccess: () => client.invalidateQueries({ queryKey: ['profanity', 'words'] }),
  });
}
/**
 * 단어를 지우지 않고 비활성으로 둔다. 삭제하면 왜 걸렸던 단어인지가 사라져
 * 같은 단어를 두 번 추가하고 두 번 푸는 일이 반복된다.
 */
export function useToggleProfanityWord() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ word, active }: { word: string; active: boolean }) =>
      active
        ? api.post<void>(`/profanity/words/${encodeURIComponent(word)}/activate`)
        : api.delete<void>(`/profanity/words/${encodeURIComponent(word)}/activate`),
    onSuccess: () => client.invalidateQueries({ queryKey: ['profanity', 'words'] }),
  });
}
export function useNicknameAuditStats(days = 30) {
  return useQuery({
    queryKey: ['quality', 'nickname-audit-stats', days] as const,
    queryFn: () => api.get<NicknameAuditStats>('/quality/nickname-audit-stats', { days }),
  });
}
export function useNicknameAuditQuality(days = 30) {
  return useQuery({
    queryKey: keys.profanity.quality(days),
    queryFn: () => api.get<NicknameAuditQuality>('/quality/nickname-audit', { days }),
  });
}
/* ── IP 차단 ─────────────────────────────────────────────── */
export function useBlockedIps() {
  return useQuery({
    queryKey: keys.ipBlocks,
    queryFn: () => api.get<BlockedIp[]>('/ip-blocks'),
  });
}
export function useUnblockIp() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (ip: string) => api.delete<void>(`/ip-blocks/${encodeURIComponent(ip)}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: keys.ipBlocks });
      client.invalidateQueries({ queryKey: keys.overview.queue });
    },
  });
}
/* ── 방 ──────────────────────────────────────────────────── */
export function useRoomSearch(joinCode: string, page: number) {
  return useQuery({
    queryKey: keys.rooms.search(joinCode, page),
    queryFn: () => api.get<PageResponse<RoomSummary>>('/rooms', { joinCode, page }),
  });
}
/** 방 화면 상단 그래프. 기간 안의 방을 한 줄씩 읽으므로 서버가 기간에 상한을 건다. */
export function useRoomStats(days: number) {
  return useQuery({
    queryKey: keys.rooms.stats(days),
    queryFn: () => api.get<RoomStats>('/rooms/stats', { days }),
  });
}
export function useRoomDetail(roomId: number | null) {
  return useQuery({
    queryKey: keys.rooms.detail(roomId ?? 0),
    queryFn: () => api.get<RoomDetail>(`/rooms/${roomId}`),
    // 패널이 닫혀 있으면 열린 방이 없다. 막지 않으면 /rooms/0 을 불러 404 가 난다.
    enabled: roomId !== null,
  });
}
/* ── 유저 ────────────────────────────────────────────────── */
export function useUserSearch(keyword: string, page: number) {
  return useQuery({
    queryKey: keys.users.search(keyword, page),
    queryFn: () => api.get<PageResponse<UserRow>>('/users', { keyword, page }),
  });
}
/** 유저 화면 상단 그래프. days 는 가입 추이에만 걸린다. */
export function useUserStats(days: number) {
  return useQuery({
    queryKey: keys.users.stats(days),
    queryFn: () => api.get<UserStats>('/users/stats', { days }),
  });
}
export function useProviderStats() {
  return useQuery({
    queryKey: keys.users.providers,
    queryFn: () => api.get<ProviderStats>('/users/providers'),
  });
}
export function useUserDetail(userId: number | null) {
  return useQuery({
    queryKey: keys.users.detail(userId ?? 0),
    queryFn: () => api.get<UserDetail>(`/users/${userId}`),
    enabled: userId !== null,
  });
}
/* ── 패치노트 ────────────────────────────────────────────── */
export function usePatchNotes() {
  return useQuery({
    queryKey: keys.patchNotes,
    queryFn: () => api.get<PatchNote[]>('/patch-notes'),
  });
}
export function usePatchNote(id: number | null) {
  return useQuery({
    queryKey: ['patch-notes', 'detail', id],
    queryFn: () => api.get<PatchNote>(`/patch-notes/${id}`),
    // 새로 쓰는 화면에서는 부를 대상이 없다.
    enabled: id !== null,
  });
}
/**
 * 카테고리 선택지는 서버가 준다. 프론트에 enum 을 복사해 두면 서버가 값을 늘려도
 * 화면에 안 나오고, 반대로 없앤 값이 화면에 남아 저장할 때만 터진다.
 */
export function usePatchNoteCategories() {
  return useQuery({
    queryKey: ['patch-notes', 'categories'],
    queryFn: () => api.get<PatchNoteCategory[]>('/patch-notes/categories'),
    staleTime: Infinity,
  });
}
export type PatchNoteForm = {
  category: PatchNoteCategory;
  title: string;
  content: string;
};
export function useSavePatchNote() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, form }: { id: number | null; form: PatchNoteForm }) =>
      id === null
        ? api.post<PatchNote>('/patch-notes', form)
        : api.put<PatchNote>(`/patch-notes/${id}`, form),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.patchNotes }),
  });
}
export function useDeletePatchNote() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.delete<void>(`/patch-notes/${id}`),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.patchNotes }),
  });
}
/* ── 관리자 ──────────────────────────────────────────────── */
export function useAdminAccounts() {
  return useQuery({
    queryKey: keys.admins,
    queryFn: () => api.get<AdminAccount[]>('/accounts'),
  });
}
export function useAddAdminAccount() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (email: string) => api.post<AdminAccount>('/accounts', { email }),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.admins }),
  });
}
export function useRemoveAdminAccount() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.delete<void>(`/accounts/${id}`),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.admins }),
  });
}
/* ── ZzolBot ─────────────────────────────────────────────── */
export function useZzolBotSessions() {
  return useQuery({
    queryKey: ['zzolbot', 'sessions'],
    queryFn: () => api.get<ZzolBotSession[]>('/zzolbot/sessions'),
  });
}
export function useZzolBotFeedback() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, feedback }: { id: number; feedback: ZzolBotFeedback }) =>
      api.post<void>(`/zzolbot/sessions/${id}/feedback`, { feedback }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['zzolbot', 'sessions'] }),
  });
}
/**
 * 모니터링 알림. 무인 분석 결과라 화면을 열어 둔 사이에도 새 건이 쌓인다.
 * 30초마다 다시 읽는다. 더 짧게 잡으면 Gemini 분석 주기보다 빨라 의미가 없다.
 */
export function useMonitorAlerts() {
  return useQuery({
    queryKey: ['zzolbot', 'alerts'],
    queryFn: () => api.get<MonitorAlert[]>('/zzolbot/monitor/alerts'),
    refetchInterval: 30_000,
  });
}
export function useEvalRuns() {
  return useQuery({
    queryKey: ['zzolbot', 'eval', 'runs'],
    queryFn: () => api.get<EvalRun[]>('/zzolbot/eval/runs'),
  });
}
export function useEvalRunDetail(id: number | null) {
  return useQuery({
    queryKey: ['zzolbot', 'eval', 'runs', id],
    queryFn: () => api.get<EvalRunDetail>(`/zzolbot/eval/runs/${id}`),
    enabled: id !== null,
  });
}
export function useEvalScenarios() {
  return useQuery({
    queryKey: ['zzolbot', 'eval', 'scenarios'],
    queryFn: () => api.get<EvalScenario[]>('/zzolbot/eval/scenarios'),
  });
}
/**
 * 평가 실행. 서버가 202 로 받고 뒤에서 돈다.
 *
 * <p>이미 도는 중이면 409 다. 동시에 두 번 돌리면 Gemini 호출이 겹쳐 비용도 두 배고
 * 결과도 서로 섞인다. 화면은 409 를 실패가 아니라 "이미 돌고 있다"로 읽어야 한다.
 */
export function useStartEvalRun() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (body: { label: string; repeats?: number; kind?: string }) =>
      api.post<void>('/zzolbot/eval/runs', body),
    onSuccess: () => client.invalidateQueries({ queryKey: ['zzolbot', 'eval', 'runs'] }),
  });
}
export function useDeleteEvalScenario() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.delete<void>(`/zzolbot/eval/scenarios/${id}`),
    onSuccess: () => client.invalidateQueries({ queryKey: ['zzolbot', 'eval', 'scenarios'] }),
  });
}
/* ── 시스템 운영 ─────────────────────────────────────────── */
export function useDeadLetters(source: DeadLetterSource, page = 0) {
  return useQuery({
    queryKey: ['system', 'dead-letters', source, page],
    queryFn: () => api.get<PageResponse<DeadLetter>>('/system/dead-letters', { source, page }),
  });
}
/**
 * 다시 큐에 넣기. outbox 만 된다.
 *
 * <p>성공하면 목록과 홈의 대기 큐를 함께 무효화한다. 큐 숫자가 그대로면 조치가 안 먹은
 * 것으로 보이고, 그러면 같은 버튼을 한 번 더 누르게 된다.
 */
export function useRequeueDeadLetter() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => api.post<void>(`/system/dead-letters/outbox/${id}/requeue`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['system', 'dead-letters'] });
      client.invalidateQueries({ queryKey: keys.overview.queue });
    },
  });
}
export function useDiscardDeadLetter() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ source, id }: { source: DeadLetterSource; id: number }) =>
      api.delete<void>(`/system/dead-letters/${source}/${id}`),
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['system', 'dead-letters'] });
      client.invalidateQueries({ queryKey: keys.overview.queue });
    },
  });
}
export function useMigrations() {
  return useQuery({
    queryKey: ['system', 'migrations'],
    queryFn: () => api.get<Migrations>('/system/migrations'),
    // 배포 중이 아니면 안 바뀐다. 화면을 열어 둔 채로 다시 물을 이유가 없다.
    staleTime: 5 * 60_000,
  });
}
export function useDeployment() {
  return useQuery({
    queryKey: ['system', 'deployment'],
    queryFn: () => api.get<Deployment>('/system/deployment'),
    staleTime: Infinity,
  });
}
