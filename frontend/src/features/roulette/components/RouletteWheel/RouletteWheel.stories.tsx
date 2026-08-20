import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { ChangeEvent, useState } from 'react';
import RouletteWheel from './RouletteWheel';
import { colorList } from '@/constants/color';

const meta: Meta<typeof RouletteWheel> = {
  title: 'Composition/RouletteWheel',
  component: RouletteWheel,
};

export default meta;

export const Interactive: StoryObj<typeof RouletteWheel> = {
  render: () => {
    const [isSpinStarted, setIsSpinning] = useState(false);
    const [finalRotation, setFinalRotation] = useState(0);

    const handleSpin = () => {
      if (isSpinStarted) return;
      setIsSpinning(true);

      // 3초 후 스피닝 완료
      setTimeout(() => {
        setIsSpinning(false);
        // 테스트용: 랜덤한 finalRotation 설정
        setFinalRotation(Math.floor(Math.random() * 360));
      }, 3000);
    };

    const handleReset = () => {
      setFinalRotation(0);
      setIsSpinning(false);
    };

    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 24 }}>
        <div style={{ width: 300, height: 300 }}>
          <RouletteWheel
            playerProbabilities={mockPlayerProbabilities}
            isSpinStarted={isSpinStarted}
            finalRotation={finalRotation}
          />
        </div>
        <div style={{ display: 'flex', gap: 16 }}>
          <button onClick={handleSpin} disabled={isSpinStarted}>
            {isSpinStarted ? '돌아가는 중...' : '돌리기'}
          </button>
          <button onClick={handleReset} disabled={isSpinStarted}>
            리셋
          </button>
        </div>
        <div style={{ fontSize: 14, color: '#666' }}>Final Rotation: {finalRotation}°</div>
      </div>
    );
  },
};

export const WithFixedRotation: StoryObj<typeof RouletteWheel> = {
  render: () => {
    const [isSpinStarted, setIsSpinning] = useState(false);
    const [finalRotation, setFinalRotation] = useState(90);

    const handleSpin = () => {
      if (isSpinStarted) return;
      setIsSpinning(true);

      setTimeout(() => {
        setIsSpinning(false);
      }, 3000);
    };

    const handleRotationChange = (e: ChangeEvent<HTMLInputElement>) => {
      setFinalRotation(Number(e.target.value));
    };

    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 24 }}>
        <div style={{ width: 300, height: 300 }}>
          <RouletteWheel
            playerProbabilities={mockPlayerProbabilities}
            isSpinStarted={isSpinStarted}
            finalRotation={finalRotation}
          />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
          <button onClick={handleSpin} disabled={isSpinStarted}>
            {isSpinStarted ? '돌아가는 중...' : '돌리기'}
          </button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <label>Final Rotation:</label>
            <input
              type="range"
              min="0"
              max="360"
              value={finalRotation}
              onChange={handleRotationChange}
              disabled={isSpinStarted}
            />
            <span>{finalRotation}°</span>
          </div>
        </div>
      </div>
    );
  },
};

const mockPlayerProbabilities = [
  // 최대 길이(10자) 닉네임과 이름을 그리지 않는 얇은 조각(2%)을 함께 둔다
  {
    playerName: '제발당첨되게해주세요',
    probability: 18.0,
    playerColor: colorList[0],
  },
  {
    playerName: '커피사주세요',
    probability: 15.0,
    playerColor: colorList[1],
  },
  {
    playerName: '룰렛장인',
    probability: 14.0,
    playerColor: colorList[2],
  },
  {
    playerName: '오늘은내가쏜다',
    probability: 12.0,
    playerColor: colorList[3],
  },
  {
    playerName: '김철수',
    probability: 11.0,
    playerColor: colorList[4],
  },
  {
    playerName: '이순신',
    probability: 11.0,
    playerColor: colorList[5],
  },
  {
    playerName: '행운의여신',
    probability: 10.0,
    playerColor: colorList[6],
  },
  {
    playerName: '박영희',
    probability: 7.0,
    playerColor: colorList[7],
  },
  {
    playerName: '막차탄사람',
    probability: 2.0,
    playerColor: colorList[8],
  },
];
