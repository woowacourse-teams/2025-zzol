import useMutation from '@/apis/rest/useMutation';
import useToast from '@/components/@common/Toast/useToast';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useEffect, useRef } from 'react';

type Request = { hostName: string; adjustmentWeight: number };

const useUpdateAdjustmentWeight = (onSuccess?: () => void) => {
  const { joinCode, myName } = useIdentifier();
  const { showToast } = useToast();

  const onSuccessRef = useRef(onSuccess);
  useEffect(() => {
    onSuccessRef.current = onSuccess;
  }, [onSuccess]);

  const { mutate: _mutate, loading } = useMutation<void, Request>({
    endpoint: `/rooms/${joinCode}/settings`,
    method: 'PATCH',
    errorDisplayMode: 'toast',
    onSuccess: () => {
      showToast({ type: 'success', message: '공정성 수준이 변경됐습니다' });
      onSuccessRef.current?.();
    },
  });

  const updateAdjustmentWeight = (adjustmentWeight: number) => {
    _mutate({ hostName: myName, adjustmentWeight });
  };

  return { updateAdjustmentWeight, loading };
};

export default useUpdateAdjustmentWeight;
