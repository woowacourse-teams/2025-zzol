import { Meta, StoryObj } from '@storybook/react-webpack5';
import ProgressCounter from './ProgressCounter';

const meta = {
  title: 'Common/ProgressCounter',
  component: ProgressCounter,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: '현재 진행 상황을 "현재/전체" 형태로 표시하는 컴포넌트입니다.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    current: {
      control: { type: 'number', min: 0 },
      description: '현재 진행 수',
    },
    total: {
      control: { type: 'number', min: 1 },
      description: '전체 수',
    },
  },
} satisfies Meta<typeof ProgressCounter>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    current: 0,
    total: 10,
  },
};

export const InProgress: Story = {
  args: {
    current: 5,
    total: 10,
  },
};
