import type { Meta, StoryObj } from '@storybook/react-webpack5';
import RouletteWheelBack from './RouletteWheelBack';

const meta: Meta<typeof RouletteWheelBack> = {
  title: 'Features/Roulette/RouletteWheelBack',
  component: RouletteWheelBack,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component:
          '룰렛 휠의 뒷면을 보여주는 컴포넌트입니다. 원판과 핀만 있는 단순한 형태로 구성되어 있습니다.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {},
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  name: '기본 룰렛 뒷면',
  parameters: {
    docs: {
      description: {
        story: '룰렛 휠의 뒷면을 표시합니다. 원형 배경과 중앙의 핀으로 구성되어 있습니다.',
      },
    },
  },
};
