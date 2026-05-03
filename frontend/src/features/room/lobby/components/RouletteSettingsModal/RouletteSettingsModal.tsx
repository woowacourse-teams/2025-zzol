import Modal from '@/components/@common/Modal/Modal';
import { ChangeEvent, useEffect, useState } from 'react';
import useUpdateAdjustmentWeight from '../../hooks/useUpdateAdjustmentWeight';
import * as S from './RouletteSettingsModal.styled';

const MIN = 0.1;
const MAX = 0.9;
const STEP = 0.1;
const DEFAULT_WEIGHT = 0.7;

type Props = {
  isOpen: boolean;
  onClose: () => void;
  currentWeight?: number;
};

const RouletteSettingsModal = ({ isOpen, onClose, currentWeight = DEFAULT_WEIGHT }: Props) => {
  const [weight, setWeight] = useState(currentWeight);

  useEffect(() => {
    if (isOpen) {
      setWeight(currentWeight);
    }
  }, [isOpen, currentWeight]);

  const { updateAdjustmentWeight, loading } = useUpdateAdjustmentWeight(onClose);

  const fillPercent = Math.round(((weight - MIN) / (MAX - MIN)) * 100);

  const handleConfirm = () => {
    updateAdjustmentWeight(weight);
  };

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setWeight(parseFloat(e.target.value));
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="가중치 설정 변경">
      <S.Content>
        <S.ValueSection>
          <S.ValueText>{weight.toFixed(1)}</S.ValueText>
          {weight === DEFAULT_WEIGHT && <S.DefaultTag>기본값</S.DefaultTag>}
        </S.ValueSection>

        <S.SliderSection>
          <S.Slider
            type="range"
            aria-label="가중치 조절 슬라이더"
            aria-valuemin={MIN}
            aria-valuemax={MAX}
            aria-valuenow={weight}
            min={MIN}
            max={MAX}
            step={STEP}
            value={weight}
            onChange={handleChange}
            $fillPercent={fillPercent}
          />
          <S.RangeLabels>
            <S.RangeLabel $align="left">균등한 확률</S.RangeLabel>
            <S.RangeLabel $align="right">실력 반영</S.RangeLabel>
          </S.RangeLabels>
        </S.SliderSection>

        <S.Description>값의 크기에 따라 룰렛의 변동폭이 변경돼요!</S.Description>

        <S.ConfirmButton onClick={handleConfirm} disabled={loading} aria-busy={loading}>
          {loading ? '저장 중...' : '확인'}
        </S.ConfirmButton>
      </S.Content>
    </Modal>
  );
};

export default RouletteSettingsModal;
