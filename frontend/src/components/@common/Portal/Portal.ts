import { PropsWithChildren, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';

type Props = {
  containerId?: string;
} & PropsWithChildren;

const Portal = ({ children, containerId }: Props) => {
  const [container, setContainer] = useState<HTMLElement | null>(null);

  useEffect(() => {
    const targetContainer = containerId ? document.getElementById(containerId) : document.body;
    setContainer(targetContainer);
  }, [containerId]);

  if (!container) return children;
  return createPortal(children, container);
};

export default Portal;
