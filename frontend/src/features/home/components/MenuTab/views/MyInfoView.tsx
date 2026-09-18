import Skeleton from '@/components/@common/Skeleton/Skeleton';
import useModal from '@/components/@common/Modal/useModal';
import { useAuth } from '@/features/auth/contexts/AuthContext';
import DeleteAccountSheet from '@/features/auth/components/DeleteAccountSheet/DeleteAccountSheet';
import { useMyRecords } from '@/features/home/hooks/useMyRecords';
import { useMySeasonRank } from '@/features/home/hooks/useSeasonRanking';
import {
  RECORD_HINT,
  formatRecord,
  formatRecordDiff,
  formatWinRate,
  recordBarRatio,
  recordLabels,
  winRatePercent,
} from '@/features/home/utils/formatRecord';
import { PROVIDER_LABEL } from '@/features/home/components/tabs/MenuTab/AccountSection/AccountSection';
import { MINI_GAME_ICON_MAP, MINI_GAME_NAME_MAP } from '@/types/miniGame/common';
import type { GameRecord } from '@/types/records';
import * as S from './MyInfoView.styled';

const GameRecordCard = ({ record }: { record: GameRecord }) => {
  const { type, playCount, best, average } = record;
  const empty = best === null || average === null;
  const labels = recordLabels(type);
  const diff = empty ? null : formatRecordDiff(type, best, average);
  const ratio = empty ? null : recordBarRatio(type, best, average);
  const fills = ratio
    ? ([
        { tone: 'best', ratio: ratio.best },
        { tone: 'average', ratio: ratio.average },
      ] as const)
    : [];

  return (
    <S.GameCard $empty={empty}>
      <S.GameHeader>
        <S.IconTile $size={44} $muted={empty}>
          <img src={MINI_GAME_ICON_MAP[type]} alt="" aria-hidden="true" />
        </S.IconTile>
        <S.GameInfo>
          <S.GameName>{MINI_GAME_NAME_MAP[type]}</S.GameName>
          <S.Caption>{empty ? '아직 기록이 없어요' : RECORD_HINT[type]}</S.Caption>
        </S.GameInfo>
        <S.Chip $muted={empty}>{playCount}회</S.Chip>
      </S.GameHeader>

      {!empty && (
        <>
          <S.TileGrid>
            <S.Tile>
              <S.Caption>{labels.best}</S.Caption>
              <S.TileValue>{formatRecord(type, best)}</S.TileValue>
            </S.Tile>
            <S.Tile>
              <S.Caption>{labels.average}</S.Caption>
              <S.TileValue $muted>{formatRecord(type, average)}</S.TileValue>
            </S.Tile>
          </S.TileGrid>
          {diff && (
            <>
              <S.DiffBar>
                {/* 긴 막대를 먼저 그려 짧은 막대가 위에 보이게 한다 */}
                {[...fills]
                  .sort((a, b) => b.ratio - a.ratio)
                  .map(({ tone, ratio: r }) => (
                    <S.DiffFill key={tone} $tone={tone} $ratio={r} />
                  ))}
              </S.DiffBar>
              <S.DiffCaption>{diff}</S.DiffCaption>
            </>
          )}
        </>
      )}
    </S.GameCard>
  );
};

const RecordsSkeleton = () => (
  <>
    <Skeleton height={220} borderRadius={20} />
    <Skeleton height={104} borderRadius={16} />
    <Skeleton height={24} width={80} borderRadius={8} />
    <Skeleton height={168} borderRadius={16} />
    <Skeleton height={168} borderRadius={16} />
  </>
);

const MyInfoView = () => {
  const { isAuthenticated, user } = useAuth();
  const { data: records, loading, failed } = useMyRecords();
  const { data: myRank } = useMySeasonRank();
  const { openModal } = useModal();

  if (!isAuthenticated || !user) return null;

  const handleDeleteAccount = () => {
    openModal(<DeleteAccountSheet />, {
      title: '회원 탈퇴',
      showCloseButton: true,
      closeOnBackdropClick: true,
    });
  };

  const roulette = records?.roulette;
  const winRate = roulette ? (winRatePercent(roulette.winCount, roulette.playCount) ?? 0) : 0;
  const mostPlayed = records?.minigame.mostPlayed ?? null;

  return (
    <S.Container>
      {loading && <RecordsSkeleton />}
      {!loading && failed && <S.ErrorText>기록을 불러오지 못했어요</S.ErrorText>}
      {!loading && records && roulette && (
        <>
          <S.ProfileCard>
            <S.ProfileRow>
              <S.Avatar>{user.nickname.slice(0, 1)}</S.Avatar>
              <S.ProfileInfo>
                <S.Nickname>{user.nickname}</S.Nickname>
                <S.Caption>
                  {PROVIDER_LABEL[user.provider] ?? user.provider} · #{user.userCode}
                </S.Caption>
              </S.ProfileInfo>
              {myRank && (
                <S.SeasonPill>
                  {myRank.tier} {myRank.rank}위
                </S.SeasonPill>
              )}
            </S.ProfileRow>
            <S.WinRow>
              <S.Ring>
                <S.RingSvg viewBox="0 0 112 112" aria-hidden="true">
                  <S.RingTrack cx="56" cy="56" r="48" pathLength="100" />
                  {/* 0% 는 round linecap 때문에 점이 남아 아예 그리지 않는다 */}
                  {winRate > 0 && (
                    <S.RingArc
                      cx="56"
                      cy="56"
                      r="48"
                      pathLength="100"
                      strokeDasharray={`${winRate} 100`}
                    />
                  )}
                </S.RingSvg>
                <S.RingCenter>
                  <S.RingPercent>
                    {formatWinRate(roulette.winCount, roulette.playCount)}
                  </S.RingPercent>
                  <S.Caption>당첨 확률</S.Caption>
                </S.RingCenter>
              </S.Ring>
              <S.WinStats>
                <S.WinStatRow>
                  <S.WinStatLabel>누적 당첨</S.WinStatLabel>
                  <S.WinStatValue>{roulette.winCount}회</S.WinStatValue>
                </S.WinStatRow>
                <S.WinStatRow>
                  <S.WinStatLabel>내기 참여</S.WinStatLabel>
                  <S.WinStatValue>{roulette.playCount}판</S.WinStatValue>
                </S.WinStatRow>
                <S.WinStatRow>
                  <S.WinStatLabel>연속 생존</S.WinStatLabel>
                  <S.WinStatValue $accent>{roulette.survivalStreak}번</S.WinStatValue>
                </S.WinStatRow>
              </S.WinStats>
            </S.WinRow>
          </S.ProfileCard>

          <S.SummaryCard>
            <S.SummaryCell>
              <S.StatLabel>미니게임 총 플레이</S.StatLabel>
              <S.StatValueRow>
                <S.StatNumber>{records.minigame.totalPlayCount}</S.StatNumber>
                <S.StatUnit>판</S.StatUnit>
              </S.StatValueRow>
            </S.SummaryCell>
            <S.SummaryCell>
              <S.StatLabel>가장 많이 한 게임</S.StatLabel>
              {mostPlayed ? (
                <S.MostPlayed>
                  <S.IconTile $size={28}>
                    <img src={MINI_GAME_ICON_MAP[mostPlayed.type]} alt="" aria-hidden="true" />
                  </S.IconTile>
                  <S.MostPlayedText>
                    <S.MostPlayedName>{MINI_GAME_NAME_MAP[mostPlayed.type]}</S.MostPlayedName>
                    <S.Caption>{mostPlayed.playCount}판</S.Caption>
                  </S.MostPlayedText>
                </S.MostPlayed>
              ) : (
                <S.EmptyText>아직 없어요</S.EmptyText>
              )}
            </S.SummaryCell>
          </S.SummaryCard>

          <S.SectionHeader>
            <S.SectionTitle>게임 기록</S.SectionTitle>
            <S.Chip>완주 기준</S.Chip>
          </S.SectionHeader>
          {records.games.map((record) => (
            <GameRecordCard key={record.type} record={record} />
          ))}
        </>
      )}

      <S.InfoSection>
        <S.InfoTitle>통계 안내</S.InfoTitle>
        <S.TooltipCard>
          <S.TooltipList>
            <li>• 당첨 확률은 참여한 내기 판수 대비 당첨된 비율입니다.</li>
            <li>• 총 플레이는 로그인 상태로 참여한 모든 미니게임 판수입니다.</li>
            <li>• 게임 기록은 완주한 판만 집계합니다.</li>
            <li>• 시즌 순위는 매월 초기화됩니다.</li>
          </S.TooltipList>
        </S.TooltipCard>
      </S.InfoSection>

      <S.DangerCard>
        <S.DangerRow type="button" onClick={handleDeleteAccount}>
          <S.DangerLabel>회원 탈퇴하기</S.DangerLabel>
          <S.DangerIcon>›</S.DangerIcon>
        </S.DangerRow>
      </S.DangerCard>
    </S.Container>
  );
};

export default MyInfoView;
