import type { Meta, StoryObj } from '@storybook/react-webpack5';
import LowestProbabilitySlide from './LowestProbabilitySlide';

const meta: Meta<typeof LowestProbabilitySlide> = {
  title: 'Features/Home/DashBoard/LowestProbabilitySlide',
  component: LowestProbabilitySlide,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <div
        style={{
          padding: '2rem',
          borderRadius: '20px',
          width: '375px',
        }}
      >
        <Story />
      </div>
    ),
  ],
  argTypes: {
    players: {
      control: 'object',
      description: '당첨자 배열 (nickname, userCode)',
    },
    probability: {
      control: { type: 'range', min: 0.1, max: 100, step: 0.1 },
      description: '확률 (%)',
    },
  },
  args: {
    players: [{ nickname: '세라', userCode: 'A1B2' }],
    probability: 5,
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const LowProbability: Story = {
  args: {
    players: [{ nickname: '세라', userCode: 'A1B2' }],
    probability: 1,
  },
};

export const MultipleWinners: Story = {
  args: {
    players: [
      { nickname: '세라', userCode: 'A1B2' },
      { nickname: '민수', userCode: 'C3D4' },
      { nickname: '지영', userCode: 'E5F6' },
    ],
    probability: 3,
  },
};

export const Empty: Story = {
  args: {
    players: [],
    probability: 0,
  },
};
