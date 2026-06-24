import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useState } from 'react';
import TabBar from './TabBar';

const meta = {
  title: 'Features/Lobby/TabBar',
  component: TabBar,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof TabBar>;

export default meta;

type Story = StoryObj<typeof TabBar>;

export const Default: Story = {
  render: () => {
    const [activeTab, setActiveTab] = useState(0);
    const tabs = ['QR코드', '초대코드'];

    return (
      <div style={{ width: '400px' }}>
        <TabBar tabs={tabs} activeTabIndex={activeTab} onTabChange={setActiveTab} />
        <div style={{ padding: '20px', textAlign: 'center' }}>
          <p>현재 활성 탭: {tabs[activeTab]}</p>
        </div>
      </div>
    );
  },
};

export const ThreeTabs: Story = {
  render: () => {
    const [activeTab, setActiveTab] = useState(0);
    const tabs = ['첫 번째', '두 번째', '세 번째'];

    return (
      <div style={{ width: '500px' }}>
        <TabBar tabs={tabs} activeTabIndex={activeTab} onTabChange={setActiveTab} />
        <div style={{ padding: '20px', textAlign: 'center' }}>
          <p>현재 활성 탭: {tabs[activeTab]}</p>
        </div>
      </div>
    );
  },
};
