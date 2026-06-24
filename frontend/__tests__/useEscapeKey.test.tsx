import Modal from '@/components/@common/Modal/Modal';
import { theme } from '@/styles/theme';
import { ThemeProvider } from '@emotion/react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

describe('useEscapeKey 훅 테스트: Modal 컴포넌트 활용', () => {
  it('ESC 키를 누르면 onClose가 호출된다.', async () => {
    const onClose = jest.fn();

    render(
      <ThemeProvider theme={theme}>
        <Modal isOpen={true} onClose={onClose} title="타이틀">
          <button>버튼</button>
        </Modal>
      </ThemeProvider>
    );

    const user = userEvent.setup();
    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalled();
  });
});
