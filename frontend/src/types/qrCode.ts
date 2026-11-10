import type { QrCodeStatusResponse } from '@/apis/websocket/generated/wsContract';

export type { QrCodeStatus as QRCodeStatus } from '@/apis/websocket/generated/wsContract';

export type QRCodeEvent = QrCodeStatusResponse;
