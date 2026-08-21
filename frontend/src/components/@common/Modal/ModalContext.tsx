import {
  createContext,
  PropsWithChildren,
  ReactNode,
  useCallback,
  useContext,
  useMemo,
  useState,
} from 'react';
import Modal from './Modal';

type Options = {
  title?: string;
  showCloseButton?: boolean;
  closeOnBackdropClick?: boolean;
  showBottomCloseButton?: boolean;
};

type ModalContextType = {
  openModal: (content: ReactNode, options?: Options) => void;
  closeModal: () => void;
};

type ModalStateType = {
  content: ReactNode | null;
  options: Options;
  closeModal: () => void;
};

export const ModalContext = createContext<ModalContextType | null>(null);

const ModalStateContext = createContext<ModalStateType | null>(null);

export const ModalProvider = ({ children }: PropsWithChildren) => {
  const [content, setContent] = useState<ReactNode | null>(null);
  const [options, setOptions] = useState<Options>({});

  const openModal = useCallback((content: ReactNode, options: Options = {}) => {
    setContent(content);
    setOptions(options);
  }, []);

  const closeModal = useCallback(() => {
    setContent(null);
    setOptions({});
  }, []);

  // 모달을 여닫아도 useModal 소비자가 다시 렌더되지 않도록 고정한다. 바뀌는 값은 ModalStateContext에만 있다.
  const controls = useMemo(() => ({ openModal, closeModal }), [openModal, closeModal]);

  return (
    <ModalContext.Provider value={controls}>
      <ModalStateContext.Provider value={{ content, options, closeModal }}>
        {children}
      </ModalStateContext.Provider>
    </ModalContext.Provider>
  );
};

/**
 * 모달 내용이 실제로 렌더되는 지점. Provider가 직접 렌더하지 않고 이 컴포넌트를 따로 두는 이유는,
 * openModal에 넘긴 엘리먼트의 컨텍스트가 <b>만든 곳이 아니라 렌더되는 트리 위치</b>로 정해지기 때문이다.
 * Provider가 {children}의 형제로 렌더하면 모달 내용은 ModalProvider 아래의 어떤 Provider에도 닿지 못해,
 * 그 컨텍스트를 쓰는 모달이 열리는 순간 throw한다.
 *
 * <p>그래서 이 컴포넌트를 <b>모달이 필요로 하는 모든 Provider 안쪽</b>에 두어야 한다(App.tsx).
 */
export const ModalOutlet = () => {
  const state = useContext(ModalStateContext);
  if (!state) {
    throw new Error('ModalOutlet 은 ModalProvider 안에서 사용해야 합니다.');
  }

  const { content, options, closeModal } = state;

  return (
    <Modal
      isOpen={content !== null}
      onClose={closeModal}
      title={options.title}
      showCloseButton={options.showCloseButton}
      closeOnBackdropClick={options.closeOnBackdropClick}
      showBottomCloseButton={options.showBottomCloseButton}
    >
      {content}
    </Modal>
  );
};
