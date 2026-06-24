import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useState } from 'react';
import { useRouletteTransition } from './useRouletteTransition';
import { PlayerProbability } from '@/types/roulette';
import { colorList } from '@/constants/color';

const RouletteTransitionDemo = ({
  initialData,
  targetData,
}: {
  initialData: PlayerProbability[];
  targetData: PlayerProbability[];
}) => {
  const [prev, setPrev] = useState<PlayerProbability[] | null>(initialData);
  const [current, setCurrent] = useState<PlayerProbability[] | null>(targetData);
  const [isAnimating, setIsAnimating] = useState(false);

  const { animatedSectors, startAnimation } = useRouletteTransition(prev, current);

  const handleStartAnimation = () => {
    setPrev(current);
    setCurrent(targetData);
    setIsAnimating(true);
    startAnimation(); // 명시적으로 애니메이션 시작

    // 애니메이션 완료 후 상태 리셋
    setTimeout(() => {
      setIsAnimating(false);
    }, 1200);
  };

  const handleReset = () => {
    setPrev(initialData);
    setCurrent(initialData);
    setIsAnimating(false);
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h2>Roulette Transition Demo</h2>

      <div style={{ marginBottom: '20px' }}>
        <button
          onClick={handleStartAnimation}
          disabled={isAnimating}
          style={{
            padding: '10px 20px',
            marginRight: '10px',
            backgroundColor: isAnimating ? '#ccc' : '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: isAnimating ? 'not-allowed' : 'pointer',
          }}
        >
          {isAnimating ? 'Animating...' : 'Start Animation'}
        </button>

        <button
          onClick={handleReset}
          style={{
            padding: '10px 20px',
            backgroundColor: '#6c757d',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
          }}
        >
          Reset
        </button>
      </div>

      <div style={{ display: 'flex', gap: '20px' }}>
        {/* 원형 룰렛 시각화 */}
        <div style={{ flex: 1 }}>
          <h3>Roulette Visualization</h3>
          <div style={{ position: 'relative', width: '300px', height: '300px' }}>
            <svg width="300" height="300" viewBox="0 0 300 300">
              <circle cx="150" cy="150" r="140" fill="none" stroke="#ddd" strokeWidth="2" />

              {animatedSectors?.map((angle, index) => {
                const startAngleRad = (angle.startAngle * Math.PI) / 180;
                const endAngleRad = (angle.endAngle * Math.PI) / 180;

                const startX = 150 + 140 * Math.cos(startAngleRad);
                const startY = 150 + 140 * Math.sin(startAngleRad);
                const endX = 150 + 140 * Math.cos(endAngleRad);
                const endY = 150 + 140 * Math.sin(endAngleRad);

                const largeArcFlag = angle.endAngle - angle.startAngle > 180 ? 1 : 0;

                const pathData = [
                  `M 150 150`,
                  `L ${startX} ${startY}`,
                  `A 140 140 0 ${largeArcFlag} 1 ${endX} ${endY}`,
                  'Z',
                ].join(' ');

                const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD'];
                const color = colors[index % colors.length];

                return (
                  <g key={angle.playerName}>
                    <path d={pathData} fill={color} stroke="#fff" strokeWidth="2" />
                    <text
                      x={150 + 70 * Math.cos((startAngleRad + endAngleRad) / 2)}
                      y={150 + 70 * Math.sin((startAngleRad + endAngleRad) / 2)}
                      textAnchor="middle"
                      dominantBaseline="middle"
                      fill="white"
                      fontSize="12"
                      fontWeight="bold"
                    >
                      {angle.playerName}
                    </text>
                  </g>
                );
              })}
            </svg>
          </div>
        </div>

        {/* 데이터 표시 */}
        <div style={{ flex: 1 }}>
          <h3>Current Angles Data</h3>
          <div
            style={{
              backgroundColor: '#f8f9fa',
              padding: '15px',
              borderRadius: '4px',
              maxHeight: '300px',
              overflowY: 'auto',
            }}
          >
            {animatedSectors ? (
              <div>
                {animatedSectors.map((angle) => (
                  <div key={angle.playerName} style={{ marginBottom: '10px' }}>
                    <strong>{angle.playerName}</strong>
                    <br />
                    <span style={{ color: '#666' }}>
                      Start: {angle.startAngle.toFixed(2)}° | End: {angle.endAngle.toFixed(2)}° |
                      Size: {(angle.endAngle - angle.startAngle).toFixed(2)}°
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ color: '#666' }}>No animation data</div>
            )}
          </div>
        </div>
      </div>

      {/* 초기 데이터와 타겟 데이터 표시 */}
      <div style={{ marginTop: '20px', display: 'flex', gap: '20px' }}>
        <div style={{ flex: 1 }}>
          <h3>Initial Data</h3>
          <div
            style={{
              backgroundColor: '#e3f2fd',
              padding: '15px',
              borderRadius: '4px',
            }}
          >
            {initialData.map((item, index) => (
              <div key={index} style={{ marginBottom: '5px' }}>
                <strong>{item.playerName}</strong>: {item.probability}%
              </div>
            ))}
          </div>
        </div>

        <div style={{ flex: 1 }}>
          <h3>Target Data</h3>
          <div
            style={{
              backgroundColor: '#f3e5f5',
              padding: '15px',
              borderRadius: '4px',
            }}
          >
            {targetData.map((item, index) => (
              <div key={index} style={{ marginBottom: '5px' }}>
                <strong>{item.playerName}</strong>: {item.probability}%
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

const meta: Meta<typeof RouletteTransitionDemo> = {
  title: 'Features/Roulette/useRouletteTransition',
  component: RouletteTransitionDemo,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component: `
useRouletteTransition 훅을 시각적으로 테스트하는 컴포넌트입니다.

이 훅은 룰렛의 확률 데이터가 변경될 때 부드러운 애니메이션 전환을 제공합니다.

**주요 기능:**
- 1.2초 동안의 빠른 시작 애니메이션 (easeOutCubic 이징)
- 각 플레이어의 확률 변화를 각도로 보간
- requestAnimationFrame을 사용한 성능 최적화

**사용법:**
1. "Start Animation" 버튼을 클릭하여 애니메이션 시작
2. "Reset" 버튼으로 초기 상태로 복원
3. 원형 룰렛과 데이터를 실시간으로 확인
        `,
      },
    },
  },
};

export default meta;

type Story = StoryObj<typeof RouletteTransitionDemo>;

// 기본 스토리 - 균등한 확률에서 불균등한 확률로
export const EqualToUnequal: Story = {
  args: {
    initialData: [
      { playerName: 'Alice', probability: 25, playerColor: colorList[0] },
      { playerName: 'Bob', probability: 25, playerColor: colorList[1] },
      { playerName: 'Charlie', probability: 25, playerColor: colorList[2] },
      { playerName: 'Diana', probability: 25, playerColor: colorList[3] },
    ],
    targetData: [
      { playerName: 'Alice', probability: 40, playerColor: colorList[0] },
      { playerName: 'Bob', probability: 30, playerColor: colorList[1] },
      { playerName: 'Charlie', probability: 20, playerColor: colorList[2] },
      { playerName: 'Diana', probability: 10, playerColor: colorList[3] },
    ],
  },
};

// 스토리 - 확률이 크게 변하는 경우
export const DramaticChange: Story = {
  args: {
    initialData: [
      { playerName: 'Player1', probability: 10, playerColor: colorList[0] },
      { playerName: 'Player2', probability: 20, playerColor: colorList[1] },
      { playerName: 'Player3', probability: 30, playerColor: colorList[2] },
      { playerName: 'Player4', probability: 40, playerColor: colorList[3] },
    ],
    targetData: [
      { playerName: 'Player1', probability: 50, playerColor: colorList[0] },
      { playerName: 'Player2', probability: 25, playerColor: colorList[1] },
      { playerName: 'Player3', probability: 15, playerColor: colorList[2] },
      { playerName: 'Player4', probability: 10, playerColor: colorList[3] },
    ],
  },
};

// 스토리 - 플레이어 수가 다른 경우
export const DifferentPlayerCount: Story = {
  args: {
    initialData: [
      { playerName: 'Alice', probability: 50, playerColor: colorList[0] },
      { playerName: 'Bob', probability: 50, playerColor: colorList[1] },
    ],
    targetData: [
      { playerName: 'Alice', probability: 30, playerColor: colorList[0] },
      { playerName: 'Bob', probability: 30, playerColor: colorList[1] },
      { playerName: 'Charlie', probability: 25, playerColor: colorList[2] },
      { playerName: 'Diana', probability: 15, playerColor: colorList[3] },
    ],
  },
};

// 스토리 - 극단적인 확률 변화
export const ExtremeChange: Story = {
  args: {
    initialData: [
      { playerName: 'Winner', probability: 5, playerColor: colorList[0] },
      { playerName: 'Runner1', probability: 15, playerColor: colorList[1] },
      { playerName: 'Runner2', probability: 30, playerColor: colorList[2] },
      { playerName: 'Others', probability: 50, playerColor: colorList[3] },
    ],
    targetData: [
      { playerName: 'Winner', probability: 80, playerColor: colorList[0] },
      { playerName: 'Runner1', probability: 10, playerColor: colorList[1] },
      { playerName: 'Runner2', probability: 7, playerColor: colorList[2] },
      { playerName: 'Others', probability: 3, playerColor: colorList[3] },
    ],
  },
};

// 스토리 - 많은 플레이어
export const ManyPlayers: Story = {
  args: {
    initialData: [
      { playerName: 'Player1', probability: 10, playerColor: colorList[0] },
      { playerName: 'Player2', probability: 10, playerColor: colorList[1] },
      { playerName: 'Player3', probability: 10, playerColor: colorList[2] },
      { playerName: 'Player4', probability: 10, playerColor: colorList[3] },
      { playerName: 'Player5', probability: 10, playerColor: colorList[4] },
      { playerName: 'Player6', probability: 10, playerColor: colorList[5] },
      { playerName: 'Player7', probability: 10, playerColor: colorList[6] },
      { playerName: 'Player8', probability: 10, playerColor: colorList[7] },
      { playerName: 'Player9', probability: 10, playerColor: colorList[8] },
      { playerName: 'Player10', probability: 10, playerColor: colorList[0] },
    ],
    targetData: [
      { playerName: 'Player1', probability: 25, playerColor: colorList[0] },
      { playerName: 'Player2', probability: 20, playerColor: colorList[1] },
      { playerName: 'Player3', probability: 15, playerColor: colorList[2] },
      { playerName: 'Player4', probability: 12, playerColor: colorList[3] },
      { playerName: 'Player5', probability: 10, playerColor: colorList[4] },
      { playerName: 'Player6', probability: 8, playerColor: colorList[5] },
      { playerName: 'Player7', probability: 5, playerColor: colorList[6] },
      { playerName: 'Player8', probability: 3, playerColor: colorList[7] },
      { playerName: 'Player9', probability: 1.5, playerColor: colorList[8] },
      { playerName: 'Player10', probability: 0.5, playerColor: colorList[0] },
    ],
  },
};
