export type QRCodeStatus = 'PENDING' | 'SUCCESS' | 'ERROR';

export type QRCodeEvent = {
  status: QRCodeStatus;
  qrCodeUrl?: string;
};
