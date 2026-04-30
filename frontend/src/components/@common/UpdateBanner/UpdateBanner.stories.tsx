import type { Meta, StoryObj } from '@storybook/react-webpack5';
import UpdateBanner from './UpdateBanner';
import * as S from './UpdateBanner.styled';

const meta: Meta<typeof UpdateBanner> = {
  title: 'Common/UpdateBanner',
  component: UpdateBanner,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof UpdateBanner>;

// SW 컨텍스트가 없는 Storybook 환경에서는 hook이 updateReady=false를 반환하므로
// 배너의 시각적 상태는 styled 컴포넌트로 직접 렌더링한다
export const Visible: Story = {
  render: () => (
    <S.Banner role="status" aria-live="polite">
      <S.Message>새로운 버전이 준비됐습니다</S.Message>
      <S.UpdateButton type="button" onClick={() => {}}>
        업데이트
      </S.UpdateButton>
    </S.Banner>
  ),
};

export const Hidden: Story = {
  render: () => <UpdateBanner />,
  parameters: {
    docs: {
      description: {
        story: 'updateReady가 false일 때 아무것도 렌더링되지 않습니다.',
      },
    },
  },
};
