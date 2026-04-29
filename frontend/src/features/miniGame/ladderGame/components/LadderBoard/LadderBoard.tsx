import { colorList } from '@/constants/color';
import { useLadderGameContext } from '@/contexts/LadderGame/LadderGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { LadderLine, Pole } from '@/types/miniGame/ladderGame';
import useToast from '@/components/@common/Toast/useToast';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import * as S from './LadderBoard.styled';

const PAD_X = 20;
const PAD_Y_TOP = 40;
const PAD_Y_BOTTOM = 36;
const MIN_ROW_HEIGHT = 30;
const POLE_WIDTH = 3;
const LINE_WIDTH = 4;
const TOUCH_HIT_EXPANSION = 8;

const truncateName = (name: string) => name.slice(0, 3);

const getPoleColor = (poleIndex: number) => colorList[poleIndex % colorList.length];

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
      color: getPoleColor(pole.index),
    };
  });

const LadderBoard = () => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const pathRefs = useRef<(SVGPathElement | null)[]>([]);

  const { gameState, poles, bottomRanks, lines, ghostSegmentIndex, animationDurationMs, drawLine } =
    useLadderGameContext();
  const { myName } = useIdentifier();
  const { showToast } = useToast();

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

  // poleCount 기반으로 고정: 선이 추가될 때마다 rowHeight가 바뀌어 기존 선 위치가 이동하는 현상 방지
  const rowHeight =
    usableHeight > 0 && poleCount > 0
      ? Math.max(usableHeight / (poleCount + 1), MIN_ROW_HEIGHT)
      : MIN_ROW_HEIGHT;

  const TOP_Y = PAD_Y_TOP;
  const BOTTOM_Y = PAD_Y_TOP + usableHeight;

  const poleX = useCallback((i: number) => PAD_X + i * poleGap, [poleGap]);
  const rowY = useCallback((row: number) => TOP_Y + row * rowHeight, [TOP_Y, rowHeight]);

  const myPoleIndex = poles.findIndex((p) => p.playerName === myName);
  const alreadyDrawn = lines.some((l) => l.playerName === myName);

  const sortedLines = useMemo(() => [...lines].sort((a, b) => a.row - b.row), [lines]);

  const ghostY = useMemo(() => {
    if (ghostSegmentIndex === null) return 0;
    const sameSeg = lines.filter((l) => l.segmentIndex === ghostSegmentIndex);
    const nextRow = sameSeg.length > 0 ? Math.max(...sameSeg.map((l) => l.row)) + 1 : 1;
    return rowY(nextRow);
  }, [ghostSegmentIndex, lines, rowY]);

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
    (segmentIndex: number) => {
      if (alreadyDrawn) {
        showToast({ message: '선을 이미 그었습니다!', type: 'info' });
        return;
      }
      if (ghostSegmentIndex !== null) return;
      drawLine(segmentIndex);
    },
    [alreadyDrawn, ghostSegmentIndex, drawLine, showToast]
  );

  if (!width || !height || poleCount === 0) {
    return <S.Container ref={containerRef} />;
  }

  const isDrawing = gameState === 'DRAWING';

  return (
    <S.Container ref={containerRef}>
      <svg width={width} height={height}>
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
              stroke={getPoleColor(i)}
              strokeWidth={isMe ? POLE_WIDTH + 1 : POLE_WIDTH}
              opacity={isMe ? 1 : 0.6}
            />
          );
        })}

        {/* 이름 */}
        {poles.map((pole, i) => {
          const isMe = pole.playerName === myName;
          return (
            <text
              key={`name-${pole.playerName}`}
              x={poleX(i)}
              y={TOP_Y - 10}
              textAnchor="middle"
              fontSize={isMe ? 14 : 12}
              fontWeight={isMe ? 700 : 400}
              fill={isMe ? getPoleColor(i) : '#888'}
            >
              {truncateName(pole.playerName)}
            </text>
          );
        })}

        {/* 바닥 순위 */}
        {Object.entries(bottomRanks).map(([poleIdxStr, rank]) => {
          const idx = Number(poleIdxStr);
          return (
            <text
              key={`rank-${idx}`}
              x={poleX(idx)}
              y={BOTTOM_Y + 24}
              textAnchor="middle"
              fontSize={13}
              fontWeight={600}
              fill="#555"
            >
              {rank}위
            </text>
          );
        })}

        {/* 확정 선 */}
        {sortedLines.map((line, i) => (
          <line
            key={`line-${i}`}
            x1={poleX(line.segmentIndex)}
            y1={rowY(line.row)}
            x2={poleX(line.segmentIndex + 1)}
            y2={rowY(line.row)}
            stroke={getPoleColor(poles.find((p) => p.playerName === line.playerName)?.index ?? 0)}
            strokeWidth={LINE_WIDTH}
            strokeLinecap="round"
          />
        ))}

        {/* ghost 선 */}
        {ghostSegmentIndex !== null && (
          <line
            x1={poleX(ghostSegmentIndex)}
            y1={ghostY}
            x2={poleX(ghostSegmentIndex + 1)}
            y2={ghostY}
            stroke={myPoleIndex >= 0 ? getPoleColor(myPoleIndex) : '#aaa'}
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
              x={poleX(i) - TOUCH_HIT_EXPANSION}
              y={TOP_Y}
              width={poleGap + TOUCH_HIT_EXPANSION * 2}
              height={usableHeight}
              fill="transparent"
              style={{ cursor: alreadyDrawn ? 'not-allowed' : 'pointer' }}
              onClick={() => handleSegmentClick(i)}
            />
          ))}
      </svg>
    </S.Container>
  );
};

export default LadderBoard;
