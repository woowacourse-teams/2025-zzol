import type { Meta, StoryObj } from '@storybook/react-webpack5';
import RankItem from './RankItem';

const meta: Meta<typeof RankItem> = {
  title: 'Features/MiniGame/RacingGame/RankItem/RankItem',
  component: RankItem,
  parameters: {
    layout: 'centered',
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => (
    <div style={{ backgroundColor: '#354557', padding: '20px', borderRadius: '8px' }}>
      <RankItem playerName="Player1" rank={1} isMe={false} isFixed={false} />
    </div>
  ),
};

export const Fixed: Story = {
  render: () => (
    <div style={{ backgroundColor: '#354557', padding: '20px', borderRadius: '8px' }}>
      <RankItem playerName="Player1" rank={1} isMe={false} isFixed={true} />
    </div>
  ),
};
