import Modal from '@/components/@common/Modal/Modal';
import { theme } from '@/styles/theme';
import { ThemeProvider } from '@emotion/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

describe('useFocusTrap 훅 테스트: Modal 컴포넌트 활용', () => {
  it('모달이 열리면 첫 번째 버튼에 포커스가 잡힌다.', () => {
    render(
      <ThemeProvider theme={theme}>
        <Modal isOpen={true} onClose={() => {}} title="제목" showCloseButton={false}>
          <button>첫번째</button>
          <button>두번째</button>
        </Modal>
      </ThemeProvider>
    );
    expect(screen.getByText('첫번째')).toHaveFocus();
  });

  it('Tab 키로 포커스가 모달 내부에서만 순환한다.', async () => {
    render(
      <ThemeProvider theme={theme}>
        <Modal isOpen={true} onClose={() => {}} title="제목" showCloseButton={false}>
          <button>첫번째</button>
          <button>두번째</button>
        </Modal>
      </ThemeProvider>
    );
    const user = userEvent.setup();
    const first = screen.getByText('첫번째');
    const second = screen.getByText('두번째');

    expect(first).toHaveFocus();

    await user.tab();
    expect(second).toHaveFocus();

    // 순환
    await user.tab();
    expect(first).toHaveFocus();

    // 역순환
    await user.tab({ shift: true });
    expect(second).toHaveFocus();
  });
});
