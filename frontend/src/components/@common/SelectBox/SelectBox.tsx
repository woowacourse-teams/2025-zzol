import { ComponentProps } from 'react';
import * as S from './SelectBox.styled';
import useSelectBox from './hooks/useSelectBox';
import useClickOutside from './hooks/useClickOutside';

export type Option = {
  id: number;
  name: string;
};

export type Props = Omit<ComponentProps<'div'>, 'onChange'> & {
  options: Option[];
  value: string;
  placeholder?: string;
  width?: string;
  height?: string;
  onChange?: (value: Option) => void;
};

const SelectBox = ({
  options,
  value,
  placeholder = '선택하세요',
  width = '100%',
  height = '32px',
  onChange,
  ...rest
}: Props) => {
  const {
    isOpen,
    setIsOpen,
    containerRef,
    triggerRef,
    selectedOption,
    handleOptionClick,
    handleTriggerClick,
    handleKeyDown,
  } = useSelectBox(options, value, onChange);

  useClickOutside(containerRef, () => setIsOpen(false));

  return (
    <S.Container ref={containerRef} $width={width} $height={height} {...rest}>
      <S.Trigger
        ref={triggerRef}
        $isOpen={isOpen}
        onClick={handleTriggerClick}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        role="combobox"
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-label={selectedOption ? selectedOption.name : placeholder}
      >
        <S.SelectText $hasValue={!!selectedOption}>
          {selectedOption ? selectedOption.name : placeholder}
        </S.SelectText>
        <S.ArrowIcon $isOpen={isOpen} />
      </S.Trigger>

      <S.Content $isOpen={isOpen} role="listbox">
        {options.map((option) => (
          <S.Item
            key={option.id}
            $selected={option.name === value}
            onClick={() => handleOptionClick(option)}
            role="option"
            aria-selected={String(option.id) === value}
          >
            {option.name}
          </S.Item>
        ))}
      </S.Content>
    </S.Container>
  );
};

export default SelectBox;
