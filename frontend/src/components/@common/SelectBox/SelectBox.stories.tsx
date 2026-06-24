import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useState } from 'react';
import SelectBox, { Option } from './SelectBox';

const meta = {
  title: 'Common/SelectBox',
  component: SelectBox,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    width: {
      control: 'text',
      description: 'SelectBox의 너비',
    },
    height: {
      control: 'text',
      description: 'SelectBox의 높이',
    },
    placeholder: {
      control: 'text',
      description: '선택되지 않았을 때 표시되는 텍스트',
    },
    onChange: {
      action: 'changed',
      description: '옵션 선택 시 실행되는 함수',
    },
    onFocus: {
      action: 'focused',
      description: '포커스 시 실행되는 함수',
    },
    onBlur: {
      action: 'blurred',
      description: '포커스 해제 시 실행되는 함수',
    },
  },
} satisfies Meta<typeof SelectBox>;

export default meta;

type Story = StoryObj<typeof SelectBox>;

const basicOptions: Option[] = [
  { id: 1, name: '사과' },
  { id: 2, name: '바나나' },
  { id: 3, name: '오렌지' },
  { id: 4, name: '포도' },
];

export const Default: Story = {
  args: {
    options: basicOptions,
    placeholder: '과일을 선택하세요',
  },
};

export const WithValue: Story = {
  args: {
    options: basicOptions,
    value: 'banana',
    placeholder: '과일을 선택하세요',
  },
};

export const Interactive = () => {
  const [selectedValue, setSelectedValue] = useState<Option>({
    id: -1,
    name: '',
  });

  const coffeeOptions: Option[] = [
    { id: 1, name: '아이스 아메리카노' },
    { id: 2, name: '카페 라떼' },
    { id: 3, name: '카푸치노' },
    { id: 4, name: '마끼아또' },
    { id: 5, name: '카페 모카' },
    { id: 6, name: '에스프레소' },
  ];

  return (
    <div style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div>
        <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
          커피 선택
        </label>
        <SelectBox
          options={coffeeOptions}
          value={selectedValue.name}
          onChange={setSelectedValue}
          placeholder="커피를 선택하세요"
          width="300px"
          height="32px"
        />
      </div>
    </div>
  );
};
