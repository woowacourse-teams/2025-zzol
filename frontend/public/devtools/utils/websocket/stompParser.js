/* eslint-env browser */

/**
 * 지원하는 STOMP 명령어 목록
 */
const SUPPORTED_STOMP_COMMANDS = [
  'CONNECT',
  'CONNECTED',
  'SEND',
  'SUBSCRIBE',
  'UNSUBSCRIBE',
  'MESSAGE',
  'RECEIPT',
  'ERROR',
  'DISCONNECT',
];

/**
 * 문자열의 이스케이프 문자를 실제 문자로 변환합니다.
 * 유니코드 이스케이프(\u0000 등)와 일반 이스케이프 문자(\n, \t 등)를 처리합니다.
 *
 * @param {string} str - 이스케이프 문자를 포함한 문자열
 * @returns {string} 이스케이프가 해제된 문자열
 */
const unescapeString = (str) => {
  // 유니코드 이스케이프 먼저 처리 (\u0000 등)
  str = str.replace(/\\u([0-9a-fA-F]{4})/g, (match, hex) => {
    return String.fromCharCode(parseInt(hex, 16));
  });
  // 일반 이스케이프 문자 처리
  return str.replace(/\\(.)/g, (match, char) => {
    if (char === 'n') return '\n';
    if (char === 'r') return '\r';
    if (char === 't') return '\t';
    if (char === '\\') return '\\';
    if (char === '"') return '"';
    if (char === "'") return String.fromCharCode(39);
    return char;
  });
};

/**
 * 서버에서 클라이언트로 전송되는 STOMP 메시지 프레임을 추출합니다.
 * SockJS가 배열 형태로 감싸서 전달하는 형식을 처리합니다.
 *
 * @param {string} rawData - 원본 데이터 문자열
 * @returns {string|null} 추출된 STOMP 프레임 또는 null
 */
const extractServerToClientFrame = (rawData) => {
  // a["..."] 형식 (서버→클라이언트, SockJS 포맷)
  const doubleQuotePattern = /^a\["((?:[^"\\]|\\.)*)"\]$/;
  // a['...'] 형식
  const singleQuotePattern = /^a\['((?:[^'\\]|\\.)*)'\]$/;
  // ["..."] 형식 (배열 문자열, 클라이언트→서버에서도 사용)
  const arrayDoubleQuotePattern = /^\["((?:[^"\\]|\\.)*)"\]$/;
  // ['...'] 형식
  const arraySingleQuotePattern = /^\['((?:[^'\\]|\\.)*)'\]$/;

  let match = rawData.match(doubleQuotePattern);
  if (match) {
    return unescapeString(match[1]);
  }

  match = rawData.match(singleQuotePattern);
  if (match) {
    return unescapeString(match[1]);
  }

  match = rawData.match(arrayDoubleQuotePattern);
  if (match) {
    return unescapeString(match[1]);
  }

  match = rawData.match(arraySingleQuotePattern);
  if (match) {
    return unescapeString(match[1]);
  }

  return null;
};

/**
 * 클라이언트에서 서버로 전송되는 직접 STOMP 프레임 형식을 확인합니다.
 * 예: "SEND\ndestination:/app/test\n\nHello\x00"
 *
 * @param {string} rawData - 원본 데이터 문자열
 * @returns {string|null} STOMP 프레임이면 원본 데이터, 아니면 null
 */
const extractClientToServerFrame = (rawData) => {
  const firstLine = rawData.split('\n')[0].trim();
  if (SUPPORTED_STOMP_COMMANDS.includes(firstLine)) {
    return rawData;
  }
  return null;
};

/**
 * STOMP 프레임에서 헤더와 본문을 분리합니다.
 *
 * @param {string} stompFrame - STOMP 프레임 문자열
 * @returns {{headersPart: string, bodyPart: string}} 헤더와 본문으로 분리된 객체
 */
const splitHeaderAndBody = (stompFrame) => {
  const headerBodySplit = stompFrame.indexOf('\n\n');

  if (headerBodySplit === -1) {
    // 본문이 없는 경우 (빈 줄 없음)
    return {
      headersPart: stompFrame,
      bodyPart: '',
    };
  }

  let headersPart = stompFrame.substring(0, headerBodySplit);
  let bodyPart = stompFrame.substring(headerBodySplit + 2);

  // 끝의 null 문자 제거 (STOMP 프레임 종료 문자)
  if (bodyPart.length > 0 && bodyPart.charCodeAt(bodyPart.length - 1) === 0) {
    bodyPart = bodyPart.substring(0, bodyPart.length - 1);
  }

  return {
    headersPart,
    bodyPart,
  };
};

/**
 * STOMP 헤더 문자열을 파싱하여 객체로 변환합니다.
 *
 * @param {string} headersPart - 헤더 부분 문자열
 * @returns {{command: string, headers: Record<string, string>}|null} 파싱된 명령어와 헤더 객체 또는 null
 */
const parseStompHeaders = (headersPart) => {
  const headerLines = headersPart.split('\n');
  if (headerLines.length === 0) return null;

  const command = headerLines[0].trim();

  // 지원하는 명령어인지 확인
  if (!SUPPORTED_STOMP_COMMANDS.includes(command)) {
    return null;
  }

  const headers = {};
  for (let i = 1; i < headerLines.length; i++) {
    const line = headerLines[i].trim();
    if (!line) continue;

    const colonIndex = line.indexOf(':');
    if (colonIndex > 0) {
      const key = line.substring(0, colonIndex).trim();
      const value = line.substring(colonIndex + 1).trim();
      headers[key] = value;
    }
  }

  // 명령어도 헤더에 포함
  headers['command'] = command;

  return {
    command,
    headers,
  };
};

/**
 * STOMP 메시지를 파싱합니다.
 * 서버→클라이언트: a["MESSAGE\ndestination:...\n\nbody\u0000"] 형식
 * 클라이언트→서버: SEND\ndestination:...\n\nbody\x00 직접 프레임 형식
 *
 * @param {string} rawData - 파싱할 원본 데이터
 * @returns {{headers: Record<string, string>, body: string, rawData: string}|null} 파싱된 STOMP 메시지 또는 null
 */
export const parseStompMessage = (rawData) => {
  try {
    if (typeof rawData !== 'string') return null;

    // 서버→클라이언트 형식 (배열 포맷) 먼저 확인
    let stompFrame = extractServerToClientFrame(rawData);

    // 배열 포맷이 아니면 클라이언트→서버 직접 프레임 형식 확인
    if (!stompFrame) {
      stompFrame = extractClientToServerFrame(rawData);
      if (!stompFrame) {
        return null;
      }
    }

    // 헤더와 본문 분리
    const { headersPart, bodyPart } = splitHeaderAndBody(stompFrame);

    // 헤더 파싱
    const parsed = parseStompHeaders(headersPart);
    if (!parsed) {
      return null;
    }

    return {
      headers: parsed.headers,
      body: bodyPart,
      rawData,
    };
  } catch {
    // 파싱 실패 시 null 반환
    return null;
  }
};

