import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Headline1 from './Headline1';

const meta = {
  title: 'Common/Headline1',
  component: Headline1,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Headline1>;

export default meta;

type Story = StoryObj<typeof Headline1>;

export const Default: Story = {
  args: {
    children: 'Headline 1 텍스트',
  },
};

export const LongText: Story = {
  args: {
    children: '이것은 긴 Headline 1 텍스트 예시입니다. 여러 줄에 걸쳐 표시될 수 있습니다.',
  },
};

export const ShortText: Story = {
  args: {
    children: '짧은 제목',
  },
};
