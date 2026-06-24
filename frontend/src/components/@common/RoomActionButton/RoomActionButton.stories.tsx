import type { Meta, StoryObj } from '@storybook/react-webpack5';
import RoomActionButton from './RoomActionButton';

const meta = {
  title: 'Common/RoomActionButton',
  component: RoomActionButton,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof RoomActionButton>;

export default meta;

type Story = StoryObj<typeof RoomActionButton>;

export const Default: Story = {
  args: {
    title: '방만들기',
    descriptions: ['새로운 방을 만들어', '재미있는 커피내기를 시작해보세요 '],
    onClick: () => {},
  },
};
