import styled from '@emotion/styled';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Toast from './Toast';
import useToast from './useToast';

const Button = styled.button`
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin: 5px;

  &:hover {
    background-color: #0056b3;
  }
`;

const ButtonGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
`;

const meta: Meta<typeof Toast> = {
  title: 'Common/Toast',
  component: Toast,
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <>
        <div id="toast-root" />
        <Story />
      </>
    ),
  ],
};

export default meta;
type Story = StoryObj<typeof Toast>;

export const Success: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowSuccess = () => {
      showToast({
        type: 'success',
        message: '성공적으로 저장되었습니다!',
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowSuccess}>성공 토스트 표시</Button>
      </ButtonGroup>
    );
  },
};

export const Error: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowError = () => {
      showToast({
        type: 'error',
        message: '오류가 발생했습니다. 다시 시도해주세요.',
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowError}>오류 토스트 표시</Button>
      </ButtonGroup>
    );
  },
};

export const Warning: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowWarning = () => {
      showToast({
        type: 'warning',
        message: '주의: 이 작업은 되돌릴 수 없습니다.',
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowWarning}>경고 토스트 표시</Button>
      </ButtonGroup>
    );
  },
};

export const Info: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowInfo = () => {
      showToast({
        type: 'info',
        message: '새로운 메시지가 도착했습니다.',
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowInfo}>정보 토스트 표시</Button>
      </ButtonGroup>
    );
  },
};

export const LongMessage: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowLongMessage = () => {
      showToast({
        type: 'info',
        message:
          '이것은 매우 긴 메시지입니다. 토스트가 긴 텍스트를 어떻게 처리하는지 확인해보세요. 이 메시지는 여러 줄에 걸쳐 표시될 수 있습니다.',
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowLongMessage}>긴 메시지 토스트 표시</Button>
      </ButtonGroup>
    );
  },
};

export const ShortMessage: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowShortMessage = () => {
      showToast({
        type: 'success',
        message: '완료',
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowShortMessage}>짧은 메시지 토스트 표시</Button>
      </ButtonGroup>
    );
  },
};

export const AllTypes: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowSuccess = () => {
      showToast({
        type: 'success',
        message: '성공 메시지',
      });
    };

    const handleShowError = () => {
      showToast({
        type: 'error',
        message: '오류 메시지',
      });
    };

    const handleShowWarning = () => {
      showToast({
        type: 'warning',
        message: '경고 메시지',
      });
    };

    const handleShowInfo = () => {
      showToast({
        type: 'info',
        message: '정보 메시지',
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowSuccess}>성공 토스트</Button>
        <Button onClick={handleShowError}>오류 토스트</Button>
        <Button onClick={handleShowWarning}>경고 토스트</Button>
        <Button onClick={handleShowInfo}>정보 토스트</Button>
      </ButtonGroup>
    );
  },
  parameters: {
    docs: {
      description: {
        story:
          '모든 토스트 타입을 테스트할 수 있습니다. 각 버튼을 클릭하여 다양한 토스트를 확인해보세요.',
      },
    },
  },
};

export const CustomDuration: Story = {
  render: () => {
    const { showToast } = useToast();

    const handleShowShortDuration = () => {
      showToast({
        type: 'info',
        message: '1초 후 사라지는 토스트',
        duration: 1000,
      });
    };

    const handleShowLongDuration = () => {
      showToast({
        type: 'warning',
        message: '10초 후 사라지는 토스트',
        duration: 10000,
      });
    };

    return (
      <ButtonGroup>
        <Button onClick={handleShowShortDuration}>1초 토스트</Button>
        <Button onClick={handleShowLongDuration}>10초 토스트</Button>
      </ButtonGroup>
    );
  },
  parameters: {
    docs: {
      description: {
        story: '다양한 지속 시간을 가진 토스트를 테스트할 수 있습니다.',
      },
    },
  },
};
