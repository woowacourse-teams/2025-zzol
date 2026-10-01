/**
 * 카드 밖에 바로 놓이는 실패 자리에 입히는 표면.
 *
 * <p>지표 타일 줄처럼 카드 없이 격자에 바로 얹는 영역이 있다. 그 자리가 실패하면
 * {@code ErrorState} 의 문구만 캔버스 위에 맨몸으로 뜬다. 다른 모든 내용이 떠 있는
 * 흰 카드 위에 있는 화면에서 글자만 바닥에 붙어 있으면 화면이 깨진 것처럼 보인다.
 *
 * <p>카드 안에서는 쓰지 않는다. 카드가 이미 표면과 테두리와 그림자를 준다.
 *
 * <p>격자에서 몇 칸을 차지할지는 자리마다 다르므로 여기 넣지 않는다. 쓰는 쪽에서
 * {@code col-span-*} 를 덧붙인다.
 */
export const ERROR_SURFACE = 'rounded-lg border border-border-default bg-surface shadow-card';
