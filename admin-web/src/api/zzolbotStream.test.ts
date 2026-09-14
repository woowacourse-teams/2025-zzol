import { afterEach, describe, expect, it, vi } from 'vitest';
import { askZzolBot } from '@/api/zzolbotStream';

/**
 * SSE 프레이밍을 직접 파싱하는 코드라 청크가 어떻게 쪼개져 오느냐에 따라 깨진다.
 * 실제로 네트워크는 이벤트 경계를 지켜서 끊어 주지 않는다.
 */
function streamOf(...chunks: string[]): Response {
  const encoder = new TextEncoder();
  return {
    ok: true,
    status: 200,
    body: new ReadableStream<Uint8Array>({
      start(controller) {
        for (const chunk of chunks) {
          controller.enqueue(encoder.encode(chunk));
        }
        controller.close();
      },
    }),
  } as unknown as Response;
}

function collect() {
  const progress: string[] = [];
  let sessionId: number | null = null;
  let answer: string | null = null;
  return {
    progress,
    get sessionId() {
      return sessionId;
    },
    get answer() {
      return answer;
    },
    handlers: {
      onProgress: (tool: string) => progress.push(tool),
      onSessionId: (id: number) => {
        sessionId = id;
      },
      onResult: (text: string) => {
        answer = text;
      },
    },
  };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('askZzolBot', () => {
  it('이벤트 이름별로 콜백을 나눠 부른다', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          streamOf(
            'event: progress\ndata: countRooms\n\n',
            'event: sessionId\ndata: 42\n\n',
            'event: result\ndata: 어제 방은 12개였습니다\n\n',
          ),
        ),
    );

    const sink = collect();
    await askZzolBot('어제 방 몇 개?', sink.handlers);

    expect(sink.progress).toEqual(['countRooms']);
    expect(sink.sessionId).toBe(42);
    expect(sink.answer).toBe('어제 방은 12개였습니다');
  });

  it('data 가 여러 줄이면 개행으로 이어 붙인다', async () => {
    // 한 줄로 가정하면 줄바꿈 있는 답변이 첫 줄만 나온다.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamOf('event: result\ndata: 첫 줄\ndata: 둘째 줄\n\n')),
    );

    const sink = collect();
    await askZzolBot('q', sink.handlers);

    expect(sink.answer).toBe('첫 줄\n둘째 줄');
  });

  /**
   * 위 테스트는 우리가 지어낸 입력을 우리가 파싱한다. 서버가 정말 그 모양으로 보내는지는
   * 알 수 없어서, 실제로 나가는 바이트를 그대로 넣어 본다. 그 바이트는 백엔드의
   * SseFramingTest 가 고정하고 있다. 공백 없는 `data:` 이고 줄마다 하나씩 붙는다.
   */
  it('서버가 내보내는 바이트를 그대로 받아 답을 복원한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamOf('event:result\ndata:첫 줄\ndata:둘째 줄\n\n')),
    );

    const sink = collect();
    await askZzolBot('q', sink.handlers);

    expect(sink.answer).toBe('첫 줄\n둘째 줄');
  });

  it('답변 속 빈 줄도 그대로 돌아온다', async () => {
    // 서버는 빈 줄을 내용 없는 data: 로 내보낸다. 진짜 빈 줄이었다면 그 자리가 이벤트
    // 끝으로 읽혀 앞부분만 답이 된다. LLM 답변은 대개 문단이 여럿이라 늘 걸리는 자리다.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamOf('event:result\ndata:가\ndata:\ndata:나\n\n')),
    );

    const sink = collect();
    await askZzolBot('q', sink.handlers);

    expect(sink.answer).toBe('가\n\n나');
  });

  it('청크가 이벤트 한가운데서 끊겨도 이어 붙인다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamOf('event: res', 'ult\ndata: 나눠', '져서 왔다\n\n')),
    );

    const sink = collect();
    await askZzolBot('q', sink.handlers);

    expect(sink.answer).toBe('나눠져서 왔다');
  });

  it('마지막 이벤트가 빈 줄로 끝나지 않아도 흘린다', async () => {
    // 서버가 complete() 하면서 종료 개행 없이 스트림을 닫는 경우가 있다.
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(streamOf('event: result\ndata: 끝맺음 없음')));

    const sink = collect();
    await askZzolBot('q', sink.handlers);

    expect(sink.answer).toBe('끝맺음 없음');
  });

  it('data 뒤 공백은 하나만 걷어낸다', async () => {
    // 규격상 제거 대상은 공백 하나다. 두 개를 지우면 들여쓴 답변이 망가진다.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamOf('event: result\ndata:   들여쓴 줄\n\n')),
    );

    const sink = collect();
    await askZzolBot('q', sink.handlers);

    expect(sink.answer).toBe('  들여쓴 줄');
  });

  it('CRLF 로 와도 이벤트 이름에 캐리지리턴이 붙지 않는다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(streamOf('event: progress\r\ndata: countRooms\r\n\r\n')),
    );

    const sink = collect();
    await askZzolBot('q', sink.handlers);

    expect(sink.progress).toEqual(['countRooms']);
  });

  it('실패 응답이면 던진다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 503, body: null } as Response),
    );

    await expect(askZzolBot('q', collect().handlers)).rejects.toThrow('503');
  });
});
