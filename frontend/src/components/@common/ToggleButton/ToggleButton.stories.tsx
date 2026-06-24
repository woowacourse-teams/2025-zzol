import type { Meta, StoryObj } from '@storybook/react-webpack5';
import ToggleButton from './ToggleButton';
import { useState } from 'react';

const meta: Meta<typeof ToggleButton> = {
  title: 'Common/ToggleButton',
  component: ToggleButton,
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<typeof ToggleButton>;

export const Default: Story = {
  render: (args) => {
    const [selectedOption, setSelectedOption] = useState(args.selectedOption);
    return (
      <ToggleButton {...args} selectedOption={selectedOption} onSelectOption={setSelectedOption} />
    );
  },
  args: {
    options: ['Option 1', 'Option 2', 'Option 3'],
    selectedOption: 'Option 1',
  },
};

export const TwoOptions: Story = {
  render: (args) => {
    const [selectedOption, setSelectedOption] = useState(args.selectedOption);
    return (
      <ToggleButton {...args} selectedOption={selectedOption} onSelectOption={setSelectedOption} />
    );
  },
  args: {
    options: ['A', 'B'],
    selectedOption: 'A',
  },
};
