import type { Meta, StoryObj } from '@storybook/react-webpack5';
import TextButton from './TextButton';

const meta: Meta<typeof TextButton> = {
  title: 'Common/TextButton',
  component: TextButton,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    text: '확률 보기',
    onClick: () => console.log('확률 보기 버튼 클릭!'),
  },
};
