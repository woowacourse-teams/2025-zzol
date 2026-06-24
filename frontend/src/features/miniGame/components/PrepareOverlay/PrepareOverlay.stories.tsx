import type { Meta, StoryObj } from '@storybook/react-webpack5';
import PrepareOverlay from './PrepareOverlay';

const meta = {
  title: 'Features/MiniGame/PrepareOverlay',
  component: PrepareOverlay,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof PrepareOverlay>;

export default meta;

type Story = StoryObj<typeof PrepareOverlay>;

export const Default: Story = {
  render: () => {
    return <PrepareOverlay />;
  },
};
