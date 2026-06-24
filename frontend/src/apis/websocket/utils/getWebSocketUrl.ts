export const getWebSocketUrl = (): string => {
  const API_URL = process.env.API_URL;

  if (!API_URL) {
    throw new Error('API_URL가 정의되지 않았습니다');
  }

  return API_URL + '/ws';
};
