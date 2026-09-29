import { colorList, rankColorMap } from '@/constants/color';
import { useLadderGameContext } from '@/contexts/LadderGame/LadderGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { LadderLine, MAX_LINES_PER_PLAYER, Pole } from '@/types/miniGame/ladderGame';
import useToast from '@/components/@common/Toast/useToast';
import { MouseEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { theme } from '@/styles/theme';
import * as S from './LadderBoard.styled';

const PAD_X = 24;
const PAD_Y_TOP = 44;
const PAD_Y_BOTTOM = 44;
const NAME_CHIP_HEIGHT = 22;
const NAME_CHIP_MAX_WIDTH = 46;
const RANK_BADGE_RADIUS = 14;
const POLE_WIDTH = 3;
const LINE_WIDTH = 4;
const TOUCH_HIT_EXPANSION = 8;

const truncateName = (name: string) => name.slice(0, 3);

const getPoleColor = (poleIndex: number) => colorList[poleIndex % colorList.length];

const resolveColorIndex = (pole: Pole) => pole.colorIndex ?? pole.index;
const resolveLineColorIndex = (line: LadderLine, poles: Pole[]) =>
  line.colorIndex ?? poles.find((p) => p.playerName === line.playerName)?.index ?? 0;

// 같은 높이에서 기둥을 공유하는 구간(같은 구간, 좌우 옆 구간)에 선이 있으면 막힌다. 서버 LadderLines.isOccupied 와 같은 규칙
const isOccupied = (
  taken: { segmentIndex: number | string; row: number }[],
  segmentIndex: number,
  row: number
) => taken.some((l) => l.row === row && Math.abs(Number(l.segmentIndex) - segmentIndex) <= 1);

// 누른 높이에서 가까운 순으로 비어 있는 row 를 찾는다. 칸이 촘촘해 정확히 누르기 어려우므로 자동으로 맞춘다
const findNearestFreeRow = (
  taken: { segmentIndex: number | string; row: number }[],
  segmentIndex: number,
  tappedRow: number,
  rowCount: number
) => {
  for (let distance = 0; distance < rowCount; distance++) {
    for (const row of [tappedRow - distance, tappedRow + distance]) {
      if (row >= 1 && row <= rowCount && !isOccupied(taken, segmentIndex, row)) return row;
    }
  }
  return null;
};

const tracePaths = (
  poles: Pole[],
  sortedLines: LadderLine[],
  poleX: (i: number) => number,
  rowY: (row: number) => number,
  topY: number,
  bottomY: number
) =>
  poles.map((pole, i) => {
    const points: [number, number][] = [];
    let current = i;
    points.push([poleX(current), topY]);

    for (const line of sortedLines) {
      const y = rowY(line.row);
      const segIdx = Number(line.segmentIndex);
      if (current === segIdx) {
        points.push([poleX(current), y]);
        current = segIdx + 1;
        points.push([poleX(current), y]);
      } else if (current === segIdx + 1) {
        points.push([poleX(current), y]);
        current = segIdx;
        points.push([poleX(current), y]);
      }
    }
    points.push([poleX(current), bottomY]);

    return {
      playerName: pole.playerName,
      d: points.map(([x, y], idx) => `${idx === 0 ? 'M' : 'L'}${x},${y}`).join(' '),
      color: getPoleColor(resolveColorIndex(pole)),
    };
  });

const LadderBoard = () => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const pathRefs = useRef<(SVGPathElement | null)[]>([]);

  const {
    gameState,
    poles,
    bottomRanks,
    rowCount,
    lines,
    ghost,
    animationDurationMs,
    drawLine,
    dropGhost,
  } = useLadderGameContext();
  const { myName } = useIdentifier();
  const { showToast } = useToast();

  // 같은 자리를 동시에 누르면 서버는 먼저 도착한 요청만 받는다. 남의 선이 내 ghost 자리를 차지했다면 내 요청은
  // 반드시 거절되므로, 2초 타임아웃을 기다리지 않고 바로 거둬 다시 누를 수 있게 한다
  useEffect(() => {
    if (ghost === null) return;
    const othersLines = lines.filter((l) => l.playerName !== myName);
    if (!isOccupied(othersLines, ghost.segmentIndex, ghost.row)) return;
    dropGhost();
    showToast({ message: '다른 사람이 먼저 그은 자리예요. 다시 눌러 주세요', type: 'info' });
  }, [ghost, lines, myName, dropGhost, showToast]);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const update = () => setSize({ width: el.offsetWidth, height: el.offsetHeight });
    update();
    const observer = new ResizeObserver(update);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const { width, height } = size;
  const poleCount = poles.length;
  const usableWidth = width - PAD_X * 2;
  const usableHeight = height - PAD_Y_TOP - PAD_Y_BOTTOM;
  const poleGap = poleCount > 1 ? usableWidth / (poleCount - 1) : usableWidth;

  // 서버가 준 칸 수로 고정한다. 선이 늘어도 기존 선 위치가 움직이지 않고, 마지막 칸도 바닥 위에 남는다
  const totalRows = rowCount || poleCount * MAX_LINES_PER_PLAYER;
  const rowHeight = usableHeight > 0 ? usableHeight / (totalRows + 1) : 0;

  const TOP_Y = PAD_Y_TOP;
  const BOTTOM_Y = PAD_Y_TOP + usableHeight;

  const poleX = useCallback((i: number) => PAD_X + i * poleGap, [poleGap]);
  const rowY = useCallback((row: number) => TOP_Y + row * rowHeight, [TOP_Y, rowHeight]);

  const myPoleIndex = poles.findIndex((p) => p.playerName === myName);
  const myLineCount = lines.filter((l) => l.playerName === myName).length + (ghost ? 1 : 0);
  const allDrawn = myLineCount >= MAX_LINES_PER_PLAYER;

  const sortedLines = useMemo(() => [...lines].sort((a, b) => a.row - b.row), [lines]);

  const playerPaths = useMemo(() => {
    if (gameState !== 'RESULT' || !width || !height || poleCount === 0) return [];
    return tracePaths(poles, sortedLines, poleX, rowY, TOP_Y, BOTTOM_Y);
  }, [gameState, poles, sortedLines, poleX, rowY, TOP_Y, BOTTOM_Y, width, height, poleCount]);

  useEffect(() => {
    if (gameState !== 'RESULT' || !animationDurationMs || playerPaths.length === 0) return;
    const effectiveDuration = Math.max(1000, animationDurationMs - 500);
    const timer = setTimeout(() => {
      pathRefs.current.forEach((el) => {
        if (!el) return;
        const length = el.getTotalLength();
        el.style.strokeDasharray = `${length}`;
        el.style.strokeDashoffset = `${length}`;
        el.getBoundingClientRect();
        el.style.transition = `stroke-dashoffset ${effectiveDuration}ms cubic-bezier(0.4, 0, 0.2, 1)`;
        el.style.strokeDashoffset = '0';
      });
    }, 50);
    return () => clearTimeout(timer);
  }, [gameState, animationDurationMs, playerPaths]);

  const handleSegmentClick = useCallback(
    (segmentIndex: number, event: MouseEvent<SVGRectElement>) => {
      if (allDrawn) {
        showToast({ message: `선 ${MAX_LINES_PER_PLAYER}개를 모두 그었어요`, type: 'info' });
        return;
      }
      if (ghost !== null) return;

      const svgTop = event.currentTarget.ownerSVGElement?.getBoundingClientRect().top ?? 0;
      const tappedRow = Math.min(
        totalRows,
        Math.max(1, Math.round((event.clientY - svgTop - TOP_Y) / rowHeight))
      );
      const row = findNearestFreeRow(lines, segmentIndex, tappedRow, totalRows);
      if (row === null) {
        showToast({ message: '이 칸에는 더 그을 자리가 없어요', type: 'info' });
        return;
      }
      drawLine(segmentIndex, row);
    },
    [allDrawn, ghost, lines, totalRows, rowHeight, TOP_Y, drawLine, showToast]
  );

  if (!width || !height || poleCount === 0) {
    return <S.Container ref={containerRef} />;
  }

  const isDrawing = gameState === 'DRAWING';

  return (
    <S.Container ref={containerRef}>
      <svg width={width} height={height}>
        {/* 높이 안내선: 선이 놓일 수 있는 칸. DRAWING 동안만 보인다 */}
        {isDrawing &&
          Array.from({ length: totalRows }, (_, index) => (
            <line
              key={`guide-${index + 1}`}
              x1={poleX(0)}
              y1={rowY(index + 1)}
              x2={poleX(poleCount - 1)}
              y2={rowY(index + 1)}
              stroke={theme.color.gray[400]}
              strokeWidth={1}
              strokeDasharray="3 5"
              pointerEvents="none"
            />
          ))}

        {/* 세로 기둥 */}
        {poles.map((pole, i) => {
          const isMe = pole.playerName === myName;
          return (
            <line
              key={`pole-${pole.playerName}`}
              x1={poleX(i)}
              y1={TOP_Y}
              x2={poleX(i)}
              y2={BOTTOM_Y}
              stroke={getPoleColor(resolveColorIndex(pole))}
              strokeWidth={isMe ? POLE_WIDTH + 1 : POLE_WIDTH}
              opacity={isMe ? 1 : 0.6}
            />
          );
        })}

        {/* 이름 칩: 기둥 색으로 테두리, 내 칩만 채운다 */}
        {poles.map((pole, i) => {
          const isMe = pole.playerName === myName;
          const color = getPoleColor(resolveColorIndex(pole));
          const chipWidth = Math.min(poleGap - 4, NAME_CHIP_MAX_WIDTH);
          const chipY = TOP_Y - NAME_CHIP_HEIGHT - 10;
          return (
            <g key={`name-${pole.playerName}`}>
              <rect
                x={poleX(i) - chipWidth / 2}
                y={chipY}
                width={chipWidth}
                height={NAME_CHIP_HEIGHT}
                rx={NAME_CHIP_HEIGHT / 2}
                fill={isMe ? color : theme.color.white}
                stroke={color}
                strokeWidth={1.5}
              />
              <text
                x={poleX(i)}
                y={chipY + NAME_CHIP_HEIGHT / 2}
                dominantBaseline="central"
                textAnchor="middle"
                fontSize={11}
                fontWeight={700}
                fill={isMe ? theme.color.white : color}
              >
                {truncateName(pole.playerName)}
              </text>
            </g>
          );
        })}

        {/* 바닥 순위 배지: 1~3위는 금·은·동 */}
        {Object.entries(bottomRanks).map(([poleIdxStr, rank]) => {
          const idx = Number(poleIdxStr);
          const badgeY = BOTTOM_Y + RANK_BADGE_RADIUS + 8;
          return (
            <g key={`rank-${idx}`}>
              <circle
                cx={poleX(idx)}
                cy={badgeY}
                r={RANK_BADGE_RADIUS}
                fill={rankColorMap[rank] ?? theme.color.gray[100]}
              />
              <text
                x={poleX(idx)}
                y={badgeY}
                dominantBaseline="central"
                textAnchor="middle"
                fontSize={11}
                fontWeight={700}
                fill={theme.color.gray[700]}
              >
                {rank}위
              </text>
            </g>
          );
        })}

        {/* 확정 선 */}
        {sortedLines.map((line) => (
          <line
            key={`line-${line.playerName}-${line.row}-${line.segmentIndex}`}
            x1={poleX(Number(line.segmentIndex))}
            y1={rowY(line.row)}
            x2={poleX(Number(line.segmentIndex) + 1)}
            y2={rowY(line.row)}
            stroke={getPoleColor(resolveLineColorIndex(line, poles))}
            strokeWidth={LINE_WIDTH}
            strokeLinecap="round"
          />
        ))}

        {/* ghost 선 */}
        {ghost !== null && (
          <line
            x1={poleX(ghost.segmentIndex)}
            y1={rowY(ghost.row)}
            x2={poleX(ghost.segmentIndex + 1)}
            y2={rowY(ghost.row)}
            stroke={
              myPoleIndex >= 0
                ? getPoleColor(resolveColorIndex(poles[myPoleIndex]))
                : theme.color.gray[300]
            }
            strokeWidth={LINE_WIDTH}
            strokeLinecap="round"
            opacity={0.35}
          />
        )}

        {/* RESULT 경로 */}
        {gameState === 'RESULT' &&
          playerPaths.map((path, i) => (
            <path
              key={`path-${path.playerName}`}
              ref={(el) => {
                pathRefs.current[i] = el;
              }}
              d={path.d}
              stroke={path.color}
              strokeWidth={path.playerName === myName ? 5 : 3}
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          ))}

        {/* DRAWING 터치 영역 */}
        {isDrawing &&
          Array.from({ length: poleCount - 1 }).map((_, i) => (
            <rect
              key={`touch-${i}`}
              data-testid="ladder-segment"
              x={poleX(i) - TOUCH_HIT_EXPANSION}
              y={TOP_Y}
              width={poleGap + TOUCH_HIT_EXPANSION * 2}
              height={usableHeight}
              fill="transparent"
              style={{ cursor: allDrawn ? 'not-allowed' : 'pointer' }}
              onClick={(event) => handleSegmentClick(i, event)}
            />
          ))}
      </svg>
    </S.Container>
  );
};

export default LadderBoard;
