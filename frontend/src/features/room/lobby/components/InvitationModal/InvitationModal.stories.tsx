import type { Meta, StoryObj } from '@storybook/react-webpack5';
import InvitationModal from './InvitationModal';
import useModal from '@/components/@common/Modal/useModal';

const meta = {
  title: 'Features/Lobby/InvitationModal',
  component: InvitationModal,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof InvitationModal>;

export default meta;

type Story = StoryObj<typeof InvitationModal>;

export const Default: Story = {
  render: () => {
    const { openModal } = useModal();

    const ModalContent = () => <InvitationModal onClose={() => {}} />;

    const handleOpen = () => {
      openModal(ModalContent());
    };

    return <button onClick={handleOpen}>Invitation 모달 열기</button>;
  },
};
