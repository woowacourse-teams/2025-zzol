-- 정규화 아티팩트로 생성된 짧은 ASCII 비속어 키워드 삭제
--
-- `@!@`(시드 목록)가 정규화(리트 치환 @→a, 특수문자 제거)를 거치며 `aa`로 붕괴해 등록됐다.
-- Aho-Corasick은 부분 매칭이라 닉네임에 `aa`가 포함되기만 해도 차단되는 오탐이 발생한다.
-- ProfanityWord 도메인 가드(ASCII-only & 3자 미만 거부)와 정합을 맞춰 기존 행을 제거한다.
--
-- 조건: 글자 수 3 미만(CHAR_LENGTH) AND ASCII 전용(바이트 길이 == 글자 수).
--   한글은 utf8mb4에서 글자당 3바이트이므로 LENGTH > CHAR_LENGTH → 제외된다(`씨발` 등 보존).
DELETE FROM profanity_word
WHERE CHAR_LENGTH(word) < 3
  AND LENGTH(word) = CHAR_LENGTH(word);
