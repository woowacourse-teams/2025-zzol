import type { Meta, StoryObj } from '@storybook/react-webpack5';
import RacingPlayer from './RacingPlayer';
import { ColorList } from '@/constants/color';

const meta: Meta<typeof RacingPlayer> = {
  title: 'Features/MiniGame/RacingGame/RacingPlayer',
  component: RacingPlayer,
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <div
        style={{
          width: '100%',
          height: '400px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#1a1a2e',
          position: 'relative',
        }}
      >
        <Story />
      </div>
    ),
  ],
  argTypes: {
    isMe: {
      control: 'boolean',
      description: '본인 여부',
    },
    myPosition: {
      control: { type: 'range', min: -200, max: 200, step: 10 },
      description: '내 위치 (상대 위치 계산용)',
    },
    color: {
      control: 'select',
      options: [
        '#FF6B6B',
        '#80d6d0',
        '#a7e142',
        '#bf77f6',
        '#ffa102',
        '#5a88c8',
        '#ff8ad8',
        '#c6c8d0',
        '#2ba727',
      ] as ColorList[],
      description: '플레이어 아이콘 색상',
    },
  },
  args: {
    player: {
      playerName: '플레이어1',
      position: 0,
      speed: 20,
    },
    isMe: true,
    myPosition: 0,
    color: '#FF6B6B',
  },
};

export default meta;
type Story = StoryObj<typeof RacingPlayer>;

export const MultiplePlayersComparison: Story = {
  render: () => (
    <div
      style={{
        display: 'flex',
        gap: '2rem',
        flexWrap: 'wrap',
        justifyContent: 'center',
        alignItems: 'center',
      }}
    >
      <div style={{ textAlign: 'center' }}>
        <div style={{ color: 'white', marginBottom: '0.5rem', fontSize: '0.875rem' }}>속도: 1</div>
        <RacingPlayer
          player={{ playerName: '최소', position: 0, speed: 1 }}
          isMe={true}
          myPosition={0}
          color="#8a8b8e"
        />
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ color: 'white', marginBottom: '0.5rem', fontSize: '0.875rem' }}>속도: 3</div>
        <RacingPlayer
          player={{ playerName: '느림', position: 0, speed: 3 }}
          isMe={true}
          myPosition={0}
          color="#4cafa9"
        />
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ color: 'white', marginBottom: '0.5rem', fontSize: '0.875rem' }}>속도: 6</div>
        <RacingPlayer
          player={{ playerName: '중간', position: 0, speed: 6 }}
          isMe={true}
          myPosition={0}
          color="#85b62e"
        />
      </div>
      <div style={{ textAlign: 'center' }}>
        <div style={{ color: 'white', marginBottom: '0.5rem', fontSize: '0.875rem' }}>속도: 10</div>
        <RacingPlayer
          player={{ playerName: '최대', position: 0, speed: 10 }}
          isMe={true}
          myPosition={0}
          color="#ffa102"
        />
      </div>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '여러 속도를 동시에 비교 - useRotationAnimation 성능 및 시각적 차이 확인',
      },
    },
    controls: { disable: true },
  },
};
