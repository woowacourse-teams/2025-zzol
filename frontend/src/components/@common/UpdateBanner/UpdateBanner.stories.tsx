import { useState, type ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import UpdateBanner from './UpdateBanner';
import * as S from './UpdateBanner.styled';

const meta: Meta<typeof UpdateBanner> = {
  title: 'Common/UpdateBanner',
  component: UpdateBanner,
  tags: ['autodocs'],
  parameters: { layout: 'fullscreen' },
};

export default meta;
type Story = StoryObj<typeof UpdateBanner>;

const AppMock = ({ children }: { children: ReactNode }) => (
  <div
    style={{
      position: 'relative',
      height: '100vh',
      maxWidth: 430,
      margin: '0 auto',
      background: '#F9FAFB',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 8,
      color: '#99A1AF',
      fontSize: 13,
    }}
  >
    <span>앱 화면</span>
    <span style={{ fontSize: 11 }}>↓ 하단에서 배너를 확인하세요</span>
    {children}
  </div>
);

export const Visible: Story = {
  render: () => {
    const [dismissed, setDismissed] = useState(false);

    return (
      <AppMock>
        {!dismissed && (
          <S.Banner role="status" aria-live="polite">
            <S.Message>새로운 버전이 준비됐습니다</S.Message>
            <S.Actions>
              <S.UpdateButton type="button" onClick={() => {}}>
                업데이트
              </S.UpdateButton>
              <S.CloseButton type="button" onClick={() => setDismissed(true)} aria-label="닫기">
                ✕
              </S.CloseButton>
            </S.Actions>
          </S.Banner>
        )}
        {dismissed && (
          <span style={{ position: 'absolute', bottom: 96, fontSize: 12, color: '#99A1AF' }}>
            배너가 닫혔습니다
          </span>
        )}
      </AppMock>
    );
  },
};

export const Hidden: Story = {
  render: () => (
    <AppMock>
      <UpdateBanner />
    </AppMock>
  ),
  parameters: {
    docs: {
      description: {
        story: 'updateReady가 false일 때 아무것도 렌더링되지 않습니다.',
      },
    },
  },
};
