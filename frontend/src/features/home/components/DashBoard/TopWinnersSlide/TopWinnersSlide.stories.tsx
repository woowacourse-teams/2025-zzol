import type { Meta, StoryObj } from '@storybook/react-webpack5';
import TopWinnersSlide from './TopWinnersSlide';

const meta: Meta<typeof TopWinnersSlide> = {
  title: 'Features/Home/DashBoard/TopWinnersSlide',
  component: TopWinnersSlide,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <div
        style={{
          background: '#ff6b6b',
          padding: '2rem',
          borderRadius: '20px',
          width: '375px',
          height: '400px',
        }}
      >
        <Story />
      </div>
    ),
  ],
  argTypes: {
    winners: {
      control: 'object',
      description: 'TOP3 당첨자 데이터',
    },
  },
  args: {
    winners: [
      { nickname: '세라', userCode: 'AB12C', winCount: 20 },
      { nickname: '민수', userCode: 'DE34F', winCount: 15 },
      { nickname: '지영', userCode: 'GH56I', winCount: 12 },
    ],
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const LessThanThree: Story = {
  args: {
    winners: [
      { nickname: '세라', userCode: 'AB12C', winCount: 20 },
      { nickname: '민수', userCode: 'DE34F', winCount: 15 },
    ],
  },
};

export const SingleWinner: Story = {
  args: {
    winners: [{ nickname: '세라', userCode: 'AB12C', winCount: 20 }],
  },
};
