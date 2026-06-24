import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useEffect, useState } from 'react';
import CircularProgress from './CircularProgress';

const meta: Meta<typeof CircularProgress> = {
  title: 'Features/MiniGame/CardGame/CircularProgress',
  component: CircularProgress,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    current: {
      control: { type: 'number', min: 0, max: 10 },
      description: '현재 카운트',
    },
    total: {
      control: { type: 'number', min: 1, max: 10 },
      description: '전체 카운트',
    },
    size: {
      control: { type: 'text' },
      description: 'Progress 크기 (rem 단위, 예: "2rem")',
    },
  },
  tags: ['autodocs'],
};

export default meta;

type Story = StoryObj<typeof meta>;

export const CountdownAnimation: Story = {
  render: () => {
    const [key, setKey] = useState(0);
    const [current, setCurrent] = useState(10);

    useEffect(() => {
      if (current > 0) {
        const timer = setTimeout(() => setCurrent(current - 1), 1000);
        return () => clearTimeout(timer);
      }
    }, [current]);

    const handleReset = () => {
      setCurrent(10);
      setKey((prev) => prev + 1);
    };

    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
        <CircularProgress key={key} current={current} total={10} />
        <button onClick={handleReset}>리셋 버튼</button>
      </div>
    );
  },
};
