import { z } from "zod";

const SourceSchema = z.object({
  className: z.string(),
  methodName: z.string(),
});

const FieldEntrySchema = z.object({
  name: z.string(),
  type: z.string(),
});

const PublisherSchema = z.object({
  description: z.string(),
  source: SourceSchema,
});

const EnvelopeSchema = z.object({
  type: z.string(),
  fields: z.array(FieldEntrySchema),
  note: z.string(),
});

const TopicEntrySchema = z.object({
  path: z.string(),
  payloadType: z.string().nullable(),
  publishers: z.array(PublisherSchema),
});

const QueueEntrySchema = z.object({
  path: z.string(),
  payloadType: z.string().nullable(),
  publishers: z.array(PublisherSchema),
});

const SendEntrySchema = z.object({
  destination: z.string(),
  description: z.string(),
  requestType: z.string().nullable(),
  triggersTopics: z.array(z.string()),
  source: SourceSchema,
});

const SchemaEntrySchema = z.object({
  kind: z.enum(["record", "enum", "object"]),
  fields: z.array(FieldEntrySchema).nullable(),
  values: z.array(z.string()).nullable(),
});

const ErrorShapeSchema = z.object({
  topic: z.string(),
  payloadType: z.string(),
});

export const WsCatalogSchema = z.object({
  stompEndpoint: z.string(),
  app: z.string(),
  topicPrefix: z.string(),
  queuePrefix: z.string(),
  envelope: EnvelopeSchema,
  topics: z.array(TopicEntrySchema),
  queues: z.array(QueueEntrySchema),
  sends: z.array(SendEntrySchema),
  schemas: z.record(z.string(), SchemaEntrySchema),
  errors: ErrorShapeSchema,
});

export type WsCatalog = z.infer<typeof WsCatalogSchema>;
export type TopicEntry = z.infer<typeof TopicEntrySchema>;
export type QueueEntry = z.infer<typeof QueueEntrySchema>;
export type SendEntry = z.infer<typeof SendEntrySchema>;
export type Publisher = z.infer<typeof PublisherSchema>;
export type Source = z.infer<typeof SourceSchema>;
export type SchemaEntry = z.infer<typeof SchemaEntrySchema>;
