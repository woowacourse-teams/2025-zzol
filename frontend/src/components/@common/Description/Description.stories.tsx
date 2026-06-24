import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Description from './Description';

const meta = {
  title: 'Common/Description',
  component: Description,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Description>;

export default meta;

type Story = StoryObj<typeof Description>;

export const Default: Story = {
  args: {
    children: 'Description 텍스트',
  },
};

export const LongText: Story = {
  args: {
    children: '이것은 긴 Description 텍스트 예시입니다. 여러 줄에 걸쳐 표시될 수 있습니다.',
  },
};

export const ShortText: Story = {
  args: {
    children: '짧은 본문',
  },
};
