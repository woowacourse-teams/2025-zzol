import { PropsWithChildren, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';

type Props = {
  containerId?: string;
} & PropsWithChildren;

const Portal = ({ children, containerId }: Props) => {
  const [container, setContainer] = useState<HTMLElement | null>(null);

  useEffect(() => {
    const targetContainer = containerId ? document.getElementById(containerId) : document.body;
    // Portal 대상 DOM 은 마운트 이후에 확정되므로 effect 에서 설정한다.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setContainer(targetContainer);
  }, [containerId]);

  if (!container) return children;
  return createPortal(children, container);
};

export default Portal;
