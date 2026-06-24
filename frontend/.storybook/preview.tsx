import { ToastProvider } from '@/components/@common/Toast/ToastContext';
import { ThemeProvider } from '@emotion/react';
import type { Preview } from '@storybook/react-webpack5';
import { MemoryRouter } from 'react-router-dom';
import { ParticipantsProvider } from '@/contexts/Participants/ParticipantsProvider';
import { IdentifierProvider } from '../src/contexts/Identifier/IdentifierProvider';
import { WebSocketProvider } from '../src/apis/websocket/contexts/WebSocketProvider';
import { ModalProvider } from '../src/components/@common/Modal/ModalContext';
import CardGameProvider from '../src/contexts/CardGame/CardGameProvider';
import '../src/styles/global.css';
import '../src/styles/reset.css';
import { theme } from '../src/styles/theme';

const preview: Preview = {
  decorators: [
    (Story) => (
      <MemoryRouter>
        <ThemeProvider theme={theme}>
          <IdentifierProvider>
            <ParticipantsProvider>
              <WebSocketProvider>
                <CardGameProvider>
                  <ToastProvider>
                    <ModalProvider>
                      <Story />
                    </ModalProvider>
                  </ToastProvider>
                </CardGameProvider>
              </WebSocketProvider>
            </ParticipantsProvider>
          </IdentifierProvider>
        </ThemeProvider>
      </MemoryRouter>
    ),
  ],
};

export default preview;
