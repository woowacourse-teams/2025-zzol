import type { Meta, StoryObj } from '@storybook/react-webpack5';
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
  },
  args: {
    children: '처리 중인 닉네임',
    masked: true,
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Masked: Story = {};

export const NotMasked: Story = {
  args: {
    children: '세라',
    masked: false,
  },
};
