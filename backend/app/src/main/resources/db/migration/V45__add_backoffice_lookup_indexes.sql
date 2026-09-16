-- 백오피스 조회가 쓰는 인덱스.
-- room_session(created_at)은 V3에 이미 있어 퍼널 집계는 그대로 탄다.

-- 방 드릴다운 검색. join_code 는 유니크가 아니라(같은 코드가 시간이 지나 재사용된다)
-- 조회하면 여러 방이 나오고, 인덱스가 없으면 room_session 전체를 훑는다.
-- created_at 을 뒤에 붙여 "이 코드의 최근 방부터" 정렬까지 인덱스로 끝낸다.
CREATE INDEX idx_room_session_join_code_created_at ON room_session (join_code, created_at DESC);

-- 홈 대시보드의 일별 참여자 수. player 는 방마다 여러 행이 쌓여 가장 빨리 커지는 축이라
-- 인덱스 없이 기간 집계를 돌리면 대시보드를 열 때마다 풀스캔이 난다.
CREATE INDEX idx_player_created_at ON player (created_at);

-- 홈 대시보드의 일별 가입 수.
CREATE INDEX idx_app_user_created_at ON app_user (created_at);
