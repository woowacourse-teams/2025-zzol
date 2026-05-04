import { useEffect, useState } from 'react';
import { api } from '@/apis/rest/api';

export type PatchNoteCategory = 'NOTICE' | 'EVENT' | 'UPDATE' | 'MAINTENANCE';

export type PatchNote = {
  id: number;
  category: PatchNoteCategory;
  categoryLabel: string;
  title: string;
  content: string;
  createdAt: string | number;
  updatedAt: string | number;
};

// 백엔드가 초 단위 Unix timestamp 또는 ISO 문자열 둘 다 허용
export const formatPatchNoteDate = (value: string | number): string => {
  const ms =
    typeof value === 'number'
      ? value < 1e12
        ? value * 1000 // seconds → ms
        : value // already ms
      : new Date(value).getTime();
  return new Date(ms)
    .toLocaleDateString('ko-KR', {
      timeZone: 'Asia/Seoul',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
    .replace(/\. /g, '.')
    .replace(/\.$/, '');
};

export const useLatestPatchNote = () => {
  const [data, setData] = useState<PatchNote | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<PatchNote>('/patch-notes/latest', { bypassAuth: true })
      .then((result) => setData('id' in result ? result : null))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  return { data, loading };
};

export const usePatchNoteList = () => {
  const [data, setData] = useState<PatchNote[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api
      .get<PatchNote[]>('/patch-notes', { bypassAuth: true })
      .then(setData)
      .catch(() => setData([]))
      .finally(() => setLoading(false));
  }, []);

  return { data, loading };
};
