export const HTTP_ERROR_MESSAGE = {
  400: {
    message: '요청을 처리할 수 없어요',
    description: '입력하신 정보를 다시 확인해주세요. 문제가 지속되면 잠시 후 다시 시도해주세요.',
  },
  404: {
    message: '페이지를 찾을 수 없어요',
    description: '요청하신 페이지가 존재하지 않거나 이동되었을 수 있어요. URL을 다시 확인해주세요.',
  },
  500: {
    message: '서버에 문제가 발생했어요',
    description: '일시적인 서버 오류이니 잠시 후 다시 시도해주세요.',
  },
};

export const NETWORK_ERROR_MESSAGE = {
  message: '네트워크 에러가 발생했어요',
  description: '네트워크 연결이 불안정합니다. 인터넷 연결을 확인하고 다시 시도해주세요.',
};

export const UNEXPECTED_ERROR_MESSAGE = {
  message: '예상치 못한 문제가 발생했어요',
  description: '일시적인 오류일 수 있습니다. 잠시 후 다시 시도해주세요.',
};
