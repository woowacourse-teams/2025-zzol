import type { Meta, StoryObj } from '@storybook/react-webpack5';
import ProbabilityTag from './ProbabilityTag';

const meta: Meta<typeof ProbabilityTag> = {
  title: 'Common/ProbabilityTag',
  component: ProbabilityTag,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  decorators: [
    (Story) => (
      <div
        style={{
          background: '#ff6b6b',
          padding: '2rem',
          borderRadius: '20px',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
        }}
      >
        <Story />
      </div>
    ),
  ],
  argTypes: {
    probability: {
      control: { type: 'range', min: 1, max: 100, step: 1 },
      description: '확률 (%)',
    },
  },
  args: {
    probability: 5,
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const DifferentProbabilities: Story = {
  render: () => (
    <div
      style={{
        background: '#ff6b6b',
        padding: '2rem',
        borderRadius: '20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
        alignItems: 'center',
      }}
    >
      <ProbabilityTag probability={1} />
      <ProbabilityTag probability={5} />
      <ProbabilityTag probability={10} />
    </div>
  ),
  parameters: {
    controls: { disable: true },
  },
};
