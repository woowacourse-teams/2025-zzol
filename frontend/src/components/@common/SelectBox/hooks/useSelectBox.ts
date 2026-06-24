import { useRef, useState, KeyboardEvent } from 'react';
import { Option } from '../SelectBox';

const useSelectBox = (options: Option[], value: string, onChange?: (value: Option) => void) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((option) => option.name === value);

  const handleOptionClick = (option: Option) => {
    onChange?.(option);
    setIsOpen(false);
    triggerRef.current?.focus();
  };

  const handleTriggerClick = () => {
    setIsOpen(!isOpen);
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
      case ' ': {
        e.preventDefault();
        setIsOpen(!isOpen);
        break;
      }
      case 'Escape': {
        setIsOpen(false);
        triggerRef.current?.focus();
        break;
      }
      case 'ArrowDown': {
        e.preventDefault();
        if (!isOpen) setIsOpen(true);
        break;
      }
      case 'ArrowUp': {
        e.preventDefault();
        if (isOpen) setIsOpen(false);
        break;
      }
    }
  };

  return {
    isOpen,
    setIsOpen,
    containerRef,
    triggerRef,
    selectedOption,
    handleOptionClick,
    handleTriggerClick,
    handleKeyDown,
  };
};

export default useSelectBox;
