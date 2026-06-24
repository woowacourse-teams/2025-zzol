import Headline4 from '../Headline4/Headline4';
import * as S from './ToggleButton.styled';

type Props<Option extends string> = {
  options: Option[];
  selectedOption: Option;
  onSelectOption: (option: Option) => void;
};

const ToggleButton = <Option extends string>({
  options,
  selectedOption,
  onSelectOption,
}: Props<Option>) => {
  const selectedIndex = options.indexOf(selectedOption);

  return (
    <S.Container role="tabList">
      <S.Track>
        {options.map((option, index) => (
          <S.Option
            key={option}
            onClick={() => onSelectOption(option)}
            role="tab"
            aria-selected={selectedIndex === index}
            tabIndex={selectedIndex === index ? 0 : -1}
            aria-label={`${option}, ${index + 1}/${options.length}`}
            data-testid={`toggle-option-${option}`}
          >
            <Headline4 color={selectedIndex === index ? 'white' : 'gray-400'}>{option}</Headline4>
          </S.Option>
        ))}
        <S.Thumb index={selectedIndex} optionCount={options.length} />
      </S.Track>
    </S.Container>
  );
};

export default ToggleButton;
