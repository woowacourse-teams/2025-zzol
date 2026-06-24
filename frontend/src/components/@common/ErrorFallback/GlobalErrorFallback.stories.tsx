import type { Meta, StoryObj } from '@storybook/react-webpack5';
import GlobalErrorFallback from './GlobalErrorFallback';
import { ApiError } from '@/apis/rest/error';

const meta: Meta<typeof GlobalErrorFallback> = {
  title: 'Common/GlobalErrorFallback',
  component: GlobalErrorFallback,
  tags: ['autodocs'],
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component:
          '전역 에러 발생 시 표시되는 폴백 컴포넌트입니다. 메인 페이지로 돌아가는 버튼을 제공합니다.',
      },
    },
  },
  argTypes: {
    error: {
      description: '발생한 에러 객체',
      control: false,
    },
  },
};

export default meta;

type Story = StoryObj<typeof GlobalErrorFallback>;

export const BadRequest: Story = {
  args: {
    error: new ApiError({
      status: 400,
      message: 'Bad Request',
      data: null,
      displayMode: 'fallback',
    }),
  },
  parameters: {
    docs: {
      description: {
        story: '400 Bad Request 에러가 발생했을 때의 화면입니다.',
      },
    },
  },
};

export const Forbidden: Story = {
  args: {
    error: new ApiError({
      status: 403,
      message: 'Forbidden',
      data: null,
      displayMode: 'fallback',
    }),
  },
  parameters: {
    docs: {
      description: {
        story: '403 Forbidden 에러가 발생했을 때의 화면입니다.',
      },
    },
  },
};

export const NotFound: Story = {
  args: {
    error: new ApiError({
      status: 404,
      message: 'Not Found',
      data: null,
      displayMode: 'fallback',
    }),
  },
  parameters: {
    docs: {
      description: {
        story: '404 Not Found 에러가 발생했을 때의 화면입니다.',
      },
    },
  },
};

export const InternalServerError: Story = {
  args: {
    error: new ApiError({
      status: 500,
      message: 'Internal Server Error',
      data: null,
      displayMode: 'fallback',
    }),
  },
  parameters: {
    docs: {
      description: {
        story: '500 Internal Server Error가 발생했을 때의 화면입니다.',
      },
    },
  },
};

export const UnknownError: Story = {
  args: {
    error: new Error('Unknown Error'),
  },
  parameters: {
    docs: {
      description: {
        story: '예상치 못한 에러가 발생했을 때의 화면입니다.',
      },
    },
  },
};

export const UndefinedHttpError: Story = {
  args: {
    error: new ApiError({
      status: 418,
      message: "I'm a teapot",
      data: null,
      displayMode: 'fallback',
    }),
  },
  parameters: {
    docs: {
      description: {
        story: '정의되지 않은 HTTP 상태 코드(418)가 발생했을 때의 화면입니다.',
      },
    },
  },
};
