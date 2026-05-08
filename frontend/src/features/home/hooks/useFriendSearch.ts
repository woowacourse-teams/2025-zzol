import { ChangeEvent, useCallback, useEffect, useRef, useState } from 'react';
import { friendsApi } from '@/features/friends/api/friendsApi';
import { SearchedUser } from '@/features/friends/types';

type SearchMode = '닉네임' | '유저코드';

const DEBOUNCE_MS = 300;

export const useFriendSearch = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchMode, setSearchMode] = useState<SearchMode>('닉네임');
  const [searchResults, setSearchResults] = useState<SearchedUser[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const searchIdRef = useRef(0);

  const doSearch = useCallback(async (query: string, mode: SearchMode) => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    if (mode === '유저코드' && query.trim().length !== 5) return;

    const id = ++searchIdRef.current;
    try {
      setSearchLoading(true);
      const results =
        mode === '닉네임'
          ? await friendsApi.searchByNickname(query.trim())
          : await friendsApi.searchByUserCode(query.trim());
      if (id === searchIdRef.current) setSearchResults(results);
    } catch {
      if (id === searchIdRef.current) setSearchResults([]);
    } finally {
      if (id === searchIdRef.current) setSearchLoading(false);
    }
  }, []);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return () => {
        if (debounceRef.current) clearTimeout(debounceRef.current);
      };
    }
    debounceRef.current = setTimeout(() => {
      doSearch(searchQuery, searchMode);
    }, DEBOUNCE_MS);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [searchQuery, searchMode, doSearch]);

  const handleSearchChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = searchMode === '유저코드' ? e.target.value.toUpperCase() : e.target.value;
    setSearchQuery(value);
  };

  const handleModeChange = (mode: SearchMode) => {
    setSearchMode(mode);
    setSearchQuery('');
    setSearchResults([]);
  };

  return {
    searchQuery,
    searchMode,
    searchResults,
    searchLoading,
    isSearching: searchQuery.trim().length > 0,
    handleSearchChange,
    handleModeChange,
  };
};
