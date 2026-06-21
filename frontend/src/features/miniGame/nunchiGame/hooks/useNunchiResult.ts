import useFetch from '@/apis/rest/useFetch';
import { NunchiResultEntry, NunchiResultResponse } from '@/types/miniGame/nunchiGame';

/**
 * 눈치게임 3계층 결과 조회(요구사항 I, ADR 구현 노트 N7 — BE 확정).
 *
 * 기존 결과 페이지(MiniGameResultPage)는 단순 rank 나열이라 재사용 불가.
 * rank 숫자만으로는 충돌/미입력 그룹 구분이 안 되므로 BE 가 각 사람의 tier 를 함께 노출한다.
 * FE 는 같은 rank 를 묶고 tier 배지를 렌더한다.
 *
 * 전송(확정): nunchi 전용 REST — `GET /minigames/nunchi/result?joinCode={joinCode}`.
 * 응답: { results: [{ playerName, rank, tier }] } (NunchiResultResponse).
 * 호출부는 기존 결과 페이지(MiniGameResultPage)와 동일한 useFetch 패턴을 따른다.
 *
 * @returns 결과 엔트리 배열(tier 별 그룹화 가능) + 로딩 상태.
 */
export const useNunchiResult = (joinCode: string) => {
  const { data, loading } = useFetch<NunchiResultResponse>({
    endpoint: `/minigames/nunchi/result?joinCode=${joinCode}`,
    enabled: !!joinCode,
  });

  const results: NunchiResultEntry[] = data?.results ?? [];

  return { results, loading };
};
