import type { Meta, StoryObj } from '@storybook/react-webpack5';
import CardIcon from '@/assets/card-icon.svg';
import RacingIcon from '@/assets/racing-icon.svg';
import GameActionButton from './GameActionButton';

const meta: Meta<typeof GameActionButton> = {
  title: 'Common/GameActionButton',
  component: GameActionButton,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    onClick: { action: 'clicked' },
    isSelected: {
      control: 'boolean',
      description: '버튼의 선택 상태를 나타냅니다',
    },
    isDisabled: {
      control: 'boolean',
      description: '버튼의 비활성화 상태를 나타냅니다',
    },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const CardGame: Story = {
  args: {
    isSelected: false,
    isDisabled: false,
    gameName: '카드게임',
    description: ['2라운드 동안 매번 카드 1장씩 뒤집어', '가장 높은 점수를 내보세요!'],
    icon: <img src={CardIcon} alt="card game" style={{ width: '30px', height: 'auto' }} />,
  },
};

export const CardGameSelected: Story = {
  args: {
    isSelected: true,
    isDisabled: false,
    gameName: '카드게임',
    description: ['2라운드 동안 매번 카드 1장씩 뒤집어', '가장 높은 점수를 내보세요!'],
    icon: <img src={CardIcon} alt="card game" style={{ width: '30px', height: 'auto' }} />,
  },
};

export const RacingGame: Story = {
  args: {
    isSelected: false,
    isDisabled: false,
    gameName: '레이싱게임',
    description: ['화면을 클릭해 속도를 높여서', '가장 먼저 도착하세요!'],
    icon: <img src={RacingIcon} alt="racing game" style={{ width: '30px', height: 'auto' }} />,
  },
};

export const RacingGameSelected: Story = {
  args: {
    isSelected: true,
    isDisabled: false,
    gameName: '레이싱게임',
    description: ['화면을 클릭해 속도를 높여서', '가장 먼저 도착하세요!'],
    icon: <img src={RacingIcon} alt="racing game" style={{ width: '30px', height: 'auto' }} />,
  },
};

export const Disabled: Story = {
  args: {
    isSelected: false,
    isDisabled: true,
    gameName: '카드게임',
    description: ['2라운드 동안 매번 카드 1장씩 뒤집어', '가장 높은 점수를 내보세요!'],
    icon: <img src={CardIcon} alt="card game" style={{ width: '30px', height: 'auto' }} />,
  },
};
