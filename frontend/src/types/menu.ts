export type TemperatureOption = 'HOT' | 'ICE';

export type TemperatureAvailability = 'ICE_ONLY' | 'HOT_ONLY' | 'BOTH';

export type Menu = {
  id: number;
  name: string;
  temperatureAvailability: TemperatureAvailability;
};

export type PlayerMenu = {
  id: number;
  name: string;
  temperature: TemperatureOption;
  categoryImageUrl: string;
};

export type Category = {
  id: number;
  name: string;
  imageUrl: string;
};
