import { Component, ReactNode } from 'react';
import { ApiError, NetworkError } from '@/apis/rest/error';
import LocalErrorFallback from '@/components/@common/ErrorFallback/LocalErrorFallback';

type Props = {
  children: ReactNode;
  fallback?: (error: Error, retry: () => void) => ReactNode;
};

type State = {
  error: Error | null;
};

class LocalErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    if (error instanceof ApiError || error instanceof NetworkError) {
      if (error.displayMode === 'fallback') {
        return { error };
      }
    }

    throw error;
  }

  handleRetry = (): void => {
    this.setState({ error: null });
  };

  render(): ReactNode {
    const {
      fallback = (error: Error, retry: () => void) => (
        <LocalErrorFallback error={error} handleRetry={retry} />
      ),
    } = this.props;

    if (this.state.error) {
      return fallback(this.state.error, this.handleRetry);
    }

    return this.props.children;
  }
}

export default LocalErrorBoundary;
