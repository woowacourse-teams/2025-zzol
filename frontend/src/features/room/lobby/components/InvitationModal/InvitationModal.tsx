import CopyIcon from '@/assets/copy-icon.svg';
import Headline4 from '@/components/@common/Headline4/Headline4';
import Paragraph from '@/components/@common/Paragraph/Paragraph';
import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import useToast from '@/components/@common/Toast/useToast';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import TabBar from '@/features/room/lobby/components/TabBar/TabBar';
import { useState } from 'react';
import * as S from './InvitationModal.styled';
import { useInvitationModalScreenReader } from './useInvitationModalScreenReader';

type props = {
  onClose: () => void;
};

const InvitationModal = ({ onClose }: props) => {
  const { showToast } = useToast();
  const { joinCode, qrCodeUrl } = useIdentifier();
  const [activeTabIndex, setActiveTabIndex] = useState(0);
  const tabs = ['QR코드', '초대코드'];
  const { screenReaderRef, message: screenReaderMessage } = useInvitationModalScreenReader();

  const handleCopy = async () => {
    copyToClipboard(joinCode, '초대 코드가 복사되었습니다.');
  };

  const handleShareLink = async () => {
    const shareUrl = `${window.location.origin}/join/${joinCode}`;
    copyToClipboard(shareUrl, '참여 링크가 복사되었습니다.');
  };

  const copyToClipboard = async (text: string, message: string) => {
    await navigator.clipboard.writeText(text);
    showToast({
      type: 'success',
      message,
    });
    onClose();
  };

  return (
    <S.Container>
      {screenReaderMessage && (
        <ScreenReaderOnly aria-live="assertive" ref={screenReaderRef}>
          {screenReaderMessage}
        </ScreenReaderOnly>
      )}
      <TabBar tabs={tabs} activeTabIndex={activeTabIndex} onTabChange={setActiveTabIndex} />
      {activeTabIndex === 0 ? (
        <QRSection qrCodeUrl={qrCodeUrl} handleShareLink={handleShareLink} />
      ) : (
        <CodeSection joinCode={joinCode} handleCopy={handleCopy} />
      )}
    </S.Container>
  );
};

export default InvitationModal;

type QRSectionProps = {
  qrCodeUrl: string | null;
  handleShareLink: () => void;
};

type CodeSectionProps = {
  joinCode: string;
  handleCopy: () => void;
};

const QRSection = ({ qrCodeUrl, handleShareLink }: QRSectionProps) => {
  return (
    <S.QRSection>
      <S.QRCode>
        {qrCodeUrl && qrCodeUrl.trim() !== '' ? (
          <img src={qrCodeUrl} alt="QR Code" />
        ) : (
          <S.Description>QR 코드 생성 중...</S.Description>
        )}
      </S.QRCode>
      <S.ShareButton onClick={handleShareLink} aria-label="링크 공유하기">
        <Paragraph>링크 공유하기</Paragraph>
      </S.ShareButton>
      <S.Wrapper>
        <Paragraph>QR코드를 스캔하면</Paragraph>
        <Paragraph>바로 게임에 참여할 수 있어요!</Paragraph>
      </S.Wrapper>
    </S.QRSection>
  );
};

const CodeSection = ({ joinCode, handleCopy }: CodeSectionProps) => {
  return (
    <S.CodeSection>
      <S.Wrapper>
        <Paragraph>초대코드를 복사하여</Paragraph>
        <Paragraph>친구들을 초대해보세요!</Paragraph>
      </S.Wrapper>
      <S.CodeBox>
        <S.EmptyBox />
        <Headline4 aria-label={joinCode.split('').join(' ')} aria-live="polite">
          {joinCode}
        </Headline4>
        <S.CopyIcon src={CopyIcon} onClick={handleCopy} aria-label="초대코드 복사하기" />
      </S.CodeBox>
    </S.CodeSection>
  );
};
