import { ApiError } from '@/api/client';
import { readToken } from '@/auth/tokenStore';
import { API_BASE_URL } from '@/lib/env';

type StreamHandlers = {
  /** 도구 호출 진행 상황. 답이 나오기 전까지 여러 번 온다. */
  onProgress: (toolName: string) => void;
  onSessionId: (sessionId: number) => void;
  onResult: (answer: string) => void;
};

/**
 * ZzolBot 질문. 서버가 SSE 로 답한다.
 *
 * <p>{@code EventSource} 를 쓰지 않는다. 그쪽은 GET 만 되고 헤더를 못 붙인다. 질문은
 * 본문으로 보내야 하고 관리자 토큰은 Authorization 헤더로 가야 하므로, POST 로 열고
 * 응답 본문 스트림을 직접 읽는다. 레거시 백오피스도 같은 방식이었다.
 *
 * <p>여기서 SSE 프레이밍을 직접 파싱한다. 이벤트는 빈 줄로 끝나고, {@code data:} 는
 * 여러 줄로 쪼개져 온다. 모아서 개행으로 잇는다. 한 줄로 가정하면 줄바꿈이 있는 답변이
 * 첫 줄만 나온다.
 *
 * <p>"서버가 {@code data:} 를 한 번만 붙이니 둘째 줄부터 버려진다"는 의심이 리뷰에서
 * 나왔는데, 재현해 보니 Spring 이 개행마다 {@code data:} 를 붙이고 있었다. 답변 속 빈 줄도
 * 내용 없는 {@code data:} 로 나가서 이벤트가 거기서 끊기지 않는다. 그 바이트 모양은
 * 백엔드의 {@code SseFramingTest} 가 고정하고, 이 파서가 그걸 되돌리는 것은 옆의
 * 테스트가 같은 바이트로 확인한다.
 */
export async function askZzolBot(
  question: string,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const token = readToken();
  const response = await fetch(`${API_BASE_URL}/admin/api/zzolbot/ask`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ question }),
    signal,
  });

  if (!response.ok) {
    throw new ApiError(response.status, undefined, `ZzolBot 호출 실패 (${response.status})`);
  }
  if (!response.body) {
    throw new ApiError(response.status, undefined, '응답 본문이 비어 있습니다.');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let eventName = '';
  let dataLines: string[] = [];

  const dispatch = () => {
    if (eventName === '') {
      return;
    }
    const data = dataLines.join('\n');
    if (eventName === 'progress') {
      handlers.onProgress(data);
    } else if (eventName === 'sessionId') {
      handlers.onSessionId(Number(data));
    } else if (eventName === 'result') {
      handlers.onResult(data);
    }
    eventName = '';
    dataLines = [];
  };

  const consume = (raw: string) => {
    const line = raw.replace(/\r$/, '');
    if (line === '') {
      dispatch();
    } else if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      // "data: x" 와 "data:x" 를 모두 받는다. 앞의 공백 하나만 규격상 제거 대상이다.
      dataLines.push(line.slice(5).replace(/^ /, ''));
    }
  };

  for (;;) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });

    // 마지막 조각은 잘렸을 수 있으니 버퍼에 남긴다.
    const lines = buffer.split('\n');
    buffer = lines.pop() ?? '';
    lines.forEach(consume);
  }

  // 스트림이 끝났다. 남은 버퍼가 마지막 줄이다.
  //
  // 서버가 complete() 하면서 종료 개행을 붙이지 않는 경우가 있는데, 이걸 흘려보내면
  // 답변 전체가 사라지고 화면은 "빈 답"을 받는다. 오류도 안 나서 원인을 찾기 어렵다.
  buffer += decoder.decode();
  if (buffer !== '') {
    consume(buffer);
  }
  dispatch();
}
