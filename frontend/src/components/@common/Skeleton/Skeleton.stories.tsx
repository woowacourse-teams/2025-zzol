import type { Meta, StoryObj } from '@storybook/react-webpack5';
import Skeleton from './Skeleton';

const meta = {
  title: 'Common/Skeleton',
  component: Skeleton,
  parameters: {
    layout: 'padded',
  },
  tags: ['autodocs'],
  argTypes: {
    width: {
      control: 'text',
      description: '스켈레톤의 너비 (px 숫자 또는 문자열)',
    },
    height: {
      control: 'text',
      description: '스켈레톤의 높이 (px 숫자 또는 문자열)',
    },
    borderRadius: {
      control: 'text',
      description: '스켈레톤의 border radius (px 숫자 또는 문자열)',
    },
  },
} satisfies Meta<typeof Skeleton>;

export default meta;

type Story = StoryObj<typeof Skeleton>;

export const Default: Story = {
  args: {
    width: '100%',
    height: '20px',
    borderRadius: '4px',
  },
};

export const Text: Story = {
  args: {
    width: '200px',
    height: '16px',
    borderRadius: '4px',
  },
  parameters: {
    docs: {
      description: {
        story: '텍스트 로딩을 표현하는 스켈레톤입니다.',
      },
    },
  },
};

export const Circle: Story = {
  args: {
    width: 50,
    height: 50,
    borderRadius: '50%',
  },
  parameters: {
    docs: {
      description: {
        story: '원형 아이콘이나 아바타 로딩을 표현하는 스켈레톤입니다.',
      },
    },
  },
};

export const Button: Story = {
  args: {
    width: 120,
    height: 40,
    borderRadius: '8px',
  },
  parameters: {
    docs: {
      description: {
        story: '버튼 로딩을 표현하는 스켈레톤입니다.',
      },
    },
  },
};

export const Card: Story = {
  args: {
    width: '300px',
    height: '200px',
    borderRadius: '12px',
  },
  parameters: {
    docs: {
      description: {
        story: '카드 컴포넌트 로딩을 표현하는 스켈레톤입니다.',
      },
    },
  },
};

export const MultipleLines: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxWidth: '400px' }}>
      <Skeleton width="100%" height={20} />
      <Skeleton width="90%" height={20} />
      <Skeleton width="95%" height={20} />
      <Skeleton width="85%" height={20} />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '여러 줄의 텍스트 로딩을 표현하는 스켈레톤입니다.',
      },
    },
  },
};

export const ProfileCard: Story = {
  render: () => (
    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '16px' }}>
      <Skeleton width={60} height={60} borderRadius="50%" />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
        <Skeleton width="60%" height={18} />
        <Skeleton width="40%" height={14} />
      </div>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '프로필 카드 형태의 스켈레톤 조합 예시입니다.',
      },
    },
  },
};

export const ListItems: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', maxWidth: '500px' }}>
      {Array.from({ length: 5 }).map((_, index) => (
        <div key={index} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Skeleton width={40} height={40} borderRadius="8px" />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <Skeleton width="70%" height={16} />
            <Skeleton width="50%" height={12} />
          </div>
        </div>
      ))}
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '리스트 아이템 형태의 스켈레톤 조합 예시입니다.',
      },
    },
  },
};

export const Sizes: Story = {
  render: () => (
    <div
      style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'flex-start' }}
    >
      <div>
        <p style={{ marginBottom: '8px', fontSize: '14px', color: '#666' }}>Small (12px)</p>
        <Skeleton width="200px" height={12} />
      </div>
      <div>
        <p style={{ marginBottom: '8px', fontSize: '14px', color: '#666' }}>Medium (16px)</p>
        <Skeleton width="200px" height={16} />
      </div>
      <div>
        <p style={{ marginBottom: '8px', fontSize: '14px', color: '#666' }}>Large (20px)</p>
        <Skeleton width="200px" height={20} />
      </div>
      <div>
        <p style={{ marginBottom: '8px', fontSize: '14px', color: '#666' }}>Extra Large (24px)</p>
        <Skeleton width="200px" height={24} />
      </div>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: '다양한 크기의 스켈레톤입니다.',
      },
    },
  },
};
