import { createContext, PropsWithChildren, ReactNode, useCallback, useState } from 'react';
import Modal from './Modal';

type Options = {
  title?: string;
  showCloseButton?: boolean;
  closeOnBackdropClick?: boolean;
};

type ModalContextType = {
  openModal: (content: ReactNode, options?: Options) => void;
  closeModal: () => void;
};

export const ModalContext = createContext<ModalContextType | null>(null);

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

  return (
    <ModalContext.Provider value={{ openModal, closeModal }}>
      {children}
      <Modal
        isOpen={content !== null}
        onClose={closeModal}
        title={options.title}
        showCloseButton={options.showCloseButton}
        closeOnBackdropClick={options.closeOnBackdropClick}
      >
        {content}
      </Modal>
    </ModalContext.Provider>
  );
};
