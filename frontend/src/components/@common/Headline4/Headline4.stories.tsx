import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Headline4 from './Headline4';

const meta = {
  title: 'Common/Headline4',
  component: Headline4,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Headline4>;

export default meta;

type Story = StoryObj<typeof Headline4>;

export const Default: Story = {
  args: {
    children: 'Headline 4 텍스트',
  },
};

export const LongText: Story = {
  args: {
    children: '이것은 긴 Headline 4 텍스트 예시입니다. 여러 줄에 걸쳐 표시될 수 있습니다.',
  },
};

export const ShortText: Story = {
  args: {
    children: '짧은 제목',
  },
};
