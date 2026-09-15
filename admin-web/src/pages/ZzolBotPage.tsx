import { useSearchParams } from 'react-router-dom';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { ZzolBotChatPanel } from '@/pages/zzolbot/ZzolBotChatPanel';
import { ZzolBotEvalPanel } from '@/pages/zzolbot/ZzolBotEvalPanel';
import { ZzolBotMonitorPanel } from '@/pages/zzolbot/ZzolBotMonitorPanel';

type Panel = 'chat' | 'monitor' | 'eval';

const TABS: { value: Panel; label: string }[] = [
  { value: 'chat', label: '진단' },
  { value: 'monitor', label: '모니터링' },
  { value: 'eval', label: '평가' },
];

const DESCRIPTION: Record<Panel, string> = {
  chat: '운영 DB 를 읽어 답합니다. Grafana 를 열기 전에 먼저 물어보는 자리입니다.',
  monitor: '알림을 받아 스스로 분석한 기록입니다. 무엇이 튀었나가 아니라 그게 무슨 뜻인가가 남습니다.',
  eval: '프롬프트나 모델을 바꿨을 때 좋아졌는지를 숫자로 봅니다.',
};

/**
 * ZzolBot. 레거시 백오피스의 세 탭을 그대로 옮겼다.
 *
 * <p>탭을 화면 셋으로 쪼개지 않았다. 셋 다 같은 봇을 다른 각도에서 보는 것이고,
 * 실제로 진단하다가 "이 질문을 골든셋에 넣자"로 넘어가는 흐름이 잦다.
 *
 * <p>탭 상태를 <b>주소에 남긴다.</b> 한때 넣지 않았다. 이 화면을 링크로 주고받는 일이
 * 없다는 판단이었고 그건 지금도 맞다. 뒤집은 이유는 링크가 아니라 <b>새로고침</b>이다.
 * 평가 탭의 실행은 몇 분이 걸리고 그동안 표를 새로 고치게 되는데, 그때마다 진단 탭으로
 * 튕겨 다시 평가 탭을 찾아 들어가야 했다.
 *
 * <p>히스토리에 쌓는다(갈음하지 않는다). 탭은 화면을 옮기는 일에 가까워서 뒤로가기가
 * 직전 탭으로 돌아가는 편이 기대에 맞다. 패널을 여닫는 것과는 성격이 다르다.
 */
export function ZzolBotPage() {
  const [params, setParams] = useSearchParams();
  const panel = readPanel(params.get('tab'));

  const setPanel = (next: Panel) => {
    const updated = new URLSearchParams(params);
    updated.set('tab', next);
    setParams(updated);
  };

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="ZzolBot"
        description={DESCRIPTION[panel]}
        actions={<Tabs tabs={TABS} value={panel} onChange={setPanel} />}
      />

      {panel === 'chat' && <ZzolBotChatPanel />}
      {panel === 'monitor' && <ZzolBotMonitorPanel />}
      {panel === 'eval' && <ZzolBotEvalPanel />}
    </div>
  );
}

/** 모르는 값이 오면 진단으로 떨어뜨린다. 주소를 손으로 고친 경우에도 화면은 떠야 한다. */
function readPanel(value: string | null): Panel {
  return TABS.some((tab) => tab.value === value) ? (value as Panel) : 'chat';
}
