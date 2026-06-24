/**
 * JSON이 끝나는 정확한 위치를 찾습니다.
 * 중괄호와 대괄호를 카운팅하여 올바른 JSON 경계를 찾습니다.
 *
 * @param text - 검색할 텍스트
 * @param startIndex - 검색 시작 위치
 * @returns JSON이 끝나는 위치 또는 -1
 */
const findJsonEnd = (text: string, startIndex: number): number => {
  let braceCount = 0;
  let bracketCount = 0;
  let inString = false;
  let escapeNext = false;

  for (let i = startIndex; i < text.length; i++) {
    const char = text[i];
    const charCode = text.charCodeAt(i);

    // null 문자를 만나면 JSON이 끝난 것으로 간주
    if (charCode === 0) {
      if (braceCount === 0 && bracketCount === 0) {
        return i;
      }
    }

    if (escapeNext) {
      escapeNext = false;
      continue;
    }

    if (char === '\\') {
      escapeNext = true;
      continue;
    }

    if (char === '"') {
      inString = !inString;
      continue;
    }

    if (inString) continue;

    if (char === '{') {
      braceCount++;
    } else if (char === '}') {
      braceCount--;
      if (braceCount === 0 && bracketCount === 0) {
        return i + 1;
      }
    } else if (char === '[') {
      bracketCount++;
    } else if (char === ']') {
      bracketCount--;
      if (braceCount === 0 && bracketCount === 0) {
        return i + 1;
      }
    }
  }

  return -1;
};

/**
 * STOMP 메시지 본문에서 구조화된 payload를 파싱합니다.
 * {success, data, errorMessage} 형태의 JSON을 추출합니다.
 *
 * @param bodyText - STOMP 메시지 본문 텍스트
 * @returns 파싱된 payload 객체 또는 null
 */
export const parseStompPayload = (
  bodyText: string
): { success?: boolean; data?: unknown; errorMessage?: string } | null => {
  if (!bodyText || typeof bodyText !== 'string') return null;

  // JSON 시작 위치 찾기
  const firstBrace = bodyText.indexOf('{');
  const firstBracket = bodyText.indexOf('[');
  let jsonStart = -1;

  if (firstBrace !== -1 && (firstBracket === -1 || firstBrace < firstBracket)) {
    jsonStart = firstBrace;
  } else if (firstBracket !== -1) {
    jsonStart = firstBracket;
  }

  if (jsonStart === -1) return null;

  // JSON 끝 위치 찾기
  const jsonEnd = findJsonEnd(bodyText, jsonStart);
  if (jsonEnd === -1) return null;

  // 유효한 JSON 부분만 추출
  const extractedJson = bodyText.substring(jsonStart, jsonEnd).trimEnd();
  // 끝의 null 문자 제거
  let cleanedJson = extractedJson;
  while (cleanedJson.length > 0 && cleanedJson.charCodeAt(cleanedJson.length - 1) === 0) {
    cleanedJson = cleanedJson.substring(0, cleanedJson.length - 1);
  }

  try {
    // 추출한 JSON 파싱
    const parsed = JSON.parse(cleanedJson);
    if (
      parsed &&
      typeof parsed === 'object' &&
      ('success' in parsed || 'data' in parsed || 'errorMessage' in parsed)
    ) {
      return parsed;
    }
  } catch (error) {
    console.warn('[STOMP Payload Parse] JSON 파싱 실패:', {
      error: error instanceof Error ? error.message : String(error),
      bodyLength: bodyText.length,
      jsonStart,
      jsonEnd,
      extractedJsonLength: cleanedJson.length,
      extractedJsonEnd: cleanedJson.substring(Math.max(0, cleanedJson.length - 50)),
      bodyEnd: bodyText.substring(Math.max(0, bodyText.length - 100)),
      hasDevtools: bodyText.includes('@devtools'),
    });
    return null;
  }

  return null;
};
