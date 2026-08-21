import { ThemeProvider } from '@emotion/react';
import { render, screen } from '@testing-library/react';
import { createContext, PropsWithChildren, useContext, useEffect } from 'react';
import { theme } from '@/styles/theme';
import { ModalOutlet, ModalProvider } from './ModalContext';
import useModal from './useModal';

/** ModalProvider보다 아래에 있는 Provider. 모달 내용이 여기 닿지 못하면 throw한다(App의 FriendsProvider와 같은 위치). */
const DeepContext = createContext<string | null>(null);

const DeepProvider = ({ children }: PropsWithChildren) => (
  <DeepContext.Provider value="깊은 컨텍스트">{children}</DeepContext.Provider>
);

const ModalContent = () => {
  const value = useContext(DeepContext);
  if (!value) {
    throw new Error('DeepProvider 밖에서 렌더됐다');
  }
  return <div>{value}</div>;
};

const Opener = () => {
  const { openModal } = useModal();
  useEffect(() => {
    openModal(<ModalContent />);
  }, [openModal]);
  return null;
};

it('모달 내용은 ModalProvider보다 아래에 있는 컨텍스트를 읽을 수 있다', () => {
  render(
    <ThemeProvider theme={theme}>
      <ModalProvider>
        <DeepProvider>
          <Opener />
          <ModalOutlet />
        </DeepProvider>
      </ModalProvider>
    </ThemeProvider>
  );

  expect(screen.getByText('깊은 컨텍스트')).toBeInTheDocument();
});
