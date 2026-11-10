import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { theme } from '@/styles/theme';
import RankingNickname from './RankingNickname';

const meta: Meta<typeof RankingNickname> = {
  title: 'Common/RankingNickname',
  component: RankingNickname,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    children: {
      control: 'text',
      description: '닉네임',
    },
    masked: {
      control: 'boolean',
      description: '검열 전 닉네임이라 서버가 가린 행',
    },
    tone: {
      control: 'inline-radio',
      options: ['light', 'dark'],
      description: 'light는 밝은 배경, dark는 짙은 색 배경',
    },
  },
  args: {
    children: '처리 중인 닉네임',
    masked: true,
    tone: 'light',
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Light: Story = {};

export const Dark: Story = {
  args: {
    tone: 'dark',
  },
  decorators: [
    (Story) => (
      <div style={{ background: theme.color.point[500], padding: '1rem' }}>
        <Story />
      </div>
    ),
  ],
};

export const NotMasked: Story = {
  args: {
    children: '세라',
    masked: false,
  },
};
