import { Component, ReactNode } from 'react';
import GlobalErrorFallback from '@/components/@common/ErrorFallback/GlobalErrorFallback';

type Props = {
  children: ReactNode;
  fallback?: ReactNode;
};

type State = {
  error: Error | null;
};

class GlobalErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  render(): ReactNode {
    const { fallback = <GlobalErrorFallback error={this.state.error!} /> } = this.props;

    if (this.state.error) {
      return fallback;
    }

    return this.props.children;
  }
}

export default GlobalErrorBoundary;
