import Headline3 from '@/components/@common/Headline3/Headline3';
import styled from '@emotion/styled';
import { expect } from '@storybook/jest';
import type { Meta, StoryContext, StoryObj } from '@storybook/react-webpack5';
import { userEvent, waitFor, within } from '@storybook/testing-library';
import { useState } from 'react';
import Modal from './Modal';
import useModal from './useModal';

const Button = styled.button`
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background-color: #0056b3;
  }
`;

const ContentWrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

const Scroll = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;

  max-height: 200px;
  overflow-y: scroll;
  padding-right: 8px;
`;

const meta = {
  title: 'Common/Modal',
  component: Modal,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof Modal>;

export default meta;

type Story = StoryObj<typeof Modal>;

export const Default: Story = {
  render: () => {
    const { openModal } = useModal();

    const ModalContent = () => (
      <p>옵션 없이 호출한 기본 모달입니다. 기본 모달은 닫기 버튼만 존재합니다.</p>
    );

    const handleOpen = () => {
      openModal(ModalContent());
    };

    return <Button onClick={handleOpen}>기본 모달</Button>;
  },
};

export const WithTitleAndCloseButton: Story = {
  render: () => {
    const { openModal } = useModal();

    const ModalContent = () => <p>제목과 닫기 버튼이 존재하는 모달입니다.</p>;

    const handleOpen = () => {
      openModal(ModalContent(), { title: '제목/닫기 있는 모달', showCloseButton: true });
    };

    return <Button onClick={handleOpen}>제목/닫기 있는 모달</Button>;
  },
};

export const WithoutTitle: Story = {
  render: () => {
    const { openModal, closeModal } = useModal();

    const ModalContent = () => {
      return (
        <ContentWrapper>
          <p>제목이 없는 모달입니다.</p>
          <Button onClick={closeModal}>확인</Button>
        </ContentWrapper>
      );
    };

    const handleOpen = () => {
      openModal(ModalContent(), { showCloseButton: true });
    };

    return <Button onClick={handleOpen}>제목 없는 모달</Button>;
  },
};

export const WithoutCloseButton: Story = {
  render: () => {
    const { openModal, closeModal } = useModal();

    const ModalContent = () => {
      return (
        <ContentWrapper>
          <p>닫기 버튼이 없는 모달입니다.</p>
          <Button onClick={closeModal}>확인</Button>
        </ContentWrapper>
      );
    };

    const handleOpen = () => {
      openModal(ModalContent(), { title: '닫기 버튼 없는 모달', showCloseButton: false });
    };

    return <Button onClick={handleOpen}>닫기 버튼 없는 모달</Button>;
  },
};

export const WithoutHeader: Story = {
  render: () => {
    const { openModal, closeModal } = useModal();

    const ModalContent = () => {
      return (
        <ContentWrapper>
          <Headline3>커스텀 헤더</Headline3>
          <p>Modal.Header를 사용하지 않은 모달입니다.</p>
          <Button onClick={closeModal}>닫기</Button>
        </ContentWrapper>
      );
    };

    const handleOpen = () => {
      openModal(ModalContent(), { showCloseButton: false });
    };

    return <Button onClick={handleOpen}>헤더 없는 모달</Button>;
  },
};

export const WithScrollContent: Story = {
  render: () => {
    const { openModal, closeModal } = useModal();

    const ModalContent = () => {
      return (
        <ContentWrapper>
          <p>위쪽 고정 텍스트입니다.</p>
          <Scroll>
            {Array.from({ length: 30 }, (_, index) => (
              <p key={index}>스크롤 테스트용 텍스트입니다.</p>
            ))}
          </Scroll>
          <p>아래쪽 고정 텍스트입니다.</p>
          <Button onClick={closeModal}>닫기</Button>
        </ContentWrapper>
      );
    };

    const handleOpen = () => {
      openModal(ModalContent(), { title: '스크롤 모달', showCloseButton: true });
    };

    return <Button onClick={handleOpen}>스크롤 모달</Button>;
  },
};

const ModalForInteraction = () => {
  const [isOpen, setIsOpen] = useState(false);

  const closeModal = () => setIsOpen(false);
  const openModal = () => setIsOpen(true);

  return (
    <>
      <Button type="button" onClick={openModal} data-testid="open-modal-btn">
        모달 열기
      </Button>
      <Modal isOpen={isOpen} onClose={closeModal} title="모달 상호작용 테스트" data-testid="modal">
        <div data-testid="modal-content">
          <p>모달 내용</p>
        </div>
      </Modal>
    </>
  );
};

export const ModalOpenAndClose = {
  render: () => <ModalForInteraction />,
  play: async ({ canvasElement, step }: StoryContext) => {
    const canvas = within(canvasElement);

    await step('초기에 모달이 보이지 않는지 확인', async () => {
      const modalContent = canvas.queryByTestId('modal-content');
      expect(modalContent).not.toBeInTheDocument();
    });

    await step('모달 열기 버튼 클릭', async () => {
      const openButton = canvas.getByTestId('open-modal-btn');
      await userEvent.click(openButton);
    });

    await step('모달이 열렸는지 확인', async () => {
      const modalContent = within(document.body).getByTestId('modal-content');
      expect(modalContent).toBeInTheDocument();

      const modalTitle = within(document.body).getByText('모달 상호작용 테스트');
      expect(modalTitle).toBeInTheDocument();
    });

    await step('모달 내부의 닫기(아이콘) 버튼 클릭', async () => {
      const closeIconButton = within(document.body).getByRole('button', { name: '모달 닫기' });
      await userEvent.click(closeIconButton);
    });

    await step('모달이 닫혔는지 확인', async () => {
      await waitFor(() => {
        const modalContent = within(document.body).queryByTestId('modal-content');
        expect(modalContent).not.toBeInTheDocument();
      });
    });
  },
};

export const ModalCloseWithEscapeKey = {
  render: () => <ModalForInteraction />,
  play: async ({ canvasElement, step }: StoryContext) => {
    const canvas = within(canvasElement);

    await step('모달 열기 버튼 클릭', async () => {
      const openButton = canvas.getByTestId('open-modal-btn');
      await userEvent.click(openButton);
    });

    await step('ESC 키를 눌러 모달 닫기', async () => {
      await userEvent.keyboard('{Escape}');
    });

    await step('모달이 닫혔는지 확인', async () => {
      await waitFor(() => {
        const modalContent = within(document.body).queryByTestId('modal-content');
        expect(modalContent).not.toBeInTheDocument();
      });
    });
  },
};
