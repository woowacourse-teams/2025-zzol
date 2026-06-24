import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Paragraph from './Paragraph';

const meta = {
  title: 'Common/Paragraph',
  component: Paragraph,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Paragraph>;

export default meta;

type Story = StoryObj<typeof Paragraph>;

export const Default: Story = {
  args: {
    children: 'Paragraph 텍스트',
  },
};

export const LongText: Story = {
  args: {
    children: '이것은 긴 Paragraph 텍스트 예시입니다. 여러 줄에 걸쳐 표시될 수 있습니다.',
  },
};

export const ShortText: Story = {
  args: {
    children: '짧은 본문',
  },
};
