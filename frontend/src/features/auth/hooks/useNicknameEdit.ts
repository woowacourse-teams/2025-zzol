import { type KeyboardEvent, useEffect, useRef, useState } from 'react';
import useToast from '@/components/@common/Toast/useToast';
import { ApiError } from '@/apis/rest/error';
import { useAuth } from '../contexts/AuthContext';

export const MAX_NICKNAME_LENGTH = 10;

export const useNicknameEdit = () => {
  const { user, updateNickname } = useAuth();
  const { showToast } = useToast();

  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isEditing) inputRef.current?.focus();
  }, [isEditing]);

  const handleEditStart = () => {
    if (!user) return;
    setEditValue(user.nickname);
    setIsEditing(true);
  };

  const handleEditSave = async () => {
    if (!user) return;
    const trimmed = editValue.trim();
    if (!trimmed) {
      showToast({ message: '닉네임을 입력해주세요.', type: 'error' });
      return;
    }
    if (trimmed === user.nickname) {
      setIsEditing(false);
      return;
    }
    setIsSaving(true);
    try {
      await updateNickname(trimmed);
      showToast({ message: '닉네임이 변경되었습니다.', type: 'success' });
      setIsEditing(false);
    } catch (e) {
      const message =
        e instanceof ApiError && e.status === 400 ? e.message : '닉네임 변경에 실패했습니다.';
      showToast({ message, type: 'error' });
    } finally {
      setIsSaving(false);
    }
  };

  const handleEditCancel = () => {
    setIsEditing(false);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      void handleEditSave();
    }
    if (e.key === 'Escape') {
      e.preventDefault();
      handleEditCancel();
    }
  };

  return {
    isEditing,
    editValue,
    setEditValue,
    isSaving,
    inputRef,
    handleEditStart,
    handleEditSave,
    handleEditCancel,
    handleKeyDown,
  };
};
